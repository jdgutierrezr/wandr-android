package com.kotlin.wandr.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kotlin.wandr.data.local.entity.ProfileEntity
import com.kotlin.wandr.data.local.entity.UserBadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile WHERE id = :userId")
    fun observeProfile(userId: String): Flow<ProfileEntity?>

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM user_badges ORDER BY earnedAtMillis DESC")
    fun observeBadges(): Flow<List<UserBadgeEntity>>

    @Transaction
    suspend fun replaceBadges(badges: List<UserBadgeEntity>) {
        clearBadges()
        upsertBadges(badges)
    }

    @Upsert
    suspend fun upsertBadges(badges: List<UserBadgeEntity>)

    @Query("DELETE FROM user_badges")
    suspend fun clearBadges()
}
