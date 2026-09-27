package com.kotlin.wandr.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kotlin.wandr.data.local.entity.CompletionEntity
import com.kotlin.wandr.data.local.entity.CompletionWithProgress
import com.kotlin.wandr.data.local.entity.ObjectiveCompletionEntity
import com.kotlin.wandr.data.local.entity.ObjectiveEntity
import com.kotlin.wandr.data.local.entity.QuestEntity
import com.kotlin.wandr.data.local.entity.QuestTagEntity
import com.kotlin.wandr.data.local.entity.QuestWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {

    // ---------- Nearby quests ----------

    @Transaction
    @Query("SELECT * FROM quests WHERE distanceKm IS NOT NULL AND distanceKm <= :radiusKm ORDER BY distanceKm")
    fun observeNearby(radiusKm: Double): Flow<List<QuestWithTags>>

    /** Replaces the last nearby result: old distances are cleared so they no longer show up. */
    @Transaction
    suspend fun replaceNearby(quests: List<QuestEntity>, tags: List<QuestTagEntity>) {
        clearDistances()
        upsertQuests(quests)
        deleteTagsFor(quests.map { it.id })
        upsertTags(tags)
    }

    @Query("UPDATE quests SET distanceKm = NULL")
    suspend fun clearDistances()

    // ---------- Quest detail ----------

    @Transaction
    @Query("SELECT * FROM quests WHERE id = :questId")
    fun observeQuest(questId: String): Flow<QuestWithTags?>

    @Query("SELECT distanceKm FROM quests WHERE id = :questId")
    suspend fun distanceOf(questId: String): Double?

    @Query("SELECT * FROM quest_objectives WHERE questId = :questId ORDER BY orderIndex")
    fun observeObjectives(questId: String): Flow<List<ObjectiveEntity>>

    @Transaction
    suspend fun saveDetail(quest: QuestEntity, tags: List<QuestTagEntity>, objectives: List<ObjectiveEntity>) {
        upsertQuests(listOf(quest))
        deleteTagsFor(listOf(quest.id))
        upsertTags(tags)
        deleteObjectivesFor(quest.id)
        upsertObjectives(objectives)
    }

    // ---------- Active quests and history ----------

    @Transaction
    @Query("SELECT * FROM quest_completions WHERE status = 'pending' ORDER BY startedAtMillis DESC")
    fun observeActive(): Flow<List<CompletionWithProgress>>

    @Query("SELECT * FROM quest_completions WHERE status = 'completed' ORDER BY completedAtMillis DESC")
    fun observeHistory(): Flow<List<CompletionEntity>>

    @Transaction
    suspend fun replaceActive(
        completions: List<CompletionEntity>,
        objectives: List<ObjectiveEntity>,
        done: List<ObjectiveCompletionEntity>,
    ) {
        deleteCompletionsByStatus("pending")
        upsertCompletions(completions)
        objectives.map { it.questId }.distinct().forEach { deleteObjectivesFor(it) }
        upsertObjectives(objectives)
        upsertObjectiveCompletions(done)
    }

    @Transaction
    suspend fun replaceHistory(completions: List<CompletionEntity>) {
        deleteCompletionsByStatus("completed")
        upsertCompletions(completions)
    }

    @Query("DELETE FROM quest_objective_completions WHERE completionId IN (SELECT id FROM quest_completions WHERE status = :status)")
    suspend fun deleteProgressByStatus(status: String)

    @Transaction
    suspend fun deleteCompletionsByStatus(status: String) {
        deleteProgressByStatus(status)
        deleteCompletionRows(status)
    }

    @Query("DELETE FROM quest_completions WHERE status = :status")
    suspend fun deleteCompletionRows(status: String)

    // ---------- Building blocks ----------

    @Upsert
    suspend fun upsertQuests(quests: List<QuestEntity>)

    @Upsert
    suspend fun upsertTags(tags: List<QuestTagEntity>)

    @Query("DELETE FROM quest_tags WHERE questId IN (:questIds)")
    suspend fun deleteTagsFor(questIds: List<String>)

    @Upsert
    suspend fun upsertObjectives(objectives: List<ObjectiveEntity>)

    @Query("DELETE FROM quest_objectives WHERE questId = :questId")
    suspend fun deleteObjectivesFor(questId: String)

    @Upsert
    suspend fun upsertCompletions(completions: List<CompletionEntity>)

    @Upsert
    suspend fun upsertObjectiveCompletions(done: List<ObjectiveCompletionEntity>)
}
