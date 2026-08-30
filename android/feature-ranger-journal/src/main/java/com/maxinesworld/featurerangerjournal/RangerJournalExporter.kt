package com.maxinesworld.featurerangerjournal

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure formatter that turns journal entries into a shareable Markdown
 * document — unit-testable, no Android dependencies.
 */
object RangerJournalExporter {

    fun exportMarkdown(
        entries: List<JournalEntry>,
        sceneName: (String) -> String,
        locale: Locale = Locale.getDefault(),
        totalLabel: String = "Total photos: %d",
        takenLabel: String = "Taken",
        noteLabel: String = "Note",
        favoriteLabel: String = "Favorite",
    ): String {
        val formatter = SimpleDateFormat("MMMM d, yyyy h:mm a", locale)
        val sorted = entries.sortedByDescending { it.takenAtEpochMillis }
        val lines = mutableListOf<String>()

        lines += "# Ranger Journal"
        lines += ""
        lines += String.format(locale, totalLabel, sorted.size)
        sorted.forEach { entry ->
            lines += ""
            lines += "## ${sceneName(entry.sceneId)}"
            lines += "- $takenLabel: ${formatter.format(Date(entry.takenAtEpochMillis))}"
            if (entry.caption.isNotBlank()) {
                lines += "- $noteLabel: ${entry.caption}"
            }
            if (entry.isFavorite) {
                lines += "- $favoriteLabel: yes"
            }
        }
        return lines.joinToString("\n")
    }

    fun exportFilename(childId: String, nowEpochMillis: Long): String =
        "ranger-journal-$childId-$nowEpochMillis.md"
}