package com.maxinesworld.coredatabase

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "parent_accounts")
data class ParentAccountEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val pinHash: String,
    val biometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "child_profiles")
data class ChildProfileEntity(
    @PrimaryKey val id: String,
    val parentId: String,
    val name: String,
    val avatarId: String = "cat_orange_default",
    val grade: Int = 3,
    val curriculum: String = "ph-matatag",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "progress_events")
data class ProgressEventEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val skillId: String,
    val lessonId: String,
    val activityId: String,
    val eventType: String,
    val accuracy: Double = 0.0,
    val attempts: Int = 0,
    val hintsUsed: Int = 0,
    val responseTimeMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)

@Entity(tableName = "mastery_records")
data class MasteryRecordEntity(
    @PrimaryKey val id: String, // "{childId}_{skillId}"
    val childId: String,
    val skillId: String,
    val state: String = "NOT_STARTED",
    val accuracy: Double = 0.0,
    val totalAttempts: Int = 0,
    val lastActivityAt: Long = 0,
    val nextReviewAt: Long = 0
)

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val type: String,
    val subject: String = "",
    val amount: Int = 0,
    val earnedAt: Long = System.currentTimeMillis(),
    val metadata: String = ""
)

@Entity(tableName = "screen_time_limits")
data class ScreenTimeLimitEntity(
    @PrimaryKey val id: String, // "{childId}_{dayType}"
    val childId: String,
    val dayType: String,
    val limitMinutes: Int = 120,
    val downtimeStart: String = "19:30",
    val downtimeEnd: String = "07:00"
)

@Entity(tableName = "daily_quests")
data class DailyQuestEntity(
    @PrimaryKey val id: String, // "{childId}_{date}"
    val childId: String,
    val date: String,
    val subjectRotations: String = "[]", // JSON array
    val completedLessons: String = "[]",
    val energyEarned: Int = 0
)

// ─── Mini-Game Reward Break Entities ───

@Entity(
    tableName = "reward_break_entitlements",
    indices = [Index(value = ["dailyQuestCompletionId"], unique = true)]
)
data class RewardBreakEntitlementEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val dailyQuestCompletionId: String,
    val durationMillis: Long,
    val remainingMillis: Long,
    val createdAtEpochMillis: Long,
    val startedAtEpochMillis: Long? = null,
    val consumedAtEpochMillis: Long? = null,
    val state: String = "CREATED" // CREATED, ACTIVE, CONSUMED
)

@Entity(
    tableName = "mini_game_results",
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class MiniGameResultEntity(
    @PrimaryKey val sessionId: String,
    val idempotencyKey: String,
    val rewardBreakId: String,
    val childId: String,
    val gameId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val roundsCompleted: Int,
    val successfulActions: Int,
    val pawTokensEarned: Int,
    val collectibleId: String? = null
)

// ─── Badge Collection System ───

@Entity(tableName = "daily_challenges", indices = [Index(value = ["childId", "challengeDate"], unique = true)])
data class DailyChallengeEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val challengeDate: String,       // ISO date YYYY-MM-DD in child's timezone
    val englishCompleted: Boolean = false,
    val filipinoCompleted: Boolean = false,
    val mathematicsCompleted: Boolean = false,
    val scienceCompleted: Boolean = false,
    val makabansaCompleted: Boolean = false,
    val allCompleted: Boolean = false,
    val badgeAwarded: Boolean = false,
    val awardedBadgeId: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "collected_badges", indices = [Index(value = ["childId", "badgeId"], unique = true)])
data class CollectedBadgeEntity(
    @PrimaryKey val id: String,      // compound: childId_badgeId
    val childId: String,
    val badgeId: String,
    val biome: String,
    val earnedDate: String,          // ISO date YYYY-MM-DD
    val earnedAtEpochMillis: Long = System.currentTimeMillis()
)

/**
 * One forgiving Wildlife Expedition per child and local calendar week.
 * Lesson IDs and normalized subject keys are stored as a small delimiter-
 * separated set so progress survives app restarts without another JSON layer.
 */
