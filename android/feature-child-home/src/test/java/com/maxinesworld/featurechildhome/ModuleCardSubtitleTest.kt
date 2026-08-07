package com.maxinesworld.featurechildhome

import com.maxinesworld.corecontent.ContentModule
import com.maxinesworld.corecontent.ContentModuleLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ModuleCardSubtitleTest {

    @Test
    fun `module subtitle does not use repeated first lesson title`() {
        val module = ContentModule(
            key = "q3-w06",
            title = "Quarter 3 · Week 6",
            lessons = listOf(
                ContentModuleLesson(
                    lessonId = "mathematics-g3-q3-w06-d01",
                    title = "Multiplication Builders",
                    day = 1,
                ),
                ContentModuleLesson(
                    lessonId = "mathematics-g3-q3-w06-d02",
                    title = "Multiplication Builders",
                    day = 2,
                ),
            ),
        )

        assertEquals("2 lessons", moduleCardSubtitle(module))
        assertFalse(moduleCardSubtitle(module).contains("Multiplication Builders"))
    }
}
