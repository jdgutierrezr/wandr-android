package com.kotlin.wandr.core.strategy

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.error.appError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface FetchStrategy {
    fun <T> fetch(
        local: Flow<T>,
        refresh: suspend () -> Unit,
        isEmpty: (T) -> Boolean = ::isEmptyDefault,
    ): Flow<Resource<T>>
}

object NetworkFirst : FetchStrategy {
    override fun <T> fetch(local: Flow<T>, refresh: suspend () -> Unit, isEmpty: (T) -> Boolean): Flow<Resource<T>> = flow {
        emit(Resource.Loading)
        val failure = safeCall { refresh() }.exceptionOrNull()
        if (failure == null) {
            emitAll(local.map { Resource.Success(it) })
            return@flow
        }
        val cached = local.first()
        if (isEmpty(cached)) {
            emit(Resource.Error(failure.appError))
        } else {
            emitAll(local.map { Resource.Success(it, isStale = true) })
        }
    }
}

object CacheFirst : FetchStrategy {
    override fun <T> fetch(local: Flow<T>, refresh: suspend () -> Unit, isEmpty: (T) -> Boolean): Flow<Resource<T>> = flow {
        if (isEmpty(local.first())) {
            emit(Resource.Loading)
            val failure = safeCall { refresh() }.exceptionOrNull()
            if (failure != null) {
                emit(Resource.Error(failure.appError))
                return@flow
            }
        }
        emitAll(local.map { Resource.Success(it) })
    }
}

object CacheOnly : FetchStrategy {
    override fun <T> fetch(local: Flow<T>, refresh: suspend () -> Unit, isEmpty: (T) -> Boolean): Flow<Resource<T>> = flow {
        val cached = local.first()
        if (isEmpty(cached)) {
            emit(Resource.Error(AppError.Network))
        } else {
            emitAll(local.map { Resource.Success(it, isStale = true) })
        }
    }
}

private fun isEmptyDefault(value: Any?): Boolean = when (value) {
    null -> true
    is Collection<*> -> value.isEmpty()
    else -> false
}
