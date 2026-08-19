package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coredatabase.LessonCompletionEntity
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.ProgressEventEntity
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coredatabase.RoomTransactionRunner
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coremodel.LessonManifest
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import com.maxinesworld.featurerewards.DailyQuestRewardResult
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import com.maxinesworld.featurerewards.LessonReward
import com.maxinesworld.featurerewards.LessonRewardPolicy
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Observable points at which tests may inject a transaction failure. */
enum class CompletionWriteStage {
    COMPLETION_INSERTED,
    PROGRESS_EVENTS_INSERTED,
    STARS_INSERTED,
    COINS_INSERTED,
    EXPEDITION_WRITTEN,
    BADGE_INSERTED,
    DAILY_QUEST_WRITTEN,
}

/** No-op in production; replace in tests to exercise rollback paths. */
@Singleton
open class CompletionWriteFailureInjector @Inject constructor() {
    open suspend fun after(stage: CompletionWriteStage) = Unit
}

data class LessonCompletionPersistenceResult(
    val completionInserted: Boolean,
    val alreadyCompleted: Boolean = false,
    val starsEarned: Int = 0,
    val coinsEarned: Int = 0,
    val expeditionProgress: ChallengeProgress = ChallengeProgress(),
    val badgeAwarded: CollectibleBadge? = null,
    val dailyQuestCompleted: Boolean = false,
    val sanctuaryPieceId: String? = null,
    val rewardBreakId: String? = null,
)

/**
 * Persists a lesson completion and every first-completion side effect in one
 * Room transaction. The ViewModel owns UI state only; it does not coordinate
 * multiple durable writes.
 */
