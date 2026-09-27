package com.kotlin.wandr.ui.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.EventRepository
import com.kotlin.wandr.domain.model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    val isShowingSavedData: Boolean = false,
    /** Event being joined or left (show a spinner on its button). */
    val pendingEventId: String? = null,
    /** Shown as is, e.g. "This event is full". */
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    init {
        refreshTrigger
            .flatMapLatest { eventRepository.upcomingEvents() }
            .onEach { resource ->
                _uiState.update { state ->
                    when (resource) {
                        Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            events = resource.data,
                            isShowingSavedData = resource.isStale,
                        )
                        is Resource.Error -> state.copy(isLoading = false, errorMessage = resource.error.message)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() = refreshTrigger.update { it + 1 }

    fun join(eventId: String) = runAction(eventId) { eventRepository.join(eventId).map { } }

    fun leave(eventId: String) = runAction(eventId) { eventRepository.leave(eventId) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    /** The list updates by itself: the repository writes the change into Room. */
    private fun runAction(eventId: String, action: suspend () -> Result<Unit>) {
        if (_uiState.value.pendingEventId != null) return
        _uiState.update { it.copy(pendingEventId = eventId, errorMessage = null) }
        viewModelScope.launch {
            val result = action()
            _uiState.update {
                it.copy(pendingEventId = null, errorMessage = result.exceptionOrNull()?.appError?.message)
            }
        }
    }
}
