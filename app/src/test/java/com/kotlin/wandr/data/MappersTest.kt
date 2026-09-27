package com.kotlin.wandr.data

import com.kotlin.wandr.data.local.entity.CompletionEntity
import com.kotlin.wandr.data.local.entity.CompletionWithProgress
import com.kotlin.wandr.data.local.entity.ObjectiveCompletionEntity
import com.kotlin.wandr.data.local.entity.ObjectiveEntity
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.mapper.toRows
import com.kotlin.wandr.data.remote.dto.ActiveCompletionDto
import com.kotlin.wandr.data.remote.dto.ActiveQuestInfoDto
import com.kotlin.wandr.data.remote.dto.CountDto
import com.kotlin.wandr.data.remote.dto.EventDto
import com.kotlin.wandr.data.remote.dto.FriendshipDto
import com.kotlin.wandr.data.remote.dto.ObjectiveCompletionDto
import com.kotlin.wandr.data.remote.dto.ObjectiveDto
import com.kotlin.wandr.data.remote.dto.ObjectiveResultDto
import com.kotlin.wandr.data.remote.dto.UserDto
import com.kotlin.wandr.data.remote.dto.UserSummaryDto
import com.kotlin.wandr.domain.model.EnergyLevel
import com.kotlin.wandr.domain.model.FriendshipStatus
import com.kotlin.wandr.domain.model.Tier
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MappersTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `profile keeps Postgres enums and parses them into Kotlin enums`() {
        val dto = UserDto(
            id = "u1", name = "Valentina", email = "v@example.com",
            currentXp = 1250, level = 8, currentStreak = 5, tier = "trailblazer", energyLevel = "active",
        )
        val profile = dto.toEntity().toDomain()
        assertEquals(Tier.TRAILBLAZER, profile.tier)
        assertEquals(EnergyLevel.ACTIVE, profile.energyLevel)
        assertEquals(50, profile.xpIntoLevel)
    }

    @Test
    fun `unknown enum values do not crash`() {
        val profile = UserDto(id = "u1", name = "n", email = "e", tier = "legend", energyLevel = null).toEntity().toDomain()
        assertEquals(Tier.BOGOTA_SCOUT, profile.tier)
        assertNull(profile.energyLevel)
    }

    @Test
    fun `event attendee count comes from the embedded count and timestamps keep their offset`() {
        val dto = json.decodeFromString<EventDto>(
            """
            {"id":"e1","title":"Salsa in the Park","starts_at":"2026-09-28T19:00:00+00:00",
             "ends_at":"2026-09-28T22:00:00.123456+00:00","capacity":100,
             "places":{"name":"Parque Simon Bolivar"},"event_attendees":[{"count":3}]}
            """.trimIndent()
        )
        val event = dto.toEntity(isAttending = true).toDomain()
        assertEquals(3, event.attendees)
        assertEquals(97, event.spotsLeft)
        assertEquals("Parque Simon Bolivar", event.placeName)
        assertEquals(Instant.parse("2026-09-28T19:00:00Z"), event.startsAt)
        assertTrue(event.isAttending)
    }

    @Test
    fun `event without attendees has zero`() {
        val dto = EventDto(id = "e1", title = "t", startsAt = "2026-09-28T19:00:00+00:00", attendees = listOf(CountDto(0)))
        assertEquals(0, dto.toEntity(isAttending = false).toDomain().attendees)
    }

    @Test
    fun `friendship shows the other user and only the receiver can accept`() {
        val dto = FriendshipDto(
            id = "f1", userId1 = "santiago", userId2 = "juan", status = "pending",
            createdAt = "2026-09-20T10:00:00+00:00",
            requester = UserSummaryDto("santiago", "Santiago Ramirez"),
            receiver = UserSummaryDto("juan", "Juan David Suarez"),
        )

        val forJuan = dto.toDomain(myId = "juan")
        assertEquals("Santiago Ramirez", forJuan.otherUser.name)
        assertTrue(forJuan.isIncoming)
        assertTrue(forJuan.canAccept)

        val forSantiago = dto.toDomain(myId = "santiago")
        assertEquals("Juan David Suarez", forSantiago.otherUser.name)
        assertFalse(forSantiago.canAccept)
        assertEquals(FriendshipStatus.PENDING, forSantiago.status)
    }

    @Test
    fun `active quest progress comes from the checked steps`() {
        val dto = ActiveCompletionDto(
            id = "c1", questId = "q1", status = "pending", createdAt = "2026-09-27T12:00:00+00:00",
            quests = ActiveQuestInfoDto(
                title = "Coffee crawl",
                questObjectives = listOf(
                    ObjectiveDto("o2", "q1", "Order", orderIndex = 2),
                    ObjectiveDto("o1", "q1", "Arrive", orderIndex = 1),
                    ObjectiveDto("o3", "q1", "Photo", requiresPhoto = true, orderIndex = 3),
                ),
            ),
            objectiveCompletions = listOf(ObjectiveCompletionDto("o1")),
        )
        val rows = dto.toRows()
        val active = CompletionWithProgress(rows.completion, rows.objectives, rows.done).toDomain()

        assertEquals(listOf("o1", "o2", "o3"), active.objectives.map { it.id })
        assertEquals(1, active.completedCount)
        assertEquals(3, active.totalCount)
        assertEquals("o2", active.nextObjective?.id)
    }

    @Test
    fun `objective result before the last step has no progress fields`() {
        val dto = json.decodeFromString<ObjectiveResultDto>(
            """{"completed_objectives":2,"total_objectives":3,"quest_completed":false,"xp_earned":0,
               "current_xp":null,"level":null,"tier":null,"current_streak":null,"new_badges":[]}"""
        )
        val result = dto.toDomain()
        assertFalse(result.questCompleted)
        assertNull(result.tier)
        assertNull(result.level)
    }

    @Test
    fun `objective result after the last step carries XP and badges`() {
        val dto = json.decodeFromString<ObjectiveResultDto>(
            """{"completed_objectives":3,"total_objectives":3,"quest_completed":true,"xp_earned":150,
               "current_xp":1400,"level":8,"tier":"trailblazer","current_streak":6,
               "new_badges":[{"id":"b1","name":"Explorer","icon_url":"x.png"}]}"""
        )
        val result = dto.toDomain()
        assertTrue(result.questCompleted)
        assertEquals(Tier.TRAILBLAZER, result.tier)
        assertEquals(listOf("Explorer"), result.newBadges.map { it.name })
    }

    @Test
    fun `sorted steps are rebuilt from Room rows`() {
        val completion = CompletionEntity("c1", "q1", "Quest", 0, "pending", 0L, null)
        val objectives = listOf(ObjectiveEntity("b", "q1", "B", false, 2), ObjectiveEntity("a", "q1", "A", false, 1))
        val done = listOf(ObjectiveCompletionEntity("c1", "b"))
        val active = CompletionWithProgress(completion, objectives, done).toDomain()
        assertEquals(listOf("a", "b"), active.objectives.map { it.id })
        assertEquals("a", active.nextObjective?.id)
    }
}
