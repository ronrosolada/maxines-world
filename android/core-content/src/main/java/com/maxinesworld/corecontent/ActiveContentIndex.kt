package com.maxinesworld.corecontent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans installed content packages under [filesDir]/content/active/
 * and provides a lessonId → filePath index.
 *
 * Indexes lessons under BOTH their native ID (e.g., "english-g3-q2-w01-d01")
 * and any derivable old-format ID (e.g., "eng-g3-m01-l01") so the NavGraph
 * can find synced content regardless of which format it requests.
 */
@Singleton
class ActiveContentIndex @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private var lessonIndex: Map<String, String> = emptyMap()
    private var lastScanTime: Long = 0L

    /** Short-code → full subject name mapping for building old-format aliases. */
    private val fullToShort = mapOf(
        "english" to "eng", "filipino" to "fil", "mathematics" to "math",
        "science" to "sci", "makabansa" to "mkb", "history" to "mkb", "gmrc" to "gmrc"
    )

    /**
     * Return the absolute file path for a lesson ID from installed content,
     * or null if not found. Re-scans every 30 seconds.
     */
    fun resolveLessonPath(lessonId: String): String? {
        if (System.currentTimeMillis() - lastScanTime > 30_000L) rebuildIndex()
        return lessonIndex[lessonId]
    }

    fun refresh() { rebuildIndex() }
    fun installedLessonCount(): Int = lessonIndex.size

    private fun rebuildIndex() {
        val newIndex = mutableMapOf<String, String>()
        val activeRoot = File(context.filesDir, "content/active")
        if (!activeRoot.isDirectory) {
            lessonIndex = newIndex
            lastScanTime = System.currentTimeMillis()
            return
        }

        activeRoot.listFiles()?.forEach { packageDir ->
            if (!packageDir.isDirectory) return@forEach
            packageDir.listFiles()?.forEach { versionDir ->
                if (!versionDir.isDirectory) return@forEach
                val manifestFile = File(versionDir, "package.json")
                if (!manifestFile.exists()) return@forEach

                try {
                    val manifest = json.decodeFromString<PackageManifest>(manifestFile.readText())
                    val lessonsDir = File(versionDir, "lessons")
                    if (!lessonsDir.isDirectory) return@forEach

                    manifest.lessonIds.forEach { lid ->
                        val lessonFile = File(lessonsDir, "$lid.json")
                        if (!lessonFile.exists()) return@forEach
                        val path = lessonFile.absolutePath

                        // Index under native ID
                        newIndex[lid] = path

                        // Also index under all derivable alias formats
                        deriveAliases(lid).forEach { alias -> newIndex[alias] = path }
                    }
                } catch (_: Exception) { /* corrupt package — skip */ }
            }
        }

        lessonIndex = newIndex
        lastScanTime = System.currentTimeMillis()
    }

    /**
     * Given a new-format ID like "english-g3-q2-w01-d01", derive all old-format
     * aliases that the NavGraph or LessonLoader might request:
     *   - "english-g3-m01-d01"  (month-day variant with full subject)
     *   - "eng-g3-m01-l01"      (short-code module-lesson variant)
     *
     * The module number is derived from the package's position among
     * installed packages for the same subject — see [buildModuleMapping].
     */
    private fun deriveAliases(newId: String): List<String> {
        val aliases = mutableListOf<String>()
        val parts = newId.split("-")  // e.g., ["english","g3","q2","w01","d01"]
        if (parts.size < 5) return aliases

        val subjectFull = parts[0]
        val shortCode = fullToShort[subjectFull] ?: subjectFull
        val quarter = parts[2].removePrefix("q")
        val week = parts[3].removePrefix("w")
        val day = parts[4].removePrefix("d")

        // Map (quarter, week) → module number using the package index
        val moduleNum = getModuleForQuarterWeek(subjectFull, quarter.toIntOrNull() ?: 1, week.toIntOrNull() ?: 1)

        val dayPadded = day.padStart(2, '0')
        val modPadded = moduleNum.toString().padStart(2, '0')

        // "english-g3-m01-d01" — full subject, month-day format
        aliases.add("$subjectFull-g3-m$modPadded-d$dayPadded")
        // "english-g3-m01-l01" — full subject, module-lesson format
        aliases.add("$subjectFull-g3-m$modPadded-l$dayPadded")
        // "eng-g3-m01-d01" — short code, month-day format
        aliases.add("$shortCode-g3-m$modPadded-d$dayPadded")
        // "eng-g3-m01-l01" — short code, module-lesson format (most common)
        aliases.add("$shortCode-g3-m$modPadded-l$dayPadded")

        return aliases
    }

    /**
     * Build module→(quarter,week) mapping from the actual installed package order.
     * Packages are scanned in filesystem order; for each subject, the Nth
     * distinct package gets module N (1-indexed).
     */
    private val moduleCache = mutableMapOf<String, Map<Pair<Int, Int>, Int>>()

    private fun getModuleForQuarterWeek(subject: String, quarter: Int, week: Int): Int {
        val cacheKey = subject
        val mapping = moduleCache.getOrPut(cacheKey) {
            buildModuleMapping(subject)
        }
        return mapping[Pair(quarter, week)] ?: 1
    }

    private fun buildModuleMapping(subject: String): Map<Pair<Int, Int>, Int> {
        val result = mutableMapOf<Pair<Int, Int>, Int>()
        val activeRoot = File(context.filesDir, "content/active")
        if (!activeRoot.isDirectory) return result

        val subjectPackages = mutableListOf<Pair<Int, Int>>()  // (quarter, week)

        activeRoot.listFiles()?.forEach { pkgDir ->
            if (!pkgDir.isDirectory) return@forEach
            // Package ID format: g3-{subject}-q{Q}-w{WW}
            val parts = pkgDir.name.split("-")
            if (parts.size < 4 || parts[1] != subject) return@forEach
            val q = parts[2].removePrefix("q").toIntOrNull() ?: return@forEach
            val w = parts[3].removePrefix("w").toIntOrNull() ?: return@forEach
            subjectPackages.add(Pair(q, w))
        }

        // Sort by quarter then week, assign module numbers 1..N
        subjectPackages.sortWith(compareBy({ it.first }, { it.second }))
        subjectPackages.forEachIndexed { index, (q, w) ->
            result[Pair(q, w)] = index + 1
        }
        return result
    }
}

/** Minimal manifest read from package.json. */
@Serializable
private data class PackageManifest(
    val packageId: String,
    val lessonIds: List<String> = emptyList()
)
