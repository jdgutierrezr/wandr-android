package com.kotlin.wandr.core.event

import com.kotlin.wandr.core.di.ApplicationScope
import com.kotlin.wandr.data.local.WandrDatabase
import com.kotlin.wandr.data.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscriber of the [AppEventBus] that keeps the cache fresh. Whoever finishes a quest does not
 * need to know that the profile, badges and history must be refreshed; this class reacts to it.
 */
@Singleton
class CacheInvalidator @Inject constructor(
    private val eventBus: AppEventBus,
    private val profileRepository: ProfileRepository,
    private val database: WandrDatabase,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return

        // New XP, level, streak, badges and history
        eventBus.subscribe<AppEvent.QuestCompleted>()
            .onEach { profileRepository.refreshAll() }
            .launchIn(scope)

        // Another user must never see the previous user's cached data
        eventBus.subscribe<AppEvent.SignedOut>()
            .onEach { withContext(Dispatchers.IO) { database.clearAllTables() } }
            .launchIn(scope)
    }
}
