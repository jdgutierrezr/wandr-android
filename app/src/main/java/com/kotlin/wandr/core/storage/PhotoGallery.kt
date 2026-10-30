package com.kotlin.wandr.core.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** The phone's public gallery. Only used when the user chooses it. */
interface PhotoGallery {
    suspend fun save(jpeg: ByteArray): Boolean
}

@Singleton
class MediaStorePhotoGallery @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoGallery {

    override suspend fun save(jpeg: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "wandr_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wandr")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { it.write(jpeg) } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }
}
