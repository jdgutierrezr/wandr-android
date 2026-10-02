package com.kotlin.wandr.core.analytics

import com.kotlin.wandr.core.di.ApplicationScope
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.telemetry.TelemetryEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BQ8 · **Observer pattern.** Subscribes to the quest events of the [AppEventBus] and turns each
 * step of the quest funnel into a telemetry row:
 *
 * QUEST_VIEWED → QUEST_ACCEPTED → NAVIGATION_STARTED → QUEST_COMPLETED (or QUEST_ABANDONED)
 *
 * The publishers (QuestDetailViewModel, ActiveQuestViewModel, QuestRepository) do not know this
 * class exists, and this class does not know who published. It republishes each step as an
 * [AppEvent.RequestTimed], so the [com.kotlin.wandr.core.telemetry.TelemetryCollector] saves it in
 * Room (works offline) and uploads it to `telemetry_events` in batches.
 *
 * `durationMs` is the time since the previous funnel step of the same quest (0 for the first),
 * so the funnel also shows how long users take between steps.
 */
@Singleton
class QuestFunnelTracker @Inject constructor(
    private val eventBus: AppEventBus,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    /** Time of the last funnel step per quest, in this app session. */
    private val lastStepAt = ConcurrentHashMap<String, Long>()

    fun start() {
        if (!started.compareAndSet(false, true)) return

        eventBus.events
            .mapNotNull { event -> funnelStep(event) }
            .onEach { (questId, name) -> eventBus.publish(record(questId, name)) }
            .launchIn(scope)
    }

    private fun record(questId: String, name: String): AppEvent.RequestTimed {
        val now = clock.millis()
        val previous = lastStepAt[questId]
        // A finished or abandoned quest starts a new funnel next time
        if (name == TelemetryEvents.QUEST_COMPLETED || name == TelemetryEvents.QUEST_ABANDONED) {
            lastStepAt.remove(questId)
        } else {
            lastStepAt[questId] = now
        }
        return AppEvent.RequestTimed(
            name = name,
            durationMs = if (previous == null) 0 else (now - previous).coerceAtLeast(0),
            success = true,
            metadata = mapOf(
                "quest_id" to questId,
                "after_previous_step" to (previous != null).toString(),
            ),
        )
    }

    private companion object {
        /** Which funnel step an event is, or null if it is not part of the funnel. */
        fun funnelStep(event: AppEvent): Pair<String, String>? = when (event) {
            is AppEvent.QuestViewed -> event.questId to TelemetryEvents.QUEST_VIEWED
            is AppEvent.QuestStarted -> event.questId to TelemetryEvents.QUEST_ACCEPTED
            is AppEvent.NavigationStarted -> event.questId to TelemetryEvents.NAVIGATION_STARTED
            is AppEvent.QuestCompleted -> event.questId to TelemetryEvents.QUEST_COMPLETED
            is AppEvent.QuestAbandoned -> event.questId to TelemetryEvents.QUEST_ABANDONED
            else -> null
        }
    }
}
