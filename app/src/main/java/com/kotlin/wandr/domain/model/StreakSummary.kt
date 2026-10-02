package com.kotlin.wandr.domain.model

/**
 * BQ4 (Type 2): "How is my activity streak evolving compared with previous weeks?"
 * Built from the `get_streak_summary()` RPC, the same one the iOS app uses.
 */
data class StreakSummary(
    val questsCompleted: Int,
    val points: Int,
    /** Consecutive days with at least one completed quest. */
    val currentStreak: Int,
    /** Always 7 values, Monday to Sunday. */
    val thisWeek: List<Boolean>,
    /** Last weeks, oldest first. The last one is the current week. */
    val weeklyHistory: List<WeeklyQuests>,
    val achievements: List<Achievement>,
) {
    val activeDaysThisWeek: Int get() = thisWeek.count { it }

    val unlockedAchievements: Int get() = achievements.count { it.isUnlocked }

    /** This week against the previous one. Null if there is not enough history. */
    val weekComparison: WeekComparison?
        get() {
            if (weeklyHistory.size < 2) return null
            val current = weeklyHistory[weeklyHistory.lastIndex].quests
            val previous = weeklyHistory[weeklyHistory.lastIndex - 1].quests
            return WeekComparison(current, previous)
        }
}

data class WeeklyQuests(
    /** Label of the week, e.g. "29 Sep" (its Monday). */
    val label: String,
    val quests: Int,
)

data class Achievement(
    val id: String,
    val name: String,
    val isUnlocked: Boolean,
)

/** Quests finished this week vs the previous week. */
data class WeekComparison(val currentWeek: Int, val previousWeek: Int) {

    enum class Trend { UP, DOWN, SAME }

    val difference: Int get() = currentWeek - previousWeek

    val trend: Trend
        get() = when {
            difference > 0 -> Trend.UP
            difference < 0 -> Trend.DOWN
            else -> Trend.SAME
        }

    /** The sentence shown under the chart. */
    val message: String
        get() = when {
            currentWeek == 0 && previousWeek == 0 -> "No quests in the last two weeks. Start one today!"
            trend == Trend.UP -> "${quests(difference)} more than last week. Keep it up!"
            trend == Trend.DOWN -> "${quests(-difference)} fewer than last week. You can catch up!"
            else -> "Same as last week: ${quests(currentWeek)}."
        }

    private fun quests(n: Int) = if (n == 1) "1 quest" else "$n quests"
}
