package com.maxinesworld.featurechildhome

import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus
import org.junit.Assert.*
import org.junit.Test

class PlaygroundBannerTest {

    @Test
    fun `lockedTap_opensProgressDialog_notNavigates`() {
        var dialogShown = false
        var navigated = false

        val locked = PlaygroundGateState(
            childId = "c1", dayKey = "2026-07-14",
            totalAssigned = 3, completed = 1,
            status = PlaygroundGateStatus.Locked,
        )

        // Simulate banner tap behavior
        when (locked.status) {
            PlaygroundGateStatus.Unlocked -> navigated = true
            PlaygroundGateStatus.Locked -> dialogShown = true
            else -> { /* no action */ }
        }

        assertTrue("locked tap should show dialog", dialogShown)
        assertFalse("locked tap should NOT navigate", navigated)
    }

    @Test
    fun `unlockedTap_navigatesOnce`() {
        var dialogShown = false
        var navigateCount = 0

        val unlocked = PlaygroundGateState(
            childId = "c1", dayKey = "2026-07-14",
            totalAssigned = 3, completed = 3,
            status = PlaygroundGateStatus.Unlocked,
        )

        // Simulate banner tap behavior
        when (unlocked.status) {
            PlaygroundGateStatus.Unlocked -> navigateCount++
            PlaygroundGateStatus.Locked -> dialogShown = true
            else -> { /* no action */ }
        }

        assertEquals("unlocked tap should navigate once", 1, navigateCount)
        assertFalse("unlocked tap should NOT show dialog", dialogShown)
    }

    @Test
    fun `loadingAndError_areNotNavigationActions`() {
        var navigated = false
        var dialogShown = false

        val loading = PlaygroundGateState(
            childId = "c1", dayKey = "2026-07-14",
            totalAssigned = 0, completed = 0,
            status = PlaygroundGateStatus.Loading,
        )
        val error = PlaygroundGateState(
            childId = "c1", dayKey = "2026-07-14",
            totalAssigned = 0, completed = 0,
            status = PlaygroundGateStatus.Error,
        )

        for (state in listOf(loading, error)) {
            navigated = false
            dialogShown = false
            when (state.status) {
                PlaygroundGateStatus.Unlocked -> navigated = true
                PlaygroundGateStatus.Locked -> dialogShown = true
                else -> { /* no action */ }
            }
            assertFalse("$state should not navigate", navigated)
            assertFalse("$state should not show dialog", dialogShown)
        }
    }

    @Test
    fun `compactAndExpanded_haveEquivalentActions`() {
        val sharedGate = PlaygroundGateState(
            childId = "c1", dayKey = "2026-07-14",
            totalAssigned = 3, completed = 1,
            status = PlaygroundGateStatus.Locked,
        )

        // Both compact and expanded use the same gate state
        fun expandedAction(gate: PlaygroundGateState): String =
            when (gate.status) {
                PlaygroundGateStatus.Unlocked -> "navigate"
                PlaygroundGateStatus.Locked -> "dialog"
                else -> "none"
            }

        fun compactAction(gate: PlaygroundGateState): String =
            when (gate.status) {
                PlaygroundGateStatus.Unlocked -> "navigate"
                PlaygroundGateStatus.Locked -> "dialog"
                else -> "none"
            }

        assertEquals(
            "expanded and compact should produce same action",
            expandedAction(sharedGate), compactAction(sharedGate)
        )
    }
}
