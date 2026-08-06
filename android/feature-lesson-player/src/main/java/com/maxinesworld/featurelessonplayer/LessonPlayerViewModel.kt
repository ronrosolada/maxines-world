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
import com.maxinesworld.corecontent.ActiveContentIndex
import com.maxinesworld.corecontent.ContentLessonLoader
import com.maxinesworld.corecontent.LessonLoader
import com.maxinesworld.corecontent.friendlyLessonTitleOf
import com.maxinesworld.coredatabase.RewardBreakDao
import com.maxinesworld.coredatabase.RewardBreakPolicy
import com.maxinesworld.engineassessment.Scorer
import com.maxinesworld.featurerewards.ChallengeProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class LessonUiState(
    val isLoading: Boolean = true,
    val lesson: LessonManifest? = null,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    /** Number of steps that belong to the final knowledge check (ASSESSMENT_V1).
     *  Practice phase = steps [0, totalSteps - assessmentStepCount). */
    val assessmentStepCount: Int = 0,
    val showFeedback: Boolean = false,
    val feedbackText: String = "",
    val feedbackCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val assessmentFailed: Boolean = false,
    val error: String? = null,
    val results: List<ActivityResult> = emptyList(),
    val badgeAwarded: CollectibleBadge? = null,
    val expeditionProgress: ChallengeProgress = ChallengeProgress(),
    val rewardBreakId: String? = null,
)

