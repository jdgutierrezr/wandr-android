package com.kotlin.wandr.core.telemetry

/** Names saved in `telemetry_events.event_name`. The SQL for BQ1 / BQ2 / BQ8 filters by them. */
object TelemetryEvents {
    /** BQ1: average response time for quest recommendations. */
    const val QUEST_RECOMMENDATIONS_LOAD = "quest_recommendations_load"

    /** BQ2: which quest step has the highest average response time. */
    const val QUEST_STEP_RESPONSE = "quest_step_response"

    // BQ8 funnel. `duration_ms` is the time since the previous funnel step of the same quest
    // (0 for the first one); `metadata.quest_id` says which quest.
    const val QUEST_VIEWED = "quest_viewed"
    const val QUEST_ACCEPTED = "quest_accepted"
    const val NAVIGATION_STARTED = "navigation_started"
    const val QUEST_COMPLETED = "quest_completed"
    const val QUEST_ABANDONED = "quest_abandoned"
}
