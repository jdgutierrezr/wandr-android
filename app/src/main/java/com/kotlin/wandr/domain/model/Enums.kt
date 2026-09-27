package com.kotlin.wandr.domain.model

/**
 * Postgres enums. [apiValue] is the exact text Supabase sends and expects.
 * Unknown values fall back to a safe default, so a new backend value never crashes the app.
 */

enum class Tier(val apiValue: String, val displayName: String) {
    BOGOTA_SCOUT("bogota_scout", "Bogota Scout"),
    TRAILBLAZER("trailblazer", "Trailblazer"),
    PATHFINDER("pathfinder", "Pathfinder"),
    MASTER_PATHFINDER("master_pathfinder", "Master Pathfinder");

    companion object {
        fun fromApi(value: String?) = entries.firstOrNull { it.apiValue == value } ?: BOGOTA_SCOUT
    }
}

enum class EnergyLevel(val apiValue: String) {
    RELAXED("relaxed"),
    ACTIVE("active");

    companion object {
        fun fromApi(value: String?) = entries.firstOrNull { it.apiValue == value }
    }
}

enum class PlaceCategory(val apiValue: String) {
    PARK("park"),
    BAR("bar"),
    MUSEUM("museum"),
    RESTAURANT("restaurant"),
    EVENT("event"),
    CULTURE("culture"),
    OUTDOORS("outdoors"),
    OTHER("other");

    companion object {
        fun fromApi(value: String?) = entries.firstOrNull { it.apiValue == value } ?: OTHER
    }
}

enum class QuestStatus(val apiValue: String) {
    PENDING("pending"),
    COMPLETED("completed"),
    ABANDONED("abandoned");

    companion object {
        fun fromApi(value: String?) = entries.firstOrNull { it.apiValue == value } ?: PENDING
    }
}

enum class FriendshipStatus(val apiValue: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    BLOCKED("blocked");

    companion object {
        fun fromApi(value: String?) = entries.firstOrNull { it.apiValue == value } ?: PENDING
    }
}

enum class NotificationType(val apiValue: String) {
    EVENT_REMINDER("event_reminder"),
    NEARBY_NEW_QUEST("nearby_new_quest"),
    COMPLETED_FRIEND_QUEST("completed_friend_quest"),
    UNLOCKED_BADGE("unlocked_badge"),
    FRIEND_REQUEST("friend_request"),
    UNKNOWN("unknown");

    companion object {
        fun fromApi(value: String?) = entries.firstOrNull { it.apiValue == value } ?: UNKNOWN
    }
}
