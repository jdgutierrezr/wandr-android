package com.kotlin.wandr.core.telemetry

import com.kotlin.wandr.core.di.ApplicationScope
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.network.ConnectivityObserver
import com.kotlin.wandr.data.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscriber of the [AppEventBus]. Saves every [AppEvent.RequestTimed] in Room and sends them to
 * Supabase in batches: when [BATCH_SIZE] are waiting, when the connection comes back, and when the
 * app goes to the background ([onAppBackground]).
 */
@Singleton
class TelemetryCollector @Inject constructor(
    private val eventBus: AppEventBus,
    private val repository: TelemetryRepository,
    private val connectivity: ConnectivityObserver,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val flushLock = Mutex()

    fun start() {
        if (!started.compareAndSet(false, true)) return

        eventBus.subscribe<AppEvent.RequestTimed>()
            .onEach { event ->
                repository.record(event)
                if (repository.pendingCount() >= BATCH_SIZE) flush()
            }
            .launchIn(scope)

        eventBus.subscribe<AppEvent.ConnectivityChanged>()
            .filter { it.isOnline }
            .onEach { flush() }
            .launchIn(scope)
    }

    fun onAppBackground() {
        scope.launch { flush() }
    }

    private suspend fun flush() {
        if (!connectivity.isOnline()) return
        // Two flushes at the same time would send the same rows twice
        flushLock.withLock { repository.flush() }
    }

    companion object {
        const val BATCH_SIZE = 20
    }
}
