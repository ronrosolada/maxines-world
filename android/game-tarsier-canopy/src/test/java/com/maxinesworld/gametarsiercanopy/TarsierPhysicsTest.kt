package com.maxinesworld.gametarsiercanopy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TarsierPhysicsTest {
    private val e = TarsierPhysics()

    private fun sim(s0: TarsierState, seconds: Float, dt: Float = 0.02f): TarsierState {
        var s = s0
        var remaining = seconds
        while (remaining > 0f) {
            val step = dt.coerceAtMost(remaining)
            s = e.tick(s, step)
            remaining -= step
        }
        return s
    }

    @Test
    fun `same seed picks the same course`() {
        assertEquals(e.initial(0).course.id, e.initial(0).course.id)
        assertEquals("canopy-moonlight", e.initial(1).course.id)
    }

    @Test
    fun `first course is the dawn canopy`() {
        assertEquals("canopy-morning", e.initial(0).course.id)
    }

    @Test
    fun `hop only applies while running on the ground`() {
        val before = e.initial(0)
        val parked = e.hop(before, HopKind.SHORT)
        assertEquals(0f, parked.velocityY, 0.0001f)
        val s0 = e.start(before)
        val airborne = e.hop(e.hop(s0, HopKind.SHORT), HopKind.LONG) // second hop ignored? still grounded, so applies
        assertTrue(airborne.velocityY > 0f)
        assertEquals(CanopyPhase.RUNNING, airborne.phase)
    }

    @Test
    fun `vine bump counts once and never ends the round`() {
        var s = e.start(e.initial(0)).copy(x = 1.3f)
        s = sim(s, 0.25f)
        assertEquals(1, s.bumps)
        assertTrue(s.passedVineIds.contains("v1"))
        assertEquals(CanopyPhase.RUNNING, s.phase)
        // Passing the same vine again never double-counts.
        s = sim(s, 0.3f)
        assertEquals(1, s.bumps)
    }

    @Test
    fun `high vine needs a bigger hop to clear`() {
        var s = e.start(e.initial(0)).copy(x = 4.6f)
        s = e.hop(s, HopKind.LONG)
        s = sim(s, 0.8f)
        assertTrue(s.passedVineIds.contains("v2"))
        assertEquals(0, s.bumps) // cleared, no bump
    }

    @Test
    fun `low vine is cleared by a short hop`() {
        var s = e.start(e.initial(0)).copy(x = 1.0f)
        s = e.hop(s, HopKind.SHORT)
        s = sim(s, 0.8f)
        assertTrue(s.passedVineIds.contains("v1"))
        assertEquals(0, s.bumps)
    }

    @Test
    fun `firefly is collected once at hop height`() {
        var s = e.start(e.initial(0)).copy(x = 0.4f)
        s = e.hop(s, HopKind.SHORT)
        s = sim(s, 0.6f, dt = 0.02f)
        assertTrue(s.collectedFireflyIds.contains("f1"))
        assertEquals(1, s.fireflies)
        // Later frames do not re-collect.
        s = sim(s, 0.6f)
        assertEquals(1, s.fireflies)
    }

    @Test
    fun `assisted mode clears every vine without bumps`() {
        var s = e.start(e.initial(0, assisted = true))
        s = sim(s, 13f)
        assertEquals(CanopyPhase.ROUND_COMPLETE, s.phase)
        assertEquals(1, s.roundsCompleted)
        assertEquals(0, s.bumps)
        assertTrue(s.passedVineIds.containsAll(listOf("v1", "v2", "v3")))
    }

    @Test
    fun `course completes exactly once`() {
        var s = e.start(e.initial(0)).copy(x = 9.9f)
        s = sim(s, 0.2f)
        assertEquals(1, s.roundsCompleted)
        assertEquals(CanopyPhase.ROUND_COMPLETE, s.phase)
        val again = e.tick(s, 0.1f)
        assertEquals(1, again.roundsCompleted)
    }

    @Test
    fun `reduced motion runs slower`() {
        val normal = sim(e.start(e.initial(0)), 1f)
        val calm = sim(e.start(e.initial(0, reducedMotion = true)), 1f)
        assertTrue(calm.x < normal.x)
    }

    @Test
    fun `next course wraps around the catalog`() {
        val finished = e.start(e.initial(0)).copy(x = 99f)
        val afterTick = e.tick(finished, 0.05f)
        assertEquals(CanopyPhase.ROUND_COMPLETE, afterTick.phase)
        val next = e.nextCourse(afterTick)
        assertEquals(CanopyPhase.READY, next.phase)
        assertEquals("canopy-moonlight", next.course.id)
        val wrapped = e.nextCourse(e.tick(e.start(e.initial(2)).copy(x = 99f), 0.05f))
        assertEquals("canopy-morning", wrapped.course.id)
    }
}