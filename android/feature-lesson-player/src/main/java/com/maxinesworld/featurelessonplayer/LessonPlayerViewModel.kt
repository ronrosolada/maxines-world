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
    private val masteryEngine: MasteryEngine,
    private val badgeAwarder: BadgeAwarder,
    private val activeContentIndex: ActiveContentIndex
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
            val starsEarned = kotlin.math.ceil(accuracy * 5).toInt().coerceIn(1, 5)
            rewardDao.insert(RewardEntity(id = UUID.randomUUID().toString(), childId = childId,
                type = "STAR", subject = lesson.subject, amount = starsEarned))
            if (accuracy >= 0.8) {
                rewardDao.insert(RewardEntity(id = UUID.randomUUID().toString(), childId = childId,
                    type = "COIN", subject = lesson.subject, amount = 10))
            }
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
        val steps = mutableListOf<ActivityStep>()

        // Story / intro as first explanation step when present.
        if (m1.introduction.isNotBlank()) {
            steps += ActivityStep(
                id = "${m1.lessonId}-intro",
                type = "ANIMATED_EXPLANATION_V1",
                narrationText = m1.introduction,
                question = m1.title
            )
        }

        m1.activities.forEach { act ->
            val extracted = extractActivityContent(act)
            steps += ActivityStep(
                id = act.activityId,
                type = mapActivityType(act.type),
                narrationText = extracted.narration.ifBlank { act.instruction },
                question = extracted.question.ifBlank { act.instruction },
                options = extracted.options,
                correctIndex = extracted.correctIndex,
                feedback = ActivityFeedback(
                    correct = act.feedback?.correct ?: "Great job!",
                    incorrect = act.feedback?.retry ?: "Let's try again!"
                )
            )
        }

        // End-of-lesson assessment items → scored MCQ steps
        m1.assessment?.items?.forEachIndexed { idx, item ->
            val prompt = item.prompt.ifBlank { item.question }.ifBlank { item.narration }
            val optionTexts: List<String>
            val correctIdx: Int
            if (item.choices.isNotEmpty()) {
                optionTexts = item.choices.map { it.text }
                correctIdx = item.choices.indexOfFirst { it.correct }.coerceAtLeast(0)
            } else {
                optionTexts = parseAssessmentOptions(item.options)
                correctIdx = item.correctOptionIds.firstOrNull()?.let { cid ->
                    val ids = parseAssessmentOptionIds(item.options)
                    ids.indexOf(cid).takeIf { it >= 0 }
                        ?: optionTexts.indexOfFirst { it.equals(cid, ignoreCase = true) }
                }?.coerceAtLeast(0) ?: 0
            }
            if (prompt.isBlank() && optionTexts.isEmpty()) return@forEachIndexed
            steps += ActivityStep(
                id = item.itemId.ifBlank { "${m1.lessonId}-q${item.sequence.ifZero(idx + 1)}" },
                type = "MULTIPLE_CHOICE_V1",
                narrationText = prompt,
                question = prompt,
                options = optionTexts,
                correctIndex = correctIdx,
                feedback = ActivityFeedback(
                    correct = item.explanation.ifBlank { "Great job!" },
                    incorrect = item.explanation.ifBlank { "Let's try again!" }
                )
            )
        }

        return LessonManifest(
            id = m1.lessonId, schemaVersion = m1.schemaVersion,
            moduleId = if (m1.month > 0) "g3-m${m1.month.toString().padStart(2, '0')}" else "g3-pack",
            title = m1.title, subject = subj, objective = m1.objective,
            skillIds = listOf(m1.lessonId),
            guideCharacter = "Milo",
            estimatedMinutes = m1.estimatedMinutes,
            languageOfInstruction = m1.language,
            steps = steps
        )
    }

    private data class ExtractedContent(
        val options: List<String> = emptyList(),
        val correctIndex: Int = -1,
        val narration: String = "",
        val question: String = ""
    )

    private fun mapActivityType(type: String): String = when (type) {
        "ANIMATED_EXPLANATION" -> "ANIMATED_EXPLANATION_V1"
        "MULTIPLE_CHOICE" -> "MULTIPLE_CHOICE_V1"
        "SORT_AND_CLASSIFY" -> "SORT_AND_CLASSIFY_V1"
        // Engines not yet implemented — fall back to playable types
        "HOTSPOT_IMAGE", "MATCHING_PAIRS", "SEQUENCE_BUILDER", "INTERACTIVE_SPEC" ->
            "ANIMATED_EXPLANATION_V1"
        else -> "ANIMATED_EXPLANATION_V1"
    }

    private fun Int.ifZero(fallback: Int): Int = if (this == 0) fallback else this

    /**
     * Pull options / correctIndex / narration out of Month1Activity.content.
     * Previously hardcoded empty options, so MCQ rendered blank.
     */
    private fun extractActivityContent(act: Month1Activity): ExtractedContent {
        val content = act.content ?: return ExtractedContent(narration = act.instruction)
        return try {
            when (content) {
                is kotlinx.serialization.json.JsonObject -> {
                    val opts = content["options"]?.let { parseStringList(it) } ?: emptyList()
                    val correct = content["correctIndex"]?.let {
                        (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
                    } ?: -1

                    // Sort: present as ordered list (fits first) so SortStep has something to show
                    val fits = content["fits"]?.let { parseStringList(it) } ?: emptyList()
                    val doesNot = content["doesNotFit"]?.let { parseStringList(it) } ?: emptyList()
                    val sortOptions = if (fits.isNotEmpty() || doesNot.isNotEmpty()) fits + doesNot else emptyList()

                    val steps = content["steps"]?.let { parseStringList(it) } ?: emptyList()
                    val examples = content["examples"]?.let { parseStringList(it) } ?: emptyList()

                    // Prefer MCQ options; for non-MCQ, fold content into narration text
                    val options = when {
                        opts.isNotEmpty() -> opts
                        act.type == "SORT_AND_CLASSIFY" && sortOptions.isNotEmpty() -> sortOptions
                        else -> emptyList()
                    }

                    val extraNarration = when {
                        opts.isNotEmpty() -> act.instruction
                        examples.isNotEmpty() -> (listOf(act.instruction) + examples).joinToString("\n\n")
                        steps.isNotEmpty() -> (listOf(act.instruction) + steps.mapIndexed { i, s -> "${i + 1}. $s" }).joinToString("\n")
                        sortOptions.isNotEmpty() && act.type != "SORT_AND_CLASSIFY" ->
                            (listOf(act.instruction) + sortOptions).joinToString("\n• ", prefix = "\n• ")
                        else -> act.instruction
                    }

                    ExtractedContent(
                        options = options,
                        correctIndex = correct,
                        narration = extraNarration,
                        question = act.instruction
                    )
                }
                is kotlinx.serialization.json.JsonPrimitive ->
                    ExtractedContent(narration = content.content, question = act.instruction)
                else -> ExtractedContent(narration = act.instruction)
            }
        } catch (_: Exception) {
            ExtractedContent(narration = act.instruction)
        }
    }

    private fun parseStringList(el: kotlinx.serialization.json.JsonElement): List<String> {
        val arr = el as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            when (item) {
                is kotlinx.serialization.json.JsonPrimitive -> item.content
                is kotlinx.serialization.json.JsonObject ->
                    item["text"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        ?: item["label"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                else -> null
            }
        }
    }

    private fun parseAssessmentOptions(el: kotlinx.serialization.json.JsonElement?): List<String> {
        if (el == null) return emptyList()
        return parseStringList(el)
    }

    private fun parseAssessmentOptionIds(el: kotlinx.serialization.json.JsonElement?): List<String> {
        val arr = el as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            when (item) {
                is kotlinx.serialization.json.JsonObject ->
                    (item["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                is kotlinx.serialization.json.JsonPrimitive -> item.content
                else -> null
            }
        }
    }
}
