package com.maxinesworld.coremodel.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FishTreatPolicyCalculateGrantsTest {

    /** Convenience: call calculateGrants with defaults that generate no grants. */
    private fun grants(
        childId: String = "child-1",
        lessonId: String = "lesson-1",
        attemptId: String = "att-1",
        localDate: String = "2026-07-14",
        isFirstCompletion: Boolean = false,
        currentScore: Double = 50.0,
        priorBestScore: Double? = null,
        masteryTransitions: List<MasteryTransition> = emptyList(),
        questId: String? = null,
        questQualifies: Boolean = false,
    ): List<Grant> = FishTreatPolicy.calculateGrants(
        childId = childId,
        lessonId = lessonId,
        attemptId = attemptId,
        localDate = localDate,
        isFirstCompletion = isFirstCompletion,
        currentScore = currentScore,
        priorBestScore = priorBestScore,
        masteryTransitions = masteryTransitions,
        questId = questId,
        questQualifies = questQualifies,
    )

    // ─── 1. First Completion = 5 ────────────────────────────────────

    @Test
    fun `first completion grants 5`() {
        val result = grants(isFirstCompletion = true)
        assertEquals(1, result.size)
        assertEquals("lesson-first:child-1:lesson-1", result[0].sourceKey)
        assertEquals(5, result[0].amount)
    }

    // ─── 2. 10pp Improvement = 2 ────────────────────────────────────

    @Test
    fun `10pp or more improvement grants 2`() {
        val result = grants(
            currentScore = 80.0,
            priorBestScore = 65.0, // 15pp improvement
        )
        assertEquals(1, result.size)
        assertEquals("lesson-improve:child-1:lesson-1:2026-07-14", result[0].sourceKey)
        assertEquals(2, result[0].amount)
    }

    @Test
    fun `exactly 10pp improvement grants 2`() {
        val result = grants(
            currentScore = 75.0,
            priorBestScore = 65.0, // exactly 10pp
        )
        assertEquals(1, result.size)
        assertEquals(2, result[0].amount)
    }

    // ─── 3. No Improvement = 0 ──────────────────────────────────────

    @Test
    fun `score decline yields no improvement grant`() {
        val result = grants(
            currentScore = 70.0,
            priorBestScore = 72.0, // declined
        )
        assertTrue(
            "Should not contain improvement grant",
            result.none { it.sourceKey.startsWith("lesson-improve") },
        )
    }

    @Test
    fun `improvement under 10pp yields no improvement grant`() {
        val result = grants(
            currentScore = 75.0,
            priorBestScore = 68.0, // 7pp, below threshold
        )
        assertTrue(
            "Should not contain improvement grant for <10pp",
            result.none { it.sourceKey.startsWith("lesson-improve") },
        )
    }

    @Test
    fun `null prior best yields no improvement grant`() {
        val result = grants(priorBestScore = null)
        assertTrue(
            "Null prior best should not generate improvement grant",
            result.none { it.sourceKey.startsWith("lesson-improve") },
        )
    }

    @Test
    fun `zero prior best does not trigger improvement when null check fails`() {
        // When priorBestScore is null, the `priorBestScore != null` guard
        // prevents the grant even though currentScore - 0.0 >= 10.0
        val result = grants(
            currentScore = 80.0,
            priorBestScore = null,
        )
        assertTrue(result.none { it.sourceKey.startsWith("lesson-improve") })
    }

    // ─── 4. Proficient Transition = 3 ───────────────────────────────

    @Test
    fun `proficient mastery transition grants 3`() {
        val result = grants(
            masteryTransitions = listOf(
                MasteryTransition(
                    skillId = "skill-phonics",
                    from = "DEVELOPING",
                    to = "PROFICIENT",
                ),
            ),
        )
        assertEquals(1, result.size)
        assertEquals("mastery-proficient:child-1:skill-phonics", result[0].sourceKey)
        assertEquals(3, result[0].amount)
    }

    @Test
    fun `proficient transition case insensitive`() {
        val result = grants(
            masteryTransitions = listOf(
                MasteryTransition(
                    skillId = "skill-math",
                    from = "DEVELOPING",
                    to = "proficient", // lowercase, should match via .uppercase()
                ),
            ),
        )
        assertEquals(1, result.size)
        assertEquals("mastery-proficient:child-1:skill-math", result[0].sourceKey)
        assertEquals(3, result[0].amount)
    }

    // ─── 5. Mastered Transition = 5 ─────────────────────────────────

    @Test
    fun `mastered transition grants 5`() {
        val result = grants(
            masteryTransitions = listOf(
                MasteryTransition(
                    skillId = "skill-phonics",
                    from = "PROFICIENT",
                    to = "MASTERED",
                ),
            ),
        )
        assertEquals(1, result.size)
        assertEquals("mastery-mastered:child-1:skill-phonics", result[0].sourceKey)
        assertEquals(5, result[0].amount)
    }

    // ─── 6. Quest Completion = 2 ────────────────────────────────────

    @Test
    fun `quest completion grants 2`() {
        val result = grants(
            questId = "quest-cat-rescue",
            questQualifies = true,
        )
        assertEquals(1, result.size)
        assertEquals("quest-complete:child-1:quest-cat-rescue", result[0].sourceKey)
        assertEquals(2, result[0].amount)
    }

    @Test
    fun `null questId yields no quest grant even when qualified`() {
        val result = grants(questId = null, questQualifies = true)
        assertTrue(result.none { it.sourceKey.startsWith("quest-complete") })
    }

    @Test
    fun `quest not qualified yields no quest grant`() {
        val result = grants(questId = "quest-1", questQualifies = false)
        assertTrue(result.none { it.sourceKey.startsWith("quest-complete") })
    }

    // ─── Combined Grants ────────────────────────────────────────────

    @Test
    fun `all five grant types can combine in a single call`() {
        val result = grants(
            isFirstCompletion = true,
            currentScore = 85.0,
            priorBestScore = 70.0, // 15pp improvement
            masteryTransitions = listOf(
                MasteryTransition("skill-a", "DEVELOPING", "PROFICIENT"),
                MasteryTransition("skill-b", "PROFICIENT", "MASTERED"),
            ),
            questId = "quest-cats",
            questQualifies = true,
        )
        assertEquals(5, result.size)

        val keys = result.map { it.sourceKey }.toSet()
        assertTrue(keys.contains("lesson-first:child-1:lesson-1"))
        assertTrue(keys.contains("lesson-improve:child-1:lesson-1:2026-07-14"))
        assertTrue(keys.contains("mastery-proficient:child-1:skill-a"))
        assertTrue(keys.contains("mastery-mastered:child-1:skill-b"))
        assertTrue(keys.contains("quest-complete:child-1:quest-cats"))

        // Verify total: 5 + 2 + 3 + 5 + 2 = 17
        val total = result.sumOf { it.amount }
        assertEquals(17, total)
    }

    @Test
    fun `empty grants when nothing qualifies`() {
        val result = grants()
        assertTrue("Should be empty", result.isEmpty())
    }

    // ─── Source Key Stability ───────────────────────────────────────

    @Test
    fun `source keys are deterministic`() {
        val a = grants(isFirstCompletion = true)
        val b = grants(isFirstCompletion = true)
        assertEquals(a.map { it.sourceKey }, b.map { it.sourceKey })
        assertEquals(a.map { it.amount }, b.map { it.amount })
    }

    @Test
    fun `source keys differ by child`() {
        val r1 = grants(childId = "alice", isFirstCompletion = true)
        val r2 = grants(childId = "bob", isFirstCompletion = true)
        assertEquals("lesson-first:alice:lesson-1", r1[0].sourceKey)
        assertEquals("lesson-first:bob:lesson-1", r2[0].sourceKey)
    }

    @Test
    fun `improvement key includes date for daily dedup`() {
        val r = grants(
            currentScore = 80.0,
            priorBestScore = 65.0,
            localDate = "2026-07-14",
        )
        assertEquals("lesson-improve:child-1:lesson-1:2026-07-14", r[0].sourceKey)
    }

    @Test
    fun `unknown mastery transition level is ignored`() {
        val result = grants(
            masteryTransitions = listOf(
                MasteryTransition("skill-x", "DEVELOPING", "BEGINNER"), // not in when branches
            ),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `multiple mastery transitions of same type both included`() {
        val result = grants(
            masteryTransitions = listOf(
                MasteryTransition("skill-a", "DEVELOPING", "PROFICIENT"),
                MasteryTransition("skill-b", "DEVELOPING", "PROFICIENT"),
            ),
        )
        assertEquals(2, result.size)
        assertEquals("mastery-proficient:child-1:skill-a", result[0].sourceKey)
        assertEquals("mastery-proficient:child-1:skill-b", result[1].sourceKey)
    }
}
