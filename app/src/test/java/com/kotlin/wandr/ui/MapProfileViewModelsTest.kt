package com.kotlin.wandr.ui

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.data.repository.PlaceRepository
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.Place
import com.kotlin.wandr.domain.model.PlaceCategory
import com.kotlin.wandr.domain.model.Tier
import com.kotlin.wandr.domain.model.UserProfile
import com.kotlin.wandr.testutil.FakeConnectivity
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.testutil.quest
import com.kotlin.wandr.ui.components.map.MapMarker
import com.kotlin.wandr.ui.feature.map.MapViewModel
import com.kotlin.wandr.ui.feature.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapProfileViewModelsTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val bus = AppEventBus()
    private val location = UserLocation(LocationProvider.BOGOTA_CENTER, UserLocation.Source.CURRENT)
    private val locationProvider = mockk<LocationProvider> { coEvery { currentLocation() } returns location }

    private val museum = Place("place-q1", "Museo del Oro", PlaceCategory.MUSEUM, null, GeoPoint(4.6, -74.07), 4.8, null, 1.2)

    private fun mapViewModel(
        quests: QuestRepository,
        places: List<Place> = listOf(museum),
        connectivity: FakeConnectivity = FakeConnectivity(),
    ): MapViewModel {
        val placeRepository = mockk<PlaceRepository>()
        every { placeRepository.nearbyPlaces(any(), any(), any(), any()) } returns flowOf(Resource.Success(places))
        return MapViewModel(quests, placeRepository, locationProvider, connectivity)
    }

    @Test
    fun `map reloads when the connection comes back`() {
        val repository = mockk<QuestRepository>()
        every { repository.nearbyQuests(any(), any(), any()) } returns flowOf(Resource.Success(listOf(quest("q1"))))
        val connectivity = FakeConnectivity(online = false)

        mapViewModel(repository, connectivity = connectivity)
        connectivity.status.value = true

        verify(exactly = 2) { repository.nearbyQuests(any(), any(), any()) }
    }

    @Test
    fun `map reloads quests when the radius changes`() {
        val repository = mockk<QuestRepository>()
        every { repository.nearbyQuests(any(), any(), any()) } returns flowOf(Resource.Success(listOf(quest("q1"))))

        val viewModel = mapViewModel(repository)
        viewModel.setRadius(10.0)

        verify { repository.nearbyQuests(LocationProvider.BOGOTA_CENTER, 5.0, any()) }
        verify { repository.nearbyQuests(LocationProvider.BOGOTA_CENTER, 10.0, any()) }
        assertEquals(10.0, viewModel.uiState.value.radiusKm, 0.0)
        assertEquals(location, viewModel.uiState.value.userLocation)
    }

    @Test
    fun `map pins each quest on its place`() {
        val repository = mockk<QuestRepository>()
        val quests = listOf(quest("q1"), quest("q2"))
        every { repository.nearbyQuests(any(), any(), any()) } returns flowOf(Resource.Success(quests))

        val state = mapViewModel(repository).uiState.value

        assertEquals(quests, state.quests)
        assertEquals(listOf(MapMarker("q1", museum.location, "Quest q1", "Museo del Oro")), state.markers)
        assertFalse(state.isLoading)
    }

    @Test
    fun `profile celebrates a quest completed anywhere in the app`() = runTest {
        val profileRepository = mockk<ProfileRepository>()
        val profile = UserProfile("u1", "Valentina", "v@e.com", null, 1250, 8, 5, Tier.TRAILBLAZER, null)
        every { profileRepository.observeProfile(any()) } returns flowOf(Resource.Success(profile))
        every { profileRepository.observeBadges(any()) } returns flowOf(Resource.Success(emptyList()))
        every { profileRepository.observeQuestHistory(any()) } returns flowOf(Resource.Success(emptyList()))
        coEvery { profileRepository.streakSummary() } returns Result.failure(AppException(AppError.Network))

        val viewModel = ProfileViewModel(profileRepository, mockk<AuthRepository>(), bus)
        assertEquals(profile, viewModel.uiState.value.profile)
        assertFalse(viewModel.uiState.value.isLoading)

        val completed = AppEvent.QuestCompleted("q1", 150, 8, Tier.TRAILBLAZER, 6, emptyList())
        bus.publish(completed)
        assertEquals(completed, viewModel.uiState.value.celebration)

        viewModel.onCelebrationShown()
        assertEquals(null, viewModel.uiState.value.celebration)
    }
}
