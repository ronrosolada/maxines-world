package com.maxinesworld.featurelessonplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoQuestPlannerTest {

    private fun cand(mediaId: String, subject: String, seconds: Int) =
        VideoQuestPlanner.Candidate(mediaId = mediaId, subjectId = subject, durationSeconds = seconds)

    private fun totalOf(selected: List<String>, frontier: List<VideoQuestPlanner.Candidate>): Int =
        selected.sumOf { id -> frontier.first { it.mediaId == id }.durationSeconds }

    @Test
    fun `empty frontier produces empty selection`() {
        assertTrue(VideoQuestPlanner.select("c1", "2026-08-20", emptyList()).isEmpty())
    }

    @Test
    fun `two subjects totalling 32 minutes are both selected`() {
        val frontier = listOf(
            cand("math-ep1", "mathematics", 960),   // 16 min
            cand("eng-ep1", "english", 960),        // 16 min -> 32 min
            cand("sci-ep1", "science", 900),
        )
        val selected = VideoQuestPlanner.select("c1", "2026-08-20", frontier)
        val total = totalOf(selected, frontier)
        assertTrue(selected.size in 2..3)
        assertTrue("total $total must be in [1800,2400]", total in VideoQuestPlanner.MIN_SECONDS..VideoQuestPlanner.MAX_SECONDS)
        val subjects = frontier.filter { it.mediaId in selected }.map { it.subjectId }.distinct()
        assertTrue(subjects.size >= 2)
    }

    @Test
    fun `never exceeds 40 minutes - a video that would overflow is skipped`() {
        val frontier = listOf(
            cand("math-ep1", "mathematics", 1500),  // 25 min
            cand("eng-ep1", "english", 1200),       // 20 min
            cand("sci-ep1", "science", 600),        // 10 min
        )
        val selected = VideoQuestPlanner.select("c1", "2026-08-20", frontier)
        val total = totalOf(selected, frontier)
        assertTrue("selected size = ${selected.size}", selected.size == 2)
        assertTrue("total $total must not exceed 40m", total <= VideoQuestPlanner.MAX_SECONDS)
        // Avoiding overflow must not collapse cross-subject when a 3rd fits within bounds.
        val subjects = frontier.filter { it.mediaId in selected }.map { it.subjectId }.distinct()
        assertTrue(subjects.size >= 2)
    }

    @Test
    fun `three short subjects can fill a 30-40 minute quest`() {
        val frontier = listOf(
            cand("math-ep1", "mathematics", 700),
            cand("eng-ep1", "english", 650),
            cand("fil-ep1", "filipino", 600),       // 700+650+600 = 1950 (32.5 min)
            cand("sci-ep1", "science", 200),
        )
        val selected = VideoQuestPlanner.select("c1", "2026-08-20", frontier)
        val total = totalOf(selected, frontier)
        assertTrue("total $total", total in VideoQuestPlanner.MIN_SECONDS..VideoQuestPlanner.MAX_SECONDS)
        val subjects = frontier.filter { it.mediaId in selected }.map { it.subjectId }.distinct()
        assertTrue(subjects.size >= 2)
    }

    @Test
    fun `single available subject still yields a quest (cross-subject is best-effort)`() {
        val frontier = listOf(cand("math-ep1", "mathematics", 1800))
        val selected = VideoQuestPlanner.select("c1", "2026-08-20", frontier)
        assertEquals(listOf("math-ep1"), selected)
    }

    @Test
    fun `selection is deterministic for same child and day`() {
        val frontier = listOf(
            cand("math-ep1", "mathematics", 960),
            cand("eng-ep1", "english", 960),
            cand("sci-ep1", "science", 500),
        )
        val a = VideoQuestPlanner.select("c1", "2026-08-20", frontier)
        val b = VideoQuestPlanner.select("c1", "2026-08-20", frontier)
        assertEquals(a, b)
    }

    @Test
    fun `completion requires every selected video to be passed`() {
        val selected = listOf("math-ep1", "eng-ep1")
        assertTrue(!VideoQuestPlanner.isCompleted(selected, emptySet()))
        assertTrue(!VideoQuestPlanner.isCompleted(selected, setOf("math-ep1")))
        assertTrue(VideoQuestPlanner.isCompleted(selected, setOf("math-ep1", "eng-ep1")))
        assertEquals(1, VideoQuestPlanner.completedCount(selected, setOf("math-ep1")))
        assertEquals(2, VideoQuestPlanner.completedCount(selected, setOf("math-ep1", "eng-ep1")))
    }
}
