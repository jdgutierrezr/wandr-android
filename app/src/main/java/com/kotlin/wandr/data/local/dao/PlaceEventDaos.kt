package com.kotlin.wandr.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kotlin.wandr.data.local.entity.EventEntity
import com.kotlin.wandr.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {

    @Query(
        """
        SELECT * FROM places
        WHERE distanceKm IS NOT NULL AND distanceKm <= :radiusKm
          AND (:category IS NULL OR category = :category)
        ORDER BY distanceKm
        """
    )
    fun observeNearby(radiusKm: Double, category: String?): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :placeId")
    fun observePlace(placeId: String): Flow<PlaceEntity?>

    @Transaction
    suspend fun replaceNearby(places: List<PlaceEntity>) {
        clearDistances()
        upsert(places)
    }

    @Query("UPDATE places SET distanceKm = NULL")
    suspend fun clearDistances()

    @Query("SELECT distanceKm FROM places WHERE id = :placeId")
    suspend fun distanceOf(placeId: String): Double?

    @Upsert
    suspend fun upsert(places: List<PlaceEntity>)
}

@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE COALESCE(endsAtMillis, startsAtMillis) >= :nowMillis ORDER BY startsAtMillis")
    fun observeUpcoming(nowMillis: Long): Flow<List<EventEntity>>

    @Transaction
    suspend fun replaceAll(events: List<EventEntity>) {
        clear()
        upsert(events)
    }

    @Upsert
    suspend fun upsert(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clear()
}
