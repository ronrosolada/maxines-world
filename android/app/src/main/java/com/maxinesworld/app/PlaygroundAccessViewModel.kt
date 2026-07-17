package com.maxinesworld.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.playground.PlaygroundGateRepository
import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin adapter over [PlaygroundGateRepository] — owns only the collected
 * StateFlow. All evaluation logic lives in the shared repository.
 */
@HiltViewModel
class PlaygroundAccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gateRepository: PlaygroundGateRepository,
) : ViewModel() {

    private val childId: String = savedStateHandle["childId"] ?: error("childId missing")

    private val _state = MutableStateFlow<PlaygroundAccessUiState>(PlaygroundAccessUiState.Loading)
    val state: StateFlow<PlaygroundAccessUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            gateRepository.gate(childId)
                .catch { e ->
                    _state.update {
                        PlaygroundAccessUiState.Blocked(
                            PlaygroundGateState(
                                childId = childId, dayKey = "", totalAssigned = 0,
                                completed = 0, status = PlaygroundGateStatus.Error,
                            )
                        )
                    }
                }
                .collect { gate ->
                    _state.update {
                        when (gate.status) {
                            PlaygroundGateStatus.Unlocked -> PlaygroundAccessUiState.Allowed
                            else -> PlaygroundAccessUiState.Blocked(gate)
                        }
                    }
                }
        }
    }
}

sealed interface PlaygroundAccessUiState {
    data object Loading : PlaygroundAccessUiState
    data object Allowed : PlaygroundAccessUiState
    data class Blocked(val gate: PlaygroundGateState) : PlaygroundAccessUiState
}
