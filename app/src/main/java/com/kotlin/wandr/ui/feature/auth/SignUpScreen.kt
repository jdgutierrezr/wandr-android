package com.kotlin.wandr.ui.feature.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.ui.components.AuthLayout
import com.kotlin.wandr.ui.components.LinkButton
import com.kotlin.wandr.ui.components.PasswordTextField
import com.kotlin.wandr.ui.components.PrimaryButton
import com.kotlin.wandr.ui.components.WandrTextField
import com.kotlin.wandr.ui.theme.WandrTheme

@Composable
fun SignUpRoute(
    onSignedUp: (AfterAuthDestination) -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.destination) {
        state.destination?.let(onSignedUp)
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    SignUpScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onGoToLogin = onGoToLogin,
    )
}

@Composable
fun SignUpScreen(
    state: SignUpUiState,
    snackbarHostState: SnackbarHostState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSubmit()
    }

    AuthLayout(
        eyebrow = "Join Wandr",
        title = "Create your account",
        subtitle = "Discover new plans around you, every day",
        snackbarHostState = snackbarHostState,
        form = {
            WandrTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = "Name",
                leadingIcon = Icons.Outlined.Person,
                enabled = !state.isLoading,
            )
            WandrTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                enabled = !state.isLoading,
            )
            PasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                supportingText = "At least 6 characters",
                imeAction = ImeAction.Done,
                onImeAction = submit,
                enabled = !state.isLoading,
            )
            // Validation (valid email, 6+ characters) lives in the ViewModel and comes back as errorMessage
            PrimaryButton(
                text = "Sign up",
                onClick = submit,
                isLoading = state.isLoading,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        },
        footer = {
            LinkButton(text = "Already have an account? Log in", onClick = onGoToLogin, enabled = !state.isLoading)
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    WandrTheme {
        SignUpScreen(
            state = SignUpUiState(name = "Ana", email = "ana@example.com"),
            snackbarHostState = SnackbarHostState(),
            onNameChange = {}, onEmailChange = {}, onPasswordChange = {}, onSubmit = {}, onGoToLogin = {},
        )
    }
}
