package com.maxinesworld.engineassessment

import com.maxinesworld.engineactivity.ActivityResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScorerTest {

    private lateinit var scorer: Scorer

    @Before
    fun setup() {
        scorer = Scorer()
    }

    @Test
    fun `empty results returns zero accuracy`() {
        assertEquals(0.0, scorer.computeAccuracy(emptyList()), 0.001)
    }

    @Test
    fun `all correct returns 1 point 0`() {
        val results = listOf(
            ActivityResult("a1", true, 1, 0, 100),
            ActivityResult("a2", true, 1, 0, 200)
        )
        assertEquals(1.0, scorer.computeAccuracy(results), 0.001)
    }

    @Test
    fun `half correct returns 0 point 5`() {
        val results = listOf(
            ActivityResult("a1", true, 1, 0, 100),
            ActivityResult("a2", false, 2, 1, 300)
        )
        assertEquals(0.5, scorer.computeAccuracy(results), 0.001)
    }

    @Test
    fun `pass threshold respected`() {
        val results = listOf(
            ActivityResult("a1", true, 1, 0, 100),
            ActivityResult("a2", true, 1, 0, 200),
            ActivityResult("a3", true, 1, 0, 300),
            ActivityResult("a4", true, 1, 0, 400),
            ActivityResult("a5", false, 1, 0, 500) // 4/5 = 0.8
        )
        assertTrue(scorer.hasPassed(results, 0.8))
        assertFalse(scorer.hasPassed(results, 0.85))
    }
}
