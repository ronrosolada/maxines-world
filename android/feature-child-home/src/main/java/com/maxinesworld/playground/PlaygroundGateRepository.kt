package com.maxinesworld.playground

import com.maxinesworld.coredatabase.DailyQuestCompletionDao
import com.maxinesworld.coredatabase.DailyQuestSetDao
import com.maxinesworld.coredatabase.PlaygroundUnlockReceiptDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaygroundGateRepository @Inject constructor(
    private val dayProvider: LocalDayProvider,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val playgroundUnlockReceiptDao: PlaygroundUnlockReceiptDao,
) {

    fun gate(childId: String): Flow<PlaygroundGateState> = flow {
        val dayKey = dayProvider.currentDayKey()
        val questSet = dailyQuestSetDao.getByChildAndDay(childId, dayKey)
        val completedIds = dailyQuestCompletionDao.getCompletedQuestIds(childId, dayKey).toSet()
        val hasReceipt = playgroundUnlockReceiptDao.existsByChildAndDay(childId, dayKey)

        val assignedIds = questSet?.assignedQuestIds?.let { json ->
            try {
                json.trimStart('[').trimEnd(']')
                    .split(",").map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }.toSet()
            } catch (_: Exception) { emptySet<String>() }
        }

        emit(
            PlaygroundGateEvaluator.evaluate(
                childId = childId,
                dayKey = dayKey,
                assignedQuestIds = if (questSet != null) assignedIds else null,
                completedQuestIds = if (questSet != null) completedIds else null,
                hasUnlockReceipt = hasReceipt,
            )
        )
    }
}
