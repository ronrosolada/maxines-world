package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import com.maxinesworld.featurerewards.TreatShopCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    private val inventoryDao: InventoryDao,
    private val dailyQuestManager: DailyQuestManager,
) : ViewModel() {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    private val _state = MutableStateFlow<PlayroomHomeUiState>(PlayroomHomeUiState.Loading)
    val state: StateFlow<PlayroomHomeUiState> = _state.asStateFlow()

    private var openingSubjectId: String? = null
    private var stateJob: Job? = null

    init {
        collectState()
    }

    private fun collectState() {
        stateJob?.cancel()
        val profileFlow = childProfileDao.observeById(childId)
        val lessonIdsFlow = lessonCompletionDao.observeDistinctLessonIds(childId)

        stateJob = viewModelScope.launch {
            try {
                combine(profileFlow, lessonIdsFlow) { profile, ids -> profile to ids }
                    .collect { (profile, lessonIds) ->
                        val dailyQuest = dailyQuestManager.ensureToday(
                            childId = childId,
                            completedLessonIds = lessonIds,
                        )
                        val badges = badgeAwarder.getCollectedBadges(childId)
                        val stars = rewardDao.getTotalByType(childId, "STAR") ?: 0
                        val coins = rewardDao.getTotalByType(childId, "COIN") ?: 0
                        val keepsakes = inventoryDao.getOwnedItemIds(childId)
                            .mapNotNull { id -> TreatShopCatalog.byId(id) }
                            .map { item -> KeepsakeUi(item.id, item.emoji, item.name) }
                        _state.value = buildContent(
                            profile?.name, lessonIds, dailyQuest, badges,
                            stars, coins, keepsakes,
                        )
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = PlayroomHomeUiState.Error(
                    message = "We couldn't load your Playroom. Try again."
                )
            }
        }
    }

    /**
     * @return true when navigation should proceed. The route must only
     * navigate on true — the guard here prevents rapid double-taps from
     * stacking duplicate module screens (audit AC12, 2026-08-06).
     */
    fun onSubjectSelected(subjectId: String): Boolean {
        if (openingSubjectId != null) return false
        val current = _state.value as? PlayroomHomeUiState.Content ?: return false
        val subject = current.subjects.firstOrNull { it.id == subjectId } ?: return false
        if (!subject.isAvailable) return false
        openingSubjectId = subjectId
        _state.value = current.copy(openingSubjectId = subjectId)
        return true
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
        dailyQuest: DailyQuestProgress,
        badges: List<com.maxinesworld.coremodel.CollectibleBadge>,
        starBalance: Int,
        coinBalance: Int,
        keepsakes: List<KeepsakeUi>,
    ): PlayroomHomeUiState.Content {
        val completed = lessonIds.toSet()

        // Per-subject progress: completed in subject ÷ total in catalog.
        // Makabansa's collection includes the legacy AP lessons, so its
        // progress counts both lesson prefixes (audit A1, 2026-08-06).
        val subjects = canonicalSubjects.map { subject ->
            val total = runCatching {
                catalog.modulesFor(subject.destination).sumOf { it.lessons.size }
            }.getOrDefault(0)
            val done = completed.count {
                it.startsWith("${subject.destination}-g3-") ||
                    (subject.id == "makabansa" && it.startsWith("araling-panlipunan-g3-"))
            }
            val progress = if (total > 0) (done * 100 / total) else null
            subject.copy(
                progressPercent = if (progress == 0) null else progress,
                availability = SubjectAvailability.Available,
                lockReason = null,
            )
        }

        val questTotal = dailyQuest.totalCount.coerceAtLeast(1)
        val completedCount = dailyQuest.completedCount.coerceIn(0, questTotal)
        val availableFirst = subjects.firstOrNull { it.isAvailable }
        val targets = QuestTargetResolver.resolve(
            assigned = dailyQuest.assignedQuestIds,
            completed = completed,
            catalog = catalog,
        )
        val nextLessonId = targets.firstOrNull { !it.isCompleted }?.lessonId
            ?: targets.firstOrNull()?.lessonId
        // No available subject → honest "Choose a subject" fallback that moves
        // focus to the grid instead of a dead Continue (audit AC28, 2026-08-06).
        val noSubjectFallback = availableFirst == null || (targets.isNotEmpty() && nextLessonId == null)
        val questUi = if (dailyQuest.isComplete) {
            QuestUi(
                task = "Today's quest complete — your wildlife friend is waiting!",
                pawPrintsCompleted = questTotal,
                pawPrintTotal = questTotal,
                isComplete = true,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = "Open Field Guide",
                buttonAction = QuestAction.ViewReward,
                targets = targets,
                nextLessonId = nextLessonId,
            )
        } else {
            val hasQuestTarget = nextLessonId != null
            QuestUi(
                task = "Complete $questTotal learning adventures today.",
                pawPrintsCompleted = completedCount,
                pawPrintTotal = questTotal,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = when {
                    noSubjectFallback -> "Choose a subject"
                    hasQuestTarget && completedCount == 0 -> "Start quest"
                    hasQuestTarget -> "Continue quest"
                    completedCount == 0 -> "Start"
                    else -> "Continue"
                },
                buttonAction = when {
                    noSubjectFallback -> QuestAction.ChooseSubject
                    hasQuestTarget -> QuestAction.OpenLesson
                    else -> QuestAction.Continue
                },
                targets = targets,
                nextLessonId = nextLessonId,
            )
        }

        val collected = badges.count { it.isCollected }
        val wildlifeStickers = WildlifeStickersUi(
            collectedCount = collected,
            totalCount = badges.size,
            stickers = badges
                .filter { it.isCollected }
                .sortedByDescending { it.collectedAtEpochMillis }
                .map { badge -> StickerUi(id = badge.id, won = true) },
        )

        return PlayroomHomeUiState.Content(
            childName = childName?.takeIf { it.isNotBlank() } ?: "",
            subjects = subjects,
            quest = questUi,
            wildlifeStickers = wildlifeStickers,
            offline = false,
            starBalance = starBalance,
            coinBalance = coinBalance,
            ownedKeepsakes = keepsakes,
        )
    }
}
