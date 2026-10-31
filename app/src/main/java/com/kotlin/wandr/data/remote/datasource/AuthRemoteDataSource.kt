package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.data.remote.dto.NewUserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    val sessionStatus: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun signUp(email: String, password: String): String {
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (e: RestException) {
            throw if (e.isEmailTaken()) AppException(AppError.EmailTaken) else e
        }
        return supabase.auth.currentUserOrNull()?.id
            ?: throw AppException(AppError.Server("Check your email to confirm your account, then log in"))
    }

    suspend fun createProfile(profile: NewUserDto) {
        try {
            supabase.from("users").insert(profile)
        } catch (e: RestException) {
            throw if (e.isEmailTaken()) AppException(AppError.EmailTaken) else e
        }
    }

    // supabase answers with codes like user_already_exists or a duplicate key on users.email
    private fun RestException.isEmailTaken(): Boolean {
        val text = "$error $message".lowercase()
        return listOf("already registered", "already_exists", "email_exists", "users_email_key")
            .any { it in text }
    }

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}
