package com.kotlin.wandr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.theme.WandrTheme

/** Rounded green progress bar (milestones, quest progress, timers). [progress] goes from 0 to 1. */
@Composable
fun WandrProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        color = MaterialTheme.colorScheme.primary,
        trackColor = WandrTheme.colors.successContainer,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
        gapSize = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(MaterialTheme.shapes.small),
    )
}

/** Small stat: "⭐ Total Points  1,250", "🔥 Current Streak  3 weeks". */
@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, unit: String? = null) {
    WandrCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = WandrTheme.colors.textSecondary)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            if (unit != null) Text(unit, style = MaterialTheme.typography.bodyMedium, color = WandrTheme.colors.textSecondary)
        }
    }
}

/** "NEXT MILESTONE  Level 5 ....... 150 pts to go  [=========   ] 850 / 1,000 XP". */
@Composable
fun MilestoneCard(title: String, current: Int, target: Int, modifier: Modifier = Modifier, label: String = "Next milestone") {
    WandrCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
            SectionLabel(label, trailing = "${(target - current).coerceAtLeast(0)} pts to go")
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("$current / $target XP", style = MaterialTheme.typography.bodySmall, color = WandrTheme.colors.textSecondary)
            }
            WandrProgressBar(progress = if (target == 0) 0f else current.toFloat() / target)
        }
    }
}

/** Achievement tile. Locked ones are grey with a padlock, like "Early Bird". */
@Composable
fun AchievementCard(
    title: String,
    description: String,
    icon: ImageVector,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = WandrTheme.colors
    WandrCard(
        modifier = modifier,
        containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else colors.locked,
        borderColor = if (isUnlocked) colors.border else colors.locked,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
            if (isUnlocked) {
                IconCircle(icon, size = 36.dp)
            } else {
                IconCircle(Icons.Outlined.Lock, size = 36.dp, containerColor = colors.border, iconColor = colors.onLocked)
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else colors.onLocked,
                )
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            StatusBadge(
                text = if (isUnlocked) "Completed" else "Locked",
                style = if (isUnlocked) BadgeStyle.Success else BadgeStyle.Neutral,
            )
        }
    }
}
