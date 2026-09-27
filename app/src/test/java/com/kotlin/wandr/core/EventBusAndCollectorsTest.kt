package com.kotlin.wandr.core

import app.cash.turbine.test
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.event.CacheInvalidator
import com.kotlin.wandr.core.telemetry.TelemetryCollector
import com.kotlin.wandr.data.local.WandrDatabase
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.data.repository.TelemetryRepository
import com.kotlin.wandr.domain.model.Tier
import com.kotlin.wandr.testutil.FakeConnectivity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventBusAndCollectorsTest {

    private val bus = AppEventBus()

    @Test
    fun `subscribers only receive the event type they asked for`() = runTest {
        bus.subscribe<AppEvent.QuestStarted>().test {
            bus.publish(AppEvent.EventJoined("e1"))
            bus.publish(AppEvent.QuestStarted("q1"))
            assertEquals(AppEvent.QuestStarted("q1"), awaitItem())
        }
    }

    // ---------- TelemetryCollector ----------

    /** In-memory stand-in for Room + Supabase. */
    private class FakeTelemetryRepository : TelemetryRepository {
        val pending = mutableListOf<AppEvent.RequestTimed>()
        val sent = mutableListOf<AppEvent.RequestTimed>()
        override suspend fun record(event: AppEvent.RequestTimed) {
            pending += event
        }
        override suspend fun pendingCount() = pending.size
        override suspend fun flush(): Result<Int> {
            val count = pending.size
            sent += pending
            pending.clear()
            return Result.success(count)
        }
    }

    private fun timed(i: Int) = AppEvent.RequestTimed("quest_step_response", durationMs = i.toLong(), success = true)

    private val connectivity = FakeConnectivity(online = true)

    private fun TestScope.collector(repository: TelemetryRepository) = TelemetryCollector(
        eventBus = bus,
        repository = repository,
        connectivity = connectivity,
        scope = backgroundScope,
    ).also { it.start() }

    @Test
    fun `collector stores events and sends them once a batch is full`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeTelemetryRepository()
        collector(repository)

        repeat(TelemetryCollector.BATCH_SIZE - 1) { bus.publish(timed(it)) }
        assertEquals(TelemetryCollector.BATCH_SIZE - 1, repository.pending.size)
        assertEquals(0, repository.sent.size)

        bus.publish(timed(99))
        assertEquals(0, repository.pending.size)
        assertEquals(TelemetryCollector.BATCH_SIZE, repository.sent.size)
    }

    @Test
    fun `collector keeps events offline and sends them when the connection returns`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeTelemetryRepository()
            connectivity.status.value = false
            collector(repository)

            repeat(TelemetryCollector.BATCH_SIZE) { bus.publish(timed(it)) }
            assertEquals(TelemetryCollector.BATCH_SIZE, repository.pending.size)
            assertEquals(0, repository.sent.size)

            connectivity.status.value = true
            bus.publish(AppEvent.ConnectivityChanged(isOnline = true))
            assertEquals(TelemetryCollector.BATCH_SIZE, repository.sent.size)
        }

    @Test
    fun `app going to background flushes`() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeTelemetryRepository()
        val collector = collector(repository)
        bus.publish(timed(1))

        collector.onAppBackground()
        testScheduler.advanceUntilIdle()
        assertEquals(1, repository.sent.size)
    }

    // ---------- CacheInvalidator ----------

    @Test
    fun `completing a quest refreshes the profile, and signing out clears the cache`() =
        runTest(UnconfinedTestDispatcher()) {
            val profileRepository = mockk<ProfileRepository>()
            coEvery { profileRepository.refreshAll() } returns Result.success(Unit)
            val database = mockk<WandrDatabase>(relaxed = true)
            CacheInvalidator(bus, profileRepository, database, backgroundScope).start()

            bus.publish(AppEvent.QuestCompleted("q1", 100, 2, Tier.BOGOTA_SCOUT, 1, emptyList()))
            coVerify(exactly = 1) { profileRepository.refreshAll() }

            bus.publish(AppEvent.SignedOut)
            // clearAllTables runs on Dispatchers.IO, outside the test scheduler
            verify(timeout = 2_000, exactly = 1) { database.clearAllTables() }
        }
}
