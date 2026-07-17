package com.maxinesworld.gamefireflycatch

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maxinesworld.engineminigame.MiniGameResult
import com.maxinesworld.engineminigame.RewardBreakClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FireflyCatchUiState(
    val game: FireflyCatchState,
    val remainingMillis: Long,
    val durationMillis: Long,
    val breakExpired: Boolean = false,
    val paused: Boolean = false
)

class FireflyCatchViewModel(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long,
    private val wallTimeMillis: () -> Long = System::currentTimeMillis,
    monotonicMillis: () -> Long = SystemClock::elapsedRealtime
) : ViewModel() {
    private val startedAt = wallTimeMillis()
    private val engine = FireflyCatchEngine(rewardBreakId.hashCode())
    private val clock = RewardBreakClock(durationMillis, monotonicMillis)
    private val _state = MutableStateFlow(
        FireflyCatchUiState(engine.initialState(), durationMillis, durationMillis)
    )
    val state: StateFlow<FireflyCatchUiState> = _state.asStateFlow()

    init {
        clock.resume()
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                val remaining = clock.remainingMillis()
                _state.update { it.copy(remainingMillis = remaining, breakExpired = remaining == 0L) }
            }
        }
        // Auto-refresh the field so a wave never stays empty.
        viewModelScope.launch {
            while (isActive) {
                delay(2200)
                _state.update {
                    if (it.paused || it.breakExpired) it
                    else if (engine.waveCleared(it.game)) it.copy(game = engine.spawnWave(it.game)) else it
                }
            }
        }
    }

    fun tap(id: Int) = _state.update {
        if (it.paused || it.breakExpired) it else it.copy(game = engine.tap(it.game, id))
    }
    fun pause() { clock.pause(); _state.update { it.copy(paused = true) } }
    fun resume() { clock.resume(); _state.update { it.copy(paused = false) } }

    fun result(): MiniGameResult {
        val g = state.value.game
        return MiniGameResult(
            rewardBreakId = rewardBreakId,
            gameId = GAME_ID,
            childId = childId,
            startedAtEpochMillis = startedAt,
            endedAtEpochMillis = wallTimeMillis(),
            roundsCompleted = g.waves,
            correctOrders = g.catches,
            pawTokensEarned = MiniGameRewardPolicy.FISH_TREATS_EARNED,
            collectibleId = MiniGameRewardPolicy.COLLECTIBLE_ID
        )
    }

    companion object { const val GAME_ID = "firefly-catch" }
}

class FireflyCatchViewModelFactory(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FireflyCatchViewModel(childId, rewardBreakId, durationMillis) as T
}
