package com.kotlin.wandr.core

import app.cash.turbine.test
import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.strategy.CacheFirst
import com.kotlin.wandr.core.strategy.CacheOnly
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.NetworkFirst
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.testutil.FakeConnectivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class FetchStrategyTest {

    /** Room stand-in: refresh writes here, the strategy observes it. */
    private val cache = MutableStateFlow<List<String>>(emptyList())
    private var refreshCalls = 0

    private suspend fun refreshOk() {
        refreshCalls++
        cache.value = listOf("fresh")
    }

    private fun refreshOffline(): Unit = throw IOException("no network")

    // ---------- NetworkFirst ----------

    @Test
    fun `network first emits loading then fresh data`() = runTest {
        cache.value = listOf("old")
        NetworkFirst.fetch(cache, ::refreshOk).test {
            assertEquals(Resource.Loading, awaitItem())
            assertEquals(Resource.Success(listOf("fresh")), awaitItem())
        }
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `network first falls back to stale cache when the network fails`() = runTest {
        cache.value = listOf("old")
        NetworkFirst.fetch(cache, ::refreshOffline).test {
            assertEquals(Resource.Loading, awaitItem())
            assertEquals(Resource.Success(listOf("old"), isStale = true), awaitItem())
        }
    }

    @Test
    fun `network first errors when the network fails and the cache is empty`() = runTest {
        NetworkFirst.fetch(cache, ::refreshOffline).test {
            assertEquals(Resource.Loading, awaitItem())
            assertEquals(Resource.Error(AppError.Network), awaitItem())
            awaitComplete()
        }
    }

    // ---------- CacheFirst ----------

    @Test
    fun `cache first does not touch the network when the cache has data`() = runTest {
        cache.value = listOf("cached")
        CacheFirst.fetch(cache, ::refreshOk).test {
            assertEquals(Resource.Success(listOf("cached")), awaitItem())
        }
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `cache first downloads when the cache is empty`() = runTest {
        CacheFirst.fetch(cache, ::refreshOk).test {
            assertEquals(Resource.Loading, awaitItem())
            assertEquals(Resource.Success(listOf("fresh")), awaitItem())
        }
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `custom isEmpty treats an empty list as valid data`() = runTest {
        CacheFirst.fetch(cache, ::refreshOk, isEmpty = { false }).test {
            assertEquals(Resource.Success(emptyList<String>()), awaitItem())
        }
        assertEquals(0, refreshCalls)
    }

    // ---------- CacheOnly ----------

    @Test
    fun `cache only marks data as stale and never refreshes`() = runTest {
        cache.value = listOf("cached")
        CacheOnly.fetch(cache, ::refreshOk).test {
            assertEquals(Resource.Success(listOf("cached"), isStale = true), awaitItem())
        }
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `cache only reports no connection when there is nothing saved`() = runTest {
        CacheOnly.fetch(cache, ::refreshOk).test {
            assertEquals(Resource.Error(AppError.Network), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `cached flow keeps emitting when Room changes (observer)`() = runTest {
        cache.value = listOf("a")
        CacheFirst.fetch(cache, ::refreshOk).test {
            assertEquals(Resource.Success(listOf("a")), awaitItem())
            cache.value = listOf("a", "b")
            assertEquals(Resource.Success(listOf("a", "b")), awaitItem())
        }
    }

    // ---------- Selector ----------

    @Test
    fun `selector uses cache only while offline, whatever the policy`() {
        val connectivity = FakeConnectivity(online = false)
        val selector = StrategySelector(connectivity)
        assertSame(CacheOnly, selector.select(FetchPolicy.NETWORK_FIRST))
        assertSame(CacheOnly, selector.select(FetchPolicy.CACHE_FIRST))

        connectivity.status.value = true
        assertSame(NetworkFirst, selector.select(FetchPolicy.NETWORK_FIRST))
        assertSame(CacheFirst, selector.select(FetchPolicy.CACHE_FIRST))
    }
}
