package com.kotlin.wandr.core.error

sealed class AppError(open val message: String) {
    data object Network : AppError("No internet connection")

    data object EmailTaken : AppError("This email already has a Wandr account")

    data object NoSpace : AppError("There is not enough space on the phone")

    data object Unauthorized : AppError("Your session expired. Please log in again")

    data class Server(override val message: String) : AppError(message)

    data class Unknown(override val message: String = "Something went wrong") : AppError(message)
}

class AppException(val error: AppError) : Exception(error.message)

val Throwable.appError: AppError
    get() = (this as? AppException)?.error ?: AppError.Unknown(message ?: "Something went wrong")
