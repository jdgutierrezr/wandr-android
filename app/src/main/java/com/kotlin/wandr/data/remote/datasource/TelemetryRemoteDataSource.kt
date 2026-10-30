package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.TelemetryEventDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    fun isSignedIn(): Boolean = supabase.auth.currentUserOrNull() != null

    suspend fun insert(events: List<TelemetryEventDto>) {
        supabase.from("telemetry_events").insert(events)
    }
}
