package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.data.remote.datasource.StorageRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

interface StorageRepository {
    /**
     * Uploads the proof photo of a quest step and returns its Storage path,
     * ready to send as `p_photo_url` to `complete_objective`.
     */
    suspend fun uploadQuestPhoto(questId: String, objectiveId: String, jpegBytes: ByteArray): Result<String>
}

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val remote: StorageRemoteDataSource,
) : StorageRepository {

    override suspend fun uploadQuestPhoto(questId: String, objectiveId: String, jpegBytes: ByteArray) = safeCall {
        // The bucket policy only lets users write inside their own folder (sql/storage.sql)
        val path = "${remote.userId()}/$questId/$objectiveId.jpg"
        remote.upload(QUEST_PHOTOS_BUCKET, path, jpegBytes)
    }

    private companion object {
        const val QUEST_PHOTOS_BUCKET = "quest-photos"
    }
}
