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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoLibraryCatalogFilterTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mediaLibrary = mockk<MediaLibrary>()
    private val videoWatchLedgerDao = mockk<VideoWatchLedgerDao>(relaxed = true)
    private val rewardDao = mockk<RewardDao>(relaxed = true)
    private val collectedBadgeDao = mockk<CollectedBadgeDao>(relaxed = true)
    private val badgeLoader = mockk<BadgeLoader>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { videoWatchLedgerDao.observePassedMediaIds(any()) } returns flowOf(emptyList())
        every { mediaLibrary.localFile(any()) } returns null
        coEvery { badgeLoader.loadAll() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `child library hides preview and other-grade catalog rows`() = runTest(testDispatcher) {
        val catalog = MediaCatalog(
            catalogVersion = 1,
            generatedAt = "test",
            media = listOf(
                asset("g3-released", gradeLevel = 3, releaseStatus = "RELEASED"),
                asset("g3-preview", gradeLevel = 3, releaseStatus = "PREVIEW"),
                asset("g1-released", gradeLevel = 1, releaseStatus = "RELEASED"),
                asset("g4-preview", gradeLevel = 4, releaseStatus = "PREVIEW"),
            ),
        )
        every { mediaLibrary.getCachedCatalog() } returns catalog
        coEvery { mediaLibrary.getCatalog() } returns catalog
        coEvery { mediaLibrary.refreshCatalog() } returns catalog

        val vm = VideoLibraryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to "maxine")),
            mediaLibrary = mediaLibrary,
            videoWatchLedgerDao = videoWatchLedgerDao,
            rewardDao = rewardDao,
            collectedBadgeDao = collectedBadgeDao,
            badgeLoader = badgeLoader,
        )
        advanceUntilIdle()

        assertEquals(listOf("g3-released"), vm.state.value.allItems.map { it.asset.mediaId })
    }

    private fun asset(mediaId: String, gradeLevel: Int, releaseStatus: String) = MediaAsset(
        mediaId = mediaId,
        title = mediaId,
        file = "media/$mediaId.mp4",
        sha256 = "a".repeat(64),
        sizeBytes = 1L,
        durationSeconds = 60,
        width = 1,
        height = 1,
        subjectId = "english",
        gradeLevel = gradeLevel,
        episodeNumber = 1,
        releaseStatus = releaseStatus,
    )
}
