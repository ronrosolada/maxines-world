package com.maxinesworld.coredesignsystem.theme

import androidx.compose.ui.graphics.Color

// ─── Core Brand Palette (from design.md) ───
val VillageTeal = Color(0xFF087F83)
val Coral = Color(0xFFF47C6B)
val SunshineGold = Color(0xFFF5B82E)
val LeafGreen = Color(0xFF66A83E)
val StoryPurple = Color(0xFF7653B5)
val SkyBlue = Color(0xFF3C9DDB)
val Ink = Color(0xFF183B4A)
val Cream = Color(0xFFFFF7E8)
val White = Color(0xFFFFFFFF)

// ─── Feedback Colors ───
val SuccessGreen = Color(0xFF2F9E62)
val Warning = Color(0xFFD98716)
val Review = Color(0xFFD9534F)

// ─── Subject Colors (from design.md) ───
object SubjectColors {
    val English = SubjectPalette(primary = StoryPurple, surface = Color(0xFFF1EBFA))
    val Filipino = SubjectPalette(primary = Color(0xFFD96555), surface = Color(0xFFFCEBE7))
    val Mathematics = SubjectPalette(primary = Color(0xFF218CC8), surface = Color(0xFFE6F4FC))
    val Science = SubjectPalette(primary = Color(0xFF57943B), surface = Color(0xFFEDF7E8))
    val History = SubjectPalette(primary = Color(0xFFB87916), surface = Color(0xFFFFF3D7))
}

data class SubjectPalette(val primary: Color, val surface: Color)

/** Shared tokens for the child Playroom surface. */
object PlayroomColors {
    val GoldTop = Color(0xFFFFD76E)
    val GoldMid = Color(0xFFFFB84D)
    val TealPressed = Color(0xFF06676A)
    val Muted = Color(0xFF4E5F66)
    val FallbackSurface = Color(0xFFF2F2F0)
    val KeepsakeHeading = Color(0xFF5C2E00)
    val BrandLabel = Color(0xFF7A3B00)
    val LockedSurfaceText = Color(0xFFFFE9A8)
    val SanctuarySurface = Color(0xFFE8F6EE)
    val SanctuaryBoardSurface = Color(0xFFBFE5CC)
    val StickerWonStart = Color(0xFFFFF6D6)
    val StickerWonEnd = Color(0xFFFFE9A8)
    val StickerLockedStart = Color(0xFFF3EFE2)
    val StickerLockedEnd = Color(0xFFECE7D8)
    val StickerLockedBorder = Color(0xFFD9C48F)
    val StickerLockedText = Color(0xFF8A6A3A)

    val SubjectAccent = mapOf(
        "mathematics" to SubjectColors.Mathematics.primary,
        "english" to SubjectColors.English.primary,
        "science" to SubjectColors.Science.primary,
        "filipino" to Color(0xFFD96555),
        "araling_panlipunan" to HeritageGold,
        "makabansa" to Color(0xFF8B5E34),
        "gmrc" to KindnessTealText,
    )

    val SubjectPale = mapOf(
        "mathematics" to SubjectColors.Mathematics.surface,
        "english" to SubjectColors.English.surface,
        "science" to SubjectColors.Science.surface,
        "filipino" to Color(0xFFFCEBE7),
        "araling_panlipunan" to Color(0xFFFFF3D7),
        "makabansa" to Color(0xFFF4EBDD),
        "gmrc" to Color(0xFFE5F7F5),
    )
}

// ─── Subject Color Tokens (package-level) ───
val HeritageGold = Color(0xFFB87916)
val KindnessTeal = Color(0xFF26A69A)
val KindnessTealText = Color(0xFF00685F)
val Molasses = Color(0xFF2B2100)     // onSecondary / dark text on gold

// ─── Accessible on-colors ───
// Saturated brand fills are retained; these dark companions keep text and
// status glyphs readable without changing the visual identity of the app.
val OnGold = Molasses
val OnCoral = Color(0xFF3D1109)
val OnLeafGreen = Color(0xFF12240A)
val OnSkyBlue = Color(0xFF06243A)
val OnSuccess = Color(0xFF06240F)
val OnError = Color(0xFF2B0000)
val SuccessGreenText = Color(0xFF1E6B41)
val ReviewText = Color(0xFFB03A36)

// ─── Village v2 scrim tokens ───
val DeepNight = Color(0xFF0B2A36)

// ─── Legacy aliases (backward compat) ───
val Teal40 = VillageTeal
val Amber40 = SunshineGold
val Orange40 = Coral
val ErrorRed = Review
val EnergyGold = SunshineGold
val SurfaceLight = Cream
val SurfaceContainer = Color(0xFFF5F0E8)
val Teal90 = VillageTeal.copy(alpha = 0.12f)
val Amber90 = SunshineGold.copy(alpha = 0.12f)
val Orange80 = Coral.copy(alpha = 0.3f)
val NumberMarketRed = SkyBlue
val DiscoveryLabPurple = LeafGreen
val HeritageHarborBrown = Color(0xFFB87916)
val BahayNgKuwentoBlue = Coral
val StoryTreeGreen = StoryPurple
