package com.maxinesworld.gamepawbeats

import kotlin.random.Random

/** Pure Simon-style sequence logic; playback timing is the host's job. */
class PawBeatsEngine(seed: Int) {
    private val random = Random(seed)
    private val pads = Pad.entries

    fun initialState(): PawBeatsState =
        PawBeatsState(sequence = listOf(pads[random.nextInt(pads.size)]), round = 1)

    /** Host calls this after it finishes flashing the sequence to the child. */
    fun beginInput(state: PawBeatsState): PawBeatsState =
        state.copy(awaitingInput = true, playerIndex = 0, feedback = "Your turn -- tap the tune!")

    /**
     * Handle a pad tap during the child's turn.
     * Correct full sequence -> extend by one and hand back for replay.
     * A wrong tap -> restart the same round (encouraging, never punishing).
     */
    fun tap(state: PawBeatsState, pad: Pad): PawBeatsState {
        if (!state.awaitingInput) return state
        val expected = state.sequence[state.playerIndex]
        if (pad != expected) {
            return state.copy(
                awaitingInput = false, playerIndex = 0,
                feedback = "Oops -- watch once more and try the same tune."
            )
        }
        val next = state.playerIndex + 1
        return if (next < state.sequence.size) {
            state.copy(playerIndex = next, feedback = "Keep going!")
        } else {
            state.copy(
                sequence = state.sequence + pads[random.nextInt(pads.size)],
                playerIndex = 0, awaitingInput = false,
                round = state.round + 1, roundsCompleted = state.roundsCompleted + 1,
                feedback = "You got it! Here comes a longer tune."
            )
        }
    }

    /** Whether the host still needs to replay the sequence before input. */
    fun needsPlayback(state: PawBeatsState): Boolean = !state.awaitingInput
}
