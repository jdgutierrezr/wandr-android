package com.kotlin.wandr.core.error

import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

object ErrorMapper {

    fun map(throwable: Throwable): AppError = when (throwable) {
        is AppException -> throwable.error
        is HttpRequestException, is HttpRequestTimeoutException, is IOException -> AppError.Network
        is UnauthorizedRestException -> AppError.Unauthorized
        // RPCs use `raise exception '<message>'`; PostgREST returns that text as the error
        is RestException -> AppError.Server(throwable.error)
        is NotAuthenticatedException -> AppError.Unauthorized
        else -> AppError.Unknown(throwable.message ?: "Something went wrong")
    }
}

class NotAuthenticatedException : IllegalStateException("Not authenticated")

/**
 * Runs [block] and wraps the outcome in a [Result] whose failure is always an [AppException].
 * Coroutine cancellation is rethrown so it is never swallowed.
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(AppException(ErrorMapper.map(e)))
}
