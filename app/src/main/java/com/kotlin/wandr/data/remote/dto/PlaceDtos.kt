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
