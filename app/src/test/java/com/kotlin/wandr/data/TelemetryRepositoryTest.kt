package com.kotlin.wandr.data

import com.kotlin.wandr.data.local.dao.TelemetryDao
import com.kotlin.wandr.data.local.entity.TelemetryEventEntity
import com.kotlin.wandr.data.remote.datasource.TelemetryRemoteDataSource
import com.kotlin.wandr.data.repository.TelemetryRepositoryImpl
import com.kotlin.wandr.testutil.FakeConnectivity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TelemetryRepositoryTest {

    private val dao = mockk<TelemetryDao>(relaxed = true)
    private val remote = mockk<TelemetryRemoteDataSource>(relaxed = true)
    private val clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
    private val scope = TestScope(StandardTestDispatcher())

    private fun repository(online: Boolean = true): TelemetryRepositoryImpl {
        every { remote.isSignedIn() } returns true
        return TelemetryRepositoryImpl(remote, dao, FakeConnectivity(online), clock, scope)
    }

    private fun pending(id: Long) = TelemetryEventEntity(
        id = id,
        name = "event",
        durationMs = 10,
        success = true,
        metadataJson = "{}",
        createdAtMillis = 0,
    )

    @Test
    fun `record saves the event in the dao`() {
        repository().record("event", 120, true, mapOf("from_cache" to "false"))
        scope.testScheduler.advanceUntilIdle()

        coVerify(timeout = 2000) {
            dao.insert(match { it.name == "event" && it.durationMs == 120L && it.metadataJson == """{"from_cache":"false"}""" })
        }
    }

    @Test
    fun `with network and 20 pending events it uploads and deletes them`() {
        val batch = (1L..20L).map { pending(it) }
        coEvery { dao.count() } returns 20
        coEvery { dao.oldest(any()) } returnsMany listOf(batch, emptyList())

        repository().record("event", 120, true)
        scope.testScheduler.advanceUntilIdle()

        coVerify(timeout = 2000) { remote.insert(match { it.size == 20 }) }
        coVerify(timeout = 2000) { dao.delete((1L..20L).toList()) }
    }

    @Test
    fun `without network nothing is uploaded`() {
        coEvery { dao.count() } returns 20

        repository(online = false).record("event", 120, true)
        scope.testScheduler.advanceUntilIdle()

        coVerify(timeout = 2000) { dao.insert(any()) }
        Thread.sleep(300)
        coVerify(exactly = 0) { remote.insert(any()) }
    }
}
