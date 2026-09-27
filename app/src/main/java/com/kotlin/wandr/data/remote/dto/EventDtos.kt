package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `events?select=*,places(name),event_attendees(count)` */
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
    /** PostgREST returns `[{"count": n}]` for an embedded count. */
    @SerialName("event_attendees") val attendees: List<CountDto> = emptyList(),
)

@Serializable
data class CountDto(val count: Int)

/** `event_attendees?user_id=eq.me&select=event_id` */
@Serializable
data class EventIdDto(@SerialName("event_id") val eventId: String)

/** JSON returned by the `rsvp_event` RPC. */
@Serializable
data class RsvpResultDto(
    @SerialName("event_id") val eventId: String,
    val attendees: Int,
    val capacity: Int? = null,
)
