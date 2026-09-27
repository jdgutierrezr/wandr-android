package com.kotlin.wandr.ui

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.data.repository.TagRepository
import com.kotlin.wandr.domain.model.EnergyLevel
import com.kotlin.wandr.domain.model.Tag
import com.kotlin.wandr.domain.model.Tier
import com.kotlin.wandr.domain.model.UserProfile
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.ui.feature.auth.AfterAuthDestination
import com.kotlin.wandr.ui.feature.auth.LoginViewModel
import com.kotlin.wandr.ui.feature.auth.SignUpViewModel
import com.kotlin.wandr.ui.feature.onboarding.OnboardingViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelsTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val profileRepository = mockk<ProfileRepository>()
    private val tagRepository = mockk<TagRepository>()

    private fun profile(energy: EnergyLevel?) = UserProfile("u1", "Valentina", "v@e.com", null, 0, 1, 0, Tier.BOGOTA_SCOUT, energy)

    private fun login(): LoginViewModel = LoginViewModel(authRepository, profileRepository, tagRepository).apply {
        onEmailChange("valentina.gomez@example.com")
        onPasswordChange("password123")
    }

    // ---------- Login ----------

    @Test
    fun `login goes home when onboarding is done`() {
        coEvery { authRepository.signIn(any(), any()) } returns Result.success(Unit)
        coEvery { profileRepository.refreshProfile() } returns Result.success(profile(EnergyLevel.ACTIVE))
        coEvery { tagRepository.refreshInterests() } returns Result.success(setOf("t1"))

        val viewModel = login()
        viewModel.submit()

        assertEquals(AfterAuthDestination.HOME, viewModel.uiState.value.destination)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `login goes to onboarding when there are no interests`() {
        coEvery { authRepository.signIn(any(), any()) } returns Result.success(Unit)
        coEvery { profileRepository.refreshProfile() } returns Result.success(profile(EnergyLevel.ACTIVE))
        coEvery { tagRepository.refreshInterests() } returns Result.success(emptySet())

        val viewModel = login()
        viewModel.submit()

        assertEquals(AfterAuthDestination.ONBOARDING, viewModel.uiState.value.destination)
    }

    @Test
    fun `wrong password shows the auth message`() {
        coEvery { authRepository.signIn(any(), any()) } returns
            Result.failure(AppException(AppError.Server("Invalid login credentials")))

        val viewModel = login()
        viewModel.submit()

        assertEquals("Invalid login credentials", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.destination)
    }

    @Test
    fun `login cannot be sent with empty fields`() {
        val viewModel = LoginViewModel(authRepository, profileRepository, tagRepository)
        viewModel.submit()
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    // ---------- Sign up ----------

    @Test
    fun `sign up validates before calling the backend`() {
        val viewModel = SignUpViewModel(authRepository)
        viewModel.onNameChange("Ana")
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("password123")
        viewModel.submit()

        assertEquals("Enter a valid email", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `new accounts always go to onboarding`() {
        coEvery { authRepository.signUp("Ana", "ana@example.com", "secret1") } returns Result.success(Unit)
        val viewModel = SignUpViewModel(authRepository)
        viewModel.onNameChange("Ana")
        viewModel.onEmailChange("ana@example.com")
        viewModel.onPasswordChange("secret1")
        viewModel.submit()

        assertEquals(AfterAuthDestination.ONBOARDING, viewModel.uiState.value.destination)
    }

    // ---------- Onboarding ----------

    @Test
    fun `onboarding saves interests and energy level`() {
        every { tagRepository.observeTags(any()) } returns
            flowOf(Resource.Success(listOf(Tag("t1", "food"), Tag("t2", "music"))))
        coEvery { tagRepository.saveInterests(setOf("t2")) } returns Result.success(Unit)
        coEvery { profileRepository.updateEnergyLevel(EnergyLevel.RELAXED) } returns Result.success(Unit)

        val viewModel = OnboardingViewModel(tagRepository, profileRepository)
        assertEquals(2, viewModel.uiState.value.tags.size)
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.toggleTag("t2")
        viewModel.selectEnergyLevel(EnergyLevel.RELAXED)
        viewModel.submit()

        assertTrue(viewModel.uiState.value.isCompleted)
        coVerify { profileRepository.updateEnergyLevel(EnergyLevel.RELAXED) }
    }

    @Test
    fun `onboarding stops if saving interests fails`() {
        every { tagRepository.observeTags(any()) } returns flowOf(Resource.Success(listOf(Tag("t1", "food"))))
        coEvery { tagRepository.saveInterests(any()) } returns Result.failure(AppException(AppError.Network))

        val viewModel = OnboardingViewModel(tagRepository, profileRepository)
        viewModel.toggleTag("t1")
        viewModel.selectEnergyLevel(EnergyLevel.ACTIVE)
        viewModel.submit()

        assertFalse(viewModel.uiState.value.isCompleted)
        assertEquals(AppError.Network.message, viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { profileRepository.updateEnergyLevel(any()) }
    }
}
