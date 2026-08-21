package com.maxinesworld.featureparent

import com.maxinesworld.coredatabase.LessonCompletionEntity
import com.maxinesworld.coremodel.currentLearningStreak
import com.maxinesworld.coremodel.localLearningDates
import com.maxinesworld.coremodel.longestLearningStreak
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    private val today = LocalDate.of(2026, 8, 5)

    @Test
    fun `consecutive days count as one trailing streak`() {
        assertEquals(
            4,
            longestLearningStreak(
                setOf(
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 2),
                    LocalDate.of(2026, 8, 3),
                    LocalDate.of(2026, 8, 4),
                ),
            ),
        )
    }

    @Test
    fun `gap breaks the trailing streak`() {
        assertEquals(
            2,
            longestLearningStreak(
                setOf(
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 2),
                    LocalDate.of(2026, 8, 4),
                    LocalDate.of(2026, 8, 5),
                ),
            ),
        )
    }

    @Test
    fun `older run does not extend a broken trailing streak`() {
        assertEquals(
            1,
            longestLearningStreak(
                setOf(
                    LocalDate.of(2026, 7, 30),
                    LocalDate.of(2026, 7, 31),
                    LocalDate.of(2026, 8, 2),
                ),
            ),
        )
    }

    @Test
    fun `current streak is stale when neither today nor yesterday has learning`() {
        assertEquals(
            0,
            currentLearningStreak(
                setOf(today.minusDays(3), today.minusDays(2)),
                today,
            ),
        )
    }

    @Test
    fun `recent activity uses one row per completed lesson`() {
        val completions = listOf(
            LessonCompletionEntity("old", "child", "lesson-old", "attempt-old", 0.8, completedAtEpochMillis = 100L),
            LessonCompletionEntity("new", "child", "lesson-new", "attempt-new", 1.0, completedAtEpochMillis = 200L),
        )

        assertEquals(
            listOf("New lesson — 100%", "Old lesson — 80%"),
            recentActivityLabels(completions) { lessonId ->
                if (lessonId == "lesson-new") "New lesson" else "Old lesson"
            },
        )
    }

    @Test
    fun `recent activity keeps only the latest attempt for each lesson`() {
        val completions = listOf(
            LessonCompletionEntity("lesson-a:old", "child", "lesson-a", "old", 0.6, completedAtEpochMillis = 100L),
            LessonCompletionEntity("lesson-a:new", "child", "lesson-a", "new", 0.9, completedAtEpochMillis = 300L),
            LessonCompletionEntity("lesson-b:new", "child", "lesson-b", "new", 1.0, completedAtEpochMillis = 200L),
        )

        assertEquals(
            listOf("Lesson A — 90%", "Lesson B — 100%"),
            recentActivityLabels(completions) { lessonId ->
                if (lessonId == "lesson-a") "Lesson A" else "Lesson B"
            },
        )
    }

    @Test
    fun `early morning completion keeps the local date`() {
        val manila = ZoneId.of("Asia/Manila")
        val timestamp = Instant.parse("2026-08-02T17:30:00Z").toEpochMilli()

        assertEquals(
            setOf(LocalDate.of(2026, 8, 3)),
            localLearningDates(listOf(timestamp), manila),
        )
    }

    @Test
    fun `multiple events on one day collapse to one date`() {
        val timestamps = listOf(
            Instant.parse("2026-08-02T00:10:00Z").toEpochMilli(),
            Instant.parse("2026-08-02T10:00:00Z").toEpochMilli(),
        )

        assertEquals(
            setOf(LocalDate.of(2026, 8, 2)),
            localLearningDates(timestamps, ZoneId.of("Asia/Manila")),
        )
    }
}
