package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.QuestHistoryDto
import com.kotlin.wandr.data.remote.dto.TagDto
import com.kotlin.wandr.data.remote.dto.UserBadgeDto
import com.kotlin.wandr.data.remote.dto.UserDto
import com.kotlin.wandr.data.remote.dto.UserInterestDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/** Facade over `users`, `user_badges` and the quest history. */
@Singleton
class ProfileRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    fun userId(): String = supabase.requireUserId()

    suspend fun fetchProfile(): UserDto =
        supabase.from("users")
            .select { filter { eq("id", userId()) } }
            .decodeSingle()

    suspend fun updateEnergyLevel(value: String) {
        supabase.from("users").update({ set("energy_level", value) }) {
            filter { eq("id", userId()) }
        }
    }

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
}

/** Facade over `tags` and `user_interests`. */
@Singleton
class TagRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun fetchTags(): List<TagDto> =
        supabase.from("tags").select { order("name", Order.ASCENDING) }.decodeList()

    suspend fun fetchInterestIds(): List<String> =
        supabase.from("user_interests")
            .select(Columns.list("user_id", "tag_id")) { filter { eq("user_id", supabase.requireUserId()) } }
            .decodeList<UserInterestDto>()
            .map { it.tagId }

    /** Replaces all the user's interests with [tagIds]. */
    suspend fun replaceInterests(tagIds: List<String>) {
        val userId = supabase.requireUserId()
        supabase.from("user_interests").delete { filter { eq("user_id", userId) } }
        if (tagIds.isNotEmpty()) {
            supabase.from("user_interests").insert(tagIds.map { UserInterestDto(userId, it) })
        }
    }
}
