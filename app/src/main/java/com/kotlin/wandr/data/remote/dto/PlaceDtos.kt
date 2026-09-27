package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row of `places`, or of the `nearby_places` RPC (which adds [distanceKm]). */
@Serializable
data class PlaceDto(
    val id: String,
    val name: String,
    val category: String? = null,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("average_rating") val averageRating: Double? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
)

/** Embedded `places(name)`. */
@Serializable
data class PlaceNameDto(val name: String)

/** Body of the `user_locations` upsert. */
@Serializable
data class UserLocationDto(
    @SerialName("user_id") val userId: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("is_broadcasting") val isBroadcasting: Boolean,
    @SerialName("updated_at") val updatedAt: String,
)

/** Row returned by the `friends_on_map` RPC. */
@Serializable
data class FriendOnMapDto(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("profile_picture") val profilePicture: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("active_quest") val activeQuest: String? = null,
)
