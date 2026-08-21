package com.maxinesworld.featurechildhome

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaCatalog
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.corenetwork.MediaCatalogClient
import com.maxinesworld.corenetwork.MediaDownloader
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.corenetwork.MediaStorage
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
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
        assertEquals(2, first.totalCount)
        assertEquals(
            setOf("math-valid-episode-1", "english-valid"),
            first.assignedMediaIds.toSet(),
        )
        assertFalse(first.assignedMediaIds.any {
            it in setOf("math-valid-episode-2", "grade-four", "preview-video", "zero-duration", "blank-subject")
        })
        assertNotNull(database.dailyQuestSetDao().getByChildAndDay("child-1", "2026-08-04"))

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
            "the lowest unpassed episode must become the next frontier",
            "math-valid-episode-2" in nextDay.assignedMediaIds,
        )
        assertFalse("math-valid-episode-1" in nextDay.assignedMediaIds)
    }

    @Test
    fun unavailableCatalogPersistsEmptyVideoMissionWithoutLessonFallback() = runBlocking {
        val manager = managerWithRawCatalog("not valid json")

        val progress = manager.ensureToday("child-1", "2026-08-05")

        assertTrue(progress.assignedMediaIds.isEmpty())
        assertTrue(progress.completedMediaIds.isEmpty())
        val persisted = database.dailyQuestSetDao().getByChildAndDay("child-1", "2026-08-05")
        assertNotNull(persisted)
        assertEquals("[]", persisted?.assignedQuestIds)
    }

    @Test
    fun recoveredCatalogRetriesAnEmptyPersistedMissionWithoutLessonIds() = runBlocking {
        val manager = managerWithRawCatalog("not valid json")
        val dayKey = "2026-08-06"

        val unavailable = manager.ensureToday("child-1", dayKey)
        assertTrue(unavailable.assignedMediaIds.isEmpty())
        assertEquals("[]", database.dailyQuestSetDao().getByChildAndDay("child-1", dayKey)?.assignedQuestIds)

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
        assertEquals(2, recovered.totalCount)
        assertTrue(recovered.assignedMediaIds.all { it.startsWith("recovered-") })
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
            dailyQuestSetDao = database.dailyQuestSetDao(),
            dailyQuestCompletionDao = database.dailyQuestCompletionDao(),
            dailyQuestRewardWriter = DailyQuestRewardWriter(
                database = database,
                dailyQuestSetDao = database.dailyQuestSetDao(),
                dailyQuestCompletionDao = database.dailyQuestCompletionDao(),
                rewardDao = database.rewardDao(),
                rewardBreakDao = database.rewardBreakDao(),
                playgroundUnlockReceiptDao = database.playgroundUnlockReceiptDao(),
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
