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
