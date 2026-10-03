package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecommendedQuestDto(
    val id: String,
    val title: String,
    val description: String?,
    val difficulty_level: Int,
    val estimated_duration: Int,
    val points_reward: Int,
    val cover_image_url: String?,
    val place_id: String,
    val place_name: String,
    val distance_km: Double,
    val matched_interests: Long,
    val energy_match: Boolean,
)