package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.QuestHistoryDto
import com.kotlin.wandr.data.remote.dto.StreakSummaryDto
import com.kotlin.wandr.data.remote.dto.UserBadgeDto
import com.kotlin.wandr.data.remote.dto.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/** Facade over `users`, `user_badges`, the quest history and the streak summary. */
@Singleton
class ProfileRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    fun userId(): String = supabase.requireUserId()

    suspend fun fetchProfile(): UserDto =
        supabase.from("users")
            .select { filter { eq("id", userId()) } }
            .decodeSingle()

    suspend fun fetchBadges(): List<UserBadgeDto> =
        supabase.from("user_badges")
            .select(Columns.raw("earned_at, badges(*)")) { filter { eq("user_id", userId()) } }
            .decodeList()

    suspend fun fetchQuestHistory(): List<QuestHistoryDto> =
        supabase.from("quest_completions")
            .select(Columns.raw("id, quest_id, completed_at, quests(title, points_reward)")) {
                filter {
                    eq("user_id", userId())
                    eq("status", "completed")
                }
                order("completed_at", Order.DESCENDING)
            }
            .decodeList()

    /** BQ4. The RPC has no parameters: it uses the logged-in user. */
    suspend fun fetchStreakSummary(): StreakSummaryDto =
        supabase.postgrest.rpc("get_streak_summary").decodeAs()
}
