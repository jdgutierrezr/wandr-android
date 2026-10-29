package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.data.local.dao.EventDao
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.remote.datasource.EventRemoteDataSource
import com.kotlin.wandr.domain.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface EventRepository {
    fun upcomingEvents(policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<List<Event>>>
}

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val remote: EventRemoteDataSource,
    private val dao: EventDao,
    private val strategies: StrategySelector,
    private val clock: Clock,
) : EventRepository {

    override fun upcomingEvents(policy: FetchPolicy) = strategies.select(policy).fetch(
        local = dao.observeUpcoming(clock.millis()).map { rows -> rows.map { it.toDomain() } },
        refresh = {
            val events = remote.upcomingEvents(clock.instant())
            val mine = remote.myEventIds()
            dao.replaceAll(events.map { it.toEntity(isAttending = it.id in mine) })
        },
    )
}
