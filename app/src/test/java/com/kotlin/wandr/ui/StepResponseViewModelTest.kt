package com.kotlin.wandr.ui

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.domain.model.StepResponseTime
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.ui.feature.analytics.StepResponseViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StepResponseViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<AnalyticsRepository>()
    private val slow = StepResponseTime("q1", "Quest q1", "o3", "Take a photo", 3, true, 4, 1800, 3000)
    private val fast = StepResponseTime("q1", "Quest q1", "o1", "Start", 1, false, 6, 400, 700)

    @Test
    fun `the slowest step comes first`() {
        coEvery { repository.stepResponseTimes() } returns Result.success(listOf(slow, fast))

        val state = StepResponseViewModel(repository).uiState.value

        assertFalse(state.isLoading)
        assertEquals("o3", state.steps!!.first().objectiveId)
    }

    @Test
    fun `without connection it asks to try again`() {
        coEvery { repository.stepResponseTimes() } returns Result.failure(AppException(AppError.Network))

        val state = StepResponseViewModel(repository).uiState.value

        assertTrue(state.loadFailed)
        assertEquals(AppError.Network.message, state.errorMessage)
    }
}
