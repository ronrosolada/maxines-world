package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coremodel.ChildLevelPolicy
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Playroom home from real persisted data.
 *
 * The Kindness (GMRC) island is a REAL gate: it unlocks only when the child
 * reaches Level 4 (12 distinct lessons completed). Previously it was
 * hardcoded locked forever. All other islands stay unlocked so the child can
 * always reach the level needed to unlock Kindness.
 */
@HiltViewModel
class PlayroomHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childProfileDao: ChildProfileDao,
    private val lessonCompletionDao: LessonCompletionDao,
) : ViewModel() {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    private val _state = MutableStateFlow(PlayroomHomeState())
    val state: StateFlow<PlayroomHomeState> = _state.asStateFlow()

    init {
        val profileFlow = childProfileDao.observeById(childId)
        val lessonCountFlow = lessonCompletionDao.observeDistinctLessonCount(childId)

        viewModelScope.launch {
            combine(profileFlow, lessonCountFlow) { profile, completedLessons ->
                buildState(profile?.name, completedLessons)
            }.collect { _state.value = it }
        }
    }

    private fun buildState(childName: String?, completedLessons: Int): PlayroomHomeState {
        val level = ChildLevelPolicy.levelFor(completedLessons)
        val kindnessUnlocked = level >= ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        val lessonsToGo = ChildLevelPolicy.lessonsRemainingTo(
            completedLessons, ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        )

        val islands = defaultPlayroomIslands.map { island ->
            if (island.id == "gmrc") {
                island.copy(
                    locked = !kindnessUnlocked,
                    subtitle = if (kindnessUnlocked) {
                        "Kindness awaits!"
                    } else {
                        "Unlocks at Level $KINDNESS_UNLOCK_LEVEL · $lessonsToGo lesson${if (lessonsToGo == 1) "" else "s"} to go"
                    }
                )
            } else island
        }

        return PlayroomHomeState(
            childName = childName ?: "Maxine",
            islands = islands,
        )
    }

    companion object {
        const val KINDNESS_UNLOCK_LEVEL = ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
    }
}
