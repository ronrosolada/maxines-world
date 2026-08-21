package com.maxinesworld.featurechildhome

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** UI coverage for the Playroom home and the immediate GMRC/reward loop. */
class PlayroomHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun stateFor(
        completed: Int = 0,
        task: QuestTaskCopy = QuestTaskCopy.IncompleteToday,
        isComplete: Boolean = false,
        buttonLabel: QuestButtonLabel = QuestButtonLabel.Continue,
        buttonAction: QuestAction = QuestAction.Continue,
        godModeEnabled: Boolean = false,
        playgroundUnlocked: Boolean = false,
        streakDays: Int = 0,
        targets: List<QuestTargetUi> = emptyList(),
        subjects: List<SubjectCardUi> = canonicalSubjects,
    ): PlayroomHomeUiState.Content =
        PlayroomHomeUiState.Content(
            childName = "Maxine",
            subjects = subjects,
            quest = QuestUi(
                task = task,
                pawPrintsCompleted = completed,
                pawPrintTotal = 3,
                isComplete = isComplete,
                recommendedSubjectId = canonicalSubjects.first().id,
                buttonLabel = buttonLabel,
                buttonAction = buttonAction,
                godModeEnabled = godModeEnabled,
                playgroundUnlocked = playgroundUnlocked,
                targets = targets,
            ),
            wildlifeStickers = WildlifeStickersUi(collectedCount = 0, totalCount = 0),
            streakDays = streakDays,
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
        onQuestAction: (QuestAction) -> Unit = {},
        onParentsClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            PlayroomHomeScreen(
                state = state,
                onSubjectClick = onSubjectClick,
                onQuestAction = onQuestAction,
                onQuestTargetClick = {},
                onHomeClick = {},
                onCollectionClick = onCollectionClick,
                onTreatShopClick = onTreatShopClick,
                onParentsClick = onParentsClick,
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
        composeRule.onAllNodesWithTag(PlayroomHomeTestTags.Streak).assertCountEquals(0)
    }

    @Test
    fun errorStateDoesNotShowFabricatedStreakCard() {
        setHome(PlayroomHomeUiState.Error("We couldn't load your Playroom."))
        composeRule.onNodeWithText("We couldn't load your Playroom.").assertIsDisplayed()
        composeRule.onAllNodesWithTag(PlayroomHomeTestTags.Streak).assertCountEquals(0)
    }

    @Test
    fun gmrcIsDisplayedAndEnabledFromFirstSession() {
        setHome(stateFor())
        scrollTo("GMRC")
        composeRule.onNodeWithText("GMRC").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun allSixCanonicalSubjectsRender() {
        val lockedSubject = canonicalSubjects.first().copy(
            availability = SubjectAvailability.Locked,
            lockReason = "Complete 3 videos",
            completedVideos = 1,
            totalVideos = 5,
        )
        setHome(stateFor(subjects = listOf(lockedSubject) + canonicalSubjects.drop(1)))
        // Makabansa is the Matatag successor of Araling Panlipunan — legacy
        // AP lessons ship inside the Makabansa collection (2026-08-06 merge).
        // The grid is a plain Column: every card exists in the tree even when
        // off-screen, so existence proves all six render; scroll to verify the
        // bottom of the grid is actually reachable and displayed.
        scrollTo("GMRC")
        composeRule.onNodeWithText("GMRC").assertIsDisplayed()
        listOf("Mathematics", "English", "Science", "Filipino", "Makabansa", "GMRC")
            .forEach { composeRule.onNodeWithText(it).assertExists() }

        scrollTo("Mathematics")
        val lockedCard = composeRule.onNodeWithTag(PlayroomHomeTestTags.subject("mathematics"))
        lockedCard.assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Mathematics, Number Fun. Locked. Complete 3 videos."),
                ),
            )
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "1 of 5 videos",
                ),
            )
        composeRule.onNodeWithText("Why it’s locked: Complete 3 videos").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 5 videos").assertIsDisplayed()
    }

    @Test
    fun subjectHeadingAndWholeCardTargetAreDiscoverable() {
        var openedSubject: String? = null
        setHome(stateFor(), onSubjectClick = { openedSubject = it })

        composeRule.onNodeWithText("Explore subjects")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.SubjectGrid).assertExists()

        composeRule.onNodeWithTag(PlayroomHomeTestTags.subject("gmrc"))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals("gmrc", openedSubject) }
    }

    @Test
    fun finalSubjectAndBottomNavigationRemainReachableAtResponsiveWidthsAndLargeFont() {
        val width = mutableStateOf(360.dp)
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = fontScale.floatValue),
            ) {
                Box(Modifier.requiredSize(width = width.value, height = 720.dp)) {
                    PlayroomHomeScreen(
                        state = stateFor(),
                        onSubjectClick = {},
                        onQuestAction = {},
                        onQuestTargetClick = {},
                        onHomeClick = {},
                        onCollectionClick = {},
                        onParentsClick = {},
                    )
                }
            }
        }

        listOf(360.dp, 840.dp, 1100.dp).forEach { responsiveWidth ->
            listOf(1f, 1.3f).forEach { responsiveFontScale ->
                composeRule.runOnIdle {
                    width.value = responsiveWidth
                    fontScale.floatValue = responsiveFontScale
                }
                composeRule.waitForIdle()

                // The grid is vertical, so reachability is provided by the
                // existing home scroll rather than an invisible horizontal
                // gesture or a clipped map row.
                composeRule.onNodeWithTag(PlayroomHomeTestTags.SubjectsHeading)
                    .performScrollTo()
                    .assertIsDisplayed()
                composeRule.onNodeWithTag(PlayroomHomeTestTags.subject("gmrc"))
                    .performScrollTo()
                    .assertIsDisplayed()
                composeRule.onNodeWithTag(PlayroomHomeTestTags.Parents).assertIsDisplayed()
            }
        }
    }

    @Test
    fun compactLargeFontUsesFullWidthSubjectCardAndDisplaysBothSubjectLabels() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 1.3f),
            ) {
                Box(Modifier.requiredSize(width = 360.dp, height = 720.dp)) {
                    PlayroomHomeScreen(
                        state = stateFor(),
                        onSubjectClick = {},
                        onQuestAction = {},
                        onQuestTargetClick = {},
                        onHomeClick = {},
                        onCollectionClick = {},
                        onParentsClick = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Mathematics", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Number Fun", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        val gridBounds = composeRule.onNodeWithTag(PlayroomHomeTestTags.SubjectGrid)
            .fetchSemanticsNode()
            .boundsInRoot
        val cardBounds = composeRule.onNodeWithTag(PlayroomHomeTestTags.subject("mathematics"))
            .fetchSemanticsNode()
            .boundsInRoot
        assertEquals(gridBounds.left, cardBounds.left, 0.5f)
        assertEquals(gridBounds.right, cardBounds.right, 0.5f)
    }

    @Test
    fun todaysQuestCopyRenders() {
        setHome(stateFor(2))
        scrollTo("Today’s mission")
        composeRule.onNodeWithText("Today’s mission").assertIsDisplayed()
        composeRule.onNodeWithText("Complete 3 learning adventures today.").assertIsDisplayed()
        composeRule.onNodeWithText("2 of 3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Today's mission. 2 of 3 complete", useUnmergedTree = true)
            .assertHasClickAction()
        composeRule.onNodeWithText("Wildlife Stickers").assertExists()
        composeRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun dailyQuestShowsItsRewardBeforeCompletionAndSanctuaryProgress() {
        setHome(stateFor())
        composeRule.onNodeWithText("Reward after all 3 adventures: a sanctuary piece + 5-minute play break").assertExists()
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
        composeRule.onAllNodesWithText("Finish all 3 lessons in Today’s mission to add this place.").assertCountEquals(0)
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
    fun questPresentationCoversCompletePlaygroundParentAndNoTargetStates() {
        val state = mutableStateOf<PlayroomHomeUiState>(stateFor())
        composeRule.setContent {
            PlayroomHomeScreen(
                state = state.value,
                onSubjectClick = {},
                onQuestAction = {},
                onQuestTargetClick = {},
                onHomeClick = {},
                onCollectionClick = {},
                onParentsClick = {},
            )
        }
        composeRule.onNodeWithTag(PlayroomHomeTestTags.TodayQuest).assertExists()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak, useUnmergedTree = true).assertExists()
        canonicalSubjects.forEach { subject ->
            composeRule.onNodeWithTag(PlayroomHomeTestTags.subject(subject.id)).assertExists()
        }
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Collection).assertHasClickAction()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Parents).assertHasClickAction()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.SelectedNavigation).assertIsSelected()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Collection).assertIsNotSelected()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Parents).assertIsNotSelected()

        // No-target state: the card remains actionable without inventing a
        // target row; the visible action is still the quest state-machine
        // action supplied by the caller.
        composeRule.onNodeWithContentDescription("Today's mission. 0 of 3 complete", useUnmergedTree = true)
            .assertHasClickAction()

        composeRule.runOnIdle {
            state.value = stateFor(
                completed = 3,
                task = QuestTaskCopy.CompleteToday,
                isComplete = true,
                buttonLabel = QuestButtonLabel.OpenSanctuary,
                buttonAction = QuestAction.ViewReward,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Today’s mission is complete — Milo’s sanctuary is growing!").assertExists()
        composeRule.onNodeWithText("Open Sanctuary").assertExists()
        composeRule.onNodeWithText("3 of 3").assertExists()

        composeRule.runOnIdle {
            state.value = stateFor(
                completed = 3,
                task = QuestTaskCopy.CompleteToday,
                isComplete = true,
                buttonLabel = QuestButtonLabel.OpenPlayground,
                buttonAction = QuestAction.OpenPlayground,
                playgroundUnlocked = true,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Open Playground").assertExists()
        composeRule.onNodeWithContentDescription("Today's mission. 3 of 3 complete", useUnmergedTree = true)
            .assertHasClickAction()

        composeRule.runOnIdle {
            state.value = stateFor(
                task = QuestTaskCopy.ParentMode,
                buttonLabel = QuestButtonLabel.OpenPlayground,
                buttonAction = QuestAction.OpenPlayground,
                godModeEnabled = true,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Parent mode: the Playground and all rewards are unlocked!").assertExists()
        composeRule.onNodeWithText("Open Playground").assertExists()
    }

    @Test
    fun primaryActionsHaveTalkBackLabels() {
        setHome(stateFor())
        listOf("Sanctuary Workshop", "Collection", "Parents, locked").forEach { label ->
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

    @Test
    fun learningStreakZeroStateExplainsWhatToDo() {
        setHome(stateFor(streakDays = 0))

        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak)
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText("Start your learning streak today").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "No learning days yet. Start your learning streak today. Each day you learn counts toward your learning days.",
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun learningStreakPositiveStateShowsNumberAndMeaning() {
        setHome(stateFor(streakDays = 7))

        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak)
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText("7 days learning").assertIsDisplayed()
        composeRule.onNodeWithText("Each day you learn counts toward your learning days.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "7 days learning. Each day you learn counts toward your learning days.",
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun learningStreakTapOpensChildExplanationNotParentDashboard() {
        var parentClicks = 0
        setHome(stateFor(streakDays = 7), onParentsClick = { parentClicks++ })

        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak).performClick()
        composeRule.onNodeWithText("Your learning days").assertIsDisplayed()
        composeRule.onNodeWithText("Got it").performClick()
        composeRule.runOnIdle { assertEquals(0, parentClicks) }
    }

    @Test
    fun learningStreakDialogUsesSingularCopyForOneDay() {
        setHome(stateFor(streakDays = 1))

        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak).performClick()
        composeRule.onNodeWithText("You have learned on 1 day in a row. Learning today keeps your learning days going.")
            .assertIsDisplayed()
    }

    @Test
    fun learningStreakReducedMotionStillRendersWithoutCelebrationRequirement() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimationsDisabled provides true) {
                PlayroomHomeScreen(
                    state = stateFor(streakDays = 7),
                    onSubjectClick = {},
                    onQuestAction = {},
                    onQuestTargetClick = {},
                    onHomeClick = {},
                    onCollectionClick = {},
                    onParentsClick = {},
                )
            }
        }

        composeRule.onNodeWithText("7 days learning").assertIsDisplayed()
        composeRule.onNodeWithTag(PlayroomHomeTestTags.Streak).assertHasClickAction()
    }
}
