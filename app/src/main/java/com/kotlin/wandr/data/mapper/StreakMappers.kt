package com.kotlin.wandr.data.mapper

import com.kotlin.wandr.data.remote.dto.AchievementDto
import com.kotlin.wandr.data.remote.dto.StreakSummaryDto
import com.kotlin.wandr.data.remote.dto.WeekDto
import com.kotlin.wandr.domain.model.Achievement
import com.kotlin.wandr.domain.model.StreakSummary
import com.kotlin.wandr.domain.model.WeeklyQuests

private const val DAYS_IN_WEEK = 7

fun StreakSummaryDto.toDomain() = StreakSummary(
    questsCompleted = stats.questsCompleted,
    points = stats.points,
    currentStreak = streak.currentStreak,
    // The UI always draws 7 days: fill or cut if the backend ever sends something else
    thisWeek = List(DAYS_IN_WEEK) { day -> streak.thisWeek.getOrElse(day) { false } },
    weeklyHistory = streak.weeklyHistory.map { it.toDomain() },
    achievements = achievements.map { it.toDomain() },
)

fun WeekDto.toDomain() = WeeklyQuests(label = weekId, quests = quests)

fun AchievementDto.toDomain() = Achievement(id = id, name = name, isUnlocked = unlocked)
