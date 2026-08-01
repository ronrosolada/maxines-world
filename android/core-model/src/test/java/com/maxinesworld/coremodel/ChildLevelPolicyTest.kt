package com.maxinesworld.coremodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildLevelPolicyTest {

    @Test
    fun `level is 1 at zero completions`() {
        assertEquals(1, ChildLevelPolicy.levelFor(0))
    }

    @Test
    fun `level steps every 4 lessons`() {
        assertEquals(1, ChildLevelPolicy.levelFor(3))
        assertEquals(2, ChildLevelPolicy.levelFor(4))
        assertEquals(3, ChildLevelPolicy.levelFor(8))
        assertEquals(4, ChildLevelPolicy.levelFor(12))
        assertEquals(5, ChildLevelPolicy.levelFor(16))
    }

    @Test
    fun `kindness unlocks exactly at 12 lessons`() {
        val unlockLessons = ChildLevelPolicy.KINDNESS_UNLOCK_LESSONS
        assertEquals(12, unlockLessons)
        assertEquals(3, ChildLevelPolicy.levelFor(unlockLessons - 1))
        assertEquals(4, ChildLevelPolicy.levelFor(unlockLessons))
    }

    @Test
    fun `lessons remaining to level 4`() {
        assertEquals(12, ChildLevelPolicy.lessonsRemainingTo(0, 4))
        assertEquals(4, ChildLevelPolicy.lessonsRemainingTo(8, 4))
        assertEquals(1, ChildLevelPolicy.lessonsRemainingTo(11, 4))
        assertEquals(0, ChildLevelPolicy.lessonsRemainingTo(12, 4))
        assertEquals(0, ChildLevelPolicy.lessonsRemainingTo(99, 4))
    }

    @Test
    fun `replay does not inflate level`() {
        // 3 distinct lessons, even if attempted 10 times
        assertEquals(1, ChildLevelPolicy.levelFor(3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative count is rejected`() {
        ChildLevelPolicy.levelFor(-1)
    }

    @Test
    fun `unlock threshold is kid-friendly`() {
        // ~3 lessons/day → Kindness opens after ~4 days of play
        assertTrue("unlock should be <= 15 lessons", ChildLevelPolicy.KINDNESS_UNLOCK_LESSONS <= 15)
    }
}
