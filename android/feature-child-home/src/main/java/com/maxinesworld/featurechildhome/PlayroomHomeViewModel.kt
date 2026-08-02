package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coremodel.ChildLevelPolicy
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Drives the Option 3 Playroom Collections home from real persisted data:
 *
 * * Per-subject progress = distinct completed lessons ÷ lessons in the
 *   bundled catalog for that subject (ModuleCatalog).
 * * Streak = consecutive days (ending today or yesterday) with at least one
 *   completed lesson, from real completion timestamps.
 * * XP = completed lessons × [XP_PER_LESSON] — a deterministic product rule
 *   over real completions; never a fabricated number.
 * * Today's Quest + sticker book come from [BadgeAwarder] (real daily
 *   challenge and collected badges).
 * * GMRC (Kindness) stays a REAL gate: unlocked only at Level 4.
 *
 * No child-sensitive data is ever logged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayroomHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: ModuleCatalog,
    private val childProfileDao: ChildProfileDao,
    private val lessonCompletionDao: LessonCompletionDao,
    private val badgeAwarder: BadgeAwarder,
) : ViewModel() {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    private val _state = MutableStateFlow<PlayroomHomeUiState>(PlayroomHomeUiState.Loading)
    val state: StateFlow<PlayroomHomeUiState> = _state.asStateFlow()

    /** Opening-subject state lives in the UI state; navigation is one-shot. */
    private var _openingSubjectId: String? = null

    init {
        collectState()
    }

    private fun collectState() {
        val profileFlow = childProfileDao.observeById(childId)
        val lessonIdsFlow = lessonCompletionDao.observeDistinctLessonIds(childId)
        val daysFlow = lessonCompletionDao.observeCompletionDays(childId)

        viewModelScope.launch {
            combine(profileFlow, lessonIdsFlow, daysFlow) { profile, ids, days ->
                Triple(profile, ids, days)
            }
                .flatMapLatest { (profile, lessonIds, days) ->
                    val quest = badgeAwarder.getTodayProgress(childId)
                    val badges = badgeAwarder.getCollectedBadges(childId)
                    flowOf(buildContent(profile?.name, lessonIds, days, quest, badges))
                }
                .collect { _state.value = it }
        }
    }

    /** Atomic single-open guard: rejects repeated activation while opening. */
    fun onSubjectSelected(subjectId: String) {
        if (_openingSubjectId != null) return
        val current = _state.value as? PlayroomHomeUiState.Content ?: return
        val subject = current.subjects.firstOrNull { it.id == subjectId } ?: return
        if (!subject.isAvailable) return
        _openingSubjectId = subjectId
        _state.value = current.copy(openingSubjectId = subjectId)
    }

    fun onOpenFinished() {
        _openingSubjectId = null
        val current = _state.value as? PlayroomHomeUiState.Content ?: return
        _state.value = current.copy(openingSubjectId = null)
    }

    fun retry() {
        _state.value = PlayroomHomeUiState.Loading
        collectState()
    }

    private suspend fun buildContent(
        childName: String?,
        lessonIds: List<String>,
        days: List<String>,
        quest: ChallengeProgress,
        badges: List<CollectibleBadge>,
    ): PlayroomHomeUiState.Content {
        val completed = lessonIds.toSet()
        val level = ChildLevelPolicy.levelFor(completed.size)
        val kindnessUnlocked = level >= ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        val lessonsToGo = ChildLevelPolicy.lessonsRemainingTo(
            completed.size, ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        )

        // Per-subject progress: completed in subject ÷ total in catalog.
        val subjects = canonicalSubjects.map { subject ->
            val total = runCatching {
                catalog.modulesFor(subject.destination).sumOf { it.lessons.size }
            }.getOrDefault(0)
            val done = completed.count { it.startsWith("${subject.destination}-g3-") }
            val progress = if (total > 0) (done * 100 / total) else null

            val (availability, lockReason) = if (subject.id == "gmrc" && !kindnessUnlocked) {
                SubjectAvailability.Locked to
                    "Locked until level $KINDNESS_UNLOCK_LEVEL · $lessonsToGo lesson${if (lessonsToGo == 1) "" else "s"} to go"
            } else SubjectAvailability.Available to null

            subject.copy(
                progressPercent = if (progress == 0) null else progress,
                availability = availability,
                lockReason = lockReason,
            )
        }

        // Quest: daily challenge paw prints (real). Total = the five challenge
        // subjects; if none recorded yet, fall back to “choose a subject”.
        val questTotal = BadgeAwarder.SUBJECTS.size
        val completedToday = quest.completedCount.coerceIn(0, questTotal)
        val availableFirst = subjects.firstOrNull { it.isAvailable }
        val questUi = if (questTotal == 0) {
            QuestUi(
                task = "Choose any subject to begin today",
                pawPrintsCompleted = 0, pawPrintTotal = 0,
                recommendedSubjectId = null,
                buttonLabel = "Choose a subject",
                buttonAction = QuestAction.ChooseSubject,
            )
        } else if (completedToday >= questTotal) {
            QuestUi(
                task = "Quest complete!",
                pawPrintsCompleted = questTotal, pawPrintTotal = questTotal,
                isComplete = true,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = "View reward",
                buttonAction = QuestAction.ViewReward,
            )
        } else {
            QuestUi(
                task = "Complete one activity to earn today’s paw print.",
                pawPrintsCompleted = completedToday, pawPrintTotal = questTotal,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = "Continue",
                buttonAction = QuestAction.Continue,
            )
        }

        val collected = badges.count { it.isCollected }
        val wildlifeStickers = WildlifeStickersUi(
            collectedCount = collected,
            totalCount = badges.size,
            stickers = badges
                .filter { it.isCollected }
                .sortedByDescending { it.collectedAtEpochMillis }
                .map { b -> StickerUi(id = b.id, won = true, emoji = b.emoji) },
        )

        return PlayroomHomeUiState.Content(
            childName = childName?.takeIf { it.isNotBlank() } ?: "",
            streakDays = computeStreak(days),
            xp = completed.size * XP_PER_LESSON,
            subjects = subjects,
            quest = questUi,
            wildlifeStickers = wildlifeStickers,
            offline = false, // bundled pack is offline-first by design
        )
    }

    /** Consecutive days with completions, ending today or yesterday. */
    internal fun computeStreak(days: List<String>): Int {
        if (days.isEmpty()) return 0
        val daySet = days.toSet()
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        var cursor = LocalDate.now()
        if (cursor.format(fmt) !in daySet) {
            cursor = cursor.minusDays(1) // allow streak ending yesterday
        }
        var streak = 0
        while (cursor.format(fmt) in daySet) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    companion object {
        const val KINDNESS_UNLOCK_LEVEL = ChildLevelPolicy.KINDNESS_UNLOCK_LEVEL
        const val XP_PER_LESSON = 10
    }
}
