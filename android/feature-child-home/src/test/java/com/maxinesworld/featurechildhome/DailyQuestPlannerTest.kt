package com.maxinesworld.featurechildhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DailyQuestPlannerTest {
    @Test
    fun `min hash value still produces a valid quest index`() {
        assertEquals(2, DailyQuestPlanner.questStartIndex(Int.MIN_VALUE, 5))
    }

    @Test
    fun `selects stable unfinished lessons for a child and day`() {
        val available = listOf("math-1", "english-1", "science-1", "filipino-1", "gmrc-1")
        val completed = setOf("math-1")

        val first = DailyQuestPlanner.selectQuestIds("child-1", "2026-08-04", completed, available)
        val second = DailyQuestPlanner.selectQuestIds("child-1", "2026-08-04", completed, available)

        assertEquals(first, second)
        assertEquals(3, first.size)
        assertFalse(first.contains("math-1"))
    }
}
