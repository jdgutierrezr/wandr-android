package com.kotlin.wandr.core.strategy

import com.kotlin.wandr.core.error.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T : Any> Flow<Resource<T?>>.requireData(
    whenMissing: AppError = AppError.Unknown("Not found"),
): Flow<Resource<T>> = map { resource ->
    when (resource) {
        is Resource.Success -> resource.data?.let { Resource.Success(it, resource.isStale) }
            ?: Resource.Error(whenMissing)
        is Resource.Error -> resource
        Resource.Loading -> Resource.Loading
    }
}
