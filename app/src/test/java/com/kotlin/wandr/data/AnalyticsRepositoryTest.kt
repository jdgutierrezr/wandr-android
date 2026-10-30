package com.kotlin.wandr.data

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.AppException
import com.kotlin.wandr.data.local.CachedDropoff
import com.kotlin.wandr.data.local.DropoffCache
import com.kotlin.wandr.data.remote.datasource.AnalyticsRemoteDataSource
import com.kotlin.wandr.data.remote.dto.QuestDropoffDto
import com.kotlin.wandr.data.repository.AnalyticsRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AnalyticsRepositoryTest {

    /** Keeps the copy in a variable instead of a file. */
    private class FakeDropoffCache(var saved: CachedDropoff? = null) : DropoffCache {
        override suspend fun read() = saved
        override suspend fun write(cached: CachedDropoff) { saved = cached }
    }

    private class FakeClock(var now: Long = 0) : Clock() {
        override fun millis() = now
        override fun instant(): Instant = Instant.ofEpochMilli(now)
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?) = this
    }

    private val remote = mockk<AnalyticsRemoteDataSource>()
    private val cache = FakeDropoffCache()
    private val clock = FakeClock(now = 1_000_000)
    private val repository = AnalyticsRepositoryImpl(remote, cache, clock)

    private fun row(abandons: Int) = QuestDropoffDto("q1", "Quest q1", 3, 1, "Step 1", abandons)

    @Test
    fun `the first time it downloads the report and saves it`() = runTest {
        coEvery { remote.questDropoff() } returns listOf(row(2))

        val report = repository.questDropoff().getOrThrow()

        assertEquals(2, report.totalAbandoned)
        assertFalse(report.isStale)
        assertEquals(clock.now, cache.saved!!.savedAtMillis)
    }

    @Test
    fun `a recent copy is used without asking Supabase`() = runTest {
        cache.saved = CachedDropoff(savedAtMillis = clock.now, rows = listOf(row(2)))
        clock.now += 10 * 60 * 1000 // 10 minutes later

        val report = repository.questDropoff().getOrThrow()

        assertEquals(2, report.totalAbandoned)
        coVerify(exactly = 0) { remote.questDropoff() }
    }

    @Test
    fun `an old copy is downloaded again`() = runTest {
        cache.saved = CachedDropoff(savedAtMillis = clock.now, rows = listOf(row(2)))
        clock.now += AnalyticsRepositoryImpl.MAX_AGE_MS
        coEvery { remote.questDropoff() } returns listOf(row(5))

        val report = repository.questDropoff().getOrThrow()

        assertEquals(5, report.totalAbandoned)
        assertEquals(clock.now, cache.saved!!.savedAtMillis)
    }

    @Test
    fun `offline it returns the last copy marked as stale`() = runTest {
        cache.saved = CachedDropoff(savedAtMillis = 0, rows = listOf(row(2)))
        coEvery { remote.questDropoff() } throws IOException("no network")

        val report = repository.questDropoff().getOrThrow()

        assertEquals(2, report.totalAbandoned)
        assertTrue(report.isStale)
        assertEquals(0L, report.updatedAtMillis)
    }

    @Test
    fun `offline without a saved copy it fails with a network error`() = runTest {
        coEvery { remote.questDropoff() } throws IOException("no network")

        val error = repository.questDropoff().exceptionOrNull()

        assertEquals(AppError.Network, (error as AppException).error)
    }

    @Test
    fun `refresh always tries to download, even with a recent copy`() = runTest {
        cache.saved = CachedDropoff(savedAtMillis = clock.now, rows = listOf(row(2)))
        coEvery { remote.questDropoff() } returns listOf(row(7))

        val report = repository.refreshQuestDropoff().getOrThrow()

        assertEquals(7, report.totalAbandoned)
        coVerify(exactly = 1) { remote.questDropoff() }
    }
}
