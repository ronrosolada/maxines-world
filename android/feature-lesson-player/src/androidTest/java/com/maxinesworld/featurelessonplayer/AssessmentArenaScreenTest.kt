package com.maxinesworld.featurelessonplayer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import com.maxinesworld.coremodel.AssessmentQuestionItem
import com.maxinesworld.coremodel.AssessmentQuestionOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AssessmentArenaScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reviewCluesDoesNotCallRestartQuizOrStartQuiz() {
        var restartCalls = 0
        var startCalls = 0
        var reviewCalls = 0

        composeRule.setContent {
            arenaUnderTest(
                state = failedFinishedState(),
                onStartQuiz = { startCalls++ },
                onRestartQuiz = { restartCalls++ },
                onReviewClues = { reviewCalls++ },
            )
        }

        composeRule.onNodeWithTag(ArenaTestTags.ReviewCluesButton).assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(1, reviewCalls)
            assertEquals(0, restartCalls)
            assertEquals(0, startCalls)
        }
    }

    @Test
    fun reviewSurfaceListsExplanationsAndIsNotAQuiz() {
        composeRule.setContent {
            arenaUnderTest(state = failedFinishedState(isReviewingClues = true))
        }

        composeRule.onNodeWithTag(ArenaTestTags.ClueReview).assertIsDisplayed()
        composeRule.onNodeWithText("How many tens in 40?").assertIsDisplayed()
        composeRule.onNodeWithText("Forty is four tens.").assertIsDisplayed()
        composeRule.onNodeWithText("Which shape has three sides?").assertIsDisplayed()
        composeRule.onNodeWithText("A triangle has three sides.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Milo's learning clue:").assertCountEquals(2)
        composeRule.onNodeWithText("Check Answer").assertDoesNotExist()
        composeRule.onNodeWithText("Option A").assertDoesNotExist()
        composeRule.onNodeWithText("A").assertDoesNotExist()
    }

    @Test
    fun filipinoReviewCluesKeepsLocalizedLabelAndDoesNotRestart() {
        var restartCalls = 0
        var reviewCalls = 0

        composeRule.setContent {
            arenaUnderTest(
                state = failedFinishedState(packId = "filipino-g3"),
                onRestartQuiz = { restartCalls++ },
                onReviewClues = { reviewCalls++ },
            )
        }

        composeRule.onNodeWithText("Balikan ang mga pahiwatig").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(1, reviewCalls)
            assertEquals(0, restartCalls)
        }
    }

    @Test
    fun reviewCluesThenBackReturnsToFailSummaryWithoutRestart() {
        var reviewing by mutableStateOf(false)
        var restartCalls by mutableIntStateOf(0)

        composeRule.setContent {
            arenaUnderTest(
                state = failedFinishedState(isReviewingClues = reviewing),
                onRestartQuiz = { restartCalls++ },
                onReviewClues = { reviewing = true },
                onExitClueReview = { reviewing = false },
            )
        }

        composeRule.onNodeWithTag(ArenaTestTags.ReviewCluesButton).performClick()
        composeRule.onNodeWithTag(ArenaTestTags.ClueReview).assertIsDisplayed()
        composeRule.onNodeWithText("Forty is four tens.").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performScrollTo().performClick()
        composeRule.onNodeWithTag(ArenaTestTags.ClueReview).assertDoesNotExist()
        composeRule.onNodeWithText("Review clues").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, restartCalls) }
    }

    @Test
    fun passedQuizDoesNotShowReviewClues() {
        composeRule.setContent {
            arenaUnderTest(state = failedFinishedState(isPassed = true, isReviewingClues = false))
        }

        composeRule.onNodeWithTag(ArenaTestTags.ReviewCluesButton).assertDoesNotExist()
        composeRule.onNodeWithText("Review clues").assertDoesNotExist()
    }
}

private fun arenaUnderTest(
    state: AssessmentArenaUiState,
    onStartQuiz: (String) -> Unit = {},
    onRestartQuiz: () -> Unit = {},
    onReviewClues: () -> Unit = {},
    onExitClueReview: () -> Unit = {},
) {
    CompositionLocalProvider(LocalAnimationsDisabled provides true) {
        MaxinesWorldTheme {
            AssessmentArenaScreen(
                state = state,
                onBack = {},
                onSelectSubject = {},
                onSelectCurriculum = {},
                onStartQuiz = onStartQuiz,
                onSelectOption = {},
                onToggleHint = {},
                onSubmitAnswer = {},
                onNextQuestion = {},
                onRestartQuiz = onRestartQuiz,
                onReviewClues = onReviewClues,
                onExitClueReview = onExitClueReview,
                onExitQuiz = {},
                onDismissCelebration = {},
            )
        }
    }
}

private fun failedFinishedState(
    packId: String = "math-g3-ph",
    isPassed: Boolean = false,
    isReviewingClues: Boolean = false,
): AssessmentArenaUiState {
    val items = listOf(
        AssessmentQuestionItem(
            sequence = 1,
            prompt = "How many tens in 40?",
            options = listOf(
                AssessmentQuestionOption("a", "Four"),
                AssessmentQuestionOption("b", "Forty"),
            ),
            correctOptionIds = listOf("a"),
            explanation = "Forty is four tens.",
        ),
        AssessmentQuestionItem(
            sequence = 2,
            prompt = "Which shape has three sides?",
            options = listOf(
                AssessmentQuestionOption("a", "Triangle"),
                AssessmentQuestionOption("b", "Square"),
            ),
            correctOptionIds = listOf("a"),
            explanation = "A triangle has three sides.",
        ),
    )
    return AssessmentArenaUiState(
        isLoading = false,
        activeQuiz = ActiveAssessmentQuizState(
            packId = packId,
            items = items,
            currentIndex = items.lastIndex,
            isAnswerSubmitted = true,
            correctCount = if (isPassed) items.size else 1,
            isFinished = true,
            isPassed = isPassed,
            earnedStars = if (isPassed) 10 else 0,
            earnedTokens = if (isPassed) 2 else 0,
            isReviewingClues = isReviewingClues,
        ),
    )
}
