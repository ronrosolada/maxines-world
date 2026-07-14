package com.maxinesworld.gamekittenmatch

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

data class KittenMatchUiState(
    val game: KittenMatchState,
    val remainingMillis: Long,
    val durationMillis: Long,
    val breakExpired: Boolean = false,
    val paused: Boolean = false
)

class KittenMatchViewModel(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long,
    private val wallTimeMillis: () -> Long = System::currentTimeMillis,
    monotonicMillis: () -> Long = SystemClock::elapsedRealtime
) : ViewModel() {
    private val startedAt = wallTimeMillis()
    private val engine = KittenMatchEngine(rewardBreakId.hashCode())
    private val clock = RewardBreakClock(durationMillis, monotonicMillis)
    private val _state = MutableStateFlow(
        KittenMatchUiState(engine.initialState(), durationMillis, durationMillis)
    )
    val state: StateFlow<KittenMatchUiState> = _state.asStateFlow()

    init {
        clock.resume()
        viewModelScope.launch {
            while (isActive) {
                delay(250)
                val remaining = clock.remainingMillis()
                _state.update { it.copy(remainingMillis = remaining, breakExpired = remaining == 0L) }
            }
        }
    }

    fun flip(slot: Int) {
        _state.update { it.copy(game = engine.flip(it.game, slot)) }
        if (_state.value.game.locked) viewModelScope.launch {
            delay(850)
            _state.update { it.copy(game = engine.resolveMismatch(it.game)) }
        }
    }

    fun nextRound() = _state.update {
        if (it.breakExpired) it else it.copy(game = engine.nextRound(it.game))
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
            correctOrders = g.matchedPairs,
            pawTokensEarned = MiniGameRewardPolicy.FISH_TREATS_EARNED,
            collectibleId = MiniGameRewardPolicy.COLLECTIBLE_ID
        )
    }

    companion object { const val GAME_ID = "kitten-match" }
}

class KittenMatchViewModelFactory(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        KittenMatchViewModel(childId, rewardBreakId, durationMillis) as T
}
