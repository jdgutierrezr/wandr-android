package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyQuestDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("difficulty_level") val difficultyLevel: Int? = null,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    @SerialName("points_reward") val pointsReward: Int = 0,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("place_id") val placeId: String,
    @SerialName("place_name") val placeName: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
)

@Serializable
data class QuestTagDto(
    @SerialName("quest_id") val questId: String,
    val tags: TagNameDto,
)

@Serializable
data class TagNameDto(val name: String)

@Serializable
data class QuestDetailDto(
    val id: String,
    @SerialName("place_id") val placeId: String,
    val title: String,
    val description: String? = null,
    @SerialName("difficulty_level") val difficultyLevel: Int? = null,
    @SerialName("estimated_duration") val estimatedDuration: Int? = null,
    @SerialName("points_reward") val pointsReward: Int = 0,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val places: PlaceDto? = null,
    @SerialName("quest_objectives") val questObjectives: List<ObjectiveDto> = emptyList(),
    @SerialName("quest_tags") val questTags: List<QuestTagNameDto> = emptyList(),
)

@Serializable
data class QuestTagNameDto(val tags: TagNameDto)

@Serializable
data class ObjectiveDto(
    val id: String,
    @SerialName("quest_id") val questId: String,
    val title: String,
    @SerialName("requires_photo") val requiresPhoto: Boolean = false,
    @SerialName("order_index") val orderIndex: Int,
)

@Serializable
data class CompletionDto(
    val id: String,
    @SerialName("quest_id") val questId: String,
    val status: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ActiveCompletionDto(
    val id: String,
    @SerialName("quest_id") val questId: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    val quests: ActiveQuestInfoDto,
    @SerialName("quest_objective_completions") val objectiveCompletions: List<ObjectiveCompletionDto> = emptyList(),
)

@Serializable
data class ActiveQuestInfoDto(
    val title: String,
    @SerialName("quest_objectives") val questObjectives: List<ObjectiveDto> = emptyList(),
)

@Serializable
data class ObjectiveCompletionDto(
    @SerialName("objective_id") val objectiveId: String,
)

@Serializable
data class ObjectiveResultDto(
    @SerialName("completed_objectives") val completedObjectives: Int,
    @SerialName("total_objectives") val totalObjectives: Int,
    @SerialName("quest_completed") val questCompleted: Boolean,
    @SerialName("xp_earned") val xpEarned: Int = 0,
    @SerialName("current_xp") val currentXp: Int? = null,
    val level: Int? = null,
    val tier: String? = null,
    @SerialName("current_streak") val currentStreak: Int? = null,
    @SerialName("new_badges") val newBadges: List<BadgeDto> = emptyList(),
)
