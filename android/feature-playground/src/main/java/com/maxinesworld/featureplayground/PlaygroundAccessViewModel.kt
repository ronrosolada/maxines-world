package com.maxinesworld.featureplayground

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.DailyQuestCompletionDao
import com.maxinesworld.coredatabase.DailyQuestSetDao
import com.maxinesworld.coredatabase.PlaygroundUnlockReceiptDao
import com.maxinesworld.playground.PlaygroundGateEvaluator
import com.maxinesworld.playground.PlaygroundGateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PlaygroundAccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val playgroundUnlockReceiptDao: PlaygroundUnlockReceiptDao,
) : ViewModel() {
    private val childId: String = savedStateHandle["childId"] ?: error("childId missing")

    private val _state = MutableStateFlow<PlaygroundAccessUiState>(PlaygroundAccessUiState.Loading)
    val state: StateFlow<PlaygroundAccessUiState> = _state.asStateFlow()

    init { evaluate() }

    private fun evaluate() {
        viewModelScope.launch {
            _state.value = PlaygroundAccessUiState.Loading
            try {
                val dayKey = LocalDate.now().toString()
                val questSet = dailyQuestSetDao.getByChildAndDay(childId, dayKey)
                val completedIds = dailyQuestCompletionDao.getCompletedQuestIds(childId, dayKey).toSet()
                val hasReceipt = playgroundUnlockReceiptDao.existsByChildAndDay(childId, dayKey)

                val assignedIds: Set<String> = questSet?.assignedQuestIds?.let { json ->
                    try {
                        json.trimStart('[').trimEnd(']')
                            .split(",").map { it.trim().removeSurrounding("\"") }
                            .filter { it.isNotBlank() }.toSet()
                    } catch (_: Exception) { emptySet() }
                } ?: emptySet()

                val gate = PlaygroundGateEvaluator.evaluate(
                    childId = childId,
                    dayKey = dayKey,
                    assignedQuestIds = if (questSet != null) assignedIds else null,
                    completedQuestIds = if (questSet != null) completedIds else null,
                    hasUnlockReceipt = hasReceipt,
                )

                _state.value = when (gate.status) {
                    com.maxinesworld.playground.PlaygroundGateStatus.Unlocked ->
                        PlaygroundAccessUiState.Allowed
                    else ->
                        PlaygroundAccessUiState.Blocked(gate)
                }
            } catch (e: Exception) {
                _state.value = PlaygroundAccessUiState.Blocked(
                    PlaygroundGateState(
                        childId = childId, dayKey = LocalDate.now().toString(),
                        totalAssigned = 0, completed = 0,
                        status = com.maxinesworld.playground.PlaygroundGateStatus.Error
                    )
                )
            }
        }
    }
}

sealed interface PlaygroundAccessUiState {
    data object Loading : PlaygroundAccessUiState
    data object Allowed : PlaygroundAccessUiState
    data class Blocked(val gate: PlaygroundGateState) : PlaygroundAccessUiState
}
