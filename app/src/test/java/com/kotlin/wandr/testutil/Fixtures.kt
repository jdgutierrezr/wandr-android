package com.kotlin.wandr.testutil

import com.kotlin.wandr.core.network.ConnectivityObserver
import com.kotlin.wandr.domain.model.ActiveQuest
import com.kotlin.wandr.domain.model.Event
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.domain.model.QuestObjective
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class FakeConnectivity(online: Boolean = true) : ConnectivityObserver {
    override val status = MutableStateFlow(online)
}

fun quest(id: String, vararg tags: String, distanceKm: Double = 1.0) = Quest(
    id = id,
    title = "Quest $id",
    description = null,
    difficultyLevel = 1,
    estimatedDurationMin = 30,
    pointsReward = 100,
    coverImageUrl = null,
    placeId = "place-$id",
    placeName = "Place $id",
    tags = tags.toList(),
    distanceKm = distanceKm,
)

fun objective(id: String, questId: String, order: Int, requiresPhoto: Boolean = false) = QuestObjective(
    id = id,
    questId = questId,
    title = "Step $order",
    requiresPhoto = requiresPhoto,
    orderIndex = order,
)

fun activeQuest(questId: String, objectives: List<QuestObjective>, done: Set<String> = emptySet()) = ActiveQuest(
    completionId = "completion-$questId",
    questId = questId,
    questTitle = "Quest $questId",
    startedAt = Instant.EPOCH,
    objectives = objectives,
    completedObjectiveIds = done,
)

fun event(id: String, attendees: Int = 0, capacity: Int? = null, isAttending: Boolean = false) = Event(
    id = id,
    title = "Event $id",
    description = null,
    coverImageUrl = null,
    startsAt = Instant.EPOCH,
    endsAt = null,
    capacity = capacity,
    placeName = null,
    attendees = attendees,
    isAttending = isAttending,
)
