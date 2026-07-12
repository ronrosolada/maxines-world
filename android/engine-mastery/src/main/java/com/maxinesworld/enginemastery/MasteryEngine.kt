package com.maxinesworld.enginemastery

import com.maxinesworld.coremodel.MasteryState
import com.maxinesworld.coremodel.ProgressEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasteryEngine @Inject constructor() {

    fun computeMastery(events: List<ProgressEvent>): MasteryState {
        if (events.isEmpty()) return MasteryState.NOT_STARTED
        val total = events.size
        val correct = events.count { it.accuracy >= 0.8 }
        val accuracy = if (total > 0) correct.toDouble() / total else 0.0

        return when {
            total < 2 -> MasteryState.INTRODUCED
            total < 5 -> MasteryState.PRACTICING
            accuracy >= 0.8 && total >= 10 -> MasteryState.MASTERED
            accuracy >= 0.6 -> MasteryState.PROFICIENT
            accuracy < 0.6 && total >= 5 -> MasteryState.NEEDS_REVIEW
            else -> MasteryState.PRACTICING
        }
    }

    fun nextReviewDays(state: MasteryState): Int = when (state) {
        MasteryState.INTRODUCED -> 1
        MasteryState.PRACTICING -> 3
        MasteryState.PROFICIENT -> 7
        MasteryState.MASTERED -> 30
        MasteryState.NEEDS_REVIEW -> 1
        MasteryState.NOT_STARTED -> 0
    }
}
