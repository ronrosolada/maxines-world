package com.maxinesworld.gamekittenmatch

import kotlin.random.Random

/** Pure, deterministic memory-pairs logic. No Android or timing deps. */
class KittenMatchEngine(seed: Int, private val pairs: Int = 6) {
    private val random = Random(seed)

    fun initialState(): KittenMatchState = KittenMatchState(cards = deal())

    /** Turn a face-down card up. Returns unchanged state when the tap is a no-op. */
    fun flip(state: KittenMatchState, slot: Int): KittenMatchState {
        if (state.locked || state.boardCleared) return state
        val card = state.cards.getOrNull(slot) ?: return state
        if (card.faceUp || card.matched) return state

        val flipped = state.cards.map { if (it.slot == slot) it.copy(faceUp = true) else it }
        val first = state.firstPick
        if (first == null) {
            return state.copy(cards = flipped, firstPick = slot, feedback = "Now find its friend!")
        }
        val a = flipped.first { it.slot == first }
        val b = flipped.first { it.slot == slot }
        return if (a.face == b.face) {
            val matched = flipped.map { if (it.slot == first || it.slot == slot) it.copy(matched = true) else it }
            val pairs = state.matchedPairs + 1
            val cleared = matched.all { it.matched }
            state.copy(
                cards = matched, firstPick = null, moves = state.moves + 1,
                matchedPairs = pairs, boardCleared = cleared,
                feedback = if (cleared) "You found every friend!" else "${a.face.label} pair!"
            )
        } else {
            state.copy(cards = flipped, firstPick = slot, moves = state.moves + 1,
                locked = true, feedback = "Not a pair -- keep looking!")
        }
    }

    /** Call after a mismatch to flip the two mismatched cards back down. */
    fun resolveMismatch(state: KittenMatchState): KittenMatchState {
        if (!state.locked) return state
        val down = state.cards.map { if (!it.matched) it.copy(faceUp = false) else it }
        return state.copy(cards = down, firstPick = null, locked = false)
    }

    /** Deal a fresh board, carrying the running score forward. */
    fun nextRound(state: KittenMatchState): KittenMatchState {
        if (!state.boardCleared) return state
        return KittenMatchState(
            cards = deal(),
            roundsCompleted = state.roundsCompleted + 1,
            matchedPairs = state.matchedPairs,
            feedback = "New friends to match!"
        )
    }

    private fun deal(): List<MatchCard> {
        val faces = MatchFace.entries.shuffled(random).take(pairs)
        return (faces + faces).shuffled(random)
            .mapIndexed { i, f -> MatchCard(slot = i, face = f) }
    }
}
