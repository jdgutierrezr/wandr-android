package com.kotlin.wandr.ui

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.domain.model.Achievement
import com.kotlin.wandr.domain.model.StreakSummary
import com.kotlin.wandr.domain.model.Tier
import com.kotlin.wandr.domain.model.WeeklyQuests
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.ui.feature.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** BQ4 on the profile: the streak summary loads, fails gracefully and refreshes after a quest. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileStreakViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val bus = AppEventBus()

    private val repository = mockk<ProfileRepository> {
        every { observeProfile(any()) } returns flowOf(Resource.Loading)
        every { observeBadges(any()) } returns flowOf(Resource.Success(emptyList()))
        every { observeQuestHistory(any()) } returns flowOf(Resource.Success(emptyList()))
    }

    private fun summary(currentWeekQuests: Int, streak: Int = 3) = StreakSummary(
        questsCompleted = 10 + currentWeekQuests,
        points = 900,
        currentStreak = streak,
        thisWeek = listOf(true, false, false, false, false, false, false),
        weeklyHistory = listOf(WeeklyQuests("22 Sep", 1), WeeklyQuests("29 Sep", currentWeekQuests)),
        achievements = listOf(Achievement("b1", "First Quest", true), Achievement("b2", "Early Bird", false)),
    )

    @Test
    fun `the streak summary is loaded when the screen opens`() {
        coEvery { repository.streakSummary() } returns Result.success(summary(currentWeekQuests = 2))

        val viewModel = ProfileViewModel(repository, mockk<AuthRepository>(), bus)

        val state = viewModel.uiState.value
        assertEquals(2, state.streak!!.weekComparison!!.currentWeek)
        assertFalse(state.isStreakLoading)
        assertFalse(state.streakFailed)
    }

    @Test
    fun `without connection the error is shown and try again reloads`() {
        coEvery { repository.streakSummary() } returns Result.failure(AppException(AppError.Network))

        val viewModel = ProfileViewModel(repository, mockk<AuthRepository>(), bus)
        assertNull(viewModel.uiState.value.streak)
        assertTrue(viewModel.uiState.value.streakFailed)
        assertEquals(AppError.Network.message, viewModel.uiState.value.errorMessage)

        coEvery { repository.streakSummary() } returns Result.success(summary(currentWeekQuests = 1))
        viewModel.loadStreak()

        assertFalse(viewModel.uiState.value.streakFailed)
        assertEquals(1, viewModel.uiState.value.streak!!.weekComparison!!.currentWeek)
    }

    @Test
    fun `finishing a quest anywhere reloads the streak (Observer)`() = runTest {
        coEvery { repository.streakSummary() } returnsMany listOf(
            Result.success(summary(currentWeekQuests = 1, streak = 3)),
            Result.success(summary(currentWeekQuests = 2, streak = 4)),
        )
        val viewModel = ProfileViewModel(repository, mockk<AuthRepository>(), bus)
        assertEquals(3, viewModel.uiState.value.streak!!.currentStreak)

        bus.publish(AppEvent.QuestCompleted("q1", 150, 5, Tier.TRAILBLAZER, 4, emptyList()))

        assertEquals(4, viewModel.uiState.value.streak!!.currentStreak)
        assertEquals(2, viewModel.uiState.value.streak!!.weekComparison!!.currentWeek)
        coVerify(exactly = 2) { repository.streakSummary() }
    }
}
