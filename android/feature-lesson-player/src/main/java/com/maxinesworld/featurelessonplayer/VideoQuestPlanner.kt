package com.maxinesworld.featurelessonplayer

import kotlin.math.abs

/**
 * "Today's Video Quest" planner.
 *
 * Selects a small set of the child's NEXT-UNLOCKED lesson videos drawn from
 * DIFFERENT subjects (2-3) whose combined accredited watch time lands in the
 * [30, 40] minute band. Selection is deterministic per (childId, dayKey) and
 * derived entirely from the current frontier (the first not-yet-passed lesson
 * of each subject), so no additional persistence is required — completion is
 * derived from the existing video-watch ledger pass state. If the contract
 * cannot be satisfied, no quest is proposed.
 */
object VideoQuestPlanner {

    /** Lower bound: quest must total at least 30 minutes (1800s). */
    const val MIN_SECONDS = 1800

    /** Upper bound: quest must not total more than 40 minutes (2400s). */
    const val MAX_SECONDS = 2400

    /** Prefer 2 subjects minimum, 3 maximum, always from different subjects. */
    const val MIN_SUBJECTS = 2
    const val MAX_SUBJECTS = 3

    /** A frontier video eligible to be part of the quest. */
    data class Candidate(
        val mediaId: String,
        val subjectId: String,
        val durationSeconds: Int,
    )

    /** Deterministic, stable rotation offset (mirrors DailyQuestPlanner). */
    internal fun startIndex(hash: Int, size: Int): Int {
        require(size > 0) { "Pool must not be empty" }
        val safe = if (hash == Int.MIN_VALUE) Int.MAX_VALUE else abs(hash)
        return safe % size
    }

    /**
     * Deterministically choose the quest's videos from the given frontier (the
     * unlocked video of each subject) so that the combined accredited duration
     * is within [MIN_SECONDS, MAX_SECONDS] and at least MIN_SUBJECTS different
     * subjects are represented. Returns the selected mediaIds in watch order, or
     * an empty list when there is nothing to propose.
     */
    fun select(childId: String, dayKey: String, frontier: List<Candidate>): List<String> {
        if (frontier.isEmpty()) return emptyList()

        val bySubject = frontier
            .filter { it.subjectId.isNotBlank() }
            .distinctBy { it.subjectId }
            .associateBy { it.subjectId }
        if (bySubject.size < MIN_SUBJECTS) return emptyList()

        val subjects = bySubject.keys.toList()
        val start = startIndex("$childId:$dayKey".hashCode(), subjects.size)
        val rotated = (0 until subjects.size)
            .map { subjects[(start + it) % subjects.size] }
        var best: List<Candidate>? = null

        fun consider(selection: List<Candidate>) {
            val total = selection.sumOf { it.durationSeconds }
            if (selection.size !in MIN_SUBJECTS..MAX_SUBJECTS ||
                total !in MIN_SECONDS..MAX_SECONDS
            ) {
                return
            }
            val currentBest = best
            val currentTotal = currentBest?.sumOf { it.durationSeconds }
            if (currentBest == null ||
                selection.size > currentBest.size ||
                (selection.size == currentBest.size &&
                    kotlin.math.abs(total - MIN_SECONDS) <
                    kotlin.math.abs((currentTotal ?: Int.MAX_VALUE) - MIN_SECONDS))
            ) {
                best = selection
            }
        }

        fun choose(nextIndex: Int, targetSize: Int, selected: List<Candidate>) {
            if (selected.size == targetSize) {
                consider(selected)
                return
            }
            val remaining = targetSize - selected.size
            val lastStart = rotated.size - remaining
            for (index in nextIndex..lastStart) {
                choose(
                    nextIndex = index + 1,
                    targetSize = targetSize,
                    selected = selected + bySubject.getValue(rotated[index]),
                )
            }
        }

        for (targetSize in MAX_SUBJECTS downTo MIN_SUBJECTS) {
            choose(nextIndex = 0, targetSize = targetSize, selected = emptyList())
        }

        return best?.map { it.mediaId }.orEmpty()
    }

    /** Quest is complete when every selected video has been passed. */
    fun isCompleted(selected: List<String>, passedMediaIds: Set<String>): Boolean =
        selected.isNotEmpty() && selected.all { it in passedMediaIds }

    /** Number of selected videos already passed (0..selected.size). */
    fun completedCount(selected: List<String>, passedMediaIds: Set<String>): Int =
        selected.count { it in passedMediaIds }

    /** Combined accredited seconds of the selected set (for display). */
    fun totalSeconds(selected: List<String>, byMediaId: Map<String, Int>): Int =
        selected.sumOf { byMediaId[it] ?: 0 }
}
