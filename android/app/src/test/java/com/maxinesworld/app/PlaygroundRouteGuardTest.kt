package com.maxinesworld.app

import com.maxinesworld.coredatabase.*
import com.maxinesworld.playground.PlaygroundGateEvaluator
import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlaygroundRouteGuardTest {

    private val childId = "child_guard_test"

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

    // ─── Shared helper: create ViewModel with given DAO state ───

    private fun createViewModel(
        questSet: DailyQuestSetEntity? = null,
        completedIds: List<String> = emptyList(),
        hasReceipt: Boolean = false,
    ): PlaygroundAccessViewModel {
        val dayKey = java.time.LocalDate.now().toString()
        coEvery { questSetDao.getByChildAndDay(childId, dayKey) } returns questSet
        coEvery { questCompletionDao.getCompletedQuestIds(childId, dayKey) } returns completedIds
        coEvery { unlockReceiptDao.existsByChildAndDay(childId, dayKey) } returns hasReceipt
        // Need a SavedStateHandle with childId — mock it
        val handle = mockk<androidx.lifecycle.SavedStateHandle>()
        coEvery { handle.get<String>("childId") } returns childId
        return PlaygroundAccessViewModel(handle, questSetDao, questCompletionDao, unlockReceiptDao)
    }

    @Test
    fun `lockedLibraryRoute_isBlocked`() = runTest {
        val vm = createViewModel(
            questSet = DailyQuestSetEntity(id = "qs-1", childId = childId, dayKey = java.time.LocalDate.now().toString(),
                assignedQuestIds = "[\"subject:english\",\"subject:filipino\",\"subject:mathematics\"]",
                assignedAtEpochMillis = 1000L),
            completedIds = listOf("subject:english"), // 1 of 3 completed
            hasReceipt = false,
        )
        val state = vm.state.value
        assertTrue("should be blocked", state is PlaygroundAccessUiState.Blocked)
        val blocked = state as PlaygroundAccessUiState.Blocked
        assertEquals("status should be Locked", PlaygroundGateStatus.Locked, blocked.gate.status)
    }

    @Test
    fun `lockedKittenMatchRoute_isBlocked`() = runTest {
        val vm = createViewModel(
            questSet = DailyQuestSetEntity(id = "qs-1", childId = childId, dayKey = java.time.LocalDate.now().toString(),
                assignedQuestIds = "[\"subject:english\",\"subject:filipino\",\"subject:mathematics\"]",
                assignedAtEpochMillis = 1000L),
            completedIds = emptyList(), // 0 of 3 completed
            hasReceipt = false,
        )
        val state = vm.state.value
        assertTrue("should be blocked", state is PlaygroundAccessUiState.Blocked)
    }

    @Test
    fun `lockedFireflyGardenRoute_isBlocked`() = runTest {
        val vm = createViewModel(
            questSet = DailyQuestSetEntity(id = "qs-1", childId = childId, dayKey = java.time.LocalDate.now().toString(),
                assignedQuestIds = "[\"subject:english\"]",
                assignedAtEpochMillis = 1000L),
            completedIds = listOf("subject:english"), // all done but not 3
            hasReceipt = false,
        )
        val state = vm.state.value
        assertTrue("should be blocked", state is PlaygroundAccessUiState.Blocked)
    }

    @Test
    fun `lockedPawBeatsRoute_isBlocked`() = runTest {
        val vm = createViewModel(
            questSet = DailyQuestSetEntity(id = "qs-1", childId = childId, dayKey = java.time.LocalDate.now().toString(),
                assignedQuestIds = "[\"subject:english\",\"subject:filipino\",\"subject:mathematics\"]",
                assignedAtEpochMillis = 1000L),
            completedIds = listOf("subject:english", "subject:mathematics"), // 2 of 3
            hasReceipt = false,
        )
        val state = vm.state.value
        assertTrue("should be blocked", state is PlaygroundAccessUiState.Blocked)
    }

    @Test
    fun `unlockedRoutes_openOffline`() = runTest {
        val vm = createViewModel(
            questSet = DailyQuestSetEntity(id = "qs-1", childId = childId, dayKey = java.time.LocalDate.now().toString(),
                assignedQuestIds = "[\"subject:english\",\"subject:filipino\",\"subject:mathematics\"]",
                assignedAtEpochMillis = 1000L),
            completedIds = listOf("subject:english", "subject:filipino", "subject:mathematics"),
            hasReceipt = true,
        )
        val state = vm.state.value
        assertTrue("should be allowed", state is PlaygroundAccessUiState.Allowed)
    }

    @Test
    fun `malformedSnapshot_failsClosed`() = runTest {
        val vm = createViewModel(
            questSet = DailyQuestSetEntity(id = "qs-1", childId = childId, dayKey = java.time.LocalDate.now().toString(),
                assignedQuestIds = "not-valid-json",
                assignedAtEpochMillis = 1000L),
            completedIds = emptyList(),
            hasReceipt = false,
        )
        val state = vm.state.value
        assertTrue("malformed data should fail closed (blocked)", state is PlaygroundAccessUiState.Blocked)
    }
}
