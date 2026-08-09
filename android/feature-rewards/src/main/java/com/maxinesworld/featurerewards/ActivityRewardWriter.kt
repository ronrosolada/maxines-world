package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records the small immediate reward for every completed activity step.
 *
 * Activity paw prints are progress receipts, not spendable currency. The
 * deterministic id makes retries and renderer re-delivery harmless.
 */
@Singleton
class ActivityRewardWriter @Inject constructor(
    private val rewardDao: RewardDao,
) {
    suspend fun award(
        childId: String,
        lessonId: String,
        activityId: String,
    ): Boolean {
        if (childId.isBlank() || lessonId.isBlank() || activityId.isBlank()) return false
        val sourceKey = "activity:$childId:$lessonId:$activityId"
        return rewardDao.insertIgnoring(
            RewardEntity(
                id = sourceKey,
                childId = childId,
                type = ACTIVITY_PAW_TYPE,
                amount = 1,
                metadata = sourceKey,
            ),
        ) != -1L
    }

    companion object {
        const val ACTIVITY_PAW_TYPE = "ACTIVITY_PAW"
    }
}
