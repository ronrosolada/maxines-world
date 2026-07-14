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

        return null
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
        "ARALING_PANLIPUNAN" to "makabansa"
    )

    fun toAppSubject(apiSubject: String): String = subjectMapping[apiSubject] ?: apiSubject.lowercase()
}
