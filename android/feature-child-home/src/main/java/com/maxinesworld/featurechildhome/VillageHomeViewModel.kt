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
data class WildlifeDiscovery(
    val id: String,
    val name: String,
    val description: String,
    val subject: String,
    val iconHex: String
)

@Immutable
data class CafeUnlockState(
    val itemId: String = "cafe-cushion-teal",
    val name: String = "Teal Cushion",
    val requiredTreats: Int = 12,
    val progress: Int = 0,
    val isUnlocked: Boolean = false,
    val isPurchased: Boolean = false,
)

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
    val discoveries: List<WildlifeDiscovery> = emptyList(),
    val showMiraRequest: Boolean = false,
    val cafeUnlock: CafeUnlockState = CafeUnlockState(),
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

                // Mira request: shown if no English progress today
                val todayEnglishEvents = progressEventDao.getByChild(childId)
                    .filter { it.lessonId.startsWith("english") && isToday(it.timestamp) }
                val showMiraRequest = todayEnglishEvents.isEmpty()

                // Café unlock
                val cafePurchasedReward = rewardDao.getTotalByType(childId, "CAFE_UNLOCK_cafe-cushion-teal") ?: 0
                val cafeUnlock = CafeUnlockState(
                    progress = fishTreatTotal.coerceAtMost(12),
                    isUnlocked = fishTreatTotal >= 12,
                    isPurchased = cafePurchasedReward > 0,
                )

                // Wildlife discoveries
                val discoveryRewards = rewardDao.getTotalByType(childId, "DISCOVERY") ?: 0
                val discoveries = if (discoveryRewards > 0) {
                    val scienceProgress = progressEventDao.getByChildAndSkill(childId, "science-g3-m01-d01").size
                    if (scienceProgress > 0) listOf(
                        WildlifeDiscovery("tarsier", "Philippine Tarsier",
                            "Tiny tree-dweller with huge eyes — found only in the Philippines!",
                            "Science", "🐒")
                    ) else emptyList()
                } else {
                    val scienceProgress = progressEventDao.getByChildAndSkill(childId, "science-g3-m01-d01").size
                    if (scienceProgress > 0) {
                        // First science completion → grant discovery
                        val discoveryKey = "${childId}:discovery:tarsier"
                        rewardDao.insert(com.maxinesworld.coredatabase.RewardEntity(
                            id = discoveryKey, childId = childId,
                            type = "DISCOVERY", subject = "Science", amount = 1))
                        listOf(
                            WildlifeDiscovery("tarsier", "Philippine Tarsier",
                                "Tiny tree-dweller with huge eyes — found only in the Philippines!",
                                "Science", "🐒")
                        )
                    } else emptyList()
                }

                val hasNewDiscovery = discoveries.isNotEmpty() && discoveryRewards == 0

                _state.value = VillageHomeV17State(
                    isLoading = false,
                    childName = profile.name,
                    level = 0,
                    currentXp = 0,
                    targetXp = 1,
                    fishTreats = fishTreatTotal,
                    hasNewDiscovery = hasNewDiscovery,
                    discoveries = discoveries,
                    showMiraRequest = showMiraRequest,
                    cafeUnlock = cafeUnlock,
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

    private fun isToday(epochMs: Long): Boolean {
        val today = java.time.Instant.ofEpochMilli(epochMs)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return today == java.time.LocalDate.now()
    }
}