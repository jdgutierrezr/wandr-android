package com.kotlin.wandr.data.mapper

import com.kotlin.wandr.data.local.CachedDropoff
import com.kotlin.wandr.data.remote.dto.QuestDropoffDto
import com.kotlin.wandr.data.remote.dto.RecommendationLoadTimeDto
import com.kotlin.wandr.data.remote.dto.StepResponseTimeDto
import com.kotlin.wandr.domain.model.DropoffPoint
import com.kotlin.wandr.domain.model.QuestDropoffReport
import com.kotlin.wandr.domain.model.RecommendationLoadTime
import com.kotlin.wandr.domain.model.StepResponseTime
import com.kotlin.wandr.data.remote.dto.LowUseCategoryDto
import com.kotlin.wandr.data.remote.dto.WeeklyCategoryStartsDto
import com.kotlin.wandr.domain.model.LowUseCategory
import com.kotlin.wandr.domain.model.WeeklyCategoryStarts
import com.kotlin.wandr.data.local.CachedLowUseCategories
import com.kotlin.wandr.domain.model.LowUseCategoriesReport

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

fun StepResponseTimeDto.toDomain() = StepResponseTime(
    questId = questId,
    questTitle = questTitle,
    objectiveId = objectiveId,
    stepTitle = stepTitle,
    stepOrder = stepOrder,
    requiresPhoto = requiresPhoto,
    requests = requests,
    avgMs = avgMs,
    maxMs = maxMs,
)

fun LowUseCategoryDto.toDomain() = LowUseCategory(
    tagId = tagId,
    tagName = tagName,
    totalStarts = totalStarts,
    averagePerWeek = averagePerWeek,
    weeklyBreakdown = weeklyBreakdown.map { it.toDomain() },
)

fun WeeklyCategoryStartsDto.toDomain() = WeeklyCategoryStarts(
    weekStart = weekStart,
    starts = starts,
)

fun CachedLowUseCategories.toReport(isStale: Boolean) = LowUseCategoriesReport(
    categories = rows.map { it.toDomain() },
    updatedAtMillis = savedAtMillis,
    isStale = isStale,
)
