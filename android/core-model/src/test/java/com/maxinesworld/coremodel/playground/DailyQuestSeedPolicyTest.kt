package com.maxinesworld.playground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyQuestSeedPolicyTest {

    @Test
    fun `sameChildAndDay_isStable`() {
        val childId = "child_a"
        val dayKey = "2026-07-14"
        val first = DailyQuestSeedPolicy.assign(childId, dayKey)
        val second = DailyQuestSeedPolicy.assign(childId, dayKey)
        assertEquals("same child+day produces identical assignment", first, second)
    }

    @Test
    fun `differentChild_isIndependentlyScoped`() {
        val dayKey = "2026-07-14"
        val a = DailyQuestSeedPolicy.assign("child_a", dayKey)
        val b = DailyQuestSeedPolicy.assign("child_b", dayKey)
        assertNotEquals("different children should not share identical assignments", a, b)
    }

    @Test
    fun `differentDay_isIndependentlyScoped`() {
        val childId = "child_c"
        val a = DailyQuestSeedPolicy.assign(childId, "2026-07-13")
        val b = DailyQuestSeedPolicy.assign(childId, "2026-07-14")
        assertNotEquals("different days should not share identical assignments", a, b)
    }

    @Test
    fun `returnsExactlyThreeUniqueQuests`() {
        val result = DailyQuestSeedPolicy.assign("any_child", "2026-07-14")
        assertEquals("must return 3 quests", 3, result.size)
        assertEquals("quests must be unique", 3, result.toSet().size)
        result.forEach { questId ->
            assertTrue("questId must start with 'subject:'", questId.startsWith("subject:"))
            assertTrue("questId must be in candidates", questId in DailyQuestSeedPolicy.candidates)
        }
    }

    @Test
    fun `questSetHash_isOrderIndependent`() {
        val idsA = listOf("subject:english", "subject:filipino", "subject:mathematics")
        val idsB = listOf("subject:mathematics", "subject:english", "subject:filipino")
        assertEquals(
            "hash must be identical regardless of input order",
            DailyQuestSeedPolicy.questSetHash(idsA),
            DailyQuestSeedPolicy.questSetHash(idsB)
        )
    }
}
