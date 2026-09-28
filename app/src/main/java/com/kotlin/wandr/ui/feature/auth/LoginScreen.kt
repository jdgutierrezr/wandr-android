package com.kotlin.wandr.ui.feature.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Email
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

/**
 * Route = the stateful part. It gets the ViewModel from Hilt, observes its state and reacts
 * to one-time things (navigate, show an error). The navigation graph only knows this function.
 */
@Composable
fun LoginRoute(
    onLoggedIn: (AfterAuthDestination) -> Unit,
    onGoToSignUp: () -> Unit,
    onOpenCatalog: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Runs again only when `destination` changes
    LaunchedEffect(state.destination) {
        state.destination?.let(onLoggedIn)
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    LoginScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onGoToSignUp = onGoToSignUp,
        onOpenCatalog = onOpenCatalog,
    )
}

/**
 * Screen = the stateless part. It only draws [state] and reports events through lambdas.
 * No ViewModel here, so it can be previewed and tested with any state.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    snackbarHostState: SnackbarHostState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoToSignUp: () -> Unit,
    onOpenCatalog: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSubmit()
    }

    AuthLayout(
        eyebrow = "Side Quests",
        title = "Welcome back",
        subtitle = "Log in to find your next quest nearby",
        snackbarHostState = snackbarHostState,
        form = {
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
                imeAction = ImeAction.Done,
                onImeAction = submit,
                enabled = !state.isLoading,
            )
            PrimaryButton(
                text = "Log in",
                onClick = submit,
                enabled = state.canSubmit,
                isLoading = state.isLoading,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        },
        footer = {
            LinkButton(
                text = "Don't have an account? Sign up",
                onClick = onGoToSignUp,
                enabled = !state.isLoading,
            )
            if (onOpenCatalog != null) {
                LinkButton(text = "Design system (debug)", onClick = onOpenCatalog)
            }
        },
    )
}

@Preview(showBackground = true, name = "Filled")
@Composable
private fun LoginScreenPreview() {
    WandrTheme {
        LoginScreen(
            state = LoginUiState(email = "valentina.gomez@example.com", password = "password123"),
            snackbarHostState = SnackbarHostState(),
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {}, onGoToSignUp = {},
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun LoginScreenLoadingPreview() {
    WandrTheme {
        LoginScreen(
            state = LoginUiState(email = "valentina.gomez@example.com", password = "password123", isLoading = true),
            snackbarHostState = SnackbarHostState(),
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {}, onGoToSignUp = {},
        )
    }
}
