package com.maxinesworld.coredesignsystem.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCardEnabledTest {
    @Test
    fun `disabled answer state removes click action`() {
        assertFalse(answerCardEnabled(AnswerCardState.DISABLED))
        assertTrue(answerCardEnabled(AnswerCardState.IDLE))
        assertTrue(answerCardEnabled(AnswerCardState.SELECTED))
        assertTrue(answerCardEnabled(AnswerCardState.CORRECT))
        assertTrue(answerCardEnabled(AnswerCardState.INCORRECT))
    }
}
