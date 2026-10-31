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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.domain.model.LowUseCategory
import com.kotlin.wandr.domain.model.LowUseCategoriesReport
import com.kotlin.wandr.ui.components.BadgeStyle
import com.kotlin.wandr.ui.components.SectionLabel
import com.kotlin.wandr.ui.components.StatusBadge
import com.kotlin.wandr.ui.components.WandrCard
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.components.SecondaryButton
import com.kotlin.wandr.ui.theme.WandrTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LowUseCategoriesRoute(
    onBack: () -> Unit,
    viewModel: LowUseCategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    LowUseCategoriesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    )
}

@Composable
fun LowUseCategoriesScreen(
    state: LowUseCategoriesUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            WandrTopBar(
                title = "Low-use Categories",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isLoading,
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val report = state.report

        when {
            report == null && state.isLoading -> CenteredLowUseCategories(padding) {
                CircularProgressIndicator()
            }

            report == null -> CenteredLowUseCategories(padding) {
                Text(
                    "We couldn't load the categories",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "This report is calculated on the server, so it needs an internet connection the first time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WandrTheme.colors.textSecondary,
                )
                SecondaryButton(
                    text = "Try again",
                    onClick = onRefresh,
                    leadingIcon = Icons.Rounded.Refresh,
                )
            }

            report.categories.isEmpty() -> LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = WandrTheme.spacing.screen,
                    vertical = WandrTheme.spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                item {
                    StatusBadge(
                        text = "Internal · team analytics (BQ7, Type 3)",
                        style = BadgeStyle.Neutral,
                    )
                }
                item {
                    report.updatedAtMillis?.let { updatedAt ->
                        StatusBadge(
                            text = if (report.isStale) {
                                "Offline · saved copy from ${updatedAt.asLowUseTime()}"
                            } else {
                                "Updated at ${updatedAt.asLowUseTime()}"
                            },
                            style = BadgeStyle.Neutral,
                        )
                    }
                }
                item {
                    WandrCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
                        ) {
                            Text(
                                "No low-use categories",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "All categories have an average of at least 3 starts per week over the last 4 complete weeks.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WandrTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }

            else -> LowUseCategoriesContent(
                report = report,
                padding = padding,
            )
        }
    }
}

@Composable
private fun LowUseCategoriesContent(
    report: LowUseCategoriesReport,
    padding: PaddingValues,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = WandrTheme.spacing.screen,
            vertical = WandrTheme.spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        item {
            StatusBadge(
                text = "Internal · team analytics (BQ7, Type 3)",
                style = BadgeStyle.Neutral,
            )
        }

        item {
            report.updatedAtMillis?.let { updatedAt ->
                StatusBadge(
                    text = if (report.isStale) {
                        "Offline · saved copy from ${updatedAt.asLowUseTime()}"
                    } else {
                        "Updated at ${updatedAt.asLowUseTime()}"
                    },
                    style = BadgeStyle.Neutral,
                )
            }
        }

        item {
            WandrCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
                ) {
                    SectionLabel("Answer")
                    Text(
                        "${report.categories.size} low-use categories",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Categories selected fewer than 3 times per week on average, over the last 4 complete weeks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WandrTheme.colors.textSecondary,
                    )
                }
            }
        }

        item {
            SectionLabel(
                "By category",
                trailing = "${report.categories.size} categories",
            )
        }

        items(
            items = report.categories,
            key = { it.tagId },
        ) { category ->
            LowUseCategoryCard(category)
        }
    }
}

@Composable
private fun LowUseCategoryCard(category: LowUseCategory) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs),
            ) {
                Text(
                    category.tagName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${category.totalStarts} starts in 4 weeks",
                    style = MaterialTheme.typography.bodySmall,
                    color = WandrTheme.colors.textSecondary,
                )
            }

            StatusBadge(
                text = "${category.averagePerWeek} starts / week",
                style = BadgeStyle.Neutral,
            )

            SectionLabel("Weekly breakdown")

            category.weeklyBreakdown.forEach { week ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs),
                ) {
                    Text(
                        week.weekStart.asWeekLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = WandrTheme.colors.textSecondary,
                    )
                    Text(
                        "${week.starts} ${if (week.starts == 1) "start" else "starts"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredLowUseCategories(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            WandrTheme.spacing.sm,
            Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(WandrTheme.spacing.screen),
    ) {
        content()
    }
}

private fun Long.asLowUseTime(): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(this))

private fun String.asWeekLabel(): String =
    runCatching {
        val date = java.time.LocalDate.parse(this.take(10))
        "Week of ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
    }.getOrDefault(this)
