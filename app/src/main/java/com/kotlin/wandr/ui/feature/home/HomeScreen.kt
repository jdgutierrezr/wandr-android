package com.kotlin.wandr.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.domain.model.RecommendedQuest
import com.kotlin.wandr.ui.theme.WandrTheme
import com.kotlin.wandr.ui.components.MainTab
import com.kotlin.wandr.ui.components.WandrBottomBar


// =============================================================
// ROUTE
// =============================================================

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onSelectTab: (MainTab) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onTagSelected = viewModel::selectTag,
        onEnergySelected = viewModel::selectEnergyLevel,
        onRadiusSelected = viewModel::selectRadius,
        onRevealMystery = viewModel::revealMysteryQuest,
        onSkipMystery = viewModel::skipMysteryQuest,
        onSelectTab = onSelectTab,
    )
}

// =============================================================
// MAIN SCREEN
// =============================================================

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onTagSelected: (String?) -> Unit,
    onEnergySelected: (String) -> Unit,
    onRadiusSelected: (Double) -> Unit,
    onRevealMystery: () -> Unit,
    onSkipMystery: () -> Unit,
    onSelectTab: (MainTab) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp,
                )
                .padding(bottom = 92.dp),
        ) {

            // =========================================================
            // HEADER
            // =========================================================

            HomeHeader(
                locationSource = state.locationSource,
                onRefresh = onRefresh,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // =========================================================
            // OFFLINE
            // =========================================================

            if (state.isShowingSavedData) {
                OfflineBanner()

                Spacer(modifier = Modifier.height(16.dp))
            }

            // =========================================================
            // ERROR
            // =========================================================

            if (state.errorMessage != null) {
                ErrorSection(
                    message = state.errorMessage,
                    onRefresh = onRefresh,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // =========================================================
            // LOADING
            // =========================================================

            if (state.isLoading && state.allQuests.isEmpty()) {
                LoadingSection()
                return@Column
            }

            // =========================================================
            // MYSTERY QUEST
            // =========================================================

            MysteryQuestHeader()

            Spacer(modifier = Modifier.height(14.dp))

            MysteryFiltersCard(
                selectedEnergyLevel = state.selectedEnergyLevel,
                selectedRadiusKm = state.selectedRadiusKm,
                onEnergySelected = onEnergySelected,
                onRadiusSelected = onRadiusSelected,
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state.recommendations.isNotEmpty()) {

                val mysteryQuest = state.recommendations.first()

                MysteryQuestCard(
                    recommendation = mysteryQuest,
                    revealed = state.isMysteryRevealed,
                    onReveal = onRevealMystery,
                    onSkip = onSkipMystery,
                )

            } else {

                MysteryEmptyCard(
                    isLoading = state.isLoadingMystery,
                    onRetry = onRefresh,
                )
            }

            // =========================================================
            // NEARBY QUESTS
            // =========================================================

            Spacer(modifier = Modifier.height(34.dp))

            SectionTitle(
                title = "Nearby quests",
                subtitle = if (state.quests.isNotEmpty()) {
                    "${state.quests.size} activities near you"
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category filters
            if (state.availableTags.isNotEmpty()) {

                TagFilterRow(
                    tags = state.availableTags,
                    selectedTag = state.selectedTag,
                    onTagSelected = onTagSelected,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Activities
            if (state.quests.isEmpty()) {

                EmptyQuestsSection(
                    hasFilter = state.selectedTag != null,
                    onClearFilter = {
                        onTagSelected(null)
                    },
                )

            } else {

                state.quests.forEach { quest ->

                    QuestCard(
                        quest = quest,
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        WandrBottomBar(
            selected = MainTab.HOME,
            onSelect = onSelectTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}


// =============================================================
// HEADER
// =============================================================

@Composable
private fun HomeHeader(
    locationSource: UserLocation.Source?,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(
            modifier = Modifier.weight(1f),
        ) {

            Text(
                text = "Wandr",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                Text(
                    text = when (locationSource) {
                        UserLocation.Source.LAST_KNOWN ->
                            "Using your last known location"

                        UserLocation.Source.FALLBACK ->
                            "Location may be approximate"

                        UserLocation.Source.CURRENT ->
                            "Activities near you"

                        null ->
                            "Finding activities near you..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WandrTheme.colors.textSecondary,
                )
            }
        }

        IconButton(
            onClick = onRefresh,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}


// =============================================================
// MYSTERY HEADER
// =============================================================

@Composable
private fun MysteryQuestHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = "Mystery Quest",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    WandrTheme.colors.successContainer
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✦",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}


// =============================================================
// MYSTERY FILTERS
// =============================================================

@Composable
private fun MysteryFiltersCard(
    selectedEnergyLevel: String,
    selectedRadiusKm: Double,
    onEnergySelected: (String) -> Unit,
    onRadiusSelected: (Double) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
        ) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    text = "☷  ADVENTURE FILTERS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Live matching",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WandrTheme.colors.textSecondary,
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // =====================================================
            // ENERGY
            // =====================================================

            Text(
                text = "Energy Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {

                MysterySegment(
                    text = "Low",
                    selected = selectedEnergyLevel == "low",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onEnergySelected("low")
                    },
                )

                MysterySegment(
                    text = "Medium",
                    selected = selectedEnergyLevel == "medium",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onEnergySelected("medium")
                    },
                )

                MysterySegment(
                    text = "High",
                    selected = selectedEnergyLevel == "high",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onEnergySelected("high")
                    },
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // =====================================================
            // RADIUS
            // =====================================================

            Text(
                text = "Max Radius",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {

                MysterySegment(
                    text = "1 km",
                    selected = selectedRadiusKm == 1.0,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onRadiusSelected(1.0)
                    },
                )

                MysterySegment(
                    text = "3 km",
                    selected = selectedRadiusKm == 3.0,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onRadiusSelected(3.0)
                    },
                )

                MysterySegment(
                    text = "5 km",
                    selected = selectedRadiusKm == 5.0,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onRadiusSelected(5.0)
                    },
                )
            }
        }
    }
}

@Composable
private fun MysterySegment(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                vertical = 11.dp,
                horizontal = 8.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                WandrTheme.colors.textSecondary
            },
            maxLines = 1,
        )
    }
}
// =============================================================
// ENERGY FILTER
// =============================================================

@Composable
private fun EnergyFilter(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    FilterChip(
        selected = selected,

        onClick = onClick,

        modifier = modifier,

        label = {
            Text(
                text = text,
                maxLines = 1,
            )
        },

        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor =
                    MaterialTheme.colorScheme.primary,

                selectedLabelColor =
                    MaterialTheme.colorScheme.onPrimary,
            ),
    )
}


// =============================================================
// RADIUS FILTER
// =============================================================

@Composable
private fun RadiusFilter(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    FilterChip(
        selected = selected,

        onClick = onClick,

        modifier = modifier,

        label = {
            Text(
                text = text,
                maxLines = 1,
            )
        },

        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor =
                    MaterialTheme.colorScheme.primary,

                selectedLabelColor =
                    MaterialTheme.colorScheme.onPrimary,
            ),
    )
}


// =============================================================
// MYSTERY QUEST CARD
// =============================================================

@Composable
private fun MysteryQuestCard(
    recommendation: RecommendedQuest,
    revealed: Boolean,
    onReveal: () -> Unit,
    onSkip: () -> Unit,
) {

    if (!revealed) {

        MysteryHiddenCard(
            recommendation = recommendation,
            onReveal = onReveal,
        )

    } else {

        MysteryRevealedCard(
            recommendation = recommendation,
            onSkip = onSkip,
        )
    }
}


// =============================================================
// HIDDEN MYSTERY CARD
// =============================================================

@Composable
private fun MysteryHiddenCard(
    recommendation: RecommendedQuest,
    onReveal: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 22.dp,
                    vertical = 28.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Mystery icon
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(
                        WandrTheme.colors.successContainer
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✉",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "Ready for the Unexpected?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "A curated hidden spot is waiting for you. " +
                        "Your quest has been matched to your preferences.",
                style = MaterialTheme.typography.bodyLarge,
                color = WandrTheme.colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = onReveal,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
            ) {
                Text(
                    text = "Reveal my quest ✦",
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "+150 bonus pts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// =============================================================
// REVEALED MYSTERY CARD
// =============================================================

@Composable
private fun MysteryRevealedCard(
    recommendation: RecommendedQuest,
    onSkip: () -> Unit,
) {
    val quest = recommendation.quest

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
            ),
        ) {

            Column(
                modifier = Modifier.padding(20.dp),
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                WandrTheme.colors.successContainer
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✦",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "MYSTERY QUEST",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )

                        Text(
                            text = quest.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = quest.placeName ?: "Nearby",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WandrTheme.colors.textSecondary,
                )

                if (!quest.description.isNullOrBlank()) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WandrTheme.colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {

                    quest.distanceKm?.let {
                        InfoChip(
                            icon = Icons.Outlined.LocationOn,
                            text = "${formatDistance(it)} km",
                        )
                    }

                    quest.estimatedDurationMin?.let {
                        InfoChip(
                            icon = Icons.Outlined.AccessTime,
                            text = "$it min",
                        )
                    }

                    InfoChip(
                        icon = Icons.Outlined.Star,
                        text = "${quest.pointsReward} XP",
                    )
                }

                if (recommendation.matchedInterests.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Text(
                        text = "Because you liked " +
                                recommendation.matchedInterests.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (recommendation.energyMatch) {

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "✓ Matches your energy level",
                        style = MaterialTheme.typography.bodySmall,
                        color = WandrTheme.colors.success,
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
        ) {
            Text(
                text = "Accept Quest ✓",
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
        ) {
            Text(
                text = "Skip",
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Skip to get another quest that matches your filters",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = WandrTheme.colors.textSecondary,
        )
    }
}

// =============================================================
// EMPTY MYSTERY
// =============================================================

@Composable
private fun MysteryEmptyCard(
    isLoading: Boolean,
    onRetry: () -> Unit,
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface,
        ),

        shape =
            MaterialTheme.shapes.large,
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    color =
                        MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text =
                        "Finding your Mystery Quest...",

                    style =
                        MaterialTheme.typography.titleMedium,

                    textAlign =
                        TextAlign.Center,
                )

            } else {

                Text(
                    text =
                        "No Mystery Quest available",

                    style =
                        MaterialTheme.typography.titleMedium,

                    textAlign =
                        TextAlign.Center,
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Try changing your energy level or radius.",

                    style =
                        MaterialTheme.typography.bodyMedium,

                    color =
                        WandrTheme.colors.textSecondary,

                    textAlign =
                        TextAlign.Center,
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Button(
                    onClick = onRetry,
                ) {

                    Text(
                        text = "Try again"
                    )
                }
            }
        }
    }
}


// =============================================================
// OFFLINE BANNER
// =============================================================

@Composable
private fun OfflineBanner() {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                WandrTheme.colors.successContainer,
        ),

        shape =
            MaterialTheme.shapes.medium,
    ) {

        Row(
            modifier =
                Modifier.padding(
                    WandrTheme.spacing.md
                ),

            verticalAlignment =
                Alignment.CenterVertically,
        ) {

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        WandrTheme.colors.success
                    ),
            )

            Spacer(
                modifier = Modifier.width(
                    WandrTheme.spacing.md
                )
            )

            Column {

                Text(
                    text = "You're offline",

                    style =
                        MaterialTheme.typography.titleSmall,

                    color =
                        MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text =
                        "Showing your saved activities",

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        WandrTheme.colors.textSecondary,
                )
            }
        }
    }
}


