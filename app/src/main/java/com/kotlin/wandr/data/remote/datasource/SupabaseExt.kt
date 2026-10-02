package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.core.error.NotAuthenticatedException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** Id of the logged-in user (`me` in the backend docs). */
internal fun SupabaseClient.requireUserId(): String =
    auth.currentUserOrNull()?.id ?: throw NotAuthenticatedException()

/**
 * Like [requireUserId], but first waits for the session to settle. Use it for calls that run
 * right after coming back from another app (the camera, for example): for a moment the session
 * is being loaded or refreshed (`Initializing` / `RefreshFailure`) and [requireUserId] would
 * fail even though the user is logged in.
 */
internal suspend fun SupabaseClient.awaitUserId(): String {
    auth.currentUserOrNull()?.id?.let { return it }
    auth.awaitInitialization()
    withTimeoutOrNull(SESSION_SETTLE_TIMEOUT) {
        auth.sessionStatus.first { it is SessionStatus.Authenticated || it is SessionStatus.NotAuthenticated }
    }
    return requireUserId()
}

private val SESSION_SETTLE_TIMEOUT = 10.seconds
