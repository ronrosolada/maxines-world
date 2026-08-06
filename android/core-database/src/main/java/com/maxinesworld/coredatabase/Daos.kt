package com.maxinesworld.coredatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentAccountDao {
    @Query("SELECT * FROM parent_accounts ORDER BY createdAt ASC LIMIT 1")
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

    @Query("SELECT * FROM child_profiles WHERE id = :childId")
    fun observeById(childId: String): Flow<ChildProfileEntity?>

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(event: ProgressEventEntity)

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
interface WildlifeExpeditionDao {
    @Query("SELECT * FROM wildlife_expeditions WHERE childId = :childId AND weekKey = :weekKey")
    suspend fun getByChildAndWeek(childId: String, weekKey: String): WildlifeExpeditionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expedition: WildlifeExpeditionEntity)
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards WHERE childId = :childId ORDER BY earnedAt DESC")
    fun observeByChild(childId: String): Flow<List<RewardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: RewardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(reward: RewardEntity): Long

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

// ─── Lesson Completion Idempotency (v7) ───

@Dao
interface LessonCompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(completion: LessonCompletionEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM lesson_completions WHERE childId = :childId AND lessonId = :lessonId)")
    suspend fun exists(childId: String, lessonId: String): Boolean

    @Query("SELECT * FROM lesson_completions WHERE childId = :childId AND lessonId = :lessonId AND attemptId = :attemptId")
    suspend fun getByAttempt(childId: String, lessonId: String, attemptId: String): LessonCompletionEntity?

    @Query("SELECT COUNT(DISTINCT lessonId) FROM lesson_completions WHERE childId = :childId AND length(trim(lessonId)) > 0")
    fun observeDistinctLessonCount(childId: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT lessonId) FROM lesson_completions WHERE childId = :childId AND length(trim(lessonId)) > 0")
    suspend fun countDistinctLessons(childId: String): Int

    @Query("SELECT DISTINCT lessonId FROM lesson_completions WHERE childId = :childId AND length(trim(lessonId)) > 0")
    fun observeDistinctLessonIds(childId: String): Flow<List<String>>

    @Query(
        "SELECT DISTINCT strftime('%Y-%m-%d', completedAtEpochMillis / 1000, 'unixepoch') AS day " +
        "FROM lesson_completions WHERE childId = :childId ORDER BY day DESC"
    )
    fun observeCompletionDays(childId: String): Flow<List<String>>
}

// ─── Fish Treat Ledger (v7) ───

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

    @Query("SELECT itemId FROM inventory WHERE childId = :childId")
    suspend fun getOwnedItemIds(childId: String): List<String>
}

// ─── Playground Gate Persistence (v7) ───

@Dao
interface DailyQuestSetDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(entry: DailyQuestSetEntity): Long

    @Query("SELECT * FROM daily_quest_sets WHERE childId = :childId AND dayKey = :dayKey")
    suspend fun getByChildAndDay(childId: String, dayKey: String): DailyQuestSetEntity?

    @Query("SELECT * FROM daily_quest_sets WHERE childId = :childId AND dayKey = :dayKey")
    fun observeByChildAndDay(childId: String, dayKey: String): Flow<DailyQuestSetEntity?>
}

@Dao
interface DailyQuestCompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(entry: DailyQuestCompletionEntity): Long

    @Query("SELECT * FROM daily_quest_completions WHERE childId = :childId AND dayKey = :dayKey")
    suspend fun getByChildAndDay(childId: String, dayKey: String): List<DailyQuestCompletionEntity>

    @Query("SELECT questId FROM daily_quest_completions WHERE childId = :childId AND dayKey = :dayKey")
    suspend fun getCompletedQuestIds(childId: String, dayKey: String): List<String>

    @Query("SELECT questId FROM daily_quest_completions WHERE childId = :childId AND dayKey = :dayKey")
    fun observeCompletedQuestIds(childId: String, dayKey: String): Flow<List<String>>
}

@Dao
interface PlaygroundUnlockReceiptDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(entry: PlaygroundUnlockReceiptEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM playground_unlock_receipts WHERE childId = :childId AND dayKey = :dayKey)")
    suspend fun existsByChildAndDay(childId: String, dayKey: String): Boolean

    @Query("SELECT * FROM playground_unlock_receipts WHERE childId = :childId AND dayKey = :dayKey")
    suspend fun getByChildAndDay(childId: String, dayKey: String): PlaygroundUnlockReceiptEntity?

    @Query("SELECT * FROM playground_unlock_receipts WHERE childId = :childId AND dayKey = :dayKey")
    fun observeByChildAndDay(childId: String, dayKey: String): Flow<PlaygroundUnlockReceiptEntity?>
}

// ─── Content Package Registry (v7) ───

@Dao
interface ContentPackageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pkg: ContentPackageEntity)

    @Query("SELECT * FROM content_packages WHERE packageId = :packageId ORDER BY version DESC")
    suspend fun getVersions(packageId: String): List<ContentPackageEntity>

    @Query("SELECT * FROM content_packages WHERE source = :source AND state = 'VERIFIED'")
    suspend fun getVerifiedBySource(source: String): List<ContentPackageEntity>

    @Query("SELECT COUNT(*) FROM content_packages")
    suspend fun count(): Int
}

@Dao
interface ActiveContentPackageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActive(active: ActiveContentPackageEntity)

    @Query("SELECT * FROM active_content_package WHERE packageId = :packageId")
    suspend fun getActive(packageId: String): ActiveContentPackageEntity?

    @Query("DELETE FROM active_content_package WHERE packageId = :packageId")
    suspend fun removeActive(packageId: String)
}

@Dao
interface ContentSyncRunDao {
    @Insert
    suspend fun insert(run: ContentSyncRunEntity)

    @Query("SELECT * FROM content_sync_runs ORDER BY startedAtEpochMillis DESC LIMIT 10")
    suspend fun getRecent(): List<ContentSyncRunEntity>

    @Query("UPDATE content_sync_runs SET state = :state, completedAtEpochMillis = :completedAt, errorMessage = :error WHERE id = :id")
    suspend fun complete(id: String, state: String, completedAt: Long, error: String?)
}
