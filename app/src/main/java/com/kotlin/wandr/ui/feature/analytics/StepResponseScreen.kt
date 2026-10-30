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
import com.kotlin.wandr.domain.model.StepResponseTime
import com.kotlin.wandr.ui.components.BadgeStyle
import com.kotlin.wandr.ui.components.SecondaryButton
import com.kotlin.wandr.ui.components.SectionLabel
import com.kotlin.wandr.ui.components.StatusBadge
import com.kotlin.wandr.ui.components.WandrCard
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.theme.WandrTheme

@Composable
fun StepResponseRoute(
    onBack: () -> Unit,
    viewModel: StepResponseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    StepResponseScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = viewModel::load,
    )
}

@Composable
fun StepResponseScreen(
    state: StepResponseUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            WandrTopBar(
                title = "Step Response Time",
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
        val steps = state.steps
        when {
            steps == null && state.isLoading -> Centered(padding) { CircularProgressIndicator() }
            steps == null -> Centered(padding) {
                Text("We couldn't load the step times", style = MaterialTheme.typography.titleMedium)
                Text(
                    "They are calculated on the server, so it needs an internet connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WandrTheme.colors.textSecondary,
                )
                SecondaryButton(text = "Try again", onClick = onRefresh, leadingIcon = Icons.Rounded.Refresh)
            }
            steps.isEmpty() -> Centered(padding) {
                Text("No measurements yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Check a few quest steps and their response time shows up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WandrTheme.colors.textSecondary,
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                item { StatusBadge(text = "Internal · team analytics (BQ2, Type 1)", style = BadgeStyle.Neutral) }
                item { AnswerCard(steps.first()) }
                item { SectionLabel("All steps", trailing = "${steps.size} steps") }
                items(steps, key = { it.objectiveId }) { StepCard(it) }
            }
        }
    }
}

@Composable
private fun AnswerCard(slowest: StepResponseTime) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
            SectionLabel("Slowest step")
            Text("${slowest.avgMs} ms", style = MaterialTheme.typography.titleLarge)
            Text(
                "Step ${slowest.stepOrder} of \"${slowest.questTitle}\": ${slowest.stepTitle}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (slowest.requiresPhoto) StatusBadge(text = "Needs a photo", style = BadgeStyle.Neutral)
        }
    }
}

@Composable
private fun StepCard(step: StepResponseTime) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs)) {
            Text("${step.questTitle} · step ${step.stepOrder}", style = MaterialTheme.typography.titleMedium)
            Text(step.stepTitle, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Average ${step.avgMs} ms · slowest ${step.maxMs} ms · ${step.requests} checks" +
                    if (step.requiresPhoto) " · photo" else "",
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

@Preview(showBackground = true)
@Composable
private fun StepResponseScreenPreview() {
    WandrTheme {
        StepResponseScreen(
            state = StepResponseUiState(
                isLoading = false,
                steps = listOf(
                    StepResponseTime("q1", "Climb Monserrate", "o3", "Take a photo at the top", 3, true, 12, 1840, 4200),
                    StepResponseTime("q1", "Climb Monserrate", "o1", "Start the trail at the base", 1, false, 15, 420, 900),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onRefresh = {},
        )
    }
}
