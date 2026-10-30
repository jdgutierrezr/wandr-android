package com.kotlin.wandr.domain.model

sealed interface StepOutcome {
    data class Synced(val result: ObjectiveResult) : StepOutcome
    data object Queued : StepOutcome
}

enum class SyncResult { DONE, RETRY }
