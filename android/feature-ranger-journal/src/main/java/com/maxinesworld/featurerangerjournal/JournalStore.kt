package com.maxinesworld.featurerangerjournal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Persistence contract for the Ranger Journal. */
interface JournalStore {
    suspend fun entries(childId: String): List<JournalEntry>
    suspend fun add(entry: JournalEntry)
    suspend fun delete(childId: String, entryId: String)
    suspend fun setFavorite(childId: String, entryId: String, favorite: Boolean)
}

private val Context.rangerJournalDataStore: DataStore<Preferences> by preferencesDataStore(name = "ranger_journal")

/**
 * DataStore-backed store: one Preferences key per child holding the whole
 * journal as an encoded JSON array. Survives restarts; no Room migration
 * surface is touched.
 */
class DataStoreJournalStore(private val context: Context) : JournalStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val listSerializer = ListSerializer(JournalEntry.serializer())

    private fun keyFor(childId: String) = stringPreferencesKey("journal_$childId")

    override suspend fun entries(childId: String): List<JournalEntry> {
        val raw = context.rangerJournalDataStore.data.first()[keyFor(childId)] ?: return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.takenAtEpochMillis }
    }

    override suspend fun add(entry: JournalEntry) {
        context.rangerJournalDataStore.edit { prefs ->
            val key = keyFor(entry.childId)
            val current = decode(prefs[key])
            if (current.any { it.id == entry.id }) return@edit
            prefs[key] = json.encodeToString(listSerializer, current + entry)
        }
    }

    override suspend fun delete(childId: String, entryId: String) {
        context.rangerJournalDataStore.edit { prefs ->
            val key = keyFor(childId)
            prefs[key] = json.encodeToString(listSerializer, decode(prefs[key]).filterNot { it.id == entryId })
        }
    }

    override suspend fun setFavorite(childId: String, entryId: String, favorite: Boolean) {
        context.rangerJournalDataStore.edit { prefs ->
            val key = keyFor(childId)
            prefs[key] = json.encodeToString(
                listSerializer,
                decode(prefs[key]).map { if (it.id == entryId) it.copy(isFavorite = favorite) else it }
            )
        }
    }

    private fun decode(raw: String?): List<JournalEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }
            .getOrDefault(emptyList())
    }
}

/** In-memory store for unit tests. */
class InMemoryJournalStore : JournalStore {
    private val byChild = mutableMapOf<String, MutableList<JournalEntry>>()

    override suspend fun entries(childId: String): List<JournalEntry> =
        byChild[childId].orEmpty().sortedByDescending { it.takenAtEpochMillis }

    override suspend fun add(entry: JournalEntry) {
        val list = byChild.getOrPut(entry.childId) { mutableListOf() }
        if (list.none { it.id == entry.id }) list += entry
    }

    override suspend fun delete(childId: String, entryId: String) {
        byChild[childId]?.removeAll { it.id == entryId }
    }

    override suspend fun setFavorite(childId: String, entryId: String, favorite: Boolean) {
        val list = byChild[childId] ?: return
        val index = list.indexOfFirst { it.id == entryId }
        if (index >= 0) list[index] = list[index].copy(isFavorite = favorite)
    }
}