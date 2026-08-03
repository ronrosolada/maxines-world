package com.maxinesworld.coredatabase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tests the persisted source of Kindness unlock progress:
 * COUNT(DISTINCT lessonId) over lesson_completions.
 *
 * These guard the Phase-2 invariants:
 *  - replay safety: attempts of the same lesson count ONCE
 *  - boundary: 11 distinct = locked, 12 = unlocked (via the DAO the ViewModel consumes)
 *  - cross-child isolation: one child's completions never inflate another's count
 *  - malformed/unrelated records never inflate progress
 *  - persistence: counts survive a full database reopen (process restart equivalent)
 */
@RunWith(AndroidJUnit4::class)
class LessonCompletionDaoTest {

    private lateinit var context: Context
    private lateinit var db: MaxinesDatabase
    private lateinit var dao: LessonCompletionDao
    private var useFileDb = false
    private var dbFileName: String? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lessonCompletionDao()
    }

    @After
    fun tearDown() {
        db.close()
        dbFileName?.let { context.deleteDatabase(it) }
    }

    private fun completion(childId: String, lessonId: String, attemptId: String, accuracy: Double = 1.0) =
        LessonCompletionEntity(
            id = "$childId:$lessonId:$attemptId",
            childId = childId,
            lessonId = lessonId,
            attemptId = attemptId,
            accuracy = accuracy,
            completedAtEpochMillis = 1_700_000_000_000L,
        )

    // ─── Replay safety ────────────────────────────────────────────────

    @Test
    fun replayingSameLessonMultipleAttemptsCountsOnce() = runBlocking {
        dao.insertIgnoring(completion("child_1", "english-g3-m01-d01", "att_1"))
        dao.insertIgnoring(completion("child_1", "english-g3-m01-d01", "att_2"))
        dao.insertIgnoring(completion("child_1", "english-g3-m01-d01", "att_3"))

        assertEquals("3 attempts of 1 lesson = 1 distinct", 1, dao.observeDistinctLessonCount("child_1").first())
    }

    @Test
    fun countDistinctLessonsMatchesFlowVariant() = runBlocking {
        dao.insertIgnoring(completion("child_1", "lesson-a", "att_1"))
        dao.insertIgnoring(completion("child_1", "lesson-b", "att_1"))
        dao.insertIgnoring(completion("child_1", "lesson-a", "att_2"))

        assertEquals("suspend count agrees with the Flow count", 2, dao.countDistinctLessons("child_1"))
        assertEquals(2, dao.observeDistinctLessonCount("child_1").first())
    }

    @Test
    fun countDistinctLessonsIsZeroForNewChild() = runBlocking {
        assertEquals("brand-new child has no lessons yet", 0, dao.countDistinctLessons("child_fresh"))
    }

    @Test
    fun duplicateAttemptIdIsIgnoredNotDoubleCounted() = runBlocking {
        // Same (childId, lessonId, attemptId) — unique index → IGNORE, no new row
        dao.insertIgnoring(completion("child_1", "english-g3-m01-d01", "att_1"))
        dao.insertIgnoring(completion("child_1", "english-g3-m01-d01", "att_1"))

        assertEquals(1, dao.observeDistinctLessonCount("child_1").first())
    }

    // ─── Boundary: 11 vs 12 distinct lessons ─────────────────────────

    @Test
    fun elevenDistinctLessonsCountsAsEleven() = runBlocking {
        repeat(11) { i ->
            dao.insertIgnoring(completion("child_1", "lesson-$i", "att_$i"))
        }
        val count = dao.observeDistinctLessonCount("child_1").first()
        assertEquals(11, count)
        // Policy boundary: locked at 11
        assertTrue(com.maxinesworld.coremodel.ChildLevelPolicy.levelFor(count) < 4)
    }

    @Test
    fun twelveDistinctLessonsCountsAsTwelve() = runBlocking {
        repeat(12) { i ->
            dao.insertIgnoring(completion("child_1", "lesson-$i", "att_$i"))
        }
        val count = dao.observeDistinctLessonCount("child_1").first()
        assertEquals(12, count)
        // Policy boundary: unlocked at 12
        assertTrue(com.maxinesworld.coremodel.ChildLevelPolicy.levelFor(count) >= 4)
    }

    // ─── Cross-child isolation ────────────────────────────────────────

    @Test
    fun otherChildrenCompletionsDoNotInflateCount() = runBlocking {
        dao.insertIgnoring(completion("child_1", "lesson-a", "att_1"))
        dao.insertIgnoring(completion("child_2", "lesson-x", "att_1"))
        dao.insertIgnoring(completion("child_2", "lesson-y", "att_1"))
        dao.insertIgnoring(completion("child_2", "lesson-z", "att_1"))

        assertEquals("child_1 sees only its own lesson", 1, dao.observeDistinctLessonCount("child_1").first())
        assertEquals("child_2 sees 3 distinct", 3, dao.observeDistinctLessonCount("child_2").first())
    }

    // ─── Malformed / unrelated records ────────────────────────────────

    @Test
    fun emptyLessonIdDoesNotInflateCount() = runBlocking {
        dao.insertIgnoring(completion("child_1", "lesson-a", "att_1"))
        // Malformed row: blank lessonId — should not be counted as real progress
        dao.insertIgnoring(completion("child_1", "", "att_bad"))

        assertEquals(1, dao.observeDistinctLessonCount("child_1").first())
    }

    @Test
    fun zeroAccuracyAttemptDoesNotInflateCount() = runBlocking {
        dao.insertIgnoring(completion("child_1", "lesson-a", "att_1", accuracy = 0.0))
        dao.insertIgnoring(completion("child_1", "lesson-a", "att_2", accuracy = 0.0))

        // Even failed-quality attempts of the same lesson count once (distinct lesson)
        assertEquals(1, dao.observeDistinctLessonCount("child_1").first())
    }

    // ─── Persistence across reopen (process-restart equivalent) ──────

    @Test
    fun countPersistsAcrossDatabaseReopen() = runBlocking {
        val fileName = "persist_test_${System.currentTimeMillis()}"
        dbFileName = fileName
        val fileDb = Room.databaseBuilder(context, MaxinesDatabase::class.java, fileName)
            .allowMainThreadQueries()
            .build()
        fileDb.lessonCompletionDao().insertIgnoring(completion("child_1", "lesson-a", "att_1"))
        fileDb.lessonCompletionDao().insertIgnoring(completion("child_1", "lesson-b", "att_1"))
        fileDb.close()

        // Simulate process restart: fresh database instance on same file
        val reopened = Room.databaseBuilder(context, MaxinesDatabase::class.java, fileName)
            .allowMainThreadQueries()
            .build()
        try {
            val count = reopened.lessonCompletionDao().observeDistinctLessonCount("child_1").first()
            assertEquals("distinct count survives reopen", 2, count)
            assertFalse("no stray rows for other child", reopened.lessonCompletionDao().exists("child_2", "lesson-a"))
        } finally {
            reopened.close()
        }
    }
}
