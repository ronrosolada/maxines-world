package com.maxinesworld.corenetwork

import kotlinx.serialization.Serializable

@Serializable
data class ProgressEventDto(
    val id: String,
    val childId: String,
    val skillId: String,
    val lessonId: String,
    val activityId: String,
    val eventType: String,
    val accuracy: Double = 0.0,
    val attempts: Int = 0,
    val hintsUsed: Int = 0,
    val responseTimeMs: Long = 0L,
    val timestamp: Long,
)

@Serializable
data class VideoWatchLedgerDto(
    val id: String,
    val childId: String,
    val mediaId: String,
    val subjectId: String,
    val accreditedSeconds: Int = 0,
    val quizPassed: Boolean = false,
    val bestQuizScore: Float = 0.0f,
    val firstPassedAtEpochMillis: Long? = null,
    val lastWatchedAtEpochMillis: Long,
)

@Serializable
data class LessonCompletionDto(
    val id: String,
    val childId: String,
    val lessonId: String,
    val attemptId: String,
    val accuracy: Double,
    val passedOnFirstAttempt: Boolean = true,
    val completedAtEpochMillis: Long,
)

@Serializable
data class CollectedBadgeDto(
    val id: String,
    val childId: String,
    val badgeId: String,
    val biome: String,
    val earnedDate: String,
    val earnedAtEpochMillis: Long,
)

@Serializable
data class RewardLedgerDto(
    val id: String,
    val childId: String,
    val amount: Int,
    val sourceKey: String,
    val occurredAtEpochMillis: Long,
)

@Serializable
data class InventoryDto(
    val id: String,
    val childId: String,
    val itemId: String,
    val acquiredAtEpochMillis: Long,
)

@Serializable
data class SyncPushRequest(
    val childId: String,
    val deviceId: String? = null,
    val progressEvents: List<ProgressEventDto> = emptyList(),
    val videoWatchRecords: List<VideoWatchLedgerDto> = emptyList(),
    val lessonCompletions: List<LessonCompletionDto> = emptyList(),
    val collectedBadges: List<CollectedBadgeDto> = emptyList(),
    val rewardLedgers: List<RewardLedgerDto> = emptyList(),
    val inventoryItems: List<InventoryDto> = emptyList(),
)

@Serializable
data class SyncPushResponse(
    val success: Boolean,
    val processedCount: Int,
    val serverTimestamp: Long,
)

@Serializable
data class SyncPullResponse(
    val childId: String,
    val serverTimestamp: Long,
    val progressEvents: List<ProgressEventDto> = emptyList(),
    val videoWatchRecords: List<VideoWatchLedgerDto> = emptyList(),
    val lessonCompletions: List<LessonCompletionDto> = emptyList(),
    val collectedBadges: List<CollectedBadgeDto> = emptyList(),
    val rewardLedgers: List<RewardLedgerDto> = emptyList(),
    val inventoryItems: List<InventoryDto> = emptyList(),
)
