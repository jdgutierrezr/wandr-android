package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.PlaceDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

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
