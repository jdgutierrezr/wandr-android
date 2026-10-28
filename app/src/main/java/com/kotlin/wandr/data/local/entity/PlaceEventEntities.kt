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
