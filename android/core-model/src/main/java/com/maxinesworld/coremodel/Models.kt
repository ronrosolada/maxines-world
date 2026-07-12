package com.maxinesworld.coremodel

import kotlinx.serialization.Serializable

// ─── Auth & Profiles ───

data class ParentAccount(
    val id: String,
    val displayName: String,
    val pinHash: String,
    val biometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChildProfile(
    val id: String,
    val parentId: String,
    val name: String,
    val avatarId: String = "cat_orange_default",
    val grade: Int = 3,
    val curriculum: String = "ph-matatag",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Curriculum & Content ───

@Serializable
data class LessonManifest(
    val id: String,
    val schemaVersion: Int,
    val subject: String,
    val moduleId: String,
    val skillIds: List<String> = emptyList(),
    val title: String,
    val objective: String,
    val guideCharacter: String,
    val estimatedMinutes: Int,
    val prerequisiteSkillIds: List<String> = emptyList(),
    val steps: List<ActivityStep> = emptyList(),
    val assessment: AssessmentBlock? = null,
    val curriculumStandard: String? = null,
    val term: Int? = null,
    val languageOfInstruction: String? = null,
    val version: Int = 1
)

@Serializable
data class ActivityStep(
    val id: String,
    val type: String,
    val narrationText: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val imageAssets: List<String> = emptyList(),
    val feedback: ActivityFeedback? = null
)

@Serializable
data class ActivityFeedback(
    val correct: String = "Great job! 🎉",
    val incorrect: String = "Let's try again! 💪"
)

@Serializable
data class AssessmentBlock(
    val passThreshold: Double = 0.8,
    val minQuestions: Int = 5
)

// ─── Progress & Mastery ───

enum class MasteryState {
    NOT_STARTED,
    INTRODUCED,
    PRACTICING,
    PROFICIENT,
    MASTERED,
    NEEDS_REVIEW
}

data class ProgressEvent(
    val id: String,
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

data class MasteryRecord(
    val childId: String,
    val skillId: String,
    val state: MasteryState = MasteryState.NOT_STARTED,
    val accuracy: Double = 0.0,
    val totalAttempts: Int = 0,
    val lastActivityAt: Long = 0,
    val nextReviewAt: Long = 0
)

// ─── Rewards ───

enum class RewardType { STAR, COIN, BADGE, VILLAGE_ENERGY, COSTUME, ACCESSORY }

data class Reward(
    val id: String,
    val childId: String,
    val type: RewardType,
    val subject: String = "",
    val amount: Int = 0,
    val earnedAt: Long = System.currentTimeMillis(),
    val metadata: String = ""
)

// ─── Screen Time ───

data class ScreenTimeLimit(
    val childId: String,
    val dayType: String, // "weekday" or "weekend"
    val limitMinutes: Int = 120,
    val downtimeStart: String = "19:30",
    val downtimeEnd: String = "07:00"
)

data class DailyQuest(
    val childId: String,
    val date: String,
    val subjectRotations: List<String> = emptyList(),
    val completedLessons: List<String> = emptyList(),
    val energyEarned: Int = 0
)
