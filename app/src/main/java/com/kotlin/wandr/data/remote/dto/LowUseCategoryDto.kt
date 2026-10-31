package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LowUseCategoryDto(
    @SerialName("tag_id")
    val tagId: String,

    @SerialName("tag_name")
    val tagName: String,

    @SerialName("total_starts")
    val totalStarts: Int,

    @SerialName("average_per_week")
    val averagePerWeek: Double,

    @SerialName("weekly_breakdown")
    val weeklyBreakdown: List<WeeklyCategoryStartsDto>,
)

@Serializable
data class WeeklyCategoryStartsDto(
    @SerialName("week_start")
    val weekStart: String,

    val starts: Int,
)
