package com.maxinesworld.gamefireflycatch

/** Glowing friends can be illuminated; a bee is a gentle leave-it-resting cue. */
enum class SpriteKind(val label: String, val assetName: String, val points: Int) {
    FIREFLY("Firefly", "ic_firefly", 1),
    BUTTERFLY("Butterfly", "ic_butterfly", 2),
    BEE("Bee", "ic_bee", 0)
}

data class Sprite(val id: Int, val kind: SpriteKind, val xPct: Float, val yPct: Float)
data class FireflyCatchState(
    val sprites: List<Sprite> = emptyList(),
    val score: Int = 0,
    val catches: Int = 0,
    val waves: Int = 0,
    val feedback: String = "Help the garden glow! Tap a firefly or butterfly."
)
