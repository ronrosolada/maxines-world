package com.maxinesworld.coremodel

import kotlin.math.abs

/**
 * "Today's Video Quest" planner.
 *
 * Selects a small set of the child's next-unlocked videos drawn from different
 * subjects. Selection is deterministic per (childId, dayKey) and derived
 * entirely from the current frontier.
 */
object VideoQuestPlanner {

    /** Lower bound: quest must total at least 30 minutes (1800s). */
    const val MIN_SECONDS = 1800

    /** Upper bound: quest must not total more than 50 minutes (3000s). */
    const val MAX_SECONDS = 3000

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
     * is within [MIN_SECONDS, MAX_SECONDS] and at least [MIN_SUBJECTS] different
     * subjects are represented. Returns an empty list when no valid quest can
     * be proposed; a partial or out-of-contract quest is never persisted.
     */
    fun select(childId: String, dayKey: String, frontier: List<Candidate>): List<String> {
        val eligibleFrontier = frontier.filter {
            it.mediaId.isNotBlank() && it.subjectId.isNotBlank() && it.durationSeconds > 0
        }
        if (eligibleFrontier.isEmpty()) return emptyList()
        val bySubject = eligibleFrontier.groupBy { it.subjectId }
        val subjects = bySubject.keys.toList()
        if (subjects.isEmpty()) return emptyList()

        val start = startIndex("$childId:$dayKey".hashCode(), subjects.size)
        val rotated = (0 until subjects.size).map { subjects[(start + it) % subjects.size] }

        val selected = mutableListOf<Candidate>()
        var total = 0
        for (subject in rotated) {
            if (selected.size >= MAX_SUBJECTS) break
            val candidate = bySubject.getValue(subject).first()
            // Hard rule: never exceed the 50-minute ceiling. A video that would
            // push the quest over 50m is skipped (try the next subject instead).
            if (total + candidate.durationSeconds > MAX_SECONDS) continue
            // Stop once we have both the duration floor AND the subject floor.
            val pastFloor = total >= MIN_SECONDS && selected.size >= MIN_SUBJECTS
            if (pastFloor) break
            selected += candidate
            total += candidate.durationSeconds
        }

        val distinctSubjects = selected.map { it.subjectId }.distinct().size
        return selected
            .takeIf {
                it.size in MIN_SUBJECTS..MAX_SUBJECTS &&
                    distinctSubjects in MIN_SUBJECTS..MAX_SUBJECTS &&
                    total in MIN_SECONDS..MAX_SECONDS
            }
            ?.map { it.mediaId }
            ?: emptyList()
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
