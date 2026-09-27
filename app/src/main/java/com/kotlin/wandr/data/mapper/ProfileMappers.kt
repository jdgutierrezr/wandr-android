package com.kotlin.wandr.data.mapper

import com.kotlin.wandr.data.local.entity.CompletionEntity
import com.kotlin.wandr.data.local.entity.ProfileEntity
import com.kotlin.wandr.data.local.entity.TagEntity
import com.kotlin.wandr.data.local.entity.UserBadgeEntity
import com.kotlin.wandr.data.remote.dto.BadgeDto
import com.kotlin.wandr.data.remote.dto.QuestHistoryDto
import com.kotlin.wandr.data.remote.dto.TagDto
import com.kotlin.wandr.data.remote.dto.UserBadgeDto
import com.kotlin.wandr.data.remote.dto.UserDto
import com.kotlin.wandr.data.remote.dto.UserSummaryDto
import com.kotlin.wandr.domain.model.Badge
import com.kotlin.wandr.domain.model.EarnedBadge
import com.kotlin.wandr.domain.model.EnergyLevel
import com.kotlin.wandr.domain.model.QuestHistoryItem
import com.kotlin.wandr.domain.model.QuestStatus
import com.kotlin.wandr.domain.model.Tag
import com.kotlin.wandr.domain.model.Tier
import com.kotlin.wandr.domain.model.UserProfile
import com.kotlin.wandr.domain.model.UserSummary

// ---------- Profile ----------

fun UserDto.toEntity() = ProfileEntity(
    id = id,
    name = name,
    email = email,
    profilePicture = profilePicture,
    currentXp = currentXp,
    level = level,
    currentStreak = currentStreak,
    tier = tier,
    energyLevel = energyLevel,
)

fun ProfileEntity.toDomain() = UserProfile(
    id = id,
    name = name,
    email = email,
    profilePicture = profilePicture,
    currentXp = currentXp,
    level = level,
    currentStreak = currentStreak,
    tier = Tier.fromApi(tier),
    energyLevel = EnergyLevel.fromApi(energyLevel),
)

fun UserSummaryDto.toDomain() = UserSummary(id = id, name = name, profilePicture = profilePicture)

// ---------- Tags ----------

fun TagDto.toEntity() = TagEntity(id = id, name = name)

fun TagEntity.toDomain() = Tag(id = id, name = name)

// ---------- Badges ----------

fun BadgeDto.toDomain() = Badge(id = id, name = name, description = description, iconUrl = iconUrl)

fun UserBadgeDto.toEntity() = UserBadgeEntity(
    badgeId = badges.id,
    name = badges.name,
    description = badges.description,
    iconUrl = badges.iconUrl,
    earnedAtMillis = earnedAt.toEpochMillis(),
)

fun UserBadgeEntity.toDomain() = EarnedBadge(
    badge = Badge(id = badgeId, name = name, description = description, iconUrl = iconUrl),
    earnedAt = earnedAtMillis.toInstant(),
)

// ---------- Quest history ----------

fun QuestHistoryDto.toEntity() = CompletionEntity(
    id = id,
    questId = questId,
    questTitle = quests.title,
    pointsReward = quests.pointsReward,
    status = QuestStatus.COMPLETED.apiValue,
    startedAtMillis = completedAt?.toEpochMillis() ?: 0L,
    completedAtMillis = completedAt?.toEpochMillis(),
)

fun CompletionEntity.toHistoryItem() = QuestHistoryItem(
    questId = questId,
    questTitle = questTitle,
    pointsReward = pointsReward,
    completedAt = (completedAtMillis ?: startedAtMillis).toInstant(),
)
