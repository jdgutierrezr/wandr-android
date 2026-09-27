package com.kotlin.wandr.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kotlin.wandr.BuildConfig
import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.ErrorMapper
import com.kotlin.wandr.data.remote.datasource.AuthRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.EventRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.QuestRemoteDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Runs the main flows against the real Supabase project with the seed user Valentina.
 *
 * **This test changes data** (starts and abandons a quest, joins and leaves an event).
 * Afterwards, run clean_database.sql and the other SQL files again to reset the seed.
 */
@RunWith(AndroidJUnit4::class)
class SupabaseIntegrationTest {

    private lateinit var supabase: SupabaseClient

    @Before
    fun signIn() = runTest {
        supabase = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            install(Auth)
            install(Postgrest)
        }
        AuthRemoteDataSource(supabase).signIn("valentina.gomez@example.com", "password123")
    }

    @Test
    fun startCheckAndAbandonAQuest() = runTest {
        val quests = QuestRemoteDataSource(supabase)
        val nearby = quests.nearbyQuests(4.6097, -74.0817, 20.0)
        assertTrue("seed has quests in Bogota", nearby.isNotEmpty())

        // Valentina already finished some quests; use the first one she can start
        val started = nearby.firstNotNullOfOrNull { quest ->
            runCatching { quests.startQuest(quest.id) }.getOrNull()?.let { quest.id }
        } ?: error("No quest could be started")

        val steps = quests.questDetail(started).questObjectives.sortedBy { it.orderIndex }
        val firstStep = steps.first()
        // Checking the only step would complete the quest (and give XP); skip it then
        if (steps.size > 1 && !firstStep.requiresPhoto) {
            val result = quests.completeObjective(started, firstStep.id, photoPath = null)
            assertTrue(result.completedObjectives >= 1)
            assertEquals(false, result.questCompleted)
        }
        assertTrue(quests.activeCompletions().any { it.questId == started })

        quests.abandonQuest(started)
        assertTrue(quests.activeCompletions().none { it.questId == started })

        // Backend rules come back as readable messages
        val error = runCatching { quests.abandonQuest(started) }.exceptionOrNull()!!
        assertEquals(AppError.Server("Quest is not in progress"), ErrorMapper.map(error))
    }

    @Test
    fun joinAndLeaveAnEvent() = runTest {
        val events = EventRemoteDataSource(supabase)
        val mine = events.myEventIds()
        val event = events.upcomingEvents(Instant.now()).first { event ->
            event.id !in mine && (event.capacity == null || (event.attendees.firstOrNull()?.count ?: 0) < event.capacity)
        }

        val joined = events.join(event.id)
        assertTrue(event.id in events.myEventIds())

        events.leave(event.id)
        assertTrue(event.id !in events.myEventIds())
        assertEquals(event.id, joined.eventId)
    }
}
