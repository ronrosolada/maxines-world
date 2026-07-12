package com.maxinesworld.engineminigame

import org.junit.Assert.*
import org.junit.Test

class RewardBreakClockTest {
    @Test fun `clock pauses while host is hidden`() {
        var now = 0L
        val clock = RewardBreakClock(10_000L) { now }
        clock.resume(); now = 3_000L; clock.pause(); now = 8_000L
        assertEquals(7_000L, clock.remainingMillis())
        clock.resume(); now = 10_000L
        assertEquals(5_000L, clock.remainingMillis())
    }

    @Test fun `clock expires once and clamps at zero`() {
        var now = 0L
        val clock = RewardBreakClock(1_000L) { now }
        clock.resume(); now = 2_500L
        assertTrue(clock.isExpired())
        assertEquals(0L, clock.remainingMillis())
    }
}
