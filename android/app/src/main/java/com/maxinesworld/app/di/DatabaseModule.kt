package com.maxinesworld.app.di

import android.content.Context
import androidx.room.Room
import com.maxinesworld.coredatabase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MaxinesDatabase {
        return Room.databaseBuilder(context, MaxinesDatabase::class.java, "maxines_world.db")
            .addMigrations(
                MaxinesMigrations.MIGRATION_1_2,
                MaxinesMigrations.MIGRATION_2_3,
                MaxinesMigrations.MIGRATION_3_7,
                MaxinesMigrations.MIGRATION_4_7,
                MaxinesMigrations.MIGRATION_6_7,
                MaxinesMigrations.MIGRATION_7_8,
            )
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
}
