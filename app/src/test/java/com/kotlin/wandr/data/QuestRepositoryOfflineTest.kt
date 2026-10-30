package com.kotlin.wandr.data

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.storage.PendingPhotoStore
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.core.sync.QuestSyncScheduler
import com.kotlin.wandr.data.local.dao.PendingActionDao
import com.kotlin.wandr.data.local.dao.QuestDao
import com.kotlin.wandr.data.local.entity.PendingActionType
import com.kotlin.wandr.data.local.entity.PendingQuestActionEntity
import com.kotlin.wandr.data.remote.datasource.QuestRemoteDataSource
import com.kotlin.wandr.data.remote.dto.ObjectiveResultDto
import com.kotlin.wandr.data.repository.QuestRepositoryImpl
import com.kotlin.wandr.data.repository.StorageRepository
import com.kotlin.wandr.domain.model.StepOutcome
import com.kotlin.wandr.domain.model.SyncResult
import com.kotlin.wandr.testutil.FakeConnectivity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock

/** Eventual connectivity: quest actions are saved offline and sent later, in order. */
class QuestRepositoryOfflineTest {

    private class FakePendingDao : PendingActionDao {
        val rows = MutableStateFlow<List<PendingQuestActionEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(action: PendingQuestActionEntity): Long {
            val id = nextId++
            rows.value = rows.value + action.copy(id = id)
            return id
        }
        override suspend fun oldest() = rows.value.firstOrNull()
        override suspend fun all() = rows.value
        override fun observeAll(): Flow<List<PendingQuestActionEntity>> = rows
        override suspend fun count() = rows.value.size
        override suspend fun delete(id: Long) { rows.value = rows.value.filterNot { it.id == id } }
        override suspend fun addAttempt(id: Long) {
            rows.value = rows.value.map { if (it.id == id) it.copy(attempts = it.attempts + 1) else it }
        }
    }

    private val remote = mockk<QuestRemoteDataSource>(relaxed = true) {
        coEvery { activeCompletions() } returns emptyList()
    }
    private val pendingDao = FakePendingDao()
    private val photoStore = mockk<PendingPhotoStore>(relaxed = true)
    private val storage = mockk<StorageRepository>()
    private val scheduler = mockk<QuestSyncScheduler>(relaxed = true)
    private val connectivity = FakeConnectivity(online = false)

    private val repository = QuestRepositoryImpl(
        remote = remote,
        questDao = mockk<QuestDao>(relaxed = true),
        placeDao = mockk(relaxed = true),
        strategies = StrategySelector(connectivity),
        eventBus = AppEventBus(),
        telemetry = mockk(relaxed = true),
        pendingDao = pendingDao,
        photoStore = photoStore,
        storage = storage,
        connectivity = connectivity,
        syncScheduler = scheduler,
        clock = Clock.systemUTC(),
    )

    @Test
    fun `offline a step is saved with its photo and the sync is scheduled`() = runTest {
        coEvery { photoStore.save(any()) } returns "photo.jpg"

        val outcome = repository.checkObjective("q1", "o2", byteArrayOf(1, 2)).getOrThrow()

        assertEquals(StepOutcome.Queued, outcome)
        val saved = pendingDao.rows.value.single()
        assertEquals(PendingActionType.COMPLETE, saved.type)
        assertEquals("photo.jpg", saved.photoFile)
        verify { scheduler.schedule() }
        coVerify(exactly = 0) { remote.completeObjective(any(), any(), any()) }
    }

    @Test
    fun `without space for the photo nothing is saved`() = runTest {
        coEvery { photoStore.save(any()) } throws AppException(AppError.NoSpace)

        val error = repository.checkObjective("q1", "o2", byteArrayOf(1)).exceptionOrNull()

        assertEquals(AppError.NoSpace, (error as AppException).error)
        assertTrue(pendingDao.rows.value.isEmpty())
    }

    @Test
    fun `if the network fails while online the step is saved instead`() = runTest {
        connectivity.status.value = true
        coEvery { remote.completeObjective("q1", "o1", null) } throws IOException("timeout")

        val outcome = repository.checkObjective("q1", "o1", null).getOrThrow()

        assertEquals(StepOutcome.Queued, outcome)
        assertEquals(1, pendingDao.rows.value.size)
    }

    @Test
    fun `sync sends the pending actions in order and empties the queue`() = runTest {
        repository.startQuest("q1")
        repository.checkObjective("q1", "o1", null)
        connectivity.status.value = true
        coEvery { remote.completeObjective("q1", "o1", null) } returns ObjectiveResultDto(1, 2, false)

        val result = repository.syncPending()

        assertEquals(SyncResult.DONE, result)
        assertTrue(pendingDao.rows.value.isEmpty())
        coVerifyOrder {
            remote.startQuest("q1")
            remote.completeObjective("q1", "o1", null)
        }
    }

    @Test
    fun `a step the server already closed is dropped without error`() = runTest {
        repository.checkObjective("q1", "o1", null)
        connectivity.status.value = true
        coEvery { remote.completeObjective(any(), any(), any()) } throws
            AppException(AppError.Server("Quest is not in progress"))

        val result = repository.syncPending()

        assertEquals(SyncResult.DONE, result)
        assertTrue(pendingDao.rows.value.isEmpty())
    }

    @Test
    fun `without network the sync asks to retry and keeps the actions`() = runTest {
        repository.abandonQuest("q1")
        coEvery { remote.abandonQuest("q1") } throws IOException("no network")

        val result = repository.syncPending()

        assertEquals(SyncResult.RETRY, result)
        assertEquals(1, pendingDao.rows.value.single().attempts)
    }
}
