package com.kotlin.wandr.ui.feature.events

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kotlin.wandr.domain.model.Event
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.ui.components.CoverImage
import com.kotlin.wandr.ui.components.MainTab
import com.kotlin.wandr.ui.components.WandrBottomBar
import com.kotlin.wandr.ui.theme.WandrTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.outlined.PersonOutline

@Composable
fun ExploreRoute(
    onSelectTab: (MainTab) -> Unit = {},
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ExploreScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onSelectTab = onSelectTab,
    )
}

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onRefresh: () -> Unit,
    onSelectTab: (MainTab) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Outdoor", "Food", "Culture", "Music")

    val filteredQuests = state.quests.filter { quest ->
        val matchesSearch = searchQuery.isBlank() || listOfNotNull(
            quest.title,
            quest.description,
            quest.placeName,
            quest.tags.joinToString(" "),
        ).joinToString(" ").contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategory == "All" ||
                quest.tags.any { it.equals(selectedCategory, ignoreCase = true) }

        matchesSearch && matchesCategory
    }

    val filteredEvents = state.events.filter { event ->
        searchQuery.isBlank() || listOfNotNull(
            event.title,
            event.description,
            event.placeName,
        ).joinToString(" ").contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 94.dp),
        ) {
            ExploreHeader()

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search quests in Bogotá.") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                shape = RoundedCornerShape(18.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "This Week",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = WandrTheme.colors.textSecondary,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                categories.forEach { category ->
                    ExploreCategoryChip(
                        text = category,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            ExploreSectionHeader(
                title = "Featured Experiences",
                onSeeAll = null,
            )

            Spacer(Modifier.height(12.dp))

            if (state.isLoadingEvents && state.events.isEmpty()) {
                LoadingExploreSection()
            } else if (filteredEvents.isEmpty()) {
                EmptyEventsSection()
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    filteredEvents.take(8).forEach { event ->
                        EventExploreCard(event)
                    }
                }
            }

            Spacer(Modifier.height(34.dp))

            ExploreSectionHeader(
                title = "Featured Quests",
                count = filteredQuests.size,
                onSeeAll = null,
            )

            Spacer(Modifier.height(12.dp))

            if (state.isLoadingQuests && state.quests.isEmpty()) {
                LoadingExploreSection()
            } else if (filteredQuests.isEmpty()) {
                EmptyQuestsSection()
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    filteredQuests.take(8).forEach { quest ->
                        FeaturedQuestCard(quest)
                    }
                }
            }

            if (state.isShowingSavedData) {
                Spacer(Modifier.height(18.dp))
                Text(
                    "Showing saved activities",
                    style = MaterialTheme.typography.bodySmall,
                    color = WandrTheme.colors.textSecondary,
                )
            }

            if (state.errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.errorMessage,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRefresh) { Text("Retry") }
                }
            }
        }

        WandrBottomBar(
            selected = MainTab.EXPLORE,
            onSelect = onSelectTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun ExploreHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WandrTheme.colors.successContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Explore,
                contentDescription = "Wandr",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            "Events & Quests",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        IconButton(onClick = {}) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications")
        }

        IconButton(onClick = {}) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = "Profile")
        }
    }
}

@Composable
private fun ExploreCategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            WandrTheme.colors.border,
        ),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ExploreSectionHeader(
    title: String,
    count: Int? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = WandrTheme.colors.textSecondary,
            )
        }
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) { Text("See all") }
        }
    }
}

@Composable
private fun EventExploreCard(event: Event) {
    Card(
        modifier = Modifier.width(235.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            CoverImage(
                imageUrl = event.coverImageUrl,
                contentDescription = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
            )

            Column(Modifier.padding(14.dp)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WandrTheme.colors.textSecondary,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatEventDate(event),
                        style = MaterialTheme.typography.bodySmall,
                        color = WandrTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (event.placeName != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        event.placeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = WandrTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedQuestCard(quest: Quest) {
    Card(
        modifier = Modifier.width(250.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Box {
                CoverImage(
                    imageUrl = quest.coverImageUrl,
                    contentDescription = quest.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                )
                if (quest.tags.isNotEmpty()) {
                    Text(
                        quest.tags.first().uppercase(),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(Modifier.padding(14.dp)) {
                Text(
                    quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WandrTheme.colors.textSecondary,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${quest.estimatedDurationMin ?: 0} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = WandrTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyEventsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            tint = WandrTheme.colors.successBorder,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "No events found.",
            style = MaterialTheme.typography.bodyLarge,
            color = WandrTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun EmptyQuestsSection() {
    Text(
        "No quests match your search or category.",
        modifier = Modifier.padding(vertical = 28.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = WandrTheme.colors.textSecondary,
    )
}

@Composable
private fun LoadingExploreSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

private fun formatEventDate(event: Event): String {
    val formatter = DateTimeFormatter.ofPattern("EEE · h:mm a")
    return event.startsAt
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
