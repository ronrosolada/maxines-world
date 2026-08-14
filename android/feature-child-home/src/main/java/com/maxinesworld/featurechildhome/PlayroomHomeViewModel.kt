package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.GodModeManager
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import com.maxinesworld.featurerewards.SanctuaryCatalog
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
    private val godModeManager: GodModeManager,
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
                combine(profileFlow, lessonIdsFlow, godModeManager.enabled) { profile, ids, godModeEnabled ->
                    Triple(profile, ids, godModeEnabled)
                }.collect { (profile, lessonIds, godModeEnabled) ->
                        val dailyQuest = dailyQuestManager.ensureToday(
                            childId = childId,
                            completedLessonIds = lessonIds,
                        )
                        val badges = badgeAwarder.getCollectedBadges(childId).let { loaded ->
                            if (godModeEnabled) loaded.map { badge -> badge.copy(isCollected = true) } else loaded
                        }
                        val stars = rewardDao.getTotalByType(childId, "STAR") ?: 0
                        val coins = rewardDao.getTotalByType(childId, "COIN") ?: 0
                        val sanctuaryRewards = rewardDao.getByChildAndType(
                            childId,
                            DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE,
                        )
                        val sanctuaryPieces = sanctuaryRewards
                            .mapNotNull { reward -> SanctuaryCatalog.byId(reward.metadata) }
                            .distinctBy { piece -> piece.id }
                        val sanctuaryCount = sanctuaryRewards.sumOf { reward -> reward.amount }.coerceAtLeast(0)
                        val sanctuary = SanctuaryUi(
                            earnedPieces = sanctuaryCount,
                            visiblePieces = sanctuaryPieces.map { piece ->
                                SanctuaryPieceUi(piece.id, piece.name, piece.description, piece.iconKey)
                            },
                            nextPiece = SanctuaryCatalog.pieces
                                .getOrNull(sanctuaryCount)
                                ?.let { piece -> SanctuaryPieceUi(piece.id, piece.name, piece.description, piece.iconKey) },
                            totalPieces = SanctuaryCatalog.pieces.size,
                        )
                        val keepsakes = inventoryDao.getOwnedItemIds(childId)
                            .mapNotNull { id -> TreatShopCatalog.byId(id) }
                            .map { item -> KeepsakeUi(item.id, item.name, item.iconKey) }
                        val visibleKeepsakes = if (godModeEnabled) {
                            TreatShopCatalog.items.map { item -> KeepsakeUi(item.id, item.name, item.iconKey) }
                        } else {
                            keepsakes
                        }
                        _state.value = buildContent(
                            profile?.name, lessonIds, dailyQuest, badges,
                            stars, coins, visibleKeepsakes, sanctuary, godModeEnabled,
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
        sanctuary: SanctuaryUi,
        godModeEnabled: Boolean,
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

        val availableFirst = subjects.firstOrNull { it.isAvailable }
        val targets = QuestTargetResolver.resolve(
            assigned = dailyQuest.assignedQuestIds,
            completed = completed,
            catalog = catalog,
        )
        val questTotal = targets.size.coerceAtLeast(1)
        val completedCount = dailyQuest.completedCount.coerceIn(0, questTotal)
        val sanctuaryComplete = sanctuary.earnedPieces >= SanctuaryCatalog.pieces.size
        val nextLessonId = targets.firstOrNull { !it.isCompleted }?.lessonId
            ?: targets.firstOrNull()?.lessonId
        // No available subject → honest "Choose a subject" fallback that moves
        // focus to the grid instead of a dead Continue (audit AC28, 2026-08-06).
        val noSubjectFallback = availableFirst == null || (targets.isNotEmpty() && nextLessonId == null)
        val questUi = if (godModeEnabled) {
            QuestUi(
                task = QuestTaskCopy.ParentMode,
                pawPrintsCompleted = completedCount,
                pawPrintTotal = questTotal,
                isComplete = dailyQuest.isComplete,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = QuestButtonLabel.OpenPlayground,
                buttonAction = QuestAction.OpenPlayground,
                targets = targets,
                nextLessonId = nextLessonId,
                godModeEnabled = true,
                sanctuaryComplete = true,
            )
        } else if (dailyQuest.isComplete) {
            QuestUi(
                task = QuestTaskCopy.CompleteToday,
                pawPrintsCompleted = questTotal,
                pawPrintTotal = questTotal,
                isComplete = true,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = QuestButtonLabel.OpenSanctuary,
                buttonAction = QuestAction.ViewReward,
                targets = targets,
                nextLessonId = nextLessonId,
                sanctuaryComplete = sanctuaryComplete,
            )
        } else {
            val hasQuestTarget = nextLessonId != null
            QuestUi(
                task = QuestTaskCopy.IncompleteToday,
                pawPrintsCompleted = completedCount,
                pawPrintTotal = questTotal,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = when {
                    noSubjectFallback -> QuestButtonLabel.ChooseSubject
                    hasQuestTarget && completedCount == 0 -> QuestButtonLabel.StartQuest
                    hasQuestTarget -> QuestButtonLabel.ContinueQuest
                    completedCount == 0 -> QuestButtonLabel.Start
                    else -> QuestButtonLabel.Continue
                },
                buttonAction = when {
                    noSubjectFallback -> QuestAction.ChooseSubject
                    hasQuestTarget -> QuestAction.OpenLesson
                    else -> QuestAction.Continue
                },
                targets = targets,
                nextLessonId = nextLessonId,
                sanctuaryComplete = sanctuaryComplete,
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

        val visibleSanctuary = if (godModeEnabled) {
            SanctuaryUi(
                earnedPieces = SanctuaryCatalog.pieces.size,
                visiblePieces = SanctuaryCatalog.pieces.map { piece ->
                    SanctuaryPieceUi(piece.id, piece.name, piece.description, piece.iconKey)
                },
                nextPiece = null,
                totalPieces = SanctuaryCatalog.pieces.size,
            )
        } else {
            sanctuary
        }

        return PlayroomHomeUiState.Content(
            childName = childName?.takeIf { it.isNotBlank() } ?: "",
            subjects = subjects,
            quest = questUi,
            wildlifeStickers = wildlifeStickers,
            offline = false,
            starBalance = starBalance,
            coinBalance = coinBalance,
            ownedKeepsakes = keepsakes,
            sanctuary = visibleSanctuary,
        )
    }
}
