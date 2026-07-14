package com.maxinesworld.coremodel.gamification

/**
 * Fish treats are the ONLY spendable currency in the child experience.
 *
 * Stars may appear as temporary lesson feedback only — never persisted
 * as a balance.
 *
 * Every treat grant must reference a stable source ID.
 */
object FishTreatPolicy {

    /** Reward type constant stored in RewardEntity.type. */
    const val TYPE = "FISH_TREAT"

    /** Base reward for any completed lesson. */
    const val BASE = 3

    /** Bonus for showing improvement after a retry. */
    const val RETRY_IMPROVEMENT = 1

    /** Bonus for crossing a mastery threshold (≥80% accuracy). */
    const val MASTERY_THRESHOLD = 2

    /** Maximum treats per single attempt. */
    const val MAX_PER_ATTEMPT = 6

    /**
     * Compute the number of fish treats for a lesson completion.
     *
     * Base: 3
     * +1 if improved after retry
     * +2 if crossed mastery threshold
     * Capped at [MAX_PER_ATTEMPT].
     */
    fun amount(completed: Boolean, improvedAfterRetry: Boolean, crossedMasteryThreshold: Boolean): Int {
        if (!completed) return 0
        var result = BASE
        if (improvedAfterRetry) result += RETRY_IMPROVEMENT
        if (crossedMasteryThreshold) result += MASTERY_THRESHOLD
        return result.coerceAtMost(MAX_PER_ATTEMPT)
    }

    /**
     * Deterministic reward key for idempotency.
     *
     * Format: lesson:{childId}:{lessonId}:{attemptId}
     */
    fun rewardKey(childId: String, lessonId: String, attemptId: String): String =
        "lesson:$childId:$lessonId:$attemptId"
}

/** Input values for a fish treat reward calculation. */
data class LessonRewardInput(
    val completed: Boolean,
    val improvedAfterRetry: Boolean,
    val crossedMasteryThreshold: Boolean,
)

// ─── 5-Tier Grant Calculation (v2 — product contract) ───

/**
 * Calculate all eligible fish-treat grants for a lesson attempt.
 * Returns a list of up to 5 grants, each with a unique stable source key.
 */
fun FishTreatPolicy.calculateGrants(
    childId: String,
    lessonId: String,
    attemptId: String,
    localDate: String, // yyyy-MM-dd
    isFirstCompletion: Boolean,
    currentScore: Double,
    priorBestScore: Double?,
    masteryTransitions: List<MasteryTransition>,
    questId: String?,
    questQualifies: Boolean,
): List<Grant> {
    val grants = mutableListOf<Grant>()

    // 1. First lesson completion: 5
    if (isFirstCompletion) {
        grants += Grant("lesson-first:$childId:$lessonId", 5)
    }

    // 2. Meaningful improvement (≥10pp above prior best, once per child/lesson/date)
    val prior = priorBestScore ?: 0.0
    if (currentScore - prior >= 10.0 && priorBestScore != null) {
        grants += Grant("lesson-improve:$childId:$lessonId:$localDate", 2)
    }

    // 3. Mastery transitions
    masteryTransitions.forEach { t ->
        when (t.to.uppercase()) {
            "PROFICIENT" -> grants += Grant("mastery-proficient:$childId:${t.skillId}", 3)
            "MASTERED" -> grants += Grant("mastery-mastered:$childId:${t.skillId}", 5)
        }
    }

    // 5. Cat request completed: 2
    if (questId != null && questQualifies) {
        grants += Grant("quest-complete:$childId:$questId", 2)
    }

    return grants
}
