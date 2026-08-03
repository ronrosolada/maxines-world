package com.maxinesworld.featureparent

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {

    @Test
    fun `consecutive days count as one streak`() {
        assertEquals(
            4,
            longestStreak(setOf("2026-08-01", "2026-08-02", "2026-08-03", "2026-08-04"))
        )
    }

    @Test
    fun `gap breaks the streak`() {
        // Missed Wednesday: only the trailing run counts.
        assertEquals(
            2,
            longestStreak(setOf("2026-08-01", "2026-08-02", "2026-08-04", "2026-08-05"))
        )
    }

    @Test
    fun `older run does not extend a broken streak`() {
        assertEquals(1, longestStreak(setOf("2026-07-30", "2026-07-31", "2026-08-02")))
    }

    @Test
    fun `unordered input and duplicates are handled`() {
        assertEquals(
            3,
            longestStreak(setOf("2026-08-03", "2026-08-01", "2026-08-03", "2026-08-02", "2026-08-01"))
        )
    }

    @Test
    fun `month boundary is consecutive`() {
        assertEquals(3, longestStreak(setOf("2026-07-31", "2026-08-01", "2026-08-02")))
    }

    @Test
    fun `year boundary is consecutive`() {
        assertEquals(3, longestStreak(setOf("2025-12-30", "2025-12-31", "2026-01-01")))
    }

    @Test
    fun `single day is a streak of one`() {
        assertEquals(1, longestStreak(setOf("2026-08-01")))
    }

    @Test
    fun `empty set has no streak`() {
        assertEquals(0, longestStreak(emptySet()))
    }

    @Test
    fun `malformed dates are ignored`() {
        assertEquals(0, longestStreak(setOf("not-a-date", "2026-13-99")))
        assertEquals(1, longestStreak(setOf("2026-08-01", "garbage")))
    }

    @Test
    fun `early morning completion keeps the local date`() {
        // 2026-08-03 01:30 Manila = 2026-08-02 17:30 UTC. The UTC bucketing
        // would drop this into the previous day; local bucketing must not.
        val manila = ZoneId.of("Asia/Manila")
        val ts = Instant.parse("2026-08-02T17:30:00Z").toEpochMilli()
        val dates = localDatesFromEpochMillis(listOf(ts), manila)
        assertEquals(setOf("2026-08-03"), dates)
    }

    @Test
    fun `multiple events on one day collapse to one date`() {
        val ts1 = Instant.parse("2026-08-02T00:10:00Z").toEpochMilli() // 08:10 Manila
        val ts2 = Instant.parse("2026-08-02T10:00:00Z").toEpochMilli() // 18:00 Manila
        val manila = ZoneId.of("Asia/Manila")
        assertEquals(
            setOf("2026-08-02"),
            localDatesFromEpochMillis(listOf(ts1, ts2), manila)
        )
    }
}
