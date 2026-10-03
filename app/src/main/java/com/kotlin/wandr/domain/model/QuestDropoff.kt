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
    /** The step the user did not finish (1-based). */
    val abandonedAtStep: Int get() = lastStep + 1

    /** "Before the first step" or "After step 2 · Watch a full performance". */
    val label: String
        get() = if (lastStep == 0) "Before the first step" else "After step $lastStep · ${lastStepTitle ?: "Step $lastStep"}"
}

/** Abandons of one quest, step by step (every step appears, even with 0 abandons). */
data class QuestDropoff(
    val questId: String,
    val questTitle: String,
    val totalSteps: Int,
    val abandoned: Int,
    /** Index = last checked step (0 = none), value = abandons that stopped there. */
    val abandonsByLastStep: List<Int>,
    val stepTitles: Map<Int, String>,
) {
    /** The last checked step with most abandons (the answer to BQ8 for this quest). */
    val worstLastStep: Int get() = abandonsByLastStep.indices.maxByOrNull { abandonsByLastStep[it] } ?: 0
}

/** Everything the BQ8 screen shows, built from the rows of `get_quest_dropoff()`. */
data class QuestDropoffReport(val points: List<DropoffPoint>) {

    val totalAbandoned: Int get() = points.sumOf { it.abandonedCount }

    /** The single quest + step where most attempts were abandoned. Null if nobody abandoned. */
    val worstPoint: DropoffPoint? get() = points.maxByOrNull { it.abandonedCount }

    /**
     * Across all quests, how many abandons happened after each step position
     * (index 0 = before the first step, 1 = after step 1…). Answers BQ8 for the app as a whole.
     */
    val abandonsByPosition: List<Int>
        get() {
            val maxSteps = points.maxOfOrNull { maxOf(it.totalSteps, it.lastStep + 1) } ?: return emptyList()
            return List(maxSteps) { position -> points.filter { it.lastStep == position }.sumOf { it.abandonedCount } }
        }

    /**
     * Smart feature: the step of [questId] where most abandoned attempts stopped, or null if
     * nobody has abandoned that quest yet. Active Quest shows a hint on that step.
     */
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

    /** One entry per quest, most abandoned quest first. */
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

/**
 * The step of a quest where users give up most often (smart feature on top of BQ8).
 * [stepOrderIndex] is the `order_index` of the step they did not finish.
 */
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
