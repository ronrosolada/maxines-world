package com.maxinesworld.featurerewards

import androidx.room.withTransaction
import com.maxinesworld.coredatabase.DailyQuestCompletionDao
import com.maxinesworld.coredatabase.DailyQuestCompletionEntity
import com.maxinesworld.coredatabase.DailyQuestSetDao
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.RewardBreakDao
import com.maxinesworld.coredatabase.RewardBreakPolicy
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** Result of reconciling the persisted Daily Quest for one child/day. */
data class DailyQuestRewardResult(
    val questComplete: Boolean = false,
    val newlyAwarded: Boolean = false,
    val sanctuaryPieceId: String? = null,
    val rewardBreakId: String? = null,
)

/**
 * Single writer for Daily Quest rewards.
 *
 * Lesson completion calls [reconcileInTransaction] while its completion
 * transaction is open. Home refresh calls [reconcile] to repair a process
 * death or a completion made before the quest set was refreshed. Every durable
 * grant is keyed by child/day, so retries cannot mint another reward.
 */
@Singleton
class DailyQuestRewardWriter @Inject constructor(
    private val database: MaxinesDatabase,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val rewardDao: RewardDao,
    private val rewardBreakDao: RewardBreakDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun reconcile(
        childId: String,
        dayKey: String = LocalDate.now(ZoneId.systemDefault()).toString(),
        completedLessonId: String? = null,
    ): DailyQuestRewardResult = database.withTransaction {
        reconcileInTransaction(childId, dayKey, completedLessonId)
    }

    /** Must be called from the caller's existing Room transaction. */
    suspend fun reconcileInTransaction(
        childId: String,
        dayKey: String,
        completedLessonId: String? = null,
    ): DailyQuestRewardResult {
        val set = dailyQuestSetDao.getByChildAndDay(childId, dayKey) ?: return DailyQuestRewardResult()
        val assigned = parseIds(set.assignedQuestIds).distinct()
        if (assigned.isEmpty()) return DailyQuestRewardResult()

        if (completedLessonId != null && completedLessonId in assigned) {
            dailyQuestCompletionDao.insertIgnoring(
                DailyQuestCompletionEntity(
                    id = "$childId:$dayKey:$completedLessonId",
                    childId = childId,
                    dayKey = dayKey,
                    questId = completedLessonId,
                    completionEventId = "lesson-completion:$childId:$completedLessonId",
                )
            )
        }

        val completed = dailyQuestCompletionDao
            .getCompletedQuestIds(childId, dayKey)
            .toSet()
        if (!assigned.all(completed::contains)) return DailyQuestRewardResult()

        val now = System.currentTimeMillis()
        val rewardId = "daily-quest:$childId:$dayKey:sanctuary-piece"
        val existingReward = rewardDao.getById(rewardId)
        val nextPieceId = SanctuaryCatalog.pieces
            .getOrNull(rewardDao.getTotalByType(childId, SANCTUARY_PIECE_TYPE) ?: 0)
            ?.id
        val pieceId = existingReward?.metadata ?: nextPieceId
        val inserted = if (existingReward == null && nextPieceId != null) {
            rewardDao.insertIgnoring(
                RewardEntity(
                    id = rewardId,
                    childId = childId,
                    type = SANCTUARY_PIECE_TYPE,
                    subject = "daily-quest",
                    amount = 1,
                    earnedAt = now,
                    metadata = nextPieceId,
                )
            )
        } else {
            -1L
        }

        val dailyQuestCompletionId = RewardBreakPolicy.dailyQuestCompletionId(childId, dayKey)
        val existingBreak = rewardBreakDao.getByQuestCompletion(dailyQuestCompletionId)
        if (existingBreak == null) {
            rewardBreakDao.insertIgnoring(
                RewardBreakPolicy.newEntitlement(
                    id = "reward-break:$childId:$dayKey",
                    childId = childId,
                    dailyQuestCompletionId = dailyQuestCompletionId,
                    nowEpochMillis = now,
                )
            )
        }
        val usableBreak = rewardBreakDao.getByQuestCompletion(dailyQuestCompletionId)
            ?.takeIf { RewardBreakPolicy.canUse(it, now) }

        return DailyQuestRewardResult(
            questComplete = true,
            newlyAwarded = inserted != -1L,
            sanctuaryPieceId = pieceId,
            rewardBreakId = usableBreak?.id,
        )
    }

    private fun parseIds(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }
            .getOrElse { raw.split('|').map(String::trim).filter(String::isNotEmpty) }

    companion object {
        const val SANCTUARY_PIECE_TYPE = "SANCTUARY_PIECE"
    }
}
