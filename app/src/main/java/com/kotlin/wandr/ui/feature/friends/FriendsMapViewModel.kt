package com.kotlin.wandr.ui.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.data.repository.LocationRepository
import com.kotlin.wandr.domain.model.FriendOnMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsMapUiState(
    val isLoading: Boolean = true,
    val myLocation: UserLocation? = null,
    val friends: List<FriendOnMap> = emptyList(),
    /** The user is sharing their location with friends. */
    val isBroadcasting: Boolean = false,
    val isUpdatingBroadcast: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Friends on Quest (map). Polls `friends_on_map` every [POLL_INTERVAL_MS] only while the screen
 * is visible (`WhileSubscribed`), and sends the user's position on each tick while broadcasting.
 */
@HiltViewModel
class FriendsMapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val localState = MutableStateFlow(FriendsMapUiState())

    private val polling = flow {
        while (true) {
            val location = locationProvider.currentLocation()
            val state = localState.value
            if (state.isBroadcasting) locationRepository.shareMyLocation(location.point, isBroadcasting = true)
            val friends = locationRepository.friendsOnMap()
            localState.update {
                it.copy(
                    isLoading = false,
                    myLocation = location,
                    friends = friends.getOrDefault(it.friends),
                    errorMessage = friends.exceptionOrNull()?.appError?.message ?: it.errorMessage,
                )
            }
            emit(Unit)
            delay(POLL_INTERVAL_MS)
        }
    }

    val uiState: StateFlow<FriendsMapUiState> = combine(localState, polling) { state, _ -> state }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FriendsMapUiState())

    fun setBroadcasting(enabled: Boolean) {
        if (localState.value.isUpdatingBroadcast) return
        localState.update { it.copy(isUpdatingBroadcast = true, errorMessage = null) }
        viewModelScope.launch {
            val location = locationProvider.currentLocation()
            val result = locationRepository.shareMyLocation(location.point, isBroadcasting = enabled)
            localState.update {
                it.copy(
                    isUpdatingBroadcast = false,
                    isBroadcasting = if (result.isSuccess) enabled else it.isBroadcasting,
                    myLocation = location,
                    errorMessage = result.exceptionOrNull()?.appError?.message,
                )
            }
        }
    }

    fun onErrorShown() = localState.update { it.copy(errorMessage = null) }

    companion object {
        const val POLL_INTERVAL_MS = 15_000L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
