package com.kotlin.wandr.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** A new account always goes through onboarding. */
    val destination: AfterAuthDestination? = null,
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        validate(state)?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signUp(state.name, state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, destination = AfterAuthDestination.ONBOARDING) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.appError.message) }
                }
        }
    }

    private fun validate(state: SignUpUiState): String? = when {
        state.name.isBlank() -> "Enter your name"
        !EMAIL_REGEX.matches(state.email.trim()) -> "Enter a valid email"
        state.password.length < MIN_PASSWORD_LENGTH -> "The password needs at least $MIN_PASSWORD_LENGTH characters"
        else -> null
    }

    private companion object {
        /** Supabase Auth's default minimum. */
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
