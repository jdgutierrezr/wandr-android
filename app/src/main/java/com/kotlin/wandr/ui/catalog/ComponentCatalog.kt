package com.kotlin.wandr.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.components.AchievementCard
import com.kotlin.wandr.ui.components.Avatar
import com.kotlin.wandr.ui.components.AvatarStack
import com.kotlin.wandr.ui.components.BadgeStyle
import com.kotlin.wandr.ui.components.DangerButton
import com.kotlin.wandr.ui.components.LinkButton
import com.kotlin.wandr.ui.components.MainTab
import com.kotlin.wandr.ui.components.MilestoneCard
import com.kotlin.wandr.ui.components.NotificationsAction
import com.kotlin.wandr.ui.components.ObjectiveItem
import com.kotlin.wandr.ui.components.ObjectiveState
import com.kotlin.wandr.ui.components.PasswordTextField
import com.kotlin.wandr.ui.components.PrimaryButton
import com.kotlin.wandr.ui.components.QuestCard
import com.kotlin.wandr.ui.components.QuestRow
import com.kotlin.wandr.ui.components.RewardRow
import com.kotlin.wandr.ui.components.SearchField
import com.kotlin.wandr.ui.components.SecondaryButton
import com.kotlin.wandr.ui.components.SectionHeader
import com.kotlin.wandr.ui.components.SectionLabel
import com.kotlin.wandr.ui.components.SelectableCard
import com.kotlin.wandr.ui.components.StatCard
import com.kotlin.wandr.ui.components.WeekComparisonMessage
import com.kotlin.wandr.ui.components.WeekDaysRow
import com.kotlin.wandr.ui.components.WeeklyQuestsChart
import com.kotlin.wandr.domain.model.WeekComparison
import com.kotlin.wandr.domain.model.WeeklyQuests
import com.kotlin.wandr.ui.components.DropoffBar
import com.kotlin.wandr.ui.components.DropoffBars
import com.kotlin.wandr.ui.components.StatusBadge
import com.kotlin.wandr.ui.components.ToggleRow
import com.kotlin.wandr.ui.components.WandrBottomBar
import com.kotlin.wandr.ui.components.WandrChip
import com.kotlin.wandr.ui.components.WandrTextField
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.components.XpBadge
import com.kotlin.wandr.ui.theme.Cream
import com.kotlin.wandr.ui.theme.ForestGreen
import com.kotlin.wandr.ui.theme.Sage
import com.kotlin.wandr.ui.theme.Sand
import com.kotlin.wandr.ui.theme.Taupe
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Every token and component of the Wandr design system on one scrollable screen.
 * Open it with the previews below, or in the app (debug builds) from the Login screen.
 * When you create a component, add it here too.
 */
@Composable
fun ComponentCatalog(onBack: (() -> Unit)? = null) {
    Scaffold(
        topBar = { WandrTopBar(title = "Design system", onBack = onBack, streak = 3) { NotificationsAction(unreadCount = 2, onClick = {}) } },
        bottomBar = {
            var tab by remember { mutableStateOf(MainTab.MAP) }
            WandrBottomBar(selected = tab, onSelect = { tab = it })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xl),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(WandrTheme.spacing.screen),
        ) {
            TokensSection()
            ButtonsSection()
            InputsSection()
            BadgesAndChipsSection()
            QuestSection()
            ProgressSection()
            SelectionSection()
            PeopleSection()
            Spacer(Modifier.height(WandrTheme.spacing.xl))
        }
    }
}

@Composable
private fun CatalogGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
        SectionHeader(title = title)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokensSection() = CatalogGroup("Tokens") {
    SectionLabel("Brand palette")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
        listOf("163820" to ForestGreen, "5A7B62" to Sage, "BFA78A" to Sand, "9A876F" to Taupe, "FCFAF6" to Cream)
            .forEach { (hex, color) -> ColorSwatch(hex, color) }
    }
    SectionLabel("Type scale")
    Column {
        Text("Headline · Quest Complete!", style = MaterialTheme.typography.headlineSmall)
        Text("Title · Try a new coffee shop", style = MaterialTheme.typography.titleMedium)
        Text("Body · Find a spot you've never visited.", style = MaterialTheme.typography.bodyMedium)
        Text("LABEL · OBJECTIVES", style = MaterialTheme.typography.labelSmall)
    }
    SectionLabel("Hero gradient")
    Box(Modifier.fillMaxWidth().height(56.dp).background(WandrTheme.colors.heroGradient, MaterialTheme.shapes.medium))
}

