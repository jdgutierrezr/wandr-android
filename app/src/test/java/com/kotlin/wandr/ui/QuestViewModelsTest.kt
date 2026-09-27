package com.kotlin.wandr.ui

import androidx.lifecycle.SavedStateHandle
import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.data.repository.StorageRepository
import com.kotlin.wandr.data.repository.TagRepository
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.QuestDetail
import com.kotlin.wandr.domain.model.Tag
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.testutil.activeQuest
import com.kotlin.wandr.testutil.objective
import com.kotlin.wandr.testutil.quest
import com.kotlin.wandr.ui.feature.home.HomeViewModel
import com.kotlin.wandr.ui.feature.quest.ActiveQuestViewModel
import com.kotlin.wandr.ui.feature.quest.QuestDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuestViewModelsTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val questRepository = mockk<QuestRepository>()
    private val tagRepository = mockk<TagRepository>()
    private val storageRepository = mockk<StorageRepository>()
    private val bus = AppEventBus()
    private val location = UserLocation(LocationProvider.BOGOTA_CENTER, UserLocation.Source.FALLBACK)
    private val locationProvider = mockk<LocationProvider> { coEvery { currentLocation() } returns location }

    // ---------- Home ----------

    private fun home(nearby: Resource<List<com.kotlin.wandr.domain.model.Quest>>): HomeViewModel {
        every { questRepository.nearbyQuests(any(), any(), any()) } returns flowOf(Resource.Loading, nearby)
        every { tagRepository.observeInterestIds() } returns flowOf(setOf("t-food"))
        every { tagRepository.observeTags(any()) } returns
            flowOf(Resource.Success(listOf(Tag("t-food", "food"), Tag("t-music", "music"))))
        return HomeViewModel(questRepository, tagRepository, locationProvider, bus)
    }

    @Test
    fun `home recommends quests that match the user's interests`() {
        val viewModel = home(Resource.Success(listOf(quest("q1", "music"), quest("q2", "food", "outdoors"))))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.quests.size)
        assertEquals(listOf("q2"), state.recommendations.map { it.quest.id })
        assertEquals("food", state.recommendations.single().becauseYouLiked)
        assertEquals(listOf("food", "music", "outdoors"), state.availableTags)
        assertEquals(UserLocation.Source.FALLBACK, state.locationSource)
    }

    @Test
    fun `home filters by tag`() {
        val viewModel = home(Resource.Success(listOf(quest("q1", "music"), quest("q2", "food"))))
        viewModel.selectTag("music")
        assertEquals(listOf("q1"), viewModel.uiState.value.quests.map { it.id })
        viewModel.selectTag(null)
        assertEquals(2, viewModel.uiState.value.quests.size)
    }

    @Test
    fun `home tells the user when it shows saved data`() {
        val viewModel = home(Resource.Success(listOf(quest("q1")), isStale = true))
        assertTrue(viewModel.uiState.value.isShowingSavedData)
    }

    @Test
    fun `home reloads by itself when the connection returns`() = runTest {
        val viewModel = home(Resource.Success(listOf(quest("q1")), isStale = true))
        bus.publish(AppEvent.ConnectivityChanged(isOnline = true))
        verify(exactly = 2) { questRepository.nearbyQuests(any(), any(), any()) }
        assertTrue(viewModel.uiState.value.quests.isNotEmpty())
    }

    // ---------- Quest detail ----------

    @Test
    fun `quest detail starts the quest and asks to open the tracker`() {
        val detail = QuestDetail(quest("q1"), place = null, objectives = listOf(objective("o1", "q1", 1)))
        every { questRepository.questDetail("q1", any()) } returns flowOf(Resource.Success(detail))
        every { questRepository.activeQuests(any()) } returns flowOf(Resource.Success(emptyList()))
        coEvery { questRepository.startQuest("q1") } returns Result.success(Unit)

        val viewModel = QuestDetailViewModel(SavedStateHandle(mapOf(QuestDetailViewModel.QUEST_ID_ARG to "q1")), questRepository)
        assertEquals(detail, viewModel.uiState.value.detail)
        assertFalse(viewModel.uiState.value.isInProgress)

        viewModel.startQuest()
        assertEquals("q1", viewModel.uiState.value.startedQuestId)
    }

    @Test
    fun `quest detail shows backend rules as messages`() {
        every { questRepository.questDetail("q1", any()) } returns flowOf(Resource.Loading)
        every { questRepository.activeQuests(any()) } returns
            flowOf(Resource.Success(listOf(activeQuest("q1", listOf(objective("o1", "q1", 1))))))
        coEvery { questRepository.startQuest("q1") } returns
            Result.failure(AppException(AppError.Server("You already completed this quest")))

        val viewModel = QuestDetailViewModel(SavedStateHandle(mapOf(QuestDetailViewModel.QUEST_ID_ARG to "q1")), questRepository)
        assertTrue(viewModel.uiState.value.isInProgress)
        viewModel.startQuest()
        assertEquals("You already completed this quest", viewModel.uiState.value.errorMessage)
    }

    // ---------- Active quest ----------

    private val photoStep = objective("o2", "q1", 2, requiresPhoto = true)
    private val active = activeQuest("q1", listOf(objective("o1", "q1", 1), photoStep), done = setOf("o1"))

    private fun tracker(): ActiveQuestViewModel {
        every { questRepository.activeQuests(any()) } returns MutableStateFlow(Resource.Success(listOf(active)))
        return ActiveQuestViewModel(questRepository, storageRepository)
    }

    @Test
    fun `a photo step cannot be checked without a photo`() {
        val viewModel = tracker()
        viewModel.completeObjective("q1", "o2", photoJpeg = null)
        assertEquals("This step needs a photo", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { questRepository.completeObjective(any(), any(), any()) }
    }

    @Test
    fun `photo is uploaded first and its path is sent with the step`() {
        val bytes = byteArrayOf(1, 2, 3)
        val done = ObjectiveResult(2, 2, true, 150, 1400, 8, null, 6, emptyList())
        coEvery { storageRepository.uploadQuestPhoto("q1", "o2", bytes) } returns Result.success("u1/q1/o2.jpg")
        coEvery { questRepository.completeObjective("q1", "o2", "u1/q1/o2.jpg") } returns Result.success(done)

        val viewModel = tracker()
        viewModel.completeObjective("q1", "o2", bytes)

        coVerifyOrder {
            storageRepository.uploadQuestPhoto("q1", "o2", bytes)
            questRepository.completeObjective("q1", "o2", "u1/q1/o2.jpg")
        }
        assertEquals(done, viewModel.uiState.value.lastResult)
        assertEquals(null, viewModel.uiState.value.submittingObjectiveId)
    }

    @Test
    fun `a failed upload does not check the step`() {
        coEvery { storageRepository.uploadQuestPhoto(any(), any(), any()) } returns
            Result.failure(AppException(AppError.Network))

        val viewModel = tracker()
        viewModel.completeObjective("q1", "o2", byteArrayOf(1))

        assertEquals(AppError.Network.message, viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { questRepository.completeObjective(any(), any(), any()) }
    }
}
