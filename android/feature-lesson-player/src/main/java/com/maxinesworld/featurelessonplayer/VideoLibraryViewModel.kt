package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
import com.maxinesworld.featurerewards.BadgeLoader
import com.maxinesworld.featurerewards.VideoWatchRewardPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideoLibraryItemUi(
    val asset: MediaAsset,
    val isDownloading: Boolean = false,
    val localPath: String? = null,
    val isPassed: Boolean = false,
    val isLocked: Boolean = false,
    val error: String? = null,
)

/** A single lesson inside the "Today's Video Quest" (cross-subject set). */
data class VideoQuestItemUi(
    val mediaId: String,
    val title: String,
    val subjectId: String,
    val durationSeconds: Int,
    val isPassed: Boolean,
)

/** "Today's Video Quest": 2-3 next-unlocked lessons from different subjects. */
data class VideoQuestUi(
    val items: List<VideoQuestItemUi>,
    val totalSeconds: Int,
    val completedCount: Int,
    val isComplete: Boolean,
) {
    val subjectCount: Int get() = items.map { it.subjectId }.distinct().size
}

data class VideoLibraryUiState(
    val isLoading: Boolean = true,
    val upcomingItems: List<VideoLibraryItemUi> = emptyList(),
    val completedItems: List<VideoLibraryItemUi> = emptyList(),
    val error: String? = null,
    val playingMediaId: String? = null,
    val assessmentQuiz: MediaAssessmentQuizState? = null,
    val watchedMediaIds: Set<String> = emptySet(),
    val passedMediaIds: Set<String> = emptySet(),
    val filterSubjectId: String? = null,
    val isDownloadingAll: Boolean = false,
    val downloadAllProgress: Float = 0f,
    val downloadAllCompletedCount: Int = 0,
    val downloadAllTotalCount: Int = 0,
    val newlyAwardedStickerName: String? = null,
    val videoQuest: VideoQuestUi? = null,
) {
    val allItems: List<VideoLibraryItemUi>
        get() = upcomingItems + completedItems
}

