package com.kotlin.wandr.ui

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.data.repository.EventRepository
import com.kotlin.wandr.data.repository.FriendRepository
import com.kotlin.wandr.data.repository.LocationRepository
import com.kotlin.wandr.data.repository.NotificationRepository
import com.kotlin.wandr.data.repository.PlaceRepository
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.domain.model.FriendOnMap
import com.kotlin.wandr.domain.model.Friendship
import com.kotlin.wandr.domain.model.FriendshipStatus
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.Notification
import com.kotlin.wandr.domain.model.NotificationType
import com.kotlin.wandr.domain.model.Place
import com.kotlin.wandr.domain.model.PlaceCategory
import com.kotlin.wandr.domain.model.RsvpResult
import com.kotlin.wandr.domain.model.Tier
import com.kotlin.wandr.domain.model.UserProfile
import com.kotlin.wandr.domain.model.UserSummary
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.testutil.event
import com.kotlin.wandr.ui.feature.events.EventsViewModel
import com.kotlin.wandr.ui.feature.friends.FriendsMapViewModel
import com.kotlin.wandr.ui.feature.friends.FriendsViewModel
import com.kotlin.wandr.ui.feature.map.MapViewModel
import com.kotlin.wandr.ui.feature.notifications.NotificationsViewModel
import com.kotlin.wandr.ui.feature.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelsTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val bus = AppEventBus()
    private val location = UserLocation(LocationProvider.BOGOTA_CENTER, UserLocation.Source.CURRENT)
    private val locationProvider = mockk<LocationProvider> { coEvery { currentLocation() } returns location }

    // ---------- Events ----------

    @Test
    fun `events show the RPC message when an event is full`() {
        val repository = mockk<EventRepository>()
        every { repository.upcomingEvents(any()) } returns flowOf(Resource.Success(listOf(event("e1", 30, 30))))
        coEvery { repository.join("e1") } returns Result.failure(AppException(AppError.Server("This event is full")))

        val viewModel = EventsViewModel(repository)
        assertTrue(viewModel.uiState.value.events.single().isFull)

        viewModel.join("e1")
        assertEquals("This event is full", viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.pendingEventId)
    }

    @Test
    fun `joining and leaving go through the repository`() {
        val repository = mockk<EventRepository>()
        every { repository.upcomingEvents(any()) } returns flowOf(Resource.Success(listOf(event("e1"))))
        coEvery { repository.join("e1") } returns Result.success(RsvpResult("e1", 1, null))
        coEvery { repository.leave("e1") } returns Result.success(Unit)

        val viewModel = EventsViewModel(repository)
        viewModel.join("e1")
        viewModel.leave("e1")

        coVerify { repository.join("e1") }
        coVerify { repository.leave("e1") }
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    // ---------- Map ----------

    @Test
    fun `map reloads places when the category changes`() {
        val repository = mockk<PlaceRepository>()
        val museum = Place("p1", "Museo del Oro", PlaceCategory.MUSEUM, null, GeoPoint(4.6, -74.07), 4.8, null, 1.2)
        every { repository.nearbyPlaces(any(), any(), any(), any()) } returns flowOf(Resource.Success(listOf(museum)))

        val viewModel = MapViewModel(repository, locationProvider)
        viewModel.selectCategory(PlaceCategory.MUSEUM)

        verify { repository.nearbyPlaces(LocationProvider.BOGOTA_CENTER, 5.0, null, any()) }
        verify { repository.nearbyPlaces(LocationProvider.BOGOTA_CENTER, 5.0, PlaceCategory.MUSEUM, any()) }
        assertEquals(listOf(museum), viewModel.uiState.value.places)
        assertEquals(location, viewModel.uiState.value.userLocation)
    }

    // ---------- Friends on map ----------

    @Test
    fun `friends map polls only while observed and shares location when broadcasting`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = mockk<LocationRepository>()
            val friend = FriendOnMap("u2", "Santiago", null, GeoPoint(4.6, -74.08), Instant.EPOCH, 0.4, "Coffee crawl")
            coEvery { repository.friendsOnMap() } returns Result.success(listOf(friend))
            coEvery { repository.shareMyLocation(any(), any()) } returns Result.success(Unit)

            val viewModel = FriendsMapViewModel(repository, locationProvider)
            coVerify(exactly = 0) { repository.friendsOnMap() }

            val screen = backgroundScope.launch { viewModel.uiState.collect { } }
            assertEquals(listOf(friend), viewModel.uiState.value.friends)

            viewModel.setBroadcasting(true)
            assertTrue(viewModel.uiState.value.isBroadcasting)

            advanceTimeBy(FriendsMapViewModel.POLL_INTERVAL_MS + 1)
            coVerify(exactly = 2) { repository.friendsOnMap() }
            // Once by the switch, once by the second poll
            coVerify(exactly = 2) { repository.shareMyLocation(LocationProvider.BOGOTA_CENTER, true) }
            screen.cancel()
        }

    // ---------- Friends ----------

    private fun friendship(id: String, status: FriendshipStatus, incoming: Boolean) =
        Friendship(id, UserSummary("u-$id", "User $id", null), status, incoming, Instant.EPOCH)

    @Test
    fun `friends are split into friends, received and sent requests`() {
        val repository = mockk<FriendRepository>()
        coEvery { repository.friendships() } returns Result.success(
            listOf(
                friendship("a", FriendshipStatus.ACCEPTED, incoming = false),
                friendship("b", FriendshipStatus.PENDING, incoming = true),
                friendship("c", FriendshipStatus.PENDING, incoming = false),
                friendship("d", FriendshipStatus.BLOCKED, incoming = true),
            )
        )
        coEvery { repository.searchUsers(any()) } returns Result.success(emptyList())

        val state = FriendsViewModel(repository, bus).uiState.value
        assertEquals(listOf("a"), state.friends.map { it.id })
        assertEquals(listOf("b"), state.incomingRequests.map { it.id })
        assertEquals(listOf("c"), state.sentRequests.map { it.id })
        assertEquals(listOf("d"), state.blocked.map { it.id })
    }

    @Test
    fun `friends reload when a friendship changes anywhere`() = runTest {
        val repository = mockk<FriendRepository>()
        coEvery { repository.friendships() } returns Result.success(emptyList())
        coEvery { repository.searchUsers(any()) } returns Result.success(emptyList())

        FriendsViewModel(repository, bus)
        bus.publish(AppEvent.FriendshipsChanged)
        coVerify(exactly = 2) { repository.friendships() }
    }

    // ---------- Notifications ----------

    @Test
    fun `marking as read is undone if the backend fails`() {
        val repository = mockk<NotificationRepository>()
        val notification = Notification("n1", NotificationType.FRIEND_REQUEST, "Santiago sent you a request", false, Instant.EPOCH)
        coEvery { repository.notifications() } returns Result.success(listOf(notification))
        coEvery { repository.markAsRead("n1") } returns Result.failure(AppException(AppError.Network))

        val viewModel = NotificationsViewModel(repository)
        assertEquals(1, viewModel.uiState.value.unreadCount)

        viewModel.markAsRead("n1")
        assertEquals(1, viewModel.uiState.value.unreadCount)
        assertEquals(AppError.Network.message, viewModel.uiState.value.errorMessage)
    }

    // ---------- Profile ----------

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
