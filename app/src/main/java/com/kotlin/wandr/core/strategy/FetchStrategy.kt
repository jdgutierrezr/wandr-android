package com.kotlin.wandr.core.strategy

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.error.appError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Strategy pattern: how a repository combines the local cache (Room) and the network (Supabase).
 *
 * Room is the single source of truth: every strategy ends up observing [local]. They only differ
 * in when they call [refresh], which downloads fresh data and saves it into Room.
 */
interface FetchStrategy {
    fun <T> fetch(
        local: Flow<T>,
        refresh: suspend () -> Unit,
        isEmpty: (T) -> Boolean = ::isEmptyDefault,
    ): Flow<Resource<T>>
}

/** Always tries the network first. If it fails, falls back to the cache and marks it stale. */
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

/** Uses the cache when it has data; only goes to the network when the cache is empty. */
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

/** Never touches the network. Used while the device is offline. */
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
