package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.*
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coremodel.gamification.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Badge award mechanics.
 * Daily Five-Subject Challenge is preserved for legacy quest tracking.
 * Wildlife badges are awarded ONLY via content-metadata mapping
 * (WildlifeBadgeEvaluator), never by catalog order.
 */
@Singleton
class BadgeAwarder @Inject constructor(
    private val dailyChallengeDao: DailyChallengeDao,
    private val collectedBadgeDao: CollectedBadgeDao,
    private val badgeLoader: BadgeLoader,
) {
    companion object {
        val SUBJECTS = listOf("english", "filipino", "mathematics", "science", "makabansa")
    }

    /**
     * Legacy daily challenge tracking (preserved for quests).
     * Wildlife badge awards have been moved to WildlifeBadgeEvaluator.
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
        if (allDone) {
            dailyChallengeDao.upsert(updated.copy(allCompleted = true, badgeAwarded = true))
        } else {
            dailyChallengeDao.upsert(updated)
        }

        return ChallengeProgress(
            english = updated.englishCompleted, filipino = updated.filipinoCompleted,
            mathematics = updated.mathematicsCompleted, science = updated.scienceCompleted,
            makabansa = updated.makabansaCompleted, completedCount = completedCount,
            newlyAwardedBadge = null, // wildlife badges now come from evaluator
        )
    }

    /**
     * Award a wildlife badge using content-metadata evaluation.
     * Returns: (badge display info, or null if not eligible)
     */
    suspend fun evaluateAndAwardWildlifeBadge(
        childId: String,
        lessonMetadata: WildlifeDiscoveryMetadata?,
        attemptFacts: AttemptQualification,
    ): CollectibleBadge? {
        val catalog = badgeLoader.loadAll()
        val alreadyCollected = collectedBadgeDao.getAllByChild(childId).map { it.badgeId }.toSet()

        return when (val evaluation = WildlifeBadgeEvaluator.evaluate(catalog, lessonMetadata, attemptFacts, alreadyCollected)) {
            is BadgeEvaluation.Award -> {
                val badge = catalog.first { it.id == evaluation.badgeId }
                collectedBadgeDao.insert(CollectedBadgeEntity(
                    id = deterministicUuid("badge-award:$childId:${evaluation.badgeId}").toString(),
                    childId = childId,
                    badgeId = evaluation.badgeId,
                    biome = badge.biome,
                    earnedDate = todayDate(),
                ))
                badge.copy(isCollected = true)
            }
            else -> null
        }
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
            newlyAwardedBadge = null,
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
