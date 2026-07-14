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

// ─── Lesson Completion Idempotency ───

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
    val completedAtEpochMillis: Long = System.currentTimeMillis()
)
