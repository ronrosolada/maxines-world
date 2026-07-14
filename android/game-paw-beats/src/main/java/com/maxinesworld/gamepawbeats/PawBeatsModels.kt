package com.maxinesworld.gamepawbeats

import androidx.compose.ui.graphics.Color

/** Four pads use bundled vectors and visible labels, so sound is optional. */
enum class Pad(val index: Int, val label: String, val assetName: String, val color: Color) {
    CAT(0, "Cat", "ic_cat_pad", Color(0xFF087F83)),
    FROG(1, "Frog", "ic_frog_pad", Color(0xFFF5B82E)),
    BIRD(2, "Bird", "ic_bird_pad", Color(0xFFF47C6B)),
    OWL(3, "Owl", "ic_owl_pad", Color(0xFF6C5CE7))
}

data class PawBeatsState(
    val sequence: List<Pad> = emptyList(),
    val playerIndex: Int = 0,
    val round: Int = 0,
    val roundsCompleted: Int = 0,
    val awaitingInput: Boolean = false,
    val flashing: Pad? = null,
    val feedback: String = "Watch Maxine, then tap the same tune!"
)
