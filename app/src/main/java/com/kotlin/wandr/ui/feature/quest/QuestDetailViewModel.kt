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
    /** The user already has this quest in progress: show "Continue" instead of "Start". */
    val isInProgress: Boolean = false,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
    /** Set when the quest was started; the screen opens the tracker. */
    val startedQuestId: String? = null,
)

@HiltViewModel
class QuestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questRepository: QuestRepository,
) : ViewModel() {

    private val questId: String = checkNotNull(savedStateHandle[QUEST_ID_ARG]) { "Missing $QUEST_ID_ARG" }

    private val _uiState = MutableStateFlow(QuestDetailUiState())
    val uiState: StateFlow<QuestDetailUiState> = _uiState.asStateFlow()

    init {
        questRepository.questDetail(questId)
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

        // Only the cache: the tracker is the one that downloads active quests
        questRepository.activeQuests(FetchPolicy.CACHE_FIRST)
            .onEach { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { state -> state.copy(isInProgress = resource.data.any { it.questId == questId }) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun startQuest() {
        if (_uiState.value.isStarting) return
        _uiState.update { it.copy(isStarting = true, errorMessage = null) }
        viewModelScope.launch {
            questRepository.startQuest(questId)
                .onSuccess { _uiState.update { it.copy(isStarting = false, startedQuestId = questId) } }
                .onFailure { error -> _uiState.update { it.copy(isStarting = false, errorMessage = error.appError.message) } }
        }
    }

    fun onNavigatedToTracker() = _uiState.update { it.copy(startedQuestId = null) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    companion object {
        /** Navigation argument with the quest id. */
        const val QUEST_ID_ARG = "questId"
    }
}
