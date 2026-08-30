package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.CheckpointFeedbackLadder
import com.maxinesworld.coremodel.CheckpointOption
import com.maxinesworld.coremodel.CheckpointType
import com.maxinesworld.coremodel.VideoCheckpointItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCheckpointRuntimeTest {
    private val checkpoints = listOf(
        checkpoint("cp-2", 2_000),
        checkpoint("cp-1", 1_000),
    )

    @Test
    fun `runtime pauses at checkpoints in playback order and only once`() {
        val runtime = VideoCheckpointRuntime(checkpoints)

        assertNull(runtime.check(999))
        assertEquals("cp-1", runtime.check(1_000)?.checkpointId)
        assertEquals("cp-1", runtime.activeCheckpoint?.checkpointId)
        assertNull(runtime.check(2_500))

        runtime.submit("a")
        runtime.acknowledge()
        assertNull(runtime.activeCheckpoint)
        assertEquals("cp-2", runtime.check(2_500)?.checkpointId)
        runtime.submit("a")
        runtime.acknowledge()

        assertNull(runtime.check(9_000))
        assertEquals(setOf("cp-1", "cp-2"), runtime.completedCheckpointIds)
    }

    @Test
    fun `answer cannot be acknowledged until it is submitted`() {
        val runtime = VideoCheckpointRuntime(checkpoints)
        runtime.check(1_000)

        assertFalse(runtime.acknowledge())
        assertFalse(runtime.submit(null))
        assertTrue(runtime.submit("a"))
        assertTrue(runtime.answerIsCorrect)
        assertTrue(runtime.acknowledge())
    }

    @Test
    fun `wrong attempts reveal tiered hints in authored order`() {
        val runtime = VideoCheckpointRuntime(checkpoints)
        runtime.check(1_000)

        assertFalse(runtime.submit("b"))
        assertEquals("clue", runtime.visibleHint)
        assertFalse(runtime.submit("b"))
        assertEquals("worked", runtime.visibleHint)
        assertFalse(runtime.submit("b"))
        assertEquals("prereq", runtime.visibleHint)
    }

    private fun checkpoint(id: String, positionMs: Long) = VideoCheckpointItem(
        checkpointId = id,
        positionMs = positionMs,
        type = CheckpointType.QUICK_CHECK,
        prompt = "Question?",
        options = listOf(CheckpointOption("a", "Correct"), CheckpointOption("b", "Try again")),
        correctOptionId = "a",
        feedbackLadder = CheckpointFeedbackLadder("clue", "worked", "prereq"),
    )
}
