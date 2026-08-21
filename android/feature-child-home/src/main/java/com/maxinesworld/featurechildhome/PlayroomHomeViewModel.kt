package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.GodModeManager
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.PlaygroundUnlockReceiptDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import com.maxinesworld.featurerewards.SanctuaryCatalog
import com.maxinesworld.featurerewards.TreatShopCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class HomeDataTuple(
    val profile: com.maxinesworld.coredatabase.ChildProfileEntity?,
    val lessonIds: List<String>,
    val totalAccreditedSeconds: Int,
    val godModeEnabled: Boolean,
    val playgroundUnlocked: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayroomHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: ModuleCatalog,
    private val mediaLibrary: MediaLibrary,
    private val childProfileDao: ChildProfileDao,
    private val lessonCompletionDao: LessonCompletionDao,
    private val badgeAwarder: BadgeAwarder,
    private val rewardDao: RewardDao,
    private val inventoryDao: InventoryDao,
    private val videoWatchLedgerDao: VideoWatchLedgerDao,
    private val dailyQuestManager: DailyQuestManager,
    private val godModeManager: GodModeManager,
    private val playgroundUnlockReceiptDao: PlaygroundUnlockReceiptDao,
) : ViewModel() {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    private val _state = MutableStateFlow<PlayroomHomeUiState>(PlayroomHomeUiState.Loading)
    val state: StateFlow<PlayroomHomeUiState> = _state.asStateFlow()

    private var openingSubjectId: String? = null
    private var stateJob: Job? = null
    private var videoProgressJob: Job? = null
    private var finalContentJob: Job? = null
    private val baseContent = MutableStateFlow<PlayroomHomeUiState.Content?>(null)
    private val videoAssets = MutableStateFlow<List<MediaAsset>?>(null)
    private val passedVideoIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        collectState()
        collectVideoProgress()
        collectFinalContent()
    }

    private fun collectVideoProgress() {
        videoProgressJob?.cancel()
        videoAssets.value = null
        videoProgressJob = viewModelScope.launch {
            launch {
                videoWatchLedgerDao.observePassedMediaIds(childId).collect { ids ->
                    passedVideoIds.value = ids.toSet()
                }
            }
            launch {
                // Video progress is optional for the home screen. A missing LAN
                // catalog must not turn a usable home into an error state, and
                // legacy lesson completion must never be shown as video progress.
                try {
                    videoAssets.value = mediaLibrary.getCatalog().media
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep the home usable and explicitly show unavailable
                    // progress instead of substituting legacy lesson counts.
                }
            }
        }
    }

    private fun collectFinalContent() {
        finalContentJob?.cancel()
        finalContentJob = viewModelScope.launch {
            combine(baseContent, videoAssets, passedVideoIds) { content, assets, passed ->
                content?.let { withVideoProgress(it, assets, passed) }
            }.collect { content ->
                if (content != null) _state.value = content
            }
        }
    }

    private fun collectState() {
        stateJob?.cancel()
        val profileFlow = childProfileDao.observeById(childId)
        val lessonIdsFlow = lessonCompletionDao.observeDistinctLessonIds(childId)
        val accreditedSecondsFlow = videoWatchLedgerDao.observeTotalAccreditedSeconds(childId)

        stateJob = viewModelScope.launch {
            try {
                val playgroundUnlockFlow = playgroundUnlockReceiptDao.observeByChildAndDay(
                    childId = childId,
                    dayKey = LocalDate.now(ZoneId.systemDefault()).toString(),
                )
                combine(
                    profileFlow,
                    lessonIdsFlow,
                    accreditedSecondsFlow,
                    godModeManager.isEnabled(childId),
                    playgroundUnlockFlow,
                ) { profile, ids, seconds, godMode, playgroundReceipt ->
                    val playgroundUnlocked = playgroundReceipt != null
                    HomeDataTuple(profile, ids, seconds, godMode, playgroundUnlocked)
                }.collect { data ->
                    val dailyQuest = dailyQuestManager.ensureToday(
                        childId = childId,
                        completedLessonIds = data.lessonIds,
                    )
                    val badges = badgeAwarder.getCollectedBadges(childId).let { loaded ->
                        if (data.godModeEnabled) loaded.map { badge -> badge.copy(isCollected = true) } else loaded
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
                            SanctuaryPieceUi(
                                id = piece.id,
                                name = piece.name,
                                description = piece.description,
                                iconKey = piece.iconKey,
                                residentWildlife = piece.residentWildlife,
                                funFact = piece.funFact,
                            )
                        },
                        nextPiece = SanctuaryCatalog.pieces
                            .getOrNull(sanctuaryCount)
                            ?.let { piece ->
                                SanctuaryPieceUi(
                                    id = piece.id,
                                    name = piece.name,
                                    description = piece.description,
                                    iconKey = piece.iconKey,
                                    residentWildlife = piece.residentWildlife,
                                    funFact = piece.funFact,
                                )
                            },
                        totalPieces = SanctuaryCatalog.pieces.size,
                    )
                    val keepsakes = inventoryDao.getOwnedItemIds(childId)
                        .mapNotNull { id -> TreatShopCatalog.byId(id) }
                        .map { item -> KeepsakeUi(item.id, item.name, item.iconKey) }
                    val visibleKeepsakes = if (data.godModeEnabled) {
                        TreatShopCatalog.items.map { item -> KeepsakeUi(item.id, item.name, item.iconKey) }
                    } else {
                        keepsakes
                    }
                    baseContent.value = buildContent(
                        data.profile?.name,
                        data.lessonIds,
                        dailyQuest,
                        badges,
                        stars,
                        coins,
                        data.totalAccreditedSeconds,
                        visibleKeepsakes,
                        sanctuary,
                        data.godModeEnabled,
                        data.playgroundUnlocked,
                    ).copy(openingSubjectId = openingSubjectId)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                baseContent.value = null
                _state.value = PlayroomHomeUiState.Error(
                    message = "We couldn't load your Playroom. Try again."
                )
            }
        }
    }

    fun onSubjectSelected(subjectId: String): Boolean {
        if (openingSubjectId != null) return false
        val current = _state.value as? PlayroomHomeUiState.Content ?: return false
        val subject = current.subjects.firstOrNull { it.id == subjectId } ?: return false
        if (!subject.isAvailable) return false
        openingSubjectId = subjectId
        baseContent.value = baseContent.value?.copy(openingSubjectId = subjectId)
        return true
    }

    fun onOpenFinished() {
        openingSubjectId = null
        baseContent.value = baseContent.value?.copy(openingSubjectId = null)
    }

    fun retry() {
        openingSubjectId = null
        baseContent.value = null
        _state.value = PlayroomHomeUiState.Loading
        collectState()
        collectVideoProgress()
    }

    private suspend fun buildContent(
        childName: String?,
        lessonIds: List<String>,
        dailyQuest: DailyQuestProgress,
        badges: List<com.maxinesworld.coremodel.CollectibleBadge>,
        starBalance: Int,
        coinBalance: Int,
        totalAccreditedSeconds: Int,
        keepsakes: List<KeepsakeUi>,
        sanctuary: SanctuaryUi,
        godModeEnabled: Boolean,
        playgroundUnlocked: Boolean = false,
    ): PlayroomHomeUiState.Content {
        val completed = lessonIds.toSet()

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
                // Kept for compatibility with internal/legacy calculations only;
                // the child-facing card renders video counts below.
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
            val showPlayground = playgroundUnlocked || godModeEnabled
            QuestUi(
                task = QuestTaskCopy.CompleteToday,
                pawPrintsCompleted = questTotal,
                pawPrintTotal = questTotal,
                isComplete = true,
                recommendedSubjectId = availableFirst?.id,
                buttonLabel = if (showPlayground) QuestButtonLabel.OpenPlayground else QuestButtonLabel.OpenSanctuary,
                buttonAction = if (showPlayground) QuestAction.OpenPlayground else QuestAction.ViewReward,
                targets = targets,
                nextLessonId = nextLessonId,
                sanctuaryComplete = sanctuaryComplete,
                playgroundUnlocked = playgroundUnlocked,
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
                    SanctuaryPieceUi(
                        id = piece.id,
                        name = piece.name,
                        description = piece.description,
                        iconKey = piece.iconKey,
                        residentWildlife = piece.residentWildlife,
                        funFact = piece.funFact,
                    )
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
            totalAccreditedSeconds = totalAccreditedSeconds,
            ownedKeepsakes = keepsakes,
            sanctuary = visibleSanctuary,
        )
    }
}
