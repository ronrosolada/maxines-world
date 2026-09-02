package com.maxinesworld.featurechildhome

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TodayQuestCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun questTargetOfflineReadinessDefaultsToFalseAndPreservesExplicitTrue() {
        assertFalse(videoTarget().isReadyOffline)
        assertTrue(videoTarget(isReadyOffline = true).isReadyOffline)
    }

    @Test
    fun readyIncompleteVideoShowsOfflineBadgeAndQuestTargetSemantics() {
        setCard(targets = listOf(videoTarget(isReadyOffline = true)))

        composeRule.onNodeWithText("Offline", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription(
            "Quest target: Science: Living Things · 02:00",
        ).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun videoWithoutOfflineReadinessShowsGettingReadyNotOffline() {
        setCard(
            quest = quest(
                buttonLabel = QuestButtonLabel.StartQuest,
                buttonAction = QuestAction.OpenVideoQuest,
            ),
            targets = listOf(videoTarget(isReadyOffline = false)),
        )

        composeRule.onNodeWithText("Offline", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Getting ready", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Start quest", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.QuestAction)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun continueQuestLabelStaysAMissionStartOnAnUnreadiedVideo() {
        setCard(
            quest = quest(
                buttonLabel = QuestButtonLabel.ContinueQuest,
                buttonAction = QuestAction.OpenVideoQuest,
            ),
            targets = listOf(videoTarget(isReadyOffline = false)),
        )

        composeRule.onNodeWithText("Continue quest", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.QuestAction)
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText("Getting ready", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Offline", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun completedReadyVideoHidesOfflineBadgeAndAnnouncesDoneState() {
        setCard(targets = listOf(videoTarget(isCompleted = true, isReadyOffline = true)))

        composeRule.onNodeWithText("Offline", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(
            "Quest target done: Living Things · 02:00",
        ).assertIsDisplayed()
    }

    @Test
    fun arenaTargetUsesQuizSemanticsAndShowsOfflineBadgeWhenReady() {
        setCard(targets = listOf(arenaTarget(isReadyOffline = true)))

        composeRule.onNodeWithText("Offline", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Quiz").assertExists()
        composeRule.onNodeWithContentDescription(
            "Quiz target: Grade 3 Science Challenge",
        ).assertIsDisplayed()
    }

    @Test
    fun completedArenaUsesQuizDoneSemanticsAndHidesOfflineBadge() {
        setCard(targets = listOf(arenaTarget(isCompleted = true, isReadyOffline = true)))

        composeRule.onNodeWithText("Offline", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(
            "Quiz target done: Grade 3 Science Challenge",
        ).assertIsDisplayed()
    }

    @Test
    fun targetClickReturnsTheExactSelectedTargetWithoutInvokingQuestAction() {
        val first = videoTarget(mediaId = "science-one", title = "Living Things")
        val second = videoTarget(mediaId = "science-two", title = "Habitats")
        val clickedTargets = mutableListOf<QuestTargetUi>()
        val actions = mutableListOf<QuestAction>()
        setCard(
            targets = listOf(first, second),
            onQuestAction = { actions += it },
            onQuestTargetClick = { clickedTargets += it },
        )

        composeRule.onNodeWithContentDescription(
            "Quest target: Science: Habitats · 02:00",
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(1, clickedTargets.size)
            assertSame(second, clickedTargets.single())
            assertTrue(actions.isEmpty())
        }
    }

    @Test
    fun wholeCardClickDispatchesConfiguredCompletionActionOnce() {
        val actions = mutableListOf<QuestAction>()
        setCard(
            quest = quest(
                isComplete = true,
                completed = 3,
                buttonLabel = QuestButtonLabel.OpenSanctuary,
                buttonAction = QuestAction.ViewReward,
            ),
            onQuestAction = { actions += it },
        )

        composeRule.onNodeWithContentDescription(
            "Today's mission. 3 of 3 complete",
        ).assertHasClickAction().performClick()

        composeRule.runOnIdle { assertEquals(listOf(QuestAction.ViewReward), actions) }
        composeRule.onNodeWithText("Reward earned: a sanctuary piece + one 5-minute play break").assertIsDisplayed()
    }

    @Test
    fun visibleCallToActionDispatchesConfiguredActionOnce() {
        val actions = mutableListOf<QuestAction>()
        setCard(
            quest = quest(buttonLabel = QuestButtonLabel.Retry, buttonAction = QuestAction.RetryMission),
            onQuestAction = { actions += it },
        )

        composeRule.onNodeWithContentDescription("Today's mission. 1 of 3 complete")
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle { assertEquals(listOf(QuestAction.RetryMission), actions) }
    }

    private fun setCard(
        quest: QuestUi = quest(),
        targets: List<QuestTargetUi> = quest.targets,
        onQuestAction: (QuestAction) -> Unit = {},
        onQuestTargetClick: (QuestTargetUi) -> Unit = {},
    ) {
        composeRule.setContent {
            TodayQuestCard(
                quest = quest.copy(targets = targets),
                onQuestAction = onQuestAction,
                onQuestTargetClick = onQuestTargetClick,
            )
        }
    }

    private fun quest(
        isComplete: Boolean = false,
        completed: Int = 1,
        buttonLabel: QuestButtonLabel = QuestButtonLabel.Continue,
        buttonAction: QuestAction = QuestAction.Continue,
        targets: List<QuestTargetUi> = emptyList(),
    ) = QuestUi(
        task = if (isComplete) QuestTaskCopy.CompleteToday else QuestTaskCopy.IncompleteToday,
        pawPrintsCompleted = completed,
        pawPrintTotal = 3,
        isComplete = isComplete,
        buttonLabel = buttonLabel,
        buttonAction = buttonAction,
        targets = targets,
    )

    private fun videoTarget(
        mediaId: String = "science-video",
        title: String = "Living Things",
        isCompleted: Boolean = false,
        isReadyOffline: Boolean = false,
    ) = QuestTargetUi(
        mediaId = mediaId,
        title = title,
        subjectId = "science",
        displaySubject = "Science",
        durationSeconds = 120,
        durationLabel = "02:00",
        isCompleted = isCompleted,
        isReadyOffline = isReadyOffline,
    )

    private fun arenaTarget(
        isCompleted: Boolean = false,
        isReadyOffline: Boolean = false,
    ) = QuestTargetUi(
        mediaId = "arena:science-g3",
        title = "Grade 3 Science Challenge",
        subjectId = "science",
        displaySubject = "Science",
        durationSeconds = 0,
        durationLabel = "",
        isCompleted = isCompleted,
        type = QuestTargetType.ARENA,
        arenaPackId = "science-g3",
        isReadyOffline = isReadyOffline,
    )
}
