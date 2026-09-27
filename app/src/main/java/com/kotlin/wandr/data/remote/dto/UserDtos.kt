package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row of `users`. */
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("profile_picture") val profilePicture: String? = null,
    @SerialName("current_xp") val currentXp: Int = 0,
    val level: Int = 1,
    @SerialName("current_streak") val currentStreak: Int = 0,
    val tier: String? = null,
    @SerialName("energy_level") val energyLevel: String? = null,
)

/** Body of `POST users` right after sign up. Only these columns can be inserted by the app. */
@Serializable
data class NewUserDto(
    val id: String,
    val name: String,
    val email: String,
)

@Serializable
data class UserSummaryDto(
    val id: String,
    val name: String,
    @SerialName("profile_picture") val profilePicture: String? = null,
)

@Serializable
data class TagDto(
    val id: String,
    val name: String,
)

/** Row of `user_interests`. */
@Serializable
data class UserInterestDto(
    @SerialName("user_id") val userId: String,
    @SerialName("tag_id") val tagId: String,
)

@Serializable
data class BadgeDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
)

/** `user_badges?select=earned_at,badges(*)` */
@Serializable
data class UserBadgeDto(
    @SerialName("earned_at") val earnedAt: String,
    val badges: BadgeDto,
)

/** `quest_completions?status=eq.completed&select=id,quest_id,completed_at,quests(title,points_reward)` */
@Serializable
data class QuestHistoryDto(
    val id: String,
    @SerialName("quest_id") val questId: String,
    @SerialName("completed_at") val completedAt: String? = null,
    val quests: QuestTitleDto,
)

@Serializable
data class QuestTitleDto(
    val title: String,
    @SerialName("points_reward") val pointsReward: Int = 0,
)
