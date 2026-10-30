package com.kotlin.wandr.data.mapper

import com.kotlin.wandr.data.local.CachedDropoff
import com.kotlin.wandr.data.remote.dto.QuestDropoffDto
import com.kotlin.wandr.data.remote.dto.RecommendationLoadTimeDto
import com.kotlin.wandr.domain.model.DropoffPoint
import com.kotlin.wandr.domain.model.QuestDropoffReport
import com.kotlin.wandr.domain.model.RecommendationLoadTime

fun QuestDropoffDto.toDomain() = DropoffPoint(
    questId = questId,
    questTitle = questTitle,
    totalSteps = totalSteps,
    lastStep = lastStep,
    lastStepTitle = lastStepTitle,
    abandonedCount = abandonedCount,
)

fun CachedDropoff.toReport(isStale: Boolean) = QuestDropoffReport(
    points = rows.map { it.toDomain() },
    updatedAtMillis = savedAtMillis,
    isStale = isStale,
)

fun RecommendationLoadTimeDto.toDomain() = RecommendationLoadTime(
    requests = requests,
    avgMs = avgMs,
    p95Ms = p95Ms,
    networkAvgMs = networkAvgMs,
    cacheAvgMs = cacheAvgMs,
)
