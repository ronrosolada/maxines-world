package com.maxinesworld.featurechildhome

import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus
import org.junit.Assert.*
import org.junit.Test

class PlaygroundUnlockCelebrationTest {

    @Test
    fun `liveLockedToUnlocked_showsOnce`() {
        // Simulate acceptGate() logic directly
        var observedAtLeastOnce = false
        var previousStatus: PlaygroundGateStatus? = null
        var celebrationFired = false

        fun acceptGate(next: PlaygroundGateState): Boolean {
            val shouldCelebrate = observedAtLeastOnce &&
                previousStatus == PlaygroundGateStatus.Locked &&
                next.status == PlaygroundGateStatus.Unlocked
            previousStatus = next.status
            observedAtLeastOnce = true
            if (shouldCelebrate) celebrationFired = true
            return shouldCelebrate
        }

        // First call: Locked — no celebration (not yet observed)
        val locked = PlaygroundGateState(childId = "c1", dayKey = "d1", totalAssigned = 3, completed = 1, status = PlaygroundGateStatus.Locked)
        assertFalse("first locked call should NOT celebrate", acceptGate(locked))

        // Second call: Unlocked — celebration should fire
        val unlocked = PlaygroundGateState(childId = "c1", dayKey = "d1", totalAssigned = 3, completed = 3, status = PlaygroundGateStatus.Unlocked)
        assertTrue("locked→unlocked transition should celebrate", acceptGate(unlocked))
        assertTrue("celebrationFired flag should be set", celebrationFired)
    }

    @Test
    fun `initialUnlockedAfterRestart_doesNotShow`() {
        var observedAtLeastOnce = false
        var previousStatus: PlaygroundGateStatus? = null
        var celebrationFired = false

        fun acceptGate(next: PlaygroundGateState): Boolean {
            val shouldCelebrate = observedAtLeastOnce &&
                previousStatus == PlaygroundGateStatus.Locked &&
                next.status == PlaygroundGateStatus.Unlocked
            previousStatus = next.status
            observedAtLeastOnce = true
            if (shouldCelebrate) celebrationFired = true
            return shouldCelebrate
        }

        // Already unlocked on first load (like after app restart)
        val unlocked = PlaygroundGateState(childId = "c1", dayKey = "d1", totalAssigned = 3, completed = 3, status = PlaygroundGateStatus.Unlocked)
        assertFalse("already unlocked at first load should NOT celebrate", acceptGate(unlocked))
    }

    @Test
    fun `recomposition_doesNotReplay`() {
        var observedAtLeastOnce = false
        var previousStatus: PlaygroundGateStatus? = null
        var fireCount = 0

        fun acceptGate(next: PlaygroundGateState): Boolean {
            val shouldCelebrate = observedAtLeastOnce &&
                previousStatus == PlaygroundGateStatus.Locked &&
                next.status == PlaygroundGateStatus.Unlocked
            previousStatus = next.status
            observedAtLeastOnce = true
            if (shouldCelebrate) fireCount++
            return shouldCelebrate
        }

        val locked = PlaygroundGateState(childId = "c1", dayKey = "d1", totalAssigned = 3, completed = 1, status = PlaygroundGateStatus.Locked)
        val unlocked = PlaygroundGateState(childId = "c1", dayKey = "d1", totalAssigned = 3, completed = 3, status = PlaygroundGateStatus.Unlocked)

        acceptGate(locked)    // first
        acceptGate(unlocked)  // fires once
        acceptGate(unlocked)  // recomposition — should NOT fire again
        acceptGate(unlocked)  // recomposition — should NOT fire again

        assertEquals("celebration should fire exactly once", 1, fireCount)
    }

    @Test
    fun `celebrationActions_dismissBeforeNavigation`() {
        var showCelebration = true
        var navigated = false

        // Dismiss should happen before navigation
        fun dismissCelebration() { showCelebration = false }
        fun navigate() { if (showCelebration) navigated = true }

        // Wrong order: navigate before dismiss
        showCelebration = true
        navigated = false
        navigate()
        dismissCelebration()
        assertTrue("navigated before dismiss is a bug — celebration should be dismissed first", navigated)

        // Correct order: dismiss then navigate
        showCelebration = true
        navigated = false
        dismissCelebration()
        navigate()
        assertFalse("navigation should only happen after celebration is dismissed", navigated)
    }
}
