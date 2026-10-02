package com.kotlin.wandr.ui.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.domain.model.Achievement
import com.kotlin.wandr.domain.model.Badge
import com.kotlin.wandr.domain.model.EarnedBadge
import com.kotlin.wandr.domain.model.StreakSummary
import com.kotlin.wandr.domain.model.Tier
import com.kotlin.wandr.domain.model.UserProfile
import com.kotlin.wandr.domain.model.WeeklyQuests
import com.kotlin.wandr.ui.components.AchievementCard
import com.kotlin.wandr.ui.components.Avatar
import com.kotlin.wandr.ui.components.BadgeStyle
import com.kotlin.wandr.ui.components.MainTab
import com.kotlin.wandr.ui.components.MilestoneCard
import com.kotlin.wandr.ui.components.SecondaryButton
import com.kotlin.wandr.ui.components.SectionHeader
import com.kotlin.wandr.ui.components.SectionLabel
import com.kotlin.wandr.ui.components.StatCard
import com.kotlin.wandr.ui.components.StatusBadge
import com.kotlin.wandr.ui.components.WandrBottomBar
import com.kotlin.wandr.ui.components.WandrCard
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.components.WeekComparisonMessage
import com.kotlin.wandr.ui.components.WeekDaysRow
import com.kotlin.wandr.ui.components.WeeklyQuestsChart
import com.kotlin.wandr.ui.theme.WandrTheme
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The backend counts days in Bogota time, so "today" must too. */
private val BOGOTA = ZoneId.of("America/Bogota")

/**
 * Route = the stateful part: gets the ViewModel, observes it and handles one-time things
 * (snackbars, the celebration, leaving after signing out).
 */
@Composable
fun ProfileRoute(
    onSignedOut: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSignedOut) {
        if (state.isSignedOut) onSignedOut()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }
    LaunchedEffect(state.celebration) {
        state.celebration?.let {
            snackbarHostState.showSnackbar("Quest completed! +${it.xpEarned} XP · ${it.currentStreak} day streak")
            viewModel.onCelebrationShown()
        }
    }

    ProfileScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onRetryStreak = viewModel::loadStreak,
        onSignOut = viewModel::signOut,
        onSelectTab = onSelectTab,
    )
}

/** Screen = the stateless part. Only draws [state]. */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onRetryStreak: () -> Unit,
    onSignOut: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    todayIndex: Int = LocalDate.now(BOGOTA).dayOfWeek.value - 1,
) {
    val profile = state.profile
    val streak = state.streak

    Scaffold(
        topBar = {
            WandrTopBar(
                title = "Side Quests",
                streak = streak?.currentStreak ?: profile?.currentStreak,
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Log out")
                    }
                },
            )
        },
        bottomBar = { WandrBottomBar(selected = MainTab.PROFILE, onSelect = onSelectTab) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (profile == null && state.isLoading) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.lg),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (state.isShowingSavedData) {
                item { SavedDataBanner() }
            }
            if (profile != null) {
                item { ProfileHeader(profile) }
            }
            item { StatsRow(profile, streak) }
            if (profile != null) {
                item {
                    MilestoneCard(
                        title = "Level ${profile.level + 1}",
                        current = profile.xpIntoLevel,
                        target = UserProfile.XP_PER_LEVEL,
                    )
                }
            }

            // ---------- BQ4: streak evolution ----------
            when {
                streak != null -> {
                    item { ThisWeekCard(streak, todayIndex) }
                    item { WeeklyHistoryCard(streak) }
                    item {
                        SectionHeader(
                            title = "Achievements",
                            count = "${streak.unlockedAchievements}/${streak.achievements.size}",
                        )
                    }
                    // Two per row, like the mockup
                    streak.achievements.chunked(2).forEach { pair ->
                        item { AchievementRow(pair, state.badges) }
                    }
                }
                state.isStreakLoading -> item { StreakLoadingCard() }
                state.streakFailed -> item { StreakErrorCard(onRetry = onRetryStreak) }
            }
        }
    }
}

@Composable
private fun SavedDataBanner() {
    StatusBadge(text = "Offline · showing saved data", style = BadgeStyle.Neutral)
}

@Composable
private fun ProfileHeader(profile: UserProfile) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Avatar(name = profile.name, imageUrl = profile.profilePicture, size = 96.dp, showRing = true)
        Text(profile.name, style = MaterialTheme.typography.headlineSmall)
        StatusBadge(text = "Level ${profile.level} · ${profile.tier.displayName}")
    }
}

