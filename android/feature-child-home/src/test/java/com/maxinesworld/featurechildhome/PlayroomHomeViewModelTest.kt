package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.corecontent.ContentModule
import com.maxinesworld.corecontent.ContentModuleLesson
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaCatalog
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredatabase.GodModeManager
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.PlaygroundUnlockReceiptDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import io.mockk.*
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    fun `per-subject video progress uses passed media ids and active media total`() = runTest(dispatcher) {
        val vm = buildViewModel(
            completedLessons = listOf("mathematics-g3-m01-d01", "mathematics-g3-m01-d02", "mathematics-g3-m01-d03"),
            catalog = catalogWithTotals(mapOf("mathematics" to 6)),
            videoCatalog = mediaCatalogWithTotals("mathematics" to 6),
            passedVideoMediaIds = (1..3).map { "mathematics-video-$it" },
        )
        advanceUntilIdle()
        val mathematics = content(vm).subjects.first { it.id == "mathematics" }
        assertEquals(3, mathematics.completedVideos)
        assertEquals(6, mathematics.totalVideos)
    }

    @Test
    fun `missing video catalog does not expose legacy lesson progress as video progress`() = runTest(dispatcher) {
        val vm = buildViewModel(
            completedLessons = (1..3).map { "mathematics-g3-m01-d%02d".format(it) },
            catalog = catalogWithTotals(mapOf("mathematics" to 6)),
            videoCatalogLoadFails = true,
        )
        advanceUntilIdle()
        val mathematics = content(vm).subjects.first { it.id == "mathematics" }
        assertEquals(null, mathematics.completedVideos)
        assertEquals(null, mathematics.totalVideos)
    }

    @Test
    fun `video progress helper applies latest values without changing home content`() = runTest(dispatcher) {
        val vm = buildViewModel(
            videoCatalog = mediaCatalogWithTotals("mathematics" to 2),
            passedVideoMediaIds = listOf("mathematics-video-1"),
        )
        advanceUntilIdle()

        val base = content(vm).copy(subjects = canonicalSubjects)
        val derived = withVideoProgress(
            baseContent = base,
            assets = mediaCatalogWithTotals("mathematics" to 2).media,
            passedMediaIds = setOf("mathematics-video-1", "mathematics-video-2"),
        )

        val mathematics = derived.subjects.first { it.id == "mathematics" }
        assertEquals(2, mathematics.completedVideos)
        assertEquals(2, mathematics.totalVideos)
        assertEquals(base.quest, derived.quest)
        assertEquals(base.childName, derived.childName)
    }

    @Test
    fun `latest passed ids win when an older base content emission completes later`() = runTest(dispatcher) {
        val profiles = MutableStateFlow<ChildProfileEntity?>(profile("Maxine"))
        val passedIds = MutableStateFlow(listOf("mathematics-video-1"))
        val rebuildStarted = CompletableDeferred<Unit>()
        val releaseRebuild = CompletableDeferred<Unit>()
        val catalogCalls = AtomicInteger()
        var initialCallCount = Int.MAX_VALUE
        val catalog = mockk<ModuleCatalog>()
        coEvery { catalog.modulesFor(any()) } coAnswers {
            val call = catalogCalls.incrementAndGet()
            if (call > initialCallCount && rebuildStarted.complete(Unit)) {
                releaseRebuild.await()
            }
            emptyList()
        }

        val vm = buildViewModel(
            catalog = catalog,
            videoCatalog = mediaCatalogWithTotals("mathematics" to 2),
            profileFlow = profiles,
            passedVideoMediaIdsFlow = passedIds,
        )
        advanceUntilIdle()
        initialCallCount = catalogCalls.get()
        assertEquals(1, content(vm).subjects.first { it.id == "mathematics" }.completedVideos)

        profiles.value = profile("Updated Maxine")
        runCurrent()
        rebuildStarted.await()

        passedIds.value = listOf("mathematics-video-1", "mathematics-video-2")
        runCurrent()
        releaseRebuild.complete(Unit)
        advanceUntilIdle()

        val mathematics = content(vm).subjects.first { it.id == "mathematics" }
        assertEquals(2, mathematics.completedVideos)
        assertEquals(2, mathematics.totalVideos)
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
        advanceUntilIdle()
        assertEquals("mathematics", content(vm).openingSubjectId)
        vm.onSubjectSelected("english")
        advanceUntilIdle()
        assertEquals("mathematics", content(vm).openingSubjectId)
        vm.onOpenFinished()
        advanceUntilIdle()
        assertNull(content(vm).openingSubjectId)
    }

    @Test
    fun `GMRC can be opened immediately`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onSubjectSelected("gmrc")
        advanceUntilIdle()
        assertEquals("gmrc", content(vm).openingSubjectId)
    }

    private fun buildViewModel(
        completedLessons: List<String> = emptyList(),
        quest: ChallengeProgress = ChallengeProgress(),
        badges: List<com.maxinesworld.coremodel.CollectibleBadge> = emptyList(),
        childName: String? = "Maxine",
        catalog: ModuleCatalog = emptyCatalog(),
        videoCatalog: MediaCatalog = MediaCatalog(1, "", emptyList()),
        passedVideoMediaIds: List<String> = emptyList(),
        passedVideoMediaIdsFlow: Flow<List<String>> = flowOf(passedVideoMediaIds),
        profileFlow: Flow<ChildProfileEntity?>? = null,
        videoCatalogLoadFails: Boolean = false,
        starBalance: Int = 0,
        coinBalance: Int = 0,
        godModeEnabled: Boolean = false,
        shouldFailExpedition: () -> Boolean = { false },
    ): PlayroomHomeViewModel {
        val profileDao = mockk<ChildProfileDao>()
        coEvery { profileDao.observeById("child_1") } returns (
            profileFlow ?: flowOf(childName?.let(::profile))
        )
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.observeDistinctLessonIds("child_1") } returns flowOf(completedLessons)
        val mediaLibrary = mockk<MediaLibrary>()
        if (videoCatalogLoadFails) {
            coEvery { mediaLibrary.getCatalog() } throws IllegalStateException("media catalog unavailable")
        } else {
            coEvery { mediaLibrary.getCatalog() } returns videoCatalog
        }
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
        every { videoWatchLedgerDao.observePassedMediaIds("child_1") } returns passedVideoMediaIdsFlow
        every { videoWatchLedgerDao.observeTotalAccreditedSeconds("child_1") } returns flowOf(0)
        coEvery { videoWatchLedgerDao.getTotalAccreditedSeconds("child_1") } returns 0
        val playgroundUnlockReceiptDao = mockk<PlaygroundUnlockReceiptDao>()
        every {
            playgroundUnlockReceiptDao.observeByChildAndDay("child_1", any())
        } returns flowOf(null)
        val godModeManager = mockk<GodModeManager>()
        every { godModeManager.isEnabled("child_1") } returns flowOf(godModeEnabled)
        return PlayroomHomeViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to "child_1")),
            catalog = catalog,
            mediaLibrary = mediaLibrary,
            childProfileDao = profileDao,
            lessonCompletionDao = completionDao,
            badgeAwarder = awarder,
            rewardDao = rewardDao,
            inventoryDao = inventoryDao,
            videoWatchLedgerDao = videoWatchLedgerDao,
            dailyQuestManager = dailyQuestManager,
            godModeManager = godModeManager,
            playgroundUnlockReceiptDao = playgroundUnlockReceiptDao,
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

    private fun mediaCatalogWithTotals(vararg subjectAndCount: Pair<String, Int>): MediaCatalog {
        val assets = subjectAndCount.flatMap { (subject, count) ->
            (1..count).map { index ->
                MediaAsset(
                    mediaId = "$subject-video-$index",
                    title = "Video $index",
                    file = "$subject/$index.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 60,
                    width = 1,
                    height = 1,
                    subjectId = subject,
                    episodeNumber = index,
                )
            }
        }
        return MediaCatalog(catalogVersion = 1, generatedAt = "test", media = assets)
    }

    private fun profile(name: String) = ChildProfileEntity(
        id = "child_1", parentId = "parent_1", name = name,
        avatarId = "cat_orange_default", grade = 3, curriculum = "ph-matatag",
        createdAt = 0L,
    )

    private fun badge(id: String, collected: Boolean) = com.maxinesworld.coremodel.CollectibleBadge(
        id = id, biome = "test", name = "Badge $id", title = "T", funFact = "F",
        isCollected = collected,
    )
}
