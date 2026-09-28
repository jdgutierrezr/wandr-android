package com.kotlin.wandr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Photo placeholder + image from a URL. The soft background and icon show while the photo
 * loads or when there is none (the seed data uses example.com URLs that do not exist).
 */
@Composable
fun CoverImage(imageUrl: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.background(WandrTheme.colors.successContainer)) {
        Icon(Icons.Rounded.Image, contentDescription = null, tint = WandrTheme.colors.successBorder, modifier = Modifier.size(40.dp))
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** "📍 Chapinero Alto, Bogotá" / "🕒 ~25 mins" line. */
@Composable
fun MetaText(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs), modifier = modifier) {
        Icon(icon, contentDescription = null, tint = WandrTheme.colors.textSecondary, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = WandrTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Big quest / event card with the photo on top: "Upcoming Quests", "Quest of the Week".
 * [badge] goes over the photo (e.g. "Happening Now"); [footer] is free space for buttons.
 */
@Composable
fun QuestCard(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    category: String? = null,
    location: String? = null,
    duration: String? = null,
    xp: Int? = null,
    badge: String? = null,
    imageHeight: Dp = 160.dp,
    onClick: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    WandrCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = 0.dp) {
        Box {
            CoverImage(imageUrl, contentDescription = title, modifier = Modifier.fillMaxWidth().height(imageHeight))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(WandrTheme.spacing.md),
            ) {
                if (category != null) StatusBadge(text = category, style = BadgeStyle.Solid) else Box {}
                if (badge != null) StatusBadge(text = badge)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
            modifier = Modifier.padding(WandrTheme.spacing.lg),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
                if (location != null) MetaText(location, Icons.Outlined.LocationOn, Modifier.weight(1f, fill = false))
                if (duration != null) MetaText(duration, Icons.Outlined.Schedule)
            }
            if (xp != null) XpBadge(xp)
            footer()
        }
    }
}

/** Compact quest row with a thumbnail on the right: the card over the Discovery Map. */
@Composable
fun QuestRow(
    title: String,
    subtitle: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    label: String? = null,
    xp: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    WandrCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs)) {
                if (label != null) StatusBadge(text = label)
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WandrTheme.colors.textSecondary, maxLines = 1)
            }
            Box(contentAlignment = Alignment.BottomCenter) {
                CoverImage(imageUrl, contentDescription = null, modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small))
                if (xp != null) XpBadge(xp, style = BadgeStyle.Solid, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

/** State of a step in "Quest Objectives". */
enum class ObjectiveState { Pending, InProgress, Done }

/**
 * One step of a quest. Done = green check and struck-through title; InProgress = spinner and a
 * green left edge; a camera icon marks steps that need a photo.
 */
@Composable
fun ObjectiveItem(
    title: String,
    state: ObjectiveState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    requiresPhoto: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = WandrTheme.colors
    val highlighted = state == ObjectiveState.InProgress
    WandrCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = if (highlighted) colors.successBorder else colors.border,
        contentPadding = WandrTheme.spacing.md,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            ObjectiveMarker(state)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal),
                    textDecoration = if (state == ObjectiveState.Done) TextDecoration.LineThrough else null,
                    color = if (state == ObjectiveState.Done) colors.textSecondary else MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            when {
                highlighted -> StatusBadge(text = "In Progress")
                requiresPhoto -> Icon(Icons.Outlined.CameraAlt, contentDescription = "Needs a photo", tint = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ObjectiveMarker(state: ObjectiveState) {
    val size = 22.dp
    when (state) {
        ObjectiveState.Done -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
        ) { Icon(Icons.Rounded.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
        ObjectiveState.InProgress -> CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = WandrTheme.colors.successContainer,
            modifier = Modifier.size(size),
        )
        ObjectiveState.Pending -> Box(
            Modifier.size(size).clip(RoundedCornerShape(6.dp)).border(1.5.dp, WandrTheme.colors.border, RoundedCornerShape(6.dp))
        )
    }
}

/** "Rewards · Unlocked immediately on completion ....... [💎 +50 XP]" block of Quest Details. */
@Composable
fun RewardRow(title: String, description: String, xp: Int, icon: ImageVector, modifier: Modifier = Modifier) {
    WandrCard(modifier = modifier.fillMaxWidth(), containerColor = WandrTheme.colors.locked, borderColor = WandrTheme.colors.locked) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = WandrTheme.colors.textSecondary)
            }
            IconCircle(icon, size = 32.dp, containerColor = MaterialTheme.colorScheme.primary, iconColor = MaterialTheme.colorScheme.onPrimary)
            XpBadge(xp)
        }
    }
}
