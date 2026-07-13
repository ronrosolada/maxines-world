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

// ─── Subject Color Tokens (package-level) ───
val HeritageGold = Color(0xFFB87916)
val KindnessTeal = Color(0xFF26A69A)
val Molasses = Color(0xFF2B2100)     // onSecondary / dark text on gold

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
