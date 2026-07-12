package com.maxinesworld.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.ParentAccountDao
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.MasteryRecordDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.ScreenTimeLimitDao
import com.maxinesworld.coredatabase.DailyQuestDao
import com.maxinesworld.coredatabase.RewardBreakDao
import com.maxinesworld.coredatabase.MiniGameResultDao
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
        return Room.databaseBuilder(
            context,
            MaxinesDatabase::class.java,
            "maxines_world.db"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideParentAccountDao(db: MaxinesDatabase): ParentAccountDao = db.parentAccountDao()

    @Provides
    fun provideChildProfileDao(db: MaxinesDatabase): ChildProfileDao = db.childProfileDao()

    @Provides
    fun provideProgressEventDao(db: MaxinesDatabase): ProgressEventDao = db.progressEventDao()

    @Provides
    fun provideMasteryRecordDao(db: MaxinesDatabase): MasteryRecordDao = db.masteryRecordDao()

    @Provides
    fun provideRewardDao(db: MaxinesDatabase): RewardDao = db.rewardDao()

    @Provides
    fun provideScreenTimeLimitDao(db: MaxinesDatabase): ScreenTimeLimitDao = db.screenTimeLimitDao()

    @Provides
    fun provideDailyQuestDao(db: MaxinesDatabase): DailyQuestDao = db.dailyQuestDao()

    @Provides
    fun provideRewardBreakDao(db: MaxinesDatabase): RewardBreakDao = db.rewardBreakDao()

    @Provides
    fun provideMiniGameResultDao(db: MaxinesDatabase): MiniGameResultDao = db.miniGameResultDao()

    // ─── Migrations ───
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `reward_break_entitlements` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, `dailyQuestCompletionId` TEXT NOT NULL, `durationMillis` INTEGER NOT NULL, `remainingMillis` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `startedAtEpochMillis` INTEGER, `consumedAtEpochMillis` INTEGER, `state` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_break_entitlements_dailyQuestCompletionId` ON `reward_break_entitlements` (`dailyQuestCompletionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `mini_game_results` (`sessionId` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `rewardBreakId` TEXT NOT NULL, `childId` TEXT NOT NULL, `gameId` TEXT NOT NULL, `startedAtEpochMillis` INTEGER NOT NULL, `endedAtEpochMillis` INTEGER NOT NULL, `roundsCompleted` INTEGER NOT NULL, `successfulActions` INTEGER NOT NULL, `pawTokensEarned` INTEGER NOT NULL, `collectibleId` TEXT, PRIMARY KEY(`sessionId`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mini_game_results_idempotencyKey` ON `mini_game_results` (`idempotencyKey`)")
        }
    }
}
