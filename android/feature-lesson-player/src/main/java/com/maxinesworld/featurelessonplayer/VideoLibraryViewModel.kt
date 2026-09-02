package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coremodel.ChildFacingMediaPolicy
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VideoLibraryItemUi(
    val asset: MediaAsset,
    val isDownloading: Boolean = false,
    val localPath: String? = null,
    val isPassed: Boolean = false,
    val isLocked: Boolean = false,
    val error: String? = null,
)

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
        loadLocalFirstAndSync()
    }

    private fun observePassedVideos() {
        viewModelScope.launch {
            videoWatchLedgerDao.observePassedMediaIds(childId).collect { passedList ->
                val passedSet = passedList.toSet()
                _state.update { current ->
                    current.copy(passedMediaIds = passedSet)
                }
                if (rawAssets.isNotEmpty()) {
                    reorganizeItems(passedSet)
                }
            }
        }
    }

    /**
     * Stale-While-Revalidate pattern:
     * 1. Immediately renders from cached memory or local disk catalog without network delays (0ms).
     * 2. Silently triggers background catalog refresh if network is available, updating state seamlessly.
     */
    private fun loadLocalFirstAndSync() {
        // Fast synchronous attempt from in-memory cache
        val fastCache = runCatching { mediaLibrary.getCachedCatalog() }.getOrNull()
        if (fastCache != null) {
            acceptCatalog(fastCache.media)
            _state.update { it.copy(isLoading = false) }
        }

        viewModelScope.launch {
            if (rawAssets.isEmpty()) {
                val cached = runCatching { mediaLibrary.getCatalog() }.getOrNull()
                if (cached != null) {
                    acceptCatalog(cached.media)
                    _state.update { it.copy(isLoading = false) }
                }
            }

            // MediaCatalogClient.fetchRaw already hops to Dispatchers.IO.
            runCatching { mediaLibrary.refreshCatalog() }
                .onSuccess { freshCatalog ->
                    acceptCatalog(freshCatalog.media)
                    _state.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    // Only surface an error if we had nothing cached locally
                    if (rawAssets.isEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "The video list could not be loaded.",
                            )
                        }
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = rawAssets.isEmpty(), error = null) }
            runCatching { mediaLibrary.refreshCatalog() }
                .onSuccess { catalog ->
                    acceptCatalog(catalog.media)
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    if (rawAssets.isEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "The video list could not be loaded.",
                            )
                        }
                    }
                }
        }
    }

    /**
     * Child surfaces only see Grade 3 RELEASED videos. Preview and other-grade
     * catalog rows stay on disk for future review and must not appear as core
     * curriculum or participate in the subject sequence lock.
     */
    private fun acceptCatalog(media: List<MediaAsset>) {
        rawAssets = ChildFacingMediaPolicy.childFacing(media)
        reorganizeItems(_state.value.passedMediaIds)
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
        if (!VideoWatchRewardPolicy.shouldCreditCurriculumWatch(item.asset)) return
        if (item.isDownloading || item.localPath != null) return
        updateItemInState(mediaId) { it.copy(isDownloading = true, error = null) }

        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { mediaLibrary.downloadChildFacing(mediaId) } }
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
                runCatching { withContext(Dispatchers.IO) { mediaLibrary.downloadChildFacing(mediaId) } }
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
        val isAlreadyPassed = mediaId in _state.value.passedMediaIds
        _state.update {
            it.copy(
                assessmentQuiz = beginMediaAssessmentQuiz(
                    mediaId = mediaId,
                    assessment = assessment,
                    isReplay = isAlreadyPassed,
                ),
            )
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
                    creditFirstPassingAssessment(
                        asset = asset,
                        score = advanced.correctCount.toFloat() / assessment.items.size.toFloat(),
                    )
                }
                // Curriculum credit only. A leftover PREVIEW / other-grade asset
                // can finish the quiz UI, but it must not look passed or paid.
                if (VideoWatchRewardPolicy.shouldCreditCurriculumWatch(asset)) {
                    val updatedPassed = _state.value.passedMediaIds + asset.mediaId
                    _state.update { it.copy(passedMediaIds = updatedPassed) }
                    reorganizeItems(updatedPassed)
                }
            }
        }
        _state.update { it.copy(assessmentQuiz = advanced) }
    }

    /**
     * First-pass curriculum credit. Re-checks [ChildFacingMediaPolicy] so a
     * leftover PREVIEW / other-grade [asset] cannot mint stars, stickers, or
     * accredited seconds even if it reached this writer.
     */
    internal suspend fun creditFirstPassingAssessment(
        asset: MediaAsset,
        score: Float,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (!VideoWatchRewardPolicy.shouldCreditCurriculumWatch(asset)) return false
        val actualChildId = childId.ifBlank { "default_child" }
        val existing = videoWatchLedgerDao.getEntry(actualChildId, asset.mediaId)
        if (existing?.quizPassed == true) {
            // Replay pass: refresh the best score only, never re-award.
            if (score > existing.bestQuizScore) {
                videoWatchLedgerDao.insertOrUpdate(
                    existing.copy(
                        bestQuizScore = score,
                        lastWatchedAtEpochMillis = now,
                    )
                )
            }
            _state.update { current ->
                current.copy(assessmentQuiz = current.assessmentQuiz?.copy(isReplay = true))
            }
            return false
        }

        val ledger = videoWatchLedgerDao.getAllByChild(actualChildId)
        val prevTotalSeconds = VideoWatchRewardPolicy.accreditedSecondsForChildFacing(
            passedEntries = ledger
                .filter { it.quizPassed }
                .map { it.mediaId to it.accreditedSeconds },
            catalog = rawAssets,
        )
        val prevEarnedStickers = VideoWatchRewardPolicy.calculateEarnedStickers(prevTotalSeconds)

        // Seed an unpassed row when needed. The unique child/media index
        // makes this safe if two completion callbacks arrive together.
        if (existing == null) {
            videoWatchLedgerDao.insertIgnoring(
                VideoWatchLedgerEntity(
                    id = "${actualChildId}_${asset.mediaId}",
                    childId = actualChildId,
                    mediaId = asset.mediaId,
                    subjectId = asset.subjectId,
                    accreditedSeconds = asset.durationSeconds,
                    quizPassed = false,
                    bestQuizScore = 0.0f,
                    firstPassedAtEpochMillis = null,
                    lastWatchedAtEpochMillis = now,
                )
            )
        }

        // This is the sole reward gate. Only the caller that changes
        // quizPassed false -> true may mint first-pass rewards.
        val firstPassClaimed = videoWatchLedgerDao.claimFirstPassingAssessment(
            childId = actualChildId,
            mediaId = asset.mediaId,
            score = score,
            passedAt = now,
        ) == 1

        if (!firstPassClaimed) {
            _state.update { current ->
                current.copy(assessmentQuiz = current.assessmentQuiz?.copy(isReplay = true))
            }
            return false
        }

        rewardDao.insertIgnoring(
            RewardEntity(
                id = "video-assessment:$actualChildId:${asset.mediaId}:STAR",
                childId = actualChildId,
                type = "STAR",
                subject = asset.subjectId,
                amount = 5,
                earnedAt = now,
                metadata = "video_assessment_passed:${asset.mediaId}",
            )
        )

        val newTotalSeconds = prevTotalSeconds + asset.durationSeconds.coerceAtLeast(0)
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
        return true
    }

    fun restartAssessment() {
        val quiz = _state.value.assessmentQuiz ?: return
        val asset = _state.value.allItems.firstOrNull { it.asset.mediaId == quiz.mediaId }?.asset ?: return
        val assessment = asset.assessment ?: return
        _state.update { it.copy(assessmentQuiz = restartQuiz(quiz, assessment)) }
    }

    /**
     * Fail-summary Watch again / Rewatch. Closes the quiz and starts the same
     * local file through [play]. Does not open a new scored attempt.
     */
    fun rewatchAfterFailedAssessment() {
        val mediaId = _state.value.assessmentQuiz?.mediaId ?: return
        _state.update { it.copy(assessmentQuiz = null) }
        play(mediaId)
    }

    fun closeAssessment() {
        _state.update { it.copy(assessmentQuiz = null) }
    }
}
