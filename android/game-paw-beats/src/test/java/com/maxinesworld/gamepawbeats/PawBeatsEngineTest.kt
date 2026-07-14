package com.maxinesworld.gamepawbeats

import org.junit.Assert.*
import org.junit.Test

class PawBeatsEngineTest {
    @Test fun `repeating the full sequence extends it and advances the round`() {
        val engine = PawBeatsEngine(9)
        var state = engine.beginInput(engine.initialState())
        val startLen = state.sequence.size
        val startRound = state.round
        state.sequence.toList().forEach { state = engine.tap(state, it) }
        assertEquals(startLen + 1, state.sequence.size)
        assertEquals(startRound + 1, state.round)
        assertEquals(1, state.roundsCompleted)
        assertFalse(state.awaitingInput)
    }

    @Test fun `a wrong tap restarts the same round without losing progress badges`() {
        val engine = PawBeatsEngine(9)
        var state = engine.beginInput(engine.initialState())
        val wrong = Pad.entries.first { it != state.sequence.first() }
        state = engine.tap(state, wrong)
        assertFalse(state.awaitingInput)
        assertEquals(0, state.playerIndex)
        assertEquals(0, state.roundsCompleted)
    }

    @Test fun `taps are ignored until it is the child's turn`() {
        val engine = PawBeatsEngine(3)
        val state = engine.initialState() // awaitingInput = false
        assertSame(state, engine.tap(state, Pad.CAT))
    }
}
