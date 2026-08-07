package com.maxinesworld.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies the subject → module → lesson hierarchy against the REAL
 * bundled content pack, using the same ID rules as ModuleCatalog.
 *
 * This pins the user-facing promise: tapping "Number Fun" must show a
 * list of math modules, never dump straight into a lesson.
 */
class ModuleStructureTest {

    private fun lessonsDir(): File {
        val candidates = listOf(
            File("src/main/assets/content-pack/month-01/lessons"),
            File("app/src/main/assets/content-pack/month-01/lessons"),
            File("android/app/src/main/assets/content-pack/month-01/lessons")
        )
        return candidates.first { it.isDirectory }
    }

    private fun lessonIds(subject: String): List<String> =
        lessonsDir().listFiles { f -> f.name.endsWith(".json") && f.name.startsWith("$subject-g3-") }
            .map { it.name.removeSuffix(".json") }
            .sorted()

    private fun moduleOf(lessonId: String): String? {
        val m = Regex("""^[a-z-]+-g3-(m\d+|q\d-w\d+)-d\d+$""").matchEntire(lessonId)
        return m?.groupValues?.get(1)
    }

    @Test
    fun `mathematics has multiple modules, not a single dump`() {
        val ids = lessonIds("mathematics")
        assertTrue("math must have lessons, got ${ids.size}", ids.isNotEmpty())

        val modules = ids.mapNotNull { moduleOf(it) }.distinct().sorted()
        // 1 legacy (m01) + 9 SLM quarter-week modules
        assertEquals("expected 10 distinct math modules", 10, modules.size)
        assertEquals("m01", modules.first())
        assertTrue("must include q2-w04", modules.contains("q2-w04"))
        assertTrue("must include q4-w09", modules.contains("q4-w09"))
    }

    @Test
    fun `every subject produces at least one module`() {
        listOf("english", "filipino", "mathematics", "science", "gmrc", "makabansa")
            .forEach { subject ->
                val ids = lessonIds(subject)
                assertTrue("$subject must have lessons", ids.isNotEmpty())
                val modules = ids.mapNotNull { moduleOf(it) }.distinct()
                assertTrue("$subject must have ≥1 module, got ${modules.size}", modules.isNotEmpty())
            }
    }

    @Test
    fun `every lesson resolves to a module`() {
        lessonsDir().listFiles { f -> f.name.endsWith(".json") }
            .map { it.name.removeSuffix(".json") }
            .forEach { lessonId ->
                val module = moduleOf(lessonId)
                assertTrue("unparseable lessonId: $lessonId", module != null)
            }
    }

    @Test
    fun `legacy araling-panlipunan resolves to its own module`() {
        val ids = lessonIds("araling-panlipunan")
        assertTrue(ids.isNotEmpty())
        assertEquals("m01", moduleOf(ids.first()))
    }
}
