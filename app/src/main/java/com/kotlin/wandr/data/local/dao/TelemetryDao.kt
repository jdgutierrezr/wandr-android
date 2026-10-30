package com.kotlin.wandr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kotlin.wandr.data.local.entity.TelemetryEventEntity

@Dao
interface TelemetryDao {

    @Insert
    suspend fun insert(event: TelemetryEventEntity)

    @Query("SELECT * FROM telemetry_events ORDER BY id LIMIT :limit")
    suspend fun oldest(limit: Int): List<TelemetryEventEntity>

    @Query("SELECT COUNT(*) FROM telemetry_events")
    suspend fun count(): Int

    @Query("DELETE FROM telemetry_events WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)
}
