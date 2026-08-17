package com.maxinesworld.featureparent

import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.*
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.featurerewards.BadgeLoader
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentDashboardGrantTest {

    private val testDispatcher = StandardTestDispatcher()
    private val childProfileDao = mockk<ChildProfileDao>(relaxed = true)
    private val rewardDao = mockk<RewardDao>(relaxed = true)
    private val masteryRecordDao = mockk<MasteryRecordDao>(relaxed = true)
    private val progressEventDao = mockk<ProgressEventDao>(relaxed = true)
    private val lessonCompletionDao = mockk<LessonCompletionDao>(relaxed = true)
    private val moduleCatalog = mockk<ModuleCatalog>(relaxed = true)
    private val godModeManager = mockk<GodModeManager>(relaxed = true)
    private val collectedBadgeDao = mockk<CollectedBadgeDao>(relaxed = true)
    private val badgeLoader = mockk<BadgeLoader>(relaxed = true)

    private lateinit var viewModel: ParentDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { godModeManager.isEnabled() } returns false
        coEvery { childProfileDao.getById("child_1") } returns ChildProfileEntity(
            id = "child_1",
            parentId = "parent_1",
            name = "Maxine",
            grade = 3,
        )
        coEvery { rewardDao.getTotalByType("child_1", "STAR") } returns 20
        coEvery { rewardDao.getTotalByType("child_1", "COIN") } returns 5
        coEvery { masteryRecordDao.getByChild("child_1") } returns emptyList()
        coEvery { progressEventDao.getByChild("child_1") } returns emptyList()
        coEvery { lessonCompletionDao.getRecentByChild("child_1", 5) } returns emptyList()

        viewModel = ParentDashboardViewModel(
            childProfileDao = childProfileDao,
            rewardDao = rewardDao,
            masteryRecordDao = masteryRecordDao,
            progressEventDao = progressEventDao,
            lessonCompletionDao = lessonCompletionDao,
            moduleCatalog = moduleCatalog,
            godModeManager = godModeManager,
            collectedBadgeDao = collectedBadgeDao,
            badgeLoader = badgeLoader,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `grantStars inserts RewardEntity with STAR type and reloads totals`() = runTest(testDispatcher) {
        val rewardSlot = slot<RewardEntity>()
        coEvery { rewardDao.insert(capture(rewardSlot)) } just Runs
        coEvery { rewardDao.getTotalByType("child_1", "STAR") } returnsMany listOf(20, 30)

        viewModel.load("child_1")
        testScheduler.advanceUntilIdle()
        assertEquals(20, viewModel.state.value.totalStars)

        viewModel.grantStars("child_1", 10)
        testScheduler.advanceUntilIdle()

        assertTrue(rewardSlot.isCaptured)
        assertEquals("child_1", rewardSlot.captured.childId)
        assertEquals("STAR", rewardSlot.captured.type)
        assertEquals(10, rewardSlot.captured.amount)
        assertEquals("parent_manual_grant", rewardSlot.captured.metadata)
        assertEquals("Granted +10 Stars!", viewModel.state.value.grantStatusMessage)
        assertEquals(30, viewModel.state.value.totalStars)
    }

    @Test
    fun `grantNextSticker inserts next uncollected badge into CollectedBadgeDao`() = runTest(testDispatcher) {
        val badge1 = CollectibleBadge(id = "eagle", biome = "forest", name = "Philippine Eagle", title = "King of the Forest", funFact = "Majestic")
        val badge2 = CollectibleBadge(id = "tamaraw", biome = "meadow", name = "Tamaraw", title = "Mindoro Dwarf Buffalo", funFact = "Rare")
        val milestone = CollectibleBadge(id = "first_steps", biome = "milestone", name = "First Steps", title = "First Steps", funFact = "Milestone")

        coEvery { badgeLoader.loadAll() } returns listOf(badge1, badge2, milestone)
        coEvery { collectedBadgeDao.getAllByChild("child_1") } returns listOf(
            CollectedBadgeEntity(id = "child_1_eagle", childId = "child_1", badgeId = "eagle", biome = "forest", earnedDate = "2026-08-10")
        )

        val insertedBadgeSlot = slot<CollectedBadgeEntity>()
        coEvery { collectedBadgeDao.insert(capture(insertedBadgeSlot)) } just Runs

        viewModel.load("child_1")
        testScheduler.advanceUntilIdle()

        viewModel.grantNextSticker("child_1")
        testScheduler.advanceUntilIdle()

        assertTrue(insertedBadgeSlot.isCaptured)
        assertEquals("child_1_tamaraw", insertedBadgeSlot.captured.id)
        assertEquals("tamaraw", insertedBadgeSlot.captured.badgeId)
        assertEquals("meadow", insertedBadgeSlot.captured.biome)
        assertEquals("Granted Wildlife Sticker: Tamaraw!", viewModel.state.value.grantStatusMessage)
    }

    @Test
    fun `grantNextSticker displays message when all stickers collected`() = runTest(testDispatcher) {
        val badge1 = CollectibleBadge(id = "eagle", biome = "forest", name = "Philippine Eagle", title = "King of the Forest", funFact = "Majestic")

        coEvery { badgeLoader.loadAll() } returns listOf(badge1)
        coEvery { collectedBadgeDao.getAllByChild("child_1") } returns listOf(
            CollectedBadgeEntity(id = "child_1_eagle", childId = "child_1", badgeId = "eagle", biome = "forest", earnedDate = "2026-08-10")
        )

        viewModel.load("child_1")
        testScheduler.advanceUntilIdle()

        viewModel.grantNextSticker("child_1")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { collectedBadgeDao.insert(any()) }
        assertEquals("All wildlife stickers already collected!", viewModel.state.value.grantStatusMessage)
    }
}
