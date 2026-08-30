package com.maxinesworld.gametarsiercanopy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic ViewModel tests: the VM is constructed with runLoop=false so no
 * infinite 16ms coroutine fights a test scheduler; the simulation is stepped
 * explicitly with [TarsierCanopyViewModel.tickFrame].
 */
class TarsierCanopyViewModelTest {

    private var virtualMillis = 0L

    private fun newViewModel(
        durationMillis: Long,
        childId: String = "child-1",
        breakId: String = "reward-break-1",
    ): TarsierCanopyViewModel {
        virtualMillis = 0L
        return TarsierCanopyViewModel(
            childId = childId,
            rewardBreakId = breakId,
            durationMillis = durationMillis,
            wallTime = { virtualMillis },
            monotonic = { virtualMillis },
            runLoop = false,
        )
    }

    private fun simulateSeconds(vm: TarsierCanopyViewModel, seconds: Float) {
        var remaining = seconds
        while (remaining > 0f) {
            val dt = if (remaining > 0.05f) 0.05f else remaining
            vm.tickFrame(dt)
            remaining -= dt
        }
    }

    @Test
    fun `initially ready with deterministic course and full break`() {
        val vm = newViewModel(300_000L)
        val s = vm.state.value
        assertEquals(CanopyPhase.READY, s.game.phase)
        assertTrue(s.game.course.id.isNotBlank())
        assertEquals(300_000L, s.remainingMillis)
        assertFalse(s.breakExpired)
        assertFalse(s.paused)
        assertTrue(s.soundEnabled)

        // Same rewardBreakId → same seeded course.
        val vm2 = newViewModel(300_000L, breakId = "reward-break-1")
        assertEquals(s.game.course.id, vm2.state.value.game.course.id)
    }

    @Test
    fun `start moves to running and frames advance the game`() {
        val vm = newViewModel(300_000L)
        vm.start()
        assertEquals(CanopyPhase.RUNNING, vm.state.value.game.phase)
        simulateSeconds(vm, 0.5f)
        val x = vm.state.value.game.x
        assertTrue("tarsier should have advanced, was $x", x > 0f)
        assertTrue(vm.state.value.game.progress > 0f)
    }

    @Test
    fun `hop only takes effect while running on the ground`() {
        val vm = newViewModel(300_000L)
        assertEquals(0f, vm.state.value.game.velocityY, 0.0001f)

        // READY phase: hop is ignored.
        vm.shortHop()
        assertEquals(0f, vm.state.value.game.velocityY, 0.0001f)

        vm.start()
        vm.shortHop()
        assertTrue(vm.state.value.game.velocityY > 0f)

        // Airborne: a second hop is ignored.
        val airVelocity = vm.state.value.game.velocityY
        vm.longHop()
        assertEquals(airVelocity, vm.state.value.game.velocityY, 0.0001f)
    }

    @Test
    fun `pause freezes the world and resume continues it`() {
        val vm = newViewModel(300_000L)
        vm.start()
        simulateSeconds(vm, 0.3f)
        val before = vm.state.value.game.x
        vm.pause()
        simulateSeconds(vm, 1f)
        assertEquals(before, vm.state.value.game.x, 0.0001f)
        assertTrue(vm.state.value.paused)
        vm.resume()
        simulateSeconds(vm, 0.3f)
        assertTrue(vm.state.value.game.x > before)
        assertFalse(vm.state.value.paused)
    }

    @Test
    fun `break expiry surfaces after the duration elapses`() {
        val vm = newViewModel(300_000L)
        vm.start()
        simulateSeconds(vm, 3f)
        val early = vm.state.value
        assertFalse(early.breakExpired)
        assertTrue(early.remainingMillis > 0L)

        virtualMillis = 300_000L
        vm.tickFrame(0.016f)
        val expired = vm.state.value
        assertTrue(expired.breakExpired)
        assertEquals(0L, expired.remainingMillis)
    }

    @Test
    fun `assist completes a round and two rounds award the collectible`() {
        val vm = newViewModel(300_000L)
        vm.start()
        vm.toggleAssist()
        assertTrue(vm.state.value.game.assistedMode)

        simulateSeconds(vm, 4f)
        val roundOne = vm.state.value.game
        assertTrue(roundOne.roundsCompleted >= 1)
        assertTrue(roundOne.phase == CanopyPhase.ROUND_COMPLETE)
        assertTrue(roundOne.fireflies >= 0)

        vm.nextCourse()
        assertEquals(CanopyPhase.READY, vm.state.value.game.phase)
        vm.start()
        simulateSeconds(vm, 4f)
        assertTrue(vm.state.value.game.roundsCompleted >= 2)

        val result = vm.result()
        assertEquals("reward-break-1", result.rewardBreakId)
        assertEquals("child-1", result.childId)
        assertTrue(result.endedAtEpochMillis >= result.startedAtEpochMillis)
        assertEquals(TARSIER_CANOPY_COLLECTIBLE_ID, result.collectibleId)
        assertEquals("reward-break-1:$TARSIER_CANOPY_GAME_ID", result.idempotencyKey)
        assertTrue(result.pawTokensEarned in 0..10)
    }

    @Test
    fun `no collectible before two rounds and toggles update state`() {
        val vm = newViewModel(300_000L)
        vm.start()
        simulateSeconds(vm, 0.3f)
        assertNull(vm.result().collectibleId)

        vm.toggleSound()
        assertFalse(vm.state.value.soundEnabled)
        vm.toggleReducedMotion()
        assertTrue(vm.state.value.game.reducedMotion)
        vm.toggleAssist()
        assertTrue(vm.state.value.game.assistedMode)
    }
}