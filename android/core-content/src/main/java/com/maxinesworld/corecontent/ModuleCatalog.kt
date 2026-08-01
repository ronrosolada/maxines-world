package com.maxinesworld.corecontent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One module of a subject's curriculum, e.g. "Quarter 2 · Week 4" (SLM)
 * or the legacy month-01 pack ("Module 1").
 */
@Serializable
data class ContentModule(
    val key: String,
    val title: String,
    val lessons: List<ContentModuleLesson>
) {
    val lessonCount: Int get() = lessons.size
}

/** A lesson inside a module — enough metadata for a picker row. */
@Serializable
data class ContentModuleLesson(
    val lessonId: String,
    val title: String,
    val day: Int,
    val estimatedMinutes: Int = 10
)

/**
 * Builds the subject → module → lesson hierarchy from the bundled
 * content-pack. Deterministic: grouping is derived from lesson IDs:
 *
 *   - legacy:  `{subject}-g3-m01-dNN`  → module key "m01" (month-01 pack)
 *   - SLM:     `{subject}-g3-qN-wNN-dMM` → module key "qN-wNN"
 *
 * Modules sort with the legacy pack first, then SLM modules in
 * (quarter, week) order. Lessons within a module sort by day.
 */
class ModuleCatalog(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val cache = mutableMapOf<String, List<ContentModule>>()

    /** All modules for a subject, in display order. Empty list if none found. */
    suspend fun modulesFor(subject: String): List<ContentModule> = withContext(Dispatchers.IO) {
        cache[subject]?.let { return@withContext it }

        val raw = runCatching {
            context.assets.list(LESSONS_DIR)
        }.getOrElse { null } ?: return@withContext emptyList()

        // lessonId -> (title, day) for every lesson in this subject
        data class LessonMeta(val lessonId: String, val title: String, val day: Int, val minutes: Int)

        val byModule = mutableMapOf<String, MutableList<LessonMeta>>()
        for (fileName in raw) {
            if (!fileName.endsWith(".json")) continue
            val lessonId = fileName.removeSuffix(".json")
            if (!lessonId.startsWith("$subject-g3-")) continue

            val moduleKey = ModuleIdRules.moduleKeyFor(lessonId) ?: continue
            val meta = runCatching {
                val rawText = context.assets.open("$LESSONS_DIR/$fileName")
                    .bufferedReader().use { it.readText() }
                val lesson = json.decodeFromString<Month1LessonShim>(rawText)
                LessonMeta(
                    lessonId = lessonId,
                    title = lesson.title,
                    day = ModuleIdRules.dayFor(lessonId),
                    minutes = lesson.estimatedMinutes
                )
            }.getOrNull() ?: continue
            byModule.getOrPut(moduleKey) { mutableListOf() }.add(meta)
        }

        val modules = byModule.map { (key, metas) ->
            ContentModule(
                key = key,
                title = ModuleIdRules.moduleTitle(key),
                lessons = metas.sortedBy { it.day }.map {
                    ContentModuleLesson(it.lessonId, it.title, it.day, it.minutes)
                }
            )
        }.sortedBy { ModuleIdRules.moduleSortRank(it.key) }

        cache[subject] = modules
        modules
    }

    /** Small decode shim — avoids pulling full Month1Lesson deserialization here. */
    @kotlinx.serialization.Serializable
    private data class Month1LessonShim(
        val title: String,
        val estimatedMinutes: Int = 10
    )

    private companion object {
        const val LESSONS_DIR = "content-pack/month-01/lessons"
    }
}

/**
 * Pure ID-parsing rules, separated from asset I/O so they are unit-testable.
 *
 *   - legacy:  `{subject}-g3-m01-dNN`  → module key "m01"
 *   - SLM:     `{subject}-g3-qN-wNN-dMM` → module key "qN-wNN"
 */
internal object ModuleIdRules {

    private val MODULE_ID_REGEX = Regex("""^[a-z-]+-g3-(m\d+|q\d-w\d+)-d\d+$""")
    private val DAY_REGEX = Regex("""-d(\d+)$""")

    fun moduleKeyFor(lessonId: String): String? {
        val m = MODULE_ID_REGEX.matchEntire(lessonId) ?: return null
        return m.groupValues[1]
    }

    fun dayFor(lessonId: String): Int {
        val m = DAY_REGEX.find(lessonId) ?: return 0
        return m.groupValues[1].toIntOrNull() ?: 0
    }

    fun moduleTitle(key: String): String = when {
        key == "m01" -> "Module 1"
        key.startsWith("q") -> {
            val parts = key.removePrefix("q").split("-w")
            val quarter = parts.getOrNull(0) ?: return key
            val weekRaw = parts.getOrNull(1) ?: return key
            // strip zero-padding: "04" → "4", keep "0" if truly zero
            val week = weekRaw.toIntOrNull()?.toString() ?: weekRaw
            "Quarter $quarter · Week $week"
        }
        else -> key
    }

    /** m01 sorts first (0), then SLM modules by (quarter*100 + week). */
    fun moduleSortRank(key: String): Int {
        if (key == "m01") return 0
        val parts = key.removePrefix("q").split("-w")
        val q = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val w = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return q * 100 + w
    }
}
