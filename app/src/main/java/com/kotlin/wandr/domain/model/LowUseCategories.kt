package com.kotlin.wandr.domain.model

data class LowUseCategory(
    val tagId: String,
    val tagName: String,
    val totalStarts: Int,
    val averagePerWeek: Double,
    val weeklyBreakdown: List<WeeklyCategoryStarts>,
)

data class WeeklyCategoryStarts(
    val weekStart: String,
    val starts: Int,
)

data class LowUseCategoriesReport(
    val categories: List<LowUseCategory>,
    val updatedAtMillis: Long? = null,
    val isStale: Boolean = false,
)
