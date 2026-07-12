package com.maxinesworld.gamepawprintparkour

enum class JumpKind(val velocity: Float) { SHORT(7.2f), LONG(9.3f) }
enum class ObstacleKind(val clearance: Float) { PUDDLE(.38f), HAY(.72f), LOG(.88f) }
data class CourseObstacle(val id: String, val x: Float, val kind: ObstacleKind)
data class CourseToken(val id: String, val x: Float, val height: Float = .7f)
data class ParkourCourse(
    val id: String, val title: String, val length: Float,
    val obstacles: List<CourseObstacle>, val tokens: List<CourseToken>
)
enum class ParkourPhase { READY, RUNNING, ROUND_COMPLETE }
data class ParkourState(
    val course: ParkourCourse,
    val phase: ParkourPhase = ParkourPhase.READY,
    val x: Float = 0f, val y: Float = 0f, val velocityY: Float = 0f,
    val tokens: Int = 0, val bumps: Int = 0, val roundsCompleted: Int = 0,
    val collectedTokenIds: Set<String> = emptySet(), val passedObstacleIds: Set<String> = emptySet(),
    val feedback: String = "Tap Jump when Milo reaches an obstacle.",
    val assistedMode: Boolean = false, val reducedMotion: Boolean = false
) { val onGround get() = y <= 0.001f; val progress get() = (x / course.length).coerceIn(0f,1f) }

data class ParkourResult(
    val rewardBreakId: String, val childId: String, val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long, val roundsCompleted: Int, val tokensCollected: Int,
    val bumps: Int, val pawTokensEarned: Int, val collectibleId: String?
) { val idempotencyKey get() = "$rewardBreakId:pawprint-parkour" }
