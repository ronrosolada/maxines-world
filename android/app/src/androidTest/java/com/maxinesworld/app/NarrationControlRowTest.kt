package com.maxinesworld.app

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.maxinesworld.featurelessonplayer.NarrationControlRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NarrationControlRowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun narrationToggleControlsReplayAndPersistsCallbackContract() {
        var replayed = 0
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                var enabled by remember { mutableStateOf(false) }
                NarrationControlRow(
                    narrationEnabled = enabled,
                    ttsSpeaking = false,
                    onToggle = { enabled = !enabled },
                    onReplay = { replayed++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Turn narration on").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Turn narration off").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Replay narration").assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription("Turn narration off").performClick()
        composeRule.onNodeWithContentDescription("Replay narration").assertIsNotEnabled()
        assertEquals(1, replayed)
    }
}
