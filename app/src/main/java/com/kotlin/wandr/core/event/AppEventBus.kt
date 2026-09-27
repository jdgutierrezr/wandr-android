package com.kotlin.wandr.core.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event-based style: publishers (repositories, decorators, connectivity) and subscribers
 * (telemetry, cache invalidation, ViewModels) only know the bus, never each other.
 */
@Singleton
class AppEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    suspend fun publish(event: AppEvent) = _events.emit(event)

    /** Non-suspending publish for callbacks. Drops the event only if 64 are already waiting. */
    fun tryPublish(event: AppEvent): Boolean = _events.tryEmit(event)

    inline fun <reified T : AppEvent> subscribe(): Flow<T> = events.filterIsInstance<T>()
}
