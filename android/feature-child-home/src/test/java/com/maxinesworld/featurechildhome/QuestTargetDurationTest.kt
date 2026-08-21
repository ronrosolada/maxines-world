package com.maxinesworld.featurechildhome

import com.maxinesworld.coremodel.MediaAsset
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestTargetDurationTest {
    @Test
    fun `duration formats as minutes and seconds`() {
        assertEquals("00:00", QuestTargetResolver.formatDuration(0))
        assertEquals("01:05", QuestTargetResolver.formatDuration(65))
        assertEquals("30:00", QuestTargetResolver.formatDuration(1800))
    }
}
