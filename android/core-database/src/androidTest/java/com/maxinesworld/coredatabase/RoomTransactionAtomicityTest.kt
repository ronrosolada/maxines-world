package com.maxinesworld.coredatabase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTransactionAtomicityTest {
    private lateinit var db: MaxinesDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun failureAfterEachCompletionWriteRollsBackAllRows() = runBlocking {
        val runner = RoomTransactionRunner(db)
        val stages = CompletionStage.values()

        stages.forEach { stage ->
            db.clearAllTables()
            try {
                runner.run {
                    db.lessonCompletionDao().insertIgnoring(
                        LessonCompletionEntity(
                            id = "completion-$stage",
                            childId = "child-1",
                            lessonId = "lesson-1",
                            attemptId = "completion-$stage",
                            accuracy = 1.0,
                            completedAtEpochMillis = 1L,
                        )
                    )
                    failAt(stage, CompletionStage.COMPLETION)

                    db.progressEventDao().insertIgnoring(
                        ProgressEventEntity(
                            id = "progress-$stage",
                            childId = "child-1",
                            skillId = "skill-1",
                            lessonId = "lesson-1",
                            activityId = "activity-1",
                            eventType = "activity_result",
                            accuracy = 1.0,
                            attempts = 1,
                            hintsUsed = 0,
                            responseTimeMs = 1L,
                        )
                    )
                    failAt(stage, CompletionStage.PROGRESS)

                    db.rewardDao().insertIgnoring(
                        RewardEntity(
                            id = "star-$stage",
                            childId = "child-1",
                            type = "STAR",
                            subject = "english",
                            amount = 1,
                            metadata = "test",
                        )
                    )
                    failAt(stage, CompletionStage.STARS)
                    db.rewardDao().insertIgnoring(
                        RewardEntity(
                            id = "coin-$stage",
                            childId = "child-1",
                            type = "COIN",
                            subject = "english",
                            amount = 10,
                            metadata = "test",
                        )
                    )
                    failAt(stage, CompletionStage.COINS)

                    db.wildlifeExpeditionDao().upsert(
                        WildlifeExpeditionEntity(
                            id = "expedition-$stage",
                            childId = "child-1",
                            weekKey = "2026-08-03",
                            completedLessonIds = "lesson-1",
                            subjectKeys = "english",
                            badgeAwarded = false,
                            awardedBadgeId = null,
                            updatedAtEpochMillis = 1L,
                        )
                    )
                    failAt(stage, CompletionStage.EXPEDITION)

                    db.collectedBadgeDao().insert(
                        CollectedBadgeEntity(
                            id = "badge-$stage",
                            childId = "child-1",
                            badgeId = "badge-1",
                            biome = "forest_friends",
                            earnedDate = "2026-08-03",
                            earnedAtEpochMillis = 1L,
                        )
                    )
                    failAt(stage, CompletionStage.BADGE)
                }
            } catch (_: InjectedFailure) {
                // Expected: the Room transaction must roll every preceding write back.
            }

            assertFalse("completion leaked at $stage", db.lessonCompletionDao().exists("child-1", "lesson-1"))
            assertEquals("progress leaked at $stage", 0, db.progressEventDao().getByChild("child-1").size)
            assertEquals("stars leaked at $stage", 0, db.rewardDao().getTotalByType("child-1", "STAR") ?: 0)
            assertEquals("coins leaked at $stage", 0, db.rewardDao().getTotalByType("child-1", "COIN") ?: 0)
            assertEquals("expedition leaked at $stage", null, db.wildlifeExpeditionDao().getByChildAndWeek("child-1", "2026-08-03"))
            assertEquals("badge leaked at $stage", 0, db.collectedBadgeDao().getAllByChild("child-1").size)
        }
    }

    private fun failAt(actual: CompletionStage, target: CompletionStage) {
        if (actual == target) throw InjectedFailure()
    }

    private class InjectedFailure : RuntimeException()

    private enum class CompletionStage {
        COMPLETION,
        PROGRESS,
        STARS,
        COINS,
        EXPEDITION,
        BADGE,
    }
}
