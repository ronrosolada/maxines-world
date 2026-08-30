package com.maxinesworld.engineactivity.renderers

import com.maxinesworld.coremodel.ActivityStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationReplayTest {
    @Test
    fun `explanation replays narration text and falls back to question`() {
        val narrated = ActivityStep(id = "a", type = "ANIMATED_EXPLANATION_V1", question = "Question", narrationText = "Listen closely")
        val fallback = ActivityStep(id = "b", type = "ANIMATED_EXPLANATION_V1", question = "Question")

        assertEquals("Listen closely", narrationPhrase(narrated))
        assertEquals("Question", narrationPhrase(fallback))
    }

    @Test
    fun `multiple choice exposes replay only when narration is authored`() {
        val narrated = ActivityStep(id = "a", type = "MULTIPLE_CHOICE_V1", question = "Question", narrationText = "Hear the clue")
        val silent = ActivityStep(id = "b", type = "MULTIPLE_CHOICE_V1", question = "Question")

        assertTrue(hasReplayableNarration(narrated))
        assertFalse(hasReplayableNarration(silent))
    }
}
