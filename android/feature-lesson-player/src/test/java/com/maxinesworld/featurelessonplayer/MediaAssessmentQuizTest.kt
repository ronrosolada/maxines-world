package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.MediaAssessment
import com.maxinesworld.coremodel.MediaAssessmentItem
import com.maxinesworld.coremodel.MediaAssessmentOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MediaAssessmentQuizTest {
    @Test
    fun `submitting an answer scores once and advancing opens the next question`() {
        val assessment = assessment()
        val initial = beginMediaAssessmentQuiz(
            mediaId = "kids-tagalog-01-introductions",
            assessment = assessment,
        )

        val selected = selectQuizOption(initial, "b")
        val submitted = submitQuizAnswer(selected, assessment)
        val advanced = advanceQuiz(submitted, assessment)

        assertEquals(1, submitted.correctCount)
        assertTrue(submitted.submitted)
        assertEquals(submitted, submitQuizAnswer(submitted, assessment))
        assertEquals(1, advanced.questionIndex)
        assertFalse(advanced.submitted)
        assertEquals(1, advanced.correctCount)
        assertEquals(4, advanced.displayedOptions.size)
    }

    @Test
    fun `last submitted question produces a result without claiming mastery`() {
        val assessment = assessment()
        var state = beginMediaAssessmentQuiz(
            mediaId = "kids-tagalog-01-introductions",
            assessment = assessment,
        )

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
    fun `restarting a quiz clears selection score and result and reshuffles`() {
        val assessment = assessment()
        val finished = advanceQuiz(
            submitQuizAnswer(
                selectQuizOption(
                    presentQuizItem(
                        MediaAssessmentQuizState(
                            mediaId = "kids-tagalog-01-introductions",
                            questionIndex = assessment.items.lastIndex,
                        ),
                        assessment,
                    ),
                    "a",
                ),
                assessment,
            ),
            assessment,
        )

        val restarted = restartQuiz(finished, assessment)

        assertEquals(0, restarted.questionIndex)
        assertEquals(0, restarted.correctCount)
        assertEquals(null, restarted.selectedOptionId)
        assertFalse(restarted.submitted)
        assertFalse(restarted.finished)
        assertEquals(
            assessment.items.first().options.map { it.id }.toSet(),
            restarted.displayedOptions.map { it.id }.toSet(),
        )
    }

    @Test
    fun `presenting the same item twice can yield different option orders`() {
        val assessment = assessment()
        val orders = (0..40).map { seed ->
            beginMediaAssessmentQuiz(
                mediaId = "kids-tagalog-01-introductions",
                assessment = assessment,
                random = Random(seed.toLong()),
            ).displayedOptions.map { it.id }
        }.toSet()

        assertTrue("retries must be allowed to show a new slot order, got $orders", orders.size >= 2)
        orders.forEach { order ->
            assertEquals(listOf("a", "b", "c", "d").toSet(), order.toSet())
        }
    }

    @Test
    fun `selecting the keyed id scores correct regardless of display slot`() {
        val assessment = assessment()
        val authored = assessment.items.first().options
        authored.indices.forEach { slot ->
            val rotated = authored.drop(slot) + authored.take(slot)
            val state = selectQuizOption(
                MediaAssessmentQuizState(
                    mediaId = "kids-tagalog-01-introductions",
                    displayedOptions = rotated,
                ),
                "b",
            )
            val submitted = submitQuizAnswer(state, assessment)
            assertEquals("keyed id b must score in slot $slot", 1, submitted.correctCount)
            assertTrue(submitted.submitted)
        }
    }

    @Test
    fun `selecting a distractor id scores wrong regardless of display slot`() {
        val assessment = assessment()
        val authored = assessment.items.first().options
        val displayed = authored.reversed()
        val submitted = submitQuizAnswer(
            selectQuizOption(
                MediaAssessmentQuizState(
                    mediaId = "kids-tagalog-01-introductions",
                    displayedOptions = displayed,
                ),
                "a",
            ),
            assessment,
        )
        assertEquals(0, submitted.correctCount)
        assertTrue(submitted.submitted)
        assertNotEquals("b", displayed.first().id)
    }

    @Test
    fun `missing selected id fails closed`() {
        val assessment = assessment()
        val presented = beginMediaAssessmentQuiz(
            mediaId = "kids-tagalog-01-introductions",
            assessment = assessment,
        )
        val missing = submitQuizAnswer(
            selectQuizOption(presented, "z"),
            assessment,
        )
        assertTrue(missing.submitted)
        assertEquals(0, missing.correctCount)

        val unpresented = submitQuizAnswer(
            selectQuizOption(
                MediaAssessmentQuizState(mediaId = "kids-tagalog-01-introductions"),
                "b",
            ),
            assessment,
        )
        assertTrue(unpresented.submitted)
        assertEquals(0, unpresented.correctCount)
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
