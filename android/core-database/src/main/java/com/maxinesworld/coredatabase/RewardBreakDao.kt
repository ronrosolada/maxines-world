package com.maxinesworld.coredatabase

import androidx.room.*

@Dao
interface RewardBreakDao {
    @Query("SELECT * FROM reward_break_entitlements WHERE id = :id")
    suspend fun getById(id: String): RewardBreakEntitlementEntity?

    @Query("SELECT * FROM reward_break_entitlements WHERE childId = :childId AND state = 'ACTIVE'")
    suspend fun getActive(childId: String): RewardBreakEntitlementEntity?

    @Query("SELECT * FROM reward_break_entitlements WHERE dailyQuestCompletionId = :dqId")
    suspend fun getByQuestCompletion(dqId: String): RewardBreakEntitlementEntity?

    /** First-write-wins: a retry must not reset an already active/consumed break. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(entitlement: RewardBreakEntitlementEntity)

    @Query("""
        UPDATE reward_break_entitlements
        SET state = 'ACTIVE', startedAtEpochMillis = :startedAtEpochMillis
        WHERE id = :id AND childId = :childId
          AND state = 'CREATED' AND remainingMillis > 0
    """)
    suspend fun startIfCreated(id: String, childId: String, startedAtEpochMillis: Long): Int

    /** Idempotent consumption; the child ID prevents cross-child route misuse. */
    @Query("""
        UPDATE reward_break_entitlements
        SET remainingMillis = 0, state = 'CONSUMED', consumedAtEpochMillis = :consumedAtEpochMillis
        WHERE id = :id AND childId = :childId AND state != 'CONSUMED'
    """)
    suspend fun consumeIfUnconsumed(id: String, childId: String, consumedAtEpochMillis: Long): Int

    @Query("UPDATE reward_break_entitlements SET remainingMillis = :remaining, state = :state WHERE id = :id AND childId = :childId")
    suspend fun updateRemaining(id: String, childId: String, remaining: Long, state: String): Int
}

@Dao
interface MiniGameResultDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(result: MiniGameResultEntity)

    @Query("SELECT * FROM mini_game_results WHERE idempotencyKey = :key")
    suspend fun getByIdempotencyKey(key: String): MiniGameResultEntity?

    @Query("SELECT * FROM mini_game_results WHERE childId = :childId ORDER BY endedAtEpochMillis DESC")
    suspend fun getByChild(childId: String): List<MiniGameResultEntity>
}
