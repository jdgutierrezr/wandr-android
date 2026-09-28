package com.kotlin.wandr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Top bar of the main screens: "Discovery Map  🔥3 ...... [filters] [bell] [avatar]".
 * [onBack] adds the back arrow (detail screens). [streak] shows the fire counter next to the title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WandrTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    streak: Int? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (streak != null) StatusBadge(text = "$streak", icon = Icons.Rounded.LocalFireDepartment)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = modifier,
    )
}

/** Bell with a dot when there are unread notifications. */
@Composable
fun NotificationsAction(unreadCount: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (unreadCount > 0) Badge(containerColor = WandrTheme.colors.danger) }) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
        }
    }
}

/** Tabs of the bottom bar, in the order of the mockups. */
enum class MainTab(val label: String, val icon: ImageVector) {
    MAP("Map", Icons.Outlined.Explore),
    FRIENDS("Friends", Icons.Outlined.Group),
    EVENTS("Events", Icons.Outlined.CalendarMonth),
    LEADERBOARD("Leaderboard", Icons.Outlined.EmojiEvents),
    PROFILE("Profile", Icons.Outlined.Person),
}

@Composable
fun WandrBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        HorizontalDivider(color = WandrTheme.colors.border)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            MainTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = {
                        Text(tab.label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), maxLines = 1, softWrap = false)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = WandrTheme.colors.successContainer,
                        unselectedIconColor = WandrTheme.colors.textSecondary,
                        unselectedTextColor = WandrTheme.colors.textSecondary,
                    ),
                )
            }
        }
    }
}
