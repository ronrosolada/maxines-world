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
 * derived from the existing video-watch ledger pass state.
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
        val bySubject = frontier.groupBy { it.subjectId }
        val subjects = bySubject.keys.toList()
        if (subjects.isEmpty()) return emptyList()

        val start = startIndex("$childId:$dayKey".hashCode(), subjects.size)
        val rotated = (0 until subjects.size).map { subjects[(start + it) % subjects.size] }

        val selected = mutableListOf<Candidate>()
        var total = 0
        for (subject in rotated) {
            if (selected.size >= MAX_SUBJECTS) break
            val candidate = bySubject.getValue(subject).first()
            // Hard rule: never exceed the 40-minute ceiling. A video that would
            // push the quest over 40m is skipped (try the next subject instead).
            if (total + candidate.durationSeconds > MAX_SECONDS) continue
            // Stop once we have both the duration floor AND the cross-subject floor.
            val pastFloor = total >= MIN_SECONDS && selected.size >= MIN_SUBJECTS
            if (pastFloor) break
            selected += candidate
            total += candidate.durationSeconds
        }

        return selected.map { it.mediaId }
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
