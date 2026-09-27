package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.data.local.dao.TagDao
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.remote.datasource.TagRemoteDataSource
import com.kotlin.wandr.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface TagRepository {
    /** Tags barely change, so the cache is enough once it has data. */
    fun observeTags(policy: FetchPolicy = FetchPolicy.CACHE_FIRST): Flow<Resource<List<Tag>>>

    /** Ids of the tags the user picked, from the cache. */
    fun observeInterestIds(): Flow<Set<String>>

    /** Downloads the user's interests into the cache and returns them. */
    suspend fun refreshInterests(): Result<Set<String>>

    /** Replaces the user's interests (onboarding). */
    suspend fun saveInterests(tagIds: Set<String>): Result<Unit>
}

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val remote: TagRemoteDataSource,
    private val dao: TagDao,
    private val strategies: StrategySelector,
) : TagRepository {

    override fun observeTags(policy: FetchPolicy) = strategies.select(policy).fetch(
        local = dao.observeTags().map { rows -> rows.map { it.toDomain() } },
        refresh = { dao.upsertTags(remote.fetchTags().map { it.toEntity() }) },
    )

    override fun observeInterestIds(): Flow<Set<String>> = dao.observeInterestIds().map { it.toSet() }

    override suspend fun refreshInterests() = safeCall {
        val ids = remote.fetchInterestIds()
        dao.replaceInterests(ids)
        ids.toSet()
    }

    override suspend fun saveInterests(tagIds: Set<String>) = safeCall {
        remote.replaceInterests(tagIds.toList())
        dao.replaceInterests(tagIds.toList())
    }
}
