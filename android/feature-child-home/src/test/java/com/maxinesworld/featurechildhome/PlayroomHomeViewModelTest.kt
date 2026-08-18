package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.corecontent.ContentModule
import com.maxinesworld.corecontent.ContentModuleLesson
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredatabase.GodModeManager
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import io.mockk.*
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayroomHomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `GMRC is available from the first session`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val gmrc = content(vm).subjects.first { it.id == "gmrc" }
        assertTrue(gmrc.isAvailable)
        assertNull(gmrc.lockReason)
    }

    @Test
    fun `six canonical subjects stay in fixed order`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        // Makabansa replaces the old separate Araling Panlipunan card — the
        // legacy AP lessons are folded into its collection (2026-08-06 merge).
        assertEquals(
            listOf("mathematics", "english", "science", "filipino", "makabansa", "gmrc"),
            content(vm).subjects.map { it.id },
        )
        assertTrue(content(vm).subjects.all { it.isAvailable })
    }


    @Test
    fun `per-subject progress divides completed lessons by catalog total`() = runTest(dispatcher) {
        val completed = (1..3).map { "mathematics-g3-m01-d%02d".format(it) }
        val vm = buildViewModel(
            completedLessons = completed,
            catalog = catalogWithTotals(mapOf("mathematics" to 6)),
        )
        advanceUntilIdle()
        assertEquals(50, content(vm).subjects.first { it.id == "mathematics" }.progressPercent)
    }

    @Test
    fun `unfinished expedition shows persistent weekly progress`() = runTest(dispatcher) {
        val vm = buildViewModel(
            quest = ChallengeProgress(english = true, gmrc = true, completedCount = 2, subjectCount = 2),
        )
        advanceUntilIdle()
        val quest = content(vm).quest
        assertEquals(2, quest.pawPrintsCompleted)
        assertEquals(3, quest.pawPrintTotal)
        assertFalse(quest.isComplete)
        assertEquals(QuestButtonLabel.ContinueQuest, quest.buttonLabel)
        assertEquals(QuestTaskCopy.IncompleteToday, quest.task)
    }

    @Test
    fun `completed expedition opens the field guide`() = runTest(dispatcher) {
        val vm = buildViewModel(
            quest = ChallengeProgress(
                english = true, gmrc = true, science = true,
                completedCount = 3, subjectCount = 2, expeditionComplete = true,
            ),
        )
        advanceUntilIdle()
        val quest = content(vm).quest
        assertTrue(quest.isComplete)
        assertEquals(QuestButtonLabel.OpenSanctuary, quest.buttonLabel)
        assertEquals(QuestAction.ViewReward, quest.buttonAction)
    }

    @Test
    fun `sticker book reflects collected badges`() = runTest(dispatcher) {
        val badges = listOf(badge("b1", true), badge("b2", false))
        val vm = buildViewModel(badges = badges)
        advanceUntilIdle()
        val stickers = content(vm).wildlifeStickers
        assertEquals(1, stickers.collectedCount)
        assertEquals(2, stickers.totalCount)
        assertEquals(listOf(true), stickers.stickers.map { it.won })
    }

    @Test
    fun `god mode exposes all reward components and the playground without changing lesson progress`() = runTest(dispatcher) {
        val vm = buildViewModel(
            completedLessons = listOf("english-g3-m01-d01"),
            badges = listOf(badge("b1", true), badge("b2", false)),
            catalog = catalogWithTotals(mapOf("english" to 1)),
            godModeEnabled = true,
        )
        advanceUntilIdle()

        val content = content(vm)
        assertEquals(100, content.subjects.first { it.id == "english" }.progressPercent)
        assertEquals(2, content.wildlifeStickers.collectedCount)
        assertEquals(3, content.ownedKeepsakes.size)
        assertEquals(12, content.sanctuary.earnedPieces)
        assertEquals(QuestAction.OpenPlayground, content.quest.buttonAction)
        assertEquals(QuestButtonLabel.OpenPlayground, content.quest.buttonLabel)
    }

    @Test
    fun `balances are loaded into the child home state`() = runTest(dispatcher) {
        val vm = buildViewModel(starBalance = 12, coinBalance = 37)
        advanceUntilIdle()

        assertEquals(12, content(vm).starBalance)
        assertEquals(37, content(vm).coinBalance)
    }

    @Test
    fun `load failure enters error and retry creates one fresh content collector`() = runTest(dispatcher) {
        var shouldFail = true
        val vm = buildViewModel(shouldFailExpedition = { shouldFail })
        advanceUntilIdle()
        assertTrue(vm.state.value is PlayroomHomeUiState.Error)

        shouldFail = false
        vm.retry()
        advanceUntilIdle()

        assertTrue(vm.state.value is PlayroomHomeUiState.Content)
    }

    @Test
    fun `opening subject rejects repeated activation and clears`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onSubjectSelected("mathematics")
        assertEquals("mathematics", content(vm).openingSubjectId)
        vm.onSubjectSelected("english")
        assertEquals("mathematics", content(vm).openingSubjectId)
        vm.onOpenFinished()
        assertNull(content(vm).openingSubjectId)
    }

    @Test
    fun `GMRC can be opened immediately`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onSubjectSelected("gmrc")
        assertEquals("gmrc", content(vm).openingSubjectId)
    }

    private fun buildViewModel(
        completedLessons: List<String> = emptyList(),
        quest: ChallengeProgress = ChallengeProgress(),
        badges: List<com.maxinesworld.coremodel.CollectibleBadge> = emptyList(),
        childName: String? = "Maxine",
        catalog: ModuleCatalog = emptyCatalog(),
        starBalance: Int = 0,
        coinBalance: Int = 0,
        godModeEnabled: Boolean = false,
        shouldFailExpedition: () -> Boolean = { false },
    ): PlayroomHomeViewModel {
        val profileDao = mockk<ChildProfileDao>()
        coEvery { profileDao.observeById("child_1") } returns flowOf(
            childName?.let {
                ChildProfileEntity(
                    id = "child_1", parentId = "parent_1", name = it,
                    avatarId = "cat_orange_default", grade = 3, curriculum = "ph-matatag",
                    createdAt = 0L,
                )
            }
        )
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.observeDistinctLessonIds("child_1") } returns flowOf(completedLessons)
        val awarder = mockk<BadgeAwarder>()
        coEvery { awarder.getExpeditionProgress("child_1") } coAnswers {
            if (shouldFailExpedition()) throw IllegalStateException("expedition load failed")
            quest
        }
        coEvery { awarder.getCollectedBadges("child_1") } returns badges
        val rewardDao = mockk<RewardDao>()
        coEvery { rewardDao.getTotalByType("child_1", "STAR") } returns starBalance
        coEvery { rewardDao.getTotalByType("child_1", "COIN") } returns coinBalance
        coEvery { rewardDao.getByChildAndType("child_1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE) } returns emptyList()
        val dailyQuestManager = mockk<DailyQuestManager>()
        val inventoryDao = mockk<InventoryDao>()
        coEvery { inventoryDao.getOwnedItemIds("child_1") } returns emptyList()
        val assignedQuestIds = (0 until 3).map { "daily-quest-$it" }
        val completedQuestIds = assignedQuestIds.take(quest.completedCount.coerceIn(0, 3))
        coEvery { dailyQuestManager.ensureToday("child_1", any(), any()) } coAnswers {
            if (shouldFailExpedition()) throw IllegalStateException("daily quest load failed")
            DailyQuestProgress("2026-08-04", assignedQuestIds, completedQuestIds)
        }
        val videoWatchLedgerDao = mockk<VideoWatchLedgerDao>()
        every { videoWatchLedgerDao.observeTotalAccreditedSeconds("child_1") } returns flowOf(0)
        coEvery { videoWatchLedgerDao.getTotalAccreditedSeconds("child_1") } returns 0
        val godModeManager = mockk<GodModeManager>()
        every { godModeManager.enabled } returns flowOf(godModeEnabled)
        return PlayroomHomeViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to "child_1")),
            catalog = catalog,
            childProfileDao = profileDao,
            lessonCompletionDao = completionDao,
            badgeAwarder = awarder,
            rewardDao = rewardDao,
            inventoryDao = inventoryDao,
            videoWatchLedgerDao = videoWatchLedgerDao,
            dailyQuestManager = dailyQuestManager,
            godModeManager = godModeManager,
        )
    }

    private fun content(vm: PlayroomHomeViewModel): PlayroomHomeUiState.Content =
        vm.state.value as PlayroomHomeUiState.Content

    private fun emptyCatalog(): ModuleCatalog {
        val catalog = mockk<ModuleCatalog>()
        coEvery { catalog.modulesFor(any()) } returns emptyList()
        return catalog
    }

    private fun catalogWithTotals(lessonCounts: Map<String, Int>): ModuleCatalog {
        val catalog = mockk<ModuleCatalog>()
        coEvery { catalog.modulesFor(any()) } answers { call ->
            val subject = call.invocation.args[0] as String
            val count = lessonCounts[subject] ?: 0
            listOf(
                ContentModule(
                    key = "m01",
                    title = "Module",
                    lessons = (1..count).map {
                        ContentModuleLesson(
                            lessonId = "${subject}-g3-m01-d%02d".format(it),
                            title = "Lesson $it",
                            day = it,
                        )
                    },
                )
            )
        }
        return catalog
    }

    private fun badge(id: String, collected: Boolean) = com.maxinesworld.coremodel.CollectibleBadge(
        id = id, biome = "test", name = "Badge $id", title = "T", funFact = "F",
        isCollected = collected,
    )
}
