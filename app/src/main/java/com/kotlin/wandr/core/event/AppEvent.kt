package com.kotlin.wandr.core.event

import com.kotlin.wandr.domain.model.Badge
import com.kotlin.wandr.domain.model.Tier

/** Something that happened in the app. Published on the [AppEventBus]. */
sealed interface AppEvent {

    data class ConnectivityChanged(val isOnline: Boolean) : AppEvent

    /** A measured backend call. Collected by the telemetry (BQ1 / BQ2). */
    data class RequestTimed(
        val name: String,
        val durationMs: Long,
        val success: Boolean,
        val metadata: Map<String, String> = emptyMap(),
    ) : AppEvent

    data object SignedIn : AppEvent
    data object SignedOut : AppEvent

    data class QuestStarted(val questId: String) : AppEvent
    data class QuestAbandoned(val questId: String) : AppEvent

    data class ObjectiveCompleted(
        val questId: String,
        val objectiveId: String,
        val completedObjectives: Int,
        val totalObjectives: Int,
    ) : AppEvent

    data class QuestCompleted(
        val questId: String,
        val xpEarned: Int,
        val level: Int,
        val tier: Tier,
        val currentStreak: Int,
        val newBadges: List<Badge>,
    ) : AppEvent

    data class BadgeUnlocked(val badge: Badge) : AppEvent

    data class EventJoined(val eventId: String) : AppEvent
    data class EventLeft(val eventId: String) : AppEvent

    data object FriendshipsChanged : AppEvent
}
