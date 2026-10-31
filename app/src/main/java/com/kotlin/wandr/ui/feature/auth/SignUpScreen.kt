package com.kotlin.wandr.ui.feature.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.kotlin.wandr.ui.components.WandrCard
import com.kotlin.wandr.ui.components.WandrTextField
import com.kotlin.wandr.ui.theme.WandrTheme

@Composable
fun SignUpRoute(
    onSignedUp: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSignedUp) {
        if (state.isSignedUp) onSignedUp()
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
                maxLength = AuthValidation.MAX_NAME,
                error = state.nameError,
                enabled = !state.isLoading,
            )
            WandrTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                maxLength = AuthValidation.MAX_EMAIL,
                error = state.emailError,
                enabled = !state.isLoading,
            )
            PasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                supportingText = "At least ${AuthValidation.MIN_PASSWORD} characters",
                maxLength = AuthValidation.MAX_PASSWORD,
                error = state.passwordError,
                imeAction = ImeAction.Done,
                onImeAction = submit,
                enabled = !state.isLoading,
            )
            PrimaryButton(
                text = "Sign up",
                onClick = submit,
                isLoading = state.isLoading,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
            if (state.emailTaken) EmailTakenCard(email = state.email, onGoToLogin = onGoToLogin)
        },
        footer = {
            LinkButton(text = "Already have an account? Log in", onClick = onGoToLogin, enabled = !state.isLoading)
        },
    )
}

@Composable
private fun EmailTakenCard(email: String, onGoToLogin: () -> Unit) {
    WandrCard(
        containerColor = WandrTheme.colors.successContainer,
        borderColor = WandrTheme.colors.successBorder,
    ) {
        Text("You're already a Wandr explorer!", style = MaterialTheme.typography.titleSmall)
        Text(
            "${email.trim()} already has an account. Log in to pick up your quests where you left them.",
            style = MaterialTheme.typography.bodySmall,
            color = WandrTheme.colors.textSecondary,
        )
        LinkButton(text = "Log in instead", onClick = onGoToLogin)
    }
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
