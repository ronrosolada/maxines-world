package com.maxinesworld.coremodel.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FishTreatPolicyTest {

    @Test
    fun `base reward for completed lesson`() {
        assertEquals(3, FishTreatPolicy.amount(true, false, false))
    }

    @Test
    fun `base plus retry improvement`() {
        assertEquals(4, FishTreatPolicy.amount(true, true, false))
    }

    @Test
    fun `base plus mastery bonus`() {
        assertEquals(5, FishTreatPolicy.amount(true, false, true))
    }

    @Test
    fun `base plus retry plus mastery`() {
        assertEquals(6, FishTreatPolicy.amount(true, true, true))
    }

    @Test
    fun `cap at max per attempt`() {
        // All bonuses applied should not exceed MAX_PER_ATTEMPT
        val result = FishTreatPolicy.amount(true, true, true)
        assertTrue(result <= FishTreatPolicy.MAX_PER_ATTEMPT)
    }

    @Test
    fun `no reward for incomplete lesson`() {
        assertEquals(0, FishTreatPolicy.amount(false, true, true))
    }

    @Test
    fun `reward key is deterministic`() {
        val key1 = FishTreatPolicy.rewardKey("child1", "lesson1", "attempt1")
        val key2 = FishTreatPolicy.rewardKey("child1", "lesson1", "attempt1")
        assertEquals(key1, key2)
    }

    @Test
    fun `reward keys differ by attempt`() {
        val key1 = FishTreatPolicy.rewardKey("child1", "lesson1", "attempt1")
        val key2 = FishTreatPolicy.rewardKey("child1", "lesson1", "attempt2")
        assertTrue(key1 != key2)
    }

    @Test
    fun `reward keys differ by child`() {
        val key1 = FishTreatPolicy.rewardKey("child1", "lesson1", "att1")
        val key2 = FishTreatPolicy.rewardKey("child2", "lesson1", "att1")
        assertTrue(key1 != key2)
    }

    @Test
    fun `stacked bonuses never exceed max`() {
        for (i in 0..10) {
            val amount = FishTreatPolicy.amount(true, true, true)
            assertTrue("Amount $amount exceeds max ${FishTreatPolicy.MAX_PER_ATTEMPT}",
                amount <= FishTreatPolicy.MAX_PER_ATTEMPT)
        }
    }
}
