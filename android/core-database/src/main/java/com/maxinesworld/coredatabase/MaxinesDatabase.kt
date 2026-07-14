package com.maxinesworld.coredatabase

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ParentAccountEntity::class,
        ChildProfileEntity::class,
        ProgressEventEntity::class,
        MasteryRecordEntity::class,
        RewardEntity::class,
        ScreenTimeLimitEntity::class,
        DailyQuestEntity::class,
        RewardBreakEntitlementEntity::class,
        MiniGameResultEntity::class,
        DailyChallengeEntity::class,
        CollectedBadgeEntity::class,
        LessonCompletionEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class MaxinesDatabase : RoomDatabase() {
    abstract fun parentAccountDao(): ParentAccountDao
    abstract fun childProfileDao(): ChildProfileDao
    abstract fun progressEventDao(): ProgressEventDao
    abstract fun masteryRecordDao(): MasteryRecordDao
    abstract fun rewardDao(): RewardDao
    abstract fun screenTimeLimitDao(): ScreenTimeLimitDao
    abstract fun dailyQuestDao(): DailyQuestDao
    abstract fun rewardBreakDao(): RewardBreakDao
    abstract fun miniGameResultDao(): MiniGameResultDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun collectedBadgeDao(): CollectedBadgeDao
    abstract fun lessonCompletionDao(): LessonCompletionDao
}
