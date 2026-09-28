package com.kotlin.wandr.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.theme.WandrTheme

/** Look of a [StatusBadge]. */
enum class BadgeStyle { Success, Neutral, Solid, Danger }

/**
 * Small pill with a status: "Completed", "In Progress", "Beginner Quest", "Trailblazer", "Locked".
 */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: BadgeStyle = BadgeStyle.Success,
    icon: ImageVector? = null,
) {
    val colors = WandrTheme.colors
    val (container, content) = when (style) {
        BadgeStyle.Success -> colors.successContainer to colors.success
        BadgeStyle.Neutral -> colors.locked to colors.onLocked
        BadgeStyle.Solid -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        BadgeStyle.Danger -> colors.dangerContainer to colors.danger
    }
    Surface(shape = CircleShape, color = container, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

/** "+50 XP" reward badge. */
@Composable
fun XpBadge(xp: Int, modifier: Modifier = Modifier, style: BadgeStyle = BadgeStyle.Success) {
    StatusBadge(text = "+$xp XP", modifier = modifier, style = style, icon = Icons.Rounded.Bolt)
}

/**
 * Filter / choice chip: categories on the map, interests in onboarding, tabs of Side Quests.
 * Selected = soft green with a green border.
 */
@Composable
fun WandrChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = WandrTheme.colors
    val container = if (selected) colors.successContainer else MaterialTheme.colorScheme.surface
    val border = if (selected) colors.successBorder else colors.border
    val content: Color = if (selected) colors.success else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, border),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}
