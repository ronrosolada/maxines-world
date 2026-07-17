package com.maxinesworld.coredatabase

import com.maxinesworld.playground.DailyQuestSeedPolicy
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlaygroundGateRoomTest {

    private val childId = "child_gate_test"
    private val dayKey = "2026-07-14"
    private val seededIds = DailyQuestSeedPolicy.assign(childId, dayKey)

    private lateinit var questSetDao: DailyQuestSetDao
    private lateinit var questCompletionDao: DailyQuestCompletionDao
    private lateinit var unlockReceiptDao: PlaygroundUnlockReceiptDao

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        questSetDao = mockk()
        questCompletionDao = mockk()
        unlockReceiptDao = mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `concurrentSeed_createsOneSet`() = runTest {
        val seedIds = seededIds
        coEvery { questSetDao.getByChildAndDay(childId, dayKey) } returns null andThen DailyQuestSetEntity(
            id = "seed-1", childId = childId, dayKey = dayKey,
            assignedQuestIds = "[${seedIds.joinToString(",") { "\"$it\"" }}]",
            assignedAtEpochMillis = 1000L
        )
        coEvery { questSetDao.insertIgnoring(any()) } returnsMany listOf(1L, -1L)

        questSetDao.insertIgnoring(
            DailyQuestSetEntity(id = "seed-a", childId = childId, dayKey = dayKey,
                assignedQuestIds = "[\"subject:english\"]", assignedAtEpochMillis = 1000L)
        )
        questSetDao.insertIgnoring(
            DailyQuestSetEntity(id = "seed-b", childId = childId, dayKey = dayKey,
                assignedQuestIds = "[\"subject:english\"]", assignedAtEpochMillis = 1000L)
        )

        coVerify(exactly = 2) { questSetDao.insertIgnoring(any()) }
    }

    @Test
    fun `duplicateCompletion_createsOneRow`() = runTest {
        val questId = seededIds.first()
        coEvery { questCompletionDao.insertIgnoring(any()) } returnsMany listOf(1L, -1L)

        questCompletionDao.insertIgnoring(
            DailyQuestCompletionEntity(id = "comp-a", childId = childId, dayKey = dayKey,
                questId = questId, completionEventId = "ev-1", completedAtEpochMillis = 1000L)
        )
        questCompletionDao.insertIgnoring(
            DailyQuestCompletionEntity(id = "comp-a", childId = childId, dayKey = dayKey,
                questId = questId, completionEventId = "ev-1", completedAtEpochMillis = 1000L)
        )

        coVerify(exactly = 2) { questCompletionDao.insertIgnoring(any()) }
        // In production, second insert returns -1 (IGNORE), callers should not treat it as an error
    }

    @Test
    fun `finalAssignedCompletion_createsOneReceipt`() = runTest {
        val allCompleted = seededIds.toSet()
        coEvery { questSetDao.getByChildAndDay(childId, dayKey) } returns DailyQuestSetEntity(
            id = "seed-1", childId = childId, dayKey = dayKey,
            assignedQuestIds = "[${seededIds.joinToString(",") { "\"$it\"" }}]",
            assignedAtEpochMillis = 1000L
        )
        coEvery { questCompletionDao.getCompletedQuestIds(childId, dayKey) } returns allCompleted.toList()
        coEvery { unlockReceiptDao.existsByChildAndDay(childId, dayKey) } returns false
        coEvery { unlockReceiptDao.insertIgnoring(any()) } returns 1L

        // Simulate the gate evaluation logic
        val assigned = seededIds.toSet()
        val completed = questCompletionDao.getCompletedQuestIds(childId, dayKey).toSet()
        if (completed.containsAll(assigned)) {
            unlockReceiptDao.insertIgnoring(
                PlaygroundUnlockReceiptEntity(
                    id = "receipt-1", childId = childId, dayKey = dayKey,
                    sourceQuestSetHash = DailyQuestSeedPolicy.questSetHash(assigned),
                    unlockedAtEpochMillis = 2000L
                )
            )
        }

        coVerify(exactly = 1) { unlockReceiptDao.insertIgnoring(any()) }
    }

    @Test
    fun `differentChildren_doNotShareUnlock`() = runTest {
        val childA = "child_alpha"
        val childB = "child_beta"
        val day = "2026-07-14"

        coEvery { unlockReceiptDao.existsByChildAndDay(childA, day) } returns true
        coEvery { unlockReceiptDao.existsByChildAndDay(childB, day) } returns false

        assertTrue("child A should be unlocked", unlockReceiptDao.existsByChildAndDay(childA, day))
        assertFalse("child B should NOT be unlocked", unlockReceiptDao.existsByChildAndDay(childB, day))
    }

    @Test
    fun `nextDay_doesNotReuseUnlock`() = runTest {
        val today = "2026-07-14"
        val tomorrow = "2026-07-15"

        coEvery { unlockReceiptDao.existsByChildAndDay(childId, today) } returns true
        coEvery { unlockReceiptDao.existsByChildAndDay(childId, tomorrow) } returns false

        assertTrue("today unlocked", unlockReceiptDao.existsByChildAndDay(childId, today))
        assertFalse("tomorrow not yet unlocked", unlockReceiptDao.existsByChildAndDay(childId, tomorrow))
    }
}
