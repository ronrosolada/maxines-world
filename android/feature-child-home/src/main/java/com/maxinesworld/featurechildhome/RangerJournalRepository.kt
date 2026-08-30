package com.maxinesworld.featurechildhome

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable data class RangerSnapshot(
    val id: String, val childId: String, val caption: String, val timestamp: Long,
    val timeOfDay: String, val species: List<String>, val stickers: List<String>, val miloPose: String,
)
@Serializable data class PawprintStamp(val speciesId: String, val speciesName: String, val unlockedAt: Long)
@Serializable data class RangerBadge(val id: String, val titleEnglish: String, val titleFilipino: String, val note: String, val unlockedAt: Long)
@Serializable data class RangerJournal(val snapshots: List<RangerSnapshot> = emptyList(), val stamps: List<PawprintStamp> = emptyList(), val badges: List<RangerBadge> = emptyList())

interface RangerJournalStore { fun read(childId: String): String?; fun write(childId: String, value: String) }
class FileRangerJournalStore(context: Context) : RangerJournalStore {
    private val root = File(context.filesDir, "ranger-journals").apply { mkdirs() }
    private fun file(childId: String) = File(root, childId.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".json")
    override fun read(childId: String) = file(childId).takeIf(File::exists)?.readText()
    override fun write(childId: String, value: String) { val target=file(childId); val temp=File(target.parentFile,target.name+".tmp"); temp.writeText(value); check(temp.renameTo(target)) }
}
class RangerJournalRepository(private val store: RangerJournalStore, private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }) {
    fun journal(childId: String): RangerJournal = store.read(childId)?.let { runCatching { json.decodeFromString<RangerJournal>(it) }.getOrNull() } ?: RangerJournal()
    fun saveSnapshot(snapshot: RangerSnapshot): RangerJournal = update(snapshot.childId) { old ->
        val newStamps = snapshot.species.filterNot { id -> old.stamps.any { it.speciesId == id } }.map { PawprintStamp(it, it.replace('_',' ').replaceFirstChar(Char::uppercase), snapshot.timestamp) }
        val badges = buildList {
            if (snapshot.timeOfDay in listOf("Dusk","Night") && snapshot.species.any { it.contains("tarsier", true) }) add(RangerBadge("night-owl","Night Owl Ranger","Tanod-Gubat sa Gabi","Visited Tarsier at dusk",snapshot.timestamp))
        }.filterNot { badge -> old.badges.any { it.id == badge.id } }
        old.copy(snapshots = listOf(snapshot) + old.snapshots, stamps = old.stamps + newStamps, badges = old.badges + badges)
    }
    fun addStamp(childId: String, stamp: PawprintStamp) = update(childId) { if (it.stamps.any { s -> s.speciesId == stamp.speciesId }) it else it.copy(stamps=it.stamps+stamp) }
    private fun update(childId: String, block: (RangerJournal)->RangerJournal): RangerJournal = block(journal(childId)).also { store.write(childId,json.encodeToString(it)) }
    fun exportText(childId: String, filipino: Boolean): String { val j=journal(childId); return buildString { appendLine(if(filipino) "TALAARAWAN NG TANOD-GUBAT" else "FIELD RANGER JOURNAL"); j.snapshots.forEach { appendLine("${it.caption} — ${it.timeOfDay} — ${it.species.joinToString()}") }; j.stamps.forEach { appendLine("🐾 ${it.speciesName}") }; j.badges.forEach { appendLine("★ ${if(filipino) it.titleFilipino else it.titleEnglish}: ${it.note}") } } }
}
