package com.kotlin.wandr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val averageRating: Double,
    val coverImageUrl: String?,
    /** Distance from the last location used in `nearby_places`. Null = not in the last nearby result. */
    val distanceKm: Double?,
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val startsAtMillis: Long,
    val endsAtMillis: Long?,
    val capacity: Int?,
    val placeName: String?,
    val attendees: Int,
    val isAttending: Boolean,
)

/** Telemetry waiting to be sent to Supabase in a batch. */
@Entity(tableName = "telemetry_events")
data class TelemetryEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationMs: Long,
    val success: Boolean,
    /** Metadata as a JSON object string. */
    val metadataJson: String,
    val createdAtMillis: Long,
)
