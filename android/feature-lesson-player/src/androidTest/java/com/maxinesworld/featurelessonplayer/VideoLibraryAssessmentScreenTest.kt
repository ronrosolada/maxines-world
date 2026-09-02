package com.maxinesworld.featurelessonplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import com.maxinesworld.coremodel.MediaAssessment
import com.maxinesworld.coremodel.MediaAssessmentItem
import com.maxinesworld.coremodel.MediaAssessmentOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoLibraryAssessmentScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun failSummaryWatchAgainCallsPlayAndDoesNotRestart() {
        var watchAgainCalls = 0
        var restartCalls = 0

        composeRule.setContent {
            quizUnderTest(
                quiz = finishedQuiz(correctCount = 1),
                onWatchAgain = { watchAgainCalls++ },
                onRestart = { restartCalls++ },
            )
        }

        composeRule.onNodeWithText("Nice try! Let's watch again.").assertIsDisplayed()
        composeRule.onNodeWithText("Watch again").assertIsDisplayed()
        composeRule.onNodeWithText("Try quiz again").assertIsDisplayed()
        composeRule.onNodeWithText("Try Again").assertDoesNotExist()
        composeRule.onNodeWithTag(VideoLibraryTestTags.WatchAgainButton).assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(1, watchAgainCalls)
            assertEquals(0, restartCalls)
        }
    }

    @Test
    fun failSummarySecondaryTryQuizAgainRestartsWithoutPlaying() {
        var watchAgainCalls = 0
        var restartCalls = 0

        composeRule.setContent {
            quizUnderTest(
                quiz = finishedQuiz(correctCount = 1),
                onWatchAgain = { watchAgainCalls++ },
                onRestart = { restartCalls++ },
            )
        }

        composeRule.onNodeWithTag(VideoLibraryTestTags.TryQuizAgainButton).assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(0, watchAgainCalls)
            assertEquals(1, restartCalls)
        }
    }

    @Test
    fun passedSummaryHasNoWatchAgainLie() {
        var watchAgainCalls = 0
        var restartCalls = 0

        composeRule.setContent {
            quizUnderTest(
                quiz = finishedQuiz(correctCount = 2),
                onWatchAgain = { watchAgainCalls++ },
                onRestart = { restartCalls++ },
            )
        }

        composeRule.onNodeWithText("Great job, Maxine!").assertIsDisplayed()
        composeRule.onNodeWithText("5 stars earned. Lesson completed!").assertIsDisplayed()
        composeRule.onNodeWithText("Watch again").assertDoesNotExist()
        composeRule.onNodeWithText("Rewatch").assertDoesNotExist()
        composeRule.onNodeWithText("Nice try! Let's watch again.").assertDoesNotExist()
        composeRule.onNodeWithTag(VideoLibraryTestTags.WatchAgainButton).assertDoesNotExist()
        composeRule.onNodeWithText("Try Again").assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(0, watchAgainCalls)
            assertEquals(0, restartCalls)
        }
    }

    @Test
    fun replayFailSummaryUsesRewatchLabelAndStillPlays() {
        var watchAgainCalls = 0
        var restartCalls = 0

        composeRule.setContent {
            quizUnderTest(
                quiz = finishedQuiz(correctCount = 0, isReplay = true),
                lessonPassed = true,
                onWatchAgain = { watchAgainCalls++ },
                onRestart = { restartCalls++ },
            )
        }

        composeRule.onNodeWithText("Rewatch").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Watch again").assertDoesNotExist()

        composeRule.runOnIdle {
            assertEquals(1, watchAgainCalls)
            assertEquals(0, restartCalls)
        }
    }
}

@Composable
private fun quizUnderTest(
    quiz: MediaAssessmentQuizState,
    lessonPassed: Boolean = false,
    onWatchAgain: () -> Unit = {},
    onRestart: () -> Unit = {},
) {
    MaxinesWorldTheme {
        MediaAssessmentQuizCard(
            quiz = quiz,
            assessment = assessment(),
            mediaTitle = "Place value",
            lessonPassed = lessonPassed,
            onSelectOption = {},
            onCheckAnswer = {},
            onNextQuestion = {},
            onRestart = onRestart,
            onWatchAgain = onWatchAgain,
            onClose = {},
        )
    }
}

private fun finishedQuiz(correctCount: Int, isReplay: Boolean = false) = MediaAssessmentQuizState(
    mediaId = "math-g3-01",
    questionIndex = 1,
    selectedOptionId = "b",
    submitted = true,
    correctCount = correctCount,
    finished = true,
    isReplay = isReplay,
)

private fun assessment() = MediaAssessment(
    questionCount = 2,
    passingCorrectCount = 2,
    items = listOf(
        MediaAssessmentItem(
            itemId = "q1",
            sequence = 1,
            prompt = "Question 1",
            options = listOf(
                MediaAssessmentOption("a", "Answer A"),
                MediaAssessmentOption("b", "Answer B"),
            ),
            correctOptionIds = listOf("a"),
            explanation = "Because this is the video clue.",
        ),
        MediaAssessmentItem(
            itemId = "q2",
            sequence = 2,
            prompt = "Question 2",
            options = listOf(
                MediaAssessmentOption("a", "Answer A"),
                MediaAssessmentOption("b", "Answer B"),
            ),
            correctOptionIds = listOf("a"),
            explanation = "Because this is the video clue.",
        ),
    ),
)
