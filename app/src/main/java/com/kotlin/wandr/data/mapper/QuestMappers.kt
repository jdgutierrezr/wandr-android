package com.kotlin.wandr.data.mapper

import com.kotlin.wandr.data.local.entity.CompletionEntity
import com.kotlin.wandr.data.local.entity.CompletionWithProgress
import com.kotlin.wandr.data.local.entity.ObjectiveCompletionEntity
import com.kotlin.wandr.data.local.entity.ObjectiveEntity
import com.kotlin.wandr.data.local.entity.QuestEntity
import com.kotlin.wandr.data.local.entity.QuestTagEntity
import com.kotlin.wandr.data.local.entity.QuestWithTags
import com.kotlin.wandr.data.remote.dto.ActiveCompletionDto
import com.kotlin.wandr.data.remote.dto.NearbyQuestDto
import com.kotlin.wandr.data.remote.dto.ObjectiveDto
import com.kotlin.wandr.data.remote.dto.ObjectiveResultDto
import com.kotlin.wandr.data.remote.dto.QuestDetailDto
import com.kotlin.wandr.data.remote.dto.QuestTagDto
import com.kotlin.wandr.data.remote.dto.RecommendedQuestDto
import com.kotlin.wandr.domain.model.ActiveQuest
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.domain.model.QuestObjective
import com.kotlin.wandr.domain.model.RecommendedQuest
import com.kotlin.wandr.domain.model.Tier

// ---------- Quests ----------

fun NearbyQuestDto.toEntity() = QuestEntity(
    id = id,
    placeId = placeId,
    placeName = placeName,
    title = title,
    description = description,
    difficultyLevel = difficultyLevel,
    estimatedDuration = estimatedDuration,
    pointsReward = pointsReward,
    coverImageUrl = coverImageUrl,
    distanceKm = distanceKm,
)

fun QuestTagDto.toEntity() = QuestTagEntity(questId = questId, tagName = tags.name)

/** [distanceKm] is kept from the cache: the detail endpoint does not know where the user is. */
fun QuestDetailDto.toEntity(distanceKm: Double?) = QuestEntity(
    id = id,
    placeId = placeId,
    placeName = places?.name,
    title = title,
    description = description,
    difficultyLevel = difficultyLevel,
    estimatedDuration = estimatedDuration,
    pointsReward = pointsReward,
    coverImageUrl = coverImageUrl,
    distanceKm = distanceKm,
)

fun QuestDetailDto.tagEntities() = questTags.map { QuestTagEntity(questId = id, tagName = it.tags.name) }

fun QuestWithTags.toDomain() = Quest(
    id = quest.id,
    title = quest.title,
    description = quest.description,
    difficultyLevel = quest.difficultyLevel,
    estimatedDurationMin = quest.estimatedDuration,
    pointsReward = quest.pointsReward,
    coverImageUrl = quest.coverImageUrl,
    placeId = quest.placeId,
    placeName = quest.placeName,
    tags = tags.map { it.tagName }.sorted(),
    distanceKm = quest.distanceKm,
)

fun RecommendedQuestDto.toDomain() = Quest(
    id = id,
    title = title,
    description = description,
    difficultyLevel = difficulty_level,
    estimatedDurationMin = estimated_duration,
    pointsReward = points_reward,
    coverImageUrl = cover_image_url,
    placeId = place_id,
    placeName = place_name,
    tags = emptyList(),
    distanceKm = distance_km,
)

fun RecommendedQuestDto.toRecommendedQuest() = RecommendedQuest(
    quest = toDomain(),
    matchedInterests = emptyList(),
    energyMatch = energy_match,
)
// ---------- Objectives ----------

fun ObjectiveDto.toEntity() = ObjectiveEntity(
    id = id,
    questId = questId,
    title = title,
    requiresPhoto = requiresPhoto,
    orderIndex = orderIndex,
)

fun ObjectiveEntity.toDomain() = QuestObjective(
    id = id,
    questId = questId,
    title = title,
    requiresPhoto = requiresPhoto,
    orderIndex = orderIndex,
)

// ---------- Active quests ----------

/** One remote row becomes three kinds of local rows. */
data class ActiveQuestRows(
    val completion: CompletionEntity,
    val objectives: List<ObjectiveEntity>,
    val done: List<ObjectiveCompletionEntity>,
)

fun ActiveCompletionDto.toRows() = ActiveQuestRows(
    completion = CompletionEntity(
        id = id,
        questId = questId,
        questTitle = quests.title,
        pointsReward = 0,
        status = status,
        startedAtMillis = createdAt.toEpochMillis(),
        completedAtMillis = null,
    ),
    objectives = quests.questObjectives.map { it.toEntity() },
    done = objectiveCompletions.map { ObjectiveCompletionEntity(completionId = id, objectiveId = it.objectiveId) },
)

fun CompletionWithProgress.toDomain() = ActiveQuest(
    completionId = completion.id,
    questId = completion.questId,
    questTitle = completion.questTitle,
    startedAt = completion.startedAtMillis.toInstant(),
    objectives = objectives.sortedBy { it.orderIndex }.map { it.toDomain() },
    completedObjectiveIds = completedObjectives.map { it.objectiveId }.toSet(),
)

fun ObjectiveResultDto.toDomain() = ObjectiveResult(
    completedObjectives = completedObjectives,
    totalObjectives = totalObjectives,
    questCompleted = questCompleted,
    xpEarned = xpEarned,
    currentXp = currentXp,
    level = level,
    tier = tier?.let(Tier::fromApi),
    currentStreak = currentStreak,
    newBadges = newBadges.map { it.toDomain() },
)
