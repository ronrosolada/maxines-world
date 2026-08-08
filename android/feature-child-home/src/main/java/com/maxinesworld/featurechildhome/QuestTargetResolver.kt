package com.maxinesworld.featurechildhome

import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.corecontent.friendlyLessonTitleOf

/**
 * Resolves quest lesson ids to displayable [QuestTargetUi] (title + subject + module).
 * Pure helper so it is unit-testable without Room. The catalog scan is cached per subject
 * by [ModuleCatalog] already, so this is cheap after first call.
 */
object QuestTargetResolver {

    suspend fun resolve(
        assigned: List<String>,
        completed: Set<String>,
        catalog: ModuleCatalog,
    ): List<QuestTargetUi> {
        if (assigned.isEmpty()) return emptyList()
        // Build a lessonId -> friendly title index by scanning each subject once.
        // We don't know the subject of a lessonId for free if the catalog was
        // filtered by English's m01 hiding etc., so use subjectForLessonId first.
        val titleIndex = mutableMapOf<String, String>()
        val subjectsToScan = assigned.mapNotNull(::subjectForLessonId).toSet().ifEmpty {
            setOf("mathematics", "english", "science", "filipino", "makabansa", "gmrc")
        }
        for (subject in subjectsToScan) {
            runCatching {
                catalog.modulesFor(subject).forEach { mod ->
                    mod.lessons.forEach { lesson ->
                        titleIndex[lesson.lessonId] = lesson.title
                    }
                }
            }
        }
        return assigned.map { id ->
            val subject = subjectForLessonId(id) ?: lessonSubjectFromId(id)
            val display = subjectDisplayName(subject)
            val rawTitle = titleIndex[id]
            val title = when {
                rawTitle != null -> rawTitle // already friendly via ModuleCatalog
                else -> friendlyLessonTitleOf(id)
            }
            QuestTargetUi(
                lessonId = id,
                title = title,
                subject = subject,
                displaySubject = display,
                moduleKey = moduleKeyFor(id),
                isCompleted = id in completed,
            )
        }
    }

    /**
     * Derive the pack subject from a lesson id: `{subject}-g3-…`.
     * Normalizes `araling-panlipunan` → `makabansa` via [subjectForPack] so
     * the display name and module lookup match the Playroom's canonical subject.
     */
    internal fun subjectForLessonId(lessonId: String): String? {
        val dash = lessonId.indexOf("-g3-")
        if (dash <= 0) return subjectForPack(lessonId.substringBefore("-"))
        val raw = lessonId.substring(0, dash)
        // raw is e.g. "mathematics" or "araling-panlipunan"
        return subjectForPack(raw) ?: raw
    }

    private fun lessonSubjectFromId(lessonId: String): String {
        val dash = lessonId.indexOf("-g3-")
        return if (dash > 0) lessonId.substring(0, dash) else lessonId.substringBefore("-")
    }

    // Mirrors ModuleIdRules.moduleKeyFor without depending on its internal visibility.
    private val MODULE_ID_REGEX = Regex("""^[a-z-]+-g3-(m\\d+|q\\d-w\\d+)-d\\d+$""")
    private fun moduleKeyFor(lessonId: String): String? {
        val m = MODULE_ID_REGEX.matchEntire(lessonId) ?: return null
        return m.groupValues[1]
    }
}