@Entity(
    tableName = "wildlife_expeditions",
    indices = [Index(value = ["childId", "weekKey"], unique = true)]
)
data class WildlifeExpeditionEntity(
    @PrimaryKey val id: String, // "{childId}_{weekKey}"
    val childId: String,
    val weekKey: String, // ISO date of the Monday starting the local week
    val completedLessonIds: String = "",
    val subjectKeys: String = "",
    val badgeAwarded: Boolean = false,
    val awardedBadgeId: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

// ─── Lesson Completion Idempotency (v6 lineage, adopted v7) ───

@Entity(
    tableName = "lesson_completions",
    indices = [Index(value = ["childId", "lessonId", "attemptId"], unique = true)]
)
data class LessonCompletionEntity(
    @PrimaryKey val id: String,       // "{childId}:{lessonId}:{attemptId}"
    val childId: String,
    val lessonId: String,
    val attemptId: String,
    val accuracy: Double,
    /** False when the child passed only after retryAssessment(); first-attempt
     *  and post-retry passes are recorded distinctly (spec CH-04). */
    val passedOnFirstAttempt: Boolean = true,
    val completedAtEpochMillis: Long = System.currentTimeMillis()
)

// ─── Fish Treat Ledger (v6 lineage, adopted v7) ───

@Entity(
    tableName = "reward_ledger",
    indices = [Index(value = ["childId"]), Index(value = ["sourceKey"], unique = true)]
)
data class RewardLedgerEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val amount: Int,
    val sourceKey: String,      // "lesson-first:{childId}:{lessonId}", "purchase:{childId}:{itemId}", etc.
    val occurredAtEpochMillis: Long = System.currentTimeMillis()
)

// ─── Inventory (v6 lineage, adopted v7) ───

@Entity(
    tableName = "inventory",
    indices = [Index(value = ["childId", "itemId"], unique = true)]
)
data class InventoryEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val itemId: String,
    val acquiredAtEpochMillis: Long = System.currentTimeMillis()
)

// ─── Playground Gate Persistence (v6 lineage, adopted v7) ───

@Entity(
    tableName = "daily_quest_sets",
    indices = [Index(value = ["childId", "dayKey"], unique = true)]
)
data class DailyQuestSetEntity(
    @PrimaryKey val id: String,           // "{childId}_{dayKey}"
    val childId: String,
    val dayKey: String,
    val assignedQuestIds: String,         // JSON array of quest IDs
    val assignedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_quest_completions",
    indices = [Index(value = ["childId", "dayKey", "questId"], unique = true)]
)
data class DailyQuestCompletionEntity(
    @PrimaryKey val id: String,           // "{childId}_{dayKey}_{questId}"
    val childId: String,
    val dayKey: String,
    val questId: String,
    val completionEventId: String,
    val completedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playground_unlock_receipts",
    indices = [Index(value = ["childId", "dayKey"], unique = true)]
)
data class PlaygroundUnlockReceiptEntity(
    @PrimaryKey val id: String,           // "{childId}_{dayKey}"
    val childId: String,
    val dayKey: String,
    val sourceQuestSetHash: String,
    val unlockedAtEpochMillis: Long = System.currentTimeMillis()
)

// ─── Content Package Registry (v4 lineage, adopted v7) ───

/** Content package registry — bundled or downloaded. */
@Entity(tableName = "content_packages", indices = [
    Index(value = ["packageId", "version"], unique = true)
])
data class ContentPackageEntity(
    @PrimaryKey val id: String,                    // packageId_version
    val packageId: String,
    val version: Int,
    val source: String,                             // BUNDLED or DOWNLOADED
    val state: String,                              // VERIFIED, STAGED, ACTIVE, QUARANTINED
    val rootPath: String,                           // absolute path or asset path
    val contentHash: String,                        // SHA-256 of package contents
    val installedAtEpochMillis: Long = System.currentTimeMillis()
)

/** Single active pointer per package ID. UNIQUE constraint = one active row per package. */
@Entity(tableName = "active_content_package")
data class ActiveContentPackageEntity(
    @PrimaryKey val packageId: String,
    val version: Int,
    val source: String,                             // BUNDLED or DOWNLOADED
    val activatedAtEpochMillis: Long = System.currentTimeMillis()
)

/** Sync run audit log. */
@Entity(tableName = "content_sync_runs")
data class ContentSyncRunEntity(
    @PrimaryKey val id: String,
    val channel: String,                            // PRODUCTION, PREVIEW
    val state: String,                              // STARTED, SUCCEEDED, FAILED
    val catalogVersion: Int? = null,
    val packagesUpdated: Int = 0,
    val startedAtEpochMillis: Long = System.currentTimeMillis(),
    val completedAtEpochMillis: Long? = null,
    val errorMessage: String? = null
)
