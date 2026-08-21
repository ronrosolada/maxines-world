package com.maxinesworld.coremodel

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
            cand("math-ep1", "mathematics", 960),
            cand("eng-ep1", "english", 960),
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
    fun `never exceeds 50 minutes - a video that would overflow is skipped`() {
        val frontier = listOf(
            cand("math-ep1", "mathematics", 1500),
            cand("eng-ep1", "english", 1200),
            cand("sci-ep1", "science", 600),
        )
        val selected = VideoQuestPlanner.select("c1", "2026-08-20", frontier)
        val total = totalOf(selected, frontier)
        assertTrue("selected size = ${selected.size}", selected.size == 2)
        assertTrue("total $total must not exceed 50m", total <= VideoQuestPlanner.MAX_SECONDS)
        val subjects = frontier.filter { it.mediaId in selected }.map { it.subjectId }.distinct()
        assertTrue(subjects.size >= 2)
    }

    @Test
    fun `three short subjects can fill a 30-50 minute quest`() {
        val frontier = listOf(
            cand("math-ep1", "mathematics", 700),
            cand("eng-ep1", "english", 650),
            cand("fil-ep1", "filipino", 600),
            cand("sci-ep1", "science", 200),
        )
        val selected = VideoQuestPlanner.select("c1", "2026-08-20", frontier)
        val total = totalOf(selected, frontier)
        assertTrue("total $total", total in VideoQuestPlanner.MIN_SECONDS..VideoQuestPlanner.MAX_SECONDS)
        val subjects = frontier.filter { it.mediaId in selected }.map { it.subjectId }.distinct()
        assertTrue(subjects.size >= 2)
    }

    @Test
    fun `single subject frontier is rejected because it cannot satisfy the contract`() {
        val selected = VideoQuestPlanner.select(
            "c1",
            "2026-08-20",
            listOf(cand("math-ep1", "mathematics", 1800)),
        )
        assertTrue(selected.isEmpty())
    }

    @Test
    fun `sparse frontier below 30 minutes produces no partial quest`() {
        val selected = VideoQuestPlanner.select(
            "c1",
            "2026-08-20",
            listOf(
                cand("math-ep1", "mathematics", 500),
                cand("eng-ep1", "english", 500),
            ),
        )
        assertTrue(selected.isEmpty())
    }

    @Test
    fun `overlong frontier with no valid combination produces no quest`() {
        val selected = VideoQuestPlanner.select(
            "c1",
            "2026-08-20",
            listOf(
                cand("math-ep1", "mathematics", 1700),
                cand("eng-ep1", "english", 1700),
                cand("sci-ep1", "science", 1700),
            ),
        )
        assertTrue(selected.isEmpty())
    }

    @Test
    fun `invalid frontier candidates do not create a mission`() {
        val selected = VideoQuestPlanner.select(
            "c1",
            "2026-08-20",
            listOf(
                cand("", "mathematics", 900),
                cand("eng-ep1", "english", 0),
                cand("sci-ep1", "science", -1),
            ),
        )
        assertTrue(selected.isEmpty())
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
