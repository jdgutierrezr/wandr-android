package com.kotlin.wandr.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val placeId: String,
    val placeName: String?,
    val title: String,
    val description: String?,
    val difficultyLevel: Int?,
    val estimatedDuration: Int?,
    val pointsReward: Int,
    val coverImageUrl: String?,
    /** Distance from the last location used in `nearby_quests`. Null = not in the last nearby result. */
    val distanceKm: Double?,
)

@Entity(tableName = "quest_tags", primaryKeys = ["questId", "tagName"])
data class QuestTagEntity(
    val questId: String,
    val tagName: String,
)

@Entity(tableName = "quest_objectives", indices = [Index("questId")])
data class ObjectiveEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val title: String,
    val requiresPhoto: Boolean,
    val orderIndex: Int,
)

/** The user's attempts at quests: pending (active) and completed (history). */
@Entity(tableName = "quest_completions", indices = [Index("questId"), Index("status")])
data class CompletionEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val questTitle: String,
    val pointsReward: Int,
    val status: String,
    val startedAtMillis: Long,
    val completedAtMillis: Long?,
)

@Entity(tableName = "quest_objective_completions", primaryKeys = ["completionId", "objectiveId"])
data class ObjectiveCompletionEntity(
    val completionId: String,
    val objectiveId: String,
)

data class QuestWithTags(
    @Embedded val quest: QuestEntity,
    @Relation(parentColumn = "id", entityColumn = "questId")
    val tags: List<QuestTagEntity>,
)

data class CompletionWithProgress(
    @Embedded val completion: CompletionEntity,
    @Relation(parentColumn = "questId", entityColumn = "questId")
    val objectives: List<ObjectiveEntity>,
    @Relation(parentColumn = "id", entityColumn = "completionId")
    val completedObjectives: List<ObjectiveCompletionEntity>,
)
