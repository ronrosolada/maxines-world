package com.maxinesworld.featurelessonplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonFeedbackLayoutTest {

    @Test
    fun `feedback reserves viewport space only while visible`() {
        assertEquals(0, LessonFeedbackLayout.bottomContentPaddingDp(showFeedback = false))

        val reserved = LessonFeedbackLayout.bottomContentPaddingDp(showFeedback = true)
        assertTrue("sticky feedback needs reserved scroll space", reserved > 0)
    }
}
