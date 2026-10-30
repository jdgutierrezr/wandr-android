package com.kotlin.wandr.core.storage

import android.content.Context
import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Photos of steps checked offline, kept until they can be uploaded. */
interface PendingPhotoStore {
    /** Returns the file name. Fails with [AppError.NoSpace] if the phone is almost full. */
    suspend fun save(jpeg: ByteArray): String

    suspend fun read(fileName: String): ByteArray?

    suspend fun delete(fileName: String)
}

@Singleton
class FilePendingPhotoStore @Inject constructor(
    @ApplicationContext context: Context,
) : PendingPhotoStore {

    // private folder: only uninstalling the app removes it
    private val folder = File(context.filesDir, "pending_photos")

    override suspend fun save(jpeg: ByteArray): String = withContext(Dispatchers.IO) {
        folder.mkdirs()
        if (folder.usableSpace < jpeg.size + MIN_FREE_BYTES) throw AppException(AppError.NoSpace)
        val file = File(folder, "${UUID.randomUUID()}.jpg")
        file.writeBytes(jpeg)
        file.name
    }

    override suspend fun read(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        File(folder, fileName).takeIf { it.exists() }?.readBytes()
    }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) { File(folder, fileName).delete() }
    }

    private companion object {
        const val MIN_FREE_BYTES = 5L * 1024 * 1024
    }
}
