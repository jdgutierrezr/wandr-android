package com.kotlin.wandr.core

import app.cash.turbine.test
import com.kotlin.wandr.core.analytics.QuestFunnelTracker
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.telemetry.TelemetryEvents
import com.kotlin.wandr.domain.model.Tier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** BQ8 · Observer: quest events become funnel rows for the telemetry. */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestFunnelTrackerTest {

    private val bus = AppEventBus()

    /** A clock the test moves by hand. */
    private class FakeClock(var now: Long = 1_000) : Clock() {
        override fun millis() = now
        override fun instant(): Instant = Instant.ofEpochMilli(now)
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
    }

    private val clock = FakeClock()

    private fun TestScope.tracker() =
        QuestFunnelTracker(eventBus = bus, clock = clock, scope = backgroundScope).also { it.start() }

    private fun completed(questId: String) = AppEvent.QuestCompleted(questId, 150, 3, Tier.BOGOTA_SCOUT, 1, emptyList())

    @Test
    fun `each funnel step becomes a telemetry row with the time since the previous step`() =
        runTest(UnconfinedTestDispatcher()) {
            tracker()
            bus.subscribe<AppEvent.RequestTimed>().test {
                bus.publish(AppEvent.QuestViewed("q1"))
                clock.now += 4_000
                bus.publish(AppEvent.QuestStarted("q1"))
                clock.now += 60_000
                bus.publish(AppEvent.NavigationStarted("q1"))
                clock.now += 1_800_000
                bus.publish(completed("q1"))

                val rows = List(4) { awaitItem() }
                assertEquals(
                    listOf(
                        TelemetryEvents.QUEST_VIEWED,
                        TelemetryEvents.QUEST_ACCEPTED,
                        TelemetryEvents.NAVIGATION_STARTED,
                        TelemetryEvents.QUEST_COMPLETED,
                    ),
                    rows.map { it.name },
                )
                assertEquals(listOf(0L, 4_000L, 60_000L, 1_800_000L), rows.map { it.durationMs })
                assertEquals(setOf("q1"), rows.map { it.metadata["quest_id"] }.toSet())
            }
        }

    @Test
    fun `abandoning closes the funnel and other events are ignored`() = runTest(UnconfinedTestDispatcher()) {
        tracker()
        bus.subscribe<AppEvent.RequestTimed>().test {
            bus.publish(AppEvent.QuestStarted("q2"))
            bus.publish(AppEvent.EventJoined("e1")) // not a quest event
            clock.now += 10_000
            bus.publish(AppEvent.QuestAbandoned("q2"))
            // Starting again begins a new funnel: no time since the abandon
            bus.publish(AppEvent.QuestStarted("q2"))

            assertEquals(TelemetryEvents.QUEST_ACCEPTED, awaitItem().name)
            val abandoned = awaitItem()
            assertEquals(TelemetryEvents.QUEST_ABANDONED, abandoned.name)
            assertEquals(10_000L, abandoned.durationMs)
            val restarted = awaitItem()
            assertEquals(TelemetryEvents.QUEST_ACCEPTED, restarted.name)
            assertEquals(0L, restarted.durationMs)
            expectNoEvents()
        }
    }
}
