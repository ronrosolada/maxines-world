package com.maxinesworld.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.InventoryEntity
import com.maxinesworld.coredatabase.MiniGameResultDao
import com.maxinesworld.coredatabase.MiniGameResultEntity
import com.maxinesworld.coredatabase.RewardBreakDao
import com.maxinesworld.coredatabase.RewardBreakEntitlementEntity
import com.maxinesworld.coredatabase.RewardBreakPolicy
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coredatabase.RoomTransactionRunner
import com.maxinesworld.engineminigame.MiniGameResult
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val miniGameResultDao: MiniGameResultDao,
    private val rewardDao: RewardDao,
    private val inventoryDao: InventoryDao,
    private val transactionRunner: RoomTransactionRunner,
) : ViewModel() {
    private val _state = MutableStateFlow<RewardBreakUiState>(RewardBreakUiState.Loading)
    val state: StateFlow<RewardBreakUiState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    fun load(childId: String, rewardBreakId: String, requireActive: Boolean = false) {
        tickerJob?.cancel()
        _state.value = RewardBreakUiState.Loading
        viewModelScope.launch {
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
            if (result.pawTokensEarned > 0) {
                rewardDao.insertIgnoring(
                    RewardEntity(
                        id = "mini-game:${result.idempotencyKey}:tokens",
                        childId = result.childId,
                        type = "COIN",
                        amount = result.pawTokensEarned,
                        subject = "reward-break",
                        metadata = "mini-game:${result.gameId}:${result.idempotencyKey}",
                    )
                )
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

    suspend fun consume(childId: String, rewardBreakId: String) {
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
