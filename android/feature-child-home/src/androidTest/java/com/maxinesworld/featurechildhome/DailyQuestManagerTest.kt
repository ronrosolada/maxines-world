package com.maxinesworld.featurechildhome

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaCatalog
import com.maxinesworld.coremodel.VideoQuestPlanner
import com.maxinesworld.coredatabase.DailyQuestCompletionEntity
import com.maxinesworld.coredatabase.DailyQuestSetEntity
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
import com.maxinesworld.corecontent.AssessmentRepository
import com.maxinesworld.corenetwork.MediaCatalogClient
import com.maxinesworld.corenetwork.MediaDownloader
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.corenetwork.MediaStorage
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import com.maxinesworld.coredatabase.RewardEntity
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyQuestManagerTest {
    private lateinit var database: MaxinesDatabase
    private lateinit var storageRoot: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storageRoot = File(context.cacheDir, "daily-quest-test-${System.nanoTime()}")
    }

    @After
    fun tearDown() {
        database.close()
        storageRoot.deleteRecursively()
    }

    @Test
    fun releasedGradeThreeFrontierFiltersMixedCatalogAndCreditsPassedMedia() = runBlocking {
        val catalog = listOf(
            asset("math-valid-episode-1", "mathematics", 1, 900),
            asset("math-valid-episode-2", "mathematics", 2, 900),
            asset("english-valid", "english", 1, 900),
            asset("zero-duration", "science", 1, 0),
            asset("grade-four", "filipino", 1, 900, grade = 4),
            asset("preview-video", "filipino", 2, 900, releaseStatus = "PREVIEW"),
            asset("blank-subject", "", 1, 900),
        )
        val manager = manager(catalog)

        val first = manager.ensureToday("child-1", "2026-08-04")
        assertEquals(3, first.totalCount)
        assertEquals(
            setOf("math-valid-episode-1", "arena:math-g3-ph", "arena:math-g3-sg"),
            first.assignedMediaIds.toSet(),
        )
        assertTrue(first.arenaPacks.all { it.title.startsWith("Grade 3") })
        assertFalse(first.assignedMediaIds.any {
            it in setOf("math-valid-episode-2", "grade-four", "preview-video", "zero-duration", "blank-subject")
        })
        assertNotNull(database.dailyQuestSetDao().getByChildAndDay("child-1", "2026-08-04"))

        first.assignedMediaIds.filterNot { it.startsWith("arena:") }.forEach { mediaId ->
            database.videoWatchLedgerDao().insertOrUpdate(
                VideoWatchLedgerEntity(
                    id = "child-1:$mediaId",
                    childId = "child-1",
                    mediaId = mediaId,
                    subjectId = if (mediaId.startsWith("math")) "mathematics" else "english",
                    accreditedSeconds = 900,
                    quizPassed = true,
                ),
            )
        }
        listOf("math-g3-ph", "math-g3-sg").forEachIndexed { index, packId ->
            database.rewardDao().insert(
                RewardEntity(
                    id = "arena:child-1:$packId",
                    childId = "child-1",
                    type = "STAR",
                    subject = "mathematics",
                    amount = 10,
                    earnedAt = index.toLong(),
                    metadata = "assessment_arena_passed:$packId",
                ),
            )
        }
        val afterPass = manager.ensureToday(
            childId = "child-1",
            dayKey = "2026-08-04",
            passedMediaIds = first.assignedMediaIds.toSet(),
        )
        assertEquals(first.assignedMediaIds, afterPass.assignedMediaIds)
        assertEquals(first.assignedMediaIds.toSet(), afterPass.completedMediaIds.toSet())
        assertEquals(first.totalCount, afterPass.completedCount)
        assertEquals(
            1,
            database.rewardDao().getTotalByType("child-1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE),
        )

        val nextDay = manager.ensureToday(
            childId = "child-1",
            dayKey = "2026-08-05",
            passedMediaIds = setOf("math-valid-episode-1"),
        )
        assertTrue(
            "the lowest unpassed episode must remain the next math frontier when selected",
            "math-valid-episode-2" in nextDay.assignedMediaIds || "english-valid" in nextDay.assignedMediaIds,
        )
        assertFalse("math-valid-episode-1" in nextDay.assignedMediaIds)
    }

    @Test
    fun oneUnpassedArenaPackUsesASecondDeterministicFrontierVideo() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packs = AssessmentRepository(context).getCatalog().packs
        packs.filter { it.title.startsWith("Grade 3") && it.id != "science-g3-ph" }
            .forEachIndexed { index, pack ->
                database.rewardDao().insert(
                    RewardEntity(
                        id = "arena:child-1:${pack.id}",
                        childId = "child-1",
                        type = "STAR",
                        subject = pack.subjectId,
                        amount = 10,
                        earnedAt = index.toLong(),
                        metadata = "assessment_arena_passed:${pack.id}",
                    ),
                )
            }
        val progress = manager(
            listOf(
                asset("math-frontier", "mathematics", 1, 1200),
                asset("science-frontier", "science", 1, 1200),
            ),
        ).ensureToday("child-1", "2026-08-11")

        assertEquals(3, progress.totalCount)
        assertEquals(2, progress.assignedMediaIds.count { !it.startsWith("arena:") })
        assertEquals(1, progress.assignedMediaIds.count { it.startsWith("arena:") })
        assertTrue(progress.assignedMediaIds.containsAll(listOf("math-frontier", "science-frontier")))
    }

    @Test
    fun orderedFallbackAddsASecondVideoWhenPlannerReturnsOne() {
        val frontier = listOf(
            VideoQuestPlanner.Candidate("planner-video", "mathematics", 900),
            VideoQuestPlanner.Candidate("fallback-video", "english", 900),
        )

        assertEquals(
            listOf("planner-video", "fallback-video"),
            composeDailyQuestIds(
                plannerVideoIds = listOf("planner-video"),
                frontier = frontier,
                arenaIds = emptyList(),
            ),
        )
    }

    @Test
    fun emptyVideoCatalogFallsBackToAssessmentArenaDailyQuests() = runBlocking {
        val progress = manager(emptyList()).ensureToday("child-1", "2026-08-23")

        assertEquals(3, progress.totalCount)
        assertTrue("All assigned quests should be arena packs when video is offline", progress.assignedMediaIds.all { it.startsWith("arena:") })
        assertEquals(3, progress.arenaPacks.size)
    }

    @Test
    fun zeroUnpassedArenaPacksUsesOnlyThePlannerVideoFrontier() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AssessmentRepository(context).getCatalog().packs
            .filter { it.title.startsWith("Grade 3") }
            .forEachIndexed { index, pack ->
                database.rewardDao().insert(
                    RewardEntity(
                        id = "arena:child-1:${pack.id}",
                        childId = "child-1",
                        type = "STAR",
                        subject = pack.subjectId,
                        amount = 10,
                        earnedAt = index.toLong(),
                        metadata = "assessment_arena_passed:${pack.id}",
                    ),
                )
            }

        val progress = manager(
            listOf(
                asset("math-frontier", "mathematics", 1, 900),
                asset("english-frontier", "english", 1, 900),
                asset("science-frontier", "science", 1, 900),
            ),
        ).ensureToday("child-1", "2026-08-12")

        assertEquals(3, progress.totalCount)
        assertEquals(3, progress.assignedMediaIds.count { !it.startsWith("arena:") })
        assertTrue(progress.assignedMediaIds.none { it.startsWith("arena:") })
    }

    @Test
    fun sparseOneVideoWithTwoUnpassedArenaPacksStillComposesMission() = runBlocking {
        val progress = manager(
            listOf(asset("only-frontier", "science", 1, 600)),
        ).ensureToday("child-1", "2026-08-13")

        assertEquals(3, progress.totalCount)
        assertEquals(listOf("only-frontier"), progress.assignedMediaIds.filterNot { it.startsWith("arena:") })
        assertEquals(2, progress.assignedMediaIds.count { it.startsWith("arena:") })
    }

    @Test
    fun legacyLessonAssignmentIsUpgradedBeforeDisplayOrReward() = runBlocking {
        val dayKey = "2026-08-07"
        database.dailyQuestSetDao().insertIgnoring(
            DailyQuestSetEntity(
                id = "child-1:$dayKey",
                childId = "child-1",
                dayKey = dayKey,
                assignedQuestIds = "[\"english-g3-m01-d01\"]",
            ),
        )
        database.dailyQuestCompletionDao().insertIgnoring(
            DailyQuestCompletionEntity(
                id = "child-1:$dayKey:english-g3-m01-d01",
                childId = "child-1",
                dayKey = dayKey,
                questId = "english-g3-m01-d01",
                completionEventId = "lesson-completion:child-1:english-g3-m01-d01",
            ),
        )

        val progress = manager(
            listOf(
                asset("replacement-math", "mathematics", 1, 900),
                asset("replacement-english", "english", 1, 900),
            ),
        ).ensureToday("child-1", dayKey)

        assertFalse("legacy lesson IDs must not remain assigned", progress.assignedMediaIds.contains("english-g3-m01-d01"))
        assertTrue(progress.assignedMediaIds.filterNot { it.startsWith("arena:") }.all { it.startsWith("replacement-") })
        assertTrue(progress.completedMediaIds.isEmpty())
        assertEquals(
            0,
            database.rewardDao().getTotalByType("child-1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE) ?: 0,
        )
    }

    @Test
    fun nonEmptyPersistedVideoMissionSurvivesCatalogOutage() = runBlocking {
        val dayKey = "2026-08-08"
        val assigned = listOf("persisted-math-video", "persisted-english-video")
        database.dailyQuestSetDao().insertIgnoring(
            DailyQuestSetEntity(
                id = "child-1:$dayKey",
                childId = "child-1",
                dayKey = dayKey,
                assignedQuestIds = Json.encodeToString(assigned),
            ),
        )

        val progress = managerWithRawCatalog("not valid json").ensureToday("child-1", dayKey)

        assertEquals(assigned, progress.assignedMediaIds)
        assertEquals(Json.encodeToString(assigned), database.dailyQuestSetDao()
            .getByChildAndDay("child-1", dayKey)?.assignedQuestIds)
        assertEquals(0, database.rewardDao().getTotalByType("child-1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE) ?: 0)
    }

    @Test
    fun disappearedOrInactiveAssignedMediaKeepsDurableAssignmentDeterministically() = runBlocking {
        val dayKey = "2026-08-10"
        val assigned = listOf("persisted-math-video", "persisted-english-video")
        database.dailyQuestSetDao().insertIgnoring(
            DailyQuestSetEntity(
                id = "child-1:$dayKey",
                childId = "child-1",
                dayKey = dayKey,
                assignedQuestIds = Json.encodeToString(assigned),
            ),
        )

        val refreshed = manager(
            listOf(asset("persisted-math-video", "mathematics", 1, 900, releaseStatus = "PREVIEW")),
        ).ensureToday("child-1", dayKey)

        assertEquals(assigned, refreshed.assignedMediaIds)
        assertTrue(refreshed.completedMediaIds.isEmpty())
        assertEquals(0, database.rewardDao().getTotalByType("child-1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE) ?: 0)
    }

    @Test
    fun unavailableCatalogFallsBackToAssessmentArenaWithoutCrash() = runBlocking {
        val manager = managerWithRawCatalog("not valid json")

        val progress = manager.ensureToday("child-1", "2026-08-05")

        assertEquals(3, progress.totalCount)
        assertTrue("Fallback to Assessment Arena when video catalog is unavailable", progress.assignedMediaIds.all { it.startsWith("arena:") })
        val persisted = database.dailyQuestSetDao().getByChildAndDay("child-1", "2026-08-05")
        assertNotNull(persisted)
    }

    @Test
    fun recoveredCatalogRetriesAnEmptyPersistedMissionWithoutLessonIds() = runBlocking {
        val manager = managerWithRawCatalog("not valid json")
        val dayKey = "2026-08-06"
        // Seed an empty persisted mission explicitly
        database.dailyQuestSetDao().insertIgnoring(
            DailyQuestSetEntity(
                id = "child-1:$dayKey",
                childId = "child-1",
                dayKey = dayKey,
                assignedQuestIds = "[]",
            ),
        )

        val unrecovered = manager.ensureToday("child-1", dayKey)
        // With empty persisted mission and valid arena packs, it upgrades cleanly
        assertTrue(unrecovered.assignedMediaIds.isNotEmpty())

        MediaStorage(storageRoot).writeCatalog(
            Json.encodeToString(
                MediaCatalog(
                    catalogVersion = 1,
                    generatedAt = "recovered",
                    media = listOf(
                        asset("recovered-math", "mathematics", 1, 900),
                        asset("recovered-english", "english", 1, 900),
                    ),
                ),
            ),
        )

        val recovered = manager.ensureToday("child-1", dayKey)
        assertEquals(3, recovered.totalCount)
        assertTrue(recovered.assignedMediaIds.filterNot { it.startsWith("arena:") }.all { it.startsWith("recovered-") })
        assertFalse(recovered.assignedMediaIds.any { it.contains("lesson") })
        assertTrue(
            database.dailyQuestSetDao()
                .getByChildAndDay("child-1", dayKey)
                ?.assignedQuestIds != "[]",
        )
    }

    private fun manager(catalog: List<MediaAsset>): DailyQuestManager =
        managerWithRawCatalog(
            Json.encodeToString(MediaCatalog(catalogVersion = 1, generatedAt = "test", media = catalog)),
        )

    private fun managerWithRawCatalog(rawCatalog: String): DailyQuestManager {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = MediaStorage(storageRoot)
        storage.writeCatalog(rawCatalog)
        val client = MediaCatalogClient(OkHttpClient())
        val mediaLibrary = MediaLibrary(
            catalogUrl = "http://127.0.0.1:1/catalog.json",
            mediaBaseUrl = "http://127.0.0.1:1/media",
            catalogClient = client,
            downloader = MediaDownloader(OkHttpClient(), storage),
            storage = storage,
        )
        return DailyQuestManager(
            mediaLibrary = mediaLibrary,
            assessmentRepository = AssessmentRepository(context),
            dailyQuestSetDao = database.dailyQuestSetDao(),
            dailyQuestCompletionDao = database.dailyQuestCompletionDao(),
            masteryRecordDao = database.masteryRecordDao(),
            rewardDao = database.rewardDao(),
            dailyQuestRewardWriter = DailyQuestRewardWriter(
                database = database,
                dailyQuestSetDao = database.dailyQuestSetDao(),
                dailyQuestCompletionDao = database.dailyQuestCompletionDao(),
                rewardDao = database.rewardDao(),
                rewardBreakDao = database.rewardBreakDao(),
                playgroundUnlockReceiptDao = database.playgroundUnlockReceiptDao(),
                videoWatchLedgerDao = database.videoWatchLedgerDao(),
            ),
        )
    }

    private fun asset(
        mediaId: String,
        subject: String,
        episode: Int,
        seconds: Int,
        grade: Int = 3,
        releaseStatus: String = "RELEASED",
    ) = MediaAsset(
        mediaId = mediaId,
        title = mediaId,
        file = "media/$mediaId.mp4",
        sha256 = "0".repeat(64),
        sizeBytes = 1L,
        durationSeconds = seconds,
        width = 1,
        height = 1,
        subjectId = subject,
        gradeLevel = grade,
        episodeNumber = episode,
        releaseStatus = releaseStatus,
    )
}
