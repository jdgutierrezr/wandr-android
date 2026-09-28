package com.kotlin.wandr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kotlin.wandr.R

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = Poppins,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Type scale taken from the mockups:
 * - display / headline: screen and quest titles ("Quest Complete!", "Try a new coffee shop")
 * - title: card titles and top bars
 * - body: descriptions
 * - label: buttons, chips, badges, and the small uppercase section labels ("OBJECTIVES")
 */
val Typography = Typography(
    displayLarge = style(48, 56, FontWeight.Bold),
    displayMedium = style(40, 48, FontWeight.Bold),
    displaySmall = style(32, 40, FontWeight.SemiBold),
    headlineLarge = style(28, 36, FontWeight.SemiBold),
    headlineMedium = style(24, 32, FontWeight.SemiBold),
    headlineSmall = style(22, 28, FontWeight.SemiBold),
    titleLarge = style(20, 28, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.SemiBold),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(10, 14, FontWeight.SemiBold, letterSpacing = 0.8),
)
