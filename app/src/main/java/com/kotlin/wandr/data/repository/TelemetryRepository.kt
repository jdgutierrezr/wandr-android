package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.data.local.dao.TelemetryDao
import com.kotlin.wandr.data.local.entity.TelemetryEventEntity
import com.kotlin.wandr.data.remote.datasource.TelemetryRemoteDataSource
import com.kotlin.wandr.data.remote.dto.TelemetryEventDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface TelemetryRepository {
    /** Saves the event locally. It is sent later, in a batch. */
    suspend fun record(event: AppEvent.RequestTimed)

    suspend fun pendingCount(): Int

    /** Sends every saved event to Supabase. Returns how many were sent. Failed ones stay saved. */
    suspend fun flush(): Result<Int>
}

@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val remote: TelemetryRemoteDataSource,
    private val dao: TelemetryDao,
    private val clock: Clock,
) : TelemetryRepository {

    override suspend fun record(event: AppEvent.RequestTimed) {
        dao.insert(
            TelemetryEventEntity(
                name = event.name,
                durationMs = event.durationMs,
                success = event.success,
                metadataJson = JsonObject(event.metadata.mapValues { JsonPrimitive(it.value) }).toString(),
                createdAtMillis = clock.millis(),
            )
        )
    }

    override suspend fun pendingCount(): Int = dao.count()

    override suspend fun flush() = safeCall {
        // RLS needs a logged-in user (user_id defaults to auth.uid())
        if (!remote.isSignedIn()) return@safeCall 0
        var sent = 0
        while (true) {
            val batch = dao.oldest(BATCH_LIMIT)
            if (batch.isEmpty()) break
            remote.insert(batch.map { it.toDto() })
            dao.delete(batch.map { it.id })
            sent += batch.size
        }
        sent
    }

    private fun TelemetryEventEntity.toDto() = TelemetryEventDto(
        eventName = name,
        durationMs = durationMs,
        success = success,
        metadata = Json.parseToJsonElement(metadataJson).jsonObject,
        appPlatform = PLATFORM,
        createdAt = Instant.ofEpochMilli(createdAtMillis).toString(),
    )

    private companion object {
        const val BATCH_LIMIT = 50
        const val PLATFORM = "android"
    }
}