@Singleton
class LessonCompletionRepository @Inject constructor(
    private val transactionRunner: RoomTransactionRunner,
    private val progressEventDao: ProgressEventDao,
    private val rewardDao: RewardDao,
    private val lessonCompletionDao: LessonCompletionDao,
    private val badgeAwarder: BadgeAwarder,
    private val dailyQuestRewardWriter: DailyQuestRewardWriter,
    private val failureInjector: CompletionWriteFailureInjector,
) {
    suspend fun complete(
        childId: String,
        lesson: LessonManifest,
        scoredResults: List<ActivityResult>,
        passedOnFirstAttempt: Boolean = true,
    ): LessonCompletionPersistenceResult {
        require(childId.isNotBlank()) { "childId must not be blank" }
        require(scoredResults.isNotEmpty()) { "a completion needs scored results" }

        // BadgeLoader may read bundled files. Do that before opening Room's
        // transaction; only database work belongs inside the transaction.
        val badgeCatalog = badgeAwarder.loadBadgeCatalog()
        return transactionRunner.run {
            // Room serializes transactions on this database. This check and the
            // deterministic insert below therefore have one first-completion
            // winner even when two completion requests arrive concurrently.
            if (lessonCompletionDao.exists(childId, lesson.id)) {
                // Replay: no side effects are written, but the UI still needs
                // the child's current expedition progress (not defaults).
                return@run LessonCompletionPersistenceResult(
                    completionInserted = false,
                    alreadyCompleted = true,
                    expeditionProgress = badgeAwarder.getExpeditionProgress(childId),
                )
            }

            val accuracy = scoredResults.count { it.correct }.toDouble() / scoredResults.size
            val isFirstLessonEver = lessonCompletionDao.countDistinctLessons(childId) == 0
            val completionKey = "lesson-completion:$childId:${lesson.id}"
            val inserted = lessonCompletionDao.insertIgnoring(
                LessonCompletionEntity(
                    id = completionKey,
                    childId = childId,
                    lessonId = lesson.id,
                    attemptId = completionKey,
                    accuracy = accuracy,
                    passedOnFirstAttempt = passedOnFirstAttempt,
                    completedAtEpochMillis = System.currentTimeMillis(),
                )
            )
            if (inserted == -1L) {
                return@run LessonCompletionPersistenceResult(
                    completionInserted = false,
                    alreadyCompleted = true,
                    expeditionProgress = badgeAwarder.getExpeditionProgress(childId),
                )
            }
            failureInjector.after(CompletionWriteStage.COMPLETION_INSERTED)

            scoredResults.forEach { result ->
                progressEventDao.insertIgnoring(
                    ProgressEventEntity(
                        id = "activity-result:$childId:${lesson.id}:${result.activityId}",
                        childId = childId,
                        skillId = lesson.skillIds.firstOrNull() ?: lesson.id,
                        lessonId = lesson.id,
                        activityId = result.activityId,
                        eventType = "activity_result",
                        accuracy = if (result.correct) 1.0 else 0.0,
                        attempts = result.attempts,
                        hintsUsed = result.hintsUsed,
                        responseTimeMs = result.responseTimeMs,
                    )
                )
            }
            failureInjector.after(CompletionWriteStage.PROGRESS_EVENTS_INSERTED)

            val rewardKey = "lesson-first:$childId:${lesson.id}"
            // Reward contract: only ASSESSED lessons earn stars/coins, expedition
            // progress, and quest rewards. An assessmentless lesson still records
            // its completion + progress events so the child advances, but it grants
            // no currency or level/quest rewards (the 30-min watch / >=80% quiz
            // contract has nothing to validate against).
            val rewardsEligible = lesson.assessment != null
            val lessonReward = if (rewardsEligible) {
                LessonRewardPolicy.forAccuracy(accuracy)
            } else {
                LessonReward(stars = 0, coins = 0)
            }
            if (rewardsEligible) {
                rewardDao.insertIgnoring(
                    RewardEntity(
                        id = "$rewardKey:STAR",
                        childId = childId,
                        type = "STAR",
                        subject = lesson.subject,
                        amount = lessonReward.stars,
                        metadata = rewardKey,
                    )
                )
                failureInjector.after(CompletionWriteStage.STARS_INSERTED)
                rewardDao.insertIgnoring(
                    RewardEntity(
                        id = "$rewardKey:COIN",
                        childId = childId,
                        type = "COIN",
                        subject = lesson.subject,
                        amount = lessonReward.coins,
                        metadata = rewardKey,
                    )
                )
                failureInjector.after(CompletionWriteStage.COINS_INSERTED)
            }

            val expedition = if (rewardsEligible) {
                badgeAwarder.recordLessonCompletion(
                    childId = childId,
                    subject = lesson.subject,
                    lessonId = lesson.id,
                    badgeCatalog = badgeCatalog,
                )
            } else {
                ChallengeProgress()
            }
            failureInjector.after(CompletionWriteStage.EXPEDITION_WRITTEN)
            val firstStepsSticker = if (rewardsEligible && isFirstLessonEver) {
                badgeAwarder.recordFirstLessonCompletion(childId, badgeCatalog)
            } else {
                null
            }
            if (firstStepsSticker != null || expedition.newlyAwardedBadge != null) {
                failureInjector.after(CompletionWriteStage.BADGE_INSERTED)
            }

            val dailyQuestReward = if (rewardsEligible) {
                dailyQuestRewardWriter.reconcileInTransaction(
                    childId = childId,
                    dayKey = LocalDate.now(ZoneId.systemDefault()).toString(),
                    completedLessonId = lesson.id,
                )
            } else {
                DailyQuestRewardResult()
            }
            failureInjector.after(CompletionWriteStage.DAILY_QUEST_WRITTEN)

            LessonCompletionPersistenceResult(
                completionInserted = true,
                starsEarned = lessonReward.stars,
                coinsEarned = lessonReward.coins,
                expeditionProgress = expedition,
                badgeAwarded = firstStepsSticker ?: expedition.newlyAwardedBadge,
                dailyQuestCompleted = dailyQuestReward.questComplete,
                sanctuaryPieceId = dailyQuestReward.sanctuaryPieceId,
                rewardBreakId = dailyQuestReward.rewardBreakId,
            )
        }
    }
}
