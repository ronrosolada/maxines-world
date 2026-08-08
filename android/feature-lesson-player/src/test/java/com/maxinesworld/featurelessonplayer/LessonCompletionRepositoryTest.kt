package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.LessonCompletionEntity
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RoomTransactionRunner
import com.maxinesworld.coremodel.LessonManifest
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.TreatPerks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonCompletionRepositoryTest {
    private class ImmediateRunner : RoomTransactionRunner(mockk<MaxinesDatabase>()) {
        override suspend fun <T> run(block: suspend () -> T): T = block()
    }

    private fun lesson() = LessonManifest(
        id = "english-lesson-1",
        schemaVersion = 1,
        subject = "english",
        moduleId = "g3-m01",
        title = "Lesson",
        objective = "Objective",
        guideCharacter = "Milo",
        estimatedMinutes = 5,
        skillIds = listOf("skill-1"),
    )

    private fun result(id: String, correct: Boolean = true) = ActivityResult(
        activityId = id,
        correct = correct,
        attempts = 1,
        hintsUsed = 0,
        responseTimeMs = 10,
    )

    private fun repository(
        completionDao: LessonCompletionDao,
        failureInjector: CompletionWriteFailureInjector = CompletionWriteFailureInjector(),
        ownedItems: List<String> = emptyList(),
        rewardInserts: MutableList<com.maxinesworld.coredatabase.RewardEntity>? = null,
    ): LessonCompletionRepository {
        val badgeAwarder = mockk<BadgeAwarder>()
        coEvery { badgeAwarder.loadBadgeCatalog() } returns emptyList()
        coEvery { badgeAwarder.recordLessonCompletion(any(), any(), any(), any()) } returns
            com.maxinesworld.featurerewards.ChallengeProgress()
        coEvery { badgeAwarder.recordFirstLessonCompletion(any(), any()) } returns null
        coEvery { badgeAwarder.getExpeditionProgress(any()) } returns
            com.maxinesworld.featurerewards.ChallengeProgress(english = true, completedCount = 7)

        val progressDao = mockk<ProgressEventDao>(relaxed = true)
        val rewardDao = mockk<RewardDao>(relaxed = true)
        if (rewardInserts != null) {
            coEvery { rewardDao.insertIgnoring(any()) } answers {
                rewardInserts += firstArg<com.maxinesworld.coredatabase.RewardEntity>()
                1L
            }
        }
        val inventoryDao = mockk<InventoryDao>(relaxed = true)
        coEvery { inventoryDao.getOwnedItemIds(any()) } returns ownedItems
        return LessonCompletionRepository(
            transactionRunner = ImmediateRunner(),
            progressEventDao = progressDao,
            rewardDao = rewardDao,
            inventoryDao = inventoryDao,
            lessonCompletionDao = completionDao,
            badgeAwarder = badgeAwarder,
            failureInjector = failureInjector,
        )
    }

    @Test
    fun `replay does not write a second completion or reward set`() = runTest {
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.exists("child-1", "english-lesson-1") } returnsMany listOf(false, true)
        coEvery { completionDao.countDistinctLessons("child-1") } returns 0
        coEvery { completionDao.insertIgnoring(any()) } returns 1L
        val repository = repository(completionDao)

        val first = repository.complete("child-1", lesson(), listOf(result("a1")))
        val replay = repository.complete("child-1", lesson(), listOf(result("a1")))

        assertTrue(first.completionInserted)
        assertTrue(replay.alreadyCompleted)
        // Replay must surface current expedition progress, not defaults.
        assertEquals(
            com.maxinesworld.featurerewards.ChallengeProgress(english = true, completedCount = 7),
            replay.expeditionProgress,
        )
        coVerify(exactly = 1) { completionDao.insertIgnoring(any()) }
    }

    @Test
    fun `first-attempt pass and post-retry pass are recorded distinctly`() = runTest {
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.exists(any(), any()) } returns false
        coEvery { completionDao.countDistinctLessons(any()) } returns 0
        val captured = mutableListOf<LessonCompletionEntity>()
        coEvery { completionDao.insertIgnoring(any()) } answers {
            captured += firstArg<LessonCompletionEntity>()
            1L
        }
        val repository = repository(completionDao)

        repository.complete("child-1", lesson(), listOf(result("a1")), passedOnFirstAttempt = true)
        repository.complete("child-2", lesson(), listOf(result("a1")), passedOnFirstAttempt = false)

        assertEquals(true, captured[0].passedOnFirstAttempt)
        assertEquals(false, captured[1].passedOnFirstAttempt)
    }

    @Test
    fun `failure after reward write escapes the transaction block for rollback`() = runTest {
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.exists(any(), any()) } returns false
        coEvery { completionDao.countDistinctLessons(any()) } returns 0
        coEvery { completionDao.insertIgnoring(any()) } returns 1L
        val failure = object : CompletionWriteFailureInjector() {
            override suspend fun after(stage: CompletionWriteStage) {
                if (stage == CompletionWriteStage.STARS_INSERTED) error("injected")
            }
        }
        val repository = repository(completionDao, failure)

        var thrown = false
        try {
            repository.complete("child-1", lesson(), listOf(result("a1")))
        } catch (error: IllegalStateException) {
            thrown = error.message == "injected"
        }

        assertTrue(thrown)
    }

    @Test
    fun `fish basket doubles stars and consumes the daily perk`() = runTest {
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.exists(any(), any()) } returns false
        coEvery { completionDao.countDistinctLessons(any()) } returns 0
        coEvery { completionDao.insertIgnoring(any()) } returns 1L
        val rewardInserts = mutableListOf<com.maxinesworld.coredatabase.RewardEntity>()
        val repository = repository(
            completionDao,
            ownedItems = listOf(TreatPerks.BASKET_ID),
            rewardInserts = rewardInserts,
        )

        val out = repository.complete("child-1", lesson(), listOf(result("a1")))

        // 100% accuracy → 3 base stars, doubled to 6; coins unchanged (10).
        assertEquals(6, out.starsEarned)
        assertEquals(10, out.coinsEarned)
        // Once-per-day PERK ledger row written.
        assertTrue(
            rewardInserts.any { it.type == "PERK" && it.metadata == TreatPerks.BASKET_ID }
        )
        assertEquals(6, rewardInserts.first { it.type == "STAR" }.amount)
    }

    @Test
    fun `cushion and bowl add stars and coins on every lesson`() = runTest {
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.exists(any(), any()) } returns false
        coEvery { completionDao.countDistinctLessons(any()) } returns 0
        coEvery { completionDao.insertIgnoring(any()) } returns 1L
        val rewardInserts = mutableListOf<com.maxinesworld.coredatabase.RewardEntity>()
        val repository = repository(
            completionDao,
            ownedItems = listOf(TreatPerks.CUSHION_ID, TreatPerks.BOWL_ID),
            rewardInserts = rewardInserts,
        )

        val out = repository.complete("child-1", lesson(), listOf(result("a1")))

        // 3 base stars + 1 cushion = 4; 10 coins + 1 bowl = 11.
        assertEquals(4, out.starsEarned)
        assertEquals(11, out.coinsEarned)
        // No PERK row for passive perks — they are always-on.
        assertTrue(rewardInserts.none { it.type == "PERK" })
    }
}