/** Points, streak and quests. The summary is the freshest source; the cached profile is the fallback. */
@Composable
private fun StatsRow(profile: UserProfile?, streak: StreakSummary?) {
    val points = streak?.points ?: profile?.currentXp
    val days = streak?.currentStreak ?: profile?.currentStreak
    Row(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md), modifier = Modifier.fillMaxWidth()) {
        StatCard(
            label = "Points",
            value = points?.let { formatNumber(it) } ?: "–",
            icon = Icons.Rounded.Star,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Streak",
            value = days?.toString() ?: "–",
            unit = if (days == 1) "day" else "days",
            icon = Icons.Rounded.LocalFireDepartment,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Quests",
            value = streak?.questsCompleted?.toString() ?: "–",
            icon = Icons.Rounded.CheckCircle,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThisWeekCard(streak: StreakSummary, todayIndex: Int) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            SectionLabel("This week", trailing = "${streak.activeDaysThisWeek}/7 days")
            WeekDaysRow(activeDays = streak.thisWeek, todayIndex = todayIndex)
        }
    }
}

@Composable
private fun WeeklyHistoryCard(streak: StreakSummary) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            SectionLabel("Quests per week", trailing = "Last ${streak.weeklyHistory.size} weeks")
            WeeklyQuestsChart(weeks = streak.weeklyHistory)
            streak.weekComparison?.let { WeekComparisonMessage(it) }
        }
    }
}

@Composable
private fun AchievementRow(pair: List<Achievement>, earned: List<EarnedBadge>) {
    Row(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md), modifier = Modifier.fillMaxWidth()) {
        pair.forEach { achievement ->
            AchievementCard(
                title = achievement.name,
                description = achievementDescription(achievement, earned),
                icon = Icons.Rounded.EmojiEvents,
                isUnlocked = achievement.isUnlocked,
                modifier = Modifier.weight(1f),
            )
        }
        // Odd number of achievements: keep the last card at half width
        if (pair.size == 1) Spacer(Modifier.weight(1f))
    }
}

/**
 * The RPC only sends id, name and unlocked. For unlocked ones, the date comes from the
 * badges the profile already downloaded (`user_badges`).
 */
private fun achievementDescription(achievement: Achievement, earned: List<EarnedBadge>): String {
    if (!achievement.isUnlocked) return "Keep exploring to unlock it"
    val earnedAt = earned.firstOrNull { it.badge.id == achievement.id }?.earnedAt ?: return "Unlocked"
    return "Unlocked " + DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH).format(earnedAt.atZone(BOGOTA))
}

@Composable
private fun StreakLoadingCard() {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Loading your weekly progress…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StreakErrorCard(onRetry: () -> Unit) {
    WandrCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            Text("We couldn't load your weekly progress", style = MaterialTheme.typography.titleMedium)
            Text(
                "Your streak history needs an internet connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = WandrTheme.colors.textSecondary,
            )
            SecondaryButton(text = "Try again", onClick = onRetry, leadingIcon = Icons.Rounded.Refresh)
        }
    }
}

private fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

// ---------- Previews ----------

private val previewProfile = UserProfile(
    id = "u1", name = "Valentina Gomez", email = "v@example.com", profilePicture = null,
    currentXp = 1250, level = 6, currentStreak = 3, tier = Tier.TRAILBLAZER, energyLevel = null,
)

private val previewStreak = StreakSummary(
    questsCompleted = 14,
    points = 1250,
    currentStreak = 3,
    thisWeek = listOf(true, true, false, true, false, false, false),
    weeklyHistory = listOf(
        WeeklyQuests("08 Sep", 2),
        WeeklyQuests("15 Sep", 4),
        WeeklyQuests("22 Sep", 1),
        WeeklyQuests("29 Sep", 3),
    ),
    achievements = listOf(
        Achievement("b1", "First Quest", true),
        Achievement("b2", "Park Explorer", true),
        Achievement("b3", "Early Bird", false),
        Achievement("b4", "Social Butterfly", false),
        Achievement("b5", "Culture Lover", false),
    ),
)

@Preview(showBackground = true, heightDp = 1500, name = "Profile with streak")
@Composable
private fun ProfileScreenPreview() {
    WandrTheme {
        ProfileScreen(
            state = ProfileUiState(
                isLoading = false,
                profile = previewProfile,
                streak = previewStreak,
                badges = listOf(EarnedBadge(Badge("b1", "First Quest", null, null), Instant.parse("2026-09-28T15:00:00Z"))),
            ),
            snackbarHostState = SnackbarHostState(),
            onRetryStreak = {}, onSignOut = {}, onSelectTab = {},
            todayIndex = 3,
        )
    }
}

@Preview(showBackground = true, name = "Streak failed (offline)")
@Composable
private fun ProfileScreenOfflinePreview() {
    WandrTheme {
        ProfileScreen(
            state = ProfileUiState(isLoading = false, profile = previewProfile, isShowingSavedData = true, streakFailed = true),
            snackbarHostState = SnackbarHostState(),
            onRetryStreak = {}, onSignOut = {}, onSelectTab = {},
            todayIndex = 3,
        )
    }
}
