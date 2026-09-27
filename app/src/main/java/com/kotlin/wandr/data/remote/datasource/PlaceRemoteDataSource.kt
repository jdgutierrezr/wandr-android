package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.FriendOnMapDto
import com.kotlin.wandr.data.remote.dto.PlaceDto
import com.kotlin.wandr.data.remote.dto.UserLocationDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Facade over `places` and the `nearby_places` RPC. */
@Singleton
class PlaceRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun nearbyPlaces(latitude: Double, longitude: Double, radiusKm: Double, category: String?): List<PlaceDto> =
        supabase.postgrest.rpc(
            "nearby_places",
            buildJsonObject {
                put("p_lat", latitude)
                put("p_lng", longitude)
                put("p_radius_km", radiusKm)
                if (category != null) put("p_category", category)
            },
        ).decodeList()
}

/** Facade over `user_locations` and the `friends_on_map` RPC. */
@Singleton
class LocationRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    /** One row per user: insert the first time, update after (`Prefer: resolution=merge-duplicates`). */
    suspend fun upsertMyLocation(latitude: Double, longitude: Double, isBroadcasting: Boolean) {
        supabase.from("user_locations").upsert(
            UserLocationDto(
                userId = supabase.requireUserId(),
                latitude = latitude,
                longitude = longitude,
                isBroadcasting = isBroadcasting,
                updatedAt = Instant.now().toString(),
            )
        )
    }

    suspend fun friendsOnMap(): List<FriendOnMapDto> =
        supabase.postgrest.rpc("friends_on_map").decodeList()
}
