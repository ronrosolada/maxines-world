package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaAssessment
import com.maxinesworld.coremodel.MediaAssessmentItem
import com.maxinesworld.coremodel.MediaAssessmentOption
import com.maxinesworld.coremodel.MediaCatalog
import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.featurerewards.BadgeLoader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoLibraryPlayTest {

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    private val mediaLibrary = mockk<MediaLibrary>()
    private val videoWatchLedgerDao = mockk<VideoWatchLedgerDao>(relaxed = true)
    private val rewardDao = mockk<RewardDao>(relaxed = true)
    private val collectedBadgeDao = mockk<CollectedBadgeDao>(relaxed = true)
    private val badgeLoader = mockk<BadgeLoader>(relaxed = true)

    private val firstLesson = asset(
        mediaId = "math-g3-01",
        title = "Place value",
        episodeNumber = 1,
    )
    private val lockedLesson = asset(
        mediaId = "math-g3-02",
        title = "Regrouping",
        episodeNumber = 2,
    )
    private val previewLesson = asset(
        mediaId = "math-g3-preview",
        title = "Preview only",
        episodeNumber = 3,
        releaseStatus = "PREVIEW",
    )
    private val otherGrade = asset(
        mediaId = "math-g1-01",
        title = "Grade 1",
        episodeNumber = 1,
        gradeLevel = 1,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { videoWatchLedgerDao.observePassedMediaIds(any()) } returns flowOf(emptyList())
        coEvery { badgeLoader.loadAll() } returns emptyList()
        val catalog = MediaCatalog(
            catalogVersion = 1,
            generatedAt = "test",
            media = listOf(firstLesson, lockedLesson, previewLesson, otherGrade),
        )
        every { mediaLibrary.getCachedCatalog() } returns catalog
        coEvery { mediaLibrary.getCatalog() } returns catalog
        coEvery { mediaLibrary.refreshCatalog() } returns catalog
        every { mediaLibrary.localFile(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unlocked undownloaded play downloads child-facing then plays`() = runTest(testDispatcher) {
        val downloaded = File("/tmp/math-g3-01.mp4")
        coEvery { mediaLibrary.downloadChildFacing("math-g3-01") } returns downloaded

        val vm = createViewModel()
        advanceUntilIdle()

        vm.play(firstLesson.mediaId)
        assertEquals(VideoLibraryAssignedPlayCopy.GETTING_READY, vm.state.value.assignedPlayMessage)
        assertTrue(vm.state.value.allItems.first { it.asset.mediaId == firstLesson.mediaId }.isDownloading)

        advanceUntilIdle()

        assertEquals(firstLesson.mediaId, vm.state.value.playingMediaId)
        assertNull(vm.state.value.assignedPlayMessage)
        assertFalse(vm.state.value.allItems.first { it.asset.mediaId == firstLesson.mediaId }.isDownloading)
        assertEquals(
            downloaded.absolutePath,
            vm.state.value.allItems.first { it.asset.mediaId == firstLesson.mediaId }.localPath,
        )
        coVerify(exactly = 1) { mediaLibrary.downloadChildFacing("math-g3-01") }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `already-local play starts immediately and does not download`() = runTest(testDispatcher) {
        every { mediaLibrary.localFile("math-g3-01") } returns File("/tmp/math-g3-01.mp4")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.play(firstLesson.mediaId)

        assertEquals(firstLesson.mediaId, vm.state.value.playingMediaId)
        assertNull(vm.state.value.assignedPlayMessage)
        coVerify(exactly = 0) { mediaLibrary.downloadChildFacing(any()) }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `locked play does not download or start`() = runTest(testDispatcher) {
        every { mediaLibrary.localFile("math-g3-02") } returns File("/tmp/math-g3-02.mp4")

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.allItems.first { it.asset.mediaId == lockedLesson.mediaId }.isLocked)
        vm.play(lockedLesson.mediaId)
        advanceUntilIdle()

        assertNull(vm.state.value.playingMediaId)
        assertNull(vm.state.value.assignedPlayMessage)
        coVerify(exactly = 0) { mediaLibrary.downloadChildFacing(any()) }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `preview play is refused and never downloaded`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.allItems.none { it.asset.mediaId == previewLesson.mediaId })
        vm.play(previewLesson.mediaId)
        advanceUntilIdle()

        assertNull(vm.state.value.playingMediaId)
        coVerify(exactly = 0) { mediaLibrary.downloadChildFacing(any()) }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `other-grade play is refused and never downloaded`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.allItems.none { it.asset.mediaId == otherGrade.mediaId })
        vm.play(otherGrade.mediaId)
        advanceUntilIdle()

        assertNull(vm.state.value.playingMediaId)
        coVerify(exactly = 0) { mediaLibrary.downloadChildFacing(any()) }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `failed prepare leaves honest error and never hangs on getting ready`() = runTest(testDispatcher) {
        coEvery { mediaLibrary.downloadChildFacing("math-g3-01") } throws IOException("The wifi is sleeping")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.play(firstLesson.mediaId)
        assertEquals(VideoLibraryAssignedPlayCopy.GETTING_READY, vm.state.value.assignedPlayMessage)
        advanceUntilIdle()

        val item = vm.state.value.allItems.first { it.asset.mediaId == firstLesson.mediaId }
        assertNull(vm.state.value.playingMediaId)
        assertEquals("The wifi is sleeping", vm.state.value.assignedPlayMessage)
        assertEquals("The wifi is sleeping", item.error)
        assertFalse(item.isDownloading)
        assertNull(item.localPath)
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `rewatch after fail with missing file prepares then plays`() = runTest(testDispatcher) {
        val downloaded = File("/tmp/math-g3-01.mp4")
        coEvery { mediaLibrary.downloadChildFacing("math-g3-01") } returns downloaded

        val vm = createViewModel()
        advanceUntilIdle()
        finishQuiz(vm, mediaId = firstLesson.mediaId, correct = false)

        assertTrue(vm.state.value.assessmentQuiz?.finished == true)
        assertNull(vm.state.value.playingMediaId)

        vm.rewatchAfterFailedAssessment()
        assertNull(vm.state.value.assessmentQuiz)
        assertEquals(VideoLibraryAssignedPlayCopy.GETTING_READY, vm.state.value.assignedPlayMessage)

        advanceUntilIdle()

        assertEquals(firstLesson.mediaId, vm.state.value.playingMediaId)
        assertNull(vm.state.value.assignedPlayMessage)
        assertFalse(firstLesson.mediaId in vm.state.value.passedMediaIds)
        coVerify(exactly = 1) { mediaLibrary.downloadChildFacing("math-g3-01") }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    private fun createViewModel() = VideoLibraryViewModel(
        savedStateHandle = SavedStateHandle(mapOf("childId" to "maxine")),
        mediaLibrary = mediaLibrary,
        videoWatchLedgerDao = videoWatchLedgerDao,
        rewardDao = rewardDao,
        collectedBadgeDao = collectedBadgeDao,
        badgeLoader = badgeLoader,
    )

    private fun finishQuiz(vm: VideoLibraryViewModel, mediaId: String, correct: Boolean) {
        vm.openAssessment(mediaId)
        val itemCount = vm.state.value.allItems
            .first { it.asset.mediaId == mediaId }
            .asset.assessment!!.items.size
        repeat(itemCount) {
            vm.selectAssessmentOption(if (correct) "a" else "b")
            vm.checkAssessmentAnswer()
            vm.nextAssessmentQuestion()
        }
    }

    private fun asset(
        mediaId: String,
        title: String,
        episodeNumber: Int,
        releaseStatus: String = "RELEASED",
        gradeLevel: Int = 3,
    ) = MediaAsset(
        mediaId = mediaId,
        title = title,
        file = "media/$mediaId.mp4",
        sha256 = "a".repeat(64),
        sizeBytes = 1L,
        durationSeconds = 60,
        width = 1,
        height = 1,
        subjectId = "mathematics",
        gradeLevel = gradeLevel,
        episodeNumber = episodeNumber,
        releaseStatus = releaseStatus,
        assessment = MediaAssessment(
            questionCount = 2,
            passingCorrectCount = 2,
            items = listOf(
                item(mediaId, 1, "a"),
                item(mediaId, 2, "a"),
            ),
        ),
    )

    private fun item(mediaId: String, sequence: Int, correct: String) = MediaAssessmentItem(
        itemId = "$mediaId-q$sequence",
        sequence = sequence,
        prompt = "Question $sequence",
        options = listOf(
            MediaAssessmentOption("a", "Answer A"),
            MediaAssessmentOption("b", "Answer B"),
            MediaAssessmentOption("c", "Answer C"),
            MediaAssessmentOption("d", "Answer D"),
        ),
        correctOptionIds = listOf(correct),
        explanation = "Because this is the video clue.",
    )
}
