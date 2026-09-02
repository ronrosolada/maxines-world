package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaCatalog
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredatabase.GodModeManager
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.RewardBreakDao
import com.maxinesworld.coredatabase.RewardBreakEntitlementEntity
import com.maxinesworld.coredatabase.RewardBreakPolicy
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import io.mockk.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
    fun `per-subject video progress uses passed media ids and active media total`() = runTest(dispatcher) {
        val vm = buildViewModel(
            videoCatalog = mediaCatalogWithTotals("mathematics" to 6),
            passedVideoMediaIds = (1..3).map { "mathematics-video-$it" },
        )
        advanceUntilIdle()
        val mathematics = content(vm).subjects.first { it.id == "mathematics" }
        assertEquals(3, mathematics.completedVideos)
        assertEquals(6, mathematics.totalVideos)
    }

    @Test
    fun `daily mission targets use media titles durations and passed media state`() = runTest(dispatcher) {
        val vm = buildViewModel(
            quest = ChallengeProgress(completedCount = 1, subjectCount = 2),
            videoCatalog = mediaCatalogWithTotals(
                "mathematics" to 1,
                "english" to 1,
                "science" to 1,
            ),
            passedVideoMediaIds = listOf("mathematics-video-1"),
        )
        advanceUntilIdle()

        val quest = content(vm).quest
        assertEquals(3, quest.targets.size)
        assertEquals("mathematics-video-1", quest.targets.first().mediaId)
        assertEquals("Video 1", quest.targets.first().title)
        assertEquals(60, quest.targets.first().durationSeconds)
        assertEquals("01:00", quest.targets.first().durationLabel)
        assertTrue(quest.targets.first().isCompleted)
        assertEquals("english-video-1", quest.nextTargetId)
    }

    @Test
    fun `arena reward metadata emission refreshes daily mission inputs`() = runTest(dispatcher) {
        val rewards = MutableStateFlow<List<RewardEntity>>(emptyList())
        val ensureCalls = AtomicInteger()
        val vm = buildViewModel(
            arenaRewardsFlow = rewards,
            dailyQuestEnsureObserver = { ensureCalls.incrementAndGet() },
        )
        advanceUntilIdle()
        val callsBeforeArenaPass = ensureCalls.get()

        rewards.value = listOf(
            RewardEntity(
                id = "assessment-arena:child_1:science-g3-ph:STAR",
                childId = "child_1",
                type = "STAR",
                subject = "science",
                amount = 10,
                metadata = "assessment_arena_passed:science-g3-ph",
            ),
        )
        advanceUntilIdle()

        assertTrue(ensureCalls.get() > callsBeforeArenaPass)
    }

    @Test
    fun `missing video catalog does not expose stale video progress`() = runTest(dispatcher) {
        val vm = buildViewModel(
            videoCatalogLoadFails = true,
        )
        advanceUntilIdle()
        val mathematics = content(vm).subjects.first { it.id == "mathematics" }
        assertEquals(null, mathematics.completedVideos)
        assertEquals(null, mathematics.totalVideos)
    }

    @Test
    fun `empty video mission is truthful and retryable when catalog is unavailable`() = runTest(dispatcher) {
        val vm = buildViewModel(videoCatalogLoadFails = true)
        advanceUntilIdle()

        val quest = content(vm).quest
        assertEquals(QuestTaskCopy.Unavailable, quest.task)
        assertEquals(QuestButtonLabel.Retry, quest.buttonLabel)
        assertEquals(QuestAction.RetryMission, quest.buttonAction)
        assertTrue(quest.targets.isEmpty())
    }

    @Test
    fun `non empty persisted video mission is retryable when catalog metadata is unavailable`() = runTest(dispatcher) {
        val vm = buildViewModel(
            videoCatalogLoadFails = true,
            persistedQuestIds = listOf("mathematics-video-1", "english-video-1"),
        )
        advanceUntilIdle()

        val quest = content(vm).quest
        assertEquals(QuestTaskCopy.Unavailable, quest.task)
        assertEquals(QuestButtonLabel.Retry, quest.buttonLabel)
        assertEquals(QuestAction.RetryMission, quest.buttonAction)
        assertTrue(quest.targets.isEmpty())
    }

    @Test
    fun `disappeared or inactive assigned media cannot route to continue`() = runTest(dispatcher) {
        val catalog = MediaCatalog(
            catalogVersion = 1,
            generatedAt = "refreshed",
            media = listOf(
                MediaAsset(
                    mediaId = "mathematics-video-1",
                    title = "Video 1",
                    file = "mathematics/1.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 60,
                    width = 1,
                    height = 1,
                    subjectId = "mathematics",
                    releaseStatus = "RELEASED",
                ),
                MediaAsset(
                    mediaId = "english-video-1",
                    title = "Retired video",
                    file = "english/1.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 60,
                    width = 1,
                    height = 1,
                    subjectId = "english",
                    releaseStatus = "PREVIEW",
                ),
            ),
        )
        val vm = buildViewModel(
            videoCatalog = catalog,
            persistedQuestIds = listOf("mathematics-video-1", "english-video-1", "science-video-1"),
        )
        advanceUntilIdle()

        val quest = content(vm).quest
        assertEquals(QuestTaskCopy.Unavailable, quest.task)
        assertEquals(QuestButtonLabel.Retry, quest.buttonLabel)
        assertEquals(QuestAction.RetryMission, quest.buttonAction)
        assertTrue(quest.targets.isEmpty())
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
    fun `home accredited seconds ignore preview and other-grade ledger rows`() = runTest(dispatcher) {
        val catalog = MediaCatalog(
            catalogVersion = 1,
            generatedAt = "test",
            media = listOf(
                MediaAsset(
                    mediaId = "g3-math",
                    title = "Grade 3",
                    file = "mathematics/1.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 120,
                    width = 1,
                    height = 1,
                    subjectId = "mathematics",
                    gradeLevel = 3,
                    releaseStatus = "RELEASED",
                ),
                MediaAsset(
                    mediaId = "g1-math",
                    title = "Grade 1",
                    file = "mathematics/g1.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 900,
                    width = 1,
                    height = 1,
                    subjectId = "mathematics",
                    gradeLevel = 1,
                    releaseStatus = "RELEASED",
                ),
                MediaAsset(
                    mediaId = "g3-preview",
                    title = "Preview",
                    file = "mathematics/preview.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 1800,
                    width = 1,
                    height = 1,
                    subjectId = "mathematics",
                    gradeLevel = 3,
                    releaseStatus = "PREVIEW",
                ),
            ),
        )
        val vm = buildViewModel(
            videoCatalog = catalog,
            passedVideoMediaIds = listOf("g3-math", "g1-math", "g3-preview"),
            watchLedger = listOf(
                ledgerEntry("g3-math", accreditedSeconds = 120, quizPassed = true),
                ledgerEntry("g1-math", accreditedSeconds = 900, quizPassed = true),
                ledgerEntry("g3-preview", accreditedSeconds = 1800, quizPassed = true),
            ),
            persistedQuestIds = listOf("g3-math"),
        )
        advanceUntilIdle()

        assertEquals(120, content(vm).totalAccreditedSeconds)
        val mathematics = content(vm).subjects.first { it.id == "mathematics" }
        assertEquals(1, mathematics.completedVideos)
        assertEquals(1, mathematics.totalVideos)
    }

    @Test
    fun `home video progress ignores preview and other-grade catalog rows`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        val derived = withVideoProgress(
            baseContent = content(vm).copy(subjects = canonicalSubjects),
            assets = listOf(
                MediaAsset(
                    mediaId = "g3-math",
                    title = "Grade 3",
                    file = "mathematics/1.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 60,
                    width = 1,
                    height = 1,
                    subjectId = "mathematics",
                    gradeLevel = 3,
                    releaseStatus = "RELEASED",
                ),
                MediaAsset(
                    mediaId = "g1-math",
                    title = "Grade 1",
                    file = "mathematics/g1.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 60,
                    width = 1,
                    height = 1,
                    subjectId = "mathematics",
                    gradeLevel = 1,
                    releaseStatus = "RELEASED",
                ),
                MediaAsset(
                    mediaId = "g3-preview",
                    title = "Preview",
                    file = "mathematics/preview.mp4",
                    sha256 = "",
                    sizeBytes = 1L,
                    durationSeconds = 60,
                    width = 1,
                    height = 1,
                    subjectId = "mathematics",
                    gradeLevel = 3,
                    releaseStatus = "PREVIEW",
                ),
            ),
            passedMediaIds = setOf("g3-math", "g1-math", "g3-preview"),
        )

        val mathematics = derived.subjects.first { it.id == "mathematics" }
        assertEquals(1, mathematics.completedVideos)
        assertEquals(1, mathematics.totalVideos)
    }

    @Test
    fun `latest passed ids win when an older base content emission completes later`() = runTest(dispatcher) {
        val profiles = MutableStateFlow<ChildProfileEntity?>(profile("Maxine"))
        val passedIds = MutableStateFlow(listOf("mathematics-video-1"))

        val vm = buildViewModel(
            videoCatalog = mediaCatalogWithTotals("mathematics" to 2),
            profileFlow = profiles,
            passedVideoMediaIdsFlow = passedIds,
        )
        advanceUntilIdle()
        assertEquals(1, content(vm).subjects.first { it.id == "mathematics" }.completedVideos)

        profiles.value = profile("Updated Maxine")
        passedIds.value = listOf("mathematics-video-1", "mathematics-video-2")
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
    fun `completed quest with unused reward break opens the playground`() = runTest(dispatcher) {
        val entitlement = RewardBreakPolicy.newEntitlement(
            id = "reward-break:child_1:2026-08-04",
            childId = "child_1",
            dailyQuestCompletionId = "child_1:2026-08-04",
            nowEpochMillis = 1_000L,
        )
        val vm = buildViewModel(
            quest = ChallengeProgress(
                english = true, gmrc = true, science = true,
                completedCount = 3, subjectCount = 2, expeditionComplete = true,
            ),
            rewardBreakFlow = flowOf(entitlement),
        )
        advanceUntilIdle()
        val quest = content(vm).quest
        assertTrue(quest.isComplete)
        assertTrue(quest.playgroundUnlocked)
        assertEquals(QuestButtonLabel.OpenPlayground, quest.buttonLabel)
        assertEquals(QuestAction.OpenPlayground, quest.buttonAction)
    }

    @Test
    fun `consumed reward break after quest completion opens sanctuary`() = runTest(dispatcher) {
        val consumed = RewardBreakEntitlementEntity(
            id = "reward-break:child_1:2026-08-04",
            childId = "child_1",
            dailyQuestCompletionId = "child_1:2026-08-04",
            durationMillis = RewardBreakPolicy.DEFAULT_DURATION_MILLIS,
            remainingMillis = 0L,
            createdAtEpochMillis = 1_000L,
            consumedAtEpochMillis = 2_000L,
            state = RewardBreakPolicy.CONSUMED,
        )
        val vm = buildViewModel(
            quest = ChallengeProgress(
                english = true, gmrc = true, science = true,
                completedCount = 3, subjectCount = 2, expeditionComplete = true,
            ),
            rewardBreakFlow = flowOf(consumed),
        )
        advanceUntilIdle()
        val quest = content(vm).quest
        assertTrue(quest.isComplete)
        assertFalse(quest.playgroundUnlocked)
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
            badges = listOf(badge("b1", true), badge("b2", false)),
            godModeEnabled = true,
        )
        advanceUntilIdle()

        val content = content(vm)
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
    fun `child home exposes the live streak from progress timestamps`() = runTest(dispatcher) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val vm = buildViewModel(
            streakTimestamps = listOf(
                timestampFor(today, zone),
                timestampFor(today.minusDays(1), zone),
            ),
        )
        advanceUntilIdle()

        assertEquals(2, content(vm).streakDays)
    }

    @Test
    fun `stale progress becomes a zero streak`() = runTest(dispatcher) {
        val zone = ZoneId.systemDefault()
        val vm = buildViewModel(
            streakTimestamps = listOf(timestampFor(LocalDate.now(zone).minusDays(3), zone)),
        )
        advanceUntilIdle()

        assertEquals(0, content(vm).streakDays)
    }

    @Test
    fun `streak timestamp database errors leave usable home content with zero streak`() = runTest(dispatcher) {
        val vm = buildViewModel(streakTimestampLoadFails = true)
        advanceUntilIdle()

        assertTrue(vm.state.value is PlayroomHomeUiState.Content)
        assertEquals(0, content(vm).streakDays)
    }

    @Test
    fun `missing child and empty progress use safe empty streak state`() = runTest(dispatcher) {
        val vm = buildViewModel(childName = null)
        advanceUntilIdle()

        assertEquals("", content(vm).childName)
        assertEquals(0, content(vm).streakDays)
    }

    @Test
    fun `missing child profile hides today's progress from the streak`() = runTest(dispatcher) {
        val zone = ZoneId.systemDefault()
        val vm = buildViewModel(
            childName = null,
            streakTimestamps = listOf(timestampFor(LocalDate.now(zone), zone)),
        )
        advanceUntilIdle()

        assertEquals(0, content(vm).streakDays)
    }

    @Test
    fun `streak adapter uses the shared live streak definition deterministically`() {
        val zone = ZoneId.of("Asia/Manila")
        val today = LocalDate.of(2026, 8, 5)

        assertEquals(
            0,
            streakDaysFromTimestamps(
                timestamps = listOf(timestampFor(today.minusDays(3), zone)),
                zone = zone,
                today = today,
            ),
        )
    }

    @Test
    fun `same timestamps become stale when evaluated on the next local date`() {
        val zone = ZoneId.of("Asia/Manila")
        val today = LocalDate.of(2026, 8, 5)
        val timestamps = listOf(timestampFor(today.minusDays(1), zone))

        assertEquals(1, streakDaysFromTimestamps(timestamps, zone, today))
        assertEquals(0, streakDaysFromTimestamps(timestamps, zone, today.plusDays(1)))
    }

    @Test
    fun `local date trigger advances at scheduled local midnights without sleeping`() = runTest {
        val zone = ZoneId.of("Asia/Manila")
        val clock = MutableTestClock(Instant.parse("2026-08-05T15:59:59Z"), zone)
        val scheduledDelays = mutableListOf<Long>()

        val dates = localDateChanges(
            zone = zone,
            clock = clock,
            delayUntilNextMidnight = { delayMillis ->
                scheduledDelays += delayMillis
                clock.advanceByMillis(delayMillis)
            },
        ).take(2).toList()

        assertEquals(listOf(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 6)), dates)
        assertEquals(1, scheduledDelays.size)
        assertEquals(1_000L, scheduledDelays.single())
    }

    @Test
    fun `next local midnight delay follows daylight saving transitions`() {
        val zone = ZoneId.of("America/New_York")

        assertEquals(
            23L * 60 * 60 * 1000,
            millisUntilNextLocalMidnight(Instant.parse("2026-03-08T05:00:00Z"), zone),
        )
        assertEquals(
            25L * 60 * 60 * 1000,
            millisUntilNextLocalMidnight(Instant.parse("2026-11-01T04:00:00Z"), zone),
        )
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
        quest: ChallengeProgress = ChallengeProgress(),
        badges: List<com.maxinesworld.coremodel.CollectibleBadge> = emptyList(),
        childName: String? = "Maxine",
        videoCatalog: MediaCatalog = mediaCatalogWithTotals(
            "mathematics" to 1,
            "english" to 1,
            "science" to 1,
        ),
        passedVideoMediaIds: List<String> = emptyList(),
        passedVideoMediaIdsFlow: Flow<List<String>> = flowOf(passedVideoMediaIds),
        profileFlow: Flow<ChildProfileEntity?>? = null,
        videoCatalogLoadFails: Boolean = false,
        starBalance: Int = 0,
        coinBalance: Int = 0,
        streakTimestamps: List<Long> = emptyList(),
        streakTimestampLoadFails: Boolean = false,
        godModeEnabled: Boolean = false,
        shouldFailExpedition: () -> Boolean = { false },
        persistedQuestIds: List<String>? = null,
        watchLedger: List<VideoWatchLedgerEntity> = emptyList(),
        arenaRewardsFlow: Flow<List<RewardEntity>> = flowOf(emptyList()),
        dailyQuestEnsureObserver: (() -> Unit)? = null,
        rewardBreakFlow: Flow<RewardBreakEntitlementEntity?> = flowOf(null),
    ): PlayroomHomeViewModel {
        val profileDao = mockk<ChildProfileDao>()
        coEvery { profileDao.observeById("child_1") } returns (
            profileFlow ?: flowOf(childName?.let(::profile))
        )
        val progressEventDao = mockk<ProgressEventDao>()
        if (streakTimestampLoadFails) {
            every { progressEventDao.observeTimestampsByChild("child_1") } throws
                IllegalStateException("streak timestamps unavailable")
        } else {
            every { progressEventDao.observeTimestampsByChild("child_1") } returns flowOf(streakTimestamps)
        }
        val mediaLibrary = mockk<MediaLibrary>()
        every { mediaLibrary.isDownloaded(any()) } returns false
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
        every { rewardDao.observeByChild("child_1") } returns arenaRewardsFlow
        coEvery { rewardDao.getTotalByType("child_1", "STAR") } returns starBalance
        coEvery { rewardDao.getTotalByType("child_1", "COIN") } returns coinBalance
        coEvery { rewardDao.getByChildAndType("child_1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE) } returns emptyList()
        val dailyQuestManager = mockk<DailyQuestManager>()
        val inventoryDao = mockk<InventoryDao>()
        coEvery { inventoryDao.getOwnedItemIds("child_1") } returns emptyList()
        val assignedQuestIds = persistedQuestIds ?: if (videoCatalogLoadFails) {
            emptyList()
        } else {
            listOf("mathematics-video-1", "english-video-1", "science-video-1")
        }
        val completedQuestIds = assignedQuestIds.take(quest.completedCount.coerceIn(0, 3))
        coEvery { dailyQuestManager.ensureToday("child_1", any(), any(), any(), any()) } coAnswers {
            dailyQuestEnsureObserver?.invoke()
            if (shouldFailExpedition()) throw IllegalStateException("daily quest load failed")
            DailyQuestProgress("2026-08-04", assignedQuestIds, completedQuestIds)
        }
        val videoWatchLedgerDao = mockk<VideoWatchLedgerDao>()
        every { videoWatchLedgerDao.observePassedMediaIds("child_1") } returns passedVideoMediaIdsFlow
        every { videoWatchLedgerDao.observeLedger("child_1") } returns flowOf(watchLedger)
        every { videoWatchLedgerDao.observeTotalAccreditedSeconds("child_1") } returns flowOf(
            watchLedger.filter { it.quizPassed }.sumOf { it.accreditedSeconds },
        )
        coEvery { videoWatchLedgerDao.getTotalAccreditedSeconds("child_1") } returns
            watchLedger.filter { it.quizPassed }.sumOf { it.accreditedSeconds }
        val rewardBreakDao = mockk<RewardBreakDao>()
        every { rewardBreakDao.observeByQuestCompletion(any()) } returns rewardBreakFlow
        val godModeManager = mockk<GodModeManager>()
        every { godModeManager.isEnabled("child_1") } returns flowOf(godModeEnabled)
        val localDateChangeSource = mockk<LocalDateChangeSource>()
        every { localDateChangeSource.observe(any()) } returns flowOf(LocalDate.now())
        return PlayroomHomeViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to "child_1")),
            mediaLibrary = mediaLibrary,
            childProfileDao = profileDao,
            progressEventDao = progressEventDao,
            badgeAwarder = awarder,
            rewardDao = rewardDao,
            inventoryDao = inventoryDao,
            videoWatchLedgerDao = videoWatchLedgerDao,
            dailyQuestManager = dailyQuestManager,
            godModeManager = godModeManager,
            rewardBreakDao = rewardBreakDao,
            localDateChangeSource = localDateChangeSource,
        )
    }

    private fun content(vm: PlayroomHomeViewModel): PlayroomHomeUiState.Content =
        vm.state.value as PlayroomHomeUiState.Content

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
                    releaseStatus = "RELEASED",
                )
            }
        }
        return MediaCatalog(catalogVersion = 1, generatedAt = "test", media = assets)
    }

    private fun ledgerEntry(
        mediaId: String,
        accreditedSeconds: Int,
        quizPassed: Boolean,
    ) = VideoWatchLedgerEntity(
        id = "child_1_$mediaId",
        childId = "child_1",
        mediaId = mediaId,
        subjectId = "mathematics",
        accreditedSeconds = accreditedSeconds,
        quizPassed = quizPassed,
    )

    private fun profile(name: String) = ChildProfileEntity(
        id = "child_1", parentId = "parent_1", name = name,
        avatarId = "cat_orange_default", grade = 3, curriculum = "ph-matatag",
        createdAt = 0L,
    )

    private fun timestampFor(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun badge(id: String, collected: Boolean) = com.maxinesworld.coremodel.CollectibleBadge(
        id = id, biome = "test", name = "Badge $id", title = "T", funFact = "F",
        isCollected = collected,
    )

    private class MutableTestClock(
        private var current: Instant,
        private val zone: ZoneId,
    ) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableTestClock(current, zone)

        override fun instant(): Instant = current

        fun advanceByMillis(millis: Long) {
            current = current.plusMillis(millis)
        }
    }
}
