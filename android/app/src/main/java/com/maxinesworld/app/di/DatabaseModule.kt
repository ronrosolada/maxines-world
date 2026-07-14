package com.maxinesworld.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
    @Provides fun provideLessonCompletionDao(db: MaxinesDatabase): LessonCompletionDao = db.lessonCompletionDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `reward_break_entitlements` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dailyQuestCompletionId` TEXT NOT NULL, `durationMillis` INTEGER NOT NULL, `remainingMillis` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `startedAtEpochMillis` INTEGER, `consumedAtEpochMillis` INTEGER, `state` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_break_entitlements_dailyQuestCompletionId` ON `reward_break_entitlements` (`dailyQuestCompletionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `mini_game_results` (`sessionId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `rewardBreakId` TEXT NOT NULL, `childId` TEXT NOT NULL, `gameId` TEXT NOT NULL, `startedAtEpochMillis` INTEGER NOT NULL, `endedAtEpochMillis` INTEGER NOT NULL, `roundsCompleted` INTEGER NOT NULL, `successfulActions` INTEGER NOT NULL, `pawTokensEarned` INTEGER NOT NULL, `collectibleId` TEXT, PRIMARY KEY(`sessionId`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mini_game_results_idempotencyKey` ON `mini_game_results` (`idempotencyKey`)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `daily_challenges` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `challengeDate` TEXT NOT NULL, `englishCompleted` INTEGER NOT NULL, `filipinoCompleted` INTEGER NOT NULL, `mathematicsCompleted` INTEGER NOT NULL, `scienceCompleted` INTEGER NOT NULL, `makabansaCompleted` INTEGER NOT NULL, `allCompleted` INTEGER NOT NULL, `badgeAwarded` INTEGER NOT NULL, `awardedBadgeId` TEXT, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_challenges_childId_challengeDate` ON `daily_challenges` (`childId`, `challengeDate`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `collected_badges` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `badgeId` TEXT NOT NULL, `biome` TEXT NOT NULL, `earnedDate` TEXT NOT NULL, `earnedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_collected_badges_childId` ON `collected_badges` (`childId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_collected_badges_badgeId` ON `collected_badges` (`badgeId`)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `lesson_completions` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `lessonId` TEXT NOT NULL, `attemptId` TEXT NOT NULL, `accuracy` REAL NOT NULL, `completedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lesson_completions_childId_lessonId_attemptId` ON `lesson_completions` (`childId`, `lessonId`, `attemptId`)")
            // Fix badge uniqueness to scope by childId+badgeId
            db.execSQL("DROP INDEX IF EXISTS `index_collected_badges_badgeId`")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_collected_badges_childId_badgeId` ON `collected_badges` (`childId`, `badgeId`)")
        }
    }
}
