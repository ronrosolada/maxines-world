package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Playroom Collections home from real persisted data.
 *
 * The home exposes subject progress, a forgiving weekly Wildlife Expedition,
 * and collected stickers. GMRC is available from the first session; level
 * progression can later unlock cosmetic spaces without blocking curriculum.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayroomHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: ModuleCatalog,
    private val childProfileDao: ChildProfileDao,
    private val lessonCompletionDao: LessonCompletionDao,
    private val badgeAwarder: BadgeAwarder,
    private val rewardDao: RewardDao,
) : ViewModel() {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    private val _state = MutableStateFlow<PlayroomHomeUiState>(PlayroomHomeUiState.Loading)
    val state: StateFlow<PlayroomHomeUiState> = _state.asStateFlow()

    private var openingSubjectId: String? = null

    init {
        collectState()
    }

    private fun collectState() {
        val profileFlow = childProfileDao.observeById(childId)
        val lessonIdsFlow = lessonCompletionDao.observeDistinctLessonIds(childId)

        viewModelScope.launch {
            combine(profileFlow, lessonIdsFlow) { profile, ids -> profile to ids }
                .collect { (profile, lessonIds) ->
                    val expedition = badgeAwarder.getExpeditionProgress(childId)
                    val badges = badgeAwarder.getCollectedBadges(childId)
                    val stars = rewardDao.getTotalByType(childId, "STAR") ?: 0
                    val coins = rewardDao.getTotalByType(childId, "COIN") ?: 0
                    _state.value = buildContent(profile?.name, lessonIds, expedition, badges, stars, coins)
                }
        }
    }

    fun onSubjectSelected(subjectId: String) {
        if (openingSubjectId != null) return
        val current = _state.value as? PlayroomHomeUiState.Content ?: return
        val subject = current.subjects.firstOrNull { it.id == subjectId } ?: return
        if (!subject.isAvailable) return
        openingSubjectId = subjectId
        _state.value = current.copy(openingSubjectId = subjectId)
    }

    fun onOpenFinished() {
        openingSubjectId = null
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
        expedition: ChallengeProgress,
        badges: List<com.maxinesworld.coremodel.CollectibleBadge>,
        starBalance: Int,
        coinBalance: Int,
    ): PlayroomHomeUiState.Content {
        val completed = lessonIds.toSet()

        // Per-subject progress: completed in subject ÷ total in catalog.
        val subjects = canonicalSubjects.map { subject ->
            val total = runCatching {
                catalog.modulesFor(subject.destination).sumOf { it.lessons.size }
            }.getOrDefault(0)
            val done = completed.count { it.startsWith("${subject.destination}-g3-") }
            val progress = if (total > 0) (done * 100 / total) else null
            subject.copy(
                progressPercent = if (progress == 0) null else progress,
                availability = SubjectAvailability.Available,
                lockReason = null,
            )
        }

        val questTotal = BadgeAwarder.EXPEDITION_TARGET_LESSONS
        val completedCount = expedition.completedCount.coerceIn(0, questTotal)
        val availableFirst = subjects.firstOrNull { it.isAvailable }
        val questUi = if (expedition.expeditionComplete) {
            QuestUi(
                task = "Expedition complete — your wildlife friend is waiting!",
                pawPrintsCompleted = questTotal,
                pawPrintTotal = questTotal,
                isComplete = true,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = "Open Field Guide",
                buttonAction = QuestAction.ViewReward,
            )
        } else {
            QuestUi(
                task = "Complete 3 adventures across 2 learning areas this week.",
                pawPrintsCompleted = completedCount,
                pawPrintTotal = questTotal,
                recommendedSubjectId = availableFirst?.id,
                // A fresh quest with no progress should invite a start, not
                // pretend there is something to continue (#33).
                buttonLabel = if (completedCount == 0) "Start" else "Continue",
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
                .map { badge -> StickerUi(id = badge.id, won = true, emoji = badge.emoji) },
        )

        return PlayroomHomeUiState.Content(
            childName = childName?.takeIf { it.isNotBlank() } ?: "",
            subjects = subjects,
            quest = questUi,
            wildlifeStickers = wildlifeStickers,
            offline = false,
            starBalance = starBalance,
            coinBalance = coinBalance,
        )
    }
}
