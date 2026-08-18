package com.maxinesworld.featurerewards

/**
 * 30-Minute Video Watch-to-Earn Policy:
 * 1 Wildlife Sticker unlocked per cumulative 1800 accredited seconds (30 mins)
 * watched with a passing assessment score (>= 80%).
 */
object VideoWatchRewardPolicy {
    const val SECONDS_PER_STICKER = 1800 // 30 minutes
    const val MINIMUM_PASSING_QUIZ_SCORE = 0.80f

    fun calculateEarnedStickers(totalAccreditedSeconds: Int): Int {
        return (totalAccreditedSeconds.coerceAtLeast(0) / SECONDS_PER_STICKER)
    }

    fun calculateProgressTowardNextSticker(totalAccreditedSeconds: Int): Float {
        val safeSeconds = totalAccreditedSeconds.coerceAtLeast(0)
        val remainder = safeSeconds % SECONDS_PER_STICKER
        return remainder.toFloat() / SECONDS_PER_STICKER.toFloat()
    }

    fun remainingSecondsToNextSticker(totalAccreditedSeconds: Int): Int {
        val safeSeconds = totalAccreditedSeconds.coerceAtLeast(0)
        val remainder = safeSeconds % SECONDS_PER_STICKER
        return SECONDS_PER_STICKER - remainder
    }
}
