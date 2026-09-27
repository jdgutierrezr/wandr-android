package com.kotlin.wandr.core.error

/**
 * Errors the UI knows how to show. Repositories never leak Supabase or Ktor exceptions.
 */
sealed class AppError(open val message: String) {
    /** No connection, timeout or DNS failure. */
    data object Network : AppError("No internet connection")

    /** Not logged in, or the session expired. */
    data object Unauthorized : AppError("Your session expired. Please log in again")

    /** The backend rejected the request. [message] comes from the RPC, e.g. "This event is full". */
    data class Server(override val message: String) : AppError(message)

    data class Unknown(override val message: String = "Something went wrong") : AppError(message)
}

/** Carries an [AppError] inside a [Result] failure. */
class AppException(val error: AppError) : Exception(error.message)

/** The [AppError] behind a failed [Result], or [AppError.Unknown] if it was not mapped. */
val Throwable.appError: AppError
    get() = (this as? AppException)?.error ?: AppError.Unknown(message ?: "Something went wrong")
