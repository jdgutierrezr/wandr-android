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
