package com.maxinesworld.coredatabase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RewardBreakEntitlementTest {
    private lateinit var db: MaxinesDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            MaxinesDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun dailyEntitlementInsertIsFirstWriteWins() = runBlocking {
        val dao = db.rewardBreakDao()
        val key = RewardBreakPolicy.dailyQuestCompletionId("child-1", "2026-08-04")
        dao.insertIgnoring(RewardBreakPolicy.newEntitlement("break-1", "child-1", key, 100L))
        dao.insertIgnoring(RewardBreakPolicy.newEntitlement("break-2", "child-1", key, 200L))

        val saved = dao.getByQuestCompletion(key)
        assertNotNull(saved)
        assertEquals("break-1", saved!!.id)
        assertEquals(100L, saved.createdAtEpochMillis)
    }

    @Test
    fun entitlementCanStartOnceAndConsumeOnce() = runBlocking {
        val dao = db.rewardBreakDao()
        val key = RewardBreakPolicy.dailyQuestCompletionId("child-1", "2026-08-04")
        dao.insertIgnoring(RewardBreakPolicy.newEntitlement("break-1", "child-1", key, 100L))

        assertEquals(1, dao.startIfCreated("break-1", "child-1", 1_000L))
        assertEquals(0, dao.startIfCreated("break-1", "child-1", 2_000L))
        val active = dao.getById("break-1")
        assertEquals(RewardBreakPolicy.ACTIVE, active!!.state)
        assertEquals(1_000L, active.startedAtEpochMillis)

        assertEquals(1, dao.consumeIfUnconsumed("break-1", "child-1", 3_000L))
        assertEquals(0, dao.consumeIfUnconsumed("break-1", "child-1", 4_000L))
        val consumed = dao.getById("break-1")
        assertEquals(RewardBreakPolicy.CONSUMED, consumed!!.state)
        assertEquals(3_000L, consumed.consumedAtEpochMillis)
        assertEquals(0L, consumed.remainingMillis)
    }

    @Test
    fun miniGameResultInsertIsFirstWriteWins() = runBlocking {
        val dao = db.miniGameResultDao()
        val first = MiniGameResultEntity(
            sessionId = "session-1",
            idempotencyKey = "break-1:cat-cafe-dash",
            rewardBreakId = "break-1",
            childId = "child-1",
            gameId = "cat-cafe-dash",
            startedAtEpochMillis = 1_000L,
            endedAtEpochMillis = 2_000L,
            roundsCompleted = 1,
            successfulActions = 2,
            pawTokensEarned = 3,
        )
        val retry = first.copy(
            sessionId = "session-2",
            endedAtEpochMillis = 9_000L,
            successfulActions = 99,
        )

        dao.insert(first)
        dao.insert(retry)

        val saved = dao.getByIdempotencyKey(first.idempotencyKey)
        assertNotNull(saved)
        assertEquals("session-1", saved!!.sessionId)
        assertEquals(2, saved.successfulActions)
    }
}
