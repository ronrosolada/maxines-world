package com.maxinesworld.coremodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MiloReviewQueueResolverTest {

    @Test
    fun `resolves needs review records as highest priority`() {
        val now = System.currentTimeMillis()
        val records = listOf(
            MasteryRecord(
                childId = "child_1",
                skillId = "math-place-value",
                state = MasteryState.NEEDS_REVIEW,
                accuracy = 0.4,
                lastActivityAt = now - TimeUnit.DAYS.toMillis(2),
                nextReviewAt = now - TimeUnit.DAYS.toMillis(1),
            ),
            MasteryRecord(
                childId = "child_1",
                skillId = "sci-living-things",
                state = MasteryState.PRACTICING,
                accuracy = 0.8,
                lastActivityAt = now - TimeUnit.DAYS.toMillis(4),
                nextReviewAt = now - TimeUnit.DAYS.toMillis(1),
            )
        )

        val due = MiloReviewQueueResolver.resolveDueItems(records, nowEpochMillis = now)
        assertEquals(2, due.size)
        assertEquals("math-place-value", due[0].skillId)
        assertEquals(ReviewReason.NEEDS_REMEDY, due[0].reason)
        assertEquals("sci-living-things", due[1].skillId)
        assertEquals(ReviewReason.SCHEDULED_SPACED, due[1].reason)
    }

    @Test
    fun `ignores mastered skills not yet due for review`() {
        val now = System.currentTimeMillis()
        val records = listOf(
            MasteryRecord(
                childId = "child_1",
                skillId = "eng-rhyming",
                state = MasteryState.MASTERED,
                accuracy = 0.95,
                lastActivityAt = now,
                nextReviewAt = now + TimeUnit.DAYS.toMillis(30),
            )
        )

        val due = MiloReviewQueueResolver.resolveDueItems(records, nowEpochMillis = now)
        assertTrue(due.isEmpty())
    }
}
