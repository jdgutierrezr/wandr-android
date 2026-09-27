package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.data.local.dao.EventDao
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.remote.datasource.EventRemoteDataSource
import com.kotlin.wandr.domain.model.Event
import com.kotlin.wandr.domain.model.RsvpResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface EventRepository {
    /** Upcoming events with how many people are going and whether the user is one of them. */
    fun upcomingEvents(policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<List<Event>>>

    /** Fails with the RPC message if the event is full or over ("This event is full"). */
    suspend fun join(eventId: String): Result<RsvpResult>

    suspend fun leave(eventId: String): Result<Unit>
}

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val remote: EventRemoteDataSource,
    private val dao: EventDao,
    private val strategies: StrategySelector,
    private val eventBus: AppEventBus,
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

    override suspend fun join(eventId: String) = safeCall {
        val result = remote.join(eventId).toDomain()
        dao.updateAttendance(eventId, attendees = result.attendees, isAttending = true)
        eventBus.publish(AppEvent.EventJoined(eventId))
        result
    }

    override suspend fun leave(eventId: String) = safeCall {
        remote.leave(eventId)
        dao.find(eventId)?.let { cached ->
            if (cached.isAttending) {
                dao.updateAttendance(eventId, attendees = (cached.attendees - 1).coerceAtLeast(0), isAttending = false)
            }
        }
        eventBus.publish(AppEvent.EventLeft(eventId))
    }
}
