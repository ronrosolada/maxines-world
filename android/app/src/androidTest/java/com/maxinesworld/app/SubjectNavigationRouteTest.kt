package com.maxinesworld.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubjectNavigationRouteTest {
    @Test
    fun subjectCardsOpenVideoLessons() {
        assertEquals(
            Routes.videoLibrary("child-1", "mathematics"),
            subjectCardDestination("child-1", "mathematics"),
        )
    }

    @Test
    fun subjectDestinationPreservesEncodedChildAndSubjectSegments() {
        assertEquals(
            "video_library/child%2F1?subject=araling-panlipunan",
            subjectCardDestination("child/1", "araling-panlipunan"),
        )
    }
}