package com.kotlin.wandr.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where to go after logging in. */
enum class AfterAuthDestination { ONBOARDING, HOME }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Set once logged in; the screen navigates and never resets it. */
    val destination: AfterAuthDestination? = null,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signIn(state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, destination = destinationAfterLogin()) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.appError.message) }
                }
        }
    }

    /** Onboarding is done when the user has an energy level and at least one interest. */
    private suspend fun destinationAfterLogin(): AfterAuthDestination {
        val profile = profileRepository.refreshProfile().getOrNull()
        val interests = tagRepository.refreshInterests().getOrNull()
        // If the check fails (e.g. offline), do not block the user on onboarding
        if (profile == null || interests == null) return AfterAuthDestination.HOME
        return if (profile.energyLevel == null || interests.isEmpty()) {
            AfterAuthDestination.ONBOARDING
        } else {
            AfterAuthDestination.HOME
        }
    }
}
