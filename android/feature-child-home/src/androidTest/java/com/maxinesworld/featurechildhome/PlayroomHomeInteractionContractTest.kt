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

    private fun stateForContract(): PlayroomHomeUiState.Content =
        PlayroomHomeUiState.Content(
            childName = "Maxine",
            subjects = canonicalSubjects.mapIndexed { index, subject ->
                subject.copy(progressPercent = when (index) {
                    0 -> null
                    1 -> 42
                    else -> 100
                })
            },
            quest = QuestUi(
                task = QuestTaskCopy.IncompleteToday,
                pawPrintsCompleted = 2,
                pawPrintTotal = 3,
                recommendedSubjectId = canonicalSubjects.first().id,
                buttonLabel = QuestButtonLabel.Continue,
            ),
            wildlifeStickers = WildlifeStickersUi(collectedCount = 2, totalCount = 12),
        )

    private fun setHome(
        width: Dp = 411.dp,
        fontScale: Float = 1f,
        onCollectionClick: () -> Unit = {},
        onParentsClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = fontScale),
            ) {
                Box(Modifier.requiredSize(width = width, height = 720.dp)) {
                    PlayroomHomeScreen(
                        state = stateForContract(),
                        onSubjectClick = {},
                        onQuestAction = {},
                        onHomeClick = {},
                        onCollectionClick = onCollectionClick,
                        onParentsClick = onParentsClick,
                    )
                }
            }
        }
    }

    @Test
    fun todayQuestExposesExactlyOnePrimaryAction() {
        setHome()

        composeRule.onNodeWithTag(PlayroomHomeTestTags.TodayQuest).assertExists()
        composeRule.onNodeWithText("Continue").assertHasClickAction()
        composeRule.onAllNodesWithText("Continue").assertCountEquals(1)
    }

    @Test
    fun eachSubjectExposesStableLabelAndProgressState() {
        setHome()

        canonicalSubjects.forEachIndexed { index, subject ->
            val expectedProgress = when (index) {
                0 -> "Not started"
                1 -> "42% complete"
                else -> "Complete"
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

        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak).assertExists()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.TodayQuest).assertExists()
        composeRule.onNodeWithText("2 of 3").assertExists()
    }
}
