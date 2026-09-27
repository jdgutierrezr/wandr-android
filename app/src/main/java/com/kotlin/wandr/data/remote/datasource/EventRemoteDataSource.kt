package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.EventDto
import com.kotlin.wandr.data.remote.dto.EventIdDto
import com.kotlin.wandr.data.remote.dto.RsvpResultDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Facade over `events`, `event_attendees` and the `rsvp_event` RPC. */
@Singleton
class EventRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun upcomingEvents(now: Instant): List<EventDto> =
        supabase.from("events")
            .select(Columns.raw("*, places(name), event_attendees(count)")) {
                filter { gte("starts_at", now.toString()) }
                order("starts_at", Order.ASCENDING)
            }
            .decodeList()

    /** Ids of the events the user is going to. */
    suspend fun myEventIds(): Set<String> =
        supabase.from("event_attendees")
            .select(Columns.list("event_id")) { filter { eq("user_id", supabase.requireUserId()) } }
            .decodeList<EventIdDto>()
            .map { it.eventId }
            .toSet()

    suspend fun join(eventId: String): RsvpResultDto =
        supabase.postgrest.rpc("rsvp_event", buildJsonObject { put("p_event_id", eventId) }).decodeAs()

    suspend fun leave(eventId: String) {
        supabase.from("event_attendees").delete {
            filter {
                eq("event_id", eventId)
                eq("user_id", supabase.requireUserId())
            }
        }
    }
}
