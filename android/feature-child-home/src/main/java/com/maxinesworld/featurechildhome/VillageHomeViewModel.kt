package com.maxinesworld.featurechildhome

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.*
import com.maxinesworld.coremodel.gamification.FishTreatPolicy
import com.maxinesworld.playground.PlaygroundGateEvaluator
import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@Immutable
data class VillageHomeState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val childName: String = "",
    val fishTreats: Int = 0,
    val hasNewDiscovery: Boolean = false,
    val discoveries: List<WildlifeDiscovery> = emptyList(),
    val showMiraRequest: Boolean = false,
    val cafeUnlock: CafeUnlockState = CafeUnlockState(),
    val questText: String = "Complete 3 activities",
    val questProgressText: String = "0 / 3",
    val destinations: List<VillageDestination> = defaultDestinations,
    val playground: PlaygroundGateState? = null,
    val showPlaygroundUnlockCelebration: Boolean = false,
)

@Immutable
data class WildlifeDiscovery(
    val id: String,
    val name: String,
    val description: String,
    val subject: String,
    val iconEmoji: String
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

@HiltViewModel
class VillageHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childProfileDao: ChildProfileDao,
    private val progressEventDao: ProgressEventDao,
    private val rewardDao: RewardDao,
    private val dailyQuestDao: DailyQuestDao,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val playgroundUnlockReceiptDao: PlaygroundUnlockReceiptDao,
) : ViewModel() {
    private val childId: String = savedStateHandle["childId"] ?: error("childId missing")

    private val _state = MutableStateFlow(VillageHomeState())
    val state: StateFlow<VillageHomeState> = _state.asStateFlow()

    private var observedGateAtLeastOnce = false
    private var previousGateStatus: PlaygroundGateStatus? = null

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
                val cafePurchased = rewardDao.getTotalByType(childId, "CAFE_UNLOCK_cafe-cushion-teal") ?: 0

                val today = LocalDate.now().toString()
                val quest = dailyQuestDao.getByChildAndDate(childId, today)
                val completedCount = quest?.let { p ->
                    try {
                        val el = kotlinx.serialization.json.Json.parseToJsonElement(p.completedLessons)
                        if (el is kotlinx.serialization.json.JsonArray) el.size else 0
                    } catch (_: Exception) { 0 }
                } ?: 0

                // ─── Playground gate evaluation ───
                val questSet = dailyQuestSetDao.getByChildAndDay(childId, today)
                val assignedIds: Set<String> = questSet?.let { parseJsonArray(it.assignedQuestIds) } ?: emptySet()
                val completedIds = dailyQuestCompletionDao.getCompletedQuestIds(childId, today).toSet()
                val hasUnlockReceipt = playgroundUnlockReceiptDao.existsByChildAndDay(childId, today)
                val playgroundState = PlaygroundGateEvaluator.evaluate(
                    childId = childId,
                    dayKey = today,
                    assignedQuestIds = if (questSet != null) assignedIds else null,
                    completedQuestIds = if (questSet != null) completedIds else null,
                    hasUnlockReceipt = hasUnlockReceipt
                )
                acceptGate(playgroundState)

                val todayEnglish = progressEventDao.getByChildAndSkill(childId, "english").filter { isToday(it.timestamp) }
                val showMira = todayEnglish.isEmpty()

                val discoveryRewards = rewardDao.getTotalByType(childId, "DISCOVERY") ?: 0
                val scienceProgress = progressEventDao.getByChildAndSkill(childId, "science").size
                val discoveries = if (discoveryRewards > 0 || scienceProgress > 0) {
                    listOf(WildlifeDiscovery("tarsier", "Philippine Tarsier",
                        "Tiny tree-dweller with huge eyes — found only in the Philippines!",
                        "Science", "\uD83D\uDC12"))
                } else emptyList()

                _state.value = VillageHomeState(
                    isLoading = false,
                    childName = profile.name,
                    fishTreats = fishTreatTotal,
                    hasNewDiscovery = discoveries.isNotEmpty() && discoveryRewards == 0,
                    discoveries = discoveries,
                    showMiraRequest = showMira,
                    cafeUnlock = CafeUnlockState(
                        progress = fishTreatTotal.coerceAtMost(12),
                        isUnlocked = fishTreatTotal >= 12,
                        isPurchased = cafePurchased > 0,
                    ),
                    questText = "Complete 3 activities",
                    questProgressText = "$completedCount / 3",
                    destinations = defaultDestinations,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun onRetry() { load() }

    private fun acceptGate(next: PlaygroundGateState) {
        val shouldCelebrate = observedGateAtLeastOnce &&
            previousGateStatus == PlaygroundGateStatus.Locked &&
            next.status == PlaygroundGateStatus.Unlocked
        previousGateStatus = next.status
        observedGateAtLeastOnce = true
        _state.update {
            it.copy(
                playground = next,
                showPlaygroundUnlockCelebration =
                    it.showPlaygroundUnlockCelebration || shouldCelebrate,
            )
        }
    }

    fun dismissPlaygroundUnlockCelebration() {
        _state.update { it.copy(showPlaygroundUnlockCelebration = false) }
    }

    private fun isToday(epochMs: Long): Boolean {
        val today = java.time.Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        return today == LocalDate.now()
    }

    private fun parseJsonArray(json: String): Set<String> {
        return try {
            val el = kotlinx.serialization.json.Json.parseToJsonElement(json)
            if (el is kotlinx.serialization.json.JsonArray) {
                el.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }.toSet()
            } else emptySet()
        } catch (_: Exception) { emptySet() }
    }
}