@HiltViewModel
class VideoLibraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaLibrary: MediaLibrary,
    private val videoWatchLedgerDao: VideoWatchLedgerDao,
    private val rewardDao: RewardDao,
    private val collectedBadgeDao: CollectedBadgeDao,
    private val badgeLoader: BadgeLoader,
) : ViewModel() {

    val childId: String = savedStateHandle["childId"] ?: "default_child"
    val initialSubject: String? = savedStateHandle.get<String>("subject")?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(VideoLibraryUiState(filterSubjectId = initialSubject))
    val state: StateFlow<VideoLibraryUiState> = _state.asStateFlow()

    private var rawAssets: List<MediaAsset> = emptyList()

    init {
        observePassedVideos()
        refresh()
    }

    private fun observePassedVideos() {
        viewModelScope.launch {
            videoWatchLedgerDao.observePassedMediaIds(childId).collect { passedList ->
                val passedSet = passedList.toSet()
                _state.update { current ->
                    current.copy(passedMediaIds = passedSet)
                }
                reorganizeItems(passedSet)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { mediaLibrary.refreshCatalog() }
                .onSuccess { catalog ->
                    rawAssets = catalog.media
                    reorganizeItems(_state.value.passedMediaIds)
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "The video list could not be loaded.",
                        )
                    }
                }
        }
    }

    private fun reorganizeItems(passedSet: Set<String>) {
        val filterSubj = _state.value.filterSubjectId
        val filtered = (if (!filterSubj.isNullOrBlank()) {
            rawAssets.filter { it.subjectId.equals(filterSubj, ignoreCase = true) }
        } else {
            rawAssets
        }).sortedBy { it.episodeNumber }

        // Sequence guard rail: a lesson video is LOCKED until every earlier video
        // in its subject's curriculum order has been passed. Prevents a child from
        // jumping ahead of the learning sequence.
        val lockedMediaIds = computeSequencedLockedIds(passedSet)

        val upcoming = mutableListOf<VideoLibraryItemUi>()
        val completed = mutableListOf<VideoLibraryItemUi>()

        filtered.forEach { asset ->
            val isPassed = asset.mediaId in passedSet
            val item = VideoLibraryItemUi(
                asset = asset,
                localPath = mediaLibrary.localFile(asset.mediaId)?.absolutePath,
                isPassed = isPassed,
                isLocked = !isPassed && asset.mediaId in lockedMediaIds,
            )
            if (isPassed) {
                completed.add(item)
            } else {
                upcoming.add(item)
            }
        }

        _state.update {
            it.copy(
                upcomingItems = upcoming,
                completedItems = completed,
            )
        }
        recomputeVideoQuest(passedSet)
    }

    /**
     * Builds the deterministic "Today's Video Quest": the next-unlocked lesson of
     * each subject (frontier) is fed to [VideoQuestPlanner], which picks 2-3 lessons
     * from different subjects whose total is within [30, 40] minutes. Completion is
     * derived from the ledger pass state; a one-time bonus is granted when done.
     */
    private var videoQuestBonusGranted = false

    private fun recomputeVideoQuest(passedSet: Set<String>) {
        val candidates = rawAssets
            .groupBy { it.subjectId.orEmpty() }
            .mapNotNull { (subject, assets) ->
                if (subject.isBlank()) return@mapNotNull null
                val frontier = assets.sortedBy { it.episodeNumber }.firstOrNull { it.mediaId !in passedSet }
                    ?: return@mapNotNull null
                VideoQuestPlanner.Candidate(frontier.mediaId, subject, frontier.durationSeconds)
            }
        val selectedIds = VideoQuestPlanner.select(childId, LocalDate.now().toString(), candidates)
        if (selectedIds.isEmpty()) {
            _state.update { it.copy(videoQuest = null) }
            return
        }
        val byId = rawAssets.associateBy { it.mediaId }
        val items = selectedIds.map { id ->
            val asset = byId.getValue(id)
            VideoQuestItemUi(
                mediaId = id,
                title = asset.title,
                subjectId = asset.subjectId.orEmpty(),
                durationSeconds = asset.durationSeconds,
                isPassed = id in passedSet,
            )
        }
        val total = items.sumOf { it.durationSeconds }
        val completedCount = items.count { it.isPassed }
        val isComplete = items.isNotEmpty() && items.all { it.isPassed }
        _state.update { it.copy(videoQuest = VideoQuestUi(items, total, completedCount, isComplete)) }
        if (isComplete && !videoQuestBonusGranted) {
            videoQuestBonusGranted = true
            grantVideoQuestBonus()
        }
    }

    private fun grantVideoQuestBonus() {
        val today = LocalDate.now().toString()
        viewModelScope.launch {
            rewardDao.insertIgnoring(
                RewardEntity(
                    id = "video-quest:$childId:$today",
                    childId = childId,
                    type = "STAR",
                    subject = "video_quest",
                    amount = 3,
                    earnedAt = System.currentTimeMillis(),
                    metadata = "daily_video_quest_completed:$today",
                )
            )
        }
    }

    /**
     * Returns the set of mediaIds that are locked because a predecessor in their
     * subject's curriculum sequence (ordered by episodeNumber) is not yet passed.
     * Computed across the full catalog per subject so the gate is stable whether
     * the hub is viewed filtered or global.
     */
    private fun computeSequencedLockedIds(passedSet: Set<String>): Set<String> {
        val locked = mutableSetOf<String>()
        rawAssets.groupBy { it.subjectId.orEmpty() }.forEach { (subject, assets) ->
            if (subject.isBlank()) return@forEach
            val ordered = assets.sortedBy { it.episodeNumber }
            var allPreviousPassed = true
            ordered.forEach { asset ->
                val isPassed = asset.mediaId in passedSet
                if (!isPassed && !allPreviousPassed) locked += asset.mediaId
                if (!isPassed) allPreviousPassed = false
            }
        }
        return locked
    }

    fun download(mediaId: String) {
        val item = _state.value.allItems.firstOrNull { it.asset.mediaId == mediaId } ?: return
        if (item.isDownloading || item.localPath != null) return
        updateItemInState(mediaId) { it.copy(isDownloading = true, error = null) }

        viewModelScope.launch {
            runCatching { mediaLibrary.download(mediaId) }
                .onSuccess { file ->
                    updateItemInState(mediaId) {
                        it.copy(isDownloading = false, localPath = file.absolutePath)
                    }
                }
                .onFailure { error ->
                    updateItemInState(mediaId) {
                        it.copy(isDownloading = false, error = error.message)
                    }
                }
        }
    }

    private fun updateItemInState(mediaId: String, transform: (VideoLibraryItemUi) -> VideoLibraryItemUi) {
        _state.update { current ->
            current.copy(
                upcomingItems = current.upcomingItems.map {
                    if (it.asset.mediaId == mediaId) transform(it) else it
                },
                completedItems = current.completedItems.map {
                    if (it.asset.mediaId == mediaId) transform(it) else it
                },
            )
        }
    }

    fun downloadAll() {
        if (_state.value.isDownloadingAll) return
        val pendingItems = _state.value.allItems.filter { it.localPath == null }
        if (pendingItems.isEmpty()) return

        val total = pendingItems.size
        _state.update {
            it.copy(
                isDownloadingAll = true,
                downloadAllProgress = 0f,
                downloadAllCompletedCount = 0,
                downloadAllTotalCount = total
            )
        }

        viewModelScope.launch {
            var completed = 0
            for (item in pendingItems) {
                val mediaId = item.asset.mediaId
                updateItemInState(mediaId) { it.copy(isDownloading = true, error = null) }
                runCatching { mediaLibrary.download(mediaId) }
                    .onSuccess { file ->
                        completed++
                        _state.update { current ->
                            current.copy(
                                downloadAllCompletedCount = completed,
                                downloadAllProgress = completed.toFloat() / total.toFloat(),
                            )
                        }
                        updateItemInState(mediaId) {
                            it.copy(isDownloading = false, localPath = file.absolutePath)
                        }
                    }
                    .onFailure { error ->
                        completed++
                        _state.update { current ->
                            current.copy(
                                downloadAllCompletedCount = completed,
                                downloadAllProgress = completed.toFloat() / total.toFloat(),
                            )
                        }
                        updateItemInState(mediaId) {
                            it.copy(isDownloading = false, error = error.message)
                        }
                    }
            }
            _state.update { it.copy(isDownloadingAll = false) }
        }
    }

    fun play(mediaId: String) {
        val item = _state.value.allItems.firstOrNull { it.asset.mediaId == mediaId }
        // Sequence guard rail: do not start a lesson that is locked behind an
        // un-passed predecessor.
        if (item == null || item.isLocked) return
        if (item.localPath != null) {
            _state.update { it.copy(playingMediaId = mediaId) }
        }
    }

    fun stopPlaying() {
        _state.update { it.copy(playingMediaId = null) }
    }

    fun markVideoWatched(mediaId: String) {
        _state.update { it.copy(watchedMediaIds = it.watchedMediaIds + mediaId) }
        openAssessment(mediaId)
    }

    fun openAssessment(mediaId: String) {
        val asset = _state.value.allItems.firstOrNull { it.asset.mediaId == mediaId }?.asset ?: return
        val assessment = asset.assessment ?: return
        if (assessment.items.isEmpty()) return
        // Sequence guard rail: a locked lesson cannot jump ahead to its quiz.
        if (_state.value.allItems.firstOrNull { it.asset.mediaId == mediaId }?.isLocked == true) return
        _state.update {
            it.copy(assessmentQuiz = MediaAssessmentQuizState(mediaId = mediaId))
        }
    }

    fun selectAssessmentOption(optionId: String) {
        val quiz = _state.value.assessmentQuiz ?: return
        _state.update { it.copy(assessmentQuiz = selectQuizOption(quiz, optionId)) }
    }

    fun checkAssessmentAnswer() {
        val quiz = _state.value.assessmentQuiz ?: return
        val asset = _state.value.allItems.firstOrNull { it.asset.mediaId == quiz.mediaId }?.asset ?: return
        val assessment = asset.assessment ?: return
        _state.update { it.copy(assessmentQuiz = submitQuizAnswer(quiz, assessment)) }
    }

    fun nextAssessmentQuestion() {
        val quiz = _state.value.assessmentQuiz ?: return
        val asset = _state.value.allItems.firstOrNull { it.asset.mediaId == quiz.mediaId }?.asset ?: return
        val assessment = asset.assessment ?: return
        val advanced = advanceQuiz(quiz, assessment)

        if (advanced.finished) {
            // Enforce the >=80% quiz contract even if a catalog entry authored a
            // looser passingCorrectCount. floor(80% of items).
            val requiredCorrect = maxOf(
                assessment.passingCorrectCount,
                Math.ceil(assessment.items.size * 0.80).toInt(),
            )
            val passed = advanced.correctCount >= requiredCorrect
            if (passed) {
                viewModelScope.launch {
                    val score = advanced.correctCount.toFloat() / assessment.items.size.toFloat()
                    val actualChildId = childId.ifBlank { "default_child" }
                    val existing = videoWatchLedgerDao.getEntry(actualChildId, asset.mediaId)
                    if (existing?.quizPassed == true) {
                        // Replay pass: refresh the best score only, never re-award.
                        if (score > existing.bestQuizScore) {
                            videoWatchLedgerDao.insertOrUpdate(
                                existing.copy(
                                    bestQuizScore = score,
                                    lastWatchedAtEpochMillis = System.currentTimeMillis(),
                                )
                            )
                        }
                    } else {
                        // Genuine first pass — award 5 stars + wildlife stickers exactly once.
                        // 1. Previous total accredited seconds BEFORE recording this video
                        val prevTotalSeconds = videoWatchLedgerDao.getTotalAccreditedSeconds(actualChildId)
                        val prevEarnedStickers = VideoWatchRewardPolicy.calculateEarnedStickers(prevTotalSeconds)

                        // 2. Record the watch ledger with FULL official video duration (idempotent upsert)
                        videoWatchLedgerDao.insertOrUpdate(
                            VideoWatchLedgerEntity(
                                id = "${actualChildId}_${asset.mediaId}",
                                childId = actualChildId,
                                mediaId = asset.mediaId,
                                subjectId = asset.subjectId,
                                accreditedSeconds = asset.durationSeconds,
                                quizPassed = true,
                                bestQuizScore = score,
                                firstPassedAtEpochMillis = System.currentTimeMillis(),
                                lastWatchedAtEpochMillis = System.currentTimeMillis(),
                            )
                        )

                        // 3. Award 5 stars — deterministic id + IGNORE makes this a one-time reward
                        rewardDao.insertIgnoring(
                            RewardEntity(
                                id = "video-assessment:$actualChildId:${asset.mediaId}:STAR",
                                childId = actualChildId,
                                type = "STAR",
                                subject = asset.subjectId,
                                amount = 5,
                                earnedAt = System.currentTimeMillis(),
                                metadata = "video_assessment_passed:${asset.mediaId}",
                            )
                        )

                        // 4. Award wildlife sticker(s) if 30-min cumulative threshold reached
                        val newTotalSeconds = videoWatchLedgerDao.getTotalAccreditedSeconds(actualChildId)
                        val newEarnedStickers = VideoWatchRewardPolicy.calculateEarnedStickers(newTotalSeconds)

                        if (newEarnedStickers > prevEarnedStickers) {
                            val stickersToAward = newEarnedStickers - prevEarnedStickers
                            val allBadges = badgeLoader.loadAll()
                            val earnedBadges = collectedBadgeDao.getAllByChild(actualChildId).map { it.badgeId }.toSet()
                            val availableBadges = allBadges.filter { it.id !in earnedBadges && it.biome != "milestone" }

                            for (i in 0 until stickersToAward) {
                                val badgeToAward = availableBadges.getOrNull(i)
                                if (badgeToAward != null) {
                                    collectedBadgeDao.insert(
                                        CollectedBadgeEntity(
                                            id = "${actualChildId}_${badgeToAward.id}",
                                            childId = actualChildId,
                                            badgeId = badgeToAward.id,
                                            biome = badgeToAward.biome,
                                            earnedDate = LocalDate.now().toString(),
                                        )
                                    )
                                    _state.update { it.copy(newlyAwardedStickerName = badgeToAward.name) }
                                }
                            }
                        }
                    }
                }
                // Immediately update in-memory state
                val updatedPassed = _state.value.passedMediaIds + asset.mediaId
                _state.update { it.copy(passedMediaIds = updatedPassed) }
                reorganizeItems(updatedPassed)
            }
        }
        _state.update { it.copy(assessmentQuiz = advanced) }
    }

    fun restartAssessment() {
        val quiz = _state.value.assessmentQuiz ?: return
        _state.update { it.copy(assessmentQuiz = restartQuiz(quiz)) }
    }

    fun closeAssessment() {
        _state.update { it.copy(assessmentQuiz = null) }
    }
}
