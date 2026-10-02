package com.kotlin.wandr.ui.feature.quest

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.domain.model.ActiveQuest
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.Place
import com.kotlin.wandr.domain.model.QuestObjective
import com.kotlin.wandr.ui.components.BadgeStyle
import com.kotlin.wandr.ui.components.DangerButton
import com.kotlin.wandr.ui.components.IconCircle
import com.kotlin.wandr.ui.components.MetaText
import com.kotlin.wandr.ui.components.ObjectiveItem
import com.kotlin.wandr.ui.components.ObjectiveState
import com.kotlin.wandr.ui.components.PrimaryButton
import com.kotlin.wandr.ui.components.SectionLabel
import com.kotlin.wandr.ui.components.StatusBadge
import com.kotlin.wandr.ui.components.WandrCard
import com.kotlin.wandr.ui.components.WandrProgressBar
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.components.XpBadge
import com.kotlin.wandr.ui.theme.WandrTheme
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Active Quest / Navigation (View 09): progress, destination, steps, "Navigate" and "Give up".
 *
 * - Progress comes from Room (`activeQuests`), so it is kept offline and after closing the app.
 * - The destination comes from the cached quest detail ([QuestDetailViewModel], same `questId`).
 * - "Navigate" reports NAVIGATION_STARTED (BQ8) and opens the maps app (external service).
 * - Photo steps use the camera.
 */
