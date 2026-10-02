package com.kotlin.wandr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.theme.WandrTheme

/** One bar of [DropoffBars]. */
data class DropoffBar(val label: String, val count: Int)

/**
 * Horizontal bars for the abandonment funnel (BQ8): one row per step, the label on top and the
 * count at the end of the bar. One series and one hue; the step with most abandons is solid,
 * the rest lighter, and every bar shows its number, so color is never the only cue.
 */
@Composable
fun DropoffBars(bars: List<DropoffBar>, modifier: Modifier = Modifier) {
    val max = bars.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val worst = bars.indices.maxByOrNull { bars[it].count }?.takeIf { bars[it].count > 0 }
    Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md), modifier = modifier.fillMaxWidth()) {
        bars.forEachIndexed { index, bar ->
            val isWorst = index == worst
            Column(
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs),
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = "${bar.label}: ${bar.count} ${if (bar.count == 1) "abandon" else "abandons"}"
                },
            ) {
                Text(
                    bar.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isWorst) MaterialTheme.colorScheme.onSurface else WandrTheme.colors.textSecondary,
                    fontWeight = if (isWorst) FontWeight.SemiBold else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
                    Box(Modifier.weight(1f).height(14.dp)) {
                        // Empty steps keep a sliver so the row does not look missing
                        val fraction = (bar.count.toFloat() / max).coerceAtLeast(0.02f)
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(14.dp)
                                .background(
                                    if (isWorst) MaterialTheme.colorScheme.primary else WandrTheme.colors.successBorder,
                                    RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                                ),
                        )
                    }
                    Text(
                        "${bar.count}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isWorst) FontWeight.Bold else null,
                        modifier = Modifier.width(28.dp),
                    )
                }
            }
        }
    }
}
