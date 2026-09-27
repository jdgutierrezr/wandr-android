package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.core.strategy.requireData
import com.kotlin.wandr.data.local.dao.PlaceDao
import com.kotlin.wandr.data.local.dao.QuestDao
import com.kotlin.wandr.data.mapper.tagEntities
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.mapper.toRows
import com.kotlin.wandr.data.remote.datasource.QuestRemoteDataSource
import com.kotlin.wandr.domain.model.ActiveQuest
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.domain.model.QuestDetail
import com.kotlin.wandr.domain.model.Tier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface QuestRepository {
    /** Quests around [location], closest first (Discovery Engine). */
    fun nearbyQuests(
        location: GeoPoint,
        radiusKm: Double = DEFAULT_RADIUS_KM,
        policy: FetchPolicy = FetchPolicy.NETWORK_FIRST,
    ): Flow<Resource<List<Quest>>>

    /** Quest, place and ordered steps (Quest Details). */
    fun questDetail(questId: String, policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<QuestDetail>>

    /** Quests in progress with their checked steps (Active Quest Tracker). Works offline (QS8). */
    fun activeQuests(policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<List<ActiveQuest>>>

    suspend fun startQuest(questId: String): Result<Unit>

    /** [photoPath] is the Storage path from [StorageRepository.uploadQuestPhoto], if the step needs one. */
    suspend fun completeObjective(questId: String, objectiveId: String, photoPath: String? = null): Result<ObjectiveResult>

    suspend fun abandonQuest(questId: String): Result<Unit>

    companion object {
        const val DEFAULT_RADIUS_KM = 5.0
    }
}

@Singleton
class QuestRepositoryImpl @Inject constructor(
    private val remote: QuestRemoteDataSource,
    private val questDao: QuestDao,
    private val placeDao: PlaceDao,
    private val strategies: StrategySelector,
    private val eventBus: AppEventBus,
) : QuestRepository {

    override fun nearbyQuests(location: GeoPoint, radiusKm: Double, policy: FetchPolicy) =
        strategies.select(policy).fetch(
            local = questDao.observeNearby(radiusKm).map { rows -> rows.map { it.toDomain() } },
            refresh = {
                val quests = remote.nearbyQuests(location.latitude, location.longitude, radiusKm)
                val tags = remote.tagsFor(quests.map { it.id })
                questDao.replaceNearby(quests.map { it.toEntity() }, tags.map { it.toEntity() })
            },
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun questDetail(questId: String, policy: FetchPolicy): Flow<Resource<QuestDetail>> {
        val local: Flow<QuestDetail?> = questDao.observeQuest(questId).flatMapLatest { quest ->
            if (quest == null) {
                flowOf(null)
            } else {
                combine(
                    questDao.observeObjectives(questId),
                    placeDao.observePlace(quest.quest.placeId),
                ) { objectives, place ->
                    QuestDetail(
                        quest = quest.toDomain(),
                        place = place?.toDomain(),
                        objectives = objectives.map { it.toDomain() },
                    )
                }
            }
        }
        return strategies.select(policy)
            .fetch(
                local = local,
                refresh = { refreshDetail(questId) },
                // A quest cached from the nearby list has no steps yet: that is not a full detail
                isEmpty = { it == null || it.objectives.isEmpty() },
            )
            .requireData()
    }

    override fun activeQuests(policy: FetchPolicy) = strategies.select(policy).fetch(
        local = questDao.observeActive().map { rows -> rows.map { it.toDomain() } },
        refresh = ::refreshActive,
        // "No active quests" is a valid answer, not an empty cache
        isEmpty = { false },
    )

    override suspend fun startQuest(questId: String) = safeCall {
        remote.startQuest(questId)
        refreshActive()
        eventBus.publish(AppEvent.QuestStarted(questId))
    }

    override suspend fun completeObjective(questId: String, objectiveId: String, photoPath: String?) = safeCall {
        val result = remote.completeObjective(questId, objectiveId, photoPath).toDomain()
        refreshActive()
        eventBus.publish(
            AppEvent.ObjectiveCompleted(questId, objectiveId, result.completedObjectives, result.totalObjectives)
        )
        if (result.questCompleted) {
            eventBus.publish(
                AppEvent.QuestCompleted(
                    questId = questId,
                    xpEarned = result.xpEarned,
                    level = result.level ?: 1,
                    tier = result.tier ?: Tier.BOGOTA_SCOUT,
                    currentStreak = result.currentStreak ?: 0,
                    newBadges = result.newBadges,
                )
            )
            result.newBadges.forEach { eventBus.publish(AppEvent.BadgeUnlocked(it)) }
        }
        result
    }

    override suspend fun abandonQuest(questId: String) = safeCall {
        remote.abandonQuest(questId)
        refreshActive()
        eventBus.publish(AppEvent.QuestAbandoned(questId))
    }

    private suspend fun refreshDetail(questId: String) {
        val dto = remote.questDetail(questId)
        questDao.saveDetail(
            quest = dto.toEntity(distanceKm = questDao.distanceOf(questId)),
            tags = dto.tagEntities(),
            objectives = dto.questObjectives.map { it.toEntity() },
        )
        dto.places?.let { place ->
            placeDao.upsert(listOf(place.toEntity(distanceKm = placeDao.distanceOf(place.id))))
        }
    }

    private suspend fun refreshActive() {
        val rows = remote.activeCompletions().map { it.toRows() }
        questDao.replaceActive(
            completions = rows.map { it.completion },
            objectives = rows.flatMap { it.objectives },
            done = rows.flatMap { it.done },
        )
    }
}
