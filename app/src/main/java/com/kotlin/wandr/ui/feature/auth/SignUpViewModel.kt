package com.kotlin.wandr.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.network.ConnectivityObserver
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
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignedUp: Boolean = false,
    val emailTaken: Boolean = false,
) {
    val hasFieldErrors: Boolean get() = nameError != null || emailError != null || passwordError != null
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val connectivity: ConnectivityObserver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null, emailTaken = false) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, errorMessage = null) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        val checked = state.copy(
            nameError = AuthValidation.nameError(state.name),
            emailError = AuthValidation.emailError(state.email),
            passwordError = AuthValidation.passwordError(state.password),
        )
        if (checked.hasFieldErrors) {
            _uiState.update {
                it.copy(
                    nameError = checked.nameError,
                    emailError = checked.emailError,
                    passwordError = checked.passwordError,
                )
            }
            return
        }
        if (!connectivity.isOnline()) {
            _uiState.update { it.copy(errorMessage = AppError.Network.message) }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signUp(state.name, state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSignedUp = true) }
                }
                .onFailure { error ->
                    if (error.appError == AppError.EmailTaken) {
                        _uiState.update {
                            it.copy(isLoading = false, emailTaken = true, emailError = AppError.EmailTaken.message)
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = error.appError.message) }
                    }
                }
        }
    }
}
