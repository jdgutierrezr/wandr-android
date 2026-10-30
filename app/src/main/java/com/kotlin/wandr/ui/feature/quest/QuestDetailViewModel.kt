package com.kotlin.wandr.ui.feature.quest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.domain.model.QuestDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestDetailUiState(
    val isLoading: Boolean = true,
    val detail: QuestDetail? = null,
    val isShowingSavedData: Boolean = false,
    val isInProgress: Boolean = false,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
    val startedQuestId: String? = null,
)

@HiltViewModel
class QuestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questRepository: QuestRepository,
) : ViewModel() {

    private var questId: String? = null
    private val jobs = mutableListOf<Job>()

    private val _uiState = MutableStateFlow(QuestDetailUiState())
    val uiState: StateFlow<QuestDetailUiState> = _uiState.asStateFlow()

    init {
        savedStateHandle.get<String>(QUEST_ID_ARG)?.let(::load)
    }

    fun load(id: String) {
        if (questId == id) return
        questId = id
        jobs.forEach { it.cancel() }
        jobs.clear()
        _uiState.value = QuestDetailUiState()

        jobs += questRepository.questDetail(id)
            .onEach { resource ->
                _uiState.update { state ->
                    when (resource) {
                        Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            detail = resource.data,
                            isShowingSavedData = resource.isStale,
                        )
                        is Resource.Error -> state.copy(isLoading = false, errorMessage = resource.error.message)
                    }
                }
            }
            .launchIn(viewModelScope)

        jobs += questRepository.activeQuests(FetchPolicy.CACHE_FIRST)
            .onEach { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { state -> state.copy(isInProgress = resource.data.any { it.questId == id }) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun startQuest() {
        val id = questId ?: return
        if (_uiState.value.isStarting) return
        _uiState.update { it.copy(isStarting = true, errorMessage = null) }
        viewModelScope.launch {
            questRepository.startQuest(id)
                .onSuccess { _uiState.update { it.copy(isStarting = false, startedQuestId = id) } }
                .onFailure { error -> _uiState.update { it.copy(isStarting = false, errorMessage = error.appError.message) } }
        }
    }

    fun onNavigatedToTracker() = _uiState.update { it.copy(startedQuestId = null) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    companion object {
        const val QUEST_ID_ARG = "questId"
    }
}
