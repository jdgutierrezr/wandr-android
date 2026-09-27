package com.kotlin.wandr.core.telemetry

/** Names saved in `telemetry_events.event_name`. The SQL for BQ1 / BQ2 filters by them. */
object TelemetryEvents {
    /** BQ1: average response time for quest recommendations. */
    const val QUEST_RECOMMENDATIONS_LOAD = "quest_recommendations_load"

    /** BQ2: which quest step has the highest average response time. */
    const val QUEST_STEP_RESPONSE = "quest_step_response"
}
