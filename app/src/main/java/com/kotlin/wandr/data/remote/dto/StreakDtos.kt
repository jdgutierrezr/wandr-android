package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of the `get_streak_summary()` RPC (BQ4). Same JSON the iOS app reads.
 *
 * ```json
 * {
 *   "user_id": "…",
 *   "stats": { "quests_completed": 12, "points": 850 },
 *   "streak": {
 *     "current_streak": 3,
 *     "this_week": [true, false, true, false, false, false, false],
 *     "weekly_history": [{ "week_id": "08 Sep", "quests": 2 }, …]
 *   },
 *   "achievements": [{ "id": "…", "name": "First Quest", "unlocked": true }, …]
 * }
 * ```
 */
@Serializable
data class StreakSummaryDto(
    @SerialName("user_id") val userId: String,
    val stats: StreakStatsDto,
    val streak: StreakDto,
    val achievements: List<AchievementDto> = emptyList(),
)

@Serializable
data class StreakStatsDto(
    @SerialName("quests_completed") val questsCompleted: Int = 0,
    /** The user's `current_xp`. */
    val points: Int = 0,
)

@Serializable
data class StreakDto(
    /** Consecutive days with at least one completed quest. */
    @SerialName("current_streak") val currentStreak: Int = 0,
    /** 7 values, Monday to Sunday (Bogota time): did the user finish a quest that day? */
    @SerialName("this_week") val thisWeek: List<Boolean> = emptyList(),
    /** Last 4 weeks, oldest first. The last one is the current week. */
    @SerialName("weekly_history") val weeklyHistory: List<WeekDto> = emptyList(),
)

@Serializable
data class WeekDto(
    /** Monday of that week, e.g. "29 Sep". */
    @SerialName("week_id") val weekId: String,
    val quests: Int = 0,
)

@Serializable
data class AchievementDto(
    val id: String,
    val name: String,
    val unlocked: Boolean = false,
)
