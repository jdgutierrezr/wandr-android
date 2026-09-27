package com.kotlin.wandr.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kotlin.wandr.data.local.entity.ProfileEntity
import com.kotlin.wandr.data.local.entity.TagEntity
import com.kotlin.wandr.data.local.entity.UserBadgeEntity
import com.kotlin.wandr.data.local.entity.UserInterestEntity
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

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeTags(): Flow<List<TagEntity>>

    @Upsert
    suspend fun upsertTags(tags: List<TagEntity>)

    @Query("SELECT tagId FROM user_interests")
    fun observeInterestIds(): Flow<List<String>>

    @Transaction
    suspend fun replaceInterests(tagIds: List<String>) {
        clearInterests()
        insertInterests(tagIds.map(::UserInterestEntity))
    }

    @Upsert
    suspend fun insertInterests(interests: List<UserInterestEntity>)

    @Query("DELETE FROM user_interests")
    suspend fun clearInterests()
}
