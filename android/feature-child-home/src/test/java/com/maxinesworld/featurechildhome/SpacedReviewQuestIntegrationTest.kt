package com.maxinesworld.featurechildhome

import com.maxinesworld.coredatabase.MasteryRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SpacedReviewQuestIntegrationTest {
    @Test
    fun `due mastery records are injected before regular missions`() {
        val now = 1_000_000L
        val records = listOf(
            MasteryRecordEntity("child_math-g2-place-value", "child", "math-g2-place-value", "PROFICIENT", .9, 5, now - 10, now - 1),
            MasteryRecordEntity("child_eng-g2-reading", "child", "eng-g2-reading", "MASTERED", .95, 12, now - 10, now + 1),
        )

        assertEquals(
            listOf("review:child:math-g2-place-value", "video-1", "arena:pack-1"),
            injectDueSpacedReviews(records, listOf("video-1", "arena:pack-1"), now),
        )
    }
}
