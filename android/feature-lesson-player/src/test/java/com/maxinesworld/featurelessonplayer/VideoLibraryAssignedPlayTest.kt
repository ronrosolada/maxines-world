package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.coremodel.MediaAsset
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VideoLibraryAssignedPlayTest {

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
    fun `assigned downloaded video plays that mediaId`() = runTest(testDispatcher) {
        every { mediaLibrary.localFile("math-g3-01") } returns File("/tmp/math-g3-01.mp4")

        val vm = createViewModel(mediaId = firstLesson.mediaId)
        advanceUntilIdle()

        assertEquals(firstLesson.mediaId, vm.state.value.playingMediaId)
        assertNull(vm.state.value.assignedPlayMessage)
        coVerify(exactly = 0) { mediaLibrary.downloadChildFacing(any()) }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `assigned undownloaded video downloads child-facing then plays`() = runTest(testDispatcher) {
        val downloaded = File("/tmp/math-g3-01.mp4")
        coEvery { mediaLibrary.downloadChildFacing("math-g3-01") } returns downloaded

        val vm = createViewModel(mediaId = firstLesson.mediaId)
        assertEquals(VideoLibraryAssignedPlayCopy.GETTING_READY, vm.state.value.assignedPlayMessage)

        advanceUntilIdle()

        assertEquals(firstLesson.mediaId, vm.state.value.playingMediaId)
        assertNull(vm.state.value.assignedPlayMessage)
        coVerify(exactly = 1) { mediaLibrary.downloadChildFacing("math-g3-01") }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `assigned locked video does not play or download`() = runTest(testDispatcher) {
        every { mediaLibrary.localFile("math-g3-02") } returns File("/tmp/math-g3-02.mp4")

        val vm = createViewModel(mediaId = lockedLesson.mediaId)
        advanceUntilIdle()

        assertTrue(vm.state.value.allItems.first { it.asset.mediaId == lockedLesson.mediaId }.isLocked)
        assertNull(vm.state.value.playingMediaId)
        assertEquals(VideoLibraryAssignedPlayCopy.LOCKED, vm.state.value.assignedPlayMessage)
        coVerify(exactly = 0) { mediaLibrary.downloadChildFacing(any()) }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `assigned preview video is refused and never downloaded`() = runTest(testDispatcher) {
        val vm = createViewModel(mediaId = previewLesson.mediaId)
        advanceUntilIdle()

        assertTrue(vm.state.value.allItems.none { it.asset.mediaId == previewLesson.mediaId })
        assertNull(vm.state.value.playingMediaId)
        assertEquals(
            VideoLibraryAssignedPlayCopy.NOT_CHILD_FACING,
            vm.state.value.assignedPlayMessage,
        )
        coVerify(exactly = 0) { mediaLibrary.downloadChildFacing(any()) }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `assigned other-grade video is refused and never downloaded`() = runTest(testDispatcher) {
        val vm = createViewModel(mediaId = otherGrade.mediaId)
        advanceUntilIdle()

        assertTrue(vm.state.value.allItems.none { it.asset.mediaId == otherGrade.mediaId })
        assertNull(vm.state.value.playingMediaId)
        assertEquals(
            VideoLibraryAssignedPlayCopy.NOT_CHILD_FACING,
            vm.state.value.assignedPlayMessage,
        )
        coVerify(exactly = 0) { mediaLibrary.downloadChildFacing(any()) }
        coVerify(exactly = 0) { mediaLibrary.download(any()) }
    }

    @Test
    fun `play still refuses a locked video after a prepare request`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.playAssigned(lockedLesson.mediaId)
        vm.play(lockedLesson.mediaId)

        assertNull(vm.state.value.playingMediaId)
        assertEquals(VideoLibraryAssignedPlayCopy.LOCKED, vm.state.value.assignedPlayMessage)
    }

    private fun createViewModel(mediaId: String? = null) = VideoLibraryViewModel(
        savedStateHandle = SavedStateHandle(
            buildMap {
                put("childId", "maxine")
                put("subject", "mathematics")
                if (mediaId != null) put("mediaId", mediaId)
            },
        ),
        mediaLibrary = mediaLibrary,
        videoWatchLedgerDao = videoWatchLedgerDao,
        rewardDao = rewardDao,
        collectedBadgeDao = collectedBadgeDao,
        badgeLoader = badgeLoader,
    )

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
    )
}
