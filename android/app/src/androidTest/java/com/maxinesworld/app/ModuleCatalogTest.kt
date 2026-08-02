package com.maxinesworld.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.corecontent.ModuleCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device contract for the subject → module → lesson hierarchy:
 * tapping a subject must produce a real module list from the bundled
 * pack, and each module must have loadable lessons.
 */
@RunWith(AndroidJUnit4::class)
class ModuleCatalogTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun mathematicsShowsModuleListNotSingleLesson() = runBlocking {
        val catalog = ModuleCatalog(context)
        val modules = catalog.modulesFor("mathematics")

        assertTrue("math must have multiple modules, got ${modules.size}", modules.size > 1)
        assertEquals("Milo's Equal-Groups Market", modules.first().title)
        assertTrue(modules.any { it.title == "Quarter 2 · Week 4" })
        assertTrue(modules.any { it.title == "Quarter 4 · Week 9" })
        // every module must carry its lessons
        modules.forEach { module ->
            assertTrue("module ${module.key} must have lessons", module.lessons.isNotEmpty())
        }
        // lessons must be day-ordered
        modules.forEach { module ->
            val days = module.lessons.map { it.day }
            assertEquals("module ${module.key} days must be ascending", days.sorted(), days)
        }
    }

    @Test
    fun englishHidesRedundantLegacyModule() = runBlocking {
        val catalog = ModuleCatalog(context)
        val modules = catalog.modulesFor("english")

        // Legacy Module 1 (20 day-lessons) duplicates SLM Q1 — must be hidden
        // so the child doesn't replay "Picture Detective" twice.
        assertTrue("english must not show legacy m01", modules.none { it.key == "m01" })
        assertTrue("english must still show SLM modules", modules.isNotEmpty())
        // Q1 SLM covers the same skills the legacy pack used to
        assertTrue(modules.any { it.title == "Quarter 1 · Week 1" })
    }

    @Test
    fun everyPlayroomSubjectHasModules() = runBlocking {
        val catalog = ModuleCatalog(context)
        listOf("english", "filipino", "mathematics", "science", "gmrc", "makabansa")
            .forEach { subject ->
                val modules = catalog.modulesFor(subject)
                assertTrue("$subject must have ≥1 module, got ${modules.size}", modules.isNotEmpty())
                modules.forEach { m ->
                    assertNotNull("module ${m.key} lessonId must exist", m.lessons.firstOrNull()?.lessonId)
                }
            }
    }
}
