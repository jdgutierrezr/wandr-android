package com.kotlin.wandr.domain.model

data class RecommendationLoadTime(
    val requests: Int,
    val avgMs: Int?,
    val p95Ms: Int?,
    val networkAvgMs: Int?,
    val cacheAvgMs: Int?,
)

data class StepResponseTime(
    val questId: String,
    val questTitle: String,
    val objectiveId: String,
    val stepTitle: String,
    val stepOrder: Int,
    val requiresPhoto: Boolean,
    val requests: Int,
    val avgMs: Int,
    val maxMs: Int,
)
