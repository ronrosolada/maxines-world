package com.maxinesworld.coremodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MiloReviewQueueResolverTest {
    private val now = TimeUnit.DAYS.toMillis(20_000)

    @Test
    fun `empty records produce an empty review queue`() {
        assertTrue(MiloReviewQueueResolver.resolveDueItems(emptyList(), now).isEmpty())
    }

    @Test
    fun `mastered record before its review timestamp is filtered out`() {
        val result = resolve(record(state = MasteryState.MASTERED, nextReviewAt = now + days(30)))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `practicing record with a future timestamp is filtered out`() {
        val result = resolve(record(state = MasteryState.PRACTICING, nextReviewAt = now + 1))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `record is due exactly at its review timestamp`() {
        val result = resolve(record(state = MasteryState.MASTERED, nextReviewAt = now))

        assertEquals(1, result.size)
        assertEquals(ReviewReason.SCHEDULED_SPACED, result.single().reason)
        assertEquals(50, result.single().priorityScore)
    }

    @Test
    fun `scheduled urgency gains one point per complete overdue day`() {
        val result = resolve(
            record(skillId = "eng-one-day", nextReviewAt = now - days(1)),
            record(skillId = "sci-six-days", nextReviewAt = now - days(6)),
        )

        assertEquals(listOf(56, 51), result.map { it.priorityScore })
        assertEquals(listOf("sci-six-days", "eng-one-day"), result.map { it.skillId })
    }

    @Test
    fun `partial overdue interval does not count as a complete day`() {
        val result = resolve(record(nextReviewAt = now - TimeUnit.HOURS.toMillis(23)))

        assertEquals(50, result.single().priorityScore)
    }

    @Test
    fun `scheduled urgency caps the overdue interval contribution at fifty days`() {
        val result = resolve(
            record(skillId = "math-fifty-days", nextReviewAt = now - days(50)),
            record(skillId = "math-hundred-days", nextReviewAt = now - days(100)),
        )

        assertEquals(listOf(100, 100), result.map { it.priorityScore })
    }

    @Test
    fun `needs review urgency is inverse to accuracy`() {
        val result = resolve(
            record(skillId = "math-high-accuracy", state = MasteryState.NEEDS_REVIEW, accuracy = 0.90),
            record(skillId = "math-low-accuracy", state = MasteryState.NEEDS_REVIEW, accuracy = 0.25),
        )

        assertEquals(listOf("math-low-accuracy", "math-high-accuracy"), result.map { it.skillId })
        assertEquals(listOf(175, 110), result.map { it.priorityScore })
    }

    @Test
    fun `accuracy uses truncated percentage in urgency score`() {
        val result = resolve(record(state = MasteryState.NEEDS_REVIEW, accuracy = 0.806))

        assertEquals(120, result.single().priorityScore)
    }

    @Test
    fun `needs review outranks even maximally overdue scheduled review`() {
        val result = resolve(
            record(skillId = "sci-overdue", state = MasteryState.MASTERED, nextReviewAt = now - days(500)),
            record(skillId = "eng-remedy", state = MasteryState.NEEDS_REVIEW, accuracy = 0.99, nextReviewAt = now + days(30)),
        )

        assertEquals(listOf("eng-remedy", "sci-overdue"), result.map { it.skillId })
        assertEquals(listOf(101, 100), result.map { it.priorityScore })
    }

    @Test
    fun `needs review remains due when its timestamp is in the future`() {
        val future = now + days(30)
        val result = resolve(record(state = MasteryState.NEEDS_REVIEW, nextReviewAt = future))

        assertEquals(1, result.size)
        assertEquals(future, result.single().dueAtEpochMillis)
        assertEquals(ReviewReason.NEEDS_REMEDY, result.single().reason)
    }

    @Test
    fun `zero timestamp is treated as a long overdue scheduled review`() {
        val result = resolve(record(state = MasteryState.MASTERED, nextReviewAt = 0))

        assertEquals(100, result.single().priorityScore)
        assertEquals(0, result.single().dueAtEpochMillis)
    }

    @Test
    fun `negative timestamp is treated as overdue without arithmetic failure`() {
        val result = resolve(record(state = MasteryState.PRACTICING, nextReviewAt = -days(1)))

        assertEquals(100, result.single().priorityScore)
        assertEquals(-days(1), result.single().dueAtEpochMillis)
    }

    @Test
    fun `equal urgency preserves source order as deterministic tie break`() {
        val result = resolve(
            record(skillId = "eng-first", state = MasteryState.NEEDS_REVIEW, accuracy = 0.5),
            record(skillId = "sci-second", state = MasteryState.NEEDS_REVIEW, accuracy = 0.5),
        )

        assertEquals(listOf("eng-first", "sci-second"), result.map { it.skillId })
    }

    @Test
    fun `lower accuracy breaks remedy priority before source order`() {
        val result = resolve(
            record(skillId = "eng-first", state = MasteryState.NEEDS_REVIEW, accuracy = 0.7),
            record(skillId = "sci-second", state = MasteryState.NEEDS_REVIEW, accuracy = 0.3),
        )

        assertEquals(listOf("sci-second", "eng-first"), result.map { it.skillId })
    }

    @Test
    fun `maximum item count is applied after priority ordering`() {
        val result = MiloReviewQueueResolver.resolveDueItems(
            records = listOf(
                record(skillId = "math-low", state = MasteryState.NEEDS_REVIEW, accuracy = 0.8),
                record(skillId = "eng-high", state = MasteryState.NEEDS_REVIEW, accuracy = 0.1),
                record(skillId = "sci-middle", state = MasteryState.NEEDS_REVIEW, accuracy = 0.5),
            ),
            nowEpochMillis = now,
            maxItems = 2,
        )

        assertEquals(listOf("eng-high", "sci-middle"), result.map { it.skillId })
    }

    @Test
    fun `zero maximum items produces an empty queue`() {
        val result = MiloReviewQueueResolver.resolveDueItems(
            records = listOf(record(state = MasteryState.NEEDS_REVIEW)),
            nowEpochMillis = now,
            maxItems = 0,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `resolved item includes stable identity subject title and due timestamp`() {
        val dueAt = now - days(2)
        val result = resolve(record(childId = "child_7", skillId = "fil-pang-uri", nextReviewAt = dueAt)).single()

        assertEquals("review:child_7:fil-pang-uri", result.id)
        assertEquals("filipino", result.subjectId)
        assertEquals("Review: Fil Pang Uri", result.title)
        assertEquals(dueAt, result.dueAtEpochMillis)
    }

    @Test
    fun `skill prefixes map to supported subjects and unknown prefix maps to general`() {
        val records = listOf("math" to "mathematics", "eng" to "english", "fil" to "filipino", "sci" to "science", "maka" to "makabansa", "gmrc" to "gmrc", "art" to "general")
            .map { (prefix, _) -> record(skillId = "$prefix-skill", nextReviewAt = now) }

        val bySkill = resolve(*records.toTypedArray()).associate { it.skillId to it.subjectId }

        records.zip(listOf("mathematics", "english", "filipino", "science", "makabansa", "gmrc", "general"))
            .forEach { (record, expected) -> assertEquals(expected, bySkill[record.skillId]) }
    }

    private fun resolve(vararg records: MasteryRecord): List<MiloReviewItem> =
        MiloReviewQueueResolver.resolveDueItems(records.toList(), nowEpochMillis = now, maxItems = Int.MAX_VALUE)

    private fun record(
        childId: String = "child_1",
        skillId: String = "math-place-value",
        state: MasteryState = MasteryState.PRACTICING,
        accuracy: Double = 0.8,
        nextReviewAt: Long = now - days(1),
    ) = MasteryRecord(
        childId = childId,
        skillId = skillId,
        state = state,
        accuracy = accuracy,
        nextReviewAt = nextReviewAt,
    )

    private fun days(value: Long): Long = TimeUnit.DAYS.toMillis(value)
}
