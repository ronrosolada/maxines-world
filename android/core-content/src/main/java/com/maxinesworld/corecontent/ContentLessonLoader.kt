package com.maxinesworld.corecontent

import android.content.Context
import com.maxinesworld.coremodel.DayManifest
import com.maxinesworld.coremodel.Month1Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads the canonical bundled lesson pack.
 *
 * Single resolution path: `content-pack/month-01/lessons/{lessonId}.json`.
 * The legacy ph-matatag fallback tree, the retired content-sync active path,
 * and the unapproved pilot pack were removed from the APK (2026-08-07,
 * external review C3), so no fallback resolution exists here and none is
 * wanted: every lesson that ships must be the canonical, educator-reviewed
 * file. [parseBundledLesson] additionally rejects any lesson whose
 * `releaseStatus` is not `RELEASED`, so an unreleased file can never reach
 * the child even if it is ever added back to assets (spec CH-02).
 */
class ContentLessonLoader(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val lessonCache = ConcurrentHashMap<String, Month1Lesson>()
    private val manifestCache = ConcurrentHashMap<Int, DayManifest>()

    suspend fun loadLesson(lessonId: String): Month1Lesson? = withContext(Dispatchers.IO) {
        lessonCache[lessonId]?.let { return@withContext it }

        val raw = try {
            context.assets.open("content-pack/month-01/lessons/$lessonId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
        if (raw == null) return@withContext null

        val result = parseBundledLesson(raw, json)
        if (result != null) lessonCache[lessonId] = result
        result
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

    /**
     * Manifest subject → app subject key.
     *
     * Araling Panlipunan deliberately maps to MAKABANSA: Makabansa is the
     * Matatag-curriculum successor of AP, and the 20 legacy AP lessons ship
     * under the Makabansa collection on the Playroom home (product decision
     * 2026-08-06; audit A1). Both uppercase (legacy m01 pack) and lowercase
     * (SLM packs) forms are folded in so credit, badges, and progress all
     * land on the same subject regardless of authoring format.
     */
    val subjectMapping = mapOf(
        "ENGLISH" to "english",
        "FILIPINO" to "filipino",
        "MATHEMATICS" to "mathematics",
        "SCIENCE" to "science",
        "ARALING_PANLIPUNAN" to "makabansa",
        "araling_panlipunan" to "makabansa",
        "MAKABANSA" to "makabansa",
    )

    fun toAppSubject(apiSubject: String): String = subjectMapping[apiSubject] ?: apiSubject.lowercase()
}

/**
 * Decode a bundled lesson and reject any file that is not release-approved.
 *
 * A lesson whose `releaseStatus` is not `RELEASED` (for example
 * `REQUIRES_EDUCATOR_REVIEW`) resolves to `null` exactly as if it were
 * missing, so the lesson-load error and retry surface handles it and a
 * partially loaded lesson is never surfaced (spec CH-02).
 */
internal fun parseBundledLesson(raw: String, json: Json): Month1Lesson? {
    val lesson = runCatching { json.decodeFromString<Month1Lesson>(raw) }.getOrNull() ?: return null
    return if (lesson.releaseStatus == "RELEASED") lesson else null
}
