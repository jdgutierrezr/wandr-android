package com.kotlin.wandr.ui.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.domain.model.QuestDropoffReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestDropoffUiState(
    val isLoading: Boolean = true,
    val report: QuestDropoffReport? = null,
    /** The report could not be loaded (it needs a connection): show "Try again". */
    val loadFailed: Boolean = false,
    val errorMessage: String? = null,
)

/** BQ8 · Abandonment funnel: where users give up quests. */
@HiltViewModel
class QuestDropoffViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestDropoffUiState())
    val uiState: StateFlow<QuestDropoffUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        loadJob = viewModelScope.launch {
            analyticsRepository.questDropoff()
                .onSuccess { report -> _uiState.update { it.copy(isLoading = false, report = report) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, loadFailed = it.report == null, errorMessage = error.appError.message)
                    }
                }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }
}
