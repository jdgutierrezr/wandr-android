package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.di.ApplicationScope
import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.network.ConnectivityObserver
import com.kotlin.wandr.data.local.dao.TelemetryDao
import com.kotlin.wandr.data.local.entity.TelemetryEventEntity
import com.kotlin.wandr.data.remote.datasource.TelemetryRemoteDataSource
import com.kotlin.wandr.data.remote.dto.TelemetryEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

interface TelemetryRepository {
    fun start()
    fun record(name: String, durationMs: Long, success: Boolean, metadata: Map<String, String> = emptyMap())
    fun flushInBackground()
}

@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val remote: TelemetryRemoteDataSource,
    private val dao: TelemetryDao,
    private val connectivity: ConnectivityObserver,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) : TelemetryRepository {

    private val mutex = Mutex()
    private val started = AtomicBoolean(false)

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        connectivity.status.filter { it }.onEach { flush() }.launchIn(scope)
    }

    override fun record(name: String, durationMs: Long, success: Boolean, metadata: Map<String, String>) {
        scope.launch(Dispatchers.IO) {
            val json = JsonObject(metadata.mapValues { JsonPrimitive(it.value) })
            dao.insert(
                TelemetryEventEntity(
                    name = name,
                    durationMs = durationMs,
                    success = success,
                    metadataJson = json.toString(),
                    createdAtMillis = clock.millis(),
                ),
            )
            if (dao.count() >= BATCH_TRIGGER) flush()
        }
    }

    override fun flushInBackground() {
        scope.launch { flush() }
    }

    private suspend fun flush() {
        if (!connectivity.isOnline() || !remote.isSignedIn()) return
        mutex.withLock {
            withContext(Dispatchers.IO) {
                safeCall {
                    while (true) {
                        val batch = dao.oldest(BATCH_SIZE)
                        if (batch.isEmpty()) break
                        remote.insert(batch.map { it.toDto() })
                        dao.delete(batch.map { it.id })
                    }
                }
            }
        }
    }

    private fun TelemetryEventEntity.toDto() = TelemetryEventDto(
        eventName = name,
        durationMs = durationMs,
        success = success,
        metadata = Json.parseToJsonElement(metadataJson) as JsonObject,
        appPlatform = "android",
        createdAt = Instant.ofEpochMilli(createdAtMillis).toString(),
    )

    private companion object {
        const val BATCH_TRIGGER = 20
        const val BATCH_SIZE = 50
    }
}
