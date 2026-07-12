package com.maxinesworld.coredesignsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Font families — default to system rounded fonts, overridden via MaxinesWorldTheme ───
var AppDisplayFont: FontFamily = FontFamily.Default
var AppBodyFont: FontFamily = FontFamily.Default

private val LightColorScheme = lightColorScheme(
    primary = VillageTeal,
    onPrimary = White,
    primaryContainer = VillageTeal.copy(alpha = 0.12f),
    secondary = SunshineGold,
    onSecondary = White,
    secondaryContainer = SunshineGold.copy(alpha = 0.12f),
    tertiary = Coral,
    surface = SurfaceLight,
    surfaceContainer = SurfaceContainer,
    background = SurfaceLight,
    error = ErrorRed
)

val MaxinesTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppDisplayFont, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontFamily = AppDisplayFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = AppDisplayFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = AppDisplayFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = AppDisplayFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = AppDisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = AppBodyFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = AppBodyFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = AppBodyFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = AppBodyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = AppBodyFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
)

@Composable
fun MaxinesWorldTheme(
    displayFont: FontFamily = AppDisplayFont,
    bodyFont: FontFamily = AppBodyFont,
    content: @Composable () -> Unit
) {
    AppDisplayFont = displayFont
    AppBodyFont = bodyFont
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MaxinesTypography,
        content = content
    )
}
