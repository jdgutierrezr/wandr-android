package com.kotlin.wandr.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * The base of almost every block in the mockups: a white surface with a thin border and
 * rounded corners. Pass [onClick] to make the whole card tappable.
 */
@Composable
fun WandrCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = WandrTheme.colors.border,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: Dp = WandrTheme.spacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(1.dp, borderColor)
    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = containerColor, border = border, content = inner)
    } else {
        Surface(modifier = modifier, shape = shape, color = containerColor, border = border, content = inner)
    }
}

/** Icon inside a soft circle, like the ones in stat cards, rewards and achievements. */
@Composable
fun IconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = WandrTheme.colors.successContainer,
    iconColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(size * 0.5f))
    }
}
