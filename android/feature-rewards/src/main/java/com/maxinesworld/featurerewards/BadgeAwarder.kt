package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.*
import com.maxinesworld.coremodel.CollectibleBadge
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core badge award mechanic: Daily Five-Subject Challenge.
 * Award one badge per day when all 5 subjects are completed.
 * Idempotent — repeated calls on the same day never double-award.
 */
@Singleton
class BadgeAwarder @Inject constructor(
    private val dailyChallengeDao: DailyChallengeDao,
    private val collectedBadgeDao: CollectedBadgeDao,
    private val badgeLoader: BadgeLoader
) {
    companion object {
        val SUBJECTS = listOf("english", "filipino", "mathematics", "science", "makabansa")
    }

    /**
     * Called after a lesson is completed. Records the subject as done for today.
     * If all 5 subjects are now complete, awards the next badge.
     * Returns: (challenge state, newly awarded badge or null)
     */
    suspend fun recordSubjectCompletion(
        childId: String,
        subject: String
    ): ChallengeProgress {
        val today = todayDate()
        val existing = dailyChallengeDao.getByChildAndDate(childId, today)
        val challenge = existing ?: DailyChallengeEntity(
            id = "${childId}_$today",
            childId = childId,
            challengeDate = today
        )

        val updated = when (subject) {
            "english" -> challenge.copy(englishCompleted = true, updatedAtEpochMillis = System.currentTimeMillis())
            "filipino" -> challenge.copy(filipinoCompleted = true, updatedAtEpochMillis = System.currentTimeMillis())
            "mathematics" -> challenge.copy(mathematicsCompleted = true, updatedAtEpochMillis = System.currentTimeMillis())
            "science" -> challenge.copy(scienceCompleted = true, updatedAtEpochMillis = System.currentTimeMillis())
            "makabansa" -> challenge.copy(makabansaCompleted = true, updatedAtEpochMillis = System.currentTimeMillis())
            else -> challenge
        }

        val completedCount = listOf(updated.englishCompleted, updated.filipinoCompleted,
            updated.mathematicsCompleted, updated.scienceCompleted, updated.makabansaCompleted).count { it }

        val allDone = completedCount == 5 && !updated.badgeAwarded
        val awardedBadge = if (allDone) {
            val badge = awardNextBadge(childId, today)
            dailyChallengeDao.upsert(updated.copy(allCompleted = true, badgeAwarded = true, awardedBadgeId = badge?.id))
            badge
        } else {
            dailyChallengeDao.upsert(updated)
            null
        }

        return ChallengeProgress(
            english = updated.englishCompleted, filipino = updated.filipinoCompleted,
            mathematics = updated.mathematicsCompleted, science = updated.scienceCompleted,
            makabansa = updated.makabansaCompleted, completedCount = completedCount,
            newlyAwardedBadge = awardedBadge
        )
    }

    private suspend fun awardNextBadge(childId: String, today: String): CollectibleBadge? {
        val allBadges = badgeLoader.loadAll()
        val earnedIds = collectedBadgeDao.getAllByChild(childId).map { it.badgeId }.toSet()
        val nextBadge = allBadges.firstOrNull { it.id !in earnedIds } ?: return null

        collectedBadgeDao.insert(CollectedBadgeEntity(
            id = "${childId}_${nextBadge.id}",
            childId = childId,
            badgeId = nextBadge.id,
            biome = nextBadge.biome,
            earnedDate = today
        ))
        return nextBadge
    }

    suspend fun getTodayProgress(childId: String): ChallengeProgress {
        val today = todayDate()
        val challenge = dailyChallengeDao.getByChildAndDate(childId, today)
            ?: return ChallengeProgress()
        return ChallengeProgress(
            english = challenge.englishCompleted, filipino = challenge.filipinoCompleted,
            mathematics = challenge.mathematicsCompleted, science = challenge.scienceCompleted,
            makabansa = challenge.makabansaCompleted,
            completedCount = listOf(challenge.englishCompleted, challenge.filipinoCompleted,
                challenge.mathematicsCompleted, challenge.scienceCompleted, challenge.makabansaCompleted).count { it },
            newlyAwardedBadge = null
        )
    }

    suspend fun getCollectedBadges(childId: String): List<CollectibleBadge> {
        val earnedIds = collectedBadgeDao.getAllByChild(childId).map { it.badgeId }.toSet()
        val all = badgeLoader.loadAll()
        return all.map { it.copy(isCollected = it.id in earnedIds) }
    }

    suspend fun getCollectedCount(childId: String): Int = collectedBadgeDao.countByChild(childId)
    suspend fun getCollectedByBiome(childId: String, biome: String): Int = collectedBadgeDao.countByBiome(childId, biome)

    private fun todayDate(): String = LocalDate.now(ZoneId.systemDefault()).toString()
}

data class ChallengeProgress(
    val english: Boolean = false, val filipino: Boolean = false,
    val mathematics: Boolean = false, val science: Boolean = false,
    val makabansa: Boolean = false, val completedCount: Int = 0,
    val newlyAwardedBadge: CollectibleBadge? = null
)
