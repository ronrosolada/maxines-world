package com.maxinesworld.featurelessonplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.coremodel.*
import com.maxinesworld.coredatabase.*
import com.maxinesworld.corecontent.ActiveContentIndex
import com.maxinesworld.corecontent.ContentLessonLoader
import com.maxinesworld.corecontent.LessonLoader
import com.maxinesworld.enginemastery.MasteryEngine
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.coremodel.gamification.FishTreatPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 application: Application,
 private val lessonLoader: LessonLoader,
 private val progressEventDao: ProgressEventDao,
 private val masteryRecordDao: MasteryRecordDao,
 private val rewardDao: RewardDao,
 private val lessonCompletionDao: LessonCompletionDao,
 private val masteryEngine: MasteryEngine,
 private val badgeAwarder: BadgeAwarder,
 private val activeContentIndex: ActiveContentIndex
) : AndroidViewModel(application) {

    private val contentLessonLoader = ContentLessonLoader(application, activeContentIndex)
    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()
    private var childId: String = ""
    /** Stable attempt ID from the lesson-start event, persistent across process death. */
    private var attemptId: String = ""

    fun loadLesson(lessonId: String, childId: String = "") {
        this.childId = childId
        this.attemptId = "${lessonId}:${System.currentTimeMillis()}"
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val lesson = withContext(Dispatchers.IO) {
                try {
                    val m1 = contentLessonLoader.loadLesson(lessonId)
                    android.util.Log.d("LessonVM", "loadLesson result: $lessonId → m1=${m1 != null}")
                    if (m1 != null) {
                        val manifest = convertToLessonManifest(m1)
                        android.util.Log.d("LessonVM", "Converted: ${manifest.steps.size} steps, subject=${manifest.subject}")
                        manifest
                    } else {
                        val fallback = lessonLoader.loadLesson(lessonId)
                        android.util.Log.d("LessonVM", "Fallback lesson: ${fallback != null}")
                        fallback
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LessonVM", "Load error: ${e.message}", e)
                    lessonLoader.loadLesson(lessonId)
                }
            }
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
            // Only mark complete if all required steps have results
            val lesson = it.lesson
            val requiredIds = lesson?.steps?.map { s -> s.id }?.toSet() ?: emptySet()
            val completedIds = it.results.map { r -> r.activityId }.toSet()
            val allDone = completedIds.containsAll(requiredIds) && next >= it.totalSteps
            if (allDone) it.copy(currentStep = next, isComplete = true)
            else it.copy(currentStep = next, showFeedback = false)
        }
        if (_state.value.isComplete) saveProgress()
    }

    private fun saveProgress() {
        val lesson = _state.value.lesson ?: return
        val scoredResults = _state.value.results.filter { it.scored }
        if (childId.isBlank()) return

        viewModelScope.launch {
            // Idempotency guard: insert completion record
            val completionId = "${childId}:${lesson.id}:${attemptId}"
            val alreadyCompleted = lessonCompletionDao.exists(childId, lesson.id)
            if (alreadyCompleted) return@launch

            val scoredCorrect = scoredResults.count { it.correct }
            val accuracy = if (scoredResults.isNotEmpty()) scoredCorrect.toDouble() / scoredResults.size else 0.0

            val inserted = lessonCompletionDao.insertIgnoring(
                LessonCompletionEntity(
                    id = completionId,
                    childId = childId,
                    lessonId = lesson.id,
                    attemptId = attemptId,
                    accuracy = accuracy
                )
            )
            // If insert ignored (duplicate key), skip all writes
            if (inserted == -1L) return@launch

            // Write progress events with deterministic IDs
            for (result in scoredResults) {
                progressEventDao.insert(ProgressEventEntity(
                    id = "${completionId}:event:${result.activityId}",
                    childId = childId,
                    skillId = lesson.skillIds.firstOrNull() ?: lesson.id,
                    lessonId = lesson.id, activityId = result.activityId,
                    eventType = "activity_result",
                    accuracy = if (result.correct) 1.0 else 0.0,
                    attempts = result.attempts, hintsUsed = result.hintsUsed,
                    responseTimeMs = result.responseTimeMs
                ))
            }

            // Fish treat reward with deterministic ID
            val improved = scoredResults.any { it.attempts > 1 && it.correct }
            val crossed = accuracy >= 0.8
            val treats = FishTreatPolicy.amount(completed = true, improvedAfterRetry = improved, crossedMasteryThreshold = crossed)
            val rewardKey = FishTreatPolicy.rewardKey(childId, lesson.id, attemptId)
            rewardDao.insert(RewardEntity(
                id = "${completionId}:reward:fish_treat",
                childId = childId,
                type = FishTreatPolicy.TYPE,
                subject = lesson.subject,
                amount = treats,
                metadata = "rewardKey=$rewardKey,accuracy=$accuracy"
            ))

            val progress = badgeAwarder.recordSubjectCompletion(childId, lesson.subject)
            if (progress.newlyAwardedBadge != null) {
                _state.update { it.copy(badgeAwarded = progress.newlyAwardedBadge) }
            }
        }
    }

    fun onActivityResult(result: ActivityResult) {
        // Prevent duplicate results for the same activity
        if (_state.value.results.any { it.activityId == result.activityId }) return
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

    private fun convertToLessonManifest(m1: Month1Lesson): LessonManifest {
        val subj = contentLessonLoader.toAppSubject(m1.subject)
        return LessonManifest(
            id = m1.lessonId, schemaVersion = m1.schemaVersion,
            moduleId = "g3-m01",
            title = m1.title, subject = subj, objective = m1.objective,
            skillIds = listOf(m1.lessonId),
            guideCharacter = if (subj == "english") "Mira" else "Milo",
            estimatedMinutes = m1.estimatedMinutes,
            languageOfInstruction = m1.language,
            steps = m1.activities.map { act ->
                ActivityStep(
                    id = act.activityId, type = when (act.type) {
                        "ANIMATED_EXPLANATION" -> "ANIMATED_EXPLANATION_V1"
                        "MULTIPLE_CHOICE" -> "MULTIPLE_CHOICE_V1"
                        "SORT_AND_CLASSIFY" -> "SORT_AND_CLASSIFY_V1"
                        "HOTSPOT_IMAGE" -> "HOTSPOT_IMAGE_V1"
                        "MATCHING_PAIRS" -> "MATCHING_PAIRS_V1"
                        "SEQUENCE_BUILDER" -> "SEQUENCE_BUILDER_V1"
                        "INTERACTIVE_SPEC" -> "INTERACTIVE_SPEC_V1"
                        else -> "ANIMATED_EXPLANATION_V1"
                    },
                    narrationText = act.instruction,
                    options = emptyList(), correctIndex = -1,
                    feedback = ActivityFeedback(
                        correct = act.feedback?.correct ?: "Great job!",
                        incorrect = act.feedback?.retry ?: "Let's try again!"
                    )
                )
            }
        )
    }
}
