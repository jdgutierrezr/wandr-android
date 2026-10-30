package com.kotlin.wandr.domain.model

/**
 * BQ8 (Type 3): "In which step do users most frequently abandon a quest without finishing it?"
 *
 * One point = how many abandoned attempts of a quest stopped right after [lastStep].
 * [lastStep] is the `order_index` of the last checked step; 0 = gave up before checking any.
 * The step where they gave up is therefore `lastStep + 1`.
 */
data class DropoffPoint(
    val questId: String,
    val questTitle: String,
    val totalSteps: Int,
    val lastStep: Int,
    val lastStepTitle: String?,
    val abandonedCount: Int,
) {
    val abandonedAtStep: Int get() = lastStep + 1

    val label: String
        get() = if (lastStep == 0) "Before the first step" else "After step $lastStep · ${lastStepTitle ?: "Step $lastStep"}"
}

data class QuestDropoff(
    val questId: String,
    val questTitle: String,
    val totalSteps: Int,
    val abandoned: Int,
    /** Index = last checked step (0 = none), value = abandons that stopped there. */
    val abandonsByLastStep: List<Int>,
    val stepTitles: Map<Int, String>,
) {
    val worstLastStep: Int get() = abandonsByLastStep.indices.maxByOrNull { abandonsByLastStep[it] } ?: 0
}

data class QuestDropoffReport(
    val points: List<DropoffPoint>,
    val updatedAtMillis: Long? = null,
    val isStale: Boolean = false,
) {

    val totalAbandoned: Int get() = points.sumOf { it.abandonedCount }

    val worstPoint: DropoffPoint? get() = points.maxByOrNull { it.abandonedCount }

    val abandonsByPosition: List<Int>
        get() {
            val maxSteps = points.maxOfOrNull { maxOf(it.totalSteps, it.lastStep + 1) } ?: return emptyList()
            return List(maxSteps) { position -> points.filter { it.lastStep == position }.sumOf { it.abandonedCount } }
        }

    fun riskiestStepFor(questId: String): StepRisk? {
        val quest = quests.firstOrNull { it.questId == questId } ?: return null
        val lastChecked = quest.worstLastStep
        val abandons = quest.abandonsByLastStep.getOrElse(lastChecked) { 0 }
        if (abandons == 0) return null
        return StepRisk(
            questId = questId,
            stepOrderIndex = lastChecked + 1,
            abandons = abandons,
            questAbandons = quest.abandoned,
        )
    }

    val quests: List<QuestDropoff>
        get() = points
            .groupBy { it.questId }
            .map { (questId, rows) ->
                val first = rows.first()
                val steps = maxOf(first.totalSteps, rows.maxOf { it.lastStep } + 1)
                QuestDropoff(
                    questId = questId,
                    questTitle = first.questTitle,
                    totalSteps = first.totalSteps,
                    abandoned = rows.sumOf { it.abandonedCount },
                    abandonsByLastStep = List(steps) { step -> rows.filter { it.lastStep == step }.sumOf { it.abandonedCount } },
                    stepTitles = rows.mapNotNull { row -> row.lastStepTitle?.let { row.lastStep to it } }.toMap(),
                )
            }
            .sortedByDescending { it.abandoned }
}
data class StepRisk(
    val questId: String,
    val stepOrderIndex: Int,
    val abandons: Int,
    val questAbandons: Int,
) {
    /** "3 of 4 people who gave up this quest stopped at this step. You've got this!" */
    val message: String
        get() {
            val people = if (questAbandons == 1) "person" else "people"
            return "$abandons of $questAbandons $people who gave up this quest stopped at this step. You've got this!"
        }
}
