package com.maxinesworld.corecontent

import com.maxinesworld.coremodel.Month1Lesson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the 100-lesson Month 1 content pack against schema drift.
 *
 * The lesson files live in the :app module's assets. This test reads them from
 * the filesystem so it can run as a plain JVM unit test.
 */
class ContentPackIntegrityTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val subjects = listOf(
        "english",
        "filipino",
        "mathematics",
        "science",
        "araling-panlipunan"
    )

    private val expectedTypeSequence = listOf(
        "ANIMATED_EXPLANATION",
        "HOTSPOT_IMAGE",
        "SORT_AND_CLASSIFY",
        "MULTIPLE_CHOICE",
        "MATCHING_PAIRS",
        "SEQUENCE_BUILDER"
    )

    private fun lessonsDir(): File {
        // core-content/ -> android/ -> app/src/main/assets/...
        val candidates = listOf(
            File("../app/src/main/assets/content-pack/month-01/lessons"),
            File("app/src/main/assets/content-pack/month-01/lessons"),
            File("android/app/src/main/assets/content-pack/month-01/lessons")
        )
        val dir = candidates.firstOrNull { it.isDirectory }
        requireNotNull(dir) {
            "Could not locate content pack. Tried: " +
                candidates.joinToString { it.absolutePath }
        }
        return dir
    }

    private fun allLessonFiles(): List<File> =
        lessonsDir().listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?: emptyList()

    @Test
    fun `content pack contains the legacy 100 plus all converted SLM lessons`() {
        val names = allLessonFiles().map { it.name }
        // Legacy hand-authored month-01 pack: 5 subjects × 20 days = 100
        assertEquals(100, names.count { "-g3-m01-d" in it })
        // Converted SLM lessons: 229 across 6 subjects (q-format IDs)
        assertEquals(229, names.count { "-g3-q" in it })
        assertEquals(329, names.size)
    }

    @Test
    fun `every subject has all twenty days`() {
        val names = allLessonFiles().map { it.name }.toSet()
        val missing = mutableListOf<String>()
        for (subject in subjects) {
            for (day in 1..20) {
                val expected = "%s-g3-m01-d%02d.json".format(subject, day)
                if (expected !in names) missing += expected
            }
        }
        assertTrue("Missing lesson files: $missing", missing.isEmpty())
    }

    @Test
    fun `gmrc and makabansa have playable converted lessons`() {
        val gmrcFiles = allLessonFiles().filter { it.name.startsWith("gmrc-g3-q") }
        val makabansaFiles = allLessonFiles().filter { it.name.startsWith("makabansa-g3-q") }
        assertTrue("gmrc must have converted lessons, got ${gmrcFiles.size}", gmrcFiles.size >= 20)
        assertTrue("makabansa must have converted lessons, got ${makabansaFiles.size}", makabansaFiles.size >= 20)
    }

    @Test
    fun `every lesson parses into Month1Lesson`() {
        val failures = mutableListOf<String>()
        for (file in allLessonFiles()) {
            runCatching { json.decodeFromString<Month1Lesson>(file.readText()) }
                .onFailure { failures += "${file.name}: ${it.message}" }
        }
        assertTrue("Lessons failed to parse:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `every lesson has the six expected activities in order`() {
        val failures = mutableListOf<String>()
        for (file in allLessonFiles()) {
            val lesson = json.decodeFromString<Month1Lesson>(file.readText())
            val types = lesson.activities.map { it.type }
            if (types != expectedTypeSequence) {
                failures += "${file.name}: expected $expectedTypeSequence but was $types"
            }
        }
        assertTrue("Activity sequence drift:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `every activity id is unique within its lesson`() {
        val failures = mutableListOf<String>()
        for (file in allLessonFiles()) {
            val lesson = json.decodeFromString<Month1Lesson>(file.readText())
            val ids = lesson.activities.map { it.activityId }
            if (ids.size != ids.toSet().size) failures += "${file.name}: duplicate activity ids $ids"
        }
        assertTrue("Duplicate activity ids:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `every lesson id matches its filename`() {
        val failures = mutableListOf<String>()
        for (file in allLessonFiles()) {
            val lesson = json.decodeFromString<Month1Lesson>(file.readText())
            val expected = file.name.removeSuffix(".json")
            if (lesson.lessonId != expected) {
                failures += "${file.name}: lessonId=${lesson.lessonId}"
            }
        }
        assertTrue("lessonId/filename mismatch:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `activity content payloads have the fields the renderers require`() {
        val failures = mutableListOf<String>()

        for (file in allLessonFiles()) {
            val lesson = json.decodeFromString<Month1Lesson>(file.readText())
            for (activity in lesson.activities) {
                val id = "${file.name}/${activity.activityId}"
                val content = activity.content

                if (content == null) {
                    failures += "$id: content is null"
                    continue
                }

                when (activity.type) {
                    "ANIMATED_EXPLANATION" -> {
                        val prim = content as? JsonPrimitive
                        if (prim == null || prim.content.isBlank()) {
                            failures += "$id: expected non-blank string content"
                        }
                    }

                    "HOTSPOT_IMAGE" -> {
                        val examples = (content as? JsonObject)?.get("examples")?.jsonArray
                        if (examples == null || examples.isEmpty()) {
                            failures += "$id: missing or empty 'examples'"
                        }
                    }

                    "SORT_AND_CLASSIFY" -> {
                        val obj = content as? JsonObject
                        val fits = obj?.get("fits")?.jsonArray
                        val doesNotFit = obj?.get("doesNotFit")?.jsonArray
                        if (fits == null || fits.isEmpty()) failures += "$id: missing 'fits'"
                        if (doesNotFit == null || doesNotFit.isEmpty()) failures += "$id: missing 'doesNotFit'"
                    }

                    "MULTIPLE_CHOICE" -> {
                        val obj = content as? JsonObject
                        val options = obj?.get("options")?.jsonArray
                        val correctIndex = obj?.get("correctIndex")?.jsonPrimitive?.content?.toIntOrNull()
                        if (options == null || options.size < 2) {
                            failures += "$id: needs at least 2 options"
                        } else if (correctIndex == null || correctIndex !in options.indices) {
                            failures += "$id: correctIndex $correctIndex out of range for ${options.size} options"
                        }
                    }

                    "MATCHING_PAIRS" -> {
                        val pairs = (content as? JsonObject)?.get("pairs")?.jsonArray
                        if (pairs == null || pairs.isEmpty()) {
                            failures += "$id: missing 'pairs'"
                        } else {
                            pairs.forEachIndexed { i, element ->
                                val obj = element.jsonObject
                                if (obj["left"] == null) failures += "$id: pair $i missing 'left'"
                                if (obj["right"] == null) failures += "$id: pair $i missing 'right'"
                            }
                        }
                    }

                    "SEQUENCE_BUILDER" -> {
                        val steps = (content as? JsonObject)?.get("steps")?.jsonArray
                        if (steps == null || steps.size < 2) {
                            failures += "$id: needs at least 2 steps"
                        }
                    }
                }
            }
        }

        assertTrue("Content payload problems:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `every lesson declares a five item assessment`() {
        val failures = mutableListOf<String>()
        for (file in allLessonFiles()) {
            val lesson = json.decodeFromString<Month1Lesson>(file.readText())
            val assessment = lesson.assessment
            if (assessment == null) {
                failures += "${file.name}: no assessment block"
            } else if (assessment.items.size != assessment.itemCount) {
                failures += "${file.name}: itemCount=${assessment.itemCount} but items=${assessment.items.size}"
            }
        }
        assertTrue("Assessment problems:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
