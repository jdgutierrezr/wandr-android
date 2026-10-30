package com.kotlin.wandr.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.domain.model.SyncResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Sends the quest actions saved offline, in order, once there is internet. */
class QuestSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun questRepository(): QuestRepository
    }

    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).questRepository()
        return when (repository.syncPending()) {
            SyncResult.DONE -> Result.success()
            SyncResult.RETRY -> Result.retry()
        }
    }
}
