package com.maxinesworld.engineminigame

/** Pure monotonic clock; it pauses when the host is not visible. */
class RewardBreakClock(
    private val durationMillis: Long = DEFAULT_DURATION_MILLIS,
    private val nowMillis: () -> Long
) {
    private var accumulatedMillis = 0L
    private var resumedAt: Long? = null

    init { require(durationMillis > 0) }

    fun resume() {
        if (resumedAt == null && !isExpired()) resumedAt = nowMillis()
    }

    fun pause() {
        resumedAt?.let { accumulatedMillis += (nowMillis() - it).coerceAtLeast(0L) }
        resumedAt = null
    }

    fun elapsedMillis(): Long = (
        accumulatedMillis + (resumedAt?.let { (nowMillis() - it).coerceAtLeast(0L) } ?: 0L)
    ).coerceAtMost(durationMillis)

    fun remainingMillis(): Long = (durationMillis - elapsedMillis()).coerceAtLeast(0L)
    fun isExpired(): Boolean = remainingMillis() == 0L

    companion object { const val DEFAULT_DURATION_MILLIS = 5 * 60 * 1000L }
}
