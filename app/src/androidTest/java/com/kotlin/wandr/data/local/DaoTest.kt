package com.kotlin.wandr.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kotlin.wandr.data.local.entity.CompletionEntity
import com.kotlin.wandr.data.local.entity.EventEntity
import com.kotlin.wandr.data.local.entity.ObjectiveCompletionEntity
import com.kotlin.wandr.data.local.entity.ObjectiveEntity
import com.kotlin.wandr.data.local.entity.QuestEntity
import com.kotlin.wandr.data.local.entity.QuestTagEntity
import com.kotlin.wandr.data.local.entity.TelemetryEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: WandrDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WandrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun quest(id: String, distanceKm: Double?) =
        QuestEntity(id, "p1", "Place", "Quest $id", null, 1, 30, 100, null, distanceKm)

    @Test
    fun nearbyQuestsAreSortedAndOldResultsDisappear() = runTest {
        val dao = db.questDao()
        dao.replaceNearby(
            listOf(quest("far", 4.0), quest("near", 0.5), quest("out", 9.0)),
            listOf(QuestTagEntity("near", "food"), QuestTagEntity("near", "music")),
        )
        val first = dao.observeNearby(radiusKm = 5.0).first()
        assertEquals(listOf("near", "far"), first.map { it.quest.id })
        assertEquals(setOf("food", "music"), first.first().tags.map { it.tagName }.toSet())

        // A new location: "far" is no longer nearby
        dao.replaceNearby(listOf(quest("new", 1.0)), emptyList())
        assertEquals(listOf("new"), dao.observeNearby(5.0).first().map { it.quest.id })
    }

    @Test
    fun activeQuestKeepsItsCheckedSteps() = runTest {
        val dao = db.questDao()
        dao.replaceActive(
            completions = listOf(CompletionEntity("c1", "q1", "Coffee crawl", 0, "pending", 1L, null)),
            objectives = listOf(
                ObjectiveEntity("o1", "q1", "Arrive", false, 1),
                ObjectiveEntity("o2", "q1", "Order", false, 2),
            ),
            done = listOf(ObjectiveCompletionEntity("c1", "o1")),
        )
        val active = dao.observeActive().first().single()
        assertEquals(2, active.objectives.size)
        assertEquals(listOf("o1"), active.completedObjectives.map { it.objectiveId })

        // Finished elsewhere: the next refresh has no pending quests
        dao.replaceActive(emptyList(), emptyList(), emptyList())
        assertTrue(dao.observeActive().first().isEmpty())
    }

    @Test
    fun pastEventsAreHidden() = runTest {
        val dao = db.eventDao()
        dao.replaceAll(
            listOf(
                EventEntity("past", "Past", null, null, 1_000L, 2_000L, null, null, 0, false),
                EventEntity("next", "Next", null, null, 10_000L, null, 10, null, 3, false),
            )
        )
        assertEquals(listOf("next"), dao.observeUpcoming(nowMillis = 5_000L).first().map { it.id })

        dao.updateAttendance("next", attendees = 4, isAttending = true)
        val updated = dao.find("next")!!
        assertEquals(4, updated.attendees)
        assertTrue(updated.isAttending)
    }

    @Test
    fun telemetryIsReadInOrderAndDeletedAfterSending() = runTest {
        val dao = db.telemetryDao()
        repeat(3) { dao.insert(TelemetryEventEntity(name = "e$it", durationMs = 10, success = true, metadataJson = "{}", createdAtMillis = it.toLong())) }
        val batch = dao.oldest(limit = 2)
        assertEquals(listOf("e0", "e1"), batch.map { it.name })
        dao.delete(batch.map { it.id })
        assertEquals(1, dao.count())
    }
}
