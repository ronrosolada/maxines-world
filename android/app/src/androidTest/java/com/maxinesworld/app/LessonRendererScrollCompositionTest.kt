package com.maxinesworld.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.MatchPair
import com.maxinesworld.coremodel.SortItem
import com.maxinesworld.engineactivity.renderers.AnimatedExplanationRenderer
import com.maxinesworld.engineactivity.renderers.MatchingPairsRenderer
import com.maxinesworld.engineactivity.renderers.MultipleChoiceRenderer
import com.maxinesworld.engineactivity.renderers.SequenceBuilderRenderer
import com.maxinesworld.engineactivity.renderers.SortAndClassifyRenderer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonRendererScrollCompositionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun multipleChoice_composes_inside_scrolling_parent() {
        val step = ActivityStep(
            id = "scroll-multiple-choice",
            type = "MULTIPLE_CHOICE_V1",
            question = "Which animal is a mammal?",
            options = listOf("Cat", "Rock"),
            correctIndex = 0,
        )

        setRendererContent {
            MultipleChoiceRenderer(step = step, onResult = {}, onHint = {})
        }

        composeRule.onNodeWithText(step.question).assertIsDisplayed()
    }

    @Test
    fun sequenceBuilder_composes_inside_scrolling_parent() {
        val step = ActivityStep(
            id = "scroll-sequence",
            type = "SEQUENCE_BUILDER_V1",
            question = "Put these in order.",
            sequenceSteps = listOf("First", "Second"),
        )

        setRendererContent {
            SequenceBuilderRenderer(step = step, onResult = {}, onHint = {})
        }

        composeRule.onNodeWithText(step.question).assertIsDisplayed()
    }

    @Test
    fun matchingPairs_composes_inside_scrolling_parent() {
        val step = ActivityStep(
            id = "scroll-matching",
            type = "MATCHING_PAIRS_V1",
            question = "Match the pairs.",
            matchPairs = listOf(MatchPair("Cat", "Mammal")),
        )

        setRendererContent {
            MatchingPairsRenderer(step = step, onResult = {}, onHint = {})
        }

        composeRule.onNodeWithText(step.question).assertIsDisplayed()
    }

    @Test
    fun sortAndClassify_composes_inside_scrolling_parent() {
        val step = ActivityStep(
            id = "scroll-sort",
            type = "SORT_AND_CLASSIFY_V1",
            question = "Sort each item.",
            sortCategories = listOf("Mammal", "Not mammal"),
            sortItems = listOf(SortItem("Cat", categoryIndex = 0)),
        )

        setRendererContent {
            SortAndClassifyRenderer(step = step, onResult = {}, onHint = {})
        }

        composeRule.onNodeWithText(step.question).assertIsDisplayed()
    }

    @Test
    fun animatedExplanation_composes_inside_scrolling_parent() {
        val step = ActivityStep(
            id = "scroll-explanation",
            type = "ANIMATED_EXPLANATION_V1",
            question = "Read this explanation.",
            narrationText = "Cats are mammals.",
        )

        setRendererContent {
            AnimatedExplanationRenderer(step = step, onResult = {}, onHint = {})
        }

        composeRule.onNodeWithText(step.narrationText).assertIsDisplayed()
    }

    private fun setRendererContent(content: @Composable () -> Unit) {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MaxinesWorldTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        content()
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }
}
