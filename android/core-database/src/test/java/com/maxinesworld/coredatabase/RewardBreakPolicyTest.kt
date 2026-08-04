package com.maxinesworld.coredatabase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardBreakPolicyTest {

    @Test
    fun `daily quest completion key is stable for a child and day`() {
        assertEquals(
            "child-1:2026-08-04",
            RewardBreakPolicy.dailyQuestCompletionId("child-1", "2026-08-04")
        )
    }

    @Test
    fun `new entitlement starts created with the full duration`() {
        val entitlement = RewardBreakPolicy.newEntitlement(
            id = "break-1",
            childId = "child-1",
            dailyQuestCompletionId = "child-1:2026-08-04",
            nowEpochMillis = 1000L
        )

        assertEquals("CREATED", entitlement.state)
        assertEquals(RewardBreakPolicy.DEFAULT_DURATION_MILLIS, entitlement.remainingMillis)
        assertEquals(null, entitlement.startedAtEpochMillis)
        assertTrue(RewardBreakPolicy.canStart(entitlement))
    }

    @Test
    fun `active entitlement remaining time decreases from its start`() {
        val entitlement = RewardBreakEntitlementEntity(
            id = "break-1",
            childId = "child-1",
            dailyQuestCompletionId = "child-1:2026-08-04",
            durationMillis = 300_000L,
            remainingMillis = 300_000L,
            createdAtEpochMillis = 1000L,
            startedAtEpochMillis = 10_000L,
            state = "ACTIVE"
        )

        assertEquals(299_000L, RewardBreakPolicy.remainingAt(entitlement, 11_000L))
        assertTrue(RewardBreakPolicy.canUse(entitlement, 11_000L))
    }

    @Test
    fun `expired active entitlement cannot be used`() {
        val entitlement = RewardBreakEntitlementEntity(
            id = "break-1",
            childId = "child-1",
            dailyQuestCompletionId = "child-1:2026-08-04",
            durationMillis = 300_000L,
            remainingMillis = 300_000L,
            createdAtEpochMillis = 1000L,
            startedAtEpochMillis = 10_000L,
            state = "ACTIVE"
        )

        assertEquals(0L, RewardBreakPolicy.remainingAt(entitlement, 310_000L))
        assertFalse(RewardBreakPolicy.canUse(entitlement, 310_000L))
    }

    @Test
    fun `consumed entitlement cannot be started or used`() {
        val entitlement = RewardBreakEntitlementEntity(
            id = "break-1",
            childId = "child-1",
            dailyQuestCompletionId = "child-1:2026-08-04",
            durationMillis = 300_000L,
            remainingMillis = 0L,
            createdAtEpochMillis = 1000L,
            consumedAtEpochMillis = 20_000L,
            state = "CONSUMED"
        )

        assertFalse(RewardBreakPolicy.canStart(entitlement))
        assertFalse(RewardBreakPolicy.canUse(entitlement, 20_000L))
    }
}
