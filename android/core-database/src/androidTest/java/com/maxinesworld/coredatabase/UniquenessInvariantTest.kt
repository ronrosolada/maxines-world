package com.maxinesworld.coredatabase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the v7 uniqueness invariants (Phase 4 audit):
 *
 *  collected_badges      UNIQUE (childId, badgeId)   — badge ownership is per-child:
 *                          two children CAN earn the same badge; one child CANNOT
 *                          earn the same badge twice.
 *  inventory             UNIQUE (childId, itemId)    — item ownership is per-child.
 *  daily_quest_sets      UNIQUE (childId, dayKey)    — one quest set per child/day.
 *  lesson_completions    UNIQUE (childId, lessonId, attemptId) — attempt idempotency.
 *
 * The composite rules are the product contract: global uniqueness would wrongly
 * allow only ONE child in the whole app to own a given badge/item.
 */
@RunWith(AndroidJUnit4::class)
class UniquenessInvariantTest {

    private lateinit var context: Context
    private lateinit var db: MaxinesDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ─── Badge ownership: per-child composite, NOT global ─────────────

    @Test
    fun twoChildrenCanEarnTheSameBadge() = runBlocking {
        val dao = db.collectedBadgeDao()
        dao.insert(
            CollectedBadgeEntity(
                id = "child_1_badge_01", childId = "child_1", badgeId = "badge_01",
                biome = "forest_friends", earnedDate = "2026-08-01", earnedAtEpochMillis = 1L
            )
        )
        dao.insert(
            CollectedBadgeEntity(
                id = "child_2_badge_01", childId = "child_2", badgeId = "badge_01",
                biome = "forest_friends", earnedDate = "2026-08-01", earnedAtEpochMillis = 2L
            )
        )
        assertEquals("child_1 badge count", 1, dao.countByChild("child_1"))
        assertEquals("child_2 badge count", 1, dao.countByChild("child_2"))
    }

    @Test
    fun sameChildCannotEarnSameBadgeTwice() = runBlocking {
        val dao = db.collectedBadgeDao()
        dao.insert(
            CollectedBadgeEntity(
                id = "child_1_badge_01", childId = "child_1", badgeId = "badge_01",
                biome = "forest_friends", earnedDate = "2026-08-01", earnedAtEpochMillis = 1L
            )
        )
        dao.insert(
            CollectedBadgeEntity(
                id = "child_1_badge_01_dup", childId = "child_1", badgeId = "badge_01",
                biome = "forest_friends", earnedDate = "2026-08-02", earnedAtEpochMillis = 2L
            )
        )
        // IGNORE conflict strategy → second insert dropped
        assertEquals("one badge per child", 1, dao.countByChild("child_1"))
        assertEquals(1, dao.countByBiome("child_1", "forest_friends"))
    }

    // ─── Inventory: per-child item ownership ──────────────────────────

    @Test
    fun twoChildrenCanOwnSameItemButOneChildOnce() = runBlocking {
        val dao = db.inventoryDao()
        dao.insertIgnoring(
            InventoryEntity(id = "i1", childId = "child_1", itemId = "fish_treat", acquiredAtEpochMillis = 1L)
        )
        dao.insertIgnoring(
            InventoryEntity(id = "i2", childId = "child_2", itemId = "fish_treat", acquiredAtEpochMillis = 1L)
        )
        dao.insertIgnoring(
            InventoryEntity(id = "i3", childId = "child_1", itemId = "fish_treat", acquiredAtEpochMillis = 2L)
        )

        // Composite unique (childId, itemId): child_1's second insert ignored
        assertTrue("child_1 owns the item", dao.owns("child_1", "fish_treat"))
        assertTrue("child_2 also owns the item", dao.owns("child_2", "fish_treat"))
        assertEquals(
            "child_1 has exactly one row",
            1L,
            db.openHelper.writableDatabase.query(
                "SELECT COUNT(*) FROM inventory WHERE childId = 'child_1' AND itemId = 'fish_treat'"
            ).let { c: android.database.Cursor ->
                c.moveToFirst()
                val n = c.getLong(0)
                c.close()
                n
            }
        )
    }

    // ─── Daily quest sets: one set per child per day ──────────────────

    @Test
    fun oneQuestSetPerChildPerDay() = runBlocking {
        val dao = db.dailyQuestSetDao()
        dao.insertIgnoring(
            DailyQuestSetEntity(
                id = "s1", childId = "child_1", dayKey = "2026-08-01",
                assignedQuestIds = "[\"q1\"]", assignedAtEpochMillis = 1L
            )
        )
        dao.insertIgnoring(
            DailyQuestSetEntity(
                id = "s2", childId = "child_1", dayKey = "2026-08-01",
                assignedQuestIds = "[\"q2\"]", assignedAtEpochMillis = 2L
            )
        )
        // Different child, same day → allowed
        dao.insertIgnoring(
            DailyQuestSetEntity(
                id = "s3", childId = "child_2", dayKey = "2026-08-01",
                assignedQuestIds = "[\"q1\"]", assignedAtEpochMillis = 3L
            )
        )
        // First insert wins under IGNORE (id s1, q1); second same-child insert dropped
        val child1Set = dao.getByChildAndDay("child_1", "2026-08-01")
        assertNotNull("child_1 has one set for the day", child1Set)
        assertEquals("first set wins", "[\"q1\"]", child1Set!!.assignedQuestIds)
        assertEquals("child_2 has its own set", "[\"q1\"]", dao.getByChildAndDay("child_2", "2026-08-01")!!.assignedQuestIds)
        assertEquals(
            "exactly one row per child/day",
            2L,
            db.openHelper.writableDatabase.query(
                "SELECT COUNT(*) FROM daily_quest_sets"
            ).let { c: android.database.Cursor ->
                c.moveToFirst()
                val n = c.getLong(0)
                c.close()
                n
            }
        )
    }

    // ─── Lesson completions: attempt-level idempotency ────────────────

    @Test
    fun lessonCompletionUniquePerChildLessonAttempt() = runBlocking {
        val dao = db.lessonCompletionDao()
        // Same child, same lesson, different attempts → allowed (distinct rows)
        dao.insertIgnoring(
            LessonCompletionEntity(
                id = "lc1", childId = "child_1", lessonId = "lesson_001",
                attemptId = "att_1", accuracy = 0.9, completedAtEpochMillis = 1L
            )
        )
        dao.insertIgnoring(
            LessonCompletionEntity(
                id = "lc2", childId = "child_1", lessonId = "lesson_001",
                attemptId = "att_2", accuracy = 1.0, completedAtEpochMillis = 2L
            )
        )
        // Same attempt again → IGNORE
        dao.insertIgnoring(
            LessonCompletionEntity(
                id = "lc3", childId = "child_1", lessonId = "lesson_001",
                attemptId = "att_1", accuracy = 1.0, completedAtEpochMillis = 3L
            )
        )
        assertNotNull(dao.getByAttempt("child_1", "lesson_001", "att_1"))
        assertNotNull(dao.getByAttempt("child_1", "lesson_001", "att_2"))
        // lc3 was ignored: att_1 still points at the original row
        val original = dao.getByAttempt("child_1", "lesson_001", "att_1")
        assertEquals(0.9, original!!.accuracy, 0.001)
        assertEquals(1L, original.completedAtEpochMillis)
        // Duplicate attempt not double-counted
        assertEquals("two distinct attempts", 1, dao.observeDistinctLessonCount("child_1").first())
    }
}
