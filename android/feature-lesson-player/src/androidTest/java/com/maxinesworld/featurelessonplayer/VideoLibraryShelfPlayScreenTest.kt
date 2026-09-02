package com.maxinesworld.featurelessonplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import com.maxinesworld.coremodel.MediaAsset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoLibraryShelfPlayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun undownloadedUnlockedWatchShowsGettingReadyNotADeadTap() {
        var playCalls = 0

        composeRule.setContent {
            MaxinesWorldTheme {
                var state by remember {
                    mutableStateOf(
                        VideoLibraryUiState(
                            isLoading = false,
                            upcomingItems = listOf(unlockedUndownloadedItem()),
                        ),
                    )
                }
                VideoLibraryContent(
                    state = state,
                    onDownloadAll = {},
                    onPlay = { mediaId ->
                        playCalls++
                        state = state.copy(
                            assignedPlayMessage = VideoLibraryAssignedPlayCopy.GETTING_READY,
                            upcomingItems = state.upcomingItems.map {
                                if (it.asset.mediaId == mediaId) {
                                    it.copy(isDownloading = true, error = null)
                                } else {
                                    it
                                }
                            },
                        )
                    },
                    onStopPlaying = {},
                    onVideoCompleted = {},
                    onOpenAssessment = {},
                    onSelectAssessmentOption = {},
                    onCheckAssessmentAnswer = {},
                    onNextAssessmentQuestion = {},
                    onCloseAssessment = {},
                    onRestartAssessment = {},
                    onWatchAgain = {},
                    onRetry = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Place value").assertIsDisplayed()
        composeRule.onNodeWithText("Download").assertDoesNotExist()
        composeRule.onNodeWithText("Complete the previous lesson first").assertDoesNotExist()
        composeRule.onNodeWithTag(VideoLibraryTestTags.playButton("math-g3-01"))
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText(VideoLibraryAssignedPlayCopy.GETTING_READY).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, playCalls)
        }
    }

    @Test
    fun lockedLessonKeepsHonestLockCopyAndHasNoPlayTap() {
        composeRule.setContent {
            MaxinesWorldTheme {
                VideoLibraryContent(
                    state = VideoLibraryUiState(
                        isLoading = false,
                        upcomingItems = listOf(lockedItem()),
                    ),
                    onDownloadAll = {},
                    onPlay = {},
                    onStopPlaying = {},
                    onVideoCompleted = {},
                    onOpenAssessment = {},
                    onSelectAssessmentOption = {},
                    onCheckAssessmentAnswer = {},
                    onNextAssessmentQuestion = {},
                    onCloseAssessment = {},
                    onRestartAssessment = {},
                    onWatchAgain = {},
                    onRetry = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Complete the previous lesson first").assertIsDisplayed()
        composeRule.onNodeWithTag(VideoLibraryTestTags.playButton("math-g3-02")).assertDoesNotExist()
        composeRule.onNodeWithText("Watch").assertDoesNotExist()
        composeRule.onNodeWithText("Download").assertDoesNotExist()
    }
}

private fun unlockedUndownloadedItem() = VideoLibraryItemUi(
    asset = MediaAsset(
        mediaId = "math-g3-01",
        title = "Place value",
        file = "media/math-g3-01.mp4",
        sha256 = "a".repeat(64),
        sizeBytes = 1L,
        durationSeconds = 60,
        width = 1,
        height = 1,
        subjectId = "mathematics",
        gradeLevel = 3,
        episodeNumber = 1,
        releaseStatus = "RELEASED",
    ),
    localPath = null,
    isLocked = false,
)

private fun lockedItem() = VideoLibraryItemUi(
    asset = MediaAsset(
        mediaId = "math-g3-02",
        title = "Regrouping",
        file = "media/math-g3-02.mp4",
        sha256 = "a".repeat(64),
        sizeBytes = 1L,
        durationSeconds = 60,
        width = 1,
        height = 1,
        subjectId = "mathematics",
        gradeLevel = 3,
        episodeNumber = 2,
        releaseStatus = "RELEASED",
    ),
    localPath = null,
    isLocked = true,
)
