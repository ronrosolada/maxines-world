package com.maxinesworld.featurechildhome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Child-facing interaction contract for the Playroom home.
 *
 * These tests intentionally target stable tags and semantics rather than
 * layout details so future visual work can preserve the information hierarchy.
 */
class PlayroomHomeInteractionContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    private data class ExpectedSubject(
        val id: String,
        val formalName: String,
        val playfulName: String,
    )

    // Keep these values independent from canonicalSubjects so an accidental
    // rename, removal, or reorder cannot update both the fixture and contract.
    private val expectedSubjects = listOf(
        ExpectedSubject("mathematics", "Mathematics", "Number Fun"),
        ExpectedSubject("english", "English", "Story Time"),
        ExpectedSubject("science", "Science", "Discovery"),
        ExpectedSubject("filipino", "Filipino", "Kwentuhan"),
        ExpectedSubject("makabansa", "Makabansa", "Bayan at Kultura"),
        ExpectedSubject("gmrc", "GMRC", "Kindness"),
    )

    private val subjectCardTag = SemanticsMatcher("a child-home subject card tag") { node ->
        node.config.contains(SemanticsProperties.TestTag) &&
            node.config[SemanticsProperties.TestTag].startsWith("home_subject_")
    }

    private fun stateForContract(
        targets: List<QuestTargetUi> = emptyList(),
        streakDays: Int = 2,
    ): PlayroomHomeUiState.Content =
        PlayroomHomeUiState.Content(
            childName = "Maxine",
            subjects = canonicalSubjects.mapIndexed { index, subject ->
                subject.copy(
                    progressPercent = when (index) {
                        0 -> null
                        1 -> 42
                        else -> 100
                    },
                    completedVideos = when (index) {
                        0 -> 0
                        1 -> 2
                        else -> 3
                    },
                    totalVideos = when (index) {
                        0 -> 4
                        1 -> 5
                        else -> 3
                    },
                )
            },
            quest = QuestUi(
                task = QuestTaskCopy.IncompleteToday,
                pawPrintsCompleted = 2,
                pawPrintTotal = 3,
                recommendedSubjectId = canonicalSubjects.first().id,
                buttonLabel = QuestButtonLabel.Continue,
                targets = targets,
            ),
            wildlifeStickers = WildlifeStickersUi(collectedCount = 2, totalCount = 12),
            streakDays = streakDays,
        )

    private fun setHome(
        width: Dp = 411.dp,
        fontScale: Float = 1f,
        targets: List<QuestTargetUi> = emptyList(),
        onCollectionClick: () -> Unit = {},
        onParentsClick: () -> Unit = {},
        onQuestAction: (QuestAction) -> Unit = {},
        onQuestTargetClick: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = fontScale),
            ) {
                Box(Modifier.requiredSize(width = width, height = 720.dp)) {
                    PlayroomHomeScreen(
                        state = stateForContract(targets),
                        onSubjectClick = {},
                        onQuestAction = onQuestAction,
                        onQuestTargetClick = onQuestTargetClick,
                        onHomeClick = {},
                        onCollectionClick = onCollectionClick,
                        onParentsClick = onParentsClick,
                    )
                }
            }
        }
    }

    @Test
    fun todayQuestExposesExactlyOnePrimaryActionAndPreservesTargetSubject() {
        val actions = mutableListOf<QuestAction>()
        val targetSubjects = mutableListOf<String>()
        setHome(
            targets = listOf(
                QuestTargetUi(
                    mediaId = "english-video-1",
                    title = "Word Roots",
                    subjectId = "english",
                    displaySubject = "English",
                    durationSeconds = 60,
                    durationLabel = "01:00",
                    isCompleted = false,
                ),
                QuestTargetUi(
                    mediaId = "science-video-2",
                    title = "Living Things",
                    subjectId = "science",
                    displaySubject = "Science",
                    durationSeconds = 120,
                    durationLabel = "02:00",
                    isCompleted = false,
                ),
            ),
            onQuestAction = { actions += it },
            onQuestTargetClick = { targetSubjects += it },
        )

        val todayQuest = composeRule.onNodeWithTag(PlayroomHomeTestTags.TodayQuest)
        todayQuest.assertExists()

        // The whole tagged card is the generous primary target. The visible
        // button remains as a discoverable affordance for the same action.
        todayQuest.assertHasClickAction()
        todayQuest.performClick()
        composeRule.runOnIdle { assertEquals(listOf(QuestAction.Continue), actions) }

        // Clicking a non-next target must preserve that row's subject instead
        // of dispatching the enum CTA, which resolves the next target.
        composeRule.onNodeWithContentDescription(
            "Quest target: Science: Living Things · 02:00",
        ).assertHasClickAction().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("science"), targetSubjects)
            assertEquals(listOf(QuestAction.Continue), actions)
        }

        composeRule.onNodeWithContentDescription(
            "Quest target: English: Word Roots · 01:00",
        ).assertHasClickAction().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("science", "english"), targetSubjects)
            assertEquals(listOf(QuestAction.Continue), actions)
        }
        composeRule.onNodeWithText("01:00").assertExists()
        composeRule.onNodeWithText("02:00").assertExists()

        composeRule.onNodeWithText("Continue").assertHasClickAction().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(QuestAction.Continue, QuestAction.Continue), actions)
        }
        composeRule.onAllNodesWithText("Continue").assertCountEquals(1)
    }

    @Test
    fun eachSubjectExposesStableLabelAndProgressState() {
        setHome()

        val renderedSubjectTags = composeRule.onAllNodes(subjectCardTag)
            .fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag] }
        assertEquals(
            expectedSubjects.map { PlayroomHomeTestTags.subject(it.id) },
            renderedSubjectTags,
        )

        expectedSubjects.forEachIndexed { index, subject ->
            val expectedProgress = when (index) {
                0 -> "Not started · 0 of 4 videos"
                1 -> "2 of 5 videos"
                else -> "Complete · 3 of 3 videos"
            }
            composeRule.onNodeWithTag(PlayroomHomeTestTags.subject(subject.id))
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ContentDescription,
                        listOf("${subject.formalName}, ${subject.playfulName}"),
                    ),
                )
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.StateDescription,
                        expectedProgress,
                    ),
                )
        }
    }

    @Test
    fun selectedNavigationDestinationIsAnnounced() {
        setHome()

        composeRule.onNodeWithTag(PlayroomHomeTestTags.SelectedNavigation)
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Home"),
                ),
            )
    }

    @Test
    fun homeNavigationContractWorksAtCompactMediumAndWideWidthsAndTwoFontScales() {
        val width = mutableStateOf(360.dp)
        val fontScale = mutableFloatStateOf(1f)
        var collectionClicks by mutableStateOf(0)
        var parentsClicks by mutableStateOf(0)

        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = fontScale.floatValue),
            ) {
                Box(Modifier.requiredSize(width = width.value, height = 720.dp)) {
                    PlayroomHomeScreen(
                        state = stateForContract(),
                        onSubjectClick = {},
                        onQuestAction = {},
                        onHomeClick = {},
                        onCollectionClick = { collectionClicks++ },
                        onParentsClick = { parentsClicks++ },
                    )
                }
            }
        }

        listOf(360.dp, 840.dp, 1100.dp).forEach { widthClassWidth ->
            listOf(1f, 1.3f).forEach { scale ->
                composeRule.runOnIdle {
                    width.value = widthClassWidth
                    fontScale.floatValue = scale
                }
                composeRule.waitForIdle()

                composeRule.onNodeWithTag(PlayroomHomeTestTags.TodayQuest).assertExists()
                composeRule.onNodeWithTag(PlayroomHomeTestTags.SelectedNavigation).assertIsSelected()
                composeRule.onNodeWithTag(PlayroomHomeTestTags.Collection).assertHasClickAction().performClick()
                composeRule.onNodeWithTag(PlayroomHomeTestTags.Parents).assertHasClickAction().performClick()
            }
        }

        composeRule.runOnIdle {
            assertEquals(6, collectionClicks)
            assertEquals(6, parentsClicks)
        }
    }

    @Test
    fun streakProgressAnchorIsPresentWithConcreteQuestProgress() {
        setHome()

        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak, useUnmergedTree = true)
            .assertExists()
            .assertHasClickAction()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.TodayQuest).assertExists()
        composeRule.onNodeWithText("2 days learning").assertExists()
        composeRule.onNodeWithContentDescription(
            "2 days learning. Each day you learn counts toward your learning days.",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun todayQuestPrecedesStreakCardInChildHomeSemantics() {
        setHome()

        val todayQuestBounds = composeRule.onNodeWithTag(PlayroomHomeTestTags.TodayQuest)
            .fetchSemanticsNode()
            .boundsInRoot
        val streakBounds = composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "Today’s Quest must precede the learning-day streak card",
            todayQuestBounds.top < streakBounds.top,
        )
    }
}
