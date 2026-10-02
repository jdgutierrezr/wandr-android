package com.kotlin.wandr.data

import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.remote.dto.StreakSummaryDto
import com.kotlin.wandr.domain.model.WeekComparison
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** BQ4: the `get_streak_summary` JSON decodes and the week comparison is right. */
class StreakSummaryTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** Same shape the RPC returns (see `sql/rpc_functions.sql` in the backend repo). */
    private val rpcResponse = """
        {
          "user_id": "11111111-1111-1111-1111-111111111111",
          "stats": { "quests_completed": 14, "points": 1250 },
          "streak": {
            "current_streak": 3,
            "this_week": [true, true, false, true, false, false, false],
            "weekly_history": [
              { "week_id": "08 Sep", "quests": 2 },
              { "week_id": "15 Sep", "quests": 4 },
              { "week_id": "22 Sep", "quests": 1 },
              { "week_id": "29 Sep", "quests": 3 }
            ]
          },
          "achievements": [
            { "id": "b1", "name": "First Quest", "unlocked": true },
            { "id": "b2", "name": "Early Bird", "unlocked": false }
          ]
        }
    """.trimIndent()

    @Test
    fun `rpc response decodes into the domain model`() {
        val summary = json.decodeFromString<StreakSummaryDto>(rpcResponse).toDomain()

        assertEquals(14, summary.questsCompleted)
        assertEquals(1250, summary.points)
        assertEquals(3, summary.currentStreak)
        assertEquals(listOf(true, true, false, true, false, false, false), summary.thisWeek)
        assertEquals(3, summary.activeDaysThisWeek)
        assertEquals(listOf("08 Sep", "15 Sep", "22 Sep", "29 Sep"), summary.weeklyHistory.map { it.label })
        assertEquals(1, summary.unlockedAchievements)
        assertEquals("First Quest", summary.achievements.first().name)
    }

    @Test
    fun `this week is compared with the previous one`() {
        val summary = json.decodeFromString<StreakSummaryDto>(rpcResponse).toDomain()

        val comparison = summary.weekComparison!!
        assertEquals(3, comparison.currentWeek)
        assertEquals(1, comparison.previousWeek)
        assertEquals(WeekComparison.Trend.UP, comparison.trend)
        assertEquals("2 quests more than last week. Keep it up!", comparison.message)
    }

    @Test
    fun `messages for fewer, same and empty weeks`() {
        assertEquals(WeekComparison.Trend.DOWN, WeekComparison(1, 4).trend)
        assertEquals("3 quests fewer than last week. You can catch up!", WeekComparison(1, 4).message)
        assertEquals("Same as last week: 1 quest.", WeekComparison(1, 1).message)
        assertEquals("No quests in the last two weeks. Start one today!", WeekComparison(0, 0).message)
    }

    @Test
    fun `this week always has 7 days and missing history means no comparison`() {
        val short = """
            { "user_id": "u", "stats": {}, "streak": { "this_week": [true], "weekly_history": [] } }
        """.trimIndent()
        val summary = json.decodeFromString<StreakSummaryDto>(short).toDomain()

        assertEquals(listOf(true, false, false, false, false, false, false), summary.thisWeek)
        assertNull(summary.weekComparison)
        assertEquals(0, summary.achievements.size)
    }
}
