package com.kotlin.wandr.data.local

import android.app.ActivityManager
import android.content.Context
import com.kotlin.wandr.data.remote.dto.QuestDropoffDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CachedDropoff(
    val savedAtMillis: Long,
    val rows: List<QuestDropoffDto>,
)

interface DropoffCache {
    suspend fun read(): CachedDropoff?

    suspend fun write(cached: CachedDropoff)
}

@Singleton
class FileDropoffCache @Inject constructor(
    @ApplicationContext private val context: Context,
) : DropoffCache {

    private val file = File(context.filesDir, FILE_NAME)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var inMemory: CachedDropoff? = null

    override suspend fun read(): CachedDropoff? {
        inMemory?.let { return it }
        val fromDisk = withContext(Dispatchers.IO) {
            runCatching {
                if (file.exists()) json.decodeFromString(CachedDropoff.serializer(), file.readText()) else null
            }.getOrNull()
        }
        fromDisk?.let { keepInMemory(it) }
        return fromDisk
    }

    override suspend fun write(cached: CachedDropoff) {
        keepInMemory(cached)
        withContext(Dispatchers.IO) {
            runCatching {
                val text = json.encodeToString(CachedDropoff.serializer(), cached)
                if (context.filesDir.usableSpace < text.length + MIN_FREE_BYTES) return@runCatching
                // Write to a temporary file first so a crash never leaves half a report
                val temporary = File(context.filesDir, "$FILE_NAME.tmp")
                temporary.writeText(text)
                temporary.renameTo(file)
            }
        }
    }

    private fun keepInMemory(cached: CachedDropoff) {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
        inMemory = if (memory.lowMemory) null else cached
    }

    private companion object {
        const val FILE_NAME = "bq8_dropoff_report.json"

        /** Leave at least 5 MB free for the rest of the app and the system. */
        const val MIN_FREE_BYTES = 5L * 1024 * 1024
    }
}
