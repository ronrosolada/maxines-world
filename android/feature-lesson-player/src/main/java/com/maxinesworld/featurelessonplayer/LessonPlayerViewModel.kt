package com.maxinesworld.featurelessonplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.coremodel.*
import com.maxinesworld.coremodel.gamification.*
import com.maxinesworld.coredatabase.*
import com.maxinesworld.corecontent.ActiveContentIndex
import com.maxinesworld.corecontent.ContentLessonLoader
import com.maxinesworld.corecontent.LessonLoader
import com.maxinesworld.enginemastery.MasteryEngine
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.playground.DailyQuestSeedPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class UnsupportedActivityTypeException(lessonId: String, activityId: String, rawType: String) :
    IllegalStateException("Unsupported activity type in lesson $lessonId / $activityId: \"$rawType\"")


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
    private val db: MaxinesDatabase,
    private val lessonLoader: LessonLoader,
    private val progressEventDao: ProgressEventDao,
    private val masteryRecordDao: MasteryRecordDao,
    private val rewardDao: RewardDao,
    private val rewardLedgerDao: RewardLedgerDao,
    private val lessonCompletionDao: LessonCompletionDao,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val playgroundUnlockReceiptDao: PlaygroundUnlockReceiptDao,
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

            // Fish treat reward with deterministic source key — written to reward_ledger (authoritative balance)
                        val improved = scoredResults.any { it.attempts > 1 && it.correct }
                        val crossed = accuracy >= 0.8
                        val treats = FishTreatPolicy.amount(completed = true, improvedAfterRetry = improved, crossedMasteryThreshold = crossed)
                        val rewardKey = FishTreatPolicy.rewardKey(childId, lesson.id, attemptId)
                        rewardLedgerDao.insertIgnoring(RewardLedgerEntity(
                            id = deterministicUuid("${completionId}:reward:fish_treat"),
                            childId = childId,
                            amount = treats,
                            sourceKey = rewardKey,
                            occurredAtEpochMillis = System.currentTimeMillis()
                        ))

            val progress = badgeAwarder.recordSubjectCompletion(childId, lesson.subject)
            if (progress.newlyAwardedBadge != null) {
                _state.update { it.copy(badgeAwarded = progress.newlyAwardedBadge) }
            }

            // ─── Playground Gate: Quest Seeding, Completion, Unlock ───
            val dayKey = LocalDate.now().toString()
            val questId = subjectToQuestId(lesson.subject) ?: return@launch
            val seededIds = DailyQuestSeedPolicy.assign(childId, dayKey)

            dailyQuestSetDao.insertIgnoring(
                DailyQuestSetEntity(
                    id = deterministicUuid("dqs:${childId}:${dayKey}"),
                    childId = childId,
                    dayKey = dayKey,
                    assignedQuestIds = toJsonArrayString(seededIds),
                    assignedAtEpochMillis = System.currentTimeMillis()
                )
            )

            val persistedSet = dailyQuestSetDao.getByChildAndDay(childId, dayKey)
                ?: return@launch
            val assigned = fromJsonArrayString(persistedSet.assignedQuestIds).toSet()

            if (assigned.isNotEmpty() && questId in assigned) {
                dailyQuestCompletionDao.insertIgnoring(
                    DailyQuestCompletionEntity(
                        id = deterministicUuid("dqc:${childId}:${dayKey}:${questId}"),
                        childId = childId,
                        dayKey = dayKey,
                        questId = questId,
                        completionEventId = completionId,
                        completedAtEpochMillis = System.currentTimeMillis()
                    )
                )

                val completed = dailyQuestCompletionDao
                    .getCompletedQuestIds(childId, dayKey)
                    .toSet()
                    .intersect(assigned)

                if (completed.containsAll(assigned)) {
                    playgroundUnlockReceiptDao.insertIgnoring(
                        PlaygroundUnlockReceiptEntity(
                            id = deterministicUuid("pur:${childId}:${dayKey}"),
                            childId = childId,
                            dayKey = dayKey,
                            sourceQuestSetHash = DailyQuestSeedPolicy.questSetHash(assigned),
                            unlockedAtEpochMillis = System.currentTimeMillis()
                        )
                    )
                }
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
        val wildlifeMeta = m1.wildlifeDiscovery?.let { wd ->
            com.maxinesworld.coremodel.gamification.WildlifeDiscoveryMetadata(
                badgeId = wd.badgeId,
                trigger = wd.trigger,
                factActivityId = wd.factActivityId
            )
        }
        return LessonManifest(
            id = m1.lessonId, schemaVersion = m1.schemaVersion,
            moduleId = "g3-m01",
            title = m1.title, subject = subj, objective = m1.objective,
            skillIds = listOf(m1.lessonId),
            guideCharacter = if (subj == "english") "Mira" else "Milo",
            estimatedMinutes = m1.estimatedMinutes,
            languageOfInstruction = m1.language,
            wildlifeDiscovery = wildlifeMeta,
            steps = m1.activities.map { act ->
                val canonical = canonicalActivityType(act.type)
                    ?: throw UnsupportedActivityTypeException(m1.lessonId, act.activityId, act.type)
                val (options, correctIndex) = extractOptionsAndCorrectIndex(act)
                ActivityStep(
                    id = act.activityId,
                    type = canonical,
                    narrationText = act.instruction,
                    question = act.instruction,
                    options = options,
                    correctIndex = correctIndex,
                    feedback = ActivityFeedback(
                        correct = act.feedback?.correct ?: "Great job!",
                        incorrect = act.feedback?.retry ?: "Let's try again!"
                    )
                )
            }
        )
    }

    /**
     * Map Month1 activity JSON `content` into ActivityStep options/correctIndex.
     * Without this, renderers fall back to A/B/C/D with correctIndex=-1 and
     * lessons cannot be scored or completed.
     */
    private fun extractOptionsAndCorrectIndex(act: Month1Activity): Pair<List<String>, Int> {
        val el = act.content ?: return emptyList<String>() to -1
        return try {
            val obj = el as? kotlinx.serialization.json.JsonObject
                ?: return emptyList<String>() to -1
            when (act.type.uppercase()) {
                "MULTIPLE_CHOICE" -> {
                    val optionsEl = obj["options"]
                    val options = when (optionsEl) {
                        is kotlinx.serialization.json.JsonArray -> optionsEl.mapNotNull { item ->
                            when (item) {
                                is kotlinx.serialization.json.JsonPrimitive -> item.content
                                is kotlinx.serialization.json.JsonObject ->
                                    item["text"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                        ?: item["label"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                else -> null
                            }
                        }
                        else -> emptyList()
                    }
                    val correctIndex = obj["correctIndex"]
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
                        ?: -1
                    options to correctIndex
                }
                "HOTSPOT_IMAGE" -> {
                    val examples = (obj["examples"] as? kotlinx.serialization.json.JsonArray)
                        ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        ?: emptyList()
                    if (examples.isNotEmpty()) {
                        examples.mapIndexed { i, _ -> "Region ${i + 1}" } to 0
                    } else emptyList<String>() to 0
                }
                "SORT_AND_CLASSIFY" -> {
                                    val fits = (obj["fits"] as? kotlinx.serialization.json.JsonArray)
                                        ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                        ?: emptyList()
                                    val doesNot = (obj["doesNotFit"] as? kotlinx.serialization.json.JsonArray)
                                        ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                        ?: emptyList()
                                    // categories first, then Fits items, then Does-not items.
                                    // correctIndex = count of items belonging to category 0 (Fits).
                                    (listOf("Fits", "Does not fit") + fits + doesNot) to fits.size
                                }
                "MATCHING_PAIRS" -> {
                                    val pairs = obj["pairs"] as? kotlinx.serialization.json.JsonArray
                                    val lefts = mutableListOf<String>()
                                    val rights = mutableListOf<String>()
                                    pairs?.forEach { p ->
                                        val o = p as? kotlinx.serialization.json.JsonObject ?: return@forEach
                                        val left = (o["left"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                            ?: (o["a"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                        val right = (o["right"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                            ?: (o["b"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                        if (left != null) lefts += left
                                        if (right != null) rights += right
                                    }
                                    // MatchingPairsRenderer splits options as left = first half, right = second half
                                    // and treats same index as a correct pair.
                                    (lefts + rights) to -1
                                }
                "SEQUENCE_BUILDER" -> {
                    val steps = (obj["steps"] as? kotlinx.serialization.json.JsonArray)
                        ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        ?: emptyList()
                    steps to -1
                }
                else -> emptyList<String>() to -1
            }
        } catch (_: Exception) {
            emptyList<String>() to -1
        }
    }

    companion object {
        fun subjectToQuestId(subject: String): String? = when (subject) {
            "english", "Reading", "Story Tree" -> "subject:english"
            "filipino", "Filipino" -> "subject:filipino"
            "mathematics", "Mathematics", "maths" -> "subject:mathematics"
            "science", "Science" -> "subject:science"
            "makabansa", "Araling Panlipunan", "history" -> "subject:makabansa"
            "gmrc", "Values", "Kindness" -> "subject:gmrc"
            else -> null
        }

        fun deterministicUuid(seed: String): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(seed.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }.take(32)
        }

        fun toJsonArrayString(ids: List<String>): String =
            ids.joinToString(",", "[", "]") { "\"$it\"" }

        fun fromJsonArrayString(json: String): List<String> {
            return try {
                val trimmed = json.trim()
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    trimmed.substring(1, trimmed.length - 1)
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotBlank() }
                } else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
