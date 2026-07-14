package com.maxinesworld.featurechildhome

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.DailyQuestDao
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coremodel.gamification.FishTreatPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class VillageHomeV17State(
    val isLoading: Boolean = true,
    val error: String? = null,
    val childName: String = "",
    val level: Int = 0,
    val currentXp: Int = 0,
    val targetXp: Int = 1,
    val fishTreats: Int = 0,
    val hasNewDiscovery: Boolean = false,
    val questText: String = "Complete 3 activities",
    val questProgressText: String = "0 / 3",
    val destinations: List<VillageDestinationV17> = defaultVillageDestinationsV17,
)

@HiltViewModel
class VillageHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childProfileDao: ChildProfileDao,
    private val progressEventDao: ProgressEventDao,
    private val rewardDao: RewardDao,
    private val dailyQuestDao: DailyQuestDao,
) : ViewModel() {
    private val childId: String = savedStateHandle["childId"] ?: error("childId missing")

    private val _state = MutableStateFlow(VillageHomeV17State())
    val state: StateFlow<VillageHomeV17State> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val profile = childProfileDao.getById(childId)
                if (profile == null) {
                    _state.value = _state.value.copy(isLoading = false, error = "Child not found")
                    return@launch
                }

                val fishTreatTotal = rewardDao.getTotalByType(childId, FishTreatPolicy.TYPE) ?: 0

                val today = java.time.LocalDate.now().toString()
                val quest = dailyQuestDao.getByChildAndDate(childId, today)
                val completedCount = quest?.let { parsed ->
                    try {
                        val el = kotlinx.serialization.json.Json.parseToJsonElement(parsed.completedLessons)
                        if (el is kotlinx.serialization.json.JsonArray) el.size else 0
                    } catch (_: Exception) { 0 }
                } ?: 0

                _state.value = VillageHomeV17State(
                    isLoading = false,
                    childName = profile.name,
                    level = 0,
                    currentXp = 0,
                    targetXp = 1,
                    fishTreats = fishTreatTotal,
                    hasNewDiscovery = false,
                    questText = "Complete 3 activities",
                    questProgressText = "$completedCount / 3",
                    destinations = defaultVillageDestinationsV17,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun onRetry() { load() }
}
