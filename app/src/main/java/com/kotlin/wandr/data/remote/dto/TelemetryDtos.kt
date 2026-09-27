package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Body of the `telemetry_events` insert. `user_id` is filled by the database (auth.uid()). */
@Serializable
data class TelemetryEventDto(
    @SerialName("event_name") val eventName: String,
    @SerialName("duration_ms") val durationMs: Long,
    val success: Boolean,
    val metadata: JsonObject,
    @SerialName("app_platform") val appPlatform: String,
    @SerialName("created_at") val createdAt: String,
)
