package com.kotlin.wandr.ui.feature.quest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.domain.model.ActiveQuest
import com.kotlin.wandr.ui.components.BadgeStyle
import com.kotlin.wandr.ui.components.MainTab
import com.kotlin.wandr.ui.components.StatusBadge
import com.kotlin.wandr.ui.components.WandrBottomBar
import com.kotlin.wandr.ui.components.WandrCard
import com.kotlin.wandr.ui.components.WandrProgressBar
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Quest / Progress: every quest in progress with its progress bar. Tap one to open the tracker.
 * It is the "Saved" tab of the bottom bar, so it has no back arrow.
 */
@Composable
fun ActiveQuestsRoute(
    onSelectTab: (MainTab) -> Unit,
    onOpenQuest: (questId: String) -> Unit,
    viewModel: ActiveQuestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ActiveQuestsScreen(state = state, onSelectTab = onSelectTab, onOpenQuest = onOpenQuest)
}

@Composable
fun ActiveQuestsScreen(state: ActiveQuestUiState, onSelectTab: (MainTab) -> Unit, onOpenQuest: (String) -> Unit) {
    Scaffold(
        topBar = { WandrTopBar(title = "My Quests") },
        bottomBar = { WandrBottomBar(selected = MainTab.SAVED, onSelect = onSelectTab) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading && state.quests.isEmpty() -> Centered(padding) { CircularProgressIndicator() }
            state.quests.isEmpty() -> Centered(padding) {
                Text("No quests in progress", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Accept a quest from its details to start tracking it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WandrTheme.colors.textSecondary,
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (state.isShowingSavedData) {
                    item { StatusBadge(text = "Offline · showing saved progress", style = BadgeStyle.Neutral) }
                }
                items(state.quests, key = { it.questId }) { quest ->
                    ActiveQuestCard(quest, onClick = { onOpenQuest(quest.questId) })
                }
            }
        }
    }
}

@Composable
private fun ActiveQuestCard(quest: ActiveQuest, onClick: () -> Unit) {
    WandrCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
            Text(quest.questTitle, style = MaterialTheme.typography.titleMedium)
            WandrProgressBar(progress = if (quest.totalCount == 0) 0f else quest.completedCount.toFloat() / quest.totalCount)
            Text(
                quest.nextObjective?.let { "Next: ${it.title}" } ?: "All steps done",
                style = MaterialTheme.typography.bodySmall,
                color = WandrTheme.colors.textSecondary,
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
