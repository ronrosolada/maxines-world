package com.maxinesworld.featurechildhome

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCardTest {
    @Test
    fun celebrationIsDisabledWhenReducedMotionIsEnabled() {
        assertFalse(shouldCelebrateStreak(streakDays = 7, animationsDisabled = true))
    }

    @Test
    fun celebrationOnlyAppliesToPositiveStreaks() {
        assertFalse(shouldCelebrateStreak(streakDays = 0, animationsDisabled = false))
        assertTrue(shouldCelebrateStreak(streakDays = 7, animationsDisabled = false))
    }
}