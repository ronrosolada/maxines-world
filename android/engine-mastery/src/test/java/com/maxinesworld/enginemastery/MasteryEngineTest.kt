package com.maxinesworld.enginemastery

import com.maxinesworld.coremodel.MasteryState
import com.maxinesworld.coremodel.ProgressEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MasteryEngineTest {

    private lateinit var engine: MasteryEngine

    @Before
    fun setup() {
        engine = MasteryEngine()
    }

    @Test
    fun `no events returns NOT_STARTED`() {
        assertEquals(MasteryState.NOT_STARTED, engine.computeMastery(emptyList()))
    }

    @Test
    fun `single low attempt returns INTRODUCED`() {
        val events = listOf(createEvent(accuracy = 0.5))
        assertEquals(MasteryState.INTRODUCED, engine.computeMastery(events))
    }

    @Test
    fun `2-4 good attempts returns PRACTICING`() {
        val events = (1..3).map { createEvent(accuracy = 0.85) }
        assertEquals(MasteryState.PRACTICING, engine.computeMastery(events))
    }

    @Test
    fun `10 perfect attempts returns MASTERED`() {
        val baseTime = System.currentTimeMillis()
        val dayMs = 86_400_000L
        // 5 events on day 1 with activity type A, 5 on day 2 with activity type B
        val events = listOf(
            *(1..5).map { createEvent(accuracy = 1.0, activityId = "lesson_mcq_v1", timestamp = baseTime) }.toTypedArray(),
            *(1..5).map { createEvent(accuracy = 1.0, activityId = "lesson_sort_v2", timestamp = baseTime + dayMs) }.toTypedArray(),
        )
        assertEquals(MasteryState.MASTERED, engine.computeMastery(events))
    }

    @Test
    fun `5-9 good attempts returns PROFICIENT`() {
        val events = (1..7).map { createEvent(accuracy = 0.85) }
        assertEquals(MasteryState.PROFICIENT, engine.computeMastery(events))
    }

    @Test
    fun `poor accuracy with many attempts returns NEEDS_REVIEW`() {
        val events = (1..10).map { createEvent(accuracy = 0.3) }
        assertEquals(MasteryState.NEEDS_REVIEW, engine.computeMastery(events))
    }

    @Test
    fun `next review days are correct per state`() {
        assertEquals(0, engine.nextReviewDays(MasteryState.NOT_STARTED))
        assertEquals(1, engine.nextReviewDays(MasteryState.INTRODUCED))
        assertEquals(3, engine.nextReviewDays(MasteryState.PRACTICING))
        assertEquals(7, engine.nextReviewDays(MasteryState.PROFICIENT))
        assertEquals(30, engine.nextReviewDays(MasteryState.MASTERED))
        assertEquals(1, engine.nextReviewDays(MasteryState.NEEDS_REVIEW))
    }

    private fun createEvent(
        accuracy: Double,
        activityId: String = "test_activity",
        timestamp: Long = System.currentTimeMillis(),
    ) = ProgressEvent(
        id = "ev_${System.nanoTime()}",
        childId = "test_child",
        skillId = "test_skill",
        lessonId = "test_lesson",
        activityId = activityId,
        eventType = "answer",
        accuracy = accuracy,
        timestamp = timestamp,
    )
}
