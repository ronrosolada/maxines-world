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
}
