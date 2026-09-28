package com.kotlin.wandr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Big selectable option with an icon: "Relaxed" / "Active" in onboarding.
 * Selected = green border and a check in the corner.
 */
@Composable
fun SelectableCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WandrTheme.colors
    WandrCard(
        modifier = modifier,
        onClick = onClick,
        borderColor = if (selected) MaterialTheme.colorScheme.primary else colors.border,
        containerColor = if (selected) colors.successContainer else MaterialTheme.colorScheme.surface,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconCircle(icon, containerColor = if (selected) MaterialTheme.colorScheme.surface else colors.successContainer)
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center)
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd),
                )
            }
        }
    }
}

/** Card with a switch: "Broadcasting location: ON", "Share live progress with friends". */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    WandrCard(modifier = modifier.fillMaxWidth(), contentPadding = WandrTheme.spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md)) {
            if (icon != null) IconCircle(icon, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WandrTheme.colors.textSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = WandrTheme.colors.locked,
                    uncheckedBorderColor = WandrTheme.colors.border,
                ),
            )
        }
    }
}
