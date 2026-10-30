package com.kotlin.wandr.core.di

import android.content.Context
import androidx.room.Room
import com.kotlin.wandr.BuildConfig
import com.kotlin.wandr.data.local.WandrDatabase
import com.kotlin.wandr.core.storage.FilePendingPhotoStore
import com.kotlin.wandr.core.storage.MediaStorePhotoGallery
import com.kotlin.wandr.core.storage.PendingPhotoStore
import com.kotlin.wandr.core.storage.PhotoGallery
import com.kotlin.wandr.core.sync.QuestSyncScheduler
import com.kotlin.wandr.core.sync.WorkManagerQuestSyncScheduler
import com.kotlin.wandr.data.local.DropoffCache
import com.kotlin.wandr.data.local.FileDropoffCache
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.data.repository.AnalyticsRepositoryImpl
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.data.repository.AuthRepositoryImpl
import com.kotlin.wandr.data.repository.EventRepository
import com.kotlin.wandr.data.repository.EventRepositoryImpl
import com.kotlin.wandr.data.repository.PlaceRepository
import com.kotlin.wandr.data.repository.PlaceRepositoryImpl
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.data.repository.ProfileRepositoryImpl
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.data.repository.QuestRepositoryImpl
import com.kotlin.wandr.data.repository.StorageRepository
import com.kotlin.wandr.data.repository.StorageRepositoryImpl
import com.kotlin.wandr.data.repository.TelemetryRepository
import com.kotlin.wandr.data.repository.TelemetryRepositoryImpl
import com.kotlin.wandr.data.repository.HomePreferencesRepository
import com.kotlin.wandr.data.repository.HomePreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    ) {
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WandrDatabase =
        Room.databaseBuilder(context, WandrDatabase::class.java, WandrDatabase.NAME)
            .addMigrations(WandrDatabase.MIGRATION_2_3, WandrDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideProfileDao(db: WandrDatabase) = db.profileDao()
    @Provides fun provideQuestDao(db: WandrDatabase) = db.questDao()
    @Provides fun providePlaceDao(db: WandrDatabase) = db.placeDao()
    @Provides fun provideEventDao(db: WandrDatabase) = db.eventDao()
    @Provides fun provideTelemetryDao(db: WandrDatabase) = db.telemetryDao()
    @Provides fun providePendingActionDao(db: WandrDatabase) = db.pendingActionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    @Binds abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository
    @Binds abstract fun bindPlaceRepository(impl: PlaceRepositoryImpl): PlaceRepository
    @Binds abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository
    @Binds abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository
    @Binds abstract fun bindDropoffCache(impl: FileDropoffCache): DropoffCache
    @Binds abstract fun bindPendingPhotoStore(impl: FilePendingPhotoStore): PendingPhotoStore
    @Binds abstract fun bindPhotoGallery(impl: MediaStorePhotoGallery): PhotoGallery
    @Binds abstract fun bindQuestSyncScheduler(impl: WorkManagerQuestSyncScheduler): QuestSyncScheduler
    @Binds abstract fun bindQuestRepository(impl: QuestRepositoryImpl): QuestRepository
    @Binds abstract fun bindTelemetryRepository(impl: TelemetryRepositoryImpl): TelemetryRepository
    @Binds abstract fun bindHomePreferencesRepository(impl: HomePreferencesRepositoryImpl): HomePreferencesRepository
}
