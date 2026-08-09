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
        onResumeLearning: (String) -> Unit = {},
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
                onResumeLearning = onResumeLearning,
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
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun dailyQuestShowsItsRewardBeforeCompletionAndSanctuaryProgress() {
        setHome(stateFor())
        composeRule.onNodeWithText("Reward at 3/3: a sanctuary piece + 5-minute play break").assertExists()
        composeRule.onNodeWithContentDescription("Quest reward: one sanctuary piece and five minute play break").assertExists()
        composeRule.onNodeWithText("Milo's Wildlife Sanctuary").assertExists()
        composeRule.onNodeWithText("0/12").assertExists()
        composeRule.onNodeWithText("Next: Sunny Meadow").assertExists()
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
        composeRule.onNodeWithText("Sanctuary Workshop").assertIsEnabled().performClick()
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
        composeRule.onNodeWithText("Milo's decorations").assertIsDisplayed()
        composeRule.onNodeWithText("Fish Treat Basket").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Milo's keepsake: Fish Treat Basket").assertIsDisplayed()
    }

    @Test
    fun noKeepsakesMeansNoStrip() {
        setHome(stateFor())
        composeRule.onNodeWithText("Milo's decorations").assertDoesNotExist()
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
        composeRule.onNodeWithText("GMRC").performClick()
        composeRule.waitForIdle()
        assertTrue(clicks > 0)
    }

    @Test
    fun greetingUsesChildName() {
        setHome(stateFor())
        composeRule.onNodeWithText("Hi, Maxine!").assertIsDisplayed()
    }

    @Test
    fun firstSessionOffersAStartHereLearningAction() {
        var openedLesson = ""
        setHome(
            stateFor().copy(
                resumeLesson = LearningResumeUi(
                    lessonId = "math-g3-q1-w01-d01",
                    title = "Shape Trail",
                    subjectId = "mathematics",
                    subjectName = "Number Fun",
                    estimatedMinutes = 10,
                    isFirstLesson = true,
                ),
            ),
            onResumeLearning = { openedLesson = it },
        )

        composeRule
            .onNodeWithContentDescription("Start your first adventure. Shape Trail. Number Fun. Start lesson.")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals("math-g3-q1-w01-d01", openedLesson) }
    }

    @Test
    fun returningLearnerSeesWhereToPickUp() {
        setHome(
            stateFor().copy(
                resumeLesson = LearningResumeUi(
                    lessonId = "science-g3-q1-w01-d02",
                    title = "Plant Detectives",
                    subjectId = "science",
                    subjectName = "Discovery",
                    estimatedMinutes = 12,
                    isFirstLesson = false,
                ),
            ),
        )

        composeRule.onNodeWithText("Pick up where you left off").assertIsDisplayed()
        composeRule.onNodeWithText("Plant Detectives").assertIsDisplayed()
    }
}
