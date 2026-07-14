package com.maxinesworld.gamefireflycatch

import kotlin.random.Random

/** Pure, deterministic spawn/scoring logic. Rendering + timing live in the host. */
class FireflyCatchEngine(seed: Int) {
    private val random = Random(seed)
    private var nextId = 0

    fun initialState(): FireflyCatchState = spawnWave(FireflyCatchState())

    /** Replace the field with a fresh wave of 3-5 sprites (a couple may be bees). */
    fun spawnWave(state: FireflyCatchState): FireflyCatchState {
        val count = 3 + random.nextInt(3)
        val sprites = (0 until count).map {
            val roll = random.nextInt(10)
            val kind = when {
                roll < 5 -> SpriteKind.FIREFLY
                roll < 8 -> SpriteKind.BUTTERFLY
                else -> SpriteKind.BEE
            }
            Sprite(nextId++, kind, random.nextFloat() * 0.86f + 0.02f, random.nextFloat() * 0.70f + 0.05f)
        }
        return state.copy(sprites = sprites, waves = state.waves + 1)
    }

    /** Tap a sprite. Glowing friends score; tapping a bee leaves it resting with a kind reminder. */
    fun tap(state: FireflyCatchState, id: Int): FireflyCatchState {
        val hit = state.sprites.firstOrNull { it.id == id } ?: return state
        val remaining = state.sprites.filterNot { it.id == id }
        return if (hit.kind == SpriteKind.BEE) {
            state.copy(sprites = remaining, feedback = "Let the bee buzz by -- let it rest; light the glowing friends!")
        } else {
            state.copy(
                sprites = remaining,
                score = state.score + hit.kind.points,
                catches = state.catches + 1,
                feedback = "+${hit.kind.points}! Lovely glow!"
            )
        }
    }

    /** True when only bees (or nothing) are left, so the host can spawn the next wave. */
    fun waveCleared(state: FireflyCatchState): Boolean =
        state.sprites.none { it.kind != SpriteKind.BEE }
}
