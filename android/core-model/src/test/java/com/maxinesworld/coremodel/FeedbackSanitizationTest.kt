package com.maxinesworld.coremodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackSanitizationTest {
    @Test
    fun `correct feedback replaces curriculum jargon`() {
        assertEquals(
            "Your groups match what we learned.",
            childFacingCorrectFeedback("Your groups follow the skill rule."),
        )
    }

    @Test
    fun `correct feedback explains learning without curriculum jargon`() {
        assertEquals(
            "It uses what we learned about base words.",
            childFacingCorrectFeedback("It applies the lesson skill about base words."),
        )
    }

    @Test
    fun `correct feedback rewrites assessment explanation language`() {
        assertEquals(
            "The correct response does not match what we learned. Other choices show what we learned.",
            childFacingCorrectFeedback(
                "The correct response does not follow the lesson skill. Other choices show the skill."
            ),
        )
    }

    @Test
    fun `incorrect feedback replaces curriculum jargon and keeps a useful fallback`() {
        val sanitized = childFacingIncorrectFeedback("B and C do not show the skill.")
        assertTrue(sanitized.contains("show what we learned", ignoreCase = true))
        assertEquals(DEFAULT_INCORRECT_FEEDBACK, childFacingIncorrectFeedback("Try again!"))
    }
}
