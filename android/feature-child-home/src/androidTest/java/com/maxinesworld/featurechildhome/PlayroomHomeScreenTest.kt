package com.maxinesworld.featurechildhome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.maxinesworld.coremodel.ChildLevelPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI coverage for the Kindness island gate states (Phase 2):
 * locked, nearly-unlocked (11/12), unlocked — and that a locked island
 * cannot be clicked through to navigation.
 */
class PlayroomHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun stateFor(completedLessons: Int): PlayroomHomeState {
        val level = ChildLevelPolicy.levelFor(completedLessons)
        val unlocked = level >= ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        val lessonsToGo = ChildLevelPolicy.lessonsRemainingTo(
            completedLessons, ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        )
        val islands = defaultPlayroomIslands.map { island ->
            if (island.id == "gmrc") {
                island.copy(
                    locked = !unlocked,
                    subtitle = if (unlocked) "Kindness awaits!"
                    else "Unlocks at Level 4 · $lessonsToGo lesson${if (lessonsToGo == 1) "" else "s"} to go"
                )
            } else island
        }
        return PlayroomHomeState(islands = islands)
    }

    @Test
    fun lockedState_showsLockedSubtitle() {
        composeRule.setContent { PlayroomHomeScreen(state = stateFor(0), onDestinationClick = {}, onQuestClick = {}, onHomeClick = {}, onProgressClick = {}, onAvatarsClick = {}, onParentsClick = {}) }
        composeRule.onNodeWithText("Unlocks at Level 4 · 12 lessons to go").assertIsDisplayed()
    }

    @Test
    fun nearlyUnlockedState_showsSingularLessonRemaining() {
        composeRule.setContent { PlayroomHomeScreen(state = stateFor(11), onDestinationClick = {}, onQuestClick = {}, onHomeClick = {}, onProgressClick = {}, onAvatarsClick = {}, onParentsClick = {}) }
        composeRule.onNodeWithText("Unlocks at Level 4 · 1 lesson to go").assertIsDisplayed()
    }

    @Test
    fun unlockedState_showsKindnessAwaits() {
        composeRule.setContent { PlayroomHomeScreen(state = stateFor(12), onDestinationClick = {}, onQuestClick = {}, onHomeClick = {}, onProgressClick = {}, onAvatarsClick = {}, onParentsClick = {}) }
        composeRule.onNodeWithText("Kindness awaits!").assertIsDisplayed()
    }

    @Test
    fun lockedIsland_clickDoesNotNavigate() {
        var clicks = 0
        composeRule.setContent {
            PlayroomHomeScreen(
                state = stateFor(0),
                onDestinationClick = { clicks++ },
                onQuestClick = {}, onHomeClick = {}, onProgressClick = {}, onAvatarsClick = {}, onParentsClick = {},
            )
        }
        // Kindness island is click-disabled when locked
        composeRule.onNodeWithText("Kindness").assertIsNotEnabled()
        composeRule.onNodeWithText("Kindness").performClick()
        composeRule.waitForIdle()
        assertEquals("locked island must not navigate", 0, clicks)
    }

    @Test
    fun unlockedIsland_clickNavigates() {
        var clicks = 0
        composeRule.setContent {
            PlayroomHomeScreen(
                state = stateFor(12),
                onDestinationClick = { clicks++ },
                onQuestClick = {}, onHomeClick = {}, onProgressClick = {}, onAvatarsClick = {}, onParentsClick = {},
            )
        }
        composeRule.onNodeWithText("Kindness").assertIsEnabled()
        composeRule.onNodeWithText("Kindness").performClick()
        composeRule.waitForIdle()
        assertTrue("unlocked island must navigate", clicks > 0)
    }
}
