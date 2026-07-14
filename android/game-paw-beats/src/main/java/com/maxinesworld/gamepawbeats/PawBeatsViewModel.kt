package com.maxinesworld.gamepawbeats

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

data class PawBeatsUiState(
    val game: PawBeatsState,
    val remainingMillis: Long,
    val durationMillis: Long,
    val breakExpired: Boolean = false,
    val paused: Boolean = false
)

class PawBeatsViewModel(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long,
    private val wallTimeMillis: () -> Long = System::currentTimeMillis,
    monotonicMillis: () -> Long = SystemClock::elapsedRealtime
) : ViewModel() {
    private val startedAt = wallTimeMillis()
    private val engine = PawBeatsEngine(rewardBreakId.hashCode())
    private val clock = RewardBreakClock(durationMillis, monotonicMillis)
    private val _state = MutableStateFlow(
        PawBeatsUiState(engine.initialState(), durationMillis, durationMillis)
    )
    val state: StateFlow<PawBeatsUiState> = _state.asStateFlow()

    /** Emits the pad the host should visibly flash during playback, or null. */
    private val _flash = MutableStateFlow<Pad?>(null)
    val flash: StateFlow<Pad?> = _flash.asStateFlow()

    init {
        clock.resume()
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                val remaining = clock.remainingMillis()
                _state.update { it.copy(remainingMillis = remaining, breakExpired = remaining == 0L) }
            }
        }
        viewModelScope.launch { playbackLoop() }
    }

    private suspend fun playbackLoop() {
        try {
        while (true) {
            delay(300)
            val s = _state.value
            if (s.paused || s.breakExpired || !engine.needsPlayback(s.game)) continue
            for (pad in s.game.sequence) {
                if (_state.value.paused || _state.value.breakExpired) break
                _flash.value = pad
                delay(520)
                _flash.value = null
                delay(220)
            }
            _state.update { it.copy(game = engine.beginInput(it.game)) }
        }
        } catch (_: kotlinx.coroutines.CancellationException) { }
    }

    fun tap(pad: Pad) = _state.update {
        if (it.paused || it.breakExpired) it else it.copy(game = engine.tap(it.game, pad))
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
            roundsCompleted = g.roundsCompleted,
            correctOrders = g.roundsCompleted,
            pawTokensEarned = MiniGameRewardPolicy.FISH_TREATS_EARNED,
            collectibleId = MiniGameRewardPolicy.COLLECTIBLE_ID
        )
    }

    companion object { const val GAME_ID = "paw-beats" }
}

class PawBeatsViewModelFactory(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PawBeatsViewModel(childId, rewardBreakId, durationMillis) as T
}
