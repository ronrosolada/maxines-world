package com.maxinesworld.featurelessonplayer

import androidx.room.withTransaction
import com.maxinesworld.coredatabase.*
import com.maxinesworld.coremodel.gamification.AttemptQualification
import com.maxinesworld.coremodel.gamification.FishTreatPolicy
import com.maxinesworld.coremodel.gamification.WildlifeDiscoveryMetadata
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.playground.DailyQuestSeedPolicy
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CompletionInput(
    val childId: String,
    val lessonId: String,
    val attemptId: String,
    val subject: String,
    val completedAtEpochMillis: Long,
    val improvedAfterRetry: Boolean,
    val crossedMasteryThreshold: Boolean,
    val scoredResults: List<ScoredResultInput>,
    val wildlifeDiscovery: WildlifeDiscoveryMetadata?,
    val attemptQualification: AttemptQualification,
)

data class ScoredResultInput(
    val activityId: String,
    val skillId: String,
    val eventType: String,
    val accuracy: Float,
    val attempts: Int,
    val hintsUsed: Int,
    val responseTimeMs: Long,
    val timestamp: Long,
)

data class CompletionReceipt(
    val completionId: String,
    val fishTreats: Int,
    val wildlifeBadge: com.maxinesworld.coremodel.CollectibleBadge?,
)

@Singleton
class LessonCompletionStore @Inject constructor(
    private val db: MaxinesDatabase,
    private val lessonCompletionDao: LessonCompletionDao,
    private val progressEventDao: ProgressEventDao,
    private val rewardLedgerDao: RewardLedgerDao,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val playgroundUnlockReceiptDao: PlaygroundUnlockReceiptDao,
    private val badgeAwarder: BadgeAwarder,
) {

    /**
     * Single Room transaction that commits lesson completion, progress events,
     * fish-treat ledger credit, wildlife badge evaluation, and daily quest
     * reconciliation — or rolls back everything on any failure.
     */
    suspend fun complete(input: CompletionInput): CompletionReceipt =
        db.withTransaction {
            // Idempotency guard: attempt-scoped, not lesson-scoped.
            lessonCompletionDao.getByAttempt(input.childId, input.lessonId, input.attemptId)
                ?.let { return@withTransaction CompletionReceipt(it.id, 0, null) }

            val completionId = stableId("completion", input.childId, input.lessonId, input.attemptId)
            check(lessonCompletionDao.insertIgnoring(
                LessonCompletionEntity(
                    id = completionId,
                    childId = input.childId,
                    lessonId = input.lessonId,
                    attemptId = input.attemptId,
                    accuracy = input.scoredResults.map { it.accuracy.toDouble() }.average().takeIf { !it.isNaN() } ?: 0.0,
                    completedAtEpochMillis = input.completedAtEpochMillis,
                )
            ) != -1L)

            // Progress events
            for (result in input.scoredResults) {
                progressEventDao.insert(
                    ProgressEventEntity(
                        id = stableId("progress", completionId, result.activityId),
                        childId = input.childId,
                        skillId = result.skillId,
                        lessonId = input.lessonId,
                        activityId = result.activityId,
                        eventType = result.eventType,
                        accuracy = result.accuracy.toDouble(),
                        attempts = result.attempts,
                        hintsUsed = result.hintsUsed,
                        responseTimeMs = result.responseTimeMs,
                        timestamp = result.timestamp,
                        syncStatus = "LOCAL",
                    )
                )
            }

            // Fish treats → reward_ledger (authoritative balance)
            val treats = FishTreatPolicy.amount(
                completed = true,
                improvedAfterRetry = input.improvedAfterRetry,
                crossedMasteryThreshold = input.crossedMasteryThreshold,
            )
            val rewardKey = FishTreatPolicy.rewardKey(input.childId, input.lessonId, input.attemptId)
            rewardLedgerDao.insertIgnoring(
                RewardLedgerEntity(
                    id = stableId("ledger", rewardKey),
                    childId = input.childId,
                    amount = treats,
                    sourceKey = rewardKey,
                    occurredAtEpochMillis = input.completedAtEpochMillis,
                )
            )

            // Wildlife badge evaluation (inside transaction)
            val wildlifeBadge = badgeAwarder.evaluateAndAwardWildlifeBadge(
                childId = input.childId,
                lessonMetadata = input.wildlifeDiscovery,
                attemptFacts = input.attemptQualification,
            )

            // Daily quest reconciliation
            recordAssignedQuestAndMaybeUnlock(input, completionId)

            CompletionReceipt(completionId, treats, wildlifeBadge)
        }

    private suspend fun recordAssignedQuestAndMaybeUnlock(input: CompletionInput, completionEventId: String) {
        val questId = LessonPlayerViewModel.subjectToQuestId(input.subject) ?: return
        val today = java.time.LocalDate.now().toString()
        val seedIds = DailyQuestSeedPolicy.assign(input.childId, today)

        // Seed the quest set (idempotent INSERT IGNORE)
        dailyQuestSetDao.insertIgnoring(
            DailyQuestSetEntity(
                id = stableId("quest-set", input.childId, today),
                childId = input.childId,
                dayKey = today,
                assignedQuestIds = seedIds.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" },
                assignedAtEpochMillis = input.completedAtEpochMillis,
            )
        )

        // Record this completion
        dailyQuestCompletionDao.insertIgnoring(
            DailyQuestCompletionEntity(
                id = stableId("quest-completion", input.childId, today, questId),
                childId = input.childId,
                dayKey = today,
                questId = questId,
                completionEventId = completionEventId,
                completedAtEpochMillis = input.completedAtEpochMillis,
            )
        )

        // Check if all assigned quests are now complete
        val completedIds = dailyQuestCompletionDao.getCompletedQuestIds(input.childId, today).toSet()
        if (completedIds.containsAll(seedIds) && !playgroundUnlockReceiptDao.existsByChildAndDay(input.childId, today)) {
            playgroundUnlockReceiptDao.insertIgnoring(
                PlaygroundUnlockReceiptEntity(
                    id = stableId("unlock-receipt", input.childId, today),
                    childId = input.childId,
                    dayKey = today,
                    sourceQuestSetHash = DailyQuestSeedPolicy.questSetHash(seedIds),
                    unlockedAtEpochMillis = input.completedAtEpochMillis,
                )
            )
        }
    }

    companion object {
        fun stableId(prefix: String, vararg parts: String): String {
            val input = parts.joinToString(":")
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .take(8)
                .joinToString("") { "%02x".format(it) }
            return "$prefix:$hash"
        }
    }
}
