package com.maxinesworld.corecontent

import android.content.Context
import com.maxinesworld.coremodel.DayManifest
import com.maxinesworld.coremodel.Month1Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class ContentLessonLoader(
    private val context: Context,
    private val activeContentIndex: ActiveContentIndex
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val lessonCache = ConcurrentHashMap<String, Month1Lesson>()
    private val manifestCache = ConcurrentHashMap<Int, DayManifest>()

    suspend fun loadLesson(lessonId: String): Month1Lesson? = withContext(Dispatchers.IO) {
        lessonCache[lessonId]?.let { return@withContext it }

        // Path 0: Check active (synced) content first
        val activePath = activeContentIndex.resolveLessonPath(lessonId)
        if (activePath != null) {
            val raw = try {
                java.io.File(activePath).readText()
            } catch (_: Exception) { null }
            if (raw != null) {
                val result = runCatching {
                    json.decodeFromString<Month1Lesson>(raw)
                }.getOrNull()
                if (result != null) {
                    lessonCache[lessonId] = result
                    return@withContext result
                }
            }
        }

        // Path 1-3: Try bundled assets
        val raw = tryPath(lessonId)
        if (raw == null) return@withContext null

        val result = runCatching {
            json.decodeFromString<Month1Lesson>(raw)
        }.onFailure {
            android.util.Log.e("ContentLoader", "JSON parse failed: ${it.message}", it)
        }.getOrNull()
        if (result != null) lessonCache[lessonId] = result
        result
    }

    /** Try to load a lesson JSON from assets, trying multiple path formats. */
    private fun tryPath(lessonId: String): String? {
        // Format 1: Month-based (month-01) — english-g3-m01-d01
        try {
            val path = "content-pack/month-01/lessons/$lessonId.json"
            android.util.Log.d("ContentLoader", "Trying: $path")
            val text = context.assets.open(path)
                .bufferedReader().use { it.readText() }
            android.util.Log.d("ContentLoader", "Found! ${text.length} chars")
            return text
        } catch (e: Exception) {
            android.util.Log.d("ContentLoader", "Not found at month-01: ${e.message}")
        }

        // Format 2: Quarter-week-based — e.g., english-g3-q2-w01-d01
        // Path: content-pack/grade-3/lessons/english-g3-q2-w01-d01.json
        try {
            return context.assets.open("content-pack/grade-3/lessons/$lessonId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {}

        // Format 3: Legacy ph-matatag path
        try {
            return context.assets.open("content/ph-matatag/grade-3/$lessonId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {}

        // Format 4: Bootstrap pack path derived from lesson ID
        // e.g. english-g3-m01-d01 → content/bootstrap/packs/g3-english-q1-w01/1/lessons/...
        //      gmrc-g3-q1-w01-d01 → content/bootstrap/packs/g3-gmrc-q1-w01/1/lessons/...
        bootstrapAssetPath(lessonId)?.let { path ->
            try {
                android.util.Log.d("ContentLoader", "Trying bootstrap: $path")
                return context.assets.open(path).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                android.util.Log.d("ContentLoader", "Bootstrap miss: ${e.message}")
            }
        }

        return null
    }

    /**
     * Map a lesson ID to its bundled bootstrap asset path, or null if unknown.
     */
    private fun bootstrapAssetPath(lessonId: String): String? {
        // quarter-week form: subject-g3-qN-wNN-dNN
        val qw = Regex(
            """^([a-z-]+)-g3-(q\d)-(w\d+)-d\d+$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(lessonId)
        if (qw != null) {
            val subject = normalizeSubject(qw.groupValues[1])
            val packId = "g3-$subject-${qw.groupValues[2].lowercase()}-${qw.groupValues[3].lowercase()}"
            return "content/bootstrap/packs/$packId/1/lessons/$lessonId.json"
        }

        // month-day form: subject-g3-mNN-dNN → approximate q1 week from day
        val md = Regex(
            """^([a-z-]+)-g3-m(\d+)-d(\d+)$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(lessonId)
        if (md != null) {
            val subject = normalizeSubject(md.groupValues[1])
            val month = md.groupValues[2].toInt()
            val day = md.groupValues[3].toInt()
            val quarter = when {
                month <= 2 -> "q1"
                month <= 4 -> "q2"
                month <= 6 -> "q3"
                else -> "q4"
            }
            // week within quarter: 5 school days per week
            val weekBase = if (month % 2 == 1) 1 else 5
            val week = weekBase + ((day - 1) / 5)
            val packId = "g3-$subject-$quarter-w${week.toString().padStart(2, '0')}"
            return "content/bootstrap/packs/$packId/1/lessons/$lessonId.json"
        }
        return null
    }

    private fun normalizeSubject(raw: String): String = when (raw.lowercase()) {
        "english", "eng" -> "english"
        "filipino", "fil" -> "filipino"
        "mathematics", "math" -> "mathematics"
        "science", "sci" -> "science"
        "makabansa", "mkb", "araling-panlipunan", "history" -> "makabansa"
        "gmrc", "values" -> "gmrc"
        else -> raw.lowercase()
    }

    suspend fun loadDayManifest(day: Int): DayManifest? = withContext(Dispatchers.IO) {
        require(day in 1..20)
        manifestCache.getOrPut(day) {
            runCatching {
                val padded = day.toString().padStart(2, '0')
                val raw = context.assets.open("content-pack/month-01/days/day-$padded.json")
                    .bufferedReader().use { it.readText() }
                json.decodeFromString<DayManifest>(raw)
            }.getOrNull()
        }
    }

    fun getAssetPath(assetId: String): String =
        "content-pack/month-01/assets/vectors/$assetId.svg"

    val subjectMapping = mapOf(
        "ENGLISH" to "english",
        "FILIPINO" to "filipino",
        "MATHEMATICS" to "mathematics",
        "SCIENCE" to "science",
        "ARALING_PANLIPUNAN" to "makabansa",
        "MAKABANSA" to "makabansa",
        "GMRC" to "gmrc"
    )

    fun toAppSubject(apiSubject: String): String = subjectMapping[apiSubject] ?: apiSubject.lowercase()
}
