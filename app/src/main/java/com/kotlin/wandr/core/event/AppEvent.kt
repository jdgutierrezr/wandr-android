package com.kotlin.wandr.core.event

import com.kotlin.wandr.domain.model.Badge
import com.kotlin.wandr.domain.model.Tier

/** Something that happened in the app. Published on the [AppEventBus]. */
sealed interface AppEvent {

    data object SignedOut : AppEvent

    data class QuestCompleted(
        val questId: String,
        val xpEarned: Int,
        val level: Int,
        val tier: Tier,
        val currentStreak: Int,
        val newBadges: List<Badge>,
    ) : AppEvent
}
