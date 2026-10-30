package com.kotlin.wandr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kotlin.wandr.data.local.dao.EventDao
import com.kotlin.wandr.data.local.dao.PlaceDao
import com.kotlin.wandr.data.local.dao.ProfileDao
import com.kotlin.wandr.data.local.dao.QuestDao
import com.kotlin.wandr.data.local.dao.TelemetryDao
import com.kotlin.wandr.data.local.entity.CompletionEntity
import com.kotlin.wandr.data.local.entity.EventEntity
import com.kotlin.wandr.data.local.entity.ObjectiveCompletionEntity
import com.kotlin.wandr.data.local.entity.ObjectiveEntity
import com.kotlin.wandr.data.local.entity.PlaceEntity
import com.kotlin.wandr.data.local.entity.ProfileEntity
import com.kotlin.wandr.data.local.entity.QuestEntity
import com.kotlin.wandr.data.local.entity.QuestTagEntity
import com.kotlin.wandr.data.local.entity.TelemetryEventEntity
import com.kotlin.wandr.data.local.entity.UserBadgeEntity

@Database(
    entities = [
        ProfileEntity::class,
        UserBadgeEntity::class,
        QuestEntity::class,
        QuestTagEntity::class,
        ObjectiveEntity::class,
        CompletionEntity::class,
        ObjectiveCompletionEntity::class,
        PlaceEntity::class,
        EventEntity::class,
        TelemetryEventEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class WandrDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun questDao(): QuestDao
    abstract fun placeDao(): PlaceDao
    abstract fun eventDao(): EventDao
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        const val NAME = "wandr.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `telemetry_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `success` INTEGER NOT NULL, `metadataJson` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL)",
                )
            }
        }
    }
}
