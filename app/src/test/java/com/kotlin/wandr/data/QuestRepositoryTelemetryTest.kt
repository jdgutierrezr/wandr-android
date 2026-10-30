package com.kotlin.wandr.data

import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.core.telemetry.TelemetryEvents
import com.kotlin.wandr.data.local.dao.PlaceDao
import com.kotlin.wandr.data.local.dao.QuestDao
import com.kotlin.wandr.data.remote.datasource.QuestRemoteDataSource
import com.kotlin.wandr.data.remote.dto.ObjectiveResultDto
import com.kotlin.wandr.data.repository.QuestRepositoryImpl
import com.kotlin.wandr.data.repository.TelemetryRepository
import com.kotlin.wandr.testutil.FakeConnectivity
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** BQ2: every complete_objective call is measured. */
class QuestRepositoryTelemetryTest {

    private val remote = mockk<QuestRemoteDataSource> {
        coEvery { activeCompletions() } returns emptyList()
    }
    private val telemetry = mockk<TelemetryRepository>(relaxed = true)
    private val repository = QuestRepositoryImpl(
        remote = remote,
        questDao = mockk<QuestDao>(relaxed = true),
        placeDao = mockk<PlaceDao>(relaxed = true),
        strategies = StrategySelector(FakeConnectivity()),
        eventBus = AppEventBus(),
        telemetry = telemetry,
    )

    @Test
    fun `a checked step is saved as quest_step_response with its ids`() = runTest {
        coEvery { remote.completeObjective("q1", "o2", "user/q1/o2.jpg") } returns ObjectiveResultDto(2, 3, false)

        repository.completeObjective("q1", "o2", "user/q1/o2.jpg")

        verify {
            telemetry.record(
                name = TelemetryEvents.QUEST_STEP_RESPONSE,
                durationMs = any(),
                success = true,
                metadata = mapOf("quest_id" to "q1", "objective_id" to "o2", "with_photo" to "true"),
            )
        }
    }

    @Test
    fun `a failed call is saved as not successful and still fails`() = runTest {
        coEvery { remote.completeObjective("q1", "o1", null) } throws IOException("no network")

        val result = repository.completeObjective("q1", "o1", null)

        assertTrue(result.isFailure)
        verify {
            telemetry.record(
                name = TelemetryEvents.QUEST_STEP_RESPONSE,
                durationMs = any(),
                success = false,
                metadata = mapOf("quest_id" to "q1", "objective_id" to "o1", "with_photo" to "false"),
            )
        }
    }
}
