package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RoomTransactionRunner
import com.maxinesworld.coremodel.LessonManifest
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.featurerewards.BadgeAwarder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
        return LessonCompletionRepository(
            transactionRunner = ImmediateRunner(),
            progressEventDao = progressDao,
            rewardDao = rewardDao,
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
}