@Composable
fun ActiveQuestRoute(
    questId: String,
    onBack: () -> Unit,
    viewModel: ActiveQuestViewModel = hiltViewModel(),
    detailViewModel: QuestDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var askToAbandon by remember { mutableStateOf(false) }
    var abandonRequested by rememberSaveable { mutableStateOf(false) }
    // Step waiting for its photo while the camera is open
    var photoObjectiveId by rememberSaveable { mutableStateOf<String?>(null) }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val objectiveId = photoObjectiveId
        photoObjectiveId = null
        if (bitmap != null && objectiveId != null) {
            viewModel.completeObjective(questId, objectiveId, bitmap.toJpeg())
        }
    }

    val quest = state.quests.firstOrNull { it.questId == questId }

    // After giving up, the quest leaves the active list: go back
    LaunchedEffect(abandonRequested, quest, state.abandoningQuestId) {
        if (abandonRequested && quest == null && state.abandoningQuestId == null) onBack()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            abandonRequested = false
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    ActiveQuestScreen(
        quest = quest,
        place = detailState.detail?.place,
        isLoading = state.isLoading,
        isShowingSavedData = state.isShowingSavedData,
        submittingObjectiveId = state.submittingObjectiveId,
        isAbandoning = state.abandoningQuestId == questId,
        lastResult = state.lastResult,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onCheckObjective = { objective ->
            if (objective.requiresPhoto) {
                photoObjectiveId = objective.id
                takePhoto.launch(null)
            } else {
                viewModel.completeObjective(questId, objective.id)
            }
        },
        onNavigate = { place ->
            // Only a real navigation counts for the BQ8 funnel
            if (context.openDirections(place)) {
                viewModel.startNavigation(questId)
            } else {
                scope.launch { snackbarHostState.showSnackbar("No maps app found on this phone") }
            }
        },
        onAbandon = { askToAbandon = true },
        onResultShown = {
            val finished = state.lastResult?.questCompleted == true
            viewModel.onResultShown()
            if (finished) onBack()
        },
    )

    if (askToAbandon) {
        AlertDialog(
            onDismissRequest = { askToAbandon = false },
            title = { Text("Give up this quest?") },
            text = { Text("It will no longer be in progress. You can start it again later from its details.") },
            confirmButton = {
                TextButton(onClick = {
                    askToAbandon = false
                    abandonRequested = true
                    viewModel.abandonQuest(questId)
                }) { Text("Give up", color = WandrTheme.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { askToAbandon = false }) { Text("Keep going") } },
        )
    }
}

@Composable
fun ActiveQuestScreen(
    quest: ActiveQuest?,
    place: Place?,
    isLoading: Boolean,
    isShowingSavedData: Boolean,
    submittingObjectiveId: String?,
    isAbandoning: Boolean,
    lastResult: ObjectiveResult?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCheckObjective: (QuestObjective) -> Unit,
    onNavigate: (Place) -> Unit,
    onAbandon: () -> Unit,
    onResultShown: () -> Unit,
) {
    Scaffold(
        topBar = { WandrTopBar(title = "Active Quest", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (quest == null) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (isLoading || lastResult != null) CircularProgressIndicator()
                else Text("This quest is not in progress", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (isShowingSavedData) {
                    item { StatusBadge(text = "Offline · your progress is saved on the phone", style = BadgeStyle.Neutral) }
                }
                item { ProgressHeader(quest) }
                item { DestinationCard(place = place, onNavigate = onNavigate) }
                item { SectionLabel("Objectives", trailing = "${quest.completedCount} of ${quest.totalCount}") }
                items(quest.objectives, key = { it.id }) { objective ->
                    val isDone = objective.id in quest.completedObjectiveIds
                    val isNext = objective.id == quest.nextObjective?.id
                    ObjectiveItem(
                        title = objective.title,
                        state = when {
                            isDone -> ObjectiveState.Done
                            objective.id == submittingObjectiveId -> ObjectiveState.InProgress
                            else -> ObjectiveState.Pending
                        },
                        subtitle = when {
                            isDone -> "Done"
                            isNext && objective.requiresPhoto -> "Tap to take the photo"
                            isNext -> "Tap when you finish this step"
                            else -> "Step ${objective.orderIndex}"
                        },
                        requiresPhoto = objective.requiresPhoto,
                        // Steps are checked in order, one at a time
                        onClick = if (isNext && submittingObjectiveId == null) ({ onCheckObjective(objective) }) else null,
                    )
                }
                item {
                    DangerButton(
                        text = if (isAbandoning) "Giving up…" else "Give up quest",
                        onClick = { if (!isAbandoning) onAbandon() },
                        leadingIcon = Icons.Outlined.Flag,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (lastResult != null) ResultDialog(lastResult, onDismiss = onResultShown)
}

@Composable
private fun ProgressHeader(quest: ActiveQuest) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
            StatusBadge(text = "In Progress")
            Text(quest.questTitle, style = MaterialTheme.typography.headlineSmall)
            WandrProgressBar(progress = if (quest.totalCount == 0) 0f else quest.completedCount.toFloat() / quest.totalCount)
            Text(
                "${quest.completedCount} of ${quest.totalCount} steps done",
                style = MaterialTheme.typography.bodySmall,
                color = WandrTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun DestinationCard(place: Place?, onNavigate: (Place) -> Unit) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
            SectionLabel("Destination")
            if (place == null) {
                Text("Loading the place…", style = MaterialTheme.typography.bodyMedium, color = WandrTheme.colors.textSecondary)
            } else {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
                place.address?.let { MetaText(it, Icons.Outlined.LocationOn) }
                place.distanceKm?.let { MetaText("%.1f km away".format(it), Icons.Rounded.Directions) }
                PrimaryButton(
                    text = "Navigate",
                    onClick = { onNavigate(place) },
                    leadingIcon = Icons.Rounded.Directions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ResultDialog(result: ObjectiveResult, onDismiss: () -> Unit) {
    val trophy: (@Composable () -> Unit)? =
        if (result.questCompleted) { { IconCircle(Icons.Rounded.EmojiEvents) } } else null
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = trophy,
        title = { Text(if (result.questCompleted) "Quest completed!" else "Step done") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
                if (result.questCompleted) {
                    XpBadge(result.xpEarned)
                    result.currentStreak?.let { Text("Streak: $it ${if (it == 1) "day" else "days"}") }
                    if (result.newBadges.isNotEmpty()) Text("New badges: " + result.newBadges.joinToString { it.name })
                } else {
                    Text("${result.completedObjectives} of ${result.totalObjectives} steps done. Keep going!")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (result.questCompleted) "Done" else "OK") } },
    )
}

/** Opens the maps app at the place. Returns false if no app can show a map. */
private fun Context.openDirections(place: Place): Boolean {
    val lat = place.location.latitude
    val lng = place.location.longitude
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(place.name)})")
    return try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

private fun Bitmap.toJpeg(): ByteArray = ByteArrayOutputStream().use { out ->
    compress(Bitmap.CompressFormat.JPEG, 85, out)
    out.toByteArray()
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ActiveQuestScreenPreview() {
    WandrTheme {
        ActiveQuestScreen(
            quest = ActiveQuest(
                completionId = "c1", questId = "q1", questTitle = "Climb Monserrate", startedAt = Instant.EPOCH,
                objectives = previewDetail.objectives, completedObjectiveIds = setOf("o1"),
            ),
            place = null,
            isLoading = false, isShowingSavedData = false, submittingObjectiveId = null, isAbandoning = false,
            lastResult = null, snackbarHostState = SnackbarHostState(),
            onBack = {}, onCheckObjective = {}, onNavigate = {}, onAbandon = {}, onResultShown = {},
        )
    }
}
