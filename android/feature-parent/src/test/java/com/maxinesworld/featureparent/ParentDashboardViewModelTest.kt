package com.maxinesworld.featureparent

import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.*
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.featurerewards.BadgeLoader
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
    private val context = mockk<android.content.Context>(relaxed = true)

    private lateinit var viewModel: ParentDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ParentDashboardViewModel(
            context = context,
            childProfileDao = childProfileDao,
            rewardDao = rewardDao,
            masteryRecordDao = masteryRecordDao,
            progressEventDao = progressEventDao,
            lessonCompletionDao = lessonCompletionDao,
            moduleCatalog = moduleCatalog,
            godModeManager = godModeManager,
            badgeLoader = badgeLoader,
            collectedBadgeDao = collectedBadgeDao,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load loads child data, available badges and earned badges`() = runTest {
        val childId = "child_maxine"
        val badges = listOf(
            CollectibleBadge("badge_tarsier", "forest", "Tarsier", "Moon Eyed", "Fun fact"),
            CollectibleBadge("badge_eagle", "sky", "Eagle", "Sky King", "Fun fact")
        )
        val earnedEntities = listOf(
            CollectedBadgeEntity(
                id = "${childId}_badge_tarsier",
                childId = childId,
                badgeId = "badge_tarsier",
                biome = "forest",
                earnedDate = "2026-08-18"
            )
        )

        coEvery { childProfileDao.getById(childId) } returns ChildProfileEntity(
            id = childId,
            parentId = "parent",
            name = "Maxine",
            avatarId = "cat_orange_default",
            grade = 3,
            curriculum = "ph-matatag",
        )
        coEvery { badgeLoader.loadAll() } returns badges
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns earnedEntities

        viewModel.load(childId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Maxine", viewModel.state.value.childName)
        assertEquals(2, viewModel.availableBadges.value.size)
        assertTrue(viewModel.earnedBadgeIds.value.contains("badge_tarsier"))
    }

    @Test
    fun `awardSticker inserts into collectedBadgeDao and updates earnedBadgeIds`() = runTest {
        val childId = "child_maxine"
        val badgeToAward = CollectibleBadge("badge_eagle", "sky", "Eagle", "Sky King", "Fun fact")

        coEvery { collectedBadgeDao.getAllByChild(childId) } returns listOf(
            CollectedBadgeEntity(
                id = "${childId}_badge_eagle",
                childId = childId,
                badgeId = "badge_eagle",
                biome = "sky",
                earnedDate = "parent_awarded"
            )
        )

        viewModel.awardSticker(childId, badgeToAward)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            collectedBadgeDao.insert(match {
                it.childId == childId && it.badgeId == "badge_eagle" && it.earnedDate == "parent_awarded"
            })
        }
        assertTrue(viewModel.earnedBadgeIds.value.contains("badge_eagle"))
    }
}
