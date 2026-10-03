package com.kotlin.wandr.domain.model

import java.time.Instant

/*
 * Models the ViewModels and the future UI work with.
 * They do not know about JSON (DTOs) or Room (entities); the mappers translate.
 */

data class GeoPoint(val latitude: Double, val longitude: Double)

// ---------- Users and progress ----------

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val profilePicture: String?,
    val currentXp: Int,
    val level: Int,
    val currentStreak: Int,
    val tier: Tier,
    val energyLevel: EnergyLevel?,
) {
    /** XP inside the current level (a level is 200 XP), for the progress bar. */
    val xpIntoLevel: Int get() = currentXp % XP_PER_LEVEL

    companion object {
        const val XP_PER_LEVEL = 200
    }
}

data class UserSummary(val id: String, val name: String, val profilePicture: String?)

data class Tag(val id: String, val name: String)

data class Badge(val id: String, val name: String, val description: String?, val iconUrl: String?)

data class EarnedBadge(val badge: Badge, val earnedAt: Instant)

data class QuestHistoryItem(
    val questId: String,
    val questTitle: String,
    val pointsReward: Int,
    val completedAt: Instant,
)

// ---------- Places and quests ----------

data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val address: String?,
    val location: GeoPoint,
    val averageRating: Double,
    val coverImageUrl: String?,
    val distanceKm: Double?,
)

data class Quest(
    val id: String,
    val title: String,
    val description: String?,
    val difficultyLevel: Int?,
    val estimatedDurationMin: Int?,
    val pointsReward: Int,
    val coverImageUrl: String?,
    val placeId: String,
    val placeName: String?,
    val tags: List<String>,
    val distanceKm: Double?,
)

data class RecommendedQuest(
    val quest: Quest,
    val matchedInterests: List<String>,
    val energyMatch: Boolean,
)

data class QuestObjective(
    val id: String,
    val questId: String,
    val title: String,
    val requiresPhoto: Boolean,
    val orderIndex: Int,
)

data class QuestDetail(
    val quest: Quest,
    val place: Place?,
    val objectives: List<QuestObjective>,
)

/** A quest the user started and has not finished ("pending"). */
data class ActiveQuest(
    val completionId: String,
    val questId: String,
    val questTitle: String,
    val startedAt: Instant,
    val objectives: List<QuestObjective>,
    val completedObjectiveIds: Set<String>,
) {
    val completedCount: Int get() = objectives.count { it.id in completedObjectiveIds }
    val totalCount: Int get() = objectives.size
    val nextObjective: QuestObjective? get() = objectives.firstOrNull { it.id !in completedObjectiveIds }
}

/** What `complete_objective` returns. Progress fields are null until the quest is completed. */
data class ObjectiveResult(
    val completedObjectives: Int,
    val totalObjectives: Int,
    val questCompleted: Boolean,
    val xpEarned: Int,
    val currentXp: Int?,
    val level: Int?,
    val tier: Tier?,
    val currentStreak: Int?,
    val newBadges: List<Badge>,
)

// ---------- Events ----------

data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val startsAt: Instant,
    val endsAt: Instant?,
    val capacity: Int?,
    val placeName: String?,
    val attendees: Int,
    val isAttending: Boolean,
) {
    val isFull: Boolean get() = capacity != null && attendees >= capacity
    val spotsLeft: Int? get() = capacity?.let { (it - attendees).coerceAtLeast(0) }
}

data class RsvpResult(val eventId: String, val attendees: Int, val capacity: Int?)

// ---------- Social ----------

data class Friendship(
    val id: String,
    val otherUser: UserSummary,
    val status: FriendshipStatus,
    /** True if the other person sent the request (only then can the user accept it). */
    val isIncoming: Boolean,
    val createdAt: Instant,
) {
    val canAccept: Boolean get() = status == FriendshipStatus.PENDING && isIncoming
}

data class FriendOnMap(
    val userId: String,
    val name: String,
    val profilePicture: String?,
    val location: GeoPoint,
    val updatedAt: Instant,
    val distanceKm: Double?,
    val activeQuest: String?,
)

data class Notification(
    val id: String,
    val type: NotificationType,
    val content: String?,
    val isRead: Boolean,
    val createdAt: Instant,
)
