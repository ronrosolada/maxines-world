package com.maxinesworld.coredesignsystem.theme

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {
    @Test
    fun `typography uses supplied font families`() {
        val marker = FontFamily.Monospace

        val typography = maxinesTypography(
            displayFont = marker,
            bodyFont = marker,
        )

        assertEquals(marker, typography.headlineMedium.fontFamily)
        assertEquals(marker, typography.bodyLarge.fontFamily)
        assertEquals(marker, typography.labelSmall.fontFamily)
    }
}
