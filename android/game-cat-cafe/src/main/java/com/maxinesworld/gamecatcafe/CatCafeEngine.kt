package com.maxinesworld.gamecatcafe

import kotlin.random.Random

class CatCafeEngine(seed: Int) {
    private val random = Random(seed)
    private val foods = FoodItem.entries
    private val customers = Customer.entries

    fun initialState(): CatCafeState = CatCafeState(order = nextOrder(0))

    fun addToTray(state: CatCafeState, item: FoodItem): CatCafeState {
        if (state.roundFinished || state.tray.size >= 2) return state
        return state.copy(tray = state.tray + item, feedback = "Great choice. Check the order card.")
    }

    fun removeFromTray(state: CatCafeState, index: Int): CatCafeState {
        if (index !in state.tray.indices || state.roundFinished) return state
        return state.copy(tray = state.tray.filterIndexed { i, _ -> i != index })
    }

    fun clearTray(state: CatCafeState): CatCafeState =
        if (state.roundFinished) state else state.copy(tray = emptyList(), feedback = "Tray cleared. Try again!")

    fun serve(state: CatCafeState): CatCafeState {
        if (state.roundFinished) return state
        val correct = state.tray.sortedBy { it.name } == state.order.items.sortedBy { it.name }
        return if (correct) {
            state.copy(
                roundsCompleted = state.roundsCompleted + 1,
                correctOrders = state.correctOrders + 1,
                feedback = "Perfect! ${state.order.customer.label} loves it!",
                roundFinished = true,
                showHint = false
            )
        } else {
            val missing = state.order.items.filterNot { it in state.tray }.firstOrNull()?.label
            state.copy(
                feedback = if (missing != null) "Almost! Look for the $missing." else "Almost! One tray item is different.",
                showHint = true
            )
        }
    }

    fun nextRound(state: CatCafeState): CatCafeState {
        if (!state.roundFinished) return state
        return CatCafeState(
            order = nextOrder(state.roundsCompleted),
            roundsCompleted = state.roundsCompleted,
            correctOrders = state.correctOrders,
            feedback = "A new customer is ready!"
        )
    }

    private fun nextOrder(round: Int): CafeOrder {
        val count = if (round < 2) 1 else 2
        return CafeOrder(
            customer = customers[random.nextInt(customers.size)],
            items = foods.shuffled(random).take(count)
        )
    }
}
