package com.maxinesworld.featurelessonplayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityAnimationTest {
    @Test
    fun `zero animator scale disables confetti animation`() {
        assertFalse(confettiAnimationEnabled(0f))
    }

    @Test
    fun `positive animator scale keeps confetti animation enabled`() {
        assertTrue(confettiAnimationEnabled(1f))
        assertTrue(confettiAnimationEnabled(0.5f))
    }

    @Test
    fun `tts fallback is localized for Filipino lessons`() {
        assertTrue(ttsUnavailableMessage("fil-PH").startsWith("Walang boses"))
        assertTrue(ttsUnavailableMessage("en-US").startsWith("Voice not available"))
    }
}
