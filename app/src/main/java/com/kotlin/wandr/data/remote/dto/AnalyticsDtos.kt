package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of the `get_quest_dropoff()` RPC (BQ8): how many abandoned attempts of a quest stopped
 * right after [lastStep]. `last_step = 0` means the user gave up before checking any step.
 */
@Serializable
data class QuestDropoffDto(
    @SerialName("quest_id") val questId: String,
    @SerialName("quest_title") val questTitle: String,
    @SerialName("total_steps") val totalSteps: Int = 0,
    @SerialName("last_step") val lastStep: Int = 0,
    @SerialName("last_step_title") val lastStepTitle: String? = null,
    @SerialName("abandoned_count") val abandonedCount: Int = 0,
    @SerialName("quest_abandoned_total") val questAbandonedTotal: Int = 0,
)

@Serializable
data class RecommendationLoadTimeDto(
    val requests: Int = 0,
    @SerialName("avg_ms") val avgMs: Int? = null,
    @SerialName("p95_ms") val p95Ms: Int? = null,
    @SerialName("network_avg_ms") val networkAvgMs: Int? = null,
    @SerialName("cache_avg_ms") val cacheAvgMs: Int? = null,
)

@Serializable
data class StepResponseTimeDto(
    @SerialName("quest_id") val questId: String,
    @SerialName("quest_title") val questTitle: String,
    @SerialName("objective_id") val objectiveId: String,
    @SerialName("step_title") val stepTitle: String,
    @SerialName("step_order") val stepOrder: Int = 0,
    @SerialName("requires_photo") val requiresPhoto: Boolean = false,
    val requests: Int = 0,
    @SerialName("avg_ms") val avgMs: Int = 0,
    @SerialName("max_ms") val maxMs: Int = 0,
)
