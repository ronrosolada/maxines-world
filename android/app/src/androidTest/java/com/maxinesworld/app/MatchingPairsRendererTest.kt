package com.maxinesworld.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.MatchPair
import com.maxinesworld.engineactivity.renderers.MatchingPairsRenderer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchingPairsRendererTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun matchedLeftItemCannotBeReusedWhenRightLabelsRepeat() {
        val step = ActivityStep(
            id = "matching-repeat-labels",
            type = "MATCHING_PAIRS_V1",
            question = "Match each example.",
            matchPairs = listOf(
                MatchPair("L1", "fits"),
                MatchPair("L2", "fits"),
            ),
        )

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MaxinesWorldTheme {
                    MatchingPairsRenderer(
                        step = step,
                        onResult = {},
                        onHint = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription("Left: L1", useUnmergedTree = true)
            .performClick()
        composeRule
            .onNodeWithContentDescription("Right: Milo says yes 2", useUnmergedTree = true)
            .performClick()

        composeRule
            .onNodeWithContentDescription("Left: L1 — matched", useUnmergedTree = true)
            .assertIsNotEnabled()
    }
}
