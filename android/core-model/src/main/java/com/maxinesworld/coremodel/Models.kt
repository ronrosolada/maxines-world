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
    val vocabulary: List<VocabTerm> = emptyList(),
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
    /** Stable id for an optional offline media asset referenced by this step. */
    val mediaId: String? = null,
    val feedback: ActivityFeedback? = null,
    // ─── Typed activity payloads (added for the activity pipeline repair) ───
    // Populated by LessonPlayerViewModel.convertToLessonManifest from the
    // lesson JSON `content` object. All default to empty so existing
    // construction sites remain source-compatible.
    val sortCategories: List<String> = emptyList(),
    val sortItems: List<SortItem> = emptyList(),
    val matchPairs: List<MatchPair> = emptyList(),
    val sequenceSteps: List<String> = emptyList(),
    val hotspotExamples: List<String> = emptyList(),
    val completionRule: String = "",
    val completionTargetCount: Int = 0,
    /** Authored hint text. Empty means this activity has no hint affordance. */
    val hintText: String = "",
    /**
     * Whether this step contributes to accuracy and mastery. Practice and
     * exploratory steps are unscored (`false`); authored assessment steps are
     * scored (`true`). Single source of truth enforced in
     * LessonPlayerViewModel.onActivityResult (spec CH-04).
     */
    val scored: Boolean = true,
)

@Serializable
data class SortItem(
    val label: String,
    val categoryIndex: Int
)

@Serializable
data class MatchPair(
    val left: String,
    val right: String
)

const val DEFAULT_INCORRECT_FEEDBACK = "Look at the example again and try once more. 💪"

private val GENERIC_INCORRECT_FEEDBACK = setOf(
    "Let's try again!",
    "Try again!",
    "Incorrect. Try again.",
)

/** Replace opaque curriculum language before it reaches a child-facing surface. */
private fun sanitizeLearnerFeedbackText(text: String): String = text
    .replace("does not follow the lesson skill", "does not match what we learned", ignoreCase = true)
    .replace("do not follow the lesson skill", "do not match what we learned", ignoreCase = true)
    .replace("follows the lesson skill", "matches what we learned", ignoreCase = true)
    .replace("follow the lesson skill", "match what we learned", ignoreCase = true)
    .replace("follows the skill explanation", "matches what we learned", ignoreCase = true)
    .replace("follow the skill explanation", "match what we learned", ignoreCase = true)
    .replace("follows the lesson explanation", "matches what we learned", ignoreCase = true)
    .replace("follow the lesson explanation", "match what we learned", ignoreCase = true)
    .replace("applies the lesson skill", "uses what we learned", ignoreCase = true)
    .replace("apply the lesson skill", "use what we learned", ignoreCase = true)
    .replace("applies the skill", "uses what we learned", ignoreCase = true)
    .replace("apply the skill", "use what we learned", ignoreCase = true)
    .replace("uses the lesson skill", "uses what we learned", ignoreCase = true)
    .replace("use the lesson skill", "use what we learned", ignoreCase = true)
    .replace("follow the skill rule", "match what we learned", ignoreCase = true)
    .replace("does not show the skill", "does not show what we learned", ignoreCase = true)
    .replace("do not show the skill", "do not show what we learned", ignoreCase = true)
    .replace("shows the skill", "shows what we learned", ignoreCase = true)
    .replace("show the skill", "show what we learned", ignoreCase = true)
    .replace("The other choices show the skill", "The other choices match what we learned", ignoreCase = true)

/** Keep curriculum copy understandable to a Grade 3 learner at the moment of retry. */
fun sanitizeIncorrectFeedback(raw: String?): String? {
    val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (text in GENERIC_INCORRECT_FEEDBACK) return null
    return sanitizeLearnerFeedbackText(text)
}

fun childFacingIncorrectFeedback(
    raw: String?,
    fallback: String = DEFAULT_INCORRECT_FEEDBACK,
): String = sanitizeIncorrectFeedback(raw) ?: fallback

/** Apply the same child-facing copy guard to success/explanation feedback. */
fun sanitizeCorrectFeedback(raw: String?): String? =
    raw?.trim()?.takeIf { it.isNotEmpty() }?.let(::sanitizeLearnerFeedbackText)

fun childFacingCorrectFeedback(
    raw: String?,
    fallback: String = "Great job!",
): String = sanitizeCorrectFeedback(raw) ?: fallback

@Serializable
data class ActivityFeedback(
    val correct: String = "Great job!",
    val incorrect: String = DEFAULT_INCORRECT_FEEDBACK
)

@Serializable
data class AssessmentBlock(
    val passThreshold: Double = 0.8,
    val minQuestions: Int = 5,
    /** Converted playable assessment steps (type ASSESSMENT_V1), appended after
     *  the practice activities in [LessonManifest.steps]. */
    val items: List<ActivityStep> = emptyList(),
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
