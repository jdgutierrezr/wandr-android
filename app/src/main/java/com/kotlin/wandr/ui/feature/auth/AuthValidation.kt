package com.kotlin.wandr.ui.feature.auth

object AuthValidation {
    const val MAX_NAME = 50
    const val MAX_EMAIL = 254
    const val MAX_PASSWORD = 64
    const val MIN_PASSWORD = 6

    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun nameError(name: String): String? =
        if (name.isBlank()) "Enter your name" else null

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email"
        !EMAIL_REGEX.matches(email.trim()) -> "Enter a valid email"
        else -> null
    }

    fun passwordError(password: String): String? =
        if (password.length < MIN_PASSWORD) "The password needs at least $MIN_PASSWORD characters" else null
}
