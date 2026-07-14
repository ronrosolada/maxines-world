package com.maxinesworld.gamekittenmatch

/** Faces use bundled vector assets; assetName is stable metadata for tests/analytics only. */
enum class MatchFace(val label: String, val assetName: String) {
    MAXINE("Maxine", "ic_maxine"),
    MILO("Milo", "ic_milo"),
    TARSIER("Tarsier", "ic_tarsier"),
    EAGLE("Philippine Eagle", "ic_eagle"),
    TAMARAW("Tamaraw", "ic_tamaraw"),
    COLUGO("Colugo", "ic_colugo"),
    PEACOCK("Peacock-Pheasant", "ic_peacock"),
    WARTY_PIG("Warty Pig", "ic_warty_pig")
}

data class MatchCard(val slot: Int, val face: MatchFace, val faceUp: Boolean = false, val matched: Boolean = false)

data class KittenMatchState(
    val cards: List<MatchCard>,
    val firstPick: Int? = null,
    val moves: Int = 0,
    val matchedPairs: Int = 0,
    val roundsCompleted: Int = 0,
    val locked: Boolean = false,
    val feedback: String = "Find the matching friends!",
    val boardCleared: Boolean = false
)