// =============================================================
// SECTION TITLE
// =============================================================

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String? = null,
) {

    Column {

        Text(
            text = title,

            style =
                MaterialTheme.typography.titleLarge,

            color =
                MaterialTheme.colorScheme.onBackground,
        )

        if (subtitle != null) {

            Spacer(
                modifier = Modifier.height(
                    WandrTheme.spacing.xs
                )
            )

            Text(
                text = subtitle,

                style =
                    MaterialTheme.typography.bodySmall,

                color =
                    WandrTheme.colors.textSecondary,
            )
        }
    }
}


// =============================================================
// TAG FILTERS
// =============================================================

@Composable
private fun TagFilterRow(
    tags: List<String>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        tags.forEach { tag ->

            FilterChip(
                selected = selectedTag == tag,
                onClick = {
                    onTagSelected(
                        if (selectedTag == tag) {
                            null
                        } else {
                            tag
                        }
                    )
                },
                label = {
                    Text(
                        text = tag.lowercase(),
                        maxLines = 1,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface,
                    selectedContainerColor =
                        WandrTheme.colors.successContainer,
                    selectedLabelColor =
                        MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}


// =============================================================
// NORMAL QUEST CARD
// =============================================================

@Composable
private fun QuestCard(
    quest: Quest,
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface,
        ),

        shape =
            MaterialTheme.shapes.large,

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            ),
    ) {

        Column(
            modifier =
                Modifier.padding(
                    WandrTheme.spacing.lg
                ),
        ) {

            Row(
                verticalAlignment =
                    Alignment.Top,
            ) {

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(
                            MaterialTheme
                                .shapes
                                .medium
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        ),

                    contentAlignment =
                        Alignment.Center,
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.LocationOn,

                        contentDescription = null,

                        modifier =
                            Modifier.size(30.dp),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,
                    )
                }

                Spacer(
                    modifier = Modifier.width(
                        WandrTheme.spacing.md
                    )
                )

                Column(
                    modifier =
                        Modifier.weight(1f),
                ) {

                    Text(
                        text =
                            quest.title,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        maxLines = 2,

                        overflow =
                            TextOverflow.Ellipsis,
                    )

                    Spacer(
                        modifier = Modifier.height(
                            WandrTheme.spacing.xs
                        )
                    )

                    Text(
                        text =
                            quest.placeName
                                ?: "Nearby",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            WandrTheme.colors
                                .textSecondary,

                        maxLines = 1,

                        overflow =
                            TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(
                    WandrTheme.spacing.md
                )
            )

            if (
                !quest.description
                    .isNullOrBlank()
            ) {

                Text(
                    text =
                        quest.description,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        WandrTheme.colors
                            .textSecondary,

                    maxLines = 2,

                    overflow =
                        TextOverflow.Ellipsis,
                )

                Spacer(
                    modifier = Modifier.height(
                        WandrTheme.spacing.md
                    )
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        WandrTheme.spacing.sm
                    ),
            ) {

                if (quest.distanceKm != null) {

                    InfoChip(
                        icon =
                            Icons.Outlined.LocationOn,

                        text =
                            "${formatDistance(
                                quest.distanceKm
                            )} km",
                    )
                }

                if (
                    quest.estimatedDurationMin
                    != null
                ) {

                    InfoChip(
                        icon =
                            Icons.Outlined.AccessTime,

                        text =
                            "${quest.estimatedDurationMin} min",
                    )
                }

                InfoChip(
                    icon =
                        Icons.Outlined.Star,

                    text =
                        "${quest.pointsReward} XP",
                )
            }

            if (quest.tags.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(
                        WandrTheme.spacing.md
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            WandrTheme.spacing.xs
                        ),
                ) {

                    quest.tags
                        .take(3)
                        .forEach { tag ->

                            AssistChip(
                                onClick = {},

                                label = {
                                    Text(
                                        text = tag
                                    )
                                },

                                colors =
                                    AssistChipDefaults
                                        .assistChipColors(
                                            containerColor =
                                                WandrTheme
                                                    .colors
                                                    .successContainer
                                        ),
                            )
                        }
                }
            }
        }
    }
}


