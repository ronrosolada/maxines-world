package com.maxinesworld.coredesignsystem.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeAnimationTest {
    @Test
    fun `zero animator scale disables animations`() {
        assertFalse(animationsEnabledForScale(0f))
    }

    @Test
    fun `positive animator scale enables animations`() {
        assertTrue(animationsEnabledForScale(1f))
        assertTrue(animationsEnabledForScale(0.5f))
    }
}
