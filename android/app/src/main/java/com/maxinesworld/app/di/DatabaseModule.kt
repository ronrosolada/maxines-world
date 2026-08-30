package com.maxinesworld.app.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.maxinesworld.coredatabase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "maxines_world.db"

    /**
     * Corruption guard (audit F2, 2026-08-06): before Room opens the DB,
     * run `PRAGMA quick_check(1)` on the raw file. If the database is
     * corrupt, quarantine it (rename to *.corrupt-<ts> for support) instead
     * of crash-looping on open — Room then recreates a fresh database and
     * the app stays usable for the child.
     */
    private fun quarantineCorruptDatabaseIfNeeded(context: Context) {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) return
        val healthy = runCatching {
            SQLiteDatabase.openDatabase(
                dbFile.path, null, SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                db.rawQuery("PRAGMA quick_check(1)", null).use { cursor ->
                    cursor.moveToFirst() &&
                        cursor.getString(0).equals("ok", ignoreCase = true)
                }
            }
        }.getOrDefault(false)
        if (healthy) return
        val stamp = System.currentTimeMillis()
        listOf("", "-wal", "-shm").forEach { suffix ->
            val f = File(dbFile.path + suffix)
            if (f.exists()) f.renameTo(File(f.path + ".corrupt-$stamp"))
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MaxinesDatabase {
        quarantineCorruptDatabaseIfNeeded(context)
        return Room.databaseBuilder(context, MaxinesDatabase::class.java, DB_NAME)
            .addMigrations(
                MaxinesMigrations.MIGRATION_1_2,
                MaxinesMigrations.MIGRATION_2_3,
                MaxinesMigrations.MIGRATION_3_7,
                MaxinesMigrations.MIGRATION_4_7,
                MaxinesMigrations.MIGRATION_6_7,
                MaxinesMigrations.MIGRATION_7_8,
                MaxinesMigrations.MIGRATION_8_9,
                MaxinesMigrations.MIGRATION_9_10,
                MaxinesMigrations.MIGRATION_10_11,
                MaxinesMigrations.MIGRATION_11_12,
            )
            // Last-resort crash prevention: if an unknown schema version ever
            // appears (e.g. a build that shipped and was later rolled back),
            // reset the database rather than permanently bricking the app.
            // Progress loss is preferable to a child being unable to open it.
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    @Provides fun provideParentAccountDao(db: MaxinesDatabase): ParentAccountDao = db.parentAccountDao()
    @Provides fun provideChildProfileDao(db: MaxinesDatabase): ChildProfileDao = db.childProfileDao()
    @Provides fun provideProgressEventDao(db: MaxinesDatabase): ProgressEventDao = db.progressEventDao()
    @Provides fun provideMasteryRecordDao(db: MaxinesDatabase): MasteryRecordDao = db.masteryRecordDao()
    @Provides fun provideRewardDao(db: MaxinesDatabase): RewardDao = db.rewardDao()
    @Provides fun provideScreenTimeLimitDao(db: MaxinesDatabase): ScreenTimeLimitDao = db.screenTimeLimitDao()
    @Provides fun provideDailyQuestDao(db: MaxinesDatabase): DailyQuestDao = db.dailyQuestDao()
    @Provides fun provideRewardBreakDao(db: MaxinesDatabase): RewardBreakDao = db.rewardBreakDao()
    @Provides fun provideMiniGameResultDao(db: MaxinesDatabase): MiniGameResultDao = db.miniGameResultDao()
    @Provides fun provideDailyChallengeDao(db: MaxinesDatabase): DailyChallengeDao = db.dailyChallengeDao()
    @Provides fun provideCollectedBadgeDao(db: MaxinesDatabase): CollectedBadgeDao = db.collectedBadgeDao()
    @Provides fun provideWildlifeExpeditionDao(db: MaxinesDatabase): WildlifeExpeditionDao = db.wildlifeExpeditionDao()
    @Provides fun provideLessonCompletionDao(db: MaxinesDatabase): LessonCompletionDao = db.lessonCompletionDao()
    @Provides fun provideRewardLedgerDao(db: MaxinesDatabase): RewardLedgerDao = db.rewardLedgerDao()
    @Provides fun provideInventoryDao(db: MaxinesDatabase): InventoryDao = db.inventoryDao()
    @Provides fun provideDailyQuestSetDao(db: MaxinesDatabase): DailyQuestSetDao = db.dailyQuestSetDao()
    @Provides fun provideDailyQuestCompletionDao(db: MaxinesDatabase): DailyQuestCompletionDao = db.dailyQuestCompletionDao()
    @Provides fun providePlaygroundUnlockReceiptDao(db: MaxinesDatabase): PlaygroundUnlockReceiptDao = db.playgroundUnlockReceiptDao()
    @Provides fun provideContentPackageDao(db: MaxinesDatabase): ContentPackageDao = db.contentPackageDao()
    @Provides fun provideActiveContentPackageDao(db: MaxinesDatabase): ActiveContentPackageDao = db.activeContentPackageDao()
    @Provides fun provideContentSyncRunDao(db: MaxinesDatabase): ContentSyncRunDao = db.contentSyncRunDao()
    @Provides fun provideVideoWatchLedgerDao(db: MaxinesDatabase): VideoWatchLedgerDao = db.videoWatchLedgerDao()
}
