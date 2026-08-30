package com.maxinesworld.gametarsiercanopy

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maxinesworld.engineminigame.RewardBreakClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TarsierUiState(
    val game: TarsierState,
    val remainingMillis: Long,
    val durationMillis: Long,
    val breakExpired: Boolean = false,
    val paused: Boolean = false,
    val soundEnabled: Boolean = true,
)

class TarsierCanopyViewModel(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long,
    private val wallTime: () -> Long = System::currentTimeMillis,
    monotonic: () -> Long = SystemClock::elapsedRealtime,
    private val runLoop: Boolean = true,
) : ViewModel() {

    private val startedAt = wallTime()
    private val engine = TarsierPhysics()
    private val clock = RewardBreakClock(durationMillis, monotonic)

    private val _state = MutableStateFlow(
        TarsierUiState(engine.initial(rewardBreakId.hashCode()), durationMillis, durationMillis)
    )
    val state = _state.asStateFlow()

    private var loopJob: kotlinx.coroutines.Job? = null

    init {
        clock.resume()
        if (runLoop) {
            loopJob = viewModelScope.launch {
                var previous = monotonic()
                while (isActive) {
                    delay(16)
                    val now = monotonic()
                    val dt = (now - previous) / 1000f
                    previous = now
                    _state.update { u ->
                        val remaining = clock.remainingMillis()
                        val active = !u.paused && u.game.phase == CanopyPhase.RUNNING
                        u.copy(
                            game = if (active) engine.tick(u.game, dt) else u.game,
                            remainingMillis = remaining,
                            breakExpired = remaining == 0L,
                        )
                    }
                }
            }
        }
    }

    /**
     * Advances the simulation by one frame. The production game loop calls this
     * every ~16ms; tests drive it directly for deterministic stepping.
     */
    internal fun tickFrame(deltaSeconds: Float) {
        val remaining = clock.remainingMillis()
        val active = !state.value.paused && state.value.game.phase == CanopyPhase.RUNNING
        _state.update { u ->
            u.copy(
                game = if (active) engine.tick(u.game, deltaSeconds) else u.game,
                remainingMillis = remaining,
                breakExpired = remaining == 0L,
            )
        }
    }

    /** Stops the game loop — used by tests; harmless in production (VM teardown cancels anyway). */
    internal fun shutdown() {
        loopJob?.cancel()
    }

    fun start() = _state.update { it.copy(game = engine.start(it.game)) }
    fun shortHop() = _state.update { it.copy(game = engine.hop(it.game, HopKind.SHORT)) }
    fun longHop() = _state.update { it.copy(game = engine.hop(it.game, HopKind.LONG)) }
    fun nextCourse() = _state.update { if (it.breakExpired) it else it.copy(game = engine.nextCourse(it.game)) }
    fun toggleAssist() = _state.update { it.copy(game = engine.setAssisted(it.game, !it.game.assistedMode)) }
    fun toggleReducedMotion() = _state.update { it.copy(game = engine.setReducedMotion(it.game, !it.game.reducedMotion)) }
    fun toggleSound() = _state.update { it.copy(soundEnabled = !it.soundEnabled) }
    fun pause() {
        clock.pause()
        _state.update { it.copy(paused = true) }
    }
    fun resume() {
        clock.resume()
        _state.update { it.copy(paused = false) }
    }

    fun result(): TarsierResult {
        val g = state.value.game
        return TarsierResult(
            rewardBreakId = rewardBreakId,
            childId = childId,
            startedAtEpochMillis = startedAt,
            endedAtEpochMillis = wallTime(),
            roundsCompleted = g.roundsCompleted,
            firefliesCollected = g.fireflies,
            bumps = g.bumps,
            pawTokensEarned = (g.fireflies / 3 + g.roundsCompleted).coerceAtMost(10),
            collectibleId = if (g.roundsCompleted >= 2) TARSIER_CANOPY_COLLECTIBLE_ID else null,
        )
    }
}

class TarsierCanopyViewModelFactory(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TarsierCanopyViewModel(childId, rewardBreakId, durationMillis) as T
}