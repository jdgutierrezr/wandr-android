package com.kotlin.wandr.integration

import com.kotlin.wandr.BuildConfig
import com.kotlin.wandr.data.remote.datasource.AnalyticsRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.AuthRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.EventRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.FriendRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.LocationRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.NotificationRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.PlaceRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.ProfileRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.QuestRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.TagRemoteDataSource
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.mapper.toRows
import com.kotlin.wandr.domain.model.QuestDropoffReport
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.Instant

/**
 * Decodes real Supabase responses with our DTOs and mappers. **Read only**: it never changes data.
 *
 * Skipped by default. Run it with:
 *   WANDR_SUPABASE_SMOKE=1 ./gradlew testDebugUnitTest --tests "*SupabaseReadOnlySmokeTest*"
 */
class SupabaseReadOnlySmokeTest {

    @Test
    fun readEndpointsDecodeWithTestUser() = runBlocking {
        assumeTrue("Set WANDR_SUPABASE_SMOKE=1 to run", System.getenv("WANDR_SUPABASE_SMOKE") == "1")

        // Plain JVM: keep the session in memory instead of Android storage
        val supabase = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            install(Auth) {
                sessionManager = MemorySessionManager()
                codeVerifierCache = MemoryCodeVerifierCache()
                enableLifecycleCallbacks = false
                autoLoadFromStorage = false
                alwaysAutoRefresh = false
            }
            install(Postgrest)
        }
        AuthRemoteDataSource(supabase).signIn("valentina.gomez@example.com", "password123")

        val profile = ProfileRemoteDataSource(supabase)
        val me = profile.fetchProfile().toEntity().toDomain()
        println("profile: $me")
        assertEquals("Valentina Gomez", me.name)
        println("badges: " + profile.fetchBadges().map { it.toEntity().toDomain() })
        println("history: " + profile.fetchQuestHistory().map { it.toEntity() })

        val tags = TagRemoteDataSource(supabase)
        println("tags: " + tags.fetchTags().map { it.name })
        println("interests: " + tags.fetchInterestIds())

        val quests = QuestRemoteDataSource(supabase)
        val nearby = quests.nearbyQuests(4.6097, -74.0817, 15.0)
        println("nearby quests: " + nearby.map { "${it.title} (${it.distanceKm} km)" })
        assertTrue("seed has quests in Bogota", nearby.isNotEmpty())
        println("quest tags: " + quests.tagsFor(nearby.map { it.id }).map { it.toEntity() })
        val detail = quests.questDetail(nearby.first().id)
        println("detail: ${detail.title}, place=${detail.places?.name}, steps=" +
            detail.questObjectives.map { "${it.orderIndex}:${it.title}" } + ", tags=" + detail.questTags.map { it.tags.name })
        assertEquals(detail.questObjectives.sortedBy { it.orderIndex }, detail.questObjectives)
        println("active: " + quests.activeCompletions().map { it.toRows() })

        // BQ8: get_quest_dropoff. The seed has at least one abandoned quest (Camila, Jazz al Parque)
        val dropoff = QuestDropoffReport(AnalyticsRemoteDataSource(supabase).questDropoff().map { it.toDomain() })
        println("dropoff: worst=${dropoff.worstPoint?.label} byPosition=${dropoff.abandonsByPosition}")
        assertTrue("seed has abandoned quests", dropoff.totalAbandoned > 0)

        val places = PlaceRemoteDataSource(supabase)
        println("nearby places: " + places.nearbyPlaces(4.6097, -74.0817, 15.0, null).map { it.toEntity().toDomain() })
        println("nearby museums: " + places.nearbyPlaces(4.6097, -74.0817, 15.0, "museum").map { it.name })

        val events = EventRemoteDataSource(supabase)
        val mine = events.myEventIds()
        println("events: " + events.upcomingEvents(Instant.now()).map { it.toEntity(it.id in mine).toDomain() })

        val friends = FriendRemoteDataSource(supabase)
        println("friendships: " + friends.friendships().map { it.toDomain(friends.userId()) })
        println("search 'san': " + friends.searchUsers("san").map { it.name })

        println("notifications: " + NotificationRemoteDataSource(supabase).notifications().map { it.toDomain() })
        println("friends on map: " + LocationRemoteDataSource(supabase).friendsOnMap().map { it.toDomain() })
    }
}
