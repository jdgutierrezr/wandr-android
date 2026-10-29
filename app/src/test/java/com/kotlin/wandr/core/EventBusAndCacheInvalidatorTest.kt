package com.kotlin.wandr.core

import app.cash.turbine.test
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.event.CacheInvalidator
import com.kotlin.wandr.data.local.WandrDatabase
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.domain.model.Tier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventBusAndCacheInvalidatorTest {

    private val bus = AppEventBus()

    @Test
    fun `subscribers only receive the event type they asked for`() = runTest {
        bus.subscribe<AppEvent.QuestCompleted>().test {
            bus.publish(AppEvent.SignedOut)
            val completed = AppEvent.QuestCompleted("q1", 100, 2, Tier.BOGOTA_SCOUT, 1, emptyList())
            bus.publish(completed)
            assertEquals(completed, awaitItem())
        }
    }

    @Test
    fun `completing a quest refreshes the profile, and signing out clears the cache`() =
        runTest(UnconfinedTestDispatcher()) {
            val profileRepository = mockk<ProfileRepository>()
            coEvery { profileRepository.refreshAll() } returns Result.success(Unit)
            val database = mockk<WandrDatabase>(relaxed = true)
            CacheInvalidator(bus, profileRepository, database, backgroundScope).start()

            bus.publish(AppEvent.QuestCompleted("q1", 100, 2, Tier.BOGOTA_SCOUT, 1, emptyList()))
            coVerify(exactly = 1) { profileRepository.refreshAll() }

            bus.publish(AppEvent.SignedOut)
            verify(timeout = 2_000, exactly = 1) { database.clearAllTables() }
        }
}
