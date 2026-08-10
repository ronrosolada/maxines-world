package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.coremodel.MediaAsset
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null,
)

data class VideoLibraryUiState(
    val isLoading: Boolean = true,
    val items: List<VideoLibraryItemUi> = emptyList(),
    val error: String? = null,
    val playingMediaId: String? = null,
    val assessmentQuiz: MediaAssessmentQuizState? = null,
    val watchedMediaIds: Set<String> = emptySet(),
)

@HiltViewModel
class VideoLibraryViewModel @Inject constructor(
    private val mediaLibrary: MediaLibrary,
) : ViewModel() {
    private val _state = MutableStateFlow(VideoLibraryUiState())
    val state: StateFlow<VideoLibraryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { mediaLibrary.refreshCatalog() }
                .onSuccess { assets ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = assets.map { asset ->
                                VideoLibraryItemUi(
                                    asset = asset,
                                    localPath = mediaLibrary.localFile(asset.mediaId)?.absolutePath,
                                )
                            },
                        )
                    }
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

    fun download(mediaId: String) {
        val item = _state.value.items.firstOrNull { it.asset.mediaId == mediaId } ?: return
        if (item.isDownloading || item.localPath != null) return
        _state.update { current ->
            current.copy(
                items = current.items.map {
                    if (it.asset.mediaId == mediaId) it.copy(isDownloading = true, error = null) else it
                },
            )
        }
        viewModelScope.launch {
            runCatching { mediaLibrary.download(mediaId) }
                .onSuccess { file ->
                    _state.update { current ->
                        current.copy(
                            items = current.items.map {
                                if (it.asset.mediaId == mediaId) {
                                    it.copy(isDownloading = false, localPath = file.absolutePath)
                                } else it
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { current ->
                        current.copy(
                            items = current.items.map {
                                if (it.asset.mediaId == mediaId) {
                                    it.copy(
                                        isDownloading = false,
                                        error = error.message ?: "Download failed.",
                                    )
                                } else it
                            },
                        )
                    }
                }
        }
    }

    fun play(mediaId: String) {
        if (_state.value.items.any { it.asset.mediaId == mediaId && it.localPath != null }) {
            _state.update { it.copy(playingMediaId = mediaId) }
        }
    }

    fun stopPlaying() {
        _state.update { it.copy(playingMediaId = null) }
    }

    /** Unlock a Tagalog video's memory check only after playback reaches the end. */
    fun markVideoWatched(mediaId: String) {
        val item = _state.value.items.firstOrNull { it.asset.mediaId == mediaId }
        if (item?.localPath == null) return
        _state.update { current ->
            current.copy(watchedMediaIds = current.watchedMediaIds + mediaId)
        }
    }

    fun startAssessment(mediaId: String) {
        val current = _state.value
        val item = current.items.firstOrNull { it.asset.mediaId == mediaId }
        if (item == null || !canOpenMediaAssessment(item, current.watchedMediaIds)) return
        _state.update {
            it.copy(
                assessmentQuiz = MediaAssessmentQuizState(mediaId = mediaId),
            )
        }
    }

    fun selectAssessmentOption(optionId: String) {
        _state.update { current ->
            current.assessmentQuiz?.let { quiz ->
                current.copy(assessmentQuiz = selectQuizOption(quiz, optionId))
            } ?: current
        }
    }

    fun submitAssessment() {
        _state.update { current ->
            val quiz = current.assessmentQuiz ?: return@update current
            val assessment = current.items
                .firstOrNull { it.asset.mediaId == quiz.mediaId }
                ?.asset
                ?.assessment
                ?: return@update current
            current.copy(assessmentQuiz = submitQuizAnswer(quiz, assessment))
        }
    }

    fun nextAssessment() {
        _state.update { current ->
            val quiz = current.assessmentQuiz ?: return@update current
            val assessment = current.items
                .firstOrNull { it.asset.mediaId == quiz.mediaId }
                ?.asset
                ?.assessment
                ?: return@update current
            current.copy(assessmentQuiz = advanceQuiz(quiz, assessment))
        }
    }

    fun restartAssessment() {
        _state.update { current ->
            current.assessmentQuiz?.let { quiz ->
                current.copy(assessmentQuiz = restartQuiz(quiz))
            } ?: current
        }
    }

    fun closeAssessment() {
        _state.update { it.copy(assessmentQuiz = null) }
    }
}
