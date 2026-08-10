package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.MediaAssessment
import com.maxinesworld.coremodel.MediaAssessmentItem
import com.maxinesworld.coremodel.MediaAssessmentOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAssessmentQuizTest {
    @Test
    fun `submitting an answer scores once and advancing opens the next question`() {
        val assessment = assessment()
        val initial = MediaAssessmentQuizState(mediaId = "kids-tagalog-01-introductions")

        val selected = selectQuizOption(initial, "b")
        val submitted = submitQuizAnswer(selected, assessment)
        val advanced = advanceQuiz(submitted, assessment)

        assertEquals(1, submitted.correctCount)
        assertTrue(submitted.submitted)
        assertEquals(submitted, submitQuizAnswer(submitted, assessment))
        assertEquals(1, advanced.questionIndex)
        assertFalse(advanced.submitted)
        assertEquals(1, advanced.correctCount)
    }

    @Test
    fun `last submitted question produces a result without claiming mastery`() {
        val assessment = assessment()
        var state = MediaAssessmentQuizState(mediaId = "kids-tagalog-01-introductions")

        repeat(assessment.items.size) { index ->
            state = selectQuizOption(state, if (index == 0) "b" else "b")
            state = submitQuizAnswer(state, assessment)
            state = advanceQuiz(state, assessment)
        }

        assertTrue(state.finished)
        assertEquals(1, state.correctCount)
        assertFalse(assessment.claimsMastery)
    }

    @Test
    fun `restarting a quiz clears selection score and result`() {
        val assessment = assessment()
        val finished = advanceQuiz(
            submitQuizAnswer(
                selectQuizOption(
                    MediaAssessmentQuizState(
                        mediaId = "kids-tagalog-01-introductions",
                        questionIndex = assessment.items.lastIndex,
                    ),
                    "a",
                ),
                assessment,
            ),
            assessment,
        )

        val restarted = restartQuiz(finished)

        assertEquals(0, restarted.questionIndex)
        assertEquals(0, restarted.correctCount)
        assertEquals(null, restarted.selectedOptionId)
        assertFalse(restarted.submitted)
        assertFalse(restarted.finished)
    }

    private fun assessment() = MediaAssessment(
        questionCount = 2,
        passingCorrectCount = 2,
        claimsMastery = false,
        items = listOf(
            item(1, correct = "b"),
            item(2, correct = "a"),
        ),
    )

    private fun item(sequence: Int, correct: String) = MediaAssessmentItem(
        itemId = "kids-tagalog-01-introductions-q${sequence.toString().padStart(2, '0')}",
        sequence = sequence,
        prompt = "Question $sequence",
        options = listOf(
            MediaAssessmentOption("a", "Answer A"),
            MediaAssessmentOption("b", "Answer B"),
            MediaAssessmentOption("c", "Answer C"),
            MediaAssessmentOption("d", "Answer D"),
        ),
        correctOptionIds = listOf(correct),
        explanation = "Because this is the video clue.",
    )
}
