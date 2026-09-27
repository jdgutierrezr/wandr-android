package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.FriendshipDto
import com.kotlin.wandr.data.remote.dto.NotificationDto
import com.kotlin.wandr.data.remote.dto.TelemetryEventDto
import com.kotlin.wandr.data.remote.dto.UserSummaryDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Facade over `friendships`, the friend RPCs and the user search. */
@Singleton
class FriendRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    fun userId(): String = supabase.requireUserId()

    /** RLS only returns friendships where the user is one of the two sides. */
    suspend fun friendships(): List<FriendshipDto> =
        supabase.from("friendships")
            .select(
                Columns.raw(
                    "*, " +
                        "requester:users!friendships_user_id_1_fkey(id, name, profile_picture), " +
                        "receiver:users!friendships_user_id_2_fkey(id, name, profile_picture)"
                )
            ) { order("created_at", Order.DESCENDING) }
            .decodeList()

    suspend fun searchUsers(query: String): List<UserSummaryDto> =
        supabase.from("users")
            .select(Columns.list("id", "name", "profile_picture")) {
                filter {
                    ilike("name", "%$query%")
                    neq("id", userId())
                }
                limit(20)
            }
            .decodeList()

    suspend fun sendRequest(friendId: String): FriendshipDto =
        supabase.postgrest.rpc("send_friend_request", buildJsonObject { put("p_friend_id", friendId) }).decodeAs()

    suspend fun accept(friendshipId: String): FriendshipDto =
        supabase.postgrest.rpc("accept_friend_request", buildJsonObject { put("p_friendship_id", friendshipId) })
            .decodeAs()

    suspend fun block(friendshipId: String) {
        supabase.from("friendships").update({ set("status", "blocked") }) {
            filter { eq("id", friendshipId) }
        }
    }

    suspend fun remove(friendshipId: String) {
        supabase.from("friendships").delete { filter { eq("id", friendshipId) } }
    }
}

/** Facade over `notifications`. RLS only returns the user's own. */
@Singleton
class NotificationRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun notifications(): List<NotificationDto> =
        supabase.from("notifications")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList()

    suspend fun markAsRead(notificationId: String) {
        supabase.from("notifications").update({ set("read_status", true) }) {
            filter { eq("id", notificationId) }
        }
    }
}

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