@HiltViewModel
class LessonPlayerViewModel @Inject constructor(
    application: Application,
    private val lessonLoader: LessonLoader,
    private val scorer: Scorer,
    private val activeContentIndex: ActiveContentIndex,
    private val lessonCompletionRepository: LessonCompletionRepository,
    private val rewardBreakDao: RewardBreakDao,
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
                    assessmentStepCount = lesson?.assessment?.items?.size ?: 0,
                    // Fresh lesson: never carry a prior lesson's badge reveal or
                    // reward-break entitlement into the new run.
                    badgeAwarded = null,
                    rewardBreakId = null,
                    error = if (lesson == null) "Could not load lesson." else null)
            }
        }
    }

    fun onNextStep() {
        val snapshot = _state.value
        val next = snapshot.currentStep + 1
        val lesson = snapshot.lesson
        val decision = lesson?.let {
            evaluateLessonCompletion(it, snapshot.results, next, scorer)
        }
        val completed = decision?.complete == true
        val assessmentFailed = decision?.assessmentFailed == true
        _state.update {
            when {
                completed -> it.copy(
                    currentStep = next,
                    isComplete = true,
                    assessmentFailed = false,
                    showFeedback = false,
                )
                assessmentFailed -> it.copy(
                    currentStep = next.coerceAtMost(it.totalSteps),
                    isComplete = false,
                    assessmentFailed = true,
                    showFeedback = false,
                )
                else -> it.copy(
                    currentStep = next,
                    showFeedback = false,
                )
            }
        }
        if (completed) saveProgress()
    }

    /** Restart only the authored assessment; practice results remain visible. */
    fun retryAssessment() {
        val lesson = _state.value.lesson ?: return
        val assessmentIds = lesson.assessment?.items?.map { it.id }?.toSet().orEmpty()
        if (assessmentIds.isEmpty()) return
        val assessmentStart = lesson.steps.indexOfFirst { it.id in assessmentIds }
        if (assessmentStart < 0) return
        _state.update {
            it.copy(
                currentStep = assessmentStart,
                results = it.results.filterNot { result -> result.activityId in assessmentIds },
                assessmentFailed = false,
                showFeedback = false,
            )
        }
    }

    private fun saveProgress() {
        if (progressSaved) return
        progressSaved = true
        val lesson = _state.value.lesson ?: return
        val scoredResults = _state.value.results.filter { it.scored }
        if (childId.isBlank() || scoredResults.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                lessonCompletionRepository.complete(childId, lesson, scoredResults)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        expeditionProgress = result.expeditionProgress,
                        badgeAwarded = result.badgeAwarded,
                    )
                }
                // One idempotent reward break entitlement per child and local day.
                // Creation starts in CREATED; the hub starts the clock only when
                // the child chooses a game.
                val now = System.currentTimeMillis()
                val dayKey = LocalDate.now(ZoneId.systemDefault()).toString()
                val dailyQuestCompletionId = RewardBreakPolicy.dailyQuestCompletionId(childId, dayKey)
                val existingBreak = rewardBreakDao.getByQuestCompletion(dailyQuestCompletionId)
                val rewardBreak = if (existingBreak == null) {
                    val created = RewardBreakPolicy.newEntitlement(
                        id = UUID.randomUUID().toString(),
                        childId = childId,
                        dailyQuestCompletionId = dailyQuestCompletionId,
                        nowEpochMillis = now,
                    )
                    rewardBreakDao.insertIgnoring(created)
                    rewardBreakDao.getByQuestCompletion(dailyQuestCompletionId)
                } else {
                    existingBreak
                }
                val usableBreakId = rewardBreak
                    ?.takeIf { RewardBreakPolicy.canUse(it, now) }
                    ?.id

                _state.update { it.copy(rewardBreakId = usableBreakId) }
            }.onFailure { error ->
                // Allow a process/UI retry after a rolled-back transaction.
                progressSaved = false
                _state.update { it.copy(error = "Could not save lesson progress: ${error.message}") }
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
        // The authored knowledge check becomes a distinct playable phase after
        // the practice activities (review: assessments were authored but never
        // delivered — completion used to end after the 6 activities).
        val assessmentSteps = m1.assessment?.items.orEmpty().map { toAssessmentStep(it) }
        return LessonManifest(
            id = m1.lessonId, schemaVersion = m1.schemaVersion,
            moduleId = "g3-m01",
            title = friendlyLessonTitleOf(m1.title), subject = subj, objective = m1.objective,
            skillIds = listOf(m1.lessonId),
            guideCharacter = "Milo",
            estimatedMinutes = m1.estimatedMinutes,
            languageOfInstruction = m1.language,
            vocabulary = m1.vocabulary,
            assessment = m1.assessment?.let { a ->
                AssessmentBlock(
                    passThreshold = if (a.itemCount > 0) a.passingCorrectCount.toDouble() / a.itemCount else 0.8,
                    minQuestions = a.itemCount,
                    items = assessmentSteps,
                )
            },
            steps = m1.activities.map { act -> toActivityStep(act, m1.language) } + assessmentSteps
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

/**
 * Converts one authored assessment item into a playable MCQ step.
 *
 * The on-disk format stores options as [{id, text}] with correctOptionIds
 * pointing at option ids; the renderer needs a plain list plus an index.
 * Feedback uses the authored explanation for both outcomes so a wrong
 * answer receives the corrective rationale.
 */
internal fun toAssessmentStep(item: AssessmentItem): ActivityStep {
    val optionJson = item.options as? JsonArray
    val optionIds = optionJson?.mapNotNull { el ->
        (el as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull()
    } ?: emptyList()
    val optionTexts = optionJson?.mapNotNull { el ->
        (el as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull()
    } ?: emptyList()
    val key = item.correctOptionIds.firstOrNull()
    val correctIndex = if (key != null) optionIds.indexOf(key) else -1
    return ActivityStep(
        id = "assessment-${item.itemId.ifBlank { "q${item.sequence}" }}",
        type = "ASSESSMENT_V1",
        question = item.prompt,
        options = optionTexts,
        correctIndex = correctIndex,
        feedback = ActivityFeedback(
            correct = item.explanation.ifBlank { "Great job! 🎉" },
            incorrect = item.explanation.ifBlank { "Let's try again! 💪" },
        ),
    )
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
internal fun toActivityStep(act: Month1Activity, language: String? = null): ActivityStep {
    val type = rendererType(act.type)
    val content = act.content
    val obj = content as? JsonObject

    val childFacingInstruction = act.instruction
        .replace("does not show the skill", "false")
        .replace("shows the skill", "true")

    var question = childFacingInstruction
    var options: List<String> = emptyList()
    var correctIndex = -1
    var sortCategories: List<String> = emptyList()
    var sortItems: List<SortItem> = emptyList()
    var matchPairs: List<MatchPair> = emptyList()
    var sequenceSteps: List<String> = emptyList()
    var hotspotExamples: List<String> = emptyList()
    var narration = childFacingInstruction

    // Curriculum jargon in authored instructions ("shows the skill") is
    // opaque to a 7-year-old. The same child-facing wording is used for
    // narration and the activity question so the lesson shell cannot render
    // two versions of one instruction.
    // Bucket labels must follow the language of the authored lesson. English
    // True/False is not an acceptable fallback for Filipino content.

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
                val authoredCategories = obj?.stringList("categories")
                    ?.takeIf { it.size >= 2 }
                val filipino = language?.startsWith("fil", ignoreCase = true) == true ||
                    act.activityId.startsWith("filipino-", ignoreCase = true) ||
                    act.instruction.contains("angkop", ignoreCase = true)
                sortCategories = authoredCategories ?: if (filipino) {
                    listOf("Angkop", "Hindi angkop")
                } else {
                    listOf("Fits", "Does not fit")
                }
                val sortInstruction = sortInstructionWithCategories(childFacingInstruction, sortCategories)
                question = sortInstruction
                narration = sortInstruction
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
                    if (left != null && right != null) {
                        // Same kid-friendly treatment as sort buckets (#36):
                        // "shows the skill" → "true" / "does not show the skill" → "false".
                        MatchPair(left, right.replace("does not show the skill", "false").replace("shows the skill", "true"))
                    } else null
                } ?: emptyList()
            }

            "SEQUENCE_BUILDER" -> {
                sequenceSteps = obj?.stringList("steps") ?: emptyList()
            }
        }
    }.onFailure {
        android.util.Log.e("LessonVM", "content parse failed for ${act.activityId}: ${it.message}", it)
    }

    // Hints may be authored either as a top-level activity field or inside
    // the typed content object. Preserve them so the renderer can expose a
    // real help affordance instead of a button that silently does nothing.
    val hintText = obj?.get("hint")?.jsonPrimitive?.contentOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: act.hint.orEmpty()

    val filipino = language?.startsWith("fil", ignoreCase = true) == true ||
        act.activityId.startsWith("filipino-", ignoreCase = true) ||
        act.instruction.contains("angkop", ignoreCase = true)
    val defaultIncorrect = if (type == "SORT_AND_CLASSIFY_V1") {
        if (filipino) "May ilang card sa maling kahon. Ilipat at subukan muli."
        else "Some cards are in the wrong box. Move them and try again."
    } else {
        "Let's try again!"
    }

    val authoredIncorrect = act.feedback?.retry?.takeIf { it.isNotBlank() }
    val incorrectFeedback = if (type == "SORT_AND_CLASSIFY_V1") {
        listOfNotNull(authoredIncorrect, defaultIncorrect).joinToString(" ")
    } else {
        authoredIncorrect ?: defaultIncorrect
    }

    return ActivityStep(
        id = act.activityId,
        type = type,
        narrationText = narration,
        question = question,
        options = options,
        correctIndex = correctIndex,
        imageAssets = listOfNotNull(act.assetId?.takeIf { it.isNotBlank() }),
        feedback = ActivityFeedback(
            correct = act.feedback?.correct?.takeIf { it.isNotBlank() } ?: "Great job!",
            incorrect = incorrectFeedback,
        ),
        sortCategories = sortCategories,
        sortItems = sortItems,
        matchPairs = matchPairs,
        sequenceSteps = sequenceSteps,
        hotspotExamples = hotspotExamples,
        completionRule = act.completionRule?.type.orEmpty(),
        completionTargetCount = act.completionRule?.targetCount ?: 0,
        hintText = hintText
    )
}

/** Keep the sort prompt and its visible category labels in the same vocabulary. */
internal fun sortInstructionWithCategories(instruction: String, categories: List<String>): String {
    if (categories.size < 2) return instruction

    return instruction
        .replace("true", categories[0], ignoreCase = true)
        .replace("false", categories[1], ignoreCase = true)
}
