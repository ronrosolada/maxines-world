package com.maxinesworld.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.maxinesworld.featurerewards.TreatShopContent
import com.maxinesworld.featurerewards.TreatShopUiState
import org.junit.Assert.assertTrue
import org.junit.Test

class TreatShopScreenTest {
    @get:org.junit.Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shopShowsBalanceItemsAndPurchaseAction() {
        var purchased = false
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                TreatShopContent(
                    state = TreatShopUiState(coins = 10),
                    onBack = {},
                    onPurchase = { purchased = true },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Milo's Sanctuary Workshop").assertIsDisplayed()
        composeRule.onNodeWithText("10 sanctuary tokens").assertIsDisplayed()
        composeRule.onNodeWithText("Fish Treat Basket").assertIsDisplayed()
        composeRule.onNodeWithText("5 tokens").assertIsDisplayed()
        composeRule.onAllNodesWithText("Get it")[0].performClick()
        composeRule.runOnIdle { assertTrue(purchased) }
    }

    @Test
    fun shopShowsDistinctArtworkPerItem() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                TreatShopContent(
                    state = TreatShopUiState(coins = 20),
                    onBack = {},
                    onPurchase = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Every item exposes its own accessible, non-emoji artwork.
        composeRule.onNodeWithContentDescription("Fish Treat Basket").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cozy Milo Cushion").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Starry Food Bowl").assertIsDisplayed()
    }

    @Test
    fun purchaseRevealShowsCelebration() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                TreatShopContent(
                    state = TreatShopUiState(
                        coins = 10,
                        message = "Fish Treat Basket is now part of Milo's sanctuary.",
                    ),
                    onBack = {},
                    onPurchase = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Fish Treat Basket is now part of Milo's sanctuary.")
            .assertIsDisplayed()
    }
}
