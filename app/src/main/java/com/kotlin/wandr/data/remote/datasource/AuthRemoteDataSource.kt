package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.data.remote.dto.NewUserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
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
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return supabase.auth.currentUserOrNull()?.id
            ?: throw AppException(AppError.Server("Check your email to confirm your account, then log in"))
    }

    suspend fun createProfile(profile: NewUserDto) {
        supabase.from("users").insert(profile)
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
