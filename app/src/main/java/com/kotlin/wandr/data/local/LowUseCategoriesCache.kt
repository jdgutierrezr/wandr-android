package com.kotlin.wandr.data.local

import android.app.ActivityManager
import android.content.Context
import com.kotlin.wandr.data.remote.dto.LowUseCategoryDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File

@Serializable
data class CachedLowUseCategories(
    val savedAtMillis: Long,
    val rows: List<LowUseCategoryDto>,
)

interface LowUseCategoriesCache {
    suspend fun read(): CachedLowUseCategories?

    suspend fun write(cached: CachedLowUseCategories)
}

@Singleton
class FileLowUseCategoriesCache @Inject constructor(
    @ApplicationContext private val context: Context,
) : LowUseCategoriesCache {

    private val file = File(context.filesDir, FILE_NAME)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var inMemory: CachedLowUseCategories? = null

    override suspend fun read(): CachedLowUseCategories? {
        inMemory?.let { return it }

        val fromDisk = withContext(Dispatchers.IO) {
            runCatching {
                if (file.exists()) {
                    json.decodeFromString(
                        CachedLowUseCategories.serializer(),
                        file.readText(),
                    )
                } else {
                    null
                }
            }.getOrNull()
        }

        fromDisk?.let { keepInMemory(it) }
        return fromDisk
    }

    override suspend fun write(cached: CachedLowUseCategories) {
        keepInMemory(cached)

        withContext(Dispatchers.IO) {
            runCatching {
                val text = json.encodeToString(
                    CachedLowUseCategories.serializer(),
                    cached,
                )

                if (context.filesDir.usableSpace < text.length + MIN_FREE_BYTES) {
                    return@runCatching
                }

                val temporary = File(context.filesDir, "$FILE_NAME.tmp")
                temporary.writeText(text)
                temporary.renameTo(file)
            }
        }
    }

    private fun keepInMemory(cached: CachedLowUseCategories) {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also {
            activityManager?.getMemoryInfo(it)
        }

        inMemory = if (memory.lowMemory) null else cached
    }

    private companion object {
        const val FILE_NAME = "bq7_low_use_categories.json"
        const val MIN_FREE_BYTES = 5L * 1024 * 1024
    }
}
