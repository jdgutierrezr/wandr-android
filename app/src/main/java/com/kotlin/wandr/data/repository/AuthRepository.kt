package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.data.remote.datasource.AuthRemoteDataSource
import com.kotlin.wandr.data.remote.dto.NewUserDto
import com.kotlin.wandr.domain.model.SessionState
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val sessionState: Flow<SessionState>
    fun currentUserId(): String?

    /** Creates the account and the profile row with the same id. */
    suspend fun signUp(name: String, email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val eventBus: AppEventBus,
) : AuthRepository {

    override val sessionState: Flow<SessionState> = remote.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated ->
                    status.session.user?.id?.let(SessionState::SignedIn) ?: SessionState.SignedOut
                is SessionStatus.NotAuthenticated -> SessionState.SignedOut
                is SessionStatus.Initializing -> SessionState.Loading
                // Refreshing the token failed (usually offline): the saved session is still usable
                is SessionStatus.RefreshFailure ->
                    remote.currentUserId()?.let(SessionState::SignedIn) ?: SessionState.SignedOut
            }
        }
        .distinctUntilChanged()

    override fun currentUserId(): String? = remote.currentUserId()

    override suspend fun signUp(name: String, email: String, password: String) = safeCall {
        val userId = remote.signUp(email.trim(), password)
        remote.createProfile(NewUserDto(id = userId, name = name.trim(), email = email.trim()))
        eventBus.publish(AppEvent.SignedIn)
    }

    override suspend fun signIn(email: String, password: String) = safeCall {
        remote.signIn(email.trim(), password)
        eventBus.publish(AppEvent.SignedIn)
    }

    override suspend fun signOut() = safeCall {
        remote.signOut()
        eventBus.publish(AppEvent.SignedOut)
    }
}
