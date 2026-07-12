package com.maxinesworld.enginemastery

import com.maxinesworld.coremodel.MasteryState
import com.maxinesworld.coremodel.ProgressEvent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasteryEngine @Inject constructor() {

    /**
     * Compute mastery from progress events. Per audit requirements:
     * - Recent accuracy ≥ 80% (last 5 events)
     * - At least 2 different activity types used (variation check)
     * - Limited hint dependence (fewer than 2 hints per event on average)
     * - At least one delayed-review event (different calendar day)
     * - Minimum attempt count before mastery
     */
    fun computeMastery(events: List<ProgressEvent>): MasteryState {
        if (events.isEmpty()) return MasteryState.NOT_STARTED

        val total = events.size
        val correct = events.count { it.accuracy >= 0.8 }
        val accuracy = if (total > 0) correct.toDouble() / total else 0.0

        // Recent accuracy: last 5 events
        val recent = events.takeLast(5)
        val recentAccuracy = if (recent.isNotEmpty()) {
            recent.count { it.accuracy >= 0.8 }.toDouble() / recent.size
        } else 0.0

        // Activity variation: count distinct activity types
        val distinctTypes = events.map { it.activityId.substringAfterLast("_") }.distinct().size

        // Delayed review: check for events on different calendar days
        val distinctDays = events.map {
            TimeUnit.MILLISECONDS.toDays(it.timestamp)
        }.distinct().size

        // Hint dependence: average hints per event
        val avgHints = if (total > 0) events.map { it.hintsUsed }.average() else 0.0

        val canMaster = recentAccuracy >= 0.8
                && distinctTypes >= 2
                && distinctDays >= 2
                && avgHints < 2.0
                && total >= 10

        val canBeProficient = recentAccuracy >= 0.6
                && distinctTypes >= 1
                && total >= 5

        return when {
            total < 2 -> MasteryState.INTRODUCED
            total < 5 -> MasteryState.PRACTICING
            canMaster -> MasteryState.MASTERED
            canBeProficient -> MasteryState.PROFICIENT
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
