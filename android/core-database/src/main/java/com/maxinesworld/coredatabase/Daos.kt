package com.maxinesworld.coredatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentAccountDao {
    @Query("SELECT * FROM parent_accounts LIMIT 1")
    suspend fun getParent(): ParentAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(parent: ParentAccountEntity)

    @Query("SELECT COUNT(*) FROM parent_accounts")
    suspend fun count(): Int

    @Query("DELETE FROM parent_accounts")
    suspend fun deleteAll()
}

@Dao
interface ChildProfileDao {
    @Query("SELECT * FROM child_profiles WHERE id = :childId")
    suspend fun getById(childId: String): ChildProfileEntity?

    @Query("SELECT * FROM child_profiles WHERE parentId = :parentId")
    fun observeByParent(parentId: String): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM child_profiles WHERE parentId = :parentId")
    suspend fun getByParent(parentId: String): List<ChildProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(child: ChildProfileEntity)

    @Delete
    suspend fun delete(child: ChildProfileEntity)
}

@Dao
interface ProgressEventDao {
    @Query("SELECT * FROM progress_events WHERE childId = :childId ORDER BY timestamp DESC")
    fun observeByChild(childId: String): Flow<List<ProgressEventEntity>>

    @Query("SELECT * FROM progress_events WHERE childId = :childId AND skillId = :skillId ORDER BY timestamp DESC")
    suspend fun getByChildAndSkill(childId: String, skillId: String): List<ProgressEventEntity>

    @Query("SELECT * FROM progress_events WHERE childId = :childId ORDER BY timestamp DESC")
    suspend fun getByChild(childId: String): List<ProgressEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: ProgressEventEntity)

    @Query("UPDATE progress_events SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT * FROM progress_events WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ProgressEventEntity>
}

@Dao
interface MasteryRecordDao {
    @Query("SELECT * FROM mastery_records WHERE childId = :childId")
    fun observeByChild(childId: String): Flow<List<MasteryRecordEntity>>

    @Query("SELECT * FROM mastery_records WHERE childId = :childId AND skillId = :skillId")
    suspend fun getByChildAndSkill(childId: String, skillId: String): MasteryRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: MasteryRecordEntity)

    @Query("SELECT * FROM mastery_records WHERE childId = :childId")
    suspend fun getByChild(childId: String): List<MasteryRecordEntity>
}

// ─── Badge Collection System ───

@Dao
interface DailyChallengeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(challenge: DailyChallengeEntity)

    @Query("SELECT * FROM daily_challenges WHERE childId = :childId AND challengeDate = :date")
    suspend fun getByChildAndDate(childId: String, date: String): DailyChallengeEntity?

    @Query("SELECT * FROM daily_challenges WHERE childId = :childId ORDER BY challengeDate DESC LIMIT 1")
    suspend fun getLatest(childId: String): DailyChallengeEntity?
}

@Dao
interface CollectedBadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(badge: CollectedBadgeEntity)

    @Query("SELECT * FROM collected_badges WHERE childId = :childId ORDER BY earnedAtEpochMillis ASC")
    suspend fun getAllByChild(childId: String): List<CollectedBadgeEntity>

    @Query("SELECT COUNT(*) FROM collected_badges WHERE childId = :childId")
    suspend fun countByChild(childId: String): Int

    @Query("SELECT COUNT(*) FROM collected_badges WHERE childId = :childId AND biome = :biome")
    suspend fun countByBiome(childId: String, biome: String): Int
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards WHERE childId = :childId ORDER BY earnedAt DESC")
    fun observeByChild(childId: String): Flow<List<RewardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: RewardEntity)

    @Query("SELECT SUM(amount) FROM rewards WHERE childId = :childId AND type = :type")
    suspend fun getTotalByType(childId: String, type: String): Int?
}

@Dao
interface ScreenTimeLimitDao {
    @Query("SELECT * FROM screen_time_limits WHERE childId = :childId")
    suspend fun getByChild(childId: String): List<ScreenTimeLimitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: ScreenTimeLimitEntity)
}

@Dao
interface DailyQuestDao {
    @Query("SELECT * FROM daily_quests WHERE childId = :childId AND date = :date")
    suspend fun getByChildAndDate(childId: String, date: String): DailyQuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quest: DailyQuestEntity)
}

@Dao
interface LessonCompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(completion: LessonCompletionEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM lesson_completions WHERE childId = :childId AND lessonId = :lessonId)")
    suspend fun exists(childId: String, lessonId: String): Boolean

    @Query("SELECT * FROM lesson_completions WHERE childId = :childId AND lessonId = :lessonId AND attemptId = :attemptId")
    suspend fun getByAttempt(childId: String, lessonId: String, attemptId: String): LessonCompletionEntity?
}

// ─── Cat Café Purchase System ───

@Dao
interface RewardLedgerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(entry: RewardLedgerEntity): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reward_ledger WHERE childId = :childId")
    suspend fun fishTreatBalance(childId: String): Int
}

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(item: InventoryEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM inventory WHERE childId = :childId AND itemId = :itemId)")
    suspend fun owns(childId: String, itemId: String): Boolean
}
