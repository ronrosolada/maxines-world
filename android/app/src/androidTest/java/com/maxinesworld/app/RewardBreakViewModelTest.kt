package com.maxinesworld.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.GodModeManager
import com.maxinesworld.coredatabase.RewardBreakEntitlementEntity
import com.maxinesworld.coredatabase.RewardBreakPolicy
import com.maxinesworld.coredatabase.RoomTransactionRunner
import com.maxinesworld.engineminigame.MiniGameResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RewardBreakViewModelTest {
    private lateinit var database: MaxinesDatabase
    private lateinit var viewModel: RewardBreakViewModel
    private lateinit var godModeManager: GodModeManager

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        godModeManager = GodModeManager(context)
        godModeManager.setEnabled("child-1", false)
        viewModel = RewardBreakViewModel(
            rewardBreakDao = database.rewardBreakDao(),
            miniGameResultDao = database.miniGameResultDao(),
            rewardDao = database.rewardDao(),
            inventoryDao = database.inventoryDao(),
            transactionRunner = RoomTransactionRunner(database),
            godModeManager = godModeManager,
        )
        val now = System.currentTimeMillis()
        database.rewardBreakDao().insertIgnoring(
            RewardBreakEntitlementEntity(
                id = "break-1",
                childId = "child-1",
                dailyQuestCompletionId = "child-1:2026-08-09",
                durationMillis = RewardBreakPolicy.DEFAULT_DURATION_MILLIS,
                remainingMillis = RewardBreakPolicy.DEFAULT_DURATION_MILLIS,
                createdAtEpochMillis = now - 100,
                startedAtEpochMillis = now - 50,
                state = RewardBreakPolicy.ACTIVE,
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun miniGameTokensAndCollectibleArePersistedOnce() = runBlocking {
        val now = System.currentTimeMillis()
        val result = MiniGameResult(
            sessionId = "session-1",
            rewardBreakId = "break-1",
            gameId = "kitten-match",
            childId = "child-1",
            startedAtEpochMillis = now - 25,
            endedAtEpochMillis = now,
            roundsCompleted = 3,
            correctOrders = 8,
            pawTokensEarned = 4,
            collectibleId = "milo-blue-paw",
        )

        assertTrue(viewModel.saveResult(result))
        assertTrue(viewModel.saveResult(result))
        // Mini-games are a reward break, not a currency source: no spendable COIN
        // is minted from in-game tokens. The collectible + idempotency still hold.
        assertEquals(0, database.rewardDao().getTotalByType("child-1", "COIN"))
        assertTrue(database.inventoryDao().owns("child-1", "milo-blue-paw"))
        assertTrue(database.miniGameResultDao().getByIdempotencyKey(result.idempotencyKey) != null)
    }

    @Test
    fun godModeOpensPlaygroundWithoutCreatingOrConsumingAnEntitlement() = runBlocking {
        godModeManager.setEnabled("child-god", true)

        val remaining = viewModel.begin("child-god", "missing-break")

        assertEquals(RewardBreakPolicy.DEFAULT_DURATION_MILLIS, remaining)
        val state = viewModel.state.value as RewardBreakUiState.Ready
        assertTrue(state.started)
        assertEquals(0, database.rewardBreakDao().getById("missing-break")?.remainingMillis ?: 0)
    }
}