// =============================================================
// INFO CHIP
// =============================================================

@Composable
private fun InfoChip(
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    text: String,
) {

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
            )
            .padding(
                horizontal =
                    WandrTheme.spacing.sm,

                vertical =
                    WandrTheme.spacing.xs,
            ),

        verticalAlignment =
            Alignment.CenterVertically,
    ) {

        Icon(
            imageVector = icon,

            contentDescription = null,

            modifier =
                Modifier.size(15.dp),

            tint =
                MaterialTheme
                    .colorScheme
                    .primary,
        )

        Spacer(
            modifier = Modifier.width(
                WandrTheme.spacing.xs
            )
        )

        Text(
            text = text,

            style =
                MaterialTheme.typography.labelSmall,
        )
    }
}


// =============================================================
// LOADING
// =============================================================

@Composable
private fun LoadingSection() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),

        contentAlignment =
            Alignment.Center,
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {

            CircularProgressIndicator(
                color =
                    MaterialTheme.colorScheme.primary,
            )

            Spacer(
                modifier = Modifier.height(
                    WandrTheme.spacing.md
                )
            )

            Text(
                text =
                    "Finding quests near you...",

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    WandrTheme.colors.textSecondary,
            )
        }
    }
}


// =============================================================
// EMPTY QUESTS
// =============================================================

