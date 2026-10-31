package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.ErrorMapper
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.network.ConnectivityObserver
import com.kotlin.wandr.core.storage.PendingPhotoStore
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.core.strategy.requireData
import com.kotlin.wandr.core.sync.QuestSyncScheduler
import com.kotlin.wandr.core.telemetry.TelemetryEvents
import com.kotlin.wandr.data.local.dao.PendingActionDao
import com.kotlin.wandr.data.local.dao.PlaceDao
import com.kotlin.wandr.data.local.dao.QuestDao
import com.kotlin.wandr.data.local.entity.CompletionEntity
import com.kotlin.wandr.data.local.entity.ObjectiveCompletionEntity
import com.kotlin.wandr.data.local.entity.PendingActionType
import com.kotlin.wandr.data.local.entity.PendingQuestActionEntity
import com.kotlin.wandr.data.mapper.tagEntities
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.mapper.toRows
import com.kotlin.wandr.data.remote.datasource.QuestRemoteDataSource
import com.kotlin.wandr.data.remote.dto.RecommendedQuestDto
import com.kotlin.wandr.domain.model.ActiveQuest
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.domain.model.QuestDetail
import com.kotlin.wandr.domain.model.StepOutcome
import com.kotlin.wandr.domain.model.SyncResult
import com.kotlin.wandr.domain.model.Tier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeSource

interface QuestRepository {
    fun nearbyQuests(
        location: GeoPoint,
        radiusKm: Double = DEFAULT_RADIUS_KM,
        policy: FetchPolicy = FetchPolicy.NETWORK_FIRST,
    ): Flow<Resource<List<Quest>>>

    /**
     * Personalized quest recommendations based on location,
     * user interests and energy level.
     */
    suspend fun recommendedQuests(
        location: GeoPoint,
        radiusKm: Double = DEFAULT_RADIUS_KM,
        energyLevel: String = "relaxed",
    ): Result<List<RecommendedQuestDto>>

    /** Every quest the user hasn't started or finished, closest first. */
    suspend fun availableQuests(location: GeoPoint): Result<List<Quest>>

