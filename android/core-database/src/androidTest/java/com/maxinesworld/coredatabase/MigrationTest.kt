package com.maxinesworld.coredatabase

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Tests Room database migrations from v1 → v2 → v3,
 * verifying data preservation across schema changes.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration_test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MaxinesDatabase::class.java,
        emptyList(), // No manual migrations — Room auto-migrates from schemas
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setUp() {
        // Ensure clean state
        InstrumentationRegistry.getInstrumentation().targetContext
            .deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .deleteDatabase(TEST_DB)
    }

    // ─────────────────────────────────────────────
    // Migrate v1 → v2 → v3 preserving existing data
    // ─────────────────────────────────────────────

    @Test
    @Throws(IOException::class)
    fun migrate1to2to3_preservesData() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testId = "migration_test_child"

        // ── Phase 1: Create v1 database and insert data ──
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            // parent_accounts
            execSQL("""
                INSERT INTO parent_accounts (id, displayName, pinHash, biometricEnabled, createdAt)
                VALUES ('parent_1', 'Test Parent', 'hash_abc', 0, 1700000000000)
            """.trimIndent())

            // child_profiles
            execSQL("""
                INSERT INTO child_profiles (id, parentId, name, avatarId, grade, curriculum, createdAt)
                VALUES ('$testId', 'parent_1', 'Test Child', 'cat_orange_default', 3, 'ph-matatag', 1700000000000)
            """.trimIndent())

            // progress_events
            execSQL("""
                INSERT INTO progress_events (id, childId, skillId, lessonId, activityId, eventType, accuracy, attempts, hintsUsed, responseTimeMs, timestamp, syncStatus)
                VALUES ('prog_1', '$testId', 'skill_001', 'lesson_001', 'act_001', 'COMPLETED', 0.85, 2, 1, 5000, 1700000000000, 'PENDING')
            """.trimIndent())

            // mastery_records
            execSQL("""
                INSERT INTO mastery_records (id, childId, skillId, state, accuracy, totalAttempts, lastActivityAt, nextReviewAt)
                VALUES ('${testId}_skill_001', '$testId', 'skill_001', 'PRACTICING', 0.75, 10, 1700000000000, 1700086400000)
            """.trimIndent())

            // rewards
            execSQL("""
                INSERT INTO rewards (id, childId, type, subject, amount, earnedAt, metadata)
                VALUES ('reward_1', '$testId', 'STAR', 'english', 5, 1700000000000, '{}')
            """.trimIndent())

            // screen_time_limits
            execSQL("""
                INSERT INTO screen_time_limits (id, childId, dayType, limitMinutes, downtimeStart, downtimeEnd)
                VALUES ('${testId}_weekday', '$testId', 'weekday', 120, '19:30', '07:00')
            """.trimIndent())

            // daily_quests
            execSQL("""
                INSERT INTO daily_quests (id, childId, date, subjectRotations, completedLessons, energyEarned)
                VALUES ('${testId}_2026-07-13', '$testId', '2026-07-13', '["english","filipino","mathematics","science","makabansa"]', '["lesson_001"]', 15)
            """.trimIndent())

            close()
        }

        // ── Phase 2: Migrate to v2, verify v1 data preserved + new tables exist ──
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        // Verify v1 data survived migration
        val parentCursor = dbV2.query("SELECT * FROM parent_accounts WHERE id = 'parent_1'")
        assertTrue("parent account preserved", parentCursor.moveToFirst())
        assertEquals("parent displayName", "Test Parent", parentCursor.getString(parentCursor.getColumnIndexOrThrow("displayName")))
        assertEquals("parent pinHash", "hash_abc", parentCursor.getString(parentCursor.getColumnIndexOrThrow("pinHash")))
        parentCursor.close()

        val childCursor = dbV2.query("SELECT * FROM child_profiles WHERE id = '$testId'")
        assertTrue("child profile preserved", childCursor.moveToFirst())
        assertEquals("child name", "Test Child", childCursor.getString(childCursor.getColumnIndexOrThrow("name")))
        assertEquals("child grade", 3, childCursor.getInt(childCursor.getColumnIndexOrThrow("grade")))
        childCursor.close()

        val progressCursor = dbV2.query("SELECT * FROM progress_events WHERE id = 'prog_1'")
        assertTrue("progress event preserved", progressCursor.moveToFirst())
        assertEquals("event type", "COMPLETED", progressCursor.getString(progressCursor.getColumnIndexOrThrow("eventType")))
        assertEquals("accuracy", 0.85, progressCursor.getDouble(progressCursor.getColumnIndexOrThrow("accuracy")), 0.001)
        progressCursor.close()

        val masteryCursor = dbV2.query("SELECT * FROM mastery_records WHERE childId = '$testId'")
        assertTrue("mastery record preserved", masteryCursor.moveToFirst())
        assertEquals("mastery state", "PRACTICING", masteryCursor.getString(masteryCursor.getColumnIndexOrThrow("state")))
        masteryCursor.close()

        val rewardCursor = dbV2.query("SELECT * FROM rewards WHERE id = 'reward_1'")
        assertTrue("reward preserved", rewardCursor.moveToFirst())
        assertEquals("reward type", "STAR", rewardCursor.getString(rewardCursor.getColumnIndexOrThrow("type")))
        assertEquals("reward amount", 5, rewardCursor.getInt(rewardCursor.getColumnIndexOrThrow("amount")))
        rewardCursor.close()

        val screenTimeCursor = dbV2.query("SELECT * FROM screen_time_limits WHERE childId = '$testId'")
        assertTrue("screen time limit preserved", screenTimeCursor.moveToFirst())
        assertEquals("limit minutes", 120, screenTimeCursor.getInt(screenTimeCursor.getColumnIndexOrThrow("limitMinutes")))
        screenTimeCursor.close()

        val questCursor = dbV2.query("SELECT * FROM daily_quests WHERE childId = '$testId'")
        assertTrue("daily quest preserved", questCursor.moveToFirst())
        assertEquals("quest date", "2026-07-13", questCursor.getString(questCursor.getColumnIndexOrThrow("date")))
        questCursor.close()

        // Verify v2 tables exist and can be written to
        dbV2.execSQL("""
            INSERT INTO reward_break_entitlements (id, childId, dailyQuestCompletionId, durationMillis, remainingMillis, createdAtEpochMillis, state)
            VALUES ('rbe_1', '$testId', '${testId}_2026-07-13', 600000, 600000, 1700000000000, 'CREATED')
        """.trimIndent())

        dbV2.execSQL("""
            INSERT INTO mini_game_results (sessionId, idempotencyKey, rewardBreakId, childId, gameId, startedAtEpochMillis, endedAtEpochMillis, roundsCompleted, successfulActions, pawTokensEarned)
            VALUES ('session_1', 'idem_1', 'rbe_1', '$testId', 'pawprint_parkour', 1700000000000, 1700000060000, 3, 15, 30)
        """.trimIndent())

        // Verify v2 tables readable
        val rbeCursor = dbV2.query("SELECT * FROM reward_break_entitlements WHERE id = 'rbe_1'")
        assertTrue("reward break entitlement written", rbeCursor.moveToFirst())
        assertEquals("rbe state", "CREATED", rbeCursor.getString(rbeCursor.getColumnIndexOrThrow("state")))
        rbeCursor.close()

        val mgrCursor = dbV2.query("SELECT * FROM mini_game_results WHERE sessionId = 'session_1'")
        assertTrue("mini game result written", mgrCursor.moveToFirst())
        assertEquals("game id", "pawprint_parkour", mgrCursor.getString(mgrCursor.getColumnIndexOrThrow("gameId")))
        mgrCursor.close()

        dbV2.close()

        // ── Phase 3: Migrate to v3, verify all data preserved + new tables exist ──
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        // Verify v1 data STILL intact after v2→v3 migration
        val parentV3 = dbV3.query("SELECT COUNT(*) AS cnt FROM parent_accounts WHERE id = 'parent_1'")
        parentV3.moveToFirst()
        assertEquals("parent still present at v3", 1, parentV3.getInt(0))
        parentV3.close()

        val childV3 = dbV3.query("SELECT COUNT(*) AS cnt FROM child_profiles WHERE id = '$testId'")
        childV3.moveToFirst()
        assertEquals("child still present at v3", 1, childV3.getInt(0))
        childV3.close()

        // Verify v2 data survived v2→v3 migration
        val rbeV3 = dbV3.query("SELECT COUNT(*) AS cnt FROM reward_break_entitlements WHERE id = 'rbe_1'")
        rbeV3.moveToFirst()
        assertEquals("rbe still present at v3", 1, rbeV3.getInt(0))
        rbeV3.close()

        val mgrV3 = dbV3.query("SELECT COUNT(*) AS cnt FROM mini_game_results WHERE sessionId = 'session_1'")
        mgrV3.moveToFirst()
        assertEquals("mini game result still present at v3", 1, mgrV3.getInt(0))
        mgrV3.close()

        // Verify v3 tables exist and can be written to
        dbV3.execSQL("""
            INSERT INTO daily_challenges (id, childId, challengeDate, englishCompleted, filipinoCompleted, mathematicsCompleted, scienceCompleted, makabansaCompleted, allCompleted, badgeAwarded, createdAtEpochMillis, updatedAtEpochMillis)
            VALUES ('${testId}_2026-07-13', '$testId', '2026-07-13', 1, 1, 1, 1, 1, 1, 1, 1700000000000, 1700000000000)
        """.trimIndent())

        dbV3.execSQL("""
            INSERT INTO collected_badges (id, childId, badgeId, biome, earnedDate, earnedAtEpochMillis)
            VALUES ('${testId}_badge_01', '$testId', 'badge_01', 'forest_friends', '2026-07-13', 1700000000000)
        """.trimIndent())

        // Verify v3 tables readable
        val challengeCursor = dbV3.query("SELECT * FROM daily_challenges WHERE childId = '$testId'")
        assertTrue("daily challenge written", challengeCursor.moveToFirst())
        assertEquals("challenge date", "2026-07-13", challengeCursor.getString(challengeCursor.getColumnIndexOrThrow("challengeDate")))
        assertTrue("all completed", challengeCursor.getInt(challengeCursor.getColumnIndexOrThrow("allCompleted")) == 1)
        assertTrue("badge awarded", challengeCursor.getInt(challengeCursor.getColumnIndexOrThrow("badgeAwarded")) == 1)
        challengeCursor.close()

        val badgeCursor = dbV3.query("SELECT * FROM collected_badges WHERE childId = '$testId'")
        assertTrue("collected badge written", badgeCursor.moveToFirst())
        assertEquals("badge id", "badge_01", badgeCursor.getString(badgeCursor.getColumnIndexOrThrow("badgeId")))
        assertEquals("biome", "forest_friends", badgeCursor.getString(badgeCursor.getColumnIndexOrThrow("biome")))
        badgeCursor.close()

        // Final count check — all data intact
        val counts = mapOf(
            "parent_accounts" to 1,
            "child_profiles" to 1,
            "progress_events" to 1,
            "mastery_records" to 1,
            "rewards" to 1,
            "screen_time_limits" to 1,
            "daily_quests" to 1,
            "reward_break_entitlements" to 1,
            "mini_game_results" to 1,
            "daily_challenges" to 1,
            "collected_badges" to 1
        )

        counts.forEach { (table, expected) ->
            val cursor = dbV3.query("SELECT COUNT(*) AS cnt FROM $table")
            cursor.moveToFirst()
            assertEquals("$table count at v3", expected, cursor.getInt(0))
            cursor.close()
        }

        dbV3.close()
    }
}
