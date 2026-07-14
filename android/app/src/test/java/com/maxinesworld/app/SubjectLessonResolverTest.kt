package com.maxinesworld.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubjectLessonResolverTest {
    @Test fun resolvesEveryVillageDestination() {
        assertEquals("english-g3-m01-d01", SubjectLessonResolver.resolve("english"))
        assertEquals("filipino-g3-m01-d01", SubjectLessonResolver.resolve("filipino"))
        assertEquals("mathematics-g3-m01-d01", SubjectLessonResolver.resolve("mathematics"))
        assertEquals("science-g3-m01-d01", SubjectLessonResolver.resolve("science"))
        assertEquals("mkb-g3-m01-l01", SubjectLessonResolver.resolve("history"))
        assertEquals("gmrc-g3-m01-l01", SubjectLessonResolver.resolve("gmrc"))
    }
    @Test fun neverFallsBackToWrongLesson() {
        assertNull(SubjectLessonResolver.resolve("unknown"))
    }
}
