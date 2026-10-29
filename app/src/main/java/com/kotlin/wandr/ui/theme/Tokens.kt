package com.kotlin.wandr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class WandrColors(
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val successContainer: Color,
    val successBorder: Color,
    val locked: Color,
    val onLocked: Color,
    val danger: Color,
    val dangerContainer: Color,
    val heroGradient: Brush,
)

val LightWandrColors = WandrColors(
    textSecondary = InkMuted,
    border = Line,
    success = ForestGreen,
    successContainer = Mint,
    successBorder = MintStrong,
    locked = Stone,
    onLocked = InkMuted,
    danger = Coral,
    dangerContainer = CoralLight,
    heroGradient = Brush.verticalGradient(listOf(Mint, Cream, Peach)),
)

val DarkWandrColors = WandrColors(
    textSecondary = Sand,
    border = ForestLine,
    success = Cream,
    successContainer = ForestLine,
    successBorder = Sage,
    locked = ForestSurface,
    onLocked = Taupe,
    danger = Color(0xFFF08A80),
    dangerContainer = Color(0xFF4A1F1B),
    heroGradient = Brush.verticalGradient(listOf(ForestSurface, ForestDeep)),
)

object WandrSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    val screen = 20.dp
}

object WandrRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

val WandrShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(WandrRadius.sm),
    medium = RoundedCornerShape(WandrRadius.md),
    large = RoundedCornerShape(WandrRadius.lg),
    extraLarge = RoundedCornerShape(WandrRadius.xl),
)

val LocalWandrColors = staticCompositionLocalOf { LightWandrColors }
