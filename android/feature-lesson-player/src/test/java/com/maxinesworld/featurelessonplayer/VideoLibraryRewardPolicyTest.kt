package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaAssessment
import com.maxinesworld.coremodel.MediaAssessmentItem
import com.maxinesworld.coremodel.MediaAssessmentOption
import com.maxinesworld.coremodel.MediaCatalog
import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.featurerewards.BadgeLoader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
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

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    private val mediaLibrary = mockk<MediaLibrary>()
    private val videoWatchLedgerDao = mockk<VideoWatchLedgerDao>(relaxed = true)
    private val rewardDao = mockk<RewardDao>(relaxed = true)
    private val collectedBadgeDao = mockk<CollectedBadgeDao>(relaxed = true)
    private val badgeLoader = mockk<BadgeLoader>()

    private val testAsset = MediaAsset(
        mediaId = "test-video-1",
        title = "Test Video",
        file = "media/test.mp4",
        sha256 = "dummy",
        sizeBytes = 1000L,
        durationSeconds = 1800,
        width = 1280,
        height = 720,
        subjectId = "science",
        episodeNumber = 1,
        gradeLevel = 3,
        releaseStatus = "RELEASED",
        assessment = MediaAssessment(
            questionCount = 5,
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
        CollectibleBadge(id = "badge_tarsier", biome = "rainforest", name = "Philippine Tarsier", title = "Tarsier", funFact = "Small primate"),
        CollectibleBadge(id = "badge_eagle", biome = "mountain", name = "Philippine Eagle", title = "Eagle", funFact = "Great bird"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mediaLibrary.refreshCatalog() } returns MediaCatalog(catalogVersion = 1, generatedAt = "2026-08-23", media = listOf(testAsset))
        coEvery { mediaLibrary.getCatalog() } returns MediaCatalog(catalogVersion = 1, generatedAt = "2026-08-23", media = listOf(testAsset))
        every { mediaLibrary.getCachedCatalog() } returns MediaCatalog(catalogVersion = 1, generatedAt = "2026-08-23", media = listOf(testAsset))
        coEvery { collectedBadgeDao.insert(any()) } returns Unit
        coEvery { rewardDao.insertIgnoring(any()) } returns 1L
        coEvery { videoWatchLedgerDao.insertOrUpdate(any()) } returns Unit
        every { mediaLibrary.localFile(any()) } returns java.io.File("/tmp/test.mp4")
        every { videoWatchLedgerDao.observePassedMediaIds(any()) } returns flowOf(emptyList())
        coEvery { videoWatchLedgerDao.getAllByChild(any()) } returns emptyList()
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
        coEvery { videoWatchLedgerDao.claimFirstPassingAssessment("maxine", "test-video-1", any(), any()) } returns 1
        coEvery { videoWatchLedgerDao.getAllByChild("maxine") } returns emptyList()
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

    @Test
    fun `first pass reward is not minted for preview media`() = runTest(testDispatcher) {
        val previewAsset = testAsset.copy(mediaId = "preview-video", releaseStatus = "PREVIEW")
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val credited = vm.creditFirstPassingAssessment(previewAsset, score = 1.0f)

        assertEquals(false, credited)
        coVerify(exactly = 0) { videoWatchLedgerDao.claimFirstPassingAssessment(any(), any(), any(), any()) }
        coVerify(exactly = 0) { videoWatchLedgerDao.insertIgnoring(any()) }
        coVerify(exactly = 0) { rewardDao.insertIgnoring(any()) }
        coVerify(exactly = 0) { collectedBadgeDao.insert(any()) }
        assertNull(vm.state.value.newlyAwardedStickerName)
    }

    @Test
    fun `first pass reward is not minted for other-grade media`() = runTest(testDispatcher) {
        val gradeOne = testAsset.copy(mediaId = "g1-video", gradeLevel = 1)
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val credited = vm.creditFirstPassingAssessment(gradeOne, score = 1.0f)

        assertEquals(false, credited)
        coVerify(exactly = 0) { videoWatchLedgerDao.claimFirstPassingAssessment(any(), any(), any(), any()) }
        coVerify(exactly = 0) { rewardDao.insertIgnoring(any()) }
    }

    @Test
    fun `preview ledger time does not count toward sticker thresholds`() = runTest(testDispatcher) {
        coEvery { videoWatchLedgerDao.getEntry("maxine", "test-video-1") } returns null
        coEvery { videoWatchLedgerDao.claimFirstPassingAssessment("maxine", "test-video-1", any(), any()) } returns 1
        coEvery { videoWatchLedgerDao.getAllByChild("maxine") } returns listOf(
            VideoWatchLedgerEntity(
                id = "maxine_preview-video",
                childId = "maxine",
                mediaId = "preview-video",
                subjectId = "science",
                accreditedSeconds = 1800,
                quizPassed = true,
                bestQuizScore = 1.0f,
                firstPassedAtEpochMillis = 1L,
                lastWatchedAtEpochMillis = 1L,
            )
        )

        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.openAssessment("test-video-1")
        repeat(5) {
            vm.selectAssessmentOption("a")
            vm.checkAssessmentAnswer()
            vm.nextAssessmentQuestion()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // PREVIEW's 1800s must not mint a sticker; this Grade 3 video is also 1800s
        // and is the first child-facing credit, so exactly one sticker is awarded.
        coVerify(exactly = 1) { collectedBadgeDao.insert(match { it.badgeId == "badge_tarsier" }) }
        coVerify(exactly = 1) { rewardDao.insertIgnoring(match { it.metadata == "video_assessment_passed:test-video-1" }) }
    }
}
