package com.maxinesworld.featurerewards

/**
 * The child always receives a completion reward. Accuracy adds a small,
 * understandable mastery bonus; it never removes the base reward.
 */
data class LessonReward(
    val stars: Int,
    val coins: Int,
)

object LessonRewardPolicy {
    const val BASE_STARS = 1
    const val BASE_COINS = 1
    const val MASTERY_BONUS_COINS = 1
    const val FIRST_MASTERY_THRESHOLD = 0.80
    const val HIGH_MASTERY_THRESHOLD = 0.95

    fun forAccuracy(accuracy: Double): LessonReward {
        val safeAccuracy = accuracy.coerceIn(0.0, 1.0)
        return LessonReward(
            stars = BASE_STARS +
                (if (safeAccuracy >= FIRST_MASTERY_THRESHOLD) 1 else 0) +
                (if (safeAccuracy >= HIGH_MASTERY_THRESHOLD) 1 else 0),
            coins = BASE_COINS +
                if (safeAccuracy >= FIRST_MASTERY_THRESHOLD) MASTERY_BONUS_COINS else 0,
        )
    }
}
