package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
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
        }).sortedWith(
            compareBy<MediaAsset>({ it.gradeLevel }, { it.quarter }, { it.episodeNumber }, { it.title })
        )

        val upcoming = mutableListOf<VideoLibraryItemUi>()
        val completed = mutableListOf<VideoLibraryItemUi>()

        filtered.forEach { asset ->
            val isPassed = asset.mediaId in passedSet
            val item = VideoLibraryItemUi(
                asset = asset,
                localPath = mediaLibrary.localFile(asset.mediaId)?.absolutePath,
                isPassed = isPassed,
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
        if (_state.value.allItems.any { it.asset.mediaId == mediaId && it.localPath != null }) {
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
            val passed = advanced.correctCount >= assessment.passingCorrectCount
            if (passed) {
                viewModelScope.launch {
                    val score = advanced.correctCount.toFloat() / assessment.items.size.toFloat()
                    val actualChildId = childId.ifBlank { "default_child" }
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
                    rewardDao.insert(
                        RewardEntity(
                            id = UUID.randomUUID().toString(),
                            childId = actualChildId,
                            type = "STARS",
                            subject = asset.subjectId,
                            amount = 5,
                            earnedAt = System.currentTimeMillis(),
                            metadata = "video_assessment_passed:${asset.mediaId}",
                        )
                    )
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
