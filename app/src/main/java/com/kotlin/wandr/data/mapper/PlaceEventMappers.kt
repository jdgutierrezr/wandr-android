package com.kotlin.wandr.data.mapper

import com.kotlin.wandr.data.local.entity.EventEntity
import com.kotlin.wandr.data.local.entity.PlaceEntity
import com.kotlin.wandr.data.remote.dto.EventDto
import com.kotlin.wandr.data.remote.dto.FriendOnMapDto
import com.kotlin.wandr.data.remote.dto.PlaceDto
import com.kotlin.wandr.data.remote.dto.RsvpResultDto
import com.kotlin.wandr.domain.model.Event
import com.kotlin.wandr.domain.model.FriendOnMap
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.Place
import com.kotlin.wandr.domain.model.PlaceCategory
import com.kotlin.wandr.domain.model.RsvpResult

// ---------- Places ----------

/** [distanceKm] overrides the DTO's one (the plain `places` table has no distance). */
fun PlaceDto.toEntity(distanceKm: Double? = this.distanceKm) = PlaceEntity(
    id = id,
    name = name,
    category = category ?: PlaceCategory.OTHER.apiValue,
    address = address,
    latitude = latitude,
    longitude = longitude,
    averageRating = averageRating ?: 0.0,
    coverImageUrl = coverImageUrl,
    distanceKm = distanceKm,
)

fun PlaceEntity.toDomain() = Place(
    id = id,
    name = name,
    category = PlaceCategory.fromApi(category),
    address = address,
    location = GeoPoint(latitude, longitude),
    averageRating = averageRating,
    coverImageUrl = coverImageUrl,
    distanceKm = distanceKm,
)

fun FriendOnMapDto.toDomain() = FriendOnMap(
    userId = userId,
    name = name,
    profilePicture = profilePicture,
    location = GeoPoint(latitude, longitude),
    updatedAt = updatedAt.toInstant(),
    distanceKm = distanceKm,
    activeQuest = activeQuest,
)

// ---------- Events ----------

fun EventDto.toEntity(isAttending: Boolean) = EventEntity(
    id = id,
    title = title,
    description = description,
    coverImageUrl = coverImageUrl,
    startsAtMillis = startsAt.toEpochMillis(),
    endsAtMillis = endsAt?.toEpochMillis(),
    capacity = capacity,
    placeName = places?.name,
    attendees = attendees.firstOrNull()?.count ?: 0,
    isAttending = isAttending,
)

fun EventEntity.toDomain() = Event(
    id = id,
    title = title,
    description = description,
    coverImageUrl = coverImageUrl,
    startsAt = startsAtMillis.toInstant(),
    endsAt = endsAtMillis?.toInstant(),
    capacity = capacity,
    placeName = placeName,
    attendees = attendees,
    isAttending = isAttending,
)

fun RsvpResultDto.toDomain() = RsvpResult(eventId = eventId, attendees = attendees, capacity = capacity)
