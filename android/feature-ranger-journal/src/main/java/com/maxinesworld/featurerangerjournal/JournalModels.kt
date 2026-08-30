package com.maxinesworld.featurerangerjournal

import kotlinx.serialization.Serializable

/**
 * Ranger Journal domain model.
 *
 * A [JournalEntry] is one Polaroid memory captured in a sanctuary scene.
 * Entries persist per child via [JournalStore] (DataStore on device).
 */
@Serializable
data class JournalEntry(
    val id: String,
    val childId: String,
    val sceneId: String,
    val takenAtEpochMillis: Long,
    val caption: String = "",
    val isFavorite: Boolean = false,
)

/**
 * A camera scene the child can point the Polaroid at. Colors are stored as
 * packed ARGB longs so the catalog stays pure-Kotlin and unit-testable;
 * the UI converts them with [Color].
 */
data class JournalScene(
    val id: String,
    val skyTop: Long,
    val skyBottom: Long,
    val ground: Long,
    val accent: Long,
)

object JournalScenes {
    val tarsier = JournalScene(
        id = "tarsier",
        skyTop = 0xFFBFE8F5,
        skyBottom = 0xFFF7E8C0,
        ground = 0xFF5B8C5A,
        accent = 0xFFD9A066,
    )
    val eagle = JournalScene(
        id = "eagle",
        skyTop = 0xFF8FCBEB,
        skyBottom = 0xFFDCF3FF,
        ground = 0xFF6E9BC8,
        accent = 0xFF4A6FA5,
    )
    val tamaraw = JournalScene(
        id = "tamaraw",
        skyTop = 0xFFA8E0A0,
        skyBottom = 0xFFE8F7C8,
        ground = 0xFF7FB069,
        accent = 0xFF5A4A3A,
    )
    val pawikan = JournalScene(
        id = "pawikan",
        skyTop = 0xFF7FD4E8,
        skyBottom = 0xFFE0F6FA,
        ground = 0xFFE8C98A,
        accent = 0xFF3E8E7E,
    )

    val all: List<JournalScene> = listOf(tarsier, eagle, tamaraw, pawikan)

    fun byId(id: String): JournalScene? = all.firstOrNull { it.id == id }
}