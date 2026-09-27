package com.kotlin.wandr.domain.model

sealed interface SessionState {
    /** Supabase is still loading the saved session. */
    data object Loading : SessionState
    data class SignedIn(val userId: String) : SessionState
    data object SignedOut : SessionState
}
