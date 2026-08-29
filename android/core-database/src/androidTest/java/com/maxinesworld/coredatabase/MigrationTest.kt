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
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MaxinesMigrations.MIGRATION_1_2)

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
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MaxinesMigrations.MIGRATION_2_3)

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

    // ─────────────────────────────────────────────
    // v3 → v7: the main release path (v0.17/v0.18 devices)
    // ─────────────────────────────────────────────

    @Test
    @Throws(IOException::class)
    fun migrate3to7_preservesData_andAddsTables() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testId = "migration_test_child"

        // Phase 1: create v3 database with representative data
        val dbV3 = helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO parent_accounts (id, displayName, pinHash, biometricEnabled, createdAt) VALUES ('parent_1', 'Test Parent', 'hash_abc', 0, 1700000000000)")
            execSQL("INSERT INTO child_profiles (id, parentId, name, avatarId, grade, curriculum, createdAt) VALUES ('$testId', 'parent_1', 'Test Child', 'cat_orange_default', 3, 'ph-matatag', 1700000000000)")
            execSQL("INSERT INTO progress_events (id, childId, skillId, lessonId, activityId, eventType, accuracy, attempts, hintsUsed, responseTimeMs, timestamp, syncStatus) VALUES ('prog_1', '$testId', 'skill_001', 'lesson_001', 'act_001', 'COMPLETED', 0.85, 2, 1, 5000, 1700000000000, 'PENDING')")
            execSQL("INSERT INTO collected_badges (id, childId, badgeId, biome, earnedDate, earnedAtEpochMillis) VALUES ('${testId}_badge_01', '$testId', 'badge_01', 'forest_friends', '2026-07-13', 1700000000000)")
            close()
        }

        // Phase 2: migrate 3 → 7 using the real migration
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MaxinesMigrations.MIGRATION_3_7)

        // Core data preserved
        val child = dbV7.query("SELECT COUNT(*) AS cnt FROM child_profiles WHERE id = '$testId'")
        child.moveToFirst()
        assertEquals("child preserved through 3→7", 1, child.getInt(0))
        child.close()

        val badge = dbV7.query("SELECT COUNT(*) AS cnt FROM collected_badges WHERE badgeId = 'badge_01'")
        badge.moveToFirst()
        assertEquals("badge preserved through 3→7", 1, badge.getInt(0))
        badge.close()

        // New v6-lineage tables exist and are writable
        dbV7.execSQL("INSERT INTO lesson_completions (id, childId, lessonId, attemptId, accuracy, completedAtEpochMillis) VALUES ('lc_1', '$testId', 'lesson_001', 'att_1', 0.9, 1700000000000)")
        dbV7.execSQL("INSERT INTO reward_ledger (id, childId, amount, sourceKey, occurredAtEpochMillis) VALUES ('rl_1', '$testId', 10, 'lesson-first:$testId:lesson_001', 1700000000000)")
        dbV7.execSQL("INSERT INTO inventory (id, childId, itemId, acquiredAtEpochMillis) VALUES ('inv_1', '$testId', 'fish_treat', 1700000000000)")
        dbV7.execSQL("INSERT INTO daily_quest_sets (id, childId, dayKey, assignedQuestIds, assignedAtEpochMillis) VALUES ('dqs_1', '$testId', '2026-08-01', '[\"q1\",\"q2\"]', 1700000000000)")
        dbV7.execSQL("INSERT INTO daily_quest_completions (id, childId, dayKey, questId, completionEventId, completedAtEpochMillis) VALUES ('dqc_1', '$testId', '2026-08-01', 'q1', 'ev_1', 1700000000000)")
        dbV7.execSQL("INSERT INTO playground_unlock_receipts (id, childId, dayKey, sourceQuestSetHash, unlockedAtEpochMillis) VALUES ('pur_1', '$testId', '2026-08-01', 'hash_1', 1700000000000)")

        // New v4-lineage tables exist and are writable
        dbV7.execSQL("INSERT INTO content_packages (id, packageId, version, source, state, rootPath, contentHash, installedAtEpochMillis) VALUES ('cp_1', 'ph-grade3-v1', 1, 'BUNDLED', 'ACTIVE', '/assets', 'abc123', 1700000000000)")
        dbV7.execSQL("INSERT INTO active_content_package (packageId, version, source, activatedAtEpochMillis) VALUES ('ph-grade3-v1', 1, 'BUNDLED', 1700000000000)")
        dbV7.execSQL("INSERT INTO content_sync_runs (id, channel, state, catalogVersion, packagesUpdated, startedAtEpochMillis, completedAtEpochMillis, errorMessage) VALUES ('csr_1', 'PRODUCTION', 'SUCCEEDED', 2, 1, 1700000000000, 1700000060000, NULL)")

        // Spot-check reads
        val lc = dbV7.query("SELECT * FROM lesson_completions WHERE id = 'lc_1'")
        assertTrue("lesson completion readable", lc.moveToFirst())
        assertEquals("lc accuracy", 0.9, lc.getDouble(lc.getColumnIndexOrThrow("accuracy")), 0.001)
        lc.close()

        val cp = dbV7.query("SELECT * FROM content_packages WHERE id = 'cp_1'")
        assertTrue("content package readable", cp.moveToFirst())
        assertEquals("cp state", "ACTIVE", cp.getString(cp.getColumnIndexOrThrow("state")))
        cp.close()

        dbV7.close()
    }

    // ─────────────────────────────────────────────
    // v4 → v7: v0.7.0-alpha devices (content tables already present)
    // ─────────────────────────────────────────────

    @Test
    @Throws(IOException::class)
    fun migrate4to7_preservesContentPackData() = runTest {
        val testId = "migration_test_child"

        val dbV4 = helper.createDatabase(TEST_DB, 4).apply {
            execSQL("INSERT INTO parent_accounts (id, displayName, pinHash, biometricEnabled, createdAt) VALUES ('parent_1', 'Test Parent', 'hash_abc', 0, 1700000000000)")
            execSQL("INSERT INTO content_packages (id, packageId, version, source, state, rootPath, contentHash, installedAtEpochMillis) VALUES ('cp_alpha', 'ph-grade3-v1', 1, 'DOWNLOADED', 'VERIFIED', '/data', 'abc123', 1700000000000)")
            execSQL("INSERT INTO active_content_package (packageId, version, source, activatedAtEpochMillis) VALUES ('ph-grade3-v1', 1, 'DOWNLOADED', 1700000000000)")
            execSQL("INSERT INTO content_sync_runs (id, channel, state, catalogVersion, packagesUpdated, startedAtEpochMillis, completedAtEpochMillis, errorMessage) VALUES ('csr_alpha', 'PREVIEW', 'SUCCEEDED', 1, 1, 1700000000000, 1700000060000, NULL)")
            close()
        }

        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MaxinesMigrations.MIGRATION_4_7)

        // Content data preserved from v4
        val cp = dbV7.query("SELECT * FROM content_packages WHERE id = 'cp_alpha'")
        assertTrue("alpha content package preserved", cp.moveToFirst())
        assertEquals("cp source preserved", "DOWNLOADED", cp.getString(cp.getColumnIndexOrThrow("source")))
        cp.close()

        val active = dbV7.query("SELECT * FROM active_content_package WHERE packageId = 'ph-grade3-v1'")
        assertTrue("active pointer preserved", active.moveToFirst())
        active.close()

        // v6-lineage tables now exist (they didn't in v4)
        dbV7.execSQL("INSERT INTO daily_quest_sets (id, childId, dayKey, assignedQuestIds, assignedAtEpochMillis) VALUES ('dqs_1', '$testId', '2026-08-01', '[\"q1\"]', 1700000000000)")
        val dqs = dbV7.query("SELECT * FROM daily_quest_sets WHERE id = 'dqs_1'")
        assertTrue("daily quest set writable after 4→7", dqs.moveToFirst())
        dqs.close()

        dbV7.close()
    }

    // ─────────────────────────────────────────────
    // v6 → v7: v0.6.13-next.10 devices (playground tables already present)
    // ─────────────────────────────────────────────

    @Test
    @Throws(IOException::class)
    fun migrate6to7_preservesPlaygroundData() = runTest {
        val testId = "migration_test_child"

        val dbV6 = helper.createDatabase(TEST_DB, 6).apply {
            execSQL("INSERT INTO parent_accounts (id, displayName, pinHash, biometricEnabled, createdAt) VALUES ('parent_1', 'Test Parent', 'hash_abc', 0, 1700000000000)")
            execSQL("INSERT INTO lesson_completions (id, childId, lessonId, attemptId, accuracy, completedAtEpochMillis) VALUES ('lc_next', '$testId', 'lesson_001', 'att_1', 0.95, 1700000000000)")
            execSQL("INSERT INTO reward_ledger (id, childId, amount, sourceKey, occurredAtEpochMillis) VALUES ('rl_next', '$testId', 25, 'lesson-first:$testId:lesson_001', 1700000000000)")
            execSQL("INSERT INTO daily_quest_sets (id, childId, dayKey, assignedQuestIds, assignedAtEpochMillis) VALUES ('dqs_next', '$testId', '2026-08-01', '[\"q1\",\"q2\",\"q3\"]', 1700000000000)")
            execSQL("INSERT INTO playground_unlock_receipts (id, childId, dayKey, sourceQuestSetHash, unlockedAtEpochMillis) VALUES ('pur_next', '$testId', '2026-08-01', 'hash_x', 1700000000000)")
            close()
        }

        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MaxinesMigrations.MIGRATION_6_7)

        // Playground data preserved from v6
        val lc = dbV7.query("SELECT * FROM lesson_completions WHERE id = 'lc_next'")
        assertTrue("next.10 lesson completion preserved", lc.moveToFirst())
        assertEquals("accuracy preserved", 0.95, lc.getDouble(lc.getColumnIndexOrThrow("accuracy")), 0.001)
        lc.close()

        val ledger = dbV7.query("SELECT * FROM reward_ledger WHERE id = 'rl_next'")
        assertTrue("ledger entry preserved", ledger.moveToFirst())
        assertEquals("ledger amount", 25, ledger.getInt(ledger.getColumnIndexOrThrow("amount")))
        ledger.close()

        val pur = dbV7.query("SELECT * FROM playground_unlock_receipts WHERE id = 'pur_next'")
        assertTrue("unlock receipt preserved", pur.moveToFirst())
        pur.close()

        // Migrated lesson data must contribute to Kindness progress: the distinct
        // lesson count used by ChildLevelPolicy must include the v6 row.
        val distinctCursor = dbV7.query(
            "SELECT COUNT(DISTINCT lessonId) FROM lesson_completions WHERE childId = '$testId' AND length(trim(lessonId)) > 0"
        )
        distinctCursor.moveToFirst()
        assertEquals("migrated v6 lesson counts toward progress", 1, distinctCursor.getInt(0))
        distinctCursor.close()

        // Content tables now exist (they didn't in v6)
        dbV7.execSQL("INSERT INTO content_packages (id, packageId, version, source, state, rootPath, contentHash, installedAtEpochMillis) VALUES ('cp_1', 'ph-grade3-v1', 1, 'BUNDLED', 'ACTIVE', '/assets', 'abc123', 1700000000000)")
        val cp = dbV7.query("SELECT * FROM content_packages WHERE id = 'cp_1'")
        assertTrue("content package writable after 6→7", cp.moveToFirst())
        cp.close()

        dbV7.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate7to8_addsWildlifeExpeditionTable() = runTest {
        val testId = "migration_test_child"
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL("INSERT INTO parent_accounts (id, displayName, pinHash, biometricEnabled, createdAt) VALUES ('parent_1', 'Test Parent', 'hash_abc', 0, 1700000000000)")
            close()
        }

        val dbV8 = helper.runMigrationsAndValidate(
            TEST_DB,
            8,
            true,
            MaxinesMigrations.MIGRATION_7_8,
        )
        dbV8.execSQL("INSERT INTO wildlife_expeditions (id, childId, weekKey, completedLessonIds, subjectKeys, badgeAwarded, awardedBadgeId, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('${testId}_2026-08-03', '$testId', '2026-08-03', 'lesson-1|lesson-2', 'english|gmrc', 0, NULL, 1700000000000, 1700000000000)")

        val expedition = dbV8.query("SELECT * FROM wildlife_expeditions WHERE childId = '$testId'")
        assertTrue("wildlife expedition table is writable", expedition.moveToFirst())
        assertEquals("week key preserved", "2026-08-03", expedition.getString(expedition.getColumnIndexOrThrow("weekKey")))
        assertEquals("lesson set preserved", "lesson-1|lesson-2", expedition.getString(expedition.getColumnIndexOrThrow("completedLessonIds")))
        expedition.close()
        dbV8.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate8to9_addsPassedOnFirstAttemptColumn() = runTest {
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                "INSERT INTO lesson_completions (id, childId, lessonId, attemptId, accuracy, completedAtEpochMillis) " +
                    "VALUES ('c1', 'child_1', 'lesson-1', 'c1', 0.8, 1700000000000)"
            )
            close()
        }

        val dbV9 = helper.runMigrationsAndValidate(
            TEST_DB,
            9,
            true,
            MaxinesMigrations.MIGRATION_8_9,
        )
        val existing = dbV9.query("SELECT * FROM lesson_completions WHERE id = 'c1'")
        assertTrue("completion row preserved across v9 migration", existing.moveToFirst())
        assertEquals(
            "existing rows default to first-attempt",
            1,
            existing.getInt(existing.getColumnIndexOrThrow("passedOnFirstAttempt")),
        )
        existing.close()
        dbV9.execSQL(
            "INSERT INTO lesson_completions (id, childId, lessonId, attemptId, accuracy, passedOnFirstAttempt, completedAtEpochMillis) " +
                "VALUES ('c2', 'child_1', 'lesson-2', 'c2', 0.8, 0, 1700000000000)"
        )
        val retried = dbV9.query("SELECT * FROM lesson_completions WHERE id = 'c2'")
        assertTrue(retried.moveToFirst())
        assertEquals(0, retried.getInt(retried.getColumnIndexOrThrow("passedOnFirstAttempt")))
        retried.close()
        dbV9.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate11to12_addsFilipinoProficiencyAndPreservesChildProfile() = runTest {
        helper.createDatabase(TEST_DB, 11).apply {
            // The checked-in v11 schema was regenerated after the entity changed, so
            // recreate the actual shipped v11 table shape that lacked this column.
            execSQL("ALTER TABLE child_profiles DROP COLUMN filipinoProficiency")
            execSQL(
                "INSERT INTO child_profiles (id, parentId, name, avatarId, grade, curriculum, createdAt) " +
                    "VALUES ('child_1', 'parent_1', 'Maxine', 'cat_orange_default', 3, 'ph-matatag', 1700000000000)"
            )
            close()
        }

        val dbV12 = helper.runMigrationsAndValidate(
            TEST_DB,
            12,
            true,
            MaxinesMigrations.MIGRATION_11_12,
        )
        val child = dbV12.query("SELECT * FROM child_profiles WHERE id = 'child_1'")
        assertTrue("child profile preserved across v12 migration", child.moveToFirst())
        assertEquals("Maxine", child.getString(child.getColumnIndexOrThrow("name")))
        assertEquals(
            "existing children default to beginner Filipino proficiency",
            "BEGINNER",
            child.getString(child.getColumnIndexOrThrow("filipinoProficiency")),
        )
        child.close()
        dbV12.close()
    }
}
