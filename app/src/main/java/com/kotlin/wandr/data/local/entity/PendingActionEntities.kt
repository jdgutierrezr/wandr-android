package com.kotlin.wandr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object PendingActionType {
    const val START = "start"
    const val COMPLETE = "complete"
    const val ABANDON = "abandon"
}

@Entity(tableName = "pending_quest_actions")
data class PendingQuestActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val questId: String,
    val objectiveId: String? = null,
    /** File name inside the private pending photos folder. */
    val photoFile: String? = null,
    val createdAtMillis: Long,
    val attempts: Int = 0,
)
