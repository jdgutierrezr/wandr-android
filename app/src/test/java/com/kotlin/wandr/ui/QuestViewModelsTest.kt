package com.kotlin.wandr.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.telemetry.TelemetryEvents
import com.kotlin.wandr.data.cache.MysteryQuestCache
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.data.remote.dto.RecommendedQuestDto
import com.kotlin.wandr.data.repository.HomePreferences
import com.kotlin.wandr.data.repository.HomePreferencesRepository
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.data.repository.StorageRepository
import com.kotlin.wandr.data.repository.TelemetryRepository
import com.kotlin.wandr.domain.model.DropoffPoint
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.QuestDropoffReport
import com.kotlin.wandr.domain.model.QuestDetail
import com.kotlin.wandr.testutil.FakeConnectivity
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.testutil.activeQuest
import com.kotlin.wandr.testutil.objective
import com.kotlin.wandr.testutil.quest
import com.kotlin.wandr.ui.feature.home.HomeViewModel
import com.kotlin.wandr.ui.feature.quest.ActiveQuestViewModel
import com.kotlin.wandr.ui.feature.quest.LocationStatus
import com.kotlin.wandr.ui.feature.quest.QuestDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuestViewModelsTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val questRepository = mockk<QuestRepository>()
    private val storageRepository = mockk<StorageRepository>()
    private val telemetry = mockk<TelemetryRepository>(relaxed = true)
    private val homePreferences = mockk<HomePreferencesRepository>(relaxed = true) {
        every { preferences } returns flowOf(HomePreferences())
    }
    private val mysteryCache = mockk<MysteryQuestCache>(relaxed = true) {
        every { getFresh(any(), any()) } returns null
        every { getLatest(any(), any()) } returns null
    }
    /** Drop-off data for the smart hint. Default: one abandon of q1 after step 1. */
    private val analyticsRepository = mockk<AnalyticsRepository> {
        coEvery { questDropoff() } returns Result.success(
            QuestDropoffReport(listOf(DropoffPoint("q1", "Quest q1", 2, 1, "Step 1", 1)))
        )
    }
    private val location = UserLocation(LocationProvider.BOGOTA_CENTER, UserLocation.Source.FALLBACK)
    private val locationProvider = mockk<LocationProvider> { coEvery { currentLocation() } returns location }

    /** GPS for the Active Quest tests: the test pushes readings into [gps]. */
    private val gps = MutableSharedFlow<UserLocation>(extraBufferCapacity = 8)
    private val trackerLocation = mockk<LocationProvider> {
        coEvery { currentLocation() } returns UserLocation(LocationProvider.BOGOTA_CENTER, UserLocation.Source.FALLBACK)
        every { locationUpdates(any(), any()) } returns gps
    }

    private fun home(nearby: Resource<List<com.kotlin.wandr.domain.model.Quest>>): HomeViewModel {
        every { questRepository.nearbyQuests(any(), any(), any()) } returns flowOf(Resource.Loading, nearby)
        coEvery { questRepository.recommendedQuests(any(), any(), any()) } returns Result.success(emptyList())
        return HomeViewModel(questRepository, locationProvider, homePreferences, telemetry, mysteryCache, FakeConnectivity())
    }

    @Test
    fun `home lists the nearby quests and the available tags`() {
        val viewModel = home(Resource.Success(listOf(quest("q1", "music"), quest("q2", "food", "outdoors"))))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.quests.size)
        assertEquals(listOf("food", "music", "outdoors"), state.availableTags)
        assertEquals(UserLocation.Source.FALLBACK, state.locationSource)
    }

    @Test
    fun `home records the load time once, when the first result arrives`() {
        home(Resource.Success(listOf(quest("q1"))))

        verify(exactly = 1) {
            telemetry.record(TelemetryEvents.QUEST_RECOMMENDATIONS_LOAD, any(), true, any())
        }
    }

    @Test
    fun `skipping the mystery quest never repeats the current one in a row`() {
        fun dto(id: String) = RecommendedQuestDto(id, "Quest $id", null, 1, 30, 50, null, "p-$id", "Place $id", 1.0, true)
        val viewModel = home(Resource.Success(listOf(quest("q1"))))
        coEvery { questRepository.recommendedQuests(any(), any(), any()) } returns
            Result.success(listOf(dto("a"), dto("b"), dto("c")))
        viewModel.refresh()

        repeat(6) {
            val before = viewModel.uiState.value.mysteryQuest!!.quest.id
            viewModel.skipMysteryQuest()
            assertTrue(viewModel.uiState.value.mysteryQuest!!.quest.id != before)
        }
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

    private val photoStep = objective("o2", "q1", 2, requiresPhoto = true)
    private val active = activeQuest("q1", listOf(objective("o1", "q1", 1), photoStep), done = setOf("o1"))

    private fun tracker(): ActiveQuestViewModel {
        every { questRepository.activeQuests(any()) } returns MutableStateFlow(Resource.Success(listOf(active)))
        return ActiveQuestViewModel(questRepository, storageRepository, analyticsRepository, trackerLocation)
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

    // ---------- Smart feature: drop-off hint ----------

    @Test
    fun `the tracker knows the step where most people give up`() {
        val viewModel = tracker()

        val risk = viewModel.uiState.value.riskiestStepFor("q1")!!
        assertEquals(2, risk.stepOrderIndex)
        assertEquals(1, risk.abandons)
        assertEquals(null, viewModel.uiState.value.riskiestStepFor("other-quest"))
    }

    @Test
    fun `without drop-off data there is no hint and no error`() {
        coEvery { analyticsRepository.questDropoff() } returns Result.failure(AppException(AppError.Network))

        val viewModel = tracker()

        assertEquals(null, viewModel.uiState.value.riskiestStepFor("q1"))
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    // ---------- Context-aware: live distance ----------

    private val monserrate = GeoPoint(4.6058, -74.0565)

    @Test
    fun `the live distance follows the GPS and detects the arrival`() {
        val viewModel = tracker()
        viewModel.trackDistanceTo(monserrate)

        // First fix is FALLBACK (no real position): the distance stays unknown, and the screen says why
        assertEquals(null, viewModel.uiState.value.liveDistance)
        assertEquals(LocationStatus.NO_FIX, viewModel.uiState.value.locationStatus)

        gps.tryEmit(UserLocation(GeoPoint(4.6097, -74.0817), UserLocation.Source.CURRENT))
        assertEquals(LocationStatus.LIVE, viewModel.uiState.value.locationStatus)
        val far = viewModel.uiState.value.liveDistance!!
        assertTrue(far.meters > 2_000)
        assertFalse(far.hasArrived)

        gps.tryEmit(UserLocation(GeoPoint(4.6059, -74.0566), UserLocation.Source.CURRENT))
        assertTrue(viewModel.uiState.value.liveDistance!!.hasArrived)
    }

    @Test
    fun `a remembered position is marked as approximate and stopping freezes the distance`() {
        val viewModel = tracker()
        viewModel.trackDistanceTo(monserrate)

        gps.tryEmit(UserLocation(GeoPoint(4.6097, -74.0817), UserLocation.Source.LAST_KNOWN))
        assertTrue(viewModel.uiState.value.liveDistance!!.isApproximate)

        viewModel.stopTrackingDistance()
        assertEquals(LocationStatus.OFF, viewModel.uiState.value.locationStatus)
        val before = viewModel.uiState.value.liveDistance
        gps.tryEmit(UserLocation(GeoPoint(4.6059, -74.0566), UserLocation.Source.CURRENT))
        assertEquals(before, viewModel.uiState.value.liveDistance)
    }
}
