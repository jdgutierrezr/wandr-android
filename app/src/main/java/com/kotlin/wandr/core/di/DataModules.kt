package com.kotlin.wandr.core.di

import android.content.Context
import androidx.room.Room
import com.kotlin.wandr.BuildConfig
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.data.local.WandrDatabase
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.data.repository.AnalyticsRepositoryImpl
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.data.repository.AuthRepositoryImpl
import com.kotlin.wandr.data.repository.EventRepository
import com.kotlin.wandr.data.repository.EventRepositoryImpl
import com.kotlin.wandr.data.repository.FriendRepository
import com.kotlin.wandr.data.repository.FriendRepositoryImpl
import com.kotlin.wandr.data.repository.LocationRepository
import com.kotlin.wandr.data.repository.LocationRepositoryImpl
import com.kotlin.wandr.data.repository.NotificationRepository
import com.kotlin.wandr.data.repository.NotificationRepositoryImpl
import com.kotlin.wandr.data.repository.PlaceRepository
import com.kotlin.wandr.data.repository.PlaceRepositoryImpl
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.data.repository.ProfileRepositoryImpl
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.data.repository.QuestRepositoryImpl
import com.kotlin.wandr.data.repository.StorageRepository
import com.kotlin.wandr.data.repository.StorageRepositoryImpl
import com.kotlin.wandr.data.repository.TagRepository
import com.kotlin.wandr.data.repository.TagRepositoryImpl
import com.kotlin.wandr.data.repository.TelemetryQuestRepository
import com.kotlin.wandr.data.repository.TelemetryRepository
import com.kotlin.wandr.data.repository.TelemetryRepositoryImpl
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

    /** Singleton: one client (and one session) for the whole app. */
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
            // It is only a cache: if the schema changes, rebuilding it is enough
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideProfileDao(db: WandrDatabase) = db.profileDao()
    @Provides fun provideTagDao(db: WandrDatabase) = db.tagDao()
    @Provides fun provideQuestDao(db: WandrDatabase) = db.questDao()
    @Provides fun providePlaceDao(db: WandrDatabase) = db.placeDao()
    @Provides fun provideEventDao(db: WandrDatabase) = db.eventDao()
    @Provides fun provideTelemetryDao(db: WandrDatabase) = db.telemetryDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    @Binds abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
    @Binds abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository
    @Binds abstract fun bindPlaceRepository(impl: PlaceRepositoryImpl): PlaceRepository
    @Binds abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
    @Binds abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository
    @Binds abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository
    @Binds abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds abstract fun bindTelemetryRepository(impl: TelemetryRepositoryImpl): TelemetryRepository
    @Binds abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    companion object {
        /** Decorator: whoever asks for a [QuestRepository] gets the measured one. */
        @Provides
        @Singleton
        fun provideQuestRepository(impl: QuestRepositoryImpl, eventBus: AppEventBus): QuestRepository =
            TelemetryQuestRepository(inner = impl, eventBus = eventBus)
    }
}
