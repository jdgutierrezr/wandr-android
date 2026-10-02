package com.kotlin.wandr.ui.feature.quest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.domain.model.QuestDetail
import com.kotlin.wandr.domain.model.QuestObjective
import com.kotlin.wandr.ui.components.BadgeStyle
import com.kotlin.wandr.ui.components.CoverImage
import com.kotlin.wandr.ui.components.MetaText
import com.kotlin.wandr.ui.components.ObjectiveItem
import com.kotlin.wandr.ui.components.ObjectiveState
import com.kotlin.wandr.ui.components.PrimaryButton
import com.kotlin.wandr.ui.components.RewardRow
import com.kotlin.wandr.ui.components.SectionLabel
import com.kotlin.wandr.ui.components.StatusBadge
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Quest Details. Reports QUEST_VIEWED (BQ8 funnel) when it opens and "Accept Quest" starts the
 * quest (QUEST_ACCEPTED is published by the repository). Then it opens the tracker.
 */
@Composable
fun QuestDetailRoute(
    onBack: () -> Unit,
    onOpenTracker: (questId: String) -> Unit,
    viewModel: QuestDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onScreenShown() }
    LaunchedEffect(state.startedQuestId) {
        state.startedQuestId?.let {
            onOpenTracker(it)
            viewModel.onNavigatedToTracker()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    QuestDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAccept = viewModel::startQuest,
        onContinue = { state.detail?.quest?.id?.let(onOpenTracker) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestDetailScreen(
    state: QuestDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onContinue: () -> Unit,
) {
    val detail = state.detail
    Scaffold(
        topBar = { WandrTopBar(title = "Quest Details", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (detail != null) {
                PrimaryButton(
                    text = if (state.isInProgress) "Continue quest" else "Accept Quest",
                    onClick = if (state.isInProgress) onContinue else onAccept,
                    isLoading = state.isStarting,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.md),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (detail == null) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (state.isLoading) CircularProgressIndicator() else Text("This quest could not be loaded")
            }
            return@Scaffold
        }

        val quest = detail.quest
        LazyColumn(
            contentPadding = PaddingValues(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (state.isShowingSavedData) {
                item { StatusBadge(text = "Offline · showing saved data", style = BadgeStyle.Neutral) }
            }
            item {
                CoverImage(
                    imageUrl = quest.coverImageUrl,
                    contentDescription = quest.title,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.large),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
                    if (quest.tags.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs)) {
                            quest.tags.forEach { StatusBadge(text = it) }
                        }
                    }
                    Text(quest.title, style = MaterialTheme.typography.headlineSmall)
                    val placeLine = detail.place?.let { place -> listOfNotNull(place.name, place.address).joinToString(" · ") }
                        ?: quest.placeName
                    placeLine?.let { MetaText(it, Icons.Outlined.LocationOn) }
                    quest.estimatedDurationMin?.let { MetaText("~$it min", Icons.Outlined.Schedule) }
                    quest.difficultyLevel?.let { MetaText("Difficulty $it/5", Icons.Outlined.Terrain) }
                    quest.description?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = WandrTheme.colors.textSecondary)
                    }
                }
            }
            item { SectionLabel("Objectives", trailing = "${detail.objectives.size} steps") }
            items(detail.objectives, key = { it.id }) { objective ->
                ObjectiveItem(
                    title = objective.title,
                    state = ObjectiveState.Pending,
                    subtitle = "Step ${objective.orderIndex}",
                    requiresPhoto = objective.requiresPhoto,
                )
            }
            item {
                RewardRow(
                    title = "Rewards",
                    description = "Unlocked when you finish every step",
                    xp = quest.pointsReward,
                    icon = Icons.Rounded.Diamond,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun QuestDetailScreenPreview() {
    WandrTheme {
        QuestDetailScreen(
            state = QuestDetailUiState(isLoading = false, detail = previewDetail),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onAccept = {}, onContinue = {},
        )
    }
}

internal val previewDetail = QuestDetail(
    quest = com.kotlin.wandr.domain.model.Quest(
        id = "q1", title = "Climb Monserrate", description = "Hike up to the top and enjoy the view of Bogotá.",
        difficultyLevel = 3, estimatedDurationMin = 90, pointsReward = 150, coverImageUrl = null,
        placeId = "p1", placeName = "Monserrate", tags = listOf("outdoors", "sport"), distanceKm = 2.4,
    ),
    place = null,
    objectives = listOf(
        QuestObjective("o1", "q1", "Start the trail at the base", requiresPhoto = false, orderIndex = 1),
        QuestObjective("o2", "q1", "Reach the halfway point", requiresPhoto = false, orderIndex = 2),
        QuestObjective("o3", "q1", "Take a photo at the top", requiresPhoto = true, orderIndex = 3),
    ),
)
