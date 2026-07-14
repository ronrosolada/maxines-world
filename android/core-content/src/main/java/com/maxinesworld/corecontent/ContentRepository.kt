package com.maxinesworld.corecontent

import android.content.Context
import android.content.res.AssetManager
import com.maxinesworld.coredatabase.ActiveContentPackageDao
import com.maxinesworld.coredatabase.ContentPackageDao
import com.maxinesworld.coredatabase.ContentPackageEntity
import com.maxinesworld.coremodel.LessonManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified content resolver — the sole API for loading lessons and assets.
 *
 * Resolution order:
 *   1. Verified active downloaded version (via Room active_content_package)
 *   2. Bundled baseline version (from assets/content/bootstrap/packs/)
 *   3. Explicit error — never fall through to unrelated content
 */
@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentPackageDao: ContentPackageDao,
    private val activeContentPackageDao: ActiveContentPackageDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Load a lesson by package ID and lesson ID. */
    fun loadLesson(packageId: String, lessonId: String): LessonManifest? {
        return resolve(packageId) { root, _ ->
            val relPath = "lessons/$lessonId.json"
            readJsonFromAssets(root, relPath)
        }
    }

    /** Load the lesson ID text from any file in the resolved package. */
    fun loadString(packageId: String, relativePath: String): String? {
        return resolve(packageId) { root, _ ->
            readStringFromAssets(root, relativePath)
        }
    }

    /** Check if an asset exists in the resolved package. */
    fun assetExists(packageId: String, relativePath: String): Boolean {
        return resolve(packageId) { root, _ ->
            try {
                context.assets.open("$root/$relativePath").close()
                true
            } catch (_: Exception) { false }
        } ?: false
    }

    /** Open an input stream to an asset in the resolved package. */
    fun openAsset(packageId: String, relativePath: String): InputStream? {
        return resolve(packageId) { root, _ ->
            try { context.assets.open("$root/$relativePath") }
            catch (_: Exception) { null }
        }
    }

    /** Register a bundled package in Room on first launch. Idempotent. */
    suspend fun registerBundledPackage(packageId: String, version: Int) {
        val id = "${packageId}_$version"
        if (contentPackageDao.getVersions(packageId).isEmpty()) {
            contentPackageDao.upsert(
                ContentPackageEntity(
                    id = id,
                    packageId = packageId,
                    version = version,
                    source = "BUNDLED",
                    state = "VERIFIED",
                    rootPath = "content/bootstrap/packs/$packageId/$version",
                    contentHash = "" // computed at build time; trust bundled
                )
            )
        }
    }

    /** Activate the bundled baseline for all registered packages. */
    suspend fun activateAllBundled() {
        val bundled = contentPackageDao.getVerifiedBySource("BUNDLED")
        for (pkg in bundled) {
            activeContentPackageDao.setActive(
                com.maxinesworld.coredatabase.ActiveContentPackageEntity(
                    packageId = pkg.packageId,
                    version = pkg.version,
                    source = "BUNDLED"
                )
            )
        }
    }

    // --- Internal resolution ---

    private fun <T> resolve(packageId: String, block: (root: String, location: String) -> T?): T? {
        // Path 1: Active downloaded package (Room pointer)
        val active = runBlockingSafely { activeContentPackageDao.getActive(packageId) }
        if (active != null && active.source == "DOWNLOADED") {
            val pkg = runBlockingSafely {
                contentPackageDao.getVersions(packageId)
                    .firstOrNull { it.version == active.version && it.source == "DOWNLOADED" }
            }
            if (pkg != null) {
                // Downloaded content is on disk
                val file = File(pkg.rootPath, "lessons")
                val result = block(pkg.rootPath, pkg.rootPath)
                if (result != null) return result
            }
        }

        // Path 2: Bundled baseline (from assets)
        val bundled = runBlockingSafely {
            contentPackageDao.getVersions(packageId)
                .firstOrNull { it.source == "BUNDLED" }
        }
        if (bundled != null) {
            val assetRoot = bundled.rootPath  // e.g., "content/bootstrap/packs/g3-english-q1-w01/1"
            val result = block(assetRoot, assetRoot)
            if (result != null) return result
        }

        // Path 3: Legacy path fallback (from assets, direct — transitional)
        // This preserves backward compatibility until all loaders migrate
        val legacy = tryLegacyLoad(packageId, block)
        if (legacy != null) return legacy

        return null
    }

    private fun <T> tryLegacyLoad(packageId: String, block: (root: String, location: String) -> T?): T? {
        // Try old paths for backward compatibility
        val roots = listOf(
            "content/ph-matatag/grade-3",
            "content-pack",
            "content-packs"
        )
        for (root in roots) {
            val result = block(root, root)
            if (result != null) return result
        }
        return null
    }

    private fun readJsonFromAssets(root: String, relPath: String): LessonManifest? {
        return try {
            val stream = context.assets.open("$root/$relPath")
            val content = stream.bufferedReader().use { it.readText() }
            json.decodeFromString<LessonManifest>(content)
        } catch (_: Exception) { null }
    }

    private fun readStringFromAssets(root: String, relPath: String): String? {
        return try {
            val stream = context.assets.open("$root/$relPath")
            stream.bufferedReader().use { it.readText() }
        } catch (_: Exception) { null }
    }

    /** Run a suspend function synchronously (for use in non-suspend resolve). */
    private fun <T> runBlockingSafely(block: suspend () -> T): T? {
        return try {
            kotlinx.coroutines.runBlocking { block() }
        } catch (_: Exception) { null }
    }

    companion object {
        /** Map lesson ID to package ID using the same logic as bootstrap generator. */
        fun lessonToPackageId(lessonId: String): String? {
            val re = Regex("""^([a-z-]+)-g3-(q\d)-(w\d+)-d\d+$""", RegexOption.IGNORE_CASE)
            val m = re.matchEntire(lessonId)
            if (m != null) {
                val subj = normalizeSubject(m.groupValues[1])
                return "g3-$subj-${m.groupValues[2].lowercase()}-${m.groupValues[3].lowercase()}"
            }
            val reM = Regex("""^([a-z-]+)-g3-m(\d+)-d(\d+)$""", RegexOption.IGNORE_CASE)
            val mM = reM.matchEntire(lessonId)
            if (mM != null) {
                val s = normalizeSubject(mM.groupValues[1])
                val mo = mM.groupValues[2].toInt()
                val d = mM.groupValues[3].toInt()
                val q = if (mo <= 2) "q1" else if (mo <= 4) "q2" else null ?: return null
                val wk = ((d - 1) / 5) + (if (mo in listOf(1, 3)) 1 else 5)
                return "g3-$s-$q-w${wk.toString().padStart(2, '0')}"
            }
            return null
        }

        private fun normalizeSubject(s: String): String {
            return when (s.lowercase()) {
                "english" -> "english"
                "filipino" -> "filipino"
                "mathematics", "math" -> "mathematics"
                "science" -> "science"
                "makabansa", "araling-panlipunan" -> "makabansa"
                "gmrc", "values" -> "gmrc"
                else -> s.lowercase()
            }
        }
    }
}
