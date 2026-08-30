package com.maxinesworld.featurerangerjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val loading: Boolean = true,
    val lastSavedId: String? = null,
) {
    val favoriteCount: Int get() = entries.count { it.isFavorite }
}

class RangerJournalViewModel(
    private val childId: String,
    private val store: JournalStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    private val _state = MutableStateFlow(JournalUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val entries = store.entries(childId)
            _state.update { JournalUiState(entries = entries, loading = false, lastSavedId = it.lastSavedId) }
        }
    }

    fun addEntry(sceneId: String, caption: String) {
        viewModelScope.launch {
            val entry = JournalEntry(
                id = idFactory(),
                childId = childId,
                sceneId = sceneId,
                takenAtEpochMillis = clock(),
                caption = caption.trim(),
            )
            store.add(entry)
            _state.update {
                it.copy(
                    entries = (listOf(entry) + it.entries.filter { e -> e.id != entry.id })
                        .sortedByDescending { e -> e.takenAtEpochMillis },
                    lastSavedId = entry.id,
                )
            }
        }
    }

    fun delete(entryId: String) {
        viewModelScope.launch {
            store.delete(childId, entryId)
            _state.update { state ->
                state.copy(entries = state.entries.filterNot { it.id == entryId })
            }
        }
    }

    fun toggleFavorite(entryId: String) {
        viewModelScope.launch {
            val entry = _state.value.entries.firstOrNull { it.id == entryId } ?: return@launch
            val favorite = !entry.isFavorite
            store.setFavorite(childId, entryId, favorite)
            _state.update { state ->
                state.copy(
                    entries = state.entries.map { if (it.id == entryId) it.copy(isFavorite = favorite) else it }
                )
            }
        }
    }
}

class RangerJournalViewModelFactory(
    private val childId: String,
    private val store: JournalStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RangerJournalViewModel(childId, store) as T
}