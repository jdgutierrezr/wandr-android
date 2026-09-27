package com.kotlin.wandr.core.strategy

import com.kotlin.wandr.core.network.ConnectivityObserver
import javax.inject.Inject
import javax.inject.Singleton

/** What the caller would like. The [StrategySelector] may override it when there is no network. */
enum class FetchPolicy { NETWORK_FIRST, CACHE_FIRST }

/** Picks the [FetchStrategy] for a request. Offline always means [CacheOnly]. */
@Singleton
class StrategySelector @Inject constructor(
    private val connectivity: ConnectivityObserver,
) {
    fun select(policy: FetchPolicy): FetchStrategy = when {
        !connectivity.isOnline() -> CacheOnly
        policy == FetchPolicy.CACHE_FIRST -> CacheFirst
        else -> NetworkFirst
    }
}
