package com.maxinesworld.engineactivity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEvaluationEngineTest {
    private val engine = AudioEvaluationEngine()

    @Test
    fun `close beginner phrase earns excellent encouragement`() {
        val result = engine.evaluateTranscript("Salamat po", "salamat po")

        assertEquals(AudioFeedbackState.EXCELLENT, result.state)
        assertEquals("Napakagaling!", result.message)
        assertTrue(result.mayContinue)
    }

    @Test
    fun `phonetic near match earns good effort without penalty`() {
        val result = engine.evaluateTranscript("Salamat po", "salamat")

        assertEquals(AudioFeedbackState.GOOD_EFFORT, result.state)
        assertEquals("Magandang subok! Subukan ulitin", result.message)
        assertTrue(result.mayContinue)
    }

    @Test
    fun `quiet short recording invites listening again without blocking`() {
        val result = engine.evaluateAudio(
            durationMs = 220,
            energyEnvelope = listOf(0.0, 0.01, 0.0),
        )

        assertEquals(AudioFeedbackState.TRY_AGAIN, result.state)
        assertEquals("Pakinggan nating muli", result.message)
        assertTrue(result.mayContinue)
        assertFalse(result.penalized)
    }

    @Test
    fun `clear recording energy earns excellent encouragement`() {
        val result = engine.evaluateAudio(
            durationMs = 1_500,
            energyEnvelope = listOf(0.12, 0.35, 0.26, 0.18),
        )

        assertEquals(AudioFeedbackState.EXCELLENT, result.state)
        assertTrue(result.mayContinue)
        assertFalse(result.penalized)
    }

    @Test
    fun `every feedback state remains supportive and non blocking`() {
        AudioFeedbackState.entries.forEach { state ->
            val result = engine.feedback(state)
            assertTrue(result.message.isNotBlank())
            assertTrue(result.mayContinue)
            assertFalse(result.penalized)
        }
    }
}
