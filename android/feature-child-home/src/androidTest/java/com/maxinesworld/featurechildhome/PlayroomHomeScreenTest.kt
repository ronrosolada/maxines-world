package com.maxinesworld.featurechildhome

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
                task = QuestTaskCopy.IncompleteToday,
                pawPrintsCompleted = completed,
                pawPrintTotal = 3,
                recommendedSubjectId = canonicalSubjects.first().id,
                buttonLabel = QuestButtonLabel.Continue,
            ),
            wildlifeStickers = WildlifeStickersUi(collectedCount = 0, totalCount = 0),
            sanctuary = SanctuaryUi(
                nextPiece = SanctuaryPieceUi(
                    id = "sunny-meadow",
                    name = "Sunny Meadow",
                    description = "A bright place for Milo's friends.",
                    iconKey = "meadow",
                ),
            ),
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

    /**
     * The home is one scrollable column: on narrow layouts the quest card,
     * sanctuary board, and subject grid sit below the fold. Scroll the
     * container until [text] exists before visibility or click assertions.
     */
    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
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
        scrollTo("GMRC")
        composeRule.onNodeWithText("GMRC").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun allSixCanonicalSubjectsRender() {
        setHome(stateFor())
        // Makabansa is the Matatag successor of Araling Panlipunan — legacy
        // AP lessons ship inside the Makabansa collection (2026-08-06 merge).
        // The grid is a plain Column: every card exists in the tree even when
        // off-screen, so existence proves all six render; scroll to verify the
        // bottom of the grid is actually reachable and displayed.
        scrollTo("GMRC")
        composeRule.onNodeWithText("GMRC").assertIsDisplayed()
        listOf("Mathematics", "English", "Science", "Filipino", "Makabansa", "GMRC")
            .forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun todaysQuestCopyRenders() {
        setHome(stateFor(2))
        scrollTo("Today’s Quest")
        composeRule.onNodeWithText("Today’s Quest").assertIsDisplayed()
        composeRule.onNodeWithText("Complete 3 learning adventures today.").assertIsDisplayed()
        composeRule.onNodeWithText("Wildlife Stickers").assertExists()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun dailyQuestShowsItsRewardBeforeCompletionAndSanctuaryProgress() {
        setHome(stateFor())
        composeRule.onNodeWithText("Reward at 3/3: a sanctuary piece + 5-minute play break").assertExists()
        composeRule.onNodeWithContentDescription("Quest reward: one sanctuary piece and five minute play break").assertExists()
        composeRule.onNodeWithText("Milo’s Wildlife Sanctuary").assertExists()
        composeRule.onNodeWithText("0 / 12 places added").assertExists()
        composeRule.onNodeWithText("Next place to add").assertExists()
        // The next piece name renders both on the board cell and in the
        // next-reward row — either is proof the preview surfaced it.
        composeRule.onAllNodesWithText("Sunny Meadow").assertAny(hasText("Sunny Meadow"))
    }

    @Test
    fun sanctuaryExplainsItsNextRewardInsteadOfRepeatingStickerSlots() {
        setHome(stateFor())
        composeRule.onNodeWithText("Build Milo’s home by finishing today’s 3 lessons.").assertExists()
        composeRule.onAllNodesWithText("Complete today's learning adventures to grow it.").assertCountEquals(0)
        composeRule.onNodeWithText("Wildlife Stickers").assertExists()
        composeRule.onNodeWithText("Open Field Guide").assertHasClickAction()
    }

    @Test
    fun completeSanctuaryShowsCompletionWithoutAnEarnHint() {
        // God mode (or genuinely finishing all 12 places) renders the same
        // complete state: nextPiece is null and every place is earned. The
        // earn hint must disappear — telling a child to "finish 3 lessons to
        // add this place" under "your home is complete" is contradictory.
        setHome(
            stateFor().copy(
                sanctuary = SanctuaryUi(
                    earnedPieces = 12,
                    visiblePieces = emptyList(),
                    nextPiece = null,
                    totalPieces = 12,
                ),
            )
        )
        composeRule.onNodeWithText("12 / 12 places added").assertExists()
        composeRule.onNodeWithText("Milo’s home is complete! You built every place.").assertExists()
        composeRule.onAllNodesWithText("Finish all 3 lessons in Today’s Quest to add this place.").assertCountEquals(0)
    }

    @Test
    fun homepageDoesNotDuplicateTheDailyQuestStartAction() {
        setHome(stateFor())
        composeRule.onAllNodesWithText("Start here!").assertCountEquals(0)
        composeRule.onAllNodesWithText("Start your first adventure").assertCountEquals(0)
    }

    @Test
    fun treatShopEntryPointInvokesCallback() {
        var opens = 0
        setHome(stateFor(), onTreatShopClick = { opens++ })
        // The sanctuary card sits below the fold and its children report no
        // bounds until scrolled into view. The workshop entry is the last
        // child of that card, so scroll the stickers section (just below it)
        // into view first — then the button is placed and clickable.
        composeRule.onNodeWithContentDescription("Sanctuary Workshop")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, opens) }
    }

    @Test
    fun ownedKeepsakesAreVisibleOnHome() {
        setHome(
            stateFor().copy(
                ownedKeepsakes = listOf(
                    KeepsakeUi(itemId = "fish-treat-basket", name = "Fish Treat Basket", iconKey = "basket"),
                ),
            )
        )
        composeRule.onNodeWithText("Milo’s decorations").assertIsDisplayed()
        composeRule.onNodeWithText("Fish Treat Basket").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Milo's keepsake: Fish Treat Basket").assertIsDisplayed()
    }

    @Test
    fun noKeepsakesMeansNoStrip() {
        setHome(stateFor())
        composeRule.onAllNodesWithText("Milo’s decorations").assertCountEquals(0)
    }

    @Test
    fun stableHomeInteractionTagsArePresent() {
        setHome(stateFor())
        composeRule.onNodeWithTag(PlayroomHomeTestTags.TodayQuest).assertExists()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak).assertExists()
        canonicalSubjects.forEach { subject ->
            composeRule.onNodeWithTag(PlayroomHomeTestTags.subject(subject.id)).assertExists()
        }
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Collection).assertHasClickAction()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Parents).assertHasClickAction()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.SelectedNavigation).assertIsSelected()
    }

    @Test
    fun primaryActionsHaveTalkBackLabels() {
        setHome(stateFor())
        listOf("Sanctuary Workshop", "Collection", "Parents").forEach { label ->
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
        scrollTo("GMRC")
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
