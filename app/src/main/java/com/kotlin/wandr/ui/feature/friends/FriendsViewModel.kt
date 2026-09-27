package com.kotlin.wandr.ui.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.data.repository.FriendRepository
import com.kotlin.wandr.domain.model.Friendship
import com.kotlin.wandr.domain.model.FriendshipStatus
import com.kotlin.wandr.domain.model.UserSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val isLoading: Boolean = true,
    val friendships: List<Friendship> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<UserSummary> = emptyList(),
    /** Friendship or user being changed (show a spinner on it). */
    val pendingId: String? = null,
    val errorMessage: String? = null,
) {
    val friends: List<Friendship> get() = friendships.filter { it.status == FriendshipStatus.ACCEPTED }
    val incomingRequests: List<Friendship> get() = friendships.filter { it.canAccept }
    val sentRequests: List<Friendship> get() =
        friendships.filter { it.status == FriendshipStatus.PENDING && !it.isIncoming }
    val blocked: List<Friendship> get() = friendships.filter { it.status == FriendshipStatus.BLOCKED }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    eventBus: AppEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        load()

        // Any change (here or elsewhere) reloads the list
        eventBus.subscribe<AppEvent.FriendshipsChanged>()
            .onEach { load() }
            .launchIn(viewModelScope)

        searchQuery
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { query ->
                val result = friendRepository.searchUsers(query)
                _uiState.update {
                    it.copy(
                        searchResults = result.getOrDefault(emptyList()),
                        errorMessage = result.exceptionOrNull()?.appError?.message ?: it.errorMessage,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            val result = friendRepository.friendships()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    friendships = result.getOrDefault(it.friendships),
                    errorMessage = result.exceptionOrNull()?.appError?.message ?: it.errorMessage,
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQuery.value = query
    }

    fun sendRequest(userId: String) = runAction(userId) { friendRepository.sendRequest(userId) }

    fun accept(friendshipId: String) = runAction(friendshipId) { friendRepository.accept(friendshipId) }

    fun block(friendshipId: String) = runAction(friendshipId) { friendRepository.block(friendshipId) }

    /** Remove a friend, decline a received request or cancel a sent one. */
    fun remove(friendshipId: String) = runAction(friendshipId) { friendRepository.remove(friendshipId) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    private fun runAction(id: String, action: suspend () -> Result<Unit>) {
        if (_uiState.value.pendingId != null) return
        _uiState.update { it.copy(pendingId = id, errorMessage = null) }
        viewModelScope.launch {
            val result = action()
            _uiState.update {
                it.copy(pendingId = null, errorMessage = result.exceptionOrNull()?.appError?.message)
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
