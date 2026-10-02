package com.kotlin.wandr.ui

import app.cash.turbine.test
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.domain.model.SessionState
import com.kotlin.wandr.testutil.MainDispatcherRule
import com.kotlin.wandr.ui.feature.session.SessionRoute
import com.kotlin.wandr.ui.feature.session.SessionViewModel
import com.kotlin.wandr.ui.feature.session.sessionEnded
import com.kotlin.wandr.ui.feature.session.splashRoute
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Session gate: restore the saved session on start and react when it ends. */
class SessionViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val bus = AppEventBus()
    private val session = MutableStateFlow<SessionState>(SessionState.Loading)
    private val authRepository = mockk<AuthRepository> { every { sessionState } returns session }

    @Test
    fun `the splash waits while loading, then opens Home or Login`() {
        assertNull(SessionState.Loading.splashRoute())
        assertEquals(SessionRoute.HOME, SessionState.SignedIn("u1").splashRoute())
        // Offline with a saved session: still let the user in
        assertEquals(SessionRoute.HOME, SessionState.Reconnecting.splashRoute())
        assertEquals(SessionRoute.LOGIN, SessionState.SignedOut.splashRoute())
    }

    @Test
    fun `only losing an existing session counts as the session ending`() {
        assertTrue(sessionEnded(SessionState.SignedIn("u1"), SessionState.SignedOut))
        assertTrue(sessionEnded(SessionState.Reconnecting, SessionState.SignedOut))
        // Opening the app without a saved session is not "ending" one
        assertFalse(sessionEnded(SessionState.Loading, SessionState.SignedOut))
        // A failed token refresh keeps the user in the app
        assertFalse(sessionEnded(SessionState.SignedIn("u1"), SessionState.Reconnecting))
    }

    @Test
    fun `the view model exposes the restored session`() {
        val viewModel = SessionViewModel(authRepository, bus)
        assertEquals(SessionState.Loading, viewModel.sessionState.value)

        session.value = SessionState.SignedIn("u1")
        assertEquals(SessionState.SignedIn("u1"), viewModel.sessionState.value)
    }

    @Test
    fun `an expired session publishes SignedOut so the cache is cleared`() = runTest {
        SessionViewModel(authRepository, bus)
        session.value = SessionState.SignedIn("u1")

        bus.subscribe<AppEvent.SignedOut>().test {
            session.value = SessionState.SignedOut
            assertEquals(AppEvent.SignedOut, awaitItem())
        }
    }

    @Test
    fun `starting the app signed out does not publish SignedOut`() = runTest {
        SessionViewModel(authRepository, bus)

        bus.subscribe<AppEvent.SignedOut>().test {
            session.value = SessionState.SignedOut
            expectNoEvents()
        }
    }
}
