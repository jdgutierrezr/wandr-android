package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String? = null,
    val capacity: Int? = null,
    val places: PlaceNameDto? = null,
    @SerialName("event_attendees") val attendees: List<CountDto> = emptyList(),
)

@Serializable
data class CountDto(val count: Int)

@Serializable
data class EventIdDto(@SerialName("event_id") val eventId: String)
