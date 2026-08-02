package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.WildlifeExpeditionDao
import com.maxinesworld.coredatabase.WildlifeExpeditionEntity
import com.maxinesworld.coremodel.CollectibleBadge
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent Wildlife Expedition reward policy.
 *
 * A child completes three distinct lessons from at least two learning areas
 * during the same local calendar week. Progress never resets between days,
 * GMRC counts like every other subject, and the next wildlife sticker is
 * awarded at most once per child/week.
 */
@Singleton
class BadgeAwarder @Inject constructor(
    private val wildlifeExpeditionDao: WildlifeExpeditionDao,
    private val collectedBadgeDao: CollectedBadgeDao,
    private val badgeLoader: BadgeLoader,
) {
    private val awardMutex = Mutex()

    companion object {
        const val EXPEDITION_TARGET_LESSONS = 3
        const val EXPEDITION_MIN_SUBJECTS = 2
        const val SUBJECT_MAKABANSA = "makabansa"

        /** Canonical learning-area keys used by the expedition. */
        val SUBJECTS = listOf(
            "english", "filipino", "mathematics", "science", SUBJECT_MAKABANSA, "gmrc"
        )
    }

    /** Record one distinct lesson completion in this week's expedition. */
    suspend fun recordLessonCompletion(
        childId: String,
        subject: String,
        lessonId: String,
    ): ChallengeProgress = awardMutex.withLock {
        val normalizedSubject = normalizeSubject(subject)
            ?: return@withLock getExpeditionProgressLocked(childId)
        if (lessonId.isBlank()) return@withLock getExpeditionProgressLocked(childId)

        val weekKey = currentWeekKey()
        val existing = wildlifeExpeditionDao.getByChildAndWeek(childId, weekKey)
            ?: WildlifeExpeditionEntity(
                id = "${childId}_$weekKey",
                childId = childId,
                weekKey = weekKey,
            )

        val lessonIds = existing.completedLessonIds.toSetValue()
        val subjectKeys = existing.subjectKeys.toSetValue()
        val updatedLessonIds = lessonIds + lessonId
        val updatedSubjectKeys = subjectKeys + normalizedSubject
        val qualifies = updatedLessonIds.size >= EXPEDITION_TARGET_LESSONS &&
            updatedSubjectKeys.size >= EXPEDITION_MIN_SUBJECTS

        val newlyAwarded = if (qualifies && !existing.badgeAwarded) {
            awardNextBadge(childId, weekKey)
        } else {
            null
        }

        val updated = existing.copy(
            completedLessonIds = updatedLessonIds.joinToString("|"),
            subjectKeys = updatedSubjectKeys.joinToString("|"),
            badgeAwarded = existing.badgeAwarded || qualifies,
            awardedBadgeId = existing.awardedBadgeId ?: newlyAwarded?.id,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        wildlifeExpeditionDao.upsert(updated)

        progressFrom(updated, newlyAwarded)
    }

    /**
     * Compatibility wrapper for callers that only have a subject. New lesson
     * code must call [recordLessonCompletion] so replaying one lesson cannot
     * count as another adventure.
     */
    @Deprecated("Use recordLessonCompletion(childId, subject, lessonId)")
    suspend fun recordSubjectCompletion(childId: String, subject: String): ChallengeProgress =
        recordLessonCompletion(childId, subject, "legacy:${normalizeSubject(subject) ?: subject}")

    /** Current week's expedition progress; unlike the old daily challenge it does not reset each day. */
    suspend fun getExpeditionProgress(childId: String): ChallengeProgress = awardMutex.withLock {
        getExpeditionProgressLocked(childId)
    }

    /** Compatibility name retained for existing home callers during the migration. */
    suspend fun getTodayProgress(childId: String): ChallengeProgress = getExpeditionProgress(childId)

    private suspend fun getExpeditionProgressLocked(childId: String): ChallengeProgress {
        val expedition = wildlifeExpeditionDao.getByChildAndWeek(childId, currentWeekKey())
            ?: return ChallengeProgress()
        return progressFrom(expedition)
    }

    private suspend fun awardNextBadge(childId: String, weekKey: String): CollectibleBadge? {
        val allBadges = badgeLoader.loadAll()
        val earnedIds = collectedBadgeDao.getAllByChild(childId).map { it.badgeId }.toSet()
        val nextBadge = allBadges.firstOrNull { it.id !in earnedIds } ?: return null

        collectedBadgeDao.insert(
            CollectedBadgeEntity(
                id = "${childId}_${nextBadge.id}",
                childId = childId,
                badgeId = nextBadge.id,
                biome = nextBadge.biome,
                earnedDate = weekKey,
            )
        )
        return nextBadge
    }

    suspend fun getCollectedBadges(childId: String): List<CollectibleBadge> {
        val earned = collectedBadgeDao.getAllByChild(childId).associateBy { it.badgeId }
        val all = badgeLoader.loadAll()
        return all.map { badge ->
            val record = earned[badge.id]
            badge.copy(
                isCollected = record != null,
                collectedAtEpochMillis = record?.earnedAtEpochMillis ?: 0L,
            )
        }
    }

    suspend fun getCollectedCount(childId: String): Int = collectedBadgeDao.countByChild(childId)

    suspend fun getCollectedByBiome(childId: String, biome: String): Int =
        collectedBadgeDao.countByBiome(childId, biome)

    private fun progressFrom(
        expedition: WildlifeExpeditionEntity,
        newlyAwarded: CollectibleBadge? = null,
    ): ChallengeProgress {
        val lessonIds = expedition.completedLessonIds.toSetValue()
        val subjectKeys = expedition.subjectKeys.toSetValue()
        return ChallengeProgress(
            english = "english" in subjectKeys,
            filipino = "filipino" in subjectKeys,
            mathematics = "mathematics" in subjectKeys,
            science = "science" in subjectKeys,
            makabansa = SUBJECT_MAKABANSA in subjectKeys,
            gmrc = "gmrc" in subjectKeys,
            completedCount = lessonIds.size,
            subjectCount = subjectKeys.size,
            expeditionComplete = expedition.badgeAwarded || (
                lessonIds.size >= EXPEDITION_TARGET_LESSONS &&
                    subjectKeys.size >= EXPEDITION_MIN_SUBJECTS
                ),
            newlyAwardedBadge = newlyAwarded,
        )
    }

    private fun normalizeSubject(subject: String): String? = when (subject.lowercase()) {
        "english" -> "english"
        "filipino" -> "filipino"
        "mathematics", "math" -> "mathematics"
        "science" -> "science"
        "makabansa", "araling_panlipunan", "araling-panlipunan" -> SUBJECT_MAKABANSA
        "gmrc" -> "gmrc"
        else -> null
    }

    private fun currentWeekKey(): String = LocalDate.now(ZoneId.systemDefault())
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .toString()
}

data class ChallengeProgress(
    val english: Boolean = false,
    val filipino: Boolean = false,
    val mathematics: Boolean = false,
    val science: Boolean = false,
    val makabansa: Boolean = false,
    val gmrc: Boolean = false,
    val completedCount: Int = 0,
    val subjectCount: Int = 0,
    val expeditionComplete: Boolean = false,
    val newlyAwardedBadge: CollectibleBadge? = null,
)

private fun String.toSetValue(): Set<String> =
    split('|').filter { it.isNotBlank() }.toSet()
