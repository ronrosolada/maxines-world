package com.maxinesworld.coremodel

/**
 * Child progression level policy for the Playroom home.
 *
 * Level is derived from the number of DISTINCT lessons a child has completed
 * (not attempts), so replaying a lesson never inflates the level.
 *
 * Thresholds are deliberately gentle for a young learner:
 *   Level 1: 0 lessons        Level 5: 16 lessons
 *   Level 2: 4 lessons        Level 6: 20 lessons
 *   Level 3: 8 lessons        ...
 *   Level 4: 12 lessons       (Kindness island unlock — GMRC)
 */
object ChildLevelPolicy {
    const val LESSONS_PER_LEVEL = 4
    const val KINDNESS_UNLOCK_LEVEL = 4
    const val KINDNESS_UNLOCK_LESSONS = (KINDNESS_UNLOCK_LEVEL - 1) * LESSONS_PER_LEVEL // 12

    /** 1-based level for a child with [completedLessonCount] distinct lesson completions. */
    fun levelFor(completedLessonCount: Int): Int {
        require(completedLessonCount >= 0) { "completedLessonCount must be >= 0" }
        return completedLessonCount / LESSONS_PER_LEVEL + 1
    }

    /** Lessons still needed to reach [targetLevel] from [completedLessonCount]. 0 if already there. */
    fun lessonsRemainingTo(completedLessonCount: Int, targetLevel: Int): Int {
        require(targetLevel >= 1) { "targetLevel must be >= 1" }
        val targetLessons = (targetLevel - 1) * LESSONS_PER_LEVEL
        return (targetLessons - completedLessonCount).coerceAtLeast(0)
    }
}
