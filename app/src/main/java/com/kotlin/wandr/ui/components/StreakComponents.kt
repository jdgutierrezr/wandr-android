package com.kotlin.wandr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.domain.model.WeekComparison
import com.kotlin.wandr.domain.model.WeeklyQuests
import com.kotlin.wandr.ui.theme.WandrTheme

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/**
 * "M T W T F S S" with a filled circle on the days the user finished a quest.
 * [activeDays] has 7 values, Monday first. [todayIndex] (0 = Monday) gets a ring.
 */
@Composable
fun WeekDaysRow(activeDays: List<Boolean>, modifier: Modifier = Modifier, todayIndex: Int? = null) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = modifier.fillMaxWidth()) {
        activeDays.take(DAY_LETTERS.size).forEachIndexed { index, isActive ->
            val isToday = index == todayIndex
            val description = buildString {
                append(DAY_NAMES[index])
                if (isToday) append(", today")
                append(if (isActive) ": quest completed" else ": no quests")
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs),
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Text(
                    DAY_LETTERS[index],
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else WandrTheme.colors.textSecondary,
                    fontWeight = if (isToday) FontWeight.Bold else null,
                )
                DayDot(isActive = isActive, isToday = isToday)
            }
        }
    }
}

@Composable
private fun DayDot(isActive: Boolean, isToday: Boolean) {
    val ring = when {
        isToday -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        !isActive -> Modifier.border(1.dp, WandrTheme.colors.border, CircleShape)
        else -> Modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .then(ring)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                CircleShape,
            ),
    ) {
        if (isActive) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Bar chart of quests per week (BQ4), oldest week on the left. One series, one hue: the
 * current week (the last bar) is solid, previous weeks are lighter. Every bar has its value
 * on top, so the numbers never depend on color alone.
 */
@Composable
fun WeeklyQuestsChart(weeks: List<WeeklyQuests>, modifier: Modifier = Modifier, chartHeight: Int = 120) {
    val max = weeks.maxOfOrNull { it.quests }?.coerceAtLeast(1) ?: 1
    val summary = weeks.joinToString { "${it.label}: ${it.quests}" }
    Row(
        horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.md),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Quests per week. $summary" },
    ) {
        weeks.forEachIndexed { index, week ->
            val isCurrent = index == weeks.lastIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xs),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    "${week.quests}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isCurrent) FontWeight.Bold else null,
                )
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight.dp),
                ) {
                    // An empty week still shows a thin baseline, so the slot does not look missing
                    val fraction = (week.quests.toFloat() / max).coerceAtLeast(0.03f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight(fraction)
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary else WandrTheme.colors.successBorder,
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            ),
                    )
                }
                Text(
                    if (isCurrent) "This week" else week.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else WandrTheme.colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** "2 quests more than last week. Keep it up!" with an arrow for the trend. */
@Composable
fun WeekComparisonMessage(comparison: WeekComparison, modifier: Modifier = Modifier) {
    val (icon, style) = when (comparison.trend) {
        WeekComparison.Trend.UP -> Icons.AutoMirrored.Rounded.TrendingUp to BadgeStyle.Success
        WeekComparison.Trend.DOWN -> Icons.AutoMirrored.Rounded.TrendingDown to BadgeStyle.Danger
        WeekComparison.Trend.SAME -> Icons.AutoMirrored.Rounded.TrendingFlat to BadgeStyle.Neutral
    }
    val delta = when (comparison.trend) {
        WeekComparison.Trend.UP -> "+${comparison.difference}"
        WeekComparison.Trend.DOWN -> "${comparison.difference}"
        WeekComparison.Trend.SAME -> "="
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        StatusBadge(text = delta, icon = icon, style = style)
        Text(
            comparison.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
