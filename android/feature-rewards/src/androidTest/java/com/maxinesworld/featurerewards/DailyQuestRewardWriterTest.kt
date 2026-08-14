package com.maxinesworld.featurerewards

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxinesworld.coredatabase.DailyQuestSetEntity
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.RewardBreakPolicy
import com.maxinesworld.coredatabase.RewardEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyQuestRewardWriterTest {
    private lateinit var database: MaxinesDatabase
    private lateinit var writer: DailyQuestRewardWriter

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        writer = DailyQuestRewardWriter(
            database = database,
            dailyQuestSetDao = database.dailyQuestSetDao(),
            dailyQuestCompletionDao = database.dailyQuestCompletionDao(),
            rewardDao = database.rewardDao(),
            rewardBreakDao = database.rewardBreakDao(),
        )
        runBlocking {
            database.dailyQuestSetDao().insertIgnoring(
                DailyQuestSetEntity(
                    id = "child-1:2026-08-09",
                    childId = "child-1",
                    dayKey = "2026-08-09",
                    assignedQuestIds = "[\"lesson-1\",\"lesson-2\",\"lesson-3\"]",
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun rewardBreakAndSanctuaryPieceAppearOnlyAtThreeOfThree() = runBlocking {
        val first = writer.reconcile("child-1", "2026-08-09", "lesson-1")
        val second = writer.reconcile("child-1", "2026-08-09", "lesson-2")

        assertTrue(!first.questComplete)
        assertTrue(!second.questComplete)
        assertNull(database.rewardBreakDao().getByQuestCompletion("child-1:2026-08-09"))
        assertEquals(0, database.rewardDao().getTotalByType("child-1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE) ?: 0)

        val third = writer.reconcile("child-1", "2026-08-09", "lesson-3")

        assertTrue(third.questComplete)
        assertTrue(third.newlyAwarded)
        assertNotNull(third.sanctuaryPieceId)
        assertNotNull(third.rewardBreakId)
        assertEquals(1, database.rewardDao().getTotalByType("child-1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE))
        val entitlement = database.rewardBreakDao().getByQuestCompletion("child-1:2026-08-09")
        assertNotNull(entitlement)
        assertEquals(RewardBreakPolicy.CREATED, entitlement?.state)
    }

    @Test
    fun completingTheThirdTargetAgainIsIdempotent() = runBlocking {
        writer.reconcile("child-1", "2026-08-09", "lesson-1")
        writer.reconcile("child-1", "2026-08-09", "lesson-2")
        val first = writer.reconcile("child-1", "2026-08-09", "lesson-3")
        val retry = writer.reconcile("child-1", "2026-08-09", "lesson-3")

        assertTrue(first.newlyAwarded)
        assertTrue(!retry.newlyAwarded)
        assertEquals(first.sanctuaryPieceId, retry.sanctuaryPieceId)
        assertEquals(1, database.rewardDao().getTotalByType("child-1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE))
        assertEquals(first.rewardBreakId, retry.rewardBreakId)
    }

    @Test
    fun completedSanctuaryDoesNotMintDuplicatePiece() = runBlocking {
        SanctuaryCatalog.pieces.forEachIndexed { index, piece ->
            database.rewardDao().insert(
                RewardEntity(
                    id = "existing-piece-$index",
                    childId = "child-1",
                    type = DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE,
                    amount = 1,
                    metadata = piece.id,
                )
            )
        }

        writer.reconcile("child-1", "2026-08-09", "lesson-1")
        writer.reconcile("child-1", "2026-08-09", "lesson-2")
        val result = writer.reconcile("child-1", "2026-08-09", "lesson-3")

        assertTrue(result.questComplete)
        assertTrue(!result.newlyAwarded)
        assertNull(result.sanctuaryPieceId)
        assertEquals(SanctuaryCatalog.pieces.size, database.rewardDao()
            .getTotalByType("child-1", DailyQuestRewardWriter.SANCTUARY_PIECE_TYPE))
        assertNotNull(result.rewardBreakId)
    }
}
