package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.TelemetryEventDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/** Facade over the `telemetry_events` table (see sql/telemetry.sql in the backend repo). */
@Singleton
class TelemetryRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    fun isSignedIn(): Boolean = runCatching { supabase.requireUserId() }.isSuccess

    suspend fun insert(events: List<TelemetryEventDto>) {
        supabase.from("telemetry_events").insert(events)
    }
}
