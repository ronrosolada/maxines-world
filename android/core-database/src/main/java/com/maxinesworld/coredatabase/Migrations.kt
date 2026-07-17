package com.maxinesworld.coredatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room schema-aligned migrations exported from DatabaseModule for reuse.
 *
 * Index names MUST match the Room-generated JSON schemas in
 * core-database/schemas/com.maxinesworld.coredatabase.MaxinesDatabase/.
 */
object MaxinesMigrations {

    /** v5→v6: Playground gate entities. Index names match Room schema 6 exactly. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `daily_quest_sets` (
                  `id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dayKey` TEXT NOT NULL,
                  `assignedQuestIds` TEXT NOT NULL, `assignedAtEpochMillis` INTEGER NOT NULL,
                  PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_quest_sets_childId_dayKey`
                  ON `daily_quest_sets` (`childId`, `dayKey`)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `daily_quest_completions` (
                  `id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dayKey` TEXT NOT NULL,
                  `questId` TEXT NOT NULL, `completionEventId` TEXT NOT NULL,
                  `completedAtEpochMillis` INTEGER NOT NULL,
                  PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_quest_completions_childId_dayKey_questId`
                  ON `daily_quest_completions` (`childId`, `dayKey`, `questId`)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `playground_unlock_receipts` (
                  `id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dayKey` TEXT NOT NULL,
                  `sourceQuestSetHash` TEXT NOT NULL, `unlockedAtEpochMillis` INTEGER NOT NULL,
                  PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """CREATE UNIQUE INDEX IF NOT EXISTS `index_playground_unlock_receipts_childId_dayKey`
                  ON `playground_unlock_receipts` (`childId`, `dayKey`)"""
            )
            // Clean up stray index from MIGRATION_2_3 that isn't in any Room schema
            db.execSQL("DROP INDEX IF EXISTS `index_collected_badges_childId`")
        }
    }

    /**
     * v6→v7: Repair reward_ledger / inventory indexes on devices that already
     * applied the incomplete MIGRATION_4_5 (missing childId index; wrong inventory index name).
     * No table shape changes — schemas 6 and 7 are identical except for version.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_reward_ledger_childId` " +
                    "ON `reward_ledger` (`childId`)"
            )
            db.execSQL("DROP INDEX IF EXISTS `idx_inventory_owner`")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_childId_itemId` " +
                    "ON `inventory` (`childId`, `itemId`)"
            )
        }
    }
}
