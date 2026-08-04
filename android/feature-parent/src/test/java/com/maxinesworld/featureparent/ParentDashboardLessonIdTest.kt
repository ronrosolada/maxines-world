package com.maxinesworld.featureparent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParentDashboardLessonIdTest {
    @Test
    fun `full curriculum subject ids map to catalog subjects`() {
        assertEquals("english", subjectKeyForLessonId("english-g3-q1-w01-d01"))
        assertEquals("mathematics", subjectKeyForLessonId("mathematics-g3-m01-d01"))
        assertEquals("araling-panlipunan", subjectKeyForLessonId("araling-panlipunan-g3-q2-w04-d01"))
    }

    @Test
    fun `legacy abbreviations remain compatible`() {
        assertEquals("english", subjectKeyForLessonId("eng-g3-m01-d01"))
        assertEquals("filipino", subjectKeyForLessonId("fil-g3-m01-d01"))
        assertEquals("makabansa", subjectKeyForLessonId("mkb-g3-m01-d01"))
    }

    @Test
    fun `unknown lesson id has no subject mapping`() {
        assertNull(subjectKeyForLessonId("not-a-lesson-id"))
    }
}
