package com.kotlin.wandr.data.cache

import android.os.SystemClock
import com.kotlin.wandr.data.remote.dto.RecommendedQuestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MysteryQuestCache @Inject constructor() {

    private data class CacheKey(
        val energyLevel: String,
        val radiusKm: Double,
    )

    private data class CacheEntry(
        val recommendations: List<RecommendedQuestDto>,
        val savedAt: Long,
    )

    private val entries = mutableMapOf<CacheKey, CacheEntry>()

    @Synchronized
    fun getFresh(
        energyLevel: String,
        radiusKm: Double,
    ): List<RecommendedQuestDto>? {
        val entry = entries[CacheKey(energyLevel, radiusKm)]
            ?: return null

        val elapsed = SystemClock.elapsedRealtime() - entry.savedAt

        return entry.recommendations.takeIf {
            elapsed < CACHE_TTL_MS
        }
    }

    @Synchronized
    fun getLatest(
        energyLevel: String,
        radiusKm: Double,
    ): List<RecommendedQuestDto>? {
        return entries[CacheKey(energyLevel, radiusKm)]?.recommendations
    }

    @Synchronized
    fun put(
        energyLevel: String,
        radiusKm: Double,
        recommendations: List<RecommendedQuestDto>,
    ) {
        entries[CacheKey(energyLevel, radiusKm)] = CacheEntry(
            recommendations = recommendations.toList(),
            savedAt = SystemClock.elapsedRealtime(),
        )
    }

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
    }
}
