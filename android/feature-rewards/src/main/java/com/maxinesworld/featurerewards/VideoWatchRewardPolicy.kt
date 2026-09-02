package com.maxinesworld.featurerewards

import com.maxinesworld.coremodel.ChildFacingMediaPolicy
import com.maxinesworld.coremodel.MediaAsset

/**
 * 30-Minute Video Watch-to-Earn Policy:
 * 1 Wildlife Sticker unlocked per cumulative 1800 accredited seconds (30 mins)
 * watched with a passing assessment score (>= 80%).
 *
 * Child-home totals and first-pass stickers count only Grade 3 RELEASED
 * curriculum rows. Historical PREVIEW / other-grade ledger rows stay stored
 * and are ignored at read/reward time.
 */
object VideoWatchRewardPolicy {
    const val SECONDS_PER_STICKER = 1800 // 30 minutes
    const val MINIMUM_PASSING_QUIZ_SCORE = 0.80f

    fun shouldCreditCurriculumWatch(asset: MediaAsset): Boolean =
        ChildFacingMediaPolicy.isChildFacingCurriculum(asset)

    fun accreditedSecondsForChildFacing(
        passedEntries: Iterable<Pair<String, Int>>,
        catalog: List<MediaAsset>,
    ): Int {
        val allowed = ChildFacingMediaPolicy.childFacingMediaIds(catalog)
        return passedEntries
            .asSequence()
            .filter { (mediaId, _) -> mediaId in allowed }
            .sumOf { (_, seconds) -> seconds.coerceAtLeast(0) }
    }

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
