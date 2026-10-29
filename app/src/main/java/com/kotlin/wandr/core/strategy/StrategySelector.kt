package com.kotlin.wandr.core.strategy

import com.kotlin.wandr.core.network.ConnectivityObserver
import javax.inject.Inject
import javax.inject.Singleton

enum class FetchPolicy { NETWORK_FIRST, CACHE_FIRST }

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
