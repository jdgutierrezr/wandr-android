package com.kotlin.wandr.domain.model

data class RecommendationLoadTime(
    val requests: Int,
    val avgMs: Int?,
    val p95Ms: Int?,
    val networkAvgMs: Int?,
    val cacheAvgMs: Int?,
)
