package com.kotlin.wandr.ui

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.testutil.FakeConnectivity
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.ui.feature.auth.LoginViewModel
import com.kotlin.wandr.ui.feature.auth.SignUpViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelsTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()

    private fun login(): LoginViewModel = LoginViewModel(authRepository, FakeConnectivity()).apply {
        onEmailChange("valentina.gomez@example.com")
        onPasswordChange("password123")
    }

    @Test
    fun `login goes home`() {
        coEvery { authRepository.signIn(any(), any()) } returns Result.success(Unit)

        val viewModel = login()
        viewModel.submit()

        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `wrong password shows the auth message`() {
        coEvery { authRepository.signIn(any(), any()) } returns
            Result.failure(AppException(AppError.Server("Invalid login credentials")))

        val viewModel = login()
        viewModel.submit()

        assertEquals("Invalid login credentials", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoggedIn)
    }

    @Test
    fun `login cannot be sent with empty fields`() {
        val viewModel = LoginViewModel(authRepository, FakeConnectivity())
        viewModel.submit()
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `sign up validates before calling the backend`() {
        val viewModel = SignUpViewModel(authRepository, FakeConnectivity())
        viewModel.onNameChange("Ana")
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("password123")
        viewModel.submit()

        assertEquals("Enter a valid email", viewModel.uiState.value.emailError)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `sign up shows every field error at once`() {
        val viewModel = SignUpViewModel(authRepository, FakeConnectivity())
        viewModel.onPasswordChange("123")
        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals("Enter your name", state.nameError)
        assertEquals("Enter your email", state.emailError)
        assertEquals("The password needs at least 6 characters", state.passwordError)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `editing a field clears its error`() {
        val viewModel = SignUpViewModel(authRepository, FakeConnectivity())
        viewModel.submit()
        assertEquals("Enter your name", viewModel.uiState.value.nameError)

        viewModel.onNameChange("Ana")

        assertEquals(null, viewModel.uiState.value.nameError)
        assertEquals("Enter your email", viewModel.uiState.value.emailError)
    }

    @Test
    fun `login rejects an email with a wrong format`() {
        val viewModel = LoginViewModel(authRepository, FakeConnectivity()).apply {
            onEmailChange("not-an-email")
            onPasswordChange("whatever")
        }
        viewModel.submit()

        assertEquals("Enter a valid email", viewModel.uiState.value.emailError)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `new accounts go home`() {
        coEvery { authRepository.signUp("Ana", "ana@example.com", "secret1") } returns Result.success(Unit)
        val viewModel = SignUpViewModel(authRepository, FakeConnectivity())
        viewModel.onNameChange("Ana")
        viewModel.onEmailChange("ana@example.com")
        viewModel.onPasswordChange("secret1")
        viewModel.submit()

        assertTrue(viewModel.uiState.value.isSignedUp)
    }

    @Test
    fun `login without internet shows the offline message and does not call the backend`() {
        val viewModel = LoginViewModel(authRepository, FakeConnectivity(online = false)).apply {
            onEmailChange("valentina.gomez@example.com")
            onPasswordChange("password123")
        }
        viewModel.submit()

        assertEquals(AppError.Network.message, viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `sign up without internet shows the offline message and does not call the backend`() {
        val viewModel = SignUpViewModel(authRepository, FakeConnectivity(online = false))
        viewModel.onNameChange("Ana")
        viewModel.onEmailChange("ana@example.com")
        viewModel.onPasswordChange("secret1")
        viewModel.submit()

        assertEquals(AppError.Network.message, viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }
}
