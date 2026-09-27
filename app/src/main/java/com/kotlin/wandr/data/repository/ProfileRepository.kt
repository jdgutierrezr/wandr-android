package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.core.strategy.requireData
import com.kotlin.wandr.data.local.dao.ProfileDao
import com.kotlin.wandr.data.local.dao.QuestDao
import com.kotlin.wandr.data.local.entity.ProfileEntity
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.mapper.toHistoryItem
import com.kotlin.wandr.data.remote.datasource.AuthRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.ProfileRemoteDataSource
import com.kotlin.wandr.domain.model.EarnedBadge
import com.kotlin.wandr.domain.model.EnergyLevel
import com.kotlin.wandr.domain.model.QuestHistoryItem
import com.kotlin.wandr.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileRepository {
    fun observeProfile(policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<UserProfile>>
    fun observeBadges(policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<List<EarnedBadge>>>
    fun observeQuestHistory(policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<List<QuestHistoryItem>>>

    /** Downloads the profile into the cache and returns it. */
    suspend fun refreshProfile(): Result<UserProfile>

    suspend fun updateEnergyLevel(level: EnergyLevel): Result<Unit>

    /** Downloads the profile, badges and history again (after finishing a quest, for example). */
    suspend fun refreshAll(): Result<Unit>
}

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val remote: ProfileRemoteDataSource,
    private val auth: AuthRemoteDataSource,
    private val profileDao: ProfileDao,
    private val questDao: QuestDao,
    private val strategies: StrategySelector,
) : ProfileRepository {

    override fun observeProfile(policy: FetchPolicy): Flow<Resource<UserProfile>> {
        val userId = auth.currentUserId() ?: return flowOf(Resource.Error(AppError.Unauthorized))
        return strategies.select(policy)
            .fetch(
                local = profileDao.observeProfile(userId).map { it?.toDomain() },
                refresh = { downloadProfile() },
            )
            .requireData()
    }

    override fun observeBadges(policy: FetchPolicy) = strategies.select(policy).fetch(
        local = profileDao.observeBadges().map { rows -> rows.map { it.toDomain() } },
        refresh = ::refreshBadges,
    )

    override fun observeQuestHistory(policy: FetchPolicy) = strategies.select(policy).fetch(
        local = questDao.observeHistory().map { rows -> rows.map { it.toHistoryItem() } },
        refresh = ::refreshHistory,
    )

    override suspend fun refreshProfile() = safeCall { downloadProfile().toDomain() }

    override suspend fun updateEnergyLevel(level: EnergyLevel) = safeCall {
        remote.updateEnergyLevel(level.apiValue)
        downloadProfile()
        Unit
    }

    override suspend fun refreshAll() = safeCall {
        downloadProfile()
        refreshBadges()
        refreshHistory()
    }

    private suspend fun downloadProfile(): ProfileEntity {
        val profile = remote.fetchProfile().toEntity()
        profileDao.upsertProfile(profile)
        return profile
    }

    private suspend fun refreshBadges() {
        profileDao.replaceBadges(remote.fetchBadges().map { it.toEntity() })
    }

    private suspend fun refreshHistory() {
        questDao.replaceHistory(remote.fetchQuestHistory().map { it.toEntity() })
    }
}
