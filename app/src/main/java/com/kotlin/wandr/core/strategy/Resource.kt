package com.kotlin.wandr.core.strategy

import com.kotlin.wandr.core.error.AppError

sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>

    data class Success<T>(val data: T, val isStale: Boolean = false) : Resource<T>

    data class Error(val error: AppError) : Resource<Nothing>
}
