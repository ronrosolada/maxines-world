package com.maxinesworld.featurerewards

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeRevealAnimationTest {
    @Test
    fun `zero animator scale disables reveal animation`() {
        assertFalse(badgeRevealAnimationEnabled(0f))
    }

    @Test
    fun `positive animator scale keeps reveal animation enabled`() {
        assertTrue(badgeRevealAnimationEnabled(1f))
        assertTrue(badgeRevealAnimationEnabled(0.5f))
    }
}
