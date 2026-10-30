package com.kotlin.wandr.ui.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.domain.model.RecommendationLoadTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoadTimeUiState(
    val isLoading: Boolean = true,
    val report: RecommendationLoadTime? = null,
    val loadFailed: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoadTimeViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoadTimeUiState())
    val uiState: StateFlow<LoadTimeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        loadJob = viewModelScope.launch {
            analyticsRepository.recommendationLoadTime()
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
