package com.maxinesworld.gamefireflycatch

import org.junit.Assert.*
import org.junit.Test

class FireflyCatchEngineTest {
    @Test fun `catching a glowing sprite scores its points`() {
        val engine = FireflyCatchEngine(4)
        val state = engine.initialState()
        val catchable = state.sprites.first { it.kind != SpriteKind.BEE }
        val after = engine.tap(state, catchable.id)
        assertEquals(catchable.kind.points, after.score)
        assertEquals(1, after.catches)
        assertTrue(after.sprites.none { it.id == catchable.id })
    }

    @Test fun `tapping a bee never adds score or catches`() {
        val engine = FireflyCatchEngine(6)
        var state = engine.initialState()
        // ensure a bee exists in some wave
        var bee = state.sprites.firstOrNull { it.kind == SpriteKind.BEE }
        var guard = 0
        while (bee == null && guard++ < 50) { state = engine.spawnWave(state); bee = state.sprites.firstOrNull { it.kind == SpriteKind.BEE } }
        assertNotNull("expected a bee to spawn", bee)
        val after = engine.tap(state, bee!!.id)
        assertEquals(0, after.score)
        assertEquals(0, after.catches)
    }

    @Test fun `wave is cleared once only bees remain`() {
        val engine = FireflyCatchEngine(2)
        var state = engine.initialState()
        state.sprites.filter { it.kind != SpriteKind.BEE }.forEach { state = engine.tap(state, it.id) }
        assertTrue(engine.waveCleared(state))
    }
}
