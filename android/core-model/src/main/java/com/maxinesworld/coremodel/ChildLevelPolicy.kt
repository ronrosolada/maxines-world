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
 *   Level 4: 12 lessons       (Kindness Garden cosmetic milestone)
 */
object ChildLevelPolicy {
    const val LESSONS_PER_LEVEL = 4
    /** Level milestone reserved for the cosmetic Kindness Garden; it does not gate GMRC. */
    const val KINDNESS_GARDEN_UNLOCK_LEVEL = 4
    @Deprecated("Use KINDNESS_GARDEN_UNLOCK_LEVEL; this milestone is cosmetic")
    const val KINDNESS_UNLOCK_LEVEL = KINDNESS_GARDEN_UNLOCK_LEVEL
    @Deprecated("Use lesson milestones only for cosmetic rewards")
    const val KINDNESS_UNLOCK_LESSONS = (KINDNESS_GARDEN_UNLOCK_LEVEL - 1) * LESSONS_PER_LEVEL // 12

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
