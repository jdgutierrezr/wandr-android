package com.kotlin.wandr.ui.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.domain.model.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Where the splash screen goes once the saved session is known. */
enum class SessionRoute { HOME, LOGIN }

/** Null while Supabase is still loading the saved session. */
fun SessionState.splashRoute(): SessionRoute? = when (this) {
    SessionState.Loading -> null
    is SessionState.SignedIn, SessionState.Reconnecting -> SessionRoute.HOME
    SessionState.SignedOut -> SessionRoute.LOGIN
}

/** The user had a session and lost it (logged out, or the token expired / was revoked). */
fun sessionEnded(previous: SessionState, current: SessionState): Boolean =
    current == SessionState.SignedOut &&
        (previous is SessionState.SignedIn || previous == SessionState.Reconnecting)

/**
 * Session gate (authentication, context-aware). Lives as long as the Activity and watches the
 * Supabase session:
 * - on start, the splash waits for the saved session and opens Home or Login ([splashRoute]);
 * - if the session ends while the app is open, the navigation sends the user back to Login, and
 *   this ViewModel publishes [AppEvent.SignedOut] so the cache of that user is cleared
 *   (CacheInvalidator), even when the session expired instead of a normal logout.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val eventBus: AppEventBus,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    /** Last state seen, to detect the change "had a session → lost it". */
    private var previous: SessionState = SessionState.Loading

    init {
        sessionState
            .onEach { current ->
                if (sessionEnded(previous, current)) eventBus.publish(AppEvent.SignedOut)
                previous = current
            }
            .launchIn(viewModelScope)
    }
}
