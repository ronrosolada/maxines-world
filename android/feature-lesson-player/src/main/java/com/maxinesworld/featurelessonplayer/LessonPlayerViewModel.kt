package com.maxinesworld.featurelessonplayer

import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coremodel.LessonManifest
import com.maxinesworld.coredatabase.*
import com.maxinesworld.corecontent.LessonLoader
import com.maxinesworld.enginemastery.MasteryEngine
import com.maxinesworld.featurerewards.BadgeAwarder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import javax.inject.Inject

data class LessonUiState(
    val isLoading: Boolean = true,
    val lesson: LessonManifest? = null,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val showFeedback: Boolean = false,
    val feedbackText: String = "",
    val feedbackCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
    val results: List<ActivityResult> = emptyList(),
    val badgeAwarded: CollectibleBadge? = null
)

@HiltViewModel
class LessonPlayerViewModel @Inject constructor(
    private val lessonLoader: LessonLoader,
    private val progressEventDao: ProgressEventDao,
    private val masteryRecordDao: MasteryRecordDao,
    private val rewardDao: RewardDao,
    private val masteryEngine: MasteryEngine,
    private val badgeAwarder: BadgeAwarder
) : ViewModel() {

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()
    private var childId: String = ""
    private var progressSaved = false

    fun loadLesson(lessonId: String, childId: String = "") {
        this.childId = childId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val lesson = withContext(Dispatchers.IO) { lessonLoader.loadLesson(lessonId) }
            _state.update {
                it.copy(isLoading = false, lesson = lesson,
                    totalSteps = lesson?.steps?.size ?: 0,
                    error = if (lesson == null) "Could not load lesson." else null)
            }
        }
    }

    fun onNextStep() {
        _state.update {
            val next = it.currentStep + 1
            if (next >= it.totalSteps) it.copy(currentStep = next, isComplete = true)
            else it.copy(currentStep = next, showFeedback = false)
        }
        if (_state.value.isComplete) saveProgress()
    }

    private fun saveProgress() {
        if (progressSaved) return
        progressSaved = true
        val lesson = _state.value.lesson ?: return
        val scoredResults = _state.value.results.filter { it.scored }
        if (childId.isBlank() || scoredResults.isEmpty()) return

        viewModelScope.launch {
            for (result in scoredResults) {
                progressEventDao.insert(ProgressEventEntity(
                    id = UUID.randomUUID().toString(), childId = childId,
                    skillId = lesson.skillIds.firstOrNull() ?: lesson.id,
                    lessonId = lesson.id, activityId = result.activityId,
                    eventType = "activity_result",
                    accuracy = if (result.correct) 1.0 else 0.0,
                    attempts = result.attempts, hintsUsed = result.hintsUsed,
                    responseTimeMs = result.responseTimeMs
                ))
            }
            for (skillId in lesson.skillIds) {
                val events = progressEventDao.getByChildAndSkill(childId, skillId)
                val ms = masteryEngine.computeMastery(events.map { e ->
                    com.maxinesworld.coremodel.ProgressEvent(e.id, e.childId, e.skillId, e.lessonId, e.activityId, e.eventType, e.accuracy, e.attempts, e.hintsUsed, e.responseTimeMs, e.timestamp)
                })
                masteryRecordDao.upsert(MasteryRecordEntity(
                    id = "${childId}_$skillId", childId = childId, skillId = skillId,
                    state = ms.name,
                    accuracy = if (events.isNotEmpty()) events.map { it.accuracy }.average() else 0.0,
                    totalAttempts = events.size, lastActivityAt = System.currentTimeMillis()
                ))
            }
            val scoredCorrect = scoredResults.count { it.correct }
            val accuracy = if (scoredResults.isNotEmpty()) scoredCorrect.toDouble() / scoredResults.size else 0.0
            val starsEarned = kotlin.math.ceil(accuracy * 5).toInt().coerceIn(1, 5)
            rewardDao.insert(RewardEntity(id = UUID.randomUUID().toString(), childId = childId, type = "STAR", subject = lesson.subject, amount = starsEarned))
            if (accuracy >= 0.8) {
                rewardDao.insert(RewardEntity(id = UUID.randomUUID().toString(), childId = childId, type = "COIN", subject = lesson.subject, amount = 10))
            }

            // Badge: record subject completion, check for daily challenge
            val progress = badgeAwarder.recordSubjectCompletion(childId, lesson.subject)
            if (progress.newlyAwardedBadge != null) {
                _state.update { it.copy(badgeAwarded = progress.newlyAwardedBadge) }
            }
        }
    }

    fun onActivityResult(result: ActivityResult) {
        val lesson = _state.value.lesson
        val step = lesson?.steps?.getOrNull(_state.value.currentStep)
        _state.update { it.copy(results = it.results + result) }
        if (!result.scored) onNextStep()
        else _state.update {
            it.copy(showFeedback = true,
                feedbackText = if (result.correct) step?.feedback?.correct ?: "Great job!"
                    else step?.feedback?.incorrect ?: "Let's try again!",
                feedbackCorrect = result.correct)
        }
    }
}
