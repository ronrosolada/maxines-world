package com.maxinesworld.gamekittenmatch

import org.junit.Assert.*
import org.junit.Test

class KittenMatchEngineTest {
    @Test fun `flipping two of the same face makes a matched pair`() {
        val engine = KittenMatchEngine(3)
        var state = engine.initialState()
        val first = state.cards.first()
        state = engine.flip(state, first.slot)
        val twin = state.cards.first { it.face == first.face && it.slot != first.slot }
        state = engine.flip(state, twin.slot)
        assertEquals(1, state.matchedPairs)
        assertTrue(state.cards.filter { it.face == first.face }.all { it.matched })
        assertFalse(state.locked)
    }

    @Test fun `mismatch locks the board then resolves face-down`() {
        val engine = KittenMatchEngine(5)
        var state = engine.initialState()
        val a = state.cards.first()
        val b = state.cards.first { it.face != a.face }
        state = engine.flip(state, a.slot)
        state = engine.flip(state, b.slot)
        assertTrue(state.locked)
        assertEquals(0, state.matchedPairs)
        state = engine.resolveMismatch(state)
        assertFalse(state.locked)
        assertTrue(state.cards.none { it.faceUp })
    }

    @Test fun `next board is refused until current board is cleared`() {
        val engine = KittenMatchEngine(1)
        val state = engine.initialState()
        assertSame(state, engine.nextRound(state))
    }
}