    fun questDetail(questId: String, policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<QuestDetail>>

    fun activeQuests(policy: FetchPolicy = FetchPolicy.NETWORK_FIRST): Flow<Resource<List<ActiveQuest>>>

    fun completedQuestIds(): Flow<Set<String>>

    /** Without internet it is saved on the phone and sent later. */
    suspend fun startQuest(questId: String): Result<Unit>

    suspend fun completeObjective(questId: String, objectiveId: String, photoPath: String? = null): Result<ObjectiveResult>

    /** Uploads the photo and checks the step, or saves both on the phone when offline. */
    suspend fun checkObjective(questId: String, objectiveId: String, photoJpeg: ByteArray?): Result<StepOutcome>

    /** Without internet it is saved on the phone and sent later. */
    suspend fun abandonQuest(questId: String): Result<Unit>

    fun pendingObjectiveIds(): Flow<Set<String>>

    suspend fun syncPending(): SyncResult

    companion object {
        const val DEFAULT_RADIUS_KM = 5.0
        const val ALL_QUESTS_RADIUS_KM = 20_000.0
    }
}

@Singleton
class QuestRepositoryImpl @Inject constructor(
    private val remote: QuestRemoteDataSource,
    private val questDao: QuestDao,
    private val placeDao: PlaceDao,
    private val strategies: StrategySelector,
    private val eventBus: AppEventBus,
    private val telemetry: TelemetryRepository,
    private val pendingDao: PendingActionDao,
    private val photoStore: PendingPhotoStore,
    private val storage: StorageRepository,
    private val connectivity: ConnectivityObserver,
    private val syncScheduler: QuestSyncScheduler,
    private val clock: Clock,
) : QuestRepository {

    private val syncLock = Mutex()

    override fun nearbyQuests(location: GeoPoint, radiusKm: Double, policy: FetchPolicy) =
        strategies.select(policy).fetch(
            local = questDao.observeNearby(radiusKm).map { rows -> rows.map { it.toDomain() } },
            refresh = {
                val quests = remote.nearbyQuests(location.latitude, location.longitude, radiusKm)
                val tags = remote.tagsFor(quests.map { it.id })
                questDao.replaceNearby(quests.map { it.toEntity() }, tags.map { it.toEntity() })
            },
        )

    override suspend fun availableQuests(location: GeoPoint): Result<List<Quest>> = safeCall {
        val taken = remote.takenQuestIds() + questDao.takenQuestIds()
        val quests = remote.nearbyQuests(location.latitude, location.longitude, QuestRepository.ALL_QUESTS_RADIUS_KM)
            .filterNot { it.id in taken }
        val tags = remote.tagsFor(quests.map { it.id }).groupBy({ it.questId }, { it.tags.name })
        quests.map { it.toDomain(tags[it.id].orEmpty()) }
    }

    override suspend fun recommendedQuests(location: GeoPoint, radiusKm: Double, energyLevel: String, ): Result<List<RecommendedQuestDto>> = safeCall {
        remote.recommendQuests(
            latitude = location.latitude,
            longitude = location.longitude,
            radiusKm = radiusKm,
            energyLevel = energyLevel,
        )
    }

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
                isEmpty = { it == null || it.objectives.isEmpty() },
            )
            .requireData()
    }

    override fun activeQuests(policy: FetchPolicy) = strategies.select(policy).fetch(
        local = questDao.observeActive().map { rows -> rows.map { it.toDomain() } },
        refresh = ::refreshActive,
        isEmpty = { false },
    )

    override fun completedQuestIds(): Flow<Set<String>> =
        questDao.observeHistory().map { rows -> rows.map { it.questId }.toSet() }

    override suspend fun startQuest(questId: String): Result<Unit> = safeCall {
        if (!mustQueue()) {
            try {
                remote.startQuest(questId)
                refreshActive()
                return@safeCall
            } catch (e: Exception) {
                if (!e.isNetworkError()) throw e
            }
        }
        queue(PendingQuestActionEntity(type = PendingActionType.START, questId = questId, createdAtMillis = clock.millis()))
    }

    override suspend fun completeObjective(questId: String, objectiveId: String, photoPath: String?) = safeCall {
        finishObjective(questId, objectiveId, photoPath)
    }

    override suspend fun checkObjective(questId: String, objectiveId: String, photoJpeg: ByteArray?): Result<StepOutcome> = safeCall {
        if (!mustQueue()) {
            try {
                val photoPath = photoJpeg?.let { storage.uploadQuestPhoto(questId, objectiveId, it).getOrThrow() }
                return@safeCall StepOutcome.Synced(finishObjective(questId, objectiveId, photoPath))
            } catch (e: Exception) {
                if (!e.isNetworkError()) throw e
            }
        }
        val photoFile = photoJpeg?.let { photoStore.save(it) }
        queue(
            PendingQuestActionEntity(
                type = PendingActionType.COMPLETE,
                questId = questId,
                objectiveId = objectiveId,
                photoFile = photoFile,
                createdAtMillis = clock.millis(),
            )
        )
        StepOutcome.Queued
    }

    private suspend fun finishObjective(questId: String, objectiveId: String, photoPath: String?): ObjectiveResult {
        val result = timedCompleteObjective(questId, objectiveId, photoPath).toDomain()
        refreshActive()
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
        }
        return result
    }

    private suspend fun timedCompleteObjective(questId: String, objectiveId: String, photoPath: String?) =
        TimeSource.Monotonic.markNow().let { start ->
            val response = runCatching { remote.completeObjective(questId, objectiveId, photoPath) }
            if (response.exceptionOrNull() !is CancellationException) {
                telemetry.record(
                    name = TelemetryEvents.QUEST_STEP_RESPONSE,
                    durationMs = start.elapsedNow().inWholeMilliseconds,
                    success = response.isSuccess,
                    metadata = mapOf(
                        "quest_id" to questId,
                        "objective_id" to objectiveId,
                        "with_photo" to (photoPath != null).toString(),
                    ),
                )
            }
            response.getOrThrow()
        }

    override suspend fun abandonQuest(questId: String): Result<Unit> = safeCall {
        if (!mustQueue()) {
            try {
                remote.abandonQuest(questId)
                refreshActive()
                return@safeCall
            } catch (e: Exception) {
                if (!e.isNetworkError()) throw e
            }
        }
        queue(PendingQuestActionEntity(type = PendingActionType.ABANDON, questId = questId, createdAtMillis = clock.millis()))
    }

    override fun pendingObjectiveIds(): Flow<Set<String>> =
        pendingDao.observeAll().map { actions -> actions.mapNotNull { it.objectiveId }.toSet() }

    override suspend fun syncPending(): SyncResult = syncLock.withLock {
        while (true) {
            val action = pendingDao.oldest() ?: break
            val error = safeCall { send(action) }.exceptionOrNull()?.appError
            val retryLater = error == AppError.Network || error == AppError.Unauthorized
            if (retryLater && action.attempts < MAX_ATTEMPTS) {
                pendingDao.addAttempt(action.id)
                return@withLock SyncResult.RETRY
            }
            // sent, or the server rejected it for good (e.g. "Quest is not in progress")
            pendingDao.delete(action.id)
            action.photoFile?.let { photoStore.delete(it) }
        }
        runCatching { refreshActive() }
        SyncResult.DONE
    }

    private suspend fun send(action: PendingQuestActionEntity) {
        when (action.type) {
            PendingActionType.START -> remote.startQuest(action.questId)
            PendingActionType.ABANDON -> remote.abandonQuest(action.questId)
            PendingActionType.COMPLETE -> {
                val objectiveId = action.objectiveId ?: return
                val photo = action.photoFile?.let { photoStore.read(it) }
                val photoPath = photo?.let { storage.uploadQuestPhoto(action.questId, objectiveId, it).getOrThrow() }
                finishObjective(action.questId, objectiveId, photoPath)
            }
        }
    }

    private suspend fun mustQueue() = !connectivity.isOnline() || pendingDao.count() > 0

    private suspend fun queue(action: PendingQuestActionEntity) {
        pendingDao.insert(action)
        applyLocally(action)
        syncScheduler.schedule()
    }

    private suspend fun applyLocally(action: PendingQuestActionEntity) {
        when (action.type) {
            PendingActionType.START -> {
                if (questDao.pendingCompletionId(action.questId) != null) return
                val quest = questDao.questById(action.questId) ?: return
                questDao.upsertCompletions(
                    listOf(
                        CompletionEntity(
                            id = "local-${action.questId}",
                            questId = quest.id,
                            questTitle = quest.title,
                            pointsReward = quest.pointsReward,
                            status = "pending",
                            startedAtMillis = action.createdAtMillis,
                            completedAtMillis = null,
                        )
                    )
                )
            }
            PendingActionType.COMPLETE -> {
                val completionId = questDao.pendingCompletionId(action.questId) ?: return
                val objectiveId = action.objectiveId ?: return
                questDao.upsertObjectiveCompletions(listOf(ObjectiveCompletionEntity(completionId, objectiveId)))
            }
            PendingActionType.ABANDON -> questDao.deletePendingFor(action.questId)
        }
    }

    private fun Throwable.isNetworkError() = ErrorMapper.map(this) == AppError.Network

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
        // the server does not know yet what was done offline
        pendingDao.all().forEach { applyLocally(it) }
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
    }
}
