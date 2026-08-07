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
        WildlifeExpeditionEntity::class,
        // v7: adopted from v6 lineage (playground/quest/ledger)
        LessonCompletionEntity::class,
        RewardLedgerEntity::class,
        InventoryEntity::class,
        DailyQuestSetEntity::class,
        DailyQuestCompletionEntity::class,
        PlaygroundUnlockReceiptEntity::class,
        // v7: adopted from v4 lineage (content packages)
        ContentPackageEntity::class,
        ActiveContentPackageEntity::class,
        ContentSyncRunEntity::class
    ],
    version = 9,
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
    abstract fun wildlifeExpeditionDao(): WildlifeExpeditionDao
    abstract fun lessonCompletionDao(): LessonCompletionDao
    abstract fun rewardLedgerDao(): RewardLedgerDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun dailyQuestSetDao(): DailyQuestSetDao
    abstract fun dailyQuestCompletionDao(): DailyQuestCompletionDao
    abstract fun playgroundUnlockReceiptDao(): PlaygroundUnlockReceiptDao
    abstract fun contentPackageDao(): ContentPackageDao
    abstract fun activeContentPackageDao(): ActiveContentPackageDao
    abstract fun contentSyncRunDao(): ContentSyncRunDao
}
