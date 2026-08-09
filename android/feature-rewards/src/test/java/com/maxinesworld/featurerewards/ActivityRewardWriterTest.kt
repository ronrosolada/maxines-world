package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityRewardWriterTest {
    @Test
    fun activityPawPrintUsesStableSourceKeyAndDoesNotDuplicate() = runTest {
        val rewardDao = mockk<RewardDao>()
        val captured = slot<RewardEntity>()
        coEvery { rewardDao.insertIgnoring(capture(captured)) } returnsMany listOf(1L, -1L)
        val writer = ActivityRewardWriter(rewardDao)

        assertTrue(writer.award("child-1", "lesson-1", "step-1"))
        assertFalse(writer.award("child-1", "lesson-1", "step-1"))

        assertEquals("activity:child-1:lesson-1:step-1", captured.captured.id)
        assertEquals(ActivityRewardWriter.ACTIVITY_PAW_TYPE, captured.captured.type)
        assertEquals(1, captured.captured.amount)
        coVerify(exactly = 2) { rewardDao.insertIgnoring(any()) }
    }

    @Test
    fun blankIdentifiersDoNotWriteAReward() = runTest {
        val rewardDao = mockk<RewardDao>(relaxed = true)
        val writer = ActivityRewardWriter(rewardDao)

        assertFalse(writer.award("", "lesson-1", "step-1"))
        assertFalse(writer.award("child-1", "", "step-1"))
        assertFalse(writer.award("child-1", "lesson-1", ""))
        coVerify(exactly = 0) { rewardDao.insertIgnoring(any()) }
    }
}
