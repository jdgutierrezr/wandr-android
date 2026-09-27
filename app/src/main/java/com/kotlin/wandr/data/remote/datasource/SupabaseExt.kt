package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.core.error.NotAuthenticatedException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/** Id of the logged-in user (`me` in the backend docs). */
internal fun SupabaseClient.requireUserId(): String =
    auth.currentUserOrNull()?.id ?: throw NotAuthenticatedException()
