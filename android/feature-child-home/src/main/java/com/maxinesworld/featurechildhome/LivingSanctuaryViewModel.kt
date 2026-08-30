package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import com.maxinesworld.featurerewards.SanctuaryScene
import com.maxinesworld.featurerewards.SanctuarySceneEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LivingSanctuaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rewardDao: RewardDao,
    private val badgeDao: CollectedBadgeDao,
) : ViewModel() {
    private val childId: String = checkNotNull(savedStateHandle["childId"])
    private val _scene = MutableStateFlow(SanctuaryScene(emptyList()))
    val scene: StateFlow<SanctuaryScene> = _scene.asStateFlow()

    init {
        viewModelScope.launch {
            val habitats = rewardDao.getByChildAndType(childId, DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE)
                .map { it.metadata }.toSet()
            val badges = badgeDao.getAllByChild(childId).map { it.badgeId }.toSet()
            _scene.value = SanctuarySceneEngine().build(habitats, badges, LocalDate.now().toEpochDay().toInt())
        }
    }
}
