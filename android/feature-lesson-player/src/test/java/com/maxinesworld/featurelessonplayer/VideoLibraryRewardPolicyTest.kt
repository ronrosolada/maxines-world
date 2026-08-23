package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.MediaAssessment
import com.maxinesworld.coremodel.MediaAssessmentItem
import com.maxinesworld.coremodel.MediaAssessmentOption
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.corenetwork.MediaCatalog
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.featurerewards.BadgeLoader
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoLibraryRewardPolicyTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mediaLibrary = mockk<MediaLibrary>()
    private val videoWatchLedgerDao = mockk<VideoWatchLedgerDao>(relaxed = true)
    private val rewardDao = mockk<RewardDao>(relaxed = true)
    private val collectedBadgeDao = mockk<CollectedBadgeDao>(relaxed = true)
    private val badgeLoader = mockk<BadgeLoader>()

    private val testAsset = MediaAsset(
        mediaId = "test-video-1",
        title = "Test Video",
        subjectId = "science",
        episodeNumber = 1,
        durationSeconds = 1800,
        assessment = MediaAssessment(
            mediaId = "test-video-1",
            passingCorrectCount = 4,
            items = (1..5).map { seq ->
                MediaAssessmentItem(
                    itemId = "q$seq",
                    sequence = seq,
                    prompt = "Question $seq",
                    options = listOf(
                        MediaAssessmentOption("a", "Option A"),
                        MediaAssessmentOption("b", "Option B"),
                        MediaAssessmentOption("c", "Option C"),
                        MediaAssessmentOption("d", "Option D"),
                    ),
                    correctOptionIds = listOf("a"),
                    explanation = "Explanation",
                )
            }
        )
    )

    private val sampleBadges = listOf(
        CollectibleBadge(id = "badge_tarsier", name = "Philippine Tarsier", biome = "rainforest"),
        CollectibleBadge(id = "badge_eagle", name = "Philippine Eagle", biome = "mountain"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mediaLibrary.refreshCatalog() } returns MediaCatalog(schemaVersion = 1, media = listOf(testAsset))
        coEvery { mediaLibrary.getCatalog() } returns MediaCatalog(schemaVersion = 1, media = listOf(testAsset))
        coEvery { badgeLoader.loadAll() } returns sampleBadges
        coEvery { collectedBadgeDao.getAllByChild(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): VideoLibraryViewModel {
        return VideoLibraryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to "maxine")),
            mediaLibrary = mediaLibrary,
            videoWatchLedgerDao = videoWatchLedgerDao,
            rewardDao = rewardDao,
            collectedBadgeDao = collectedBadgeDao,
            badgeLoader = badgeLoader,
        )
    }

    @Test
    fun `first time video assessment pass awards exactly 1 sticker for 1800 seconds`() = runTest(testDispatcher) {
        coEvery { videoWatchLedgerDao.getEntry("maxine", "test-video-1") } returns null
        coEvery { videoWatchLedgerDao.getTotalAccreditedSeconds("maxine") } returnsMany listOf(0, 1800)

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.openAssessment("test-video-1")
        // Answer all 5 questions correctly
        repeat(5) {
            vm.selectAssessmentOption("a")
            vm.checkAssessmentAnswer()
            vm.nextAssessmentQuestion()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { collectedBadgeDao.insert(match { it.badgeId == "badge_tarsier" }) }
        assertEquals("Philippine Tarsier", vm.state.value.newlyAwardedStickerName)
    }

    @Test
    fun `replaying same video assessment never awards extra stickers or watch time`() = runTest(testDispatcher) {
        val existingEntry = VideoWatchLedgerEntity(
            id = "maxine_test-video-1",
            childId = "maxine",
            mediaId = "test-video-1",
            subjectId = "science",
            accreditedSeconds = 1800,
            quizPassed = true,
            bestQuizScore = 1.0f,
            firstPassedAtEpochMillis = 1000L,
            lastWatchedAtEpochMillis = 1000L,
        )
        coEvery { videoWatchLedgerDao.getEntry("maxine", "test-video-1") } returns existingEntry
        coEvery { videoWatchLedgerDao.getTotalAccreditedSeconds("maxine") } returns 1800

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.openAssessment("test-video-1")
        repeat(5) {
            vm.selectAssessmentOption("a")
            vm.checkAssessmentAnswer()
            vm.nextAssessmentQuestion()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify zero badge inserts and zero newly awarded sticker
        coVerify(exactly = 0) { collectedBadgeDao.insert(any()) }
        assertNull(vm.state.value.newlyAwardedStickerName)
    }
}
