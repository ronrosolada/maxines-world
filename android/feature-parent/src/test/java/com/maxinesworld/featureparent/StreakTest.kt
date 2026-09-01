package com.maxinesworld.featureparent

import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
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
    fun `recent activity uses one row per passed video newest first`() {
        val watched = listOf(
            VideoWatchLedgerEntity(
                id = "child_video-old", childId = "child", mediaId = "video-old",
                subjectId = "english", quizPassed = true, bestQuizScore = 0.8f,
                firstPassedAtEpochMillis = 100L,
            ),
            VideoWatchLedgerEntity(
                id = "child_video-new", childId = "child", mediaId = "video-new",
                subjectId = "english", quizPassed = true, bestQuizScore = 1.0f,
                firstPassedAtEpochMillis = 200L,
            ),
        )

        assertEquals(
            listOf("New video — 100%", "Old video — 80%"),
            recentActivityLabels(watched) { mediaId ->
                if (mediaId == "video-new") "New video" else "Old video"
            },
        )
    }

    @Test
    fun `recent activity excludes videos whose memory check was not passed`() {
        val watched = listOf(
            VideoWatchLedgerEntity(
                id = "child_video-a", childId = "child", mediaId = "video-a",
                subjectId = "mathematics", quizPassed = true, bestQuizScore = 0.9f,
                firstPassedAtEpochMillis = 300L,
            ),
            VideoWatchLedgerEntity(
                id = "child_video-b", childId = "child", mediaId = "video-b",
                subjectId = "mathematics", quizPassed = false, bestQuizScore = 0.4f,
                firstPassedAtEpochMillis = null, lastWatchedAtEpochMillis = 200L,
            ),
        )

        assertEquals(
            listOf("Video A — 90%"),
            recentActivityLabels(watched) { mediaId ->
                if (mediaId == "video-a") "Video A" else "Video B"
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
