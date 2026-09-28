package com.kotlin.wandr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kotlin.wandr.ui.theme.WandrTheme

/** Small uppercase label above a group: "OBJECTIVES", "INTERESTS", "ENERGY LEVEL". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = WandrTheme.colors.textSecondary)
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Section title with an optional counter and action: "Achievements  2/8 ........ View All". */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        if (count != null) StatusBadge(text = count, style = BadgeStyle.Neutral)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            if (actionText != null) {
                TextButton(onClick = onAction) { Text(actionText, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}
