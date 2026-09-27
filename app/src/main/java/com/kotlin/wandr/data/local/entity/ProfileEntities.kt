package com.kotlin.wandr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The logged-in user's profile. Only one row. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val profilePicture: String?,
    val currentXp: Int,
    val level: Int,
    val currentStreak: Int,
    val tier: String?,
    val energyLevel: String?,
)

/** Badges the logged-in user has earned. */
@Entity(tableName = "user_badges")
data class UserBadgeEntity(
    @PrimaryKey val badgeId: String,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val earnedAtMillis: Long,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
)

/** Tags the logged-in user picked during onboarding. */
@Entity(tableName = "user_interests")
data class UserInterestEntity(
    @PrimaryKey val tagId: String,
)
