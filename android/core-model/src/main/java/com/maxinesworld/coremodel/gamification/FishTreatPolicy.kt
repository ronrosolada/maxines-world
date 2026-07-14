package com.maxinesworld.coremodel.gamification

data class LessonRewardInput(
    val completed: Boolean,
    val improvedAfterRetry: Boolean,
    val crossedMasteryThreshold: Boolean,
)

object FishTreatPolicy {
    const val TYPE = "FISH_TREAT"
    const val MAX_PER_ATTEMPT = 6

    fun amount(input: LessonRewardInput): Int {
        if (!input.completed) return 0
        return (
            3 +
                (if (input.improvedAfterRetry) 1 else 0) +
                (if (input.crossedMasteryThreshold) 2 else 0)
        ).coerceAtMost(MAX_PER_ATTEMPT)
    }

    fun rewardKey(childId: String, lessonId: String, attemptId: String): String =
        "lesson:$childId:$lessonId:$attemptId"
}
