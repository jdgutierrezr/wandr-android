package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.ActiveCompletionDto
import com.kotlin.wandr.data.remote.dto.CompletionDto
import com.kotlin.wandr.data.remote.dto.NearbyQuestDto
import com.kotlin.wandr.data.remote.dto.ObjectiveResultDto
import com.kotlin.wandr.data.remote.dto.QuestDetailDto
import com.kotlin.wandr.data.remote.dto.QuestTagDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Facade over the quest tables and RPCs. */
@Singleton
class QuestRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun nearbyQuests(latitude: Double, longitude: Double, radiusKm: Double): List<NearbyQuestDto> =
        supabase.postgrest.rpc(
            "nearby_quests",
            buildJsonObject {
                put("p_lat", latitude)
                put("p_lng", longitude)
                put("p_radius_km", radiusKm)
            },
        ).decodeList()

    /** `nearby_quests` does not return tags, so they are fetched for those quests in one call. */
    suspend fun tagsFor(questIds: List<String>): List<QuestTagDto> {
        if (questIds.isEmpty()) return emptyList()
        return supabase.from("quest_tags")
            .select(Columns.raw("quest_id, tags(name)")) { filter { isIn("quest_id", questIds) } }
            .decodeList()
    }

    suspend fun questDetail(questId: String): QuestDetailDto =
        supabase.from("quests")
            .select(Columns.raw("*, places(*), quest_objectives(*), quest_tags(tags(name))")) {
                filter { eq("id", questId) }
                order("order_index", Order.ASCENDING, referencedTable = "quest_objectives")
            }
            .decodeSingle()

    suspend fun activeCompletions(): List<ActiveCompletionDto> =
        supabase.from("quest_completions")
            .select(
                Columns.raw(
                    "id, quest_id, status, created_at, " +
                        "quests(title, quest_objectives(*)), " +
                        "quest_objective_completions(objective_id)"
                )
            ) {
                filter {
                    eq("user_id", supabase.requireUserId())
                    eq("status", "pending")
                }
            }
            .decodeList()

    suspend fun startQuest(questId: String): CompletionDto =
        supabase.postgrest.rpc("start_quest", buildJsonObject { put("p_quest_id", questId) }).decodeAs()

    suspend fun completeObjective(questId: String, objectiveId: String, photoPath: String?): ObjectiveResultDto =
        supabase.postgrest.rpc(
            "complete_objective",
            buildJsonObject {
                put("p_quest_id", questId)
                put("p_objective_id", objectiveId)
                if (photoPath != null) put("p_photo_url", photoPath)
            },
        ).decodeAs()

    suspend fun abandonQuest(questId: String): CompletionDto =
        supabase.postgrest.rpc("abandon_quest", buildJsonObject { put("p_quest_id", questId) }).decodeAs()
}

/** Facade over Supabase Storage. */
@Singleton
class StorageRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    fun userId(): String = supabase.requireUserId()

    /** Uploads (or replaces) a file and returns its path inside the bucket. */
    suspend fun upload(bucket: String, path: String, bytes: ByteArray): String {
        supabase.storage.from(bucket).upload(path, bytes) { upsert = true }
        return path
    }
}
