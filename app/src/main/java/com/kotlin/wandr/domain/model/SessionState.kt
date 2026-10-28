package com.kotlin.wandr.domain.model

sealed interface SessionState {
    data object Loading : SessionState
    data class SignedIn(val userId: String) : SessionState

    /**
     * There is a saved session, but its token could not be refreshed right now (usually no
     * connection, or the app just came back from another app). The user stays in the app.
     */
    data object Reconnecting : SessionState
    data object SignedOut : SessionState
}
