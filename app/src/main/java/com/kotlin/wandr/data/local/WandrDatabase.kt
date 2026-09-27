package com.kotlin.wandr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kotlin.wandr.data.local.dao.EventDao
import com.kotlin.wandr.data.local.dao.PlaceDao
import com.kotlin.wandr.data.local.dao.ProfileDao
import com.kotlin.wandr.data.local.dao.QuestDao
import com.kotlin.wandr.data.local.dao.TagDao
import com.kotlin.wandr.data.local.dao.TelemetryDao
import com.kotlin.wandr.data.local.entity.CompletionEntity
import com.kotlin.wandr.data.local.entity.EventEntity
import com.kotlin.wandr.data.local.entity.ObjectiveCompletionEntity
import com.kotlin.wandr.data.local.entity.ObjectiveEntity
import com.kotlin.wandr.data.local.entity.PlaceEntity
import com.kotlin.wandr.data.local.entity.ProfileEntity
import com.kotlin.wandr.data.local.entity.QuestEntity
import com.kotlin.wandr.data.local.entity.QuestTagEntity
import com.kotlin.wandr.data.local.entity.TagEntity
import com.kotlin.wandr.data.local.entity.TelemetryEventEntity
import com.kotlin.wandr.data.local.entity.UserBadgeEntity
import com.kotlin.wandr.data.local.entity.UserInterestEntity

/** Local cache. Room is the single source of truth for everything the app reads offline. */
@Database(
    entities = [
        ProfileEntity::class,
        UserBadgeEntity::class,
        TagEntity::class,
        UserInterestEntity::class,
        QuestEntity::class,
        QuestTagEntity::class,
        ObjectiveEntity::class,
        CompletionEntity::class,
        ObjectiveCompletionEntity::class,
        PlaceEntity::class,
        EventEntity::class,
        TelemetryEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class WandrDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun tagDao(): TagDao
    abstract fun questDao(): QuestDao
    abstract fun placeDao(): PlaceDao
    abstract fun eventDao(): EventDao
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        const val NAME = "wandr.db"
    }
}
