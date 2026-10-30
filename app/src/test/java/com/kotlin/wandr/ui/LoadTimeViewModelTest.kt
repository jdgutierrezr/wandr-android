package com.kotlin.wandr.ui

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.domain.model.RecommendationLoadTime
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.ui.feature.analytics.LoadTimeViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoadTimeViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<AnalyticsRepository>()
    private val report = RecommendationLoadTime(requests = 10, avgMs = 800, p95Ms = 2000, networkAvgMs = 1000, cacheAvgMs = 100)

    @Test
    fun `the report loads when the screen opens`() {
        coEvery { repository.recommendationLoadTime() } returns Result.success(report)

        val state = LoadTimeViewModel(repository).uiState.value

        assertFalse(state.isLoading)
        assertEquals(report, state.report)
    }

    @Test
    fun `without connection it asks to try again`() {
        coEvery { repository.recommendationLoadTime() } returns Result.failure(AppException(AppError.Network))

        val state = LoadTimeViewModel(repository).uiState.value

        assertTrue(state.loadFailed)
        assertEquals(AppError.Network.message, state.errorMessage)
    }
}
