package com.maxinesworld.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.SortItem
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.engineactivity.renderers.SortAndClassifyRenderer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SortAndClassifyRendererTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tapItemThenCategoryPlacesAndSubmitsCorrectly() {
        val item = "Three baskets of four kittens"
        val step = ActivityStep(
            id = "sort-tap-contract",
            type = "SORT_AND_CLASSIFY_V1",
            question = "Sort each group.",
            sortCategories = listOf("Fits", "Does not fit"),
            sortItems = listOf(SortItem(item, categoryIndex = 0)),
        )
        var result: ActivityResult? = null

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MaxinesWorldTheme {
                    SortAndClassifyRenderer(
                        step = step,
                        onResult = { result = it },
                        onHint = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription("Item: $item", useUnmergedTree = true)
            .performClick()
        composeRule
            .onNodeWithContentDescription("Category Fits (0 items)", useUnmergedTree = true)
            .assertIsEnabled()
            .performClick()
        composeRule
            .onNodeWithContentDescription("$item — sorted", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeRule
            .onNodeWithContentDescription("Submit", useUnmergedTree = true)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(true, result?.correct)
            assertEquals(1, result?.attempts)
        }
    }

    @Test
    fun emptySubmitExplainsCardsMustBePlaced() {
        val step = ActivityStep(
            id = "sort-empty-submit",
            type = "SORT_AND_CLASSIFY_V1",
            question = "Sort each group.",
            sortCategories = listOf("Fits", "Does not fit"),
            sortItems = listOf(
                SortItem("Three baskets of four kittens", categoryIndex = 0),
                SortItem("Five baskets of four kittens", categoryIndex = 1),
            ),
        )

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MaxinesWorldTheme {
                    SortAndClassifyRenderer(
                        step = step,
                        onResult = {},
                        onHint = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription(
                "Sort progress: 0 of 2 cards placed. Place all 2 cards first",
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Submit", useUnmergedTree = true)
            .assertIsNotEnabled()
    }
}
