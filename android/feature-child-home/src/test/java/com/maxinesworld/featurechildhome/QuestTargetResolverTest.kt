package com.maxinesworld.featurechildhome

import org.junit.Assert.assertEquals
import org.junit.Test

class QuestTargetResolverTest {
    @Test
    fun `duplicate assigned lessons are shown once`() {
        assertEquals(
            listOf("lesson-a", "lesson-b"),
            QuestTargetResolver.uniqueAssignedLessonIds(
                listOf("lesson-a", "lesson-b", "lesson-a")
            )
        )
    }
}
