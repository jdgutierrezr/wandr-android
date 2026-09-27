package com.kotlin.wandr.core.strategy

import com.kotlin.wandr.core.error.AppError

/** State of data that is read through a [FetchStrategy]. */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>

    /**
     * @param isStale true when the data comes from the cache because the network was not
     * available. The UI shows "No internet connection. Showing saved activities" (QS1).
     */
    data class Success<T>(val data: T, val isStale: Boolean = false) : Resource<T>

    /** Nothing to show: the network failed and the cache is empty. */
    data class Error(val error: AppError) : Resource<Nothing>
}
