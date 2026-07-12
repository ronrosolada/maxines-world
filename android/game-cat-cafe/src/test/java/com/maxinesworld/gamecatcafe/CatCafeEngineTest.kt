package com.maxinesworld.gamecatcafe

import org.junit.Assert.*
import org.junit.Test

class CatCafeEngineTest {
    @Test fun `correct tray completes exactly one round`() {
        val engine = CatCafeEngine(7)
        var state = engine.initialState()
        state.order.items.forEach { state = engine.addToTray(state, it) }
        state = engine.serve(state)
        assertTrue(state.roundFinished)
        assertEquals(1, state.correctOrders)
        val duplicate = engine.serve(state)
        assertEquals(1, duplicate.correctOrders)
    }

    @Test fun `wrong tray gives clue and no academic or game credit`() {
        val engine = CatCafeEngine(11)
        var state = engine.initialState()
        val wrong = FoodItem.entries.first { it !in state.order.items }
        state = engine.addToTray(state, wrong)
        state = engine.serve(state)
        assertFalse(state.roundFinished)
        assertEquals(0, state.correctOrders)
        assertTrue(state.showHint)
    }

    @Test fun `new round cannot start before current order is correct`() {
        val engine = CatCafeEngine(1)
        val state = engine.initialState()
        assertSame(state, engine.nextRound(state))
    }
}
