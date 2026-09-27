package com.kotlin.wandr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Cream,
    secondary = Sage,
    onSecondary = Cream,
    tertiary = Sand,
    onTertiary = ForestGreen,
    background = Cream,
    onBackground = ForestGreen,
    surface = Cream,
    onSurface = ForestGreen,
    outline = Taupe
)

private val DarkColorScheme = darkColorScheme(
    primary = Sand,
    onPrimary = ForestGreen,
    secondary = Sage,
    onSecondary = Cream,
    tertiary = Taupe,
    onTertiary = Cream,
    background = ForestGreen,
    onBackground = Cream,
    surface = ForestGreen,
    onSurface = Cream,
    outline = Taupe
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
