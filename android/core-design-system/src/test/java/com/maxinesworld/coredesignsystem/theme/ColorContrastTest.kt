package com.maxinesworld.coredesignsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {
    @Test
    fun accessibleTextTokensMeetWcagAaAgainstTheirBrandFills() {
        assertAa("gold", OnGold, SunshineGold)
        assertAa("coral", OnCoral, Coral)
        assertAa("leaf green", OnLeafGreen, LeafGreen)
        assertAa("sky blue", OnSkyBlue, SkyBlue)
        assertAa("success", OnSuccess, SuccessGreen)
        assertAa("error", OnError, ErrorRed)
        assertAa("kindness teal text", KindnessTealText, Cream)
    }

    @Test
    fun feedbackTextTokensMeetWcagAaOnWhite() {
        assertAa("success feedback", SuccessGreenText, White)
        assertAa("review feedback", ReviewText, White)
    }

    private fun assertAa(name: String, foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$name contrast was %.2f:1; expected at least 4.5:1".format(ratio), ratio >= 4.5)
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (maxOf(firstLuminance, secondLuminance) + 0.05) /
            (minOf(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }

        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }
}
