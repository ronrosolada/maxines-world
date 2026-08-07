package com.maxinesworld.coredatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All Room migrations for MaxinesDatabase, in one place so both the app
 * (DatabaseModule) and instrumented migration tests can use them.
 *
 * Migration history:
 *  v1 → v2: reward_break_entitlements + mini_game_results
 *  v2 → v3: daily_challenges + collected_badges
 *  v3 → v7: adopt v6-lineage tables (lesson_completions, reward_ledger, inventory,
 *           daily_quest_sets, daily_quest_completions, playground_unlock_receipts)
 *           + v4-lineage tables (content_packages, active_content_package,
 *           content_sync_runs) + collected_badges composite index fix
 *  v4 → v7: alpha builds already had content tables; add v6 set + index fix
 *  v6 → v7: next.10 builds already had playground set + composite index; add content tables
 *
 * Shared core tables are byte-identical across v3/v4/v6 (verified against the
 * exported schema JSONs), so all three migrations are purely additive.
 */
object MaxinesMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `reward_break_entitlements` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dailyQuestCompletionId` TEXT NOT NULL, `durationMillis` INTEGER NOT NULL, `remainingMillis` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `startedAtEpochMillis` INTEGER, `consumedAtEpochMillis` INTEGER, `state` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_break_entitlements_dailyQuestCompletionId` ON `reward_break_entitlements` (`dailyQuestCompletionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `mini_game_results` (`sessionId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `rewardBreakId` TEXT NOT NULL, `childId` TEXT NOT NULL, `gameId` TEXT NOT NULL, `startedAtEpochMillis` INTEGER NOT NULL, `endedAtEpochMillis` INTEGER NOT NULL, `roundsCompleted` INTEGER NOT NULL, `successfulActions` INTEGER NOT NULL, `pawTokensEarned` INTEGER NOT NULL, `collectibleId` TEXT, PRIMARY KEY(`sessionId`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mini_game_results_idempotencyKey` ON `mini_game_results` (`idempotencyKey`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `daily_challenges` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `challengeDate` TEXT NOT NULL, `englishCompleted` INTEGER NOT NULL, `filipinoCompleted` INTEGER NOT NULL, `mathematicsCompleted` INTEGER NOT NULL, `scienceCompleted` INTEGER NOT NULL, `makabansaCompleted` INTEGER NOT NULL, `allCompleted` INTEGER NOT NULL, `badgeAwarded` INTEGER NOT NULL, `awardedBadgeId` TEXT, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_challenges_childId_challengeDate` ON `daily_challenges` (`childId`, `challengeDate`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `collected_badges` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `badgeId` TEXT NOT NULL, `biome` TEXT NOT NULL, `earnedDate` TEXT NOT NULL, `earnedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_collected_badges_childId` ON `collected_badges` (`childId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_collected_badges_badgeId` ON `collected_badges` (`badgeId`)")
        }
    }

    /**
     * v3 → v7 (2026-08): adopt the playground/quest/ledger tables (v6 lineage) and
     * content-package tables (v4 lineage) so devices on ANY shipped build (v3, v4, v6)
     * can upgrade data-preserving. Shared core tables are byte-identical across
     * v3/v4/v6 (verified against exported schema JSONs), so migrations are additive.
     * Also fixes collected_badges indices to the composite unique(childId, badgeId).
     */
    val MIGRATION_3_7 = object : Migration(3, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            fixCollectedBadgesIndices(db)
            createV6Tables(db)
            createV4Tables(db)
        }
    }

    /** v4 → v7: alpha builds already had content-pack tables; add the v6 playground set + badge index fix. */
    val MIGRATION_4_7 = object : Migration(4, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            fixCollectedBadgesIndices(db)
            createV6Tables(db)
        }
    }

    /** v6 → v7: next.10 builds already had the playground set + composite badge index; add content-pack tables. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createV4Tables(db)
        }
    }

    /** v7 → v8: persist one non-resetting Wildlife Expedition per local week. */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `wildlife_expeditions` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `weekKey` TEXT NOT NULL, `completedLessonIds` TEXT NOT NULL, `subjectKeys` TEXT NOT NULL, `badgeAwarded` INTEGER NOT NULL, `awardedBadgeId` TEXT, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_wildlife_expeditions_childId_weekKey` ON `wildlife_expeditions` (`childId`, `weekKey`)")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Record whether a completion was a first-attempt pass (spec CH-04).
            // Additive; existing rows default to first-attempt (the historical
            // behaviour was a single completion row regardless of retries).
            db.execSQL("ALTER TABLE `lesson_completions` ADD COLUMN `passedOnFirstAttempt` INTEGER NOT NULL DEFAULT 1")
        }
    }

    private fun fixCollectedBadgesIndices(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_collected_badges_badgeId`")
        db.execSQL("DROP INDEX IF EXISTS `index_collected_badges_childId`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_collected_badges_childId_badgeId` ON `collected_badges` (`childId`, `badgeId`)")
    }

    private fun createV6Tables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `lesson_completions` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `lessonId` TEXT NOT NULL, `attemptId` TEXT NOT NULL, `accuracy` REAL NOT NULL, `completedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lesson_completions_childId_lessonId_attemptId` ON `lesson_completions` (`childId`, `lessonId`, `attemptId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `reward_ledger` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `amount` INTEGER NOT NULL, `sourceKey` TEXT NOT NULL, `occurredAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reward_ledger_childId` ON `reward_ledger` (`childId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_ledger_sourceKey` ON `reward_ledger` (`sourceKey`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `inventory` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `itemId` TEXT NOT NULL, `acquiredAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_childId_itemId` ON `inventory` (`childId`, `itemId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_quest_sets` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dayKey` TEXT NOT NULL, `assignedQuestIds` TEXT NOT NULL, `assignedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_quest_sets_childId_dayKey` ON `daily_quest_sets` (`childId`, `dayKey`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_quest_completions` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dayKey` TEXT NOT NULL, `questId` TEXT NOT NULL, `completionEventId` TEXT NOT NULL, `completedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_quest_completions_childId_dayKey_questId` ON `daily_quest_completions` (`childId`, `dayKey`, `questId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `playground_unlock_receipts` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dayKey` TEXT NOT NULL, `sourceQuestSetHash` TEXT NOT NULL, `unlockedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_playground_unlock_receipts_childId_dayKey` ON `playground_unlock_receipts` (`childId`, `dayKey`)")
    }

    private fun createV4Tables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `content_packages` (`id` TEXT NOT NULL, `packageId` TEXT NOT NULL, `version` INTEGER NOT NULL, `source` TEXT NOT NULL, `state` TEXT NOT NULL, `rootPath` TEXT NOT NULL, `contentHash` TEXT NOT NULL, `installedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_content_packages_packageId_version` ON `content_packages` (`packageId`, `version`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `active_content_package` (`packageId` TEXT NOT NULL, `version` INTEGER NOT NULL, `source` TEXT NOT NULL, `activatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`packageId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `content_sync_runs` (`id` TEXT NOT NULL, `channel` TEXT NOT NULL, `state` TEXT NOT NULL, `catalogVersion` INTEGER, `packagesUpdated` INTEGER NOT NULL, `startedAtEpochMillis` INTEGER NOT NULL, `completedAtEpochMillis` INTEGER, `errorMessage` TEXT, PRIMARY KEY(`id`))")
    }
}
