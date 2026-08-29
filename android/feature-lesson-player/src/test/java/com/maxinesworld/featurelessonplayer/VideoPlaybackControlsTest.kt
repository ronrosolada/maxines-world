package com.maxinesworld.featurelessonplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackControlsTest {
    @Test
    fun `playback speeds include beginner slow speed`() {
        assertEquals(listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f), VIDEO_PLAYBACK_SPEEDS)
    }

    @Test
    fun `replay segment seeks back five seconds without going negative`() {
        assertEquals(7_000L, replaySegmentPosition(12_000L))
        assertEquals(0L, replaySegmentPosition(4_000L))
        assertTrue(replaySegmentPosition(0L) >= 0L)
    }
}
