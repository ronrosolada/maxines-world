package com.maxinesworld.coremodel

import java.util.concurrent.TimeUnit

/**
 * Represents a skill or topic identified as needing spaced-repetition review
 * by Milo the cat guide.
 */
data class MiloReviewItem(
    val id: String,
    val skillId: String,
    val subjectId: String,
    val title: String,
    val reason: ReviewReason,
    val dueAtEpochMillis: Long,
    val priorityScore: Int,
)

enum class ReviewReason {
    NEEDS_REMEDY,      // Recent accuracy dropped below passing threshold
    SCHEDULED_SPACED,  // Interval elapsed since last practice (SM-2 loop)
    PREREQUISITE_GAP   // Prerequisite skill needs reinforcement
}

object MiloReviewQueueResolver {

    /**
     * Resolves up to [maxItems] due review challenges based on mastery state,
     * recency of practice, and error rates.
     */
    fun resolveDueItems(
        records: List<MasteryRecord>,
        nowEpochMillis: Long = System.currentTimeMillis(),
        maxItems: Int = 3,
    ): List<MiloReviewItem> {
        return records
            .filter { record ->
                record.state == MasteryState.NEEDS_REVIEW ||
                    (record.nextReviewAt != null && record.nextReviewAt <= nowEpochMillis)
            }
            .map { record ->
                val reason = if (record.state == MasteryState.NEEDS_REVIEW) {
                    ReviewReason.NEEDS_REMEDY
                } else {
                    ReviewReason.SCHEDULED_SPACED
                }

                // Higher priority score: NEEDS_REMEDY first, then most overdue
                val overdueDays = record.nextReviewAt?.let { next ->
                    TimeUnit.MILLISECONDS.toDays((nowEpochMillis - next).coerceAtLeast(0)).toInt()
                } ?: 5

                val priorityScore = if (reason == ReviewReason.NEEDS_REMEDY) {
                    100 + (100 - (record.accuracy * 100).toInt())
                } else {
                    50 + overdueDays.coerceAtMost(50)
                }

                MiloReviewItem(
                    id = "review:${record.childId}:${record.skillId}",
                    skillId = record.skillId,
                    subjectId = subjectForSkill(record.skillId),
                    title = "Review: ${record.skillId.replace('-', ' ').capitalizeWords()}",
                    reason = reason,
                    dueAtEpochMillis = record.nextReviewAt ?: nowEpochMillis,
                    priorityScore = priorityScore,
                )
            }
            .sortedByDescending { it.priorityScore }
            .take(maxItems)
    }

    private fun subjectForSkill(skillId: String): String = when {
        skillId.startsWith("math") -> "mathematics"
        skillId.startsWith("eng") -> "english"
        skillId.startsWith("fil") -> "filipino"
        skillId.startsWith("sci") -> "science"
        skillId.startsWith("maka") -> "makabansa"
        skillId.startsWith("gmrc") -> "gmrc"
        else -> "general"
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
