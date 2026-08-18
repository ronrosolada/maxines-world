package com.maxinesworld.featureprogress

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.RewardLedgerDao
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
import com.maxinesworld.corenetwork.ApiClient
import com.maxinesworld.corenetwork.SyncApiService
import com.maxinesworld.corenetwork.SyncPullResponse
import com.maxinesworld.corenetwork.SyncPushResponse
import com.maxinesworld.corenetwork.VideoWatchLedgerDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgressSyncRepositoryTest {

    private val progressEventDao = mockk<ProgressEventDao>(relaxed = true)
    private val videoWatchLedgerDao = mockk<VideoWatchLedgerDao>(relaxed = true)
    private val lessonCompletionDao = mockk<LessonCompletionDao>(relaxed = true)
    private val collectedBadgeDao = mockk<CollectedBadgeDao>(relaxed = true)
    private val rewardLedgerDao = mockk<RewardLedgerDao>(relaxed = true)
    private val inventoryDao = mockk<InventoryDao>(relaxed = true)
    private val apiClient = mockk<ApiClient>()
    private val syncService = mockk<SyncApiService>()

    private lateinit var repository: ProgressSyncRepository

    @Before
    fun setup() {
        coEvery { apiClient.createSyncService(any()) } returns syncService
        repository = ProgressSyncRepository(
            progressEventDao = progressEventDao,
            videoWatchLedgerDao = videoWatchLedgerDao,
            lessonCompletionDao = lessonCompletionDao,
            collectedBadgeDao = collectedBadgeDao,
            rewardLedgerDao = rewardLedgerDao,
            inventoryDao = inventoryDao,
            apiClient = apiClient
        )
    }

    @Test
    fun `sync pushes local data and merges pulled remote data correctly`() = runTest {
        val childId = "child_123"

        // Mock empty local state
        coEvery { progressEventDao.getPendingSyncByChild(childId) } returns emptyList()
        coEvery { videoWatchLedgerDao.getAllByChild(childId) } returns emptyList()
        coEvery { lessonCompletionDao.getRecentByChild(childId, any()) } returns emptyList()
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns emptyList()
        coEvery { rewardLedgerDao.getAllByChild(childId) } returns emptyList()
        coEvery { inventoryDao.getAllByChild(childId) } returns emptyList()

        // Mock syncService responses
        coEvery { syncService.pushSync(any()) } returns SyncPushResponse(
            success = true,
            processedCount = 5,
            serverTimestamp = 1000L
        )

        val remoteVideoRecord = VideoWatchLedgerDto(
            id = "child_123_vid1",
            childId = childId,
            mediaId = "vid1",
            subjectId = "science",
            accreditedSeconds = 300,
            quizPassed = true,
            bestQuizScore = 0.95f,
            firstPassedAtEpochMillis = 500L,
            lastWatchedAtEpochMillis = 900L
        )

        coEvery { syncService.pullSync(childId, 0L) } returns SyncPullResponse(
            childId = childId,
            serverTimestamp = 1000L,
            videoWatchRecords = listOf(remoteVideoRecord)
        )

        coEvery { videoWatchLedgerDao.getEntry(childId, "vid1") } returns null

        val result = repository.sync(childId, 0L)

        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertEquals(5, success.pushedCount)
        assertEquals(1, success.pulledCount)

        coVerify(exactly = 1) {
            videoWatchLedgerDao.insertOrUpdate(
                match {
                    it.mediaId == "vid1" &&
                    it.accreditedSeconds == 300 &&
                    it.quizPassed &&
                    it.bestQuizScore == 0.95f
                }
            )
        }
    }

    @Test
    fun `sync merges existing video watch ledger with MAX values`() = runTest {
        val childId = "child_123"

        coEvery { progressEventDao.getPendingSyncByChild(childId) } returns emptyList()
        coEvery { videoWatchLedgerDao.getAllByChild(childId) } returns emptyList()
        coEvery { lessonCompletionDao.getRecentByChild(childId, any()) } returns emptyList()
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns emptyList()
        coEvery { rewardLedgerDao.getAllByChild(childId) } returns emptyList()
        coEvery { inventoryDao.getAllByChild(childId) } returns emptyList()

        coEvery { syncService.pushSync(any()) } returns SyncPushResponse(
            success = true,
            processedCount = 0,
            serverTimestamp = 1000L
        )

        val remoteVideoRecord = VideoWatchLedgerDto(
            id = "child_123_vid1",
            childId = childId,
            mediaId = "vid1",
            subjectId = "science",
            accreditedSeconds = 100, // Less than local (200)
            quizPassed = true,       // Remote passed, local not
            bestQuizScore = 1.0f,    // Higher than local (0.5)
            firstPassedAtEpochMillis = 600L,
            lastWatchedAtEpochMillis = 800L
        )

        coEvery { syncService.pullSync(childId, 0L) } returns SyncPullResponse(
            childId = childId,
            serverTimestamp = 1000L,
            videoWatchRecords = listOf(remoteVideoRecord)
        )

        val localExisting = VideoWatchLedgerEntity(
            id = "child_123_vid1",
            childId = childId,
            mediaId = "vid1",
            subjectId = "science",
            accreditedSeconds = 200,
            quizPassed = false,
            bestQuizScore = 0.5f,
            firstPassedAtEpochMillis = null,
            lastWatchedAtEpochMillis = 700L
        )
        coEvery { videoWatchLedgerDao.getEntry(childId, "vid1") } returns localExisting

        val result = repository.sync(childId, 0L)

        assertTrue(result is SyncResult.Success)

        coVerify(exactly = 1) {
            videoWatchLedgerDao.insertOrUpdate(
                match {
                    it.accreditedSeconds == 200 && // MAX of 200 and 100
                    it.quizPassed &&              // OR of false and true
                    it.bestQuizScore == 1.0f &&   // MAX of 0.5 and 1.0
                    it.firstPassedAtEpochMillis == 600L &&
                    it.lastWatchedAtEpochMillis == 800L
                }
            )
        }
    }
}
