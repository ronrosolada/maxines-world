package com.maxinesworld.app

import com.maxinesworld.featurechildhome.subjectForPack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonRouteTest {

    private fun lessonsDir(): File {
        val candidates = listOf(
            File("src/main/assets/content-pack/month-01/lessons"),
            File("app/src/main/assets/content-pack/month-01/lessons"),
            File("android/app/src/main/assets/content-pack/month-01/lessons")
        )
        return candidates.first { it.isDirectory }
    }

    // ─── Supported subjects: deterministic mapping ────────────────────

    @Test
    fun `every supported subject maps deterministically`() {
        val supported = listOf(
            "english", "filipino", "mathematics", "science",
            "araling-panlipunan", "philippine-history", "makabansa", "heritage-harbor",
            "gmrc"
        )
        supported.forEach { subject ->
            val lessonId = lessonIdForSubject(subject)
            assertNotNull("subject '$subject' must map to a lesson", lessonId)
        }
    }

    @Test
    fun `english maps to the english lesson`() {
        assertEquals("english-g3-m01-d01", lessonIdForSubject("english"))
    }

    @Test
    fun `gmrc maps to a REAL gmrc lesson`() {
        val lessonId = lessonIdForSubject("gmrc")
        assertNotNull("gmrc must map to a lesson", lessonId)
        assertTrue("gmrc must map to gmrc content, got: $lessonId", lessonId!!.startsWith("gmrc"))
        // The mapped file must exist in the bundle (converted SLM content)
        val file = File(lessonsDir(), "$lessonId.json")
        assertTrue("gmrc-mapped lesson must exist in bundle: $file", file.isFile)
    }

    @Test
    fun `makabansa maps to a REAL makabansa lesson`() {
        val lessonId = lessonIdForSubject("makabansa")
        assertNotNull("makabansa must map to a lesson", lessonId)
        assertTrue("makabansa must map to makabansa content, got: $lessonId", lessonId!!.startsWith("makabansa"))
        assertTrue("makabansa-mapped lesson must exist in bundle", File(lessonsDir(), "$lessonId.json").isFile)
    }

    @Test
    fun `makabansa island resolves to the makabansa pack subject`() {
        assertEquals("makabansa", subjectForPack("makabansa"))
    }

    @Test
    fun `heritage aliases all map to araling-panlipunan`() {
        listOf("araling-panlipunan", "philippine-history", "heritage-harbor").forEach { alias ->
            assertEquals("alias '$alias' → AP lesson", "araling-panlipunan-g3-m01-d01", lessonIdForSubject(alias))
        }
    }

    // ─── Unknown subjects: explicit failure, NO silent fallback ───────

    @Test
    fun `unknown subject maps to null`() {
        assertNull(lessonIdForSubject("unknown-subject"))
        assertNull(lessonIdForSubject("math"))
        assertNull(lessonIdForSubject("history"))
        assertNull(lessonIdForSubject(""))
    }

    @Test
    fun `unknown subject never silently opens english`() {
        val lessonId = lessonIdForSubject("unknown-subject")
        assertNull("unknown subject must not silently map to English", lessonId)
    }

    // ─── Bundle integrity: every mapped lesson is actually loadable ───

    @Test
    fun `every non-gmrc mapped lesson file exists in the month pack`() {
        val dir = lessonsDir()
        val subjects = listOf(
            "english", "filipino", "mathematics", "science",
            "araling-panlipunan", "philippine-history", "makabansa", "heritage-harbor"
        )
        val broken = subjects.filter { subject ->
            val lessonId = lessonIdForSubject(subject) ?: return@filter true
            !File(dir, "$lessonId.json").isFile
        }
        assertTrue("Subjects routing to nonexistent lessons: $broken", broken.isEmpty())
    }
}
