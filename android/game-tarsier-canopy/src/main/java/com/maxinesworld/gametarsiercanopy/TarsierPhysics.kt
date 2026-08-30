package com.maxinesworld.gametarsiercanopy

import kotlin.math.abs

/**
 * Tarsier Canopy physics — pure Kotlin, no Android dependencies.
 *
 * World conventions:
 *  - `x` runs along the course (0..course.length); the tarsier auto-runs.
 *  - `y` is ELEVATION above the forest floor: 0 = floor, larger = higher.
 *  - Gravity pulls `y` down; hop impulses push it up.
 *  - Vines must be cleared while airborne (elevation >= vine clearance);
 *    fireflies are collected when passing close to their height.
 */
class TarsierPhysics(private val courses: List<CanopyCourse> = CanopyCourses.all) {

    fun initial(seed: Int, assisted: Boolean = false, reducedMotion: Boolean = false): TarsierState =
        TarsierState(
            course = courses[Math.floorMod(seed, courses.size)],
            assistedMode = assisted,
            reducedMotion = reducedMotion,
        )

    fun start(s: TarsierState): TarsierState =
        if (s.phase == CanopyPhase.READY) {
            s.copy(phase = CanopyPhase.RUNNING, feedback = CanopyFeedback.START)
        } else {
            s
        }

    fun hop(s: TarsierState, kind: HopKind): TarsierState =
        if (s.phase == CanopyPhase.RUNNING && s.onGround && s.velocityY <= 0f) {
            s.copy(
                velocityY = kind.velocity,
                feedback = if (kind == HopKind.LONG) CanopyFeedback.BIG_HOP else CanopyFeedback.HOP,
            )
        } else {
            s
        }

    /**
     * Advances the simulation by [deltaSeconds]. Deterministic for a given input.
     */
    fun tick(s0: TarsierState, deltaSeconds: Float): TarsierState {
        if (s0.phase != CanopyPhase.RUNNING || deltaSeconds <= 0f) return s0
        val dt = deltaSeconds.coerceAtMost(MAX_DT)

        var s = s0
        val previousX = s.x

        // Assisted mode: auto-hop just before the next vine (safety net for young players).
        if (s.assistedMode && s.onGround) {
            val nextVine = s.course.vines.firstOrNull { it.id !in s.passedVineIds && it.x >= s.x }
            if (nextVine != null) {
                val distance = nextVine.x - s.x
                val kind = when {
                    distance <= 0.8f -> if (nextVine.kind == VineKind.HIGH) HopKind.LONG else HopKind.SHORT
                    distance <= 2.0f -> HopKind.LONG
                    else -> null
                }
                if (kind != null) s = hop(s, kind)
            }
        }

        val speed = if (s.reducedMotion) CALM_SPEED else RUN_SPEED
        val nx = (s.x + speed * dt).coerceAtMost(s.course.length)
        val vy = s.velocityY - GRAVITY * dt
        val ny = (s.y + s.velocityY * dt - 0.5f * GRAVITY * dt * dt).coerceAtLeast(GROUND_ELEVATION)

        var fireflies = s.fireflies
        var bumps = s.bumps
        var collected = s.collectedFireflyIds
        var passed = s.passedVineIds
        var feedback = s.feedback

        // Collect fireflies whose x we crossed, if close enough in elevation.
        s.course.fireflies
            .filter { it.id !in collected && it.x in previousX..nx }
            .forEach { f ->
                if (abs(ny - f.height) < FIREFLY_REACH) {
                    fireflies++
                    collected = collected + f.id
                    feedback = CanopyFeedback.FIREFLY
                }
            }

        // Vines: bump unless the tarsier is high enough (elevation >= clearance).
        s.course.vines
            .filter { it.id !in passed && it.x in previousX..(nx + 0.05f) }
            .forEach { v ->
                passed = passed + v.id
                if (ny < v.kind.clearance) {
                    bumps++
                    feedback = CanopyFeedback.BUMP
                } else {
                    feedback = CanopyFeedback.GREAT_JUMP
                }
            }

        val finished = nx >= s.course.length
        val grounded = ny <= GROUND_ELEVATION

        return s.copy(
            x = nx,
            y = ny,
            velocityY = if (grounded) 0f else vy,
            fireflies = fireflies,
            bumps = bumps,
            collectedFireflyIds = collected,
            passedVineIds = passed,
            phase = if (finished) CanopyPhase.ROUND_COMPLETE else CanopyPhase.RUNNING,
            roundsCompleted = if (finished) s.roundsCompleted + 1 else s.roundsCompleted,
            feedback = if (finished) CanopyFeedback.ROUND_COMPLETE else feedback,
        )
    }

    fun nextCourse(s: TarsierState): TarsierState {
        if (s.phase != CanopyPhase.ROUND_COMPLETE) return s
        val index = Math.floorMod(courses.indexOfFirst { it.id == s.course.id } + 1, courses.size)
        return TarsierState(
            course = courses[index],
            phase = CanopyPhase.READY,
            fireflies = s.fireflies,
            bumps = s.bumps,
            roundsCompleted = s.roundsCompleted,
            feedback = CanopyFeedback.NEXT_READY,
            assistedMode = s.assistedMode,
            reducedMotion = s.reducedMotion,
        )
    }

    fun setAssisted(s: TarsierState, on: Boolean): TarsierState =
        s.copy(
            assistedMode = on,
            feedback = if (on) CanopyFeedback.ASSISTED_ON else CanopyFeedback.ASSISTED_OFF,
        )

    fun setReducedMotion(s: TarsierState, on: Boolean): TarsierState =
        s.copy(
            reducedMotion = on,
            feedback = if (on) CanopyFeedback.CALM_ON else CanopyFeedback.CALM_OFF,
        )

    companion object {
        const val GRAVITY = 18.5f
        const val GROUND_ELEVATION = 0f
        const val RUN_SPEED = 4.0f
        const val CALM_SPEED = 3.0f
        const val MAX_DT = 0.05f
        const val FIREFLY_REACH = 0.26f
    }
}