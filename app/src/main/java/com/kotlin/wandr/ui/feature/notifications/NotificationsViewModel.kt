package com.kotlin.wandr.ui.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.data.repository.NotificationRepository
import com.kotlin.wandr.domain.model.Notification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<Notification> = emptyList(),
    val errorMessage: String? = null,
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = notificationRepository.notifications()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    notifications = result.getOrDefault(it.notifications),
                    errorMessage = result.exceptionOrNull()?.appError?.message,
                )
            }
        }
    }

    /** Optimistic: marked at once, undone if the backend fails. */
    fun markAsRead(notificationId: String) {
        setRead(notificationId, true)
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId).onFailure { error ->
                setRead(notificationId, false)
                _uiState.update { it.copy(errorMessage = error.appError.message) }
            }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    private fun setRead(notificationId: String, isRead: Boolean) = _uiState.update { state ->
        state.copy(notifications = state.notifications.map { if (it.id == notificationId) it.copy(isRead = isRead) else it })
    }
}
