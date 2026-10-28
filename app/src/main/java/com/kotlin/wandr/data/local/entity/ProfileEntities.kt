package com.kotlin.wandr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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

@Entity(tableName = "user_badges")
data class UserBadgeEntity(
    @PrimaryKey val badgeId: String,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val earnedAtMillis: Long,
)
