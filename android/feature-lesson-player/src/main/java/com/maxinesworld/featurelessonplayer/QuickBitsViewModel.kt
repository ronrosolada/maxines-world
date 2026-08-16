package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coremodel.QuickBitItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class QuickBitItemUi(
    val item: QuickBitItem,
    val isDownloaded: Boolean = false,
    val localFile: File? = null,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val error: String? = null,
)

data class QuickBitsUiState(
    val isLoading: Boolean = true,
    val items: List<QuickBitItemUi> = emptyList(),
    val selectedCategory: String = "all",
    val categories: List<String> = listOf("all", "animals", "space", "science", "math"),
    val bulkProgress: BulkDownloadProgress = BulkDownloadProgress(),
    val playingItem: QuickBitItemUi? = null,
    val totalStorageBytes: Long = 0L,
    val downloadedStorageBytes: Long = 0L,
    val error: String? = null,
)

@HiltViewModel
class QuickBitsViewModel @Inject constructor(
    private val repository: QuickBitsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QuickBitsUiState())
    val state: StateFlow<QuickBitsUiState> = _state.asStateFlow()

    init {
        observeBulkProgress()
        loadCatalog()
    }

    private fun observeBulkProgress() {
        viewModelScope.launch {
            repository.bulkProgress.collect { progress ->
                _state.update { current ->
                    val updatedItems = current.items.map { itemUi ->
                        val isLocallySaved = repository.isDownloaded(itemUi.item)
                        val local = repository.getLocalFile(itemUi.item)
                        itemUi.copy(
                            isDownloaded = isLocallySaved,
                            localFile = local,
                            isDownloading = progress.isRunning && progress.currentItemTitle == itemUi.item.title
                        )
                    }
                    val downloadedBytes = updatedItems.filter { it.isDownloaded }.sumOf { it.item.sizeBytes }
                    current.copy(
                        bulkProgress = progress,
                        items = updatedItems,
                        downloadedStorageBytes = downloadedBytes
                    )
                }
            }
        }
    }

    fun loadCatalog(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val catalog = repository.getCatalog(forceRefresh)
                val itemUis = catalog.items.map { item ->
                    val isLocallySaved = repository.isDownloaded(item)
                    val local = repository.getLocalFile(item)
                    QuickBitItemUi(
                        item = item,
                        isDownloaded = isLocallySaved,
                        localFile = local
                    )
                }
                val allCategories = listOf("all") + catalog.categories.filter { it.isNotBlank() }.distinct()
                val totalBytes = itemUis.sumOf { it.item.sizeBytes }
                val downloadedBytes = itemUis.filter { it.isDownloaded }.sumOf { it.item.sizeBytes }

                _state.update {
                    it.copy(
                        isLoading = false,
                        items = itemUis,
                        categories = allCategories,
                        totalStorageBytes = totalBytes,
                        downloadedStorageBytes = downloadedBytes,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load Quick Bits catalog."
                    )
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _state.update { it.copy(selectedCategory = category) }
    }

    fun playVideo(itemUi: QuickBitItemUi) {
        _state.update { it.copy(playingItem = itemUi) }
    }

    fun stopPlaying() {
        _state.update { it.copy(playingItem = null) }
    }

    fun downloadSingle(itemUi: QuickBitItemUi) {
        if (itemUi.isDownloaded || itemUi.isDownloading) return
        viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    items = current.items.map {
                        if (it.item.id == itemUi.item.id) it.copy(isDownloading = true, error = null) else it
                    }
                )
            }
            try {
                val file = repository.downloadItem(itemUi.item) { downloaded, total ->
                    val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    _state.update { current ->
                        current.copy(
                            items = current.items.map {
                                if (it.item.id == itemUi.item.id) it.copy(downloadProgressPercent = percent) else it
                            }
                        )
                    }
                }
                _state.update { current ->
                    val updated = current.items.map {
                        if (it.item.id == itemUi.item.id) {
                            it.copy(isDownloaded = true, localFile = file, isDownloading = false, downloadProgressPercent = 100)
                        } else it
                    }
                    current.copy(
                        items = updated,
                        downloadedStorageBytes = updated.filter { it.isDownloaded }.sumOf { it.item.sizeBytes }
                    )
                }
            } catch (e: Exception) {
                _state.update { current ->
                    current.copy(
                        items = current.items.map {
                            if (it.item.id == itemUi.item.id) {
                                it.copy(isDownloading = false, error = e.message ?: "Download failed")
                            } else it
                        }
                    )
                }
            }
        }
    }

    fun downloadAll() {
        val allItems = _state.value.items.map { it.item }
        if (allItems.isEmpty() || _state.value.bulkProgress.isRunning) return
        viewModelScope.launch {
            repository.downloadAll(allItems)
            // Re-verify after download
            loadCatalog(false)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
            loadCatalog(false)
        }
    }
}
