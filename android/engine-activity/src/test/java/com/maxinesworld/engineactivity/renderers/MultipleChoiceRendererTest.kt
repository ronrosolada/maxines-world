package com.maxinesworld.engineactivity.renderers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultipleChoiceRendererTest {
    @Test
    fun `shuffled order preserves every option and moves the correct answer`() {
        val ids = listOf(
            "mathematics-g3-q1-w01-d01-a04",
            "mathematics-g3-q2-w03-d02-a04",
            "mathematics-g3-q3-w05-d04-a04",
            "mathematics-g3-q4-w09-d04-a04",
        )
        val positions = ids.map { id ->
            val order = optionOrderFor(id, optionCount = 4, correctIndex = 0)
            assertEquals(listOf(0, 1, 2, 3).toSet(), order.toSet())
            order.indexOf(0)
        }
        assertTrue("correct answer must not stay in one fixed position: $positions", positions.toSet().size > 1)
    }

    @Test
    fun `result preserves the number of hints used`() {
        val result = multipleChoiceResult(
            activityId = "lesson-a01",
            correct = true,
            attempts = 2,
            hintsUsed = 1,
            responseTimeMs = 500L,
        )

        assertEquals(1, result.hintsUsed)
        assertEquals(2, result.attempts)
    }

    @Test
    fun `correction appears after the second incorrect attempt`() {
        assertTrue(shouldShowCorrection(submitted = true, feedbackState = false, attempts = 2))
        assertTrue(shouldShowCorrection(submitted = true, feedbackState = false, attempts = 3))
        assertTrue(!shouldShowCorrection(submitted = true, feedbackState = false, attempts = 1))
        assertTrue(!shouldShowCorrection(submitted = true, feedbackState = true, attempts = 2))
        assertTrue(!shouldShowCorrection(submitted = false, feedbackState = false, attempts = 2))
    }
}
