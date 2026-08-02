package com.maxinesworld.featurelessonplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.coremodel.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.maxinesworld.coredatabase.*
import com.maxinesworld.corecontent.ActiveContentIndex
import com.maxinesworld.corecontent.ContentLessonLoader
import com.maxinesworld.corecontent.LessonLoader
import com.maxinesworld.enginemastery.MasteryEngine
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
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
    val badgeAwarded: CollectibleBadge? = null,
    val expeditionProgress: ChallengeProgress = ChallengeProgress(),
)

@HiltViewModel
class LessonPlayerViewModel @Inject constructor(
    application: Application,
    private val lessonLoader: LessonLoader,
    private val progressEventDao: ProgressEventDao,
    private val masteryRecordDao: MasteryRecordDao,
    private val rewardDao: RewardDao,
    private val masteryEngine: MasteryEngine,
    private val badgeAwarder: BadgeAwarder,
    private val activeContentIndex: ActiveContentIndex,
    private val lessonCompletionDao: LessonCompletionDao,
) : AndroidViewModel(application) {

    private val contentLessonLoader = ContentLessonLoader(application, activeContentIndex)
    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()
    private var childId: String = ""
    private var progressSaved = false

    fun loadLesson(lessonId: String, childId: String = "") {
        this.childId = childId
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
            val scoredCorrect = scoredResults.count { it.correct }
            val accuracy = if (scoredResults.isNotEmpty()) scoredCorrect.toDouble() / scoredResults.size else 0.0
            val firstLessonCompletion = !lessonCompletionDao.exists(childId, lesson.id)
            // Record idempotent lesson completion (distinct lessonId drives child level).
            lessonCompletionDao.insertIgnoring(
                LessonCompletionEntity(
                    id = UUID.randomUUID().toString(),
                    childId = childId,
                    lessonId = lesson.id,
                    attemptId = UUID.randomUUID().toString(),
                    accuracy = accuracy,
                    completedAtEpochMillis = System.currentTimeMillis(),
                )
            )
            if (firstLessonCompletion) {
                val rewardKey = "lesson-first:$childId:${lesson.id}"
                val starsEarned = 1 +
                    (if (accuracy >= 0.8) 1 else 0) +
                    (if (accuracy >= 0.95) 1 else 0)
                rewardDao.insertIgnoring(RewardEntity(
                    id = "$rewardKey:STAR",
                    childId = childId,
                    type = "STAR",
                    subject = lesson.subject,
                    amount = starsEarned.coerceIn(1, 3),
                    metadata = rewardKey,
                ))
                // Coins are persisted too — the completion screen shows them, so
                // the Parent Dashboard must be able to read them back.
                if (accuracy >= 0.8) {
                    rewardDao.insertIgnoring(RewardEntity(
                        id = "$rewardKey:COIN",
                        childId = childId,
                        type = "COIN",
                        subject = lesson.subject,
                        amount = 10,
                        metadata = rewardKey,
                    ))
                }
            }
            val progress = badgeAwarder.recordLessonCompletion(childId, lesson.subject, lesson.id)
            _state.update {
                it.copy(
                    expeditionProgress = progress,
                    badgeAwarded = progress.newlyAwardedBadge,
                )
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
            guideCharacter = "Milo",
            estimatedMinutes = m1.estimatedMinutes,
            languageOfInstruction = m1.language,
            vocabulary = m1.vocabulary,
            steps = m1.activities.map { act -> toActivityStep(act) }
        )
    }
}

/** Maps the on-disk activity type to the versioned renderer key. */
internal fun rendererType(rawType: String): String = when (rawType) {
    "ANIMATED_EXPLANATION" -> "ANIMATED_EXPLANATION_V1"
    "MULTIPLE_CHOICE" -> "MULTIPLE_CHOICE_V1"
    "SORT_AND_CLASSIFY" -> "SORT_AND_CLASSIFY_V1"
    "HOTSPOT_IMAGE" -> "HOTSPOT_IMAGE_V1"
    "MATCHING_PAIRS" -> "MATCHING_PAIRS_V1"
    "SEQUENCE_BUILDER" -> "SEQUENCE_BUILDER_V1"
    "INTERACTIVE_SPEC" -> "INTERACTIVE_SPEC_V1"
    else -> "ANIMATED_EXPLANATION_V1"
}

