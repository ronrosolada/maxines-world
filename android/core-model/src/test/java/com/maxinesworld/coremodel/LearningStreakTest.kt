package com.maxinesworld.coremodel

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningStreakTest {
    private val today = LocalDate.of(2026, 8, 5)

    @Test
    fun `empty data has no current or trailing streak`() {
        assertEquals(0, currentLearningStreak(emptySet(), today))
        assertEquals(0, longestLearningStreak(emptySet()))
    }

    @Test
    fun `today only is a live one day streak`() {
        assertEquals(1, currentLearningStreak(setOf(today), today))
    }

    @Test
    fun `yesterday only is a live one day streak`() {
        assertEquals(1, currentLearningStreak(setOf(today.minusDays(1)), today))
    }

    @Test
    fun `today and yesterday form a two day streak`() {
        assertEquals(2, currentLearningStreak(setOf(today, today.minusDays(1)), today))
    }

    @Test
    fun `a gap breaks the trailing streak`() {
        val dates = setOf(today, today.minusDays(2), today.minusDays(3))

        assertEquals(1, currentLearningStreak(dates, today))
        assertEquals(1, longestLearningStreak(dates))
    }

    @Test
    fun `month boundary is consecutive`() {
        assertEquals(
            3,
            longestLearningStreak(
                setOf(
                    LocalDate.of(2026, 7, 31),
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 2),
                ),
            ),
        )
    }

    @Test
    fun `year boundary is consecutive`() {
        assertEquals(
            3,
            longestLearningStreak(
                setOf(
                    LocalDate.of(2025, 12, 30),
                    LocalDate.of(2025, 12, 31),
                    LocalDate.of(2026, 1, 1),
                ),
            ),
        )
    }

    @Test
    fun `duplicate timestamps collapse to one local learning date`() {
        val timestamp = Instant.parse("2026-08-05T10:00:00Z").toEpochMilli()

        assertEquals(
            setOf(today),
            localLearningDates(listOf(timestamp, timestamp), ZoneId.of("UTC")),
        )
    }

    @Test
    fun `timestamps are converted in the supplied timezone`() {
        val manila = ZoneId.of("Asia/Manila")
        val timestamp = Instant.parse("2026-08-02T17:30:00Z").toEpochMilli()

        assertEquals(
            setOf(LocalDate.of(2026, 8, 3)),
            localLearningDates(listOf(timestamp), manila),
        )
    }

    @Test
    fun `non-positive timestamps are ignored at the adapter boundary`() {
        val validTimestamp = Instant.parse("2026-08-05T10:00:00Z").toEpochMilli()

        assertEquals(
            setOf(today),
            localLearningDates(listOf(0L, -1L, validTimestamp), ZoneId.of("UTC")),
        )
    }

    @Test
    fun `future timestamps do not create a live streak`() {
        val futureTimestamp = today.plusDays(1)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val futureDate = localLearningDates(listOf(futureTimestamp), ZoneId.of("UTC"))

        assertEquals(setOf(today.plusDays(1)), futureDate)
        assertEquals(0, currentLearningStreak(futureDate, today))
        assertEquals(1, currentLearningStreak(futureDate + today, today))
    }
}
