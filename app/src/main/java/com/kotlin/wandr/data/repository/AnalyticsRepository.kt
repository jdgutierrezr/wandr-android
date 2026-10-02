package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.remote.datasource.AnalyticsRemoteDataSource
import com.kotlin.wandr.domain.model.QuestDropoffReport
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsRepository {
    /**
     * BQ8: where users abandon quests (`get_quest_dropoff`). Always from the network: it is an
     * aggregate of every user, so a cached copy would be misleading.
     */
    suspend fun questDropoff(): Result<QuestDropoffReport>
}

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val remote: AnalyticsRemoteDataSource,
) : AnalyticsRepository {

    override suspend fun questDropoff() = safeCall {
        QuestDropoffReport(remote.questDropoff().map { it.toDomain() })
    }
}
