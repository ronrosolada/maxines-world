package com.maxinesworld.playground

data class PlaygroundGateState(
    val childId: String,
    val dayKey: String,
    val totalAssigned: Int,
    val completed: Int,
    val status: PlaygroundGateStatus
) {
    init {
        require(totalAssigned >= 0) { "totalAssigned must be >= 0" }
        require(completed >= 0) { "completed must be >= 0" }
        require(completed <= totalAssigned) { "completed ($completed) cannot exceed total ($totalAssigned)" }
    }

    val isUnlocked: Boolean get() = status == PlaygroundGateStatus.Unlocked
}

enum class PlaygroundGateStatus { Loading, Locked, Unlocked, NoQuests, Error }

object PlaygroundGateEvaluator {
    fun evaluate(
        childId: String,
        dayKey: String,
        assignedQuestIds: Set<String>?,
        completedQuestIds: Set<String>?,
        hasUnlockReceipt: Boolean,
        hasError: Boolean = false
    ): PlaygroundGateState {
        if (hasError) return PlaygroundGateState(childId, dayKey, 0, 0, PlaygroundGateStatus.Error)
        if (assignedQuestIds == null || completedQuestIds == null) {
            return PlaygroundGateState(childId, dayKey, 0, 0, PlaygroundGateStatus.Loading)
        }
        val validCompleted = completedQuestIds.count { it in assignedQuestIds }
        val status = when {
            hasUnlockReceipt -> PlaygroundGateStatus.Unlocked
            assignedQuestIds.isEmpty() -> PlaygroundGateStatus.NoQuests
            validCompleted == assignedQuestIds.size -> PlaygroundGateStatus.Unlocked
            else -> PlaygroundGateStatus.Locked
        }
        return PlaygroundGateState(childId, dayKey, assignedQuestIds.size, validCompleted, status)
    }
}
