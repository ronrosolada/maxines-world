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
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VideoLibraryRewatchTest {

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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { videoWatchLedgerDao.observePassedMediaIds(any()) } returns flowOf(emptyList())
        every { mediaLibrary.localFile(any()) } returns File("/tmp/lesson.mp4")
        coEvery { badgeLoader.loadAll() } returns emptyList()
        val catalog = MediaCatalog(
            catalogVersion = 1,
            generatedAt = "test",
            media = listOf(firstLesson, lockedLesson),
        )
        every { mediaLibrary.getCachedCatalog() } returns catalog
        coEvery { mediaLibrary.getCatalog() } returns catalog
        coEvery { mediaLibrary.refreshCatalog() } returns catalog
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fail-summary rewatch plays the same video and does not restart the quiz`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()
        finishQuiz(vm, mediaId = firstLesson.mediaId, correct = false)

        assertTrue(vm.state.value.assessmentQuiz?.finished == true)
        assertNull(vm.state.value.playingMediaId)

        vm.rewatchAfterFailedAssessment()

        assertNull(vm.state.value.assessmentQuiz)
        assertEquals(firstLesson.mediaId, vm.state.value.playingMediaId)
        assertFalse(firstLesson.mediaId in vm.state.value.passedMediaIds)
    }

    @Test
    fun `fail-summary rewatch does not play a locked video`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.allItems.first { it.asset.mediaId == lockedLesson.mediaId }.isLocked)
        vm.play(lockedLesson.mediaId)
        assertNull(vm.state.value.playingMediaId)

        vm.openAssessment(lockedLesson.mediaId)
        assertNull(vm.state.value.assessmentQuiz)
        vm.rewatchAfterFailedAssessment()
        assertNull(vm.state.value.playingMediaId)
    }

    @Test
    fun `passing a memory-check still credits the lesson and leaves play unused`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()
        finishQuiz(vm, mediaId = firstLesson.mediaId, correct = true)
        advanceUntilIdle()

        assertTrue(vm.state.value.assessmentQuiz?.finished == true)
        assertTrue(firstLesson.mediaId in vm.state.value.passedMediaIds)
        assertNull(vm.state.value.playingMediaId)
        assertNotNull(vm.state.value.assessmentQuiz)
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

    private fun asset(mediaId: String, title: String, episodeNumber: Int) = MediaAsset(
        mediaId = mediaId,
        title = title,
        file = "media/$mediaId.mp4",
        sha256 = "a".repeat(64),
        sizeBytes = 1L,
        durationSeconds = 60,
        width = 1,
        height = 1,
        subjectId = "mathematics",
        gradeLevel = 3,
        episodeNumber = episodeNumber,
        releaseStatus = "RELEASED",
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
