package com.maxinesworld.featurechildhome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.maxinesworld.coremodel.ChildLevelPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI coverage for the Option 3 Playroom Collections home:
 * the six canonical subjects, Kindness gate states, quest panel,
 * and that a locked card cannot be clicked through to navigation.
 */
class PlayroomHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun stateFor(completedLessons: Int): PlayroomHomeUiState.Content {
        val level = ChildLevelPolicy.levelFor(completedLessons)
        val unlocked = level >= ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        val lessonsToGo = ChildLevelPolicy.lessonsRemainingTo(
            completedLessons, ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        )
        val subjects = canonicalSubjects.map { subject ->
            if (subject.id == "gmrc" && !unlocked) {
                subject.copy(
                    availability = SubjectAvailability.Locked,
                    lockReason = "Locked until level 4 · $lessonsToGo lesson${if (lessonsToGo == 1) "" else "s"} to go",
                )
            } else subject
        }
        return PlayroomHomeUiState.Content(
            childName = "Maxine",
            streakDays = 0,
            xp = 0,
            subjects = subjects,
            quest = QuestUi(
                task = "Complete one activity to earn today’s paw print.",
                pawPrintsCompleted = 0, pawPrintTotal = 3,
                buttonLabel = "Continue",
            ),
            wildlifeStickers = WildlifeStickersUi(collectedCount = 0, totalCount = 0),
        )
    }

    private fun setHome(
        state: PlayroomHomeUiState,
        onSubjectClick: (String) -> Unit = {},
        onCollectionClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            PlayroomHomeScreen(
                state = state,
                onSubjectClick = onSubjectClick,
                onQuestAction = {},
                onHomeClick = {},
                onCollectionClick = onCollectionClick,
                onParentsClick = {},
            )
        }
    }

    @Test
    fun lockedState_showsLockReason() {
        setHome(stateFor(0))
        composeRule.onNodeWithText("Locked until level 4 · 12 lessons to go").assertIsDisplayed()
    }

    @Test
    fun nearlyUnlockedState_showsSingularLessonRemaining() {
        setHome(stateFor(11))
        composeRule.onNodeWithText("Locked until level 4 · 1 lesson to go").assertIsDisplayed()
    }

    @Test
    fun unlockedState_hasNoLockReason() {
        setHome(stateFor(12))
        composeRule.onNodeWithText("Locked until level 4 · 1 lesson to go").assertDoesNotExist()
    }

    @Test
    fun lockedCard_clickDoesNotNavigate() {
        var clicks = 0
        setHome(stateFor(0), onSubjectClick = { clicks++ })
        // Kindness card is click-disabled when locked
        composeRule.onNodeWithText("GMRC").assertIsNotEnabled()
        composeRule.onNodeWithText("GMRC").performClick()
        composeRule.waitForIdle()
        assertEquals("locked card must not navigate", 0, clicks)
    }

    @Test
    fun unlockedCard_clickNavigates() {
        var clicks = 0
        setHome(stateFor(12), onSubjectClick = { clicks++ })
        composeRule.onNodeWithText("GMRC").assertIsEnabled()
        composeRule.onNodeWithText("GMRC").performClick()
        composeRule.waitForIdle()
        assertTrue("unlocked card must navigate", clicks > 0)
    }

    @Test
    fun allSixCanonicalSubjectsRender() {
        setHome(stateFor(12))
        listOf("Mathematics", "English", "Science", "Filipino", "Araling Panlipunan", "GMRC")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun questPanelAndStickerBookRender() {
        setHome(stateFor(0))
        composeRule.onNodeWithText("Today’s Quest").assertIsDisplayed()
        composeRule.onNodeWithText("Wildlife Stickers").assertIsDisplayed()
        composeRule.onNodeWithText("Open Field Guide").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun greetingUsesChildName() {
        setHome(stateFor(0))
        composeRule.onNodeWithText("Hi, Maxine!").assertIsDisplayed()
    }

    @Test
    fun collectionNavIsAvailableAndInvokesCallback() {
        var opens = 0
        setHome(stateFor(0), onCollectionClick = { opens++ })
        composeRule.onNodeWithText("Collection").assertIsDisplayed()
        composeRule.onNodeWithText("Collection").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals("collection must navigate", 1, opens)
        composeRule.onAllNodesWithText("Avatars").assertCountEquals(0)
        composeRule.onAllNodesWithText("Coming soon").assertCountEquals(0)
    }
}
