package com.maxinesworld.coredesignsystem.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val LocalAnimationsDisabled = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = VillageTeal, onPrimary = White,
    primaryContainer = VillageTeal.copy(alpha = 0.12f),
    secondary = SunshineGold, onSecondary = OnGold,
    secondaryContainer = SunshineGold.copy(alpha = 0.12f),
    tertiary = Coral, onTertiary = OnCoral,
    surface = SurfaceLight, onSurface = Ink,
    surfaceContainer = SurfaceContainer,
    background = SurfaceLight, onBackground = Ink,
    error = ErrorRed, onError = OnError
)

internal fun maxinesTypography(
    displayFont: FontFamily,
    bodyFont: FontFamily,
): Typography = Typography(
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
    labelSmall = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
)

@Composable
fun MaxinesWorldTheme(
    displayFont: FontFamily = FontFamily.Default,
    bodyFont: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit,
) {
    val typography = remember(displayFont, bodyFont) {
        maxinesTypography(displayFont, bodyFont)
    }
    val context = LocalContext.current
    val animationsDisabled = remember(context) {
        !animationsEnabledForScale(
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        )
    }
    CompositionLocalProvider(LocalAnimationsDisabled provides animationsDisabled) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = typography,
            content = content
        )
    }
}

internal fun animationsEnabledForScale(animatorDurationScale: Float): Boolean =
    animatorDurationScale > 0f
