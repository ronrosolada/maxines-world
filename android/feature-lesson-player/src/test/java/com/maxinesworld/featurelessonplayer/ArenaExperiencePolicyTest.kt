package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.AssessmentQuestionItem
import com.maxinesworld.coremodel.AssessmentQuestionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaExperiencePolicyTest {
    @Test fun `Filipino family packs use localized arena copy and TTS`() {
        listOf("filipino-g3", "makabansa-q1", "gmrc-kindness", "pack-fil-PH").forEach { id ->
            val copy = arenaCopy(id)
            assertTrue(copy.isFilipino)
            assertEquals("Tama! Napakagaling!", copy.correctHeader)
            assertEquals("Pahiwatig ni Milo:", copy.clueHeader)
            assertEquals("Tama! ", copy.correctTtsPrefix)
            assertEquals("Pahiwatig ni Milo: ", copy.clueTtsPrefix)
            assertEquals("Tingnan ang Sagot", copy.checkAnswer)
            assertEquals("Susunod na tanong", copy.nextQuestion)
            assertEquals("Tapusin ang pagsusulit", copy.finishQuiz)
            assertEquals("Muling subukan", copy.retry)
            assertEquals("Balikan ang mga pahiwatig", copy.reviewClues)
            assertEquals("Bumalik", copy.reviewBack)
            assertEquals("fil-PH", copy.ttsLanguage)
        }
    }

    @Test fun `English subject packs retain English arena copy`() {
        listOf("mathematics-g3", "science-g3", "english-g3").forEach { id ->
            val copy = arenaCopy(id)
            assertFalse(copy.isFilipino)
            assertEquals("Correct! Awesome job!", copy.correctHeader)
            assertEquals("Milo's learning clue:", copy.clueHeader)
            assertEquals("Check Answer", copy.checkAnswer)
            assertEquals("Review clues", copy.reviewClues)
            assertEquals("Back", copy.reviewBack)
            assertEquals("en-US", copy.ttsLanguage)
        }
    }

    @Test fun `failed quiz gets growth feedback and review action without rewards`() {
        val state = arenaCompletionState(isPassed = false, isFilipino = false)
        assertEquals("Great effort! Every try makes you stronger!", state.message)
        assertTrue(state.showRetry)
        assertTrue(state.showReviewClues)
        assertFalse(state.showMasteryRewards)
        assertEquals(ArenaSoundEffect.ENCOURAGEMENT, state.soundEffect)
    }

    @Test fun `Filipino failed quiz gets localized growth feedback`() {
        assertEquals(
            "Magaling na pagsisikap! Sa bawat subok, lalo kang gumagaling!",
            arenaCompletionState(false, true).message,
        )
    }

    @Test fun `passed quiz gets mastery rewards and celebration sound`() {
        val state = arenaCompletionState(isPassed = true, isFilipino = false)
        assertTrue(state.showMasteryRewards)
        assertFalse(state.showReviewClues)
        assertEquals(ArenaSoundEffect.CELEBRATION, state.soundEffect)
    }

    @Test fun `answer transition maps to one sound effect`() {
        assertEquals(ArenaSoundEffect.CORRECT, arenaAnswerSound(isSubmitted = true, isCorrect = true))
        assertEquals(ArenaSoundEffect.ENCOURAGEMENT, arenaAnswerSound(isSubmitted = true, isCorrect = false))
        assertEquals(null, arenaAnswerSound(isSubmitted = false, isCorrect = false))
    }

    @Test fun `clue review lists prompts and explanations without option letters`() {
        val items = listOf(
            AssessmentQuestionItem(
                sequence = 1,
                prompt = "What is 8 + 5?",
                options = listOf(
                    AssessmentQuestionOption("a", "13"),
                    AssessmentQuestionOption("b", "12"),
                ),
                correctOptionIds = listOf("a"),
                explanation = "Eight and five make thirteen.",
            ),
            AssessmentQuestionItem(
                sequence = 2,
                prompt = "Which animal is a mammal?",
                options = listOf(
                    AssessmentQuestionOption("c", "Dog"),
                    AssessmentQuestionOption("d", "Frog"),
                ),
                correctOptionIds = listOf("c"),
                explanation = "Dogs are mammals.",
            ),
        )
        val review = arenaClueReviewItems(items)
        assertEquals(2, review.size)
        assertEquals("What is 8 + 5?", review[0].prompt)
        assertEquals("Eight and five make thirteen.", review[0].explanation)
        assertEquals("Which animal is a mammal?", review[1].prompt)
        assertEquals("Dogs are mammals.", review[1].explanation)
        review.forEach { item ->
            assertFalse(item.prompt.contains("Option A"))
            assertFalse(item.explanation.contains("A)"))
        }
    }

    @Test fun `reviewing clues is its own surface and not a new quiz`() {
        assertEquals(ArenaActiveSurface.ClueReview, arenaActiveSurface(isFinished = true, isReviewingClues = true))
        assertEquals(ArenaActiveSurface.FinishedSummary, arenaActiveSurface(isFinished = true, isReviewingClues = false))
        assertEquals(ArenaActiveSurface.Quiz, arenaActiveSurface(isFinished = false, isReviewingClues = false))
    }

    @Test fun `Sanctuary copy localizes descriptions`() {
        assertEquals("Mga token ng santuwaryo", sanctuaryTokensDescription("filipino"))
        assertEquals(
            "Nagkaroon ng bagong gamit ang santuwaryo ni Milo: Puno",
            sanctuaryGainedDescription("fil-PH", "Puno"),
        )
        assertEquals("Sanctuary tokens", sanctuaryTokensDescription("english"))
        assertEquals("Milo's sanctuary gained: Tree", sanctuaryGainedDescription("english", "Tree"))
    }
}
