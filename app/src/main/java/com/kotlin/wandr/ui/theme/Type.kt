package com.kotlin.wandr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kotlin.wandr.R

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private val Default = Typography()

// Material 3 default sizes and weights, using Poppins for every style
val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Poppins),
    displayMedium = Default.displayMedium.copy(fontFamily = Poppins),
    displaySmall = Default.displaySmall.copy(fontFamily = Poppins),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Poppins),
    headlineMedium = Default.headlineMedium.copy(fontFamily = Poppins),
    headlineSmall = Default.headlineSmall.copy(fontFamily = Poppins),
    titleLarge = Default.titleLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.copy(fontFamily = Poppins),
    titleSmall = Default.titleSmall.copy(fontFamily = Poppins),
    bodyLarge = Default.bodyLarge.copy(fontFamily = Poppins),
    bodyMedium = Default.bodyMedium.copy(fontFamily = Poppins),
    bodySmall = Default.bodySmall.copy(fontFamily = Poppins),
    labelLarge = Default.labelLarge.copy(fontFamily = Poppins),
    labelMedium = Default.labelMedium.copy(fontFamily = Poppins),
    labelSmall = Default.labelSmall.copy(fontFamily = Poppins)
)
