package com.maxinesworld.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.engineactivity.renderers.SequenceBuilderRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-level regression coverage for the Sequence CTA contract (#65):
 * the incomplete CTA must be a genuine no-op (disabled click must not
 * submit or advance), and the complete sequence must submit with the
 * authored order as the correct answer.
 */
@RunWith(AndroidJUnit4::class)
class SequenceCtaContractTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val step = ActivityStep(
        id = "sequence-cta-contract",
        type = "SEQUENCE_ORDER",
        question = "Put the steps in order.",
        sequenceSteps = listOf("First", "Second", "Third"),
    )

    private fun render(onResult: (ActivityResult) -> Unit) {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MaxinesWorldTheme {
                    SequenceBuilderRenderer(
                        step = step,
                        onResult = onResult,
                        onHint = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun incompleteCtaIsDisabledAndClickDoesNotSubmit() {
        var result: ActivityResult? = null
        render { result = it }

        val cta = composeRule.onNodeWithContentDescription("Place all cards (0/3)", useUnmergedTree = true)
        cta.assertIsDisplayed()
        cta.assertIsNotEnabled()

        cta.performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertNull("incomplete CTA click must not submit", result)
        }

        // After one tile, the label advances but the CTA stays a no-op.
        composeRule
            .onNodeWithContentDescription("Available: First — tap to add", useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()

        val partial = composeRule.onNodeWithContentDescription("Place all cards (1/3)", useUnmergedTree = true)
        partial.assertIsDisplayed()
        partial.assertIsNotEnabled()
        partial.performClick()
        composeRule.runOnIdle {
            assertNull("partial CTA click must not submit", result)
        }
    }

    @Test
    fun completeSequenceEnablesSubmitAndEmitsCorrectResult() {
        var result: ActivityResult? = null
        render { result = it }

        // Click the tiles in authored (original index) order — the correct sequence.
        listOf("First", "Second", "Third").forEach { item ->
            composeRule
                .onNodeWithContentDescription("Available: $item — tap to add", useUnmergedTree = true)
                .performClick()
            composeRule.waitForIdle()
        }

        val submit = composeRule.onNodeWithContentDescription("Submit", useUnmergedTree = true)
        submit.assertIsDisplayed()
        submit.performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(true, result?.correct)
            assertEquals(1, result?.attempts)
        }
    }
}
