package com.maxinesworld.gamecatcafe

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

data class CatCafeUiState(
    val game: CatCafeState,
    val remainingMillis: Long,
    val durationMillis: Long,
    val breakExpired: Boolean = false,
    val paused: Boolean = false,
    val soundEnabled: Boolean = true,
    val reducedMotion: Boolean = false
)

class CatCafeViewModel(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long,
    private val wallTimeMillis: () -> Long = System::currentTimeMillis,
    monotonicMillis: () -> Long = SystemClock::elapsedRealtime
) : ViewModel() {
    private val startedAt = wallTimeMillis()
    private val engine = CatCafeEngine(rewardBreakId.hashCode())
    private val clock = RewardBreakClock(durationMillis, monotonicMillis)
    private val _state = MutableStateFlow(CatCafeUiState(engine.initialState(), durationMillis, durationMillis))
    val state: StateFlow<CatCafeUiState> = _state.asStateFlow()

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

    fun add(item: FoodItem) = _state.update { it.copy(game = engine.addToTray(it.game, item)) }
    fun remove(index: Int) = _state.update { it.copy(game = engine.removeFromTray(it.game, index)) }
    fun clear() = _state.update { it.copy(game = engine.clearTray(it.game)) }
    fun serve() = _state.update { it.copy(game = engine.serve(it.game)) }
    fun nextRound() = _state.update {
        if (it.breakExpired) it else it.copy(game = engine.nextRound(it.game))
    }
    fun toggleSound() = _state.update { it.copy(soundEnabled = !it.soundEnabled) }
    fun toggleReducedMotion() = _state.update { it.copy(reducedMotion = !it.reducedMotion) }
    fun pause() { clock.pause(); _state.update { it.copy(paused = true) } }
    fun resume() { clock.resume(); _state.update { it.copy(paused = false) } }

    fun result(): MiniGameResult {
        val game = state.value.game
        return MiniGameResult(
            rewardBreakId = rewardBreakId,
            gameId = GAME_ID,
            childId = childId,
            startedAtEpochMillis = startedAt,
            endedAtEpochMillis = wallTimeMillis(),
            roundsCompleted = game.roundsCompleted,
            correctOrders = game.correctOrders,
            pawTokensEarned = game.correctOrders.coerceAtMost(10),
            collectibleId = if (game.correctOrders >= 3) "cat-cafe-apron-teal" else null
        )
    }

    companion object { const val GAME_ID = "cat-cafe-dash" }
}

class CatCafeViewModelFactory(
    private val childId: String,
    private val rewardBreakId: String,
    private val durationMillis: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CatCafeViewModel(childId, rewardBreakId, durationMillis) as T
}
