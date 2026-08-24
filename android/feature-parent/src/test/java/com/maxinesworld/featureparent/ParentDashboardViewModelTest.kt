package com.maxinesworld.featureparent

import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.*
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.corenetwork.AppUpdateManager
import com.maxinesworld.corenetwork.VideoPrefetchManager
import com.maxinesworld.featurerewards.BadgeLoader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val childProfileDao = mockk<ChildProfileDao>(relaxed = true)
    private val rewardDao = mockk<RewardDao>(relaxed = true)
    private val masteryRecordDao = mockk<MasteryRecordDao>(relaxed = true)
    private val progressEventDao = mockk<ProgressEventDao>(relaxed = true)
    private val lessonCompletionDao = mockk<LessonCompletionDao>(relaxed = true)
    private val moduleCatalog = mockk<ModuleCatalog>(relaxed = true)
    private val godModeManager = mockk<GodModeManager>(relaxed = true)
    private val badgeLoader = mockk<BadgeLoader>(relaxed = true)
    private val collectedBadgeDao = mockk<CollectedBadgeDao>(relaxed = true)
    private val videoWatchLedgerDao = mockk<VideoWatchLedgerDao>(relaxed = true)
    private val videoPrefetchManager = mockk<VideoPrefetchManager>(relaxed = true)
    private val appUpdateManager = mockk<AppUpdateManager>(relaxed = true)

    /** Hot flows standing in for Room's reactive streams. */
    private val childFlow = MutableStateFlow<ChildProfileEntity?>(null)
    private val rewardsFlow = MutableStateFlow<List<RewardEntity>>(emptyList())
    private val masteryFlow = MutableStateFlow<List<MasteryRecordEntity>>(emptyList())
    private val progressFlow = MutableStateFlow<List<ProgressEventEntity>>(emptyList())
    private val completionsFlow = MutableStateFlow<List<LessonCompletionEntity>>(emptyList())
    private val accreditedSecondsFlow = MutableStateFlow(0)
    private val passedMediaIdsFlow = MutableStateFlow<List<String>>(emptyList())
    private val badgeIdsFlow = MutableStateFlow<List<String>>(emptyList())

    private lateinit var viewModel: ParentDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { childProfileDao.observeById(any()) } returns childFlow
        every { rewardDao.observeByChild(any()) } returns rewardsFlow
        every { masteryRecordDao.observeByChild(any()) } returns masteryFlow
        every { progressEventDao.observeByChild(any()) } returns progressFlow
        every { lessonCompletionDao.observeRecentByChild(any(), any()) } returns completionsFlow
        every { videoWatchLedgerDao.observeTotalAccreditedSeconds(any()) } returns accreditedSecondsFlow
        every { videoWatchLedgerDao.observePassedMediaIds(any()) } returns passedMediaIdsFlow
        every { collectedBadgeDao.observeBadgeIdsByChild(any()) } returns badgeIdsFlow
        coEvery { godModeManager.isEnabledNow(any()) } returns false

        viewModel = ParentDashboardViewModel(
            childProfileDao = childProfileDao,
            rewardDao = rewardDao,
            masteryRecordDao = masteryRecordDao,
            progressEventDao = progressEventDao,
            lessonCompletionDao = lessonCompletionDao,
            moduleCatalog = moduleCatalog,
            godModeManager = godModeManager,
            badgeLoader = badgeLoader,
            collectedBadgeDao = collectedBadgeDao,
            videoWatchLedgerDao = videoWatchLedgerDao,
            videoPrefetchManager = videoPrefetchManager,
            appUpdateManager = appUpdateManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loadAndWait(childId: String) {
        viewModel.load(childId)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `load observes child data, available badges and earned badges reactively`() = runTest {
        val childId = "child_maxine"
        val badges = listOf(
            CollectibleBadge("badge_tarsier", "forest", "Tarsier", "Moon Eyed", "Fun fact"),
            CollectibleBadge("badge_eagle", "sky", "Eagle", "Sky King", "Fun fact")
        )
        coEvery { badgeLoader.loadAll() } returns badges
        childFlow.value = ChildProfileEntity(
            id = childId,
            parentId = "parent",
            name = "Maxine",
            avatarId = "cat_orange_default",
            grade = 3,
            curriculum = "ph-matatag",
        )
        badgeIdsFlow.value = listOf("badge_tarsier")

        loadAndWait(childId)

        assertEquals("Maxine", viewModel.state.value.childName)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(2, viewModel.availableBadges.value.size)
        assertTrue(viewModel.earnedBadgeIds.value.contains("badge_tarsier"))
    }

    @Test
    fun `awardSticker inserts into collectedBadgeDao`() = runTest {
        val childId = "child_maxine"
        val badgeToAward = CollectibleBadge("badge_eagle", "sky", "Eagle", "Sky King", "Fun fact")

        loadAndWait(childId)
        viewModel.awardSticker(childId, badgeToAward)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            collectedBadgeDao.insert(match {
                it.childId == childId && it.badgeId == "badge_eagle" && it.earnedDate == "parent_awarded"
            })
        }
    }

    @Test
    fun `revokeSticker deletes the badge row from collectedBadgeDao`() = runTest {
        val childId = "child_maxine"

        loadAndWait(childId)
        viewModel.revokeSticker(childId, "badge_tarsier")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            collectedBadgeDao.deleteByChildAndBadgeId(childId, "badge_tarsier")
        }
    }

    @Test
    fun `revokeSticker removes the badge from earnedBadgeIds when the dao emits post-delete state`() = runTest {
        val childId = "child_maxine"
        badgeIdsFlow.value = listOf("badge_tarsier", "badge_eagle")

        loadAndWait(childId)
        assertTrue(viewModel.earnedBadgeIds.value.contains("badge_tarsier"))

        viewModel.revokeSticker(childId, "badge_tarsier")
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate Room invalidating the observed query after the DELETE lands.
        badgeIdsFlow.value = listOf("badge_eagle")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.earnedBadgeIds.value.contains("badge_tarsier"))
        assertEquals(setOf("badge_eagle"), viewModel.earnedBadgeIds.value)
    }

    @Test
    fun `dashboard state updates in real time without calling load again`() = runTest {
        val childId = "child_maxine"
        childFlow.value = ChildProfileEntity(
            id = childId,
            parentId = "parent",
            name = "Maxine",
            avatarId = "cat_orange_default",
            grade = 3,
            curriculum = "ph-matatag",
        )

        loadAndWait(childId)
        assertEquals(0, viewModel.state.value.totalStars)
        assertEquals(0, viewModel.state.value.accreditedWatchSeconds)

        // A new star is awarded elsewhere in the app while the dashboard stays open.
        rewardsFlow.value = listOf(
            RewardEntity(id = "r1", childId = childId, type = "STAR", subject = "mathematics", amount = 5),
            RewardEntity(id = "r2", childId = childId, type = "COIN", subject = "mathematics", amount = 10),
        )
        accreditedSecondsFlow.value = 600
        passedMediaIdsFlow.value = listOf("video-1", "video-2")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, viewModel.state.value.totalStars)
        assertEquals(10, viewModel.state.value.totalCoins)
        assertEquals(600, viewModel.state.value.accreditedWatchSeconds)
        assertEquals(2, viewModel.state.value.passedVideoCount)
    }

    @Test
    fun `prefetchMedia calls videoPrefetchManager and updates state message`() = runTest {
        coEvery { videoPrefetchManager.prefetchNextVideos(3) } returns 2
        every { videoPrefetchManager.getStorageUsedBytes() } returns 10485760L // 10 MB

        loadAndWait("child_maxine")
        viewModel.prefetchMedia(3)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { videoPrefetchManager.prefetchNextVideos(3) }
        assertEquals("Downloaded 2 lesson(s) for offline use", viewModel.state.value.prefetchStatusMessage)
        assertEquals(10f, viewModel.state.value.storageUsedMb, 0.01f)
    }

    @Test
    fun `clearMediaCache delegates to videoPrefetchManager`() = runTest {
        every { videoPrefetchManager.clearStorage() } returns 3
        every { videoPrefetchManager.getStorageUsedBytes() } returns 0L

        loadAndWait("child_maxine")
        viewModel.clearMediaCache()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { videoPrefetchManager.clearStorage() }
        assertEquals(0f, viewModel.state.value.storageUsedMb, 0.01f)
        assertEquals("Storage cleared", viewModel.state.value.prefetchStatusMessage)
    }
}
