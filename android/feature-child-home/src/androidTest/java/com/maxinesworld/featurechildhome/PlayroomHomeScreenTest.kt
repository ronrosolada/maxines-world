package com.maxinesworld.featurechildhome

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** UI coverage for the Playroom home and the immediate GMRC/reward loop. */
class PlayroomHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun stateFor(completed: Int = 0): PlayroomHomeUiState.Content =
        PlayroomHomeUiState.Content(
            childName = "Maxine",
            subjects = canonicalSubjects,
            quest = QuestUi(
                task = "Complete 3 adventures across 2 learning areas this week.",
                pawPrintsCompleted = completed,
                pawPrintTotal = 3,
                recommendedSubjectId = canonicalSubjects.first().id,
                buttonLabel = "Continue",
            ),
            wildlifeStickers = WildlifeStickersUi(collectedCount = 0, totalCount = 0),
        )

    private fun setHome(
        state: PlayroomHomeUiState,
        onSubjectClick: (String) -> Unit = {},
        onCollectionClick: () -> Unit = {},
        onTreatShopClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            PlayroomHomeScreen(
                state = state,
                onSubjectClick = onSubjectClick,
                onQuestAction = {},
                onHomeClick = {},
                onCollectionClick = onCollectionClick,
                onTreatShopClick = onTreatShopClick,
                onParentsClick = {},
            )
        }
    }

    @Test
    fun loadingStateExplainsWhatMiloIsDoing() {
        setHome(PlayroomHomeUiState.Loading)
        composeRule.onNodeWithText("Loading your adventures…").assertIsDisplayed()
        composeRule.onNodeWithText("Milo is getting your adventures ready.").assertIsDisplayed()
    }

    @Test
    fun gmrcIsDisplayedAndEnabledFromFirstSession() {
        setHome(stateFor())
        composeRule.onNodeWithText("GMRC").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun allSixCanonicalSubjectsRender() {
        setHome(stateFor())
        // Makabansa is the Matatag successor of Araling Panlipunan — legacy
        // AP lessons ship inside the Makabansa collection (2026-08-06 merge).
        listOf("Mathematics", "English", "Science", "Filipino", "Makabansa", "GMRC")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun weeklyExpeditionCopyRenders() {
        setHome(stateFor(2))
        composeRule.onNodeWithText("This Week's Quest").assertIsDisplayed()
        composeRule.onNodeWithText("Complete 3 adventures across 2 learning areas this week.").assertIsDisplayed()
        composeRule.onNodeWithText("Wildlife Stickers").assertIsDisplayed()
        composeRule.onNodeWithText("Open Field Guide").assertIsDisplayed()
    }

    @Test
    fun recommendedSubjectShowsStartHereBadge() {
        setHome(stateFor())
        composeRule.onNodeWithText("Start here!").assertIsDisplayed()
    }

    @Test
    fun treatShopEntryPointInvokesCallback() {
        var opens = 0
        setHome(stateFor(), onTreatShopClick = { opens++ })
        composeRule.onNodeWithText("Treat Shop").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, opens) }
    }

    @Test
    fun primaryActionsHaveTalkBackLabels() {
        setHome(stateFor())
        listOf("Treat Shop", "Collection", "Parents").forEach { label ->
            composeRule.onNodeWithContentDescription(label, useUnmergedTree = true).assertHasClickAction()
        }
    }

    @Test
    fun collectionNavigationInvokesCallback() {
        var opens = 0
        setHome(stateFor(), onCollectionClick = { opens++ })
        composeRule.onNodeWithText("Collection").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(1, opens)
        composeRule.onAllNodesWithText("Avatars").assertCountEquals(0)
        composeRule.onAllNodesWithText("Coming soon").assertCountEquals(0)
    }

    @Test
    fun gmrcClickNavigates() {
        var clicks = 0
        setHome(stateFor(), onSubjectClick = { clicks++ })
        composeRule.onNodeWithText("GMRC").performClick()
        composeRule.waitForIdle()
        assertTrue(clicks > 0)
    }

    @Test
    fun greetingUsesChildName() {
        setHome(stateFor())
        composeRule.onNodeWithText("Hi, Maxine!").assertIsDisplayed()
    }
}
