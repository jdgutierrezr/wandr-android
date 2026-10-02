package com.kotlin.wandr.ui

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.domain.model.DropoffPoint
import com.kotlin.wandr.domain.model.QuestDropoffReport
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.ui.feature.analytics.QuestDropoffViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuestDropoffViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<AnalyticsRepository>()
    private val report = QuestDropoffReport(listOf(DropoffPoint("q1", "Quest q1", 3, 1, "Step 1", 2)))

    @Test
    fun `the funnel loads when the screen opens`() {
        coEvery { repository.questDropoff() } returns Result.success(report)

        val state = QuestDropoffViewModel(repository).uiState.value

        assertFalse(state.isLoading)
        assertEquals(report, state.report)
        assertEquals("q1", state.report!!.worstPoint!!.questId)
    }

    @Test
    fun `without connection it asks to try again, and a retry loads it`() {
        coEvery { repository.questDropoff() } returns Result.failure(AppException(AppError.Network))
        val viewModel = QuestDropoffViewModel(repository)
        assertTrue(viewModel.uiState.value.loadFailed)
        assertEquals(AppError.Network.message, viewModel.uiState.value.errorMessage)

        coEvery { repository.questDropoff() } returns Result.success(report)
        viewModel.load()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(report, viewModel.uiState.value.report)
    }

    @Test
    fun `a failed refresh keeps the report already shown`() {
        coEvery { repository.questDropoff() } returns Result.success(report)
        val viewModel = QuestDropoffViewModel(repository)

        coEvery { repository.questDropoff() } returns Result.failure(AppException(AppError.Network))
        viewModel.load()

        assertEquals(report, viewModel.uiState.value.report)
        assertFalse(viewModel.uiState.value.loadFailed)
    }
}
