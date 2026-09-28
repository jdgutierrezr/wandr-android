package com.kotlin.wandr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Cream,
    primaryContainer = Mint,
    onPrimaryContainer = ForestGreen,
    secondary = Sage,
    onSecondary = Cream,
    secondaryContainer = Mint,
    onSecondaryContainer = ForestGreen,
    tertiary = Sand,
    onTertiary = ForestGreen,
    background = Cream,
    onBackground = Ink,
    // Cards are white on the cream background, like in the mockups
    surface = White,
    onSurface = Ink,
    surfaceVariant = Stone,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = Cream,
    surfaceContainerHigh = Stone,
    outline = Taupe,
    outlineVariant = Line,
    error = Coral,
    errorContainer = CoralLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = Sand,
    onPrimary = ForestGreen,
    primaryContainer = ForestLine,
    onPrimaryContainer = Cream,
    secondary = Sage,
    onSecondary = Cream,
    tertiary = Taupe,
    onTertiary = Cream,
    background = ForestDeep,
    onBackground = Cream,
    surface = ForestSurface,
    onSurface = Cream,
    surfaceVariant = ForestLine,
    onSurfaceVariant = Sand,
    outline = Taupe,
    outlineVariant = ForestLine,
)

@Composable
fun WandrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Android 12+) replaces the brand palette with wallpaper colors,
    // so it is off by default to keep Wandr's identity consistent.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalWandrColors provides if (darkTheme) DarkWandrColors else LightWandrColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WandrShapes,
            content = content
        )
    }
}

/** Wandr tokens, next to `MaterialTheme.colorScheme` / `.typography`. */
object WandrTheme {
    val colors: WandrColors
        @Composable @ReadOnlyComposable get() = LocalWandrColors.current
    val spacing = WandrSpacing
    val radius = WandrRadius
}