@Composable
private fun ColorSwatch(hex: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(48.dp)
                .background(color, MaterialTheme.shapes.small)
                .border(1.dp, WandrTheme.colors.border, MaterialTheme.shapes.small)
        )
        Text("#$hex", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ButtonsSection() = CatalogGroup("Buttons") {
    PrimaryButton(text = "Accept Quest", onClick = {}, leadingIcon = Icons.Rounded.CheckCircle)
    PrimaryButton(text = "Continue", onClick = {}, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
    PrimaryButton(text = "Loading", onClick = {}, isLoading = true)
    SecondaryButton(text = "Give me another one", onClick = {}, leadingIcon = Icons.Rounded.Refresh)
    Row(verticalAlignment = Alignment.CenterVertically) {
        DangerButton(text = "Need Help / SOS", onClick = {}, leadingIcon = Icons.Rounded.Sos)
        LinkButton(text = "Skip for now", onClick = {})
    }
}

@Composable
private fun InputsSection() = CatalogGroup("Inputs") {
    SearchField(value = "", onValueChange = {}, onVoiceSearch = {})
    WandrTextField(value = "valentina@example.com", onValueChange = {}, label = "Email", leadingIcon = Icons.Outlined.Email)
    PasswordTextField(value = "password123", onValueChange = {})
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BadgesAndChipsSection() = CatalogGroup("Badges & chips") {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
        StatusBadge("Beginner Quest", icon = Icons.Rounded.Star)
        StatusBadge("Completed")
        StatusBadge("Locked", style = BadgeStyle.Neutral)
        StatusBadge("Food & Social", style = BadgeStyle.Solid)
        StatusBadge("SOS", style = BadgeStyle.Danger)
        XpBadge(50)
    }
    var selected by remember { mutableStateOf(setOf("Food", "Nature")) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
        listOf("Food" to Icons.Outlined.LocalCafe, "Art" to Icons.Outlined.Palette, "Sports" to Icons.Outlined.SportsSoccer, "Nature" to Icons.Outlined.Forest)
            .forEach { (label, icon) ->
                WandrChip(label, selected = label in selected, icon = icon, onClick = {
                    selected = if (label in selected) selected - label else selected + label
                })
            }
    }
}

@Composable
private fun QuestSection() = CatalogGroup("Quests") {
    QuestCard(
        title = "Salsa in the Park",
        imageUrl = null,
        category = "Music",
        badge = "Happening Now",
        location = "Parque 93, Chapinero",
        duration = "Today, 5:00 PM",
        xp = 150,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarStack(listOf("Juan David" to null, "Mateo Gomez" to null), extraCount = 12, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Join Quest", onClick = {}, modifier = Modifier.width(130.dp))
        }
    }
    QuestRow(
        title = "Try a new coffee shop in Chapinero",
        subtitle = "Café San Alberto · Calle 69a #4-71",
        imageUrl = null,
        label = "Beginner Quest",
        xp = 50,
    )
    SectionLabel("Objectives", trailing = "2 of 3")
    ObjectiveItem("Arrive at Calle 57 bakery alley", ObjectiveState.Done, subtitle = "Completed 10:15 AM · Geofence verified")
    ObjectiveItem("Order signature sourdough pan de bono", ObjectiveState.InProgress, subtitle = "Baked fresh everyday.")
    ObjectiveItem("Snap a photo review for community proof", ObjectiveState.Pending, requiresPhoto = true)
    RewardRow(title = "Rewards", description = "Unlocked immediately on completion", xp = 50, icon = Icons.Rounded.Diamond)
}

@Composable
private fun ProgressSection() = CatalogGroup("Progress") {
    Row(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
        StatCard(label = "Total Points", value = "1,250", icon = Icons.Rounded.Star, modifier = Modifier.weight(1f))
        StatCard(label = "Current Streak", value = "3", unit = "weeks", icon = Icons.Rounded.LocalFireDepartment, modifier = Modifier.weight(1f))
    }
    MilestoneCard(title = "Level 5", current = 850, target = 1000)
    Row(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
        AchievementCard("Park Explorer", "Walked 10km", Icons.Rounded.Park, isUnlocked = true, modifier = Modifier.weight(1f))
        AchievementCard("Early Bird", "Quest before 7AM", Icons.Outlined.WbSunny, isUnlocked = false, modifier = Modifier.weight(1f))
    }
    SectionLabel("Streak (BQ4)", trailing = "3/7 days")
    WeekDaysRow(activeDays = listOf(true, true, false, true, false, false, false), todayIndex = 3)
    WeeklyQuestsChart(
        weeks = listOf(WeeklyQuests("08 Sep", 2), WeeklyQuests("15 Sep", 4), WeeklyQuests("22 Sep", 1), WeeklyQuests("29 Sep", 3)),
    )
    WeekComparisonMessage(WeekComparison(currentWeek = 3, previousWeek = 1))
    SectionLabel("Abandonment funnel (BQ8)", trailing = "7 abandons")
    DropoffBars(bars = listOf(DropoffBar("Before step 1", 2), DropoffBar("After step 1", 4), DropoffBar("After step 2", 1)))
}

@Composable
private fun SelectionSection() = CatalogGroup("Selection") {
    var energy by remember { mutableStateOf("Active") }
    Row(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
        SelectableCard("Relaxed", "Quiet walks, cozy cafes & parks", Icons.Outlined.SelfImprovement, energy == "Relaxed", { energy = "Relaxed" }, Modifier.weight(1f))
        SelectableCard("Active", "Challenges, sports & long treks", Icons.AutoMirrored.Rounded.DirectionsRun, energy == "Active", { energy = "Active" }, Modifier.weight(1f))
    }
    var broadcasting by remember { mutableStateOf(true) }
    ToggleRow(
        title = "Broadcasting location: ${if (broadcasting) "ON" else "OFF"}",
        subtitle = "Visible to explorer circle · Chapinero area",
        icon = Icons.Outlined.MyLocation,
        checked = broadcasting,
        onCheckedChange = { broadcasting = it },
    )
}

@Composable
private fun PeopleSection() = CatalogGroup("People") {
    Row(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        Avatar("Sofia Rodriguez", imageUrl = null, size = 56.dp, isOnline = true, showRing = true)
        Avatar("Mateo Gomez", imageUrl = null, size = 40.dp, isOnline = true)
        Avatar("Camilo", imageUrl = null, size = 40.dp)
        AvatarStack(listOf("Ana" to null, "Luis" to null, "Sara" to null, "Tom" to null), extraCount = 8)
    }
}

@Preview(name = "Catalog", showBackground = true, heightDp = 3200)
@Composable
private fun ComponentCatalogPreview() {
    WandrTheme(darkTheme = false) { ComponentCatalog() }
}
