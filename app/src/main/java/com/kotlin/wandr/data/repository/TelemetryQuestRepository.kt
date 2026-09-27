package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.telemetry.TelemetryEvents
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.Quest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.TimeSource

/**
 * Decorator pattern: wraps the real [QuestRepository] and measures response times for the
 * business questions, without touching the real repository's code.
 *
 * - BQ1: time until quest recommendations are ready (`nearbyQuests`).
 * - BQ2: response time of each quest step (`completeObjective`).
 *
 * Every other method is delegated as is (`by inner`).
 */
class TelemetryQuestRepository(
    private val inner: QuestRepository,
    private val eventBus: AppEventBus,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : QuestRepository by inner {

    override fun nearbyQuests(location: GeoPoint, radiusKm: Double, policy: FetchPolicy): Flow<Resource<List<Quest>>> =
        flow {
            val mark = timeSource.markNow()
            var reported = false
            inner.nearbyQuests(location, radiusKm, policy).collect { resource ->
                // The first real answer (not Loading) is what the user waited for
                if (!reported && resource !is Resource.Loading) {
                    reported = true
                    eventBus.publish(
                        AppEvent.RequestTimed(
                            name = TelemetryEvents.QUEST_RECOMMENDATIONS_LOAD,
                            durationMs = mark.elapsedNow().inWholeMilliseconds,
                            success = resource is Resource.Success,
                            metadata = buildMap {
                                put("radius_km", radiusKm.toString())
                                put("policy", policy.name)
                                if (resource is Resource.Success) {
                                    put("result_count", resource.data.size.toString())
                                    put("from_cache", resource.isStale.toString())
                                }
                            },
                        )
                    )
                }
                emit(resource)
            }
        }

    override suspend fun completeObjective(questId: String, objectiveId: String, photoPath: String?): Result<ObjectiveResult> {
        val mark = timeSource.markNow()
        val result = inner.completeObjective(questId, objectiveId, photoPath)
        eventBus.publish(
            AppEvent.RequestTimed(
                name = TelemetryEvents.QUEST_STEP_RESPONSE,
                durationMs = mark.elapsedNow().inWholeMilliseconds,
                success = result.isSuccess,
                metadata = mapOf(
                    "quest_id" to questId,
                    "objective_id" to objectiveId,
                    "with_photo" to (photoPath != null).toString(),
                ),
            )
        )
        return result
    }
}
