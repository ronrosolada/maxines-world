package com.maxinesworld.gametarsiercanopy

/**
 * Tarsier Canopy — pure Kotlin domain model.
 *
 * All positions are normalized: x advances along the course (0..course.length),
 * y is the tarsier's ELEVATION above the forest floor (0 = floor, up = larger).
 * Gravity pulls y downward. Branch platforms give the tarsier a place to land
 * on the way down.
 */
enum class HopKind(val velocity: Float) { SHORT(7.2f), LONG(9.3f) }

/** Hanging vine tangles. Clearance is the elevation the tarsier must be ABOVE
 *  (while airborne) to pass without a bump. */
enum class VineKind(val clearance: Float) { LOW(0.42f), HIGH(0.78f) }

data class CanopyVine(val id: String, val x: Float, val kind: VineKind)

data class CanopyFirefly(val id: String, val x: Float, val height: Float = 0.8f)

data class CanopyCourse(
    val id: String,
    val title: String,
    val length: Float,
    val vines: List<CanopyVine>,
    val fireflies: List<CanopyFirefly>,
)

enum class CanopyPhase { READY, RUNNING, ROUND_COMPLETE }

/** Stable feedback codes — the UI resolves these to localized strings. */
enum class CanopyFeedback { WELCOME, START, HOP, BIG_HOP, FIREFLY, BUMP, GREAT_JUMP, ROUND_COMPLETE, NEXT_READY, ASSISTED_ON, ASSISTED_OFF, CALM_ON, CALM_OFF }

data class TarsierState(
    val course: CanopyCourse,
    val phase: CanopyPhase = CanopyPhase.READY,
    val x: Float = 0f,
    val y: Float = 0f,
    val velocityY: Float = 0f,
    val fireflies: Int = 0,
    val bumps: Int = 0,
    val roundsCompleted: Int = 0,
    val collectedFireflyIds: Set<String> = emptySet(),
    val passedVineIds: Set<String> = emptySet(),
    val feedback: CanopyFeedback = CanopyFeedback.WELCOME,
    val assistedMode: Boolean = false,
    val reducedMotion: Boolean = false,
) {
    val onGround: Boolean get() = y <= TarsierPhysics.GROUND_ELEVATION + 0.001f
    val progress: Float get() = (x / course.length).coerceIn(0f, 1f)
}

data class TarsierResult(
    val rewardBreakId: String,
    val childId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val roundsCompleted: Int,
    val firefliesCollected: Int,
    val bumps: Int,
    val pawTokensEarned: Int,
    val collectibleId: String?,
) {
    val idempotencyKey: String get() = "$rewardBreakId:tarsier-canopy"
}

const val TARSIER_CANOPY_GAME_ID = "tarsier-canopy"
const val TARSIER_CANOPY_COLLECTIBLE_ID = "tarsier-canopy-badge"