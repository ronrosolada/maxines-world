package com.maxinesworld.engineactivity.renderers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic guards for the WRITING_PRODUCTION_V1 conventions
 * (CH-07 M2). No Compose instantiation — mirrors RendererContractTest.
 */
class WritingProductionRendererTest {

    @Test
    fun `tile display shuffle is stable for a given id`() {
        val id = "wp-1"
        val a = writingTileDisplayOrder(id, itemCount = 4)
        val b = writingTileDisplayOrder(id, itemCount = 4)
        assertEquals(a, b)
        assertEquals((0 until 4).toSet(), a.toSet())
    }

    @Test
    fun `sentence is correct only when every tile is in original order`() {
        assertTrue(writingSentenceIsCorrect(listOf(0, 1, 2, 3), itemCount = 4))
        assertFalse(writingSentenceIsCorrect(listOf(0, 2, 1, 3), itemCount = 4))
        assertFalse(writingSentenceIsCorrect(listOf(0, 1, 2), itemCount = 4))
    }

    @Test
    fun `submit is enabled only with a complete unsubmitted sentence`() {
        assertFalse(writingSubmitEnabled(placedCount = 3, itemCount = 4, submitted = false))
        assertTrue(writingSubmitEnabled(placedCount = 4, itemCount = 4, submitted = false))
        assertFalse(writingSubmitEnabled(placedCount = 4, itemCount = 4, submitted = true))
    }

    @Test
    fun `incorrect submission offers retry before checklist escalation`() {
        assertTrue(writingActionEnabled(3, 3, submitted = true, isCorrect = false, attempts = 1))
        assertTrue(writingActionEnabled(3, 3, submitted = true, isCorrect = false, attempts = 2))
        assertFalse(writingActionEnabled(3, 3, submitted = true, isCorrect = false, attempts = 3))
        assertFalse(writingActionEnabled(3, 3, submitted = true, isCorrect = true, attempts = 1))
    }

    @Test
    fun `writing chrome follows the lesson language`() {
        assertEquals("Words:", writingUiText("en-PH", "Words:", "Mga salita:"))
        assertEquals("Mga salita:", writingUiText("fil-PH", "Words:", "Mga salita:"))
        assertEquals("Subukan muli", writingActionLabel(4, 4, true, false, 1, "fil-PH"))
    }

    @Test
    fun `action label walks the child through build, check, and retry`() {
        assertEquals(
            "Place all words (1/4)",
            writingActionLabel(1, 4, submitted = false, isCorrect = false, attempts = 0),
        )
        assertEquals(
            "Check My Sentence",
            writingActionLabel(4, 4, submitted = false, isCorrect = false, attempts = 0),
        )
        assertEquals(
            "Great job!",
            writingActionLabel(4, 4, submitted = true, isCorrect = true, attempts = 1),
        )
        assertEquals(
            "Try Again",
            writingActionLabel(4, 4, submitted = true, isCorrect = false, attempts = 1),
        )
        assertEquals(
            "See the sentence →",
            writingActionLabel(4, 4, submitted = true, isCorrect = false, attempts = 3),
        )
    }

    @Test
    fun `checklist completes only when every item is self-marked`() {
        assertTrue(writingChecklistComplete(listOf(true, true, true)))
        assertFalse(writingChecklistComplete(listOf(true, false, true)))
        assertFalse(writingChecklistComplete(emptyList()))
    }

    @Test
    fun `production task contract has sentence tiles and checklist before rendering`() {
        assertTrue(writingProductionContentIsRenderable(5, 3))
        assertFalse(writingProductionContentIsRenderable(0, 3))
        assertFalse(writingProductionContentIsRenderable(5, 0))
    }
}
