package com.kotlin.wandr.data

import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.remote.dto.QuestDropoffDto
import com.kotlin.wandr.domain.model.QuestDropoffReport
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** BQ8: the `get_quest_dropoff` rows decode and the report answers the question. */
class QuestDropoffTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** Same rows the RPC returned on the seed after a few abandons (checked in Postgres). */
    private val rpcResponse = """
        [
          {"quest_id":"q5","quest_title":"Catch a live set","total_steps":3,"last_step":1,"last_step_title":"Arrive at Parque El Country","abandoned_count":3,"quest_abandoned_total":4},
          {"quest_id":"q7","quest_title":"Climb Monserrate","total_steps":3,"last_step":0,"last_step_title":null,"abandoned_count":2,"quest_abandoned_total":3},
          {"quest_id":"q5","quest_title":"Catch a live set","total_steps":3,"last_step":2,"last_step_title":"Watch a full performance","abandoned_count":1,"quest_abandoned_total":4},
          {"quest_id":"q7","quest_title":"Climb Monserrate","total_steps":3,"last_step":1,"last_step_title":"Start the trail at the base","abandoned_count":1,"quest_abandoned_total":3}
        ]
    """.trimIndent()

    private val report = QuestDropoffReport(json.decodeFromString<List<QuestDropoffDto>>(rpcResponse).map { it.toDomain() })

    @Test
    fun `the answer is the quest and step with most abandons`() {
        val worst = report.worstPoint!!
        assertEquals("Catch a live set", worst.questTitle)
        assertEquals(1, worst.lastStep)
        assertEquals(2, worst.abandonedAtStep)
        assertEquals("After step 1 · Arrive at Parque El Country", worst.label)
        assertEquals(7, report.totalAbandoned)
    }

    @Test
    fun `abandons are added up by step position across quests`() {
        // before step 1: 2 · after step 1: 3 + 1 · after step 2: 1
        assertEquals(listOf(2, 4, 1), report.abandonsByPosition)
    }

    @Test
    fun `each quest lists every step, most abandoned quest first`() {
        val quests = report.quests
        assertEquals(listOf("q5", "q7"), quests.map { it.questId })
        assertEquals(listOf(0, 3, 1), quests[0].abandonsByLastStep)
        assertEquals(1, quests[0].worstLastStep)
        assertEquals(listOf(2, 1, 0), quests[1].abandonsByLastStep)
        assertEquals("Start the trail at the base", quests[1].stepTitles[1])
    }

    @Test
    fun `no abandons means no answer`() {
        val empty = QuestDropoffReport(emptyList())
        assertNull(empty.worstPoint)
        assertEquals(0, empty.totalAbandoned)
        assertEquals(emptyList<Int>(), empty.abandonsByPosition)
    }

    // ---------- Smart feature ----------

    @Test
    fun `the riskiest step is the one after the most common last checked step`() {
        val risk = report.riskiestStepFor("q5")!!
        // 3 abandons stopped after step 1, so step 2 is where people give up
        assertEquals(2, risk.stepOrderIndex)
        assertEquals(3, risk.abandons)
        assertEquals(4, risk.questAbandons)
        assertEquals("3 of 4 people who gave up this quest stopped at this step. You've got this!", risk.message)

        // Most Monserrate abandons happen before step 1
        assertEquals(1, report.riskiestStepFor("q7")!!.stepOrderIndex)
    }

    @Test
    fun `a quest nobody abandoned has no risky step`() {
        assertNull(report.riskiestStepFor("unknown-quest"))
        assertNull(QuestDropoffReport(emptyList()).riskiestStepFor("q5"))
    }
}