@Composable
private fun EmptyQuestsSection(
    hasFilter: Boolean,
    onClearFilter: () -> Unit,
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface,
        ),
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    WandrTheme.spacing.xl
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.LocationOn,

                contentDescription = null,

                modifier =
                    Modifier.size(40.dp),

                tint =
                    MaterialTheme
                        .colorScheme
                        .secondary,
            )

            Spacer(
                modifier = Modifier.height(
                    WandrTheme.spacing.md
                )
            )

            Text(
                text =
                    if (hasFilter) {
                        "No quests match this interest"
                    } else {
                        "No quests nearby"
                    },

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
            )

            Spacer(
                modifier = Modifier.height(
                    WandrTheme.spacing.xs
                )
            )

            Text(
                text =
                    if (hasFilter) {
                        "Try another interest or clear the filter."
                    } else {
                        "Try refreshing or moving to another area."
                    },

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    WandrTheme.colors
                        .textSecondary,

                textAlign =
                    TextAlign.Center,
            )

            if (hasFilter) {

                Spacer(
                    modifier = Modifier.height(
                        WandrTheme.spacing.md
                    )
                )

                Button(
                    onClick =
                        onClearFilter,
                ) {

                    Text(
                        text = "Clear filter"
                    )
                }
            }
        }
    }
}


