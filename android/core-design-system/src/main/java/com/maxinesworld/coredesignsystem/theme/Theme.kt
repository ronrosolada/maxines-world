package com.maxinesworld.coredesignsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

fun maxinesTypography(
    displayFont: FontFamily = FontFamily.Default,
    bodyFont: FontFamily = FontFamily.Default
) = Typography(
    displayLarge = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
)

@Composable
fun MaxinesWorldTheme(
    displayFont: FontFamily = FontFamily.Default,
    bodyFont: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val typography = remember(displayFont, bodyFont) {
        maxinesTypography(displayFont, bodyFont)
    }
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = typography,
        content = content
    )
}
