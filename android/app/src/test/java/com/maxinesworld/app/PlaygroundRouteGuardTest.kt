package com.maxinesworld.app

import com.maxinesworld.playground.PlaygroundGateEvaluator
import com.maxinesworld.playground.PlaygroundGateStatus
import org.junit.Assert.*
import org.junit.Test

class PlaygroundRouteGuardTest {

    // ─── Helpers: simulate what the guard checks ───

    private fun isBlocked(
        assignedQuestIds: Set<String>?,
        completedQuestIds: Set<String>?,
        hasUnlockReceipt: Boolean,
    ): Boolean {
        val gate = PlaygroundGateEvaluator.evaluate(
            childId = "test", dayKey = "2026-07-14",
            assignedQuestIds = assignedQuestIds,
            completedQuestIds = completedQuestIds,
            hasUnlockReceipt = hasUnlockReceipt,
        )
        return gate.status != PlaygroundGateStatus.Unlocked
    }

    @Test
    fun `lockedLibraryRoute_isBlocked`() {
        assertTrue(isBlocked(
            assignedQuestIds = setOf("subject:english", "subject:filipino", "subject:mathematics"),
            completedQuestIds = setOf("subject:english"), // 1 of 3
            hasUnlockReceipt = false,
        ))
    }

    @Test
    fun `lockedKittenMatchRoute_isBlocked`() {
        assertTrue(isBlocked(
            assignedQuestIds = setOf("subject:english", "subject:filipino", "subject:mathematics"),
            completedQuestIds = emptySet(), // 0 of 3
            hasUnlockReceipt = false,
        ))
    }

    @Test
    fun `lockedFireflyGardenRoute_isBlocked`() {
        assertTrue(isBlocked(
            assignedQuestIds = setOf("subject:english"),
            completedQuestIds = setOf("subject:english"), // 1 of 1 but not 3-of-5
            hasUnlockReceipt = false,
        ))
    }

    @Test
    fun `lockedPawBeatsRoute_isBlocked`() {
        assertTrue(isBlocked(
            assignedQuestIds = setOf("subject:english", "subject:filipino", "subject:mathematics"),
            completedQuestIds = setOf("subject:english", "subject:mathematics"), // 2 of 3
            hasUnlockReceipt = false,
        ))
    }

    @Test
    fun `unlockedRoutes_openOffline`() {
        assertFalse(isBlocked(
            assignedQuestIds = setOf("subject:english", "subject:filipino", "subject:mathematics"),
            completedQuestIds = setOf("subject:english", "subject:filipino", "subject:mathematics"),
            hasUnlockReceipt = true,
        ))
    }

    @Test
    fun `malformedSnapshot_failsClosed`() {
        // Null assignedQuestIds means no quest set was persisted — should fail closed
        assertTrue(isBlocked(
            assignedQuestIds = null,
            completedQuestIds = null,
            hasUnlockReceipt = false,
        ))
    }

    @Test
    fun `hasReceipt_upgradesToAlreadyPlayed`() {
        val gate = PlaygroundGateEvaluator.evaluate(
            childId = "test", dayKey = "2026-07-14",
            assignedQuestIds = setOf("subject:english", "subject:filipino", "subject:mathematics"),
            completedQuestIds = setOf("subject:english", "subject:filipino", "subject:mathematics"),
            hasUnlockReceipt = true,
        )
        assertEquals(PlaygroundGateStatus.Unlocked, gate.status)
    }
}
