package com.kotlin.wandr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kotlin.wandr.data.local.entity.PendingQuestActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {

    @Insert
    suspend fun insert(action: PendingQuestActionEntity): Long

    @Query("SELECT * FROM pending_quest_actions ORDER BY id LIMIT 1")
    suspend fun oldest(): PendingQuestActionEntity?

    @Query("SELECT * FROM pending_quest_actions ORDER BY id")
    suspend fun all(): List<PendingQuestActionEntity>

    @Query("SELECT * FROM pending_quest_actions ORDER BY id")
    fun observeAll(): Flow<List<PendingQuestActionEntity>>

    @Query("SELECT COUNT(*) FROM pending_quest_actions")
    suspend fun count(): Int

    @Query("DELETE FROM pending_quest_actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_quest_actions SET attempts = attempts + 1 WHERE id = :id")
    suspend fun addAttempt(id: Long)
}
