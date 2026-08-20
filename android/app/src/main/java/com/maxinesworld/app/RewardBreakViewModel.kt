package com.maxinesworld.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.InventoryEntity
import com.maxinesworld.coredatabase.GodModeManager
import com.maxinesworld.coredatabase.MiniGameResultDao
import com.maxinesworld.coredatabase.MiniGameResultEntity
import com.maxinesworld.coredatabase.PlaygroundUnlockReceiptDao
import com.maxinesworld.coredatabase.RewardBreakDao
import com.maxinesworld.coredatabase.RewardBreakEntitlementEntity
import com.maxinesworld.coredatabase.RewardBreakPolicy
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coredatabase.RoomTransactionRunner
import com.maxinesworld.engineminigame.MiniGameResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface RewardBreakUiState {
    object Loading : RewardBreakUiState

    data class Ready(
        val entitlementId: String,
        val durationMillis: Long,
        val remainingMillis: Long,
        val started: Boolean,
    ) : RewardBreakUiState

    data class Unavailable(val message: String) : RewardBreakUiState
}

/** Owns the reward-break lifecycle at the navigation boundary. */
@HiltViewModel
class RewardBreakViewModel @Inject constructor(
    private val rewardBreakDao: RewardBreakDao,
    private val playgroundUnlockReceiptDao: PlaygroundUnlockReceiptDao,
    private val miniGameResultDao: MiniGameResultDao,
    private val rewardDao: RewardDao,
    private val inventoryDao: InventoryDao,
    private val transactionRunner: RoomTransactionRunner,
    private val godModeManager: GodModeManager,
) : ViewModel() {
    private val _state = MutableStateFlow<RewardBreakUiState>(RewardBreakUiState.Loading)
    val state: StateFlow<RewardBreakUiState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    fun load(childId: String, rewardBreakId: String, requireActive: Boolean = false) {
        tickerJob?.cancel()
        _state.value = RewardBreakUiState.Loading
        viewModelScope.launch {
            if (godModeManager.isEnabledNow(childId)) {
                publishGodModeReady()
                return@launch
            }
            // Playground day-pass: once the daily quest receipt exists, the
            // playground is re-enterable for the rest of the local calendar day
            // without another 5-minute countdown expiring it.
            if (isPlaygroundUnlockedToday(childId)) {
                publishPlaygroundDayPass(childId, rewardBreakId)
                return@launch
            }
            val entitlement = rewardBreakDao.getById(rewardBreakId)
            if (entitlement == null || entitlement.childId != childId) {
                unavailable("This reward break is not available.")
                return@launch
            }

            val now = System.currentTimeMillis()
            if (requireActive && entitlement.state != RewardBreakPolicy.ACTIVE) {
                unavailable("Open the reward break from the Playroom first.")
                return@launch
            }
            if (!RewardBreakPolicy.canUse(entitlement, now)) {
                consumeIfOwned(entitlement, childId, now)
                unavailable("This reward break has finished.")
                return@launch
            }

            val remaining = RewardBreakPolicy.remainingAt(entitlement, now)
            publishReady(entitlement, remaining)
            if (entitlement.state == RewardBreakPolicy.ACTIVE) {
                startTicker(childId, rewardBreakId)
            }
        }
    }

    /** Starts a CREATED entitlement, or resumes a still-active one. */
    suspend fun begin(childId: String, rewardBreakId: String): Long? {
        if (godModeManager.isEnabledNow(childId)) {
            publishGodModeReady()
            return RewardBreakPolicy.DEFAULT_DURATION_MILLIS
        }
        // Playground day-pass: re-enterable until midnight, each launch gets a
        // fresh 5-minute session without consuming the day pass.
        if (isPlaygroundUnlockedToday(childId)) {
            val remaining = ensurePlaygroundSession(childId, rewardBreakId)
            if (remaining != null) {
                startTicker(childId, rewardBreakId)
            }
            return remaining
        }
        val entitlement = rewardBreakDao.getById(rewardBreakId)
        if (entitlement == null || entitlement.childId != childId) {
            unavailable("This reward break is not available.")
            return null
        }

        val now = System.currentTimeMillis()
        if (!RewardBreakPolicy.canUse(entitlement, now)) {
            consumeIfOwned(entitlement, childId, now)
            unavailable("This reward break has finished.")
            return null
        }

        if (entitlement.state == RewardBreakPolicy.CREATED) {
            rewardBreakDao.startIfCreated(rewardBreakId, childId, now)
        }

        val active = rewardBreakDao.getById(rewardBreakId)
        if (active == null || active.childId != childId || active.state != RewardBreakPolicy.ACTIVE) {
            unavailable("This reward break could not be started.")
            return null
        }

        val remaining = RewardBreakPolicy.remainingAt(active, now)
        if (remaining <= 0L) {
            consumeIfOwned(active, childId, now)
            unavailable("This reward break has finished.")
            return null
        }

        publishReady(active, remaining)
        startTicker(childId, rewardBreakId)
        return remaining
    }

    suspend fun saveResult(result: MiniGameResult): Boolean {
        if (godModeManager.isEnabledNow(result.childId)) return true
        // Playground day-pass: allow results for the whole local day without
        // requiring a still-ACTIVE 5-minute window. The 5-minute window still
        // gates the non-pass path so deep-link farming stays blocked.
        if (isPlaygroundUnlockedToday(result.childId)) {
            return saveResultInternal(result)
        }
        val entitlement = rewardBreakDao.getById(result.rewardBreakId)
        val now = System.currentTimeMillis()
        if (
            entitlement == null ||
            entitlement.childId != result.childId ||
            !RewardBreakPolicy.isValidResultWindow(
                entitlement = entitlement,
                resultStartedAtEpochMillis = result.startedAtEpochMillis,
                resultEndedAtEpochMillis = result.endedAtEpochMillis,
                nowEpochMillis = now,
            )
        ) {
            return false
        }

        return transactionRunner.run {
            // The result row is the idempotency gate for all mini-game
            // side-effects. A repeated navigation callback must not mint
            // another token balance or collectible.
            if (miniGameResultDao.getByIdempotencyKey(result.idempotencyKey) != null) {
                return@run true
            }
            miniGameResultDao.insert(
                MiniGameResultEntity(
                    sessionId = result.sessionId,
                    idempotencyKey = result.idempotencyKey,
                    rewardBreakId = result.rewardBreakId,
                    childId = result.childId,
                    gameId = result.gameId,
                    startedAtEpochMillis = result.startedAtEpochMillis,
                    endedAtEpochMillis = result.endedAtEpochMillis,
                    roundsCompleted = result.roundsCompleted,
                    successfulActions = result.correctOrders,
                    pawTokensEarned = result.pawTokensEarned.coerceAtLeast(0),
                    collectibleId = result.collectibleId,
                )
            )
            // Mini-games are a FINITE reward break, not a currency source. The play
            // itself is the reward, so no spendable COIN is minted from in-game
            // tokens (prevents farming Treat Shop currency by replaying games).
            if (result.pawTokensEarned > 0) {
                // Intentionally no rewardDao.insert — results are telemetry only.
            }
            result.collectibleId
                ?.takeIf { it.isNotBlank() }
                ?.let { collectibleId ->
                    inventoryDao.insertIgnoring(
                        InventoryEntity(
                            id = "mini-game-collectible:${result.childId}:$collectibleId",
                            childId = result.childId,
                            itemId = collectibleId,
                        )
                    )
                }
            true
        }
    }

    private suspend fun saveResultInternal(result: MiniGameResult): Boolean = transactionRunner.run {
        if (miniGameResultDao.getByIdempotencyKey(result.idempotencyKey) != null) {
            return@run true
        }
        miniGameResultDao.insert(
            MiniGameResultEntity(
                sessionId = result.sessionId,
                idempotencyKey = result.idempotencyKey,
                rewardBreakId = result.rewardBreakId,
                childId = result.childId,
                gameId = result.gameId,
                startedAtEpochMillis = result.startedAtEpochMillis,
                endedAtEpochMillis = result.endedAtEpochMillis,
                roundsCompleted = result.roundsCompleted,
                successfulActions = result.correctOrders,
                pawTokensEarned = result.pawTokensEarned.coerceAtLeast(0),
                collectibleId = result.collectibleId,
            )
        )
        result.collectibleId
            ?.takeIf { it.isNotBlank() }
            ?.let { collectibleId ->
                inventoryDao.insertIgnoring(
                    InventoryEntity(
                        id = "mini-game-collectible:${result.childId}:$collectibleId",
                        childId = result.childId,
                        itemId = collectibleId,
                    )
                )
            }
        true
    }

    suspend fun consume(childId: String, rewardBreakId: String) {
        if (godModeManager.isEnabledNow(childId)) {
            tickerJob?.cancel()
            unavailable("Playground complete.")
            return
        }
        // Playground day-pass: do NOT consume the entitlement on exit — the child
        // stays unlocked for the rest of the local day and can re-enter.
        if (isPlaygroundUnlockedToday(childId)) {
            tickerJob?.cancel()
            // Keep entitlement ACTIVE for re-entry; just show hub exit message.
            _state.value = RewardBreakUiState.Unavailable("Playground is open — come back anytime today!")
            return
        }
        val entitlement = rewardBreakDao.getById(rewardBreakId)
        if (entitlement?.childId == childId) {
            consumeIfOwned(entitlement, childId, System.currentTimeMillis())
        }
        tickerJob?.cancel()
        unavailable("Reward break complete.")
    }

    private fun publishReady(entitlement: RewardBreakEntitlementEntity, remaining: Long) {
        _state.value = RewardBreakUiState.Ready(
            entitlementId = entitlement.id,
            durationMillis = entitlement.durationMillis,
            remainingMillis = remaining,
            started = entitlement.state == RewardBreakPolicy.ACTIVE,
        )
    }

    private fun publishGodModeReady() {
        val duration = RewardBreakPolicy.DEFAULT_DURATION_MILLIS
        _state.value = RewardBreakUiState.Ready(
            entitlementId = GodModeManager.GOD_MODE_REWARD_BREAK_ID,
            durationMillis = duration,
            remainingMillis = duration,
            started = true,
        )
    }

    private suspend fun isPlaygroundUnlockedToday(childId: String): Boolean {
        val dayKey = LocalDate.now(ZoneId.systemDefault()).toString()
        return playgroundUnlockReceiptDao.getByChildAndDay(childId, dayKey) != null
    }

    private suspend fun publishPlaygroundDayPass(childId: String, rewardBreakId: String) {
        // Ensure an ACTIVE session exists so the hub can show Ready; create or refresh one.
        val remaining = ensurePlaygroundSession(childId, rewardBreakId) ?: RewardBreakPolicy.DEFAULT_DURATION_MILLIS
        _state.value = RewardBreakUiState.Ready(
            entitlementId = rewardBreakId,
            durationMillis = RewardBreakPolicy.DEFAULT_DURATION_MILLIS,
            remainingMillis = remaining,
            started = true,
        )
        startTicker(childId, rewardBreakId)
    }

    private suspend fun ensurePlaygroundSession(childId: String, rewardBreakId: String): Long? {
        val now = System.currentTimeMillis()
        var entitlement = rewardBreakDao.getById(rewardBreakId)
        if (entitlement == null || entitlement.childId != childId) {
            val dailyQuestCompletionId = RewardBreakPolicy.dailyQuestCompletionId(childId, LocalDate.now(ZoneId.systemDefault()).toString())
            rewardBreakDao.insertIgnoring(
                RewardBreakPolicy.newEntitlement(
                    id = rewardBreakId,
                    childId = childId,
                    dailyQuestCompletionId = dailyQuestCompletionId,
                    nowEpochMillis = now,
                )
            )
            entitlement = rewardBreakDao.getById(rewardBreakId) ?: return null
            // New entitlement is CREATED — activate it immediately for the day-pass.
            rewardBreakDao.startIfCreated(rewardBreakId, childId, now)
            entitlement = rewardBreakDao.getById(rewardBreakId) ?: return null
        }
        // Day-pass: each re-entry refreshes to a full 5-minute window. Use
        // reactivateForDayPass so CONSUMED / expired sessions cleanly re-arm
        // (resets startedAt + remaining + state in one atomic update).
        rewardBreakDao.reactivateForDayPass(
            id = rewardBreakId,
            childId = childId,
            startedAtEpochMillis = now,
            remaining = RewardBreakPolicy.DEFAULT_DURATION_MILLIS,
        )
        val refreshed = rewardBreakDao.getById(rewardBreakId) ?: entitlement
        _state.value = RewardBreakUiState.Ready(
            entitlementId = refreshed.id,
            durationMillis = refreshed.durationMillis,
            remainingMillis = RewardBreakPolicy.DEFAULT_DURATION_MILLIS,
            started = true,
        )
        return RewardBreakPolicy.DEFAULT_DURATION_MILLIS
    }

    private fun startTicker(childId: String, rewardBreakId: String) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(250L)
                val entitlement = rewardBreakDao.getById(rewardBreakId)
                if (entitlement == null || entitlement.childId != childId) {
                    unavailable("This reward break is not available.")
                    break
                }
                val remaining = RewardBreakPolicy.remainingAt(entitlement, System.currentTimeMillis())
                if (remaining <= 0L) {
                    consumeIfOwned(entitlement, childId, System.currentTimeMillis())
                    unavailable("This reward break has finished.")
                    break
                }
                _state.update { current ->
                    (current as? RewardBreakUiState.Ready)?.copy(remainingMillis = remaining)
                        ?: current
                }
            }
        }
    }

    private suspend fun consumeIfOwned(
        entitlement: RewardBreakEntitlementEntity,
        childId: String,
        nowEpochMillis: Long,
    ) {
        rewardBreakDao.consumeIfUnconsumed(entitlement.id, childId, nowEpochMillis)
    }

    private fun unavailable(message: String) {
        _state.value = RewardBreakUiState.Unavailable(message)
    }
}
