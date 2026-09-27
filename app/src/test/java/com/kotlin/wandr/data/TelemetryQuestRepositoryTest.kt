package com.kotlin.wandr.data

import app.cash.turbine.test
import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.telemetry.TelemetryEvents
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.data.repository.TelemetryQuestRepository
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.testutil.quest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class TelemetryQuestRepositoryTest {

    private val inner = mockk<QuestRepository>()
    private val bus = AppEventBus()
    private val clock = TestTimeSource()
    private val repository = TelemetryQuestRepository(inner, bus, clock)
    private val bogota = GeoPoint(4.6, -74.08)

    @Test
    fun `measures the time until quest recommendations arrive (BQ1)`() = runTest {
        every { inner.nearbyQuests(bogota, 5.0, FetchPolicy.NETWORK_FIRST) } returns flow {
            emit(Resource.Loading)
            clock += 420.milliseconds
            emit(Resource.Success(listOf(quest("q1"), quest("q2"))))
        }

        bus.subscribe<AppEvent.RequestTimed>().test {
            repository.nearbyQuests(bogota, 5.0, FetchPolicy.NETWORK_FIRST).test {
                assertEquals(Resource.Loading, awaitItem())
                assertEquals(2, (awaitItem() as Resource.Success).data.size)
                awaitComplete()
            }
            val timed = awaitItem()
            assertEquals(TelemetryEvents.QUEST_RECOMMENDATIONS_LOAD, timed.name)
            assertEquals(420L, timed.durationMs)
            assertTrue(timed.success)
            assertEquals("2", timed.metadata["result_count"])
            assertEquals("false", timed.metadata["from_cache"])
        }
    }

    @Test
    fun `measures each quest step and reports failures too (BQ2)`() = runTest {
        coEvery { inner.completeObjective("q1", "o1", null) } coAnswers {
            clock += 250.milliseconds
            Result.failure(AppException(AppError.Server("Quest is not in progress")))
        }

        bus.subscribe<AppEvent.RequestTimed>().test {
            val result = repository.completeObjective("q1", "o1", null)
            assertTrue(result.isFailure)

            val timed = awaitItem()
            assertEquals(TelemetryEvents.QUEST_STEP_RESPONSE, timed.name)
            assertEquals(250L, timed.durationMs)
            assertFalse(timed.success)
            assertEquals("o1", timed.metadata["objective_id"])
            assertEquals("q1", timed.metadata["quest_id"])
        }
    }

    @Test
    fun `returns the real result untouched`() = runTest {
        val expected = ObjectiveResult(1, 3, false, 0, null, null, null, null, emptyList())
        coEvery { inner.completeObjective("q1", "o1", "path.jpg") } returns Result.success(expected)
        assertEquals(expected, repository.completeObjective("q1", "o1", "path.jpg").getOrThrow())
    }

    @Test
    fun `methods without metrics go straight to the real repository`() = runTest {
        coEvery { inner.startQuest("q1") } returns Result.success(Unit)
        repository.startQuest("q1")
        coVerify(exactly = 1) { inner.startQuest("q1") }
    }
}
