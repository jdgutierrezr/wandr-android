package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.data.local.CachedDropoff
import com.kotlin.wandr.data.local.DropoffCache
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toReport
import com.kotlin.wandr.data.remote.datasource.AnalyticsRemoteDataSource
import com.kotlin.wandr.domain.model.QuestDropoffReport
import com.kotlin.wandr.domain.model.RecommendationLoadTime
import com.kotlin.wandr.domain.model.StepResponseTime
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsRepository {
    /**
     * BQ8: where users abandon quests (`get_quest_dropoff`). Caching strategy: a copy saved less
     * than [AnalyticsRepositoryImpl.MAX_AGE_MS] ago is used without asking Supabase. If the
     * download fails, the last saved copy is returned marked as stale, so the hint works offline.
     */
    suspend fun questDropoff(): Result<QuestDropoffReport>

    /** Same as [questDropoff] but always tries to download first (the refresh button). */
    suspend fun refreshQuestDropoff(): Result<QuestDropoffReport>

    suspend fun recommendationLoadTime(): Result<RecommendationLoadTime>

    /** BQ2: average server time of each quest step, slowest first. */
    suspend fun stepResponseTimes(): Result<List<StepResponseTime>>
}

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val remote: AnalyticsRemoteDataSource,
    private val cache: DropoffCache,
    private val clock: Clock,
) : AnalyticsRepository {

    override suspend fun questDropoff() = dropoff(forceRefresh = false)

    override suspend fun refreshQuestDropoff() = dropoff(forceRefresh = true)

    private suspend fun dropoff(forceRefresh: Boolean): Result<QuestDropoffReport> {
        val saved = cache.read()
        val now = clock.millis()

        // Recent enough: no need to ask Supabase again
        if (!forceRefresh && saved != null && now - saved.savedAtMillis in 0 until MAX_AGE_MS) {
            return Result.success(saved.toReport(isStale = false))
        }

        val downloaded = safeCall { remote.questDropoff() }
        downloaded.getOrNull()?.let { rows ->
            val fresh = CachedDropoff(savedAtMillis = now, rows = rows)
            cache.write(fresh)
            return Result.success(fresh.toReport(isStale = false))
        }

        // Offline or the server failed: the last saved copy is better than nothing
        return if (saved != null) {
            Result.success(saved.toReport(isStale = true))
        } else {
            Result.failure(downloaded.exceptionOrNull()!!)
        }
    }

    override suspend fun recommendationLoadTime() = safeCall {
        remote.recommendationLoadTime().toDomain()
    }

    override suspend fun stepResponseTimes() = safeCall {
        remote.stepResponseTimes().map { it.toDomain() }.sortedByDescending { it.avgMs }
    }

    companion object {
        /** A saved BQ8 report is reused for 30 minutes before downloading it again. */
        const val MAX_AGE_MS = 30L * 60 * 1000
    }
}
