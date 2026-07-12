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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entitlement: RewardBreakEntitlementEntity)

    @Query("UPDATE reward_break_entitlements SET remainingMillis = :remaining, state = :state WHERE id = :id")
    suspend fun updateRemaining(id: String, remaining: Long, state: String)
}

@Dao
interface MiniGameResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: MiniGameResultEntity)

    @Query("SELECT * FROM mini_game_results WHERE idempotencyKey = :key")
    suspend fun getByIdempotencyKey(key: String): MiniGameResultEntity?

    @Query("SELECT * FROM mini_game_results WHERE childId = :childId ORDER BY endedAtEpochMillis DESC")
    suspend fun getByChild(childId: String): List<MiniGameResultEntity>
}