// =============================================================
// ERROR
// =============================================================

@Composable
private fun ErrorSection(
    message: String?,
    onRefresh: () -> Unit,
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                WandrTheme.colors
                    .dangerContainer,
        ),
    ) {

        Column(
            modifier =
                Modifier.padding(
                    WandrTheme.spacing.lg
                ),
        ) {

            Text(
                text =
                    "Something went wrong",

                style =
                    MaterialTheme.typography.titleMedium,

                color =
                    WandrTheme.colors.danger,
            )

            if (!message.isNullOrBlank()) {

                Spacer(
                    modifier = Modifier.height(
                        WandrTheme.spacing.xs
                    )
                )

                Text(
                    text = message,

                    style =
                        MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(
                modifier = Modifier.height(
                    WandrTheme.spacing.md
                )
            )

            Button(
                onClick = onRefresh,
            ) {

                Text(
                    text = "Try again"
                )
            }
        }
    }
}


// =============================================================
// DISTANCE
// =============================================================

private fun formatDistance(
    distance: Double,
): String {

    return if (distance < 1.0) {

        String.format(
            "%.2f",
            distance
        )

    } else {

        String.format(
            "%.1f",
            distance
        )
    }
}


// =============================================================
// PREVIEW
// =============================================================

@Preview(
    showBackground = true
)
@Composable
private fun HomeScreenPreview() {

    WandrTheme {

        HomeScreen(

            state = HomeUiState(
                isLoading = false,

                allQuests = listOf(

                    Quest(
                        id = "1",

                        title =
                            "Try a new coffee shop",

                        description =
                            "Discover a new place and complete the challenge.",

                        difficultyLevel = 1,

                        estimatedDurationMin = 45,

                        pointsReward = 50,

                        coverImageUrl = null,

                        placeId = "place-1",

                        placeName = "Coffee Lab",

                        tags =
                            listOf(
                                "Coffee",
                                "Food"
                            ),

                        distanceKm = 0.8,
                    ),

                    Quest(
                        id = "2",

                        title =
                            "Explore a nearby park",

                        description =
                            "Take a walk and discover something new.",

                        difficultyLevel = 2,

                        estimatedDurationMin = 60,

                        pointsReward = 75,

                        coverImageUrl = null,

                        placeId = "place-2",

                        placeName = "El Virrey",

                        tags =
                            listOf(
                                "Outdoors",
                                "Nature"
                            ),

                        distanceKm = 1.4,
                    ),
                ),

                selectedTag = null,

                recommendations = listOf(

                    RecommendedQuest(

                        quest = Quest(

                            id = "3",

                            title =
                                "Discover a hidden gem",

                            description = null,

                            difficultyLevel = 1,

                            estimatedDurationMin = 30,

                            pointsReward = 40,

                            coverImageUrl = null,

                            placeId = "place-3",

                            placeName =
                                "La Candelaria",

                            tags =
                                listOf(
                                    "Culture"
                                ),

                            distanceKm = 2.0,
                        ),

                        matchedInterests =
                            listOf(
                                "Culture"
                            ),

                        energyMatch = true,
                    )
                ),

                locationSource =
                    UserLocation.Source.CURRENT,
            ),

            onRefresh = {},

            onTagSelected = {},

            onEnergySelected = {},

            onRadiusSelected = {},

            onRevealMystery = {},

            onSkipMystery = {},
        )
    }
}