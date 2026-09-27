package com.kotlin.wandr.ui.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.data.repository.TagRepository
import com.kotlin.wandr.domain.model.EnergyLevel
import com.kotlin.wandr.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val tags: List<Tag> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val energyLevel: EnergyLevel? = null,
    val isLoadingTags: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
) {
    val canSubmit: Boolean get() = selectedTagIds.isNotEmpty() && energyLevel != null && !isSaving
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        tagRepository.observeTags()
            .onEach { resource ->
                _uiState.update { state ->
                    when (resource) {
                        Resource.Loading -> state.copy(isLoadingTags = true)
                        is Resource.Success -> state.copy(isLoadingTags = false, tags = resource.data)
                        is Resource.Error -> state.copy(isLoadingTags = false, errorMessage = resource.error.message)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleTag(tagId: String) = _uiState.update { state ->
        val selected = if (tagId in state.selectedTagIds) state.selectedTagIds - tagId else state.selectedTagIds + tagId
        state.copy(selectedTagIds = selected)
    }

    fun selectEnergyLevel(level: EnergyLevel) = _uiState.update { it.copy(energyLevel = level) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        val energy = state.energyLevel
        if (!state.canSubmit || energy == null) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = tagRepository.saveInterests(state.selectedTagIds)
                .mapCatching { profileRepository.updateEnergyLevel(energy).getOrThrow() }
            _uiState.update {
                result.fold(
                    onSuccess = { _ -> it.copy(isSaving = false, isCompleted = true) },
                    onFailure = { error -> it.copy(isSaving = false, errorMessage = error.appError.message) },
                )
            }
        }
    }
}
