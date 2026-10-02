package com.kotlin.wandr.ui.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.domain.model.DropoffPoint
import com.kotlin.wandr.domain.model.QuestDropoff
import com.kotlin.wandr.domain.model.QuestDropoffReport
import com.kotlin.wandr.ui.components.BadgeStyle
import com.kotlin.wandr.ui.components.DropoffBar
import com.kotlin.wandr.ui.components.DropoffBars
import com.kotlin.wandr.ui.components.SecondaryButton
import com.kotlin.wandr.ui.components.SectionLabel
import com.kotlin.wandr.ui.components.StatusBadge
import com.kotlin.wandr.ui.components.WandrCard
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * BQ8 · Abandonment funnel (Type 3, features analysis): in which step users most often give up
 * a quest. Data: `get_quest_dropoff()` over `quest_completions` (see the analytics pipeline).
 */
@Composable
fun QuestDropoffRoute(
    onBack: () -> Unit,
    onOpenQuest: (questId: String) -> Unit,
    viewModel: QuestDropoffViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    QuestDropoffScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = viewModel::load,
        onOpenQuest = onOpenQuest,
    )
}

@Composable
fun QuestDropoffScreen(
    state: QuestDropoffUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenQuest: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            WandrTopBar(
                title = "Abandonment Funnel",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val report = state.report
        when {
            report == null && state.isLoading -> Centered(padding) { CircularProgressIndicator() }
            report == null -> Centered(padding) {
                Text("We couldn't load the funnel", style = MaterialTheme.typography.titleMedium)
                Text(
                    "It is calculated on the server, so it needs an internet connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WandrTheme.colors.textSecondary,
                )
                SecondaryButton(text = "Try again", onClick = onRefresh, leadingIcon = Icons.Rounded.Refresh)
            }
            report.totalAbandoned == 0 -> Centered(padding) {
                Text("No abandoned quests yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "When someone gives up a quest, the step where they stopped shows up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WandrTheme.colors.textSecondary,
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                report.worstPoint?.let { item { AnswerCard(it, report.totalAbandoned) } }
                item { AllQuestsCard(report) }
                item { SectionLabel("By quest", trailing = "${report.quests.size} quests") }
                items(report.quests, key = { it.questId }) { quest ->
                    QuestFunnelCard(quest, onClick = { onOpenQuest(quest.questId) })
                }
            }
        }
    }
}

/** The direct answer to BQ8. */
@Composable
private fun AnswerCard(worst: DropoffPoint, totalAbandoned: Int) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
            SectionLabel("Most frequent drop-off")
            Text(worst.questTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                if (worst.lastStep == 0) "Users give up before checking the first step."
                else "Users give up at step ${worst.abandonedAtStep}, right after \"${worst.lastStepTitle ?: "step ${worst.lastStep}"}\".",
                style = MaterialTheme.typography.bodyMedium,
            )
            StatusBadge(
                text = "${worst.abandonedCount} of $totalAbandoned abandons",
                style = BadgeStyle.Danger,
            )
        }
    }
}

/** Abandons by step position across every quest. */
@Composable
private fun AllQuestsCard(report: QuestDropoffReport) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            SectionLabel("All quests", trailing = "${report.totalAbandoned} abandons")
            DropoffBars(
                bars = report.abandonsByPosition.mapIndexed { position, count ->
                    DropoffBar(if (position == 0) "Before step 1" else "After step $position", count)
                },
            )
        }
    }
}

@Composable
private fun QuestFunnelCard(quest: QuestDropoff, onClick: () -> Unit) {
    WandrCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            Column {
                Text(quest.questTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${quest.abandoned} ${if (quest.abandoned == 1) "abandon" else "abandons"} · ${quest.totalSteps} steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = WandrTheme.colors.textSecondary,
                )
            }
            DropoffBars(
                bars = quest.abandonsByLastStep.mapIndexed { step, count ->
                    val label = if (step == 0) "Before step 1" else "After step $step · ${quest.stepTitles[step] ?: "Step $step"}"
                    DropoffBar(label, count)
                },
            )
        }
    }
}

@Composable
private fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(padding).padding(WandrTheme.spacing.screen),
    ) { content() }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun QuestDropoffScreenPreview() {
    WandrTheme {
        QuestDropoffScreen(
            state = QuestDropoffUiState(
                isLoading = false,
                report = QuestDropoffReport(
                    listOf(
                        DropoffPoint("q5", "Catch a live set", 3, 1, "Arrive at Parque El Country", 3),
                        DropoffPoint("q5", "Catch a live set", 3, 2, "Watch a full performance", 1),
                        DropoffPoint("q7", "Climb Monserrate", 3, 0, null, 2),
                        DropoffPoint("q7", "Climb Monserrate", 3, 1, "Start the trail at the base", 1),
                    ),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onRefresh = {}, onOpenQuest = {},
        )
    }
}
