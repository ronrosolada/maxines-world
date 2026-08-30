package com.maxinesworld.featurelessonplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class LessonUiLocalizationTest {
    @Test
    fun `Filipino learning area codes select Filipino UI labels`() {
        listOf("fil", "filipino", "makabansa", "gmrc").forEach { code ->
            assertEquals("Magpatuloy", lessonUiText(code, "Continue", "Magpatuloy"))
        }
    }

    @Test
    fun `English learning area code keeps English UI labels`() {
        assertEquals("Continue", lessonUiText("english", "Continue", "Magpatuloy"))
    }
}
