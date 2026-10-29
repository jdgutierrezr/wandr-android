package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.EventDto
import com.kotlin.wandr.data.remote.dto.EventIdDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun myEventIds(): Set<String> =
        supabase.from("event_attendees")
            .select(Columns.list("event_id")) { filter { eq("user_id", supabase.requireUserId()) } }
            .decodeList<EventIdDto>()
            .map { it.eventId }
            .toSet()
}
