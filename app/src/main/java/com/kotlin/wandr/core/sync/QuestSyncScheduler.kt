package com.kotlin.wandr.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface QuestSyncScheduler {
    fun schedule()
}

@Singleton
class WorkManagerQuestSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : QuestSyncScheduler {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<QuestSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "quest-sync"
    }
}
