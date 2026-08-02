package com.maxinesworld.corecontent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

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
class ModuleCatalog @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val cache = mutableMapOf<String, List<ContentModule>>()
    private var legacyTitles: Map<String, String>? = null

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
                title = moduleTitleFor(subject, key),
                lessons = metas.sortedBy { it.day }.map {
                    ContentModuleLesson(it.lessonId, it.title, it.day, it.minutes)
                }
            )
        }.sortedBy { ModuleIdRules.moduleSortRank(it.key) }
            // English legacy Module 1 (20 day-lessons) is ~90% duplicated by the
            // SLM Q1 lessons (same skill map, same titles, newer format) — the
            // legacy pack was the original Q1 gap-filler and is now redundant.
            // Hide it so a child never plays "Picture Detective" twice.
            .filterNot { it.key == "m01" && subject == "english" }

        cache[subject] = modules
        modules
    }

    /**
     * Title for a module key. The legacy m01 pack gets its real story title
     * from the SLM source manifest (e.g. math → "Milo's Equal-Groups Market");
     * SLM modules get "Quarter N · Week M". Falls back to "Module 1" if the
     * manifest is missing or has no entry for this subject.
     */
    private fun moduleTitleFor(subject: String, key: String): String {
        if (key != "m01") return ModuleIdRules.moduleTitle(key)
        val titles = legacyTitles ?: loadLegacyTitles().also { legacyTitles = it }
        return titles[subject] ?: ModuleIdRules.moduleTitle(key)
    }

    /** Read the bundled SLM manifest once; map subject → legacy module title. */
    private fun loadLegacyTitles(): Map<String, String> {
        return runCatching {
            val text = context.assets.open(MANIFEST_PATH).bufferedReader().use { it.readText() }
            val manifest = json.decodeFromString<ManifestShim>(text)
            manifest.subjects.mapNotNull { (subject, info) ->
                val legacyModule = info.modules.firstOrNull { it.id.endsWith("-m01") }
                legacyModule?.let { subject to it.title }
            }.toMap()
        }.getOrElse { emptyMap() }
    }

    /** Small decode shim — avoids pulling full Month1Lesson deserialization here. */
    @kotlinx.serialization.Serializable
    private data class Month1LessonShim(
        val title: String,
        val estimatedMinutes: Int = 10
    )

    @kotlinx.serialization.Serializable
    private data class ManifestShim(
        val subjects: Map<String, ManifestSubjectShim> = emptyMap()
    )

    @kotlinx.serialization.Serializable
    private data class ManifestSubjectShim(
        val modules: List<ManifestModuleShim> = emptyList()
    )

    @kotlinx.serialization.Serializable
    private data class ManifestModuleShim(
        val id: String = "",
        val title: String = ""
    )

    private companion object {
        const val LESSONS_DIR = "content-pack/month-01/lessons"
        const val MANIFEST_PATH = "content/ph-matatag/grade-3/manifest.json"
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
