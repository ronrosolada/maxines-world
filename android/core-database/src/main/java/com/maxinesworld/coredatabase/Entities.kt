package com.maxinesworld.coredatabase

import androidx.room.Entity
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