private fun JsonObject.stringList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull() } ?: emptyList()

private fun JsonPrimitive.contentOrNull(): String? =
    this.content.takeIf { it.isNotBlank() }

/**
 * Converts one Month1Activity into a renderer-ready ActivityStep, parsing the
 * loosely-typed `content` JSON into the typed fields each renderer needs.
 *
 * Parsing is defensive: a malformed payload yields an ActivityStep with empty
 * typed fields rather than throwing, so one bad lesson cannot crash the player.
 */
internal fun toActivityStep(act: Month1Activity): ActivityStep {
    val type = rendererType(act.type)
    val content = act.content
    val obj = content as? JsonObject

    var question = act.instruction
    var options: List<String> = emptyList()
    var correctIndex = -1
    var sortCategories: List<String> = emptyList()
    var sortItems: List<SortItem> = emptyList()
    var matchPairs: List<MatchPair> = emptyList()
    var sequenceSteps: List<String> = emptyList()
    var hotspotExamples: List<String> = emptyList()
    var narration = act.instruction

    runCatching {
        when (act.type) {
            "ANIMATED_EXPLANATION" -> {
                val body = (content as? JsonPrimitive)?.content.orEmpty()
                narration = if (body.isNotBlank()) body else act.instruction
            }

            "HOTSPOT_IMAGE" -> {
                hotspotExamples = obj?.stringList("examples") ?: emptyList()
            }

            "SORT_AND_CLASSIFY" -> {
                val fits = obj?.stringList("fits") ?: emptyList()
                val doesNotFit = obj?.stringList("doesNotFit") ?: emptyList()
                // Category 0 = fits, category 1 = does not fit.
                sortCategories = listOf("Fits the lesson", "Does not fit")
                sortItems = (fits.map { SortItem(it, 0) } + doesNotFit.map { SortItem(it, 1) })
                    .shuffled(java.util.Random(act.activityId.hashCode().toLong()))
            }

            "MULTIPLE_CHOICE" -> {
                options = obj?.stringList("options") ?: emptyList()
                correctIndex = obj?.get("correctIndex")?.jsonPrimitive?.content?.toIntOrNull() ?: -1
                if (correctIndex !in options.indices) correctIndex = -1
            }

            "MATCHING_PAIRS" -> {
                matchPairs = (obj?.get("pairs") as? JsonArray)?.mapNotNull { element ->
                    val p = element.jsonObject
                    val left = p["left"]?.jsonPrimitive?.contentOrNull()
                    val right = p["right"]?.jsonPrimitive?.contentOrNull()
                    if (left != null && right != null) MatchPair(left, right) else null
                } ?: emptyList()
            }

            "SEQUENCE_BUILDER" -> {
                sequenceSteps = obj?.stringList("steps") ?: emptyList()
            }
        }
    }.onFailure {
        android.util.Log.e("LessonVM", "content parse failed for ${act.activityId}: ${it.message}", it)
    }

    return ActivityStep(
        id = act.activityId,
        type = type,
        narrationText = narration,
        question = question,
        options = options,
        correctIndex = correctIndex,
        feedback = ActivityFeedback(
            correct = act.feedback?.correct ?: "Great job!",
            incorrect = act.feedback?.retry ?: "Let's try again!"
        ),
        sortCategories = sortCategories,
        sortItems = sortItems,
        matchPairs = matchPairs,
        sequenceSteps = sequenceSteps,
        hotspotExamples = hotspotExamples,
        completionRule = act.completionRule?.type.orEmpty(),
        completionTargetCount = act.completionRule?.targetCount ?: 0
    )
}
