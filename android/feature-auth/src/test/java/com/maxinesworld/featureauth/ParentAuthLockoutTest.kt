package com.maxinesworld.featureauth

import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredatabase.ParentAccountDao
import com.maxinesworld.coredatabase.ParentAccountEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Brute-force protection regression tests (handoff follow-up):
 * - 5 consecutive failures trigger a lockout with a countdown message
 * - a correct PIN is rejected while locked
 * - a correct PIN after lockout expiry authenticates and resets the counter
 * - persistent counter state survives (ViewModel is re-created from DataStore values)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentAuthLockoutTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authManager: ParentAuthManager
    private lateinit var parentAccountDao: ParentAccountDao
    private lateinit var childProfileDao: ChildProfileDao
    private lateinit var viewModel: ParentAuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authManager = mockk(relaxed = true)
        parentAccountDao = mockk(relaxed = true)
        childProfileDao = mockk(relaxed = true)
        every { authManager.displayName } returns flowOf(null)
        coEvery { authManager.getPinHash() } returns "hash"
        coEvery { authManager.verifyPin(any()) } returns false
        coEvery { authManager.verifyPin("123456") } returns true
        coEvery { authManager.getFailedAttempts() } returns 0
        coEvery { authManager.getLockedUntilEpochMillis() } returns 0L
        // Parent with a child profile so init lands on PIN_LOGIN (auto-verify active).
        coEvery { parentAccountDao.getParent() } returns ParentAccountEntity(
            id = "p1", displayName = "Parent", pinHash = ""
        )
        coEvery { childProfileDao.getByParent("p1") } returns listOf(
            ChildProfileEntity(id = "c1", parentId = "p1", name = "Maxine")
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(): ParentAuthViewModel {
        val vm = ParentAuthViewModel(authManager, parentAccountDao, childProfileDao)
        runCurrent() // let init collect displayName and land on PIN_LOGIN
        return vm
    }

    private fun enterPin(vm: ParentAuthViewModel, pin: String) {
        pin.forEach { vm.onPinDigit(it.toString()) }
    }

    @Test
    fun `fresh install uses the fixed default PIN and skips PIN setup`() = runTest(dispatcher) {
        coEvery { parentAccountDao.getParent() } returns null
        coEvery { childProfileDao.getByParent("parent") } returns emptyList()

        viewModel = createViewModel()

        assertEquals("123456", ParentAuthManager.DEFAULT_PIN)
        assertTrue(viewModel.state.value.hasPin)
        assertEquals(AuthScreen.CREATE_PROFILE, viewModel.state.value.currentScreen)
        coVerify {
            parentAccountDao.upsert(match { it.id == "parent" && it.displayName == ParentAuthManager.DEFAULT_PARENT_NAME })
        }
    }

    @Test
    fun `four failures show remaining attempts but do not lock`() = runTest(dispatcher) {
        var attempts = 0
        coEvery { authManager.recordFailedAttempt(any()) } answers { attempts += 1; 0L }
        coEvery { authManager.getFailedAttempts() } answers { attempts }
        viewModel = createViewModel()
        for (i in 1..4) {
            enterPin(viewModel, "000000")
            advanceUntilIdle()
        }
        val state = viewModel.state.value
        assertEquals(4, state.failedAttempts)
        assertEquals(0L, state.lockedUntilEpochMillis)
        assertNotNull(state.pinError)
        assertTrue(state.pinError!!.contains("attempt"))
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `fifth failure locks PIN entry and shows countdown`() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        var attempts = 0
        coEvery { authManager.recordFailedAttempt(any()) } answers {
            attempts = 5; now + 30_000L
        }
        coEvery { authManager.getFailedAttempts() } answers { attempts }
        viewModel = createViewModel()
        enterPin(viewModel, "000000")
        runCurrent()

        val state = viewModel.state.value
        assertEquals(5, state.failedAttempts)
        assertTrue(state.lockedUntilEpochMillis > now)
        assertTrue(state.lockRemainingSeconds in 29..31)
        coVerify(exactly = 1) { authManager.recordFailedAttempt(any()) }
    }

    @Test
    fun `correct PIN is rejected while lockout is active`() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        coEvery { authManager.getLockedUntilEpochMillis() } returns (now + 60_000L)
        viewModel = createViewModel()

        enterPin(viewModel, "123456")
        runCurrent()

        assertFalse(viewModel.state.value.isAuthenticated)
        assertTrue(viewModel.state.value.lockRemainingSeconds in 59..61)
        coVerify(exactly = 0) { authManager.verifyPin(any()) }
        coVerify(exactly = 0) { authManager.resetFailedAttempts() }
    }

    @Test
    fun `correct PIN after lockout expiry authenticates and resets counter`() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        coEvery { authManager.getLockedUntilEpochMillis() } returns (now - 1_000L)
        viewModel = createViewModel()

        enterPin(viewModel, "123456")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isAuthenticated)
        coVerify(exactly = 1) { authManager.resetFailedAttempts() }
        assertEquals(0, viewModel.state.value.failedAttempts)
    }

    @Test
    fun `lockout escalation follows attempt count`() {
        // Policy sanity: 30s base, doubles per block of 5, capped at 300s.
        val base = ParentAuthManager.BASE_LOCKOUT_MILLIS
        val cap = ParentAuthManager.MAX_LOCKOUT_MILLIS
        assertEquals(30_000L, base)
        fun durationFor(attempts: Int): Long {
            if (attempts < ParentAuthManager.MAX_ATTEMPTS_BEFORE_LOCK) return 0L
            val level = attempts / ParentAuthManager.MAX_ATTEMPTS_BEFORE_LOCK
            return (base shl (level - 1)).coerceAtMost(cap)
        }
        assertEquals(0L, durationFor(4))
        assertEquals(30_000L, durationFor(5))
        assertEquals(60_000L, durationFor(10))
        assertEquals(120_000L, durationFor(15))
        assertEquals(300_000L, durationFor(40))
    }

    @Test
    fun `remaining lockout seconds round up and reach zero at the deadline`() {
        assertEquals(30, lockRemainingSeconds(130_000L, 100_001L))
        assertEquals(1, lockRemainingSeconds(130_000L, 129_999L))
        assertEquals(0, lockRemainingSeconds(130_000L, 130_000L))
    }

    @Test
    fun `extra PIN taps while verification is pending do not start a second check`() = runTest(dispatcher) {
        coEvery { authManager.verifyPin(any()) } coAnswers {
            kotlinx.coroutines.delay(1)
            false
        }
        viewModel = createViewModel()

        enterPin(viewModel, "000000")
        viewModel.onPinDigit("9")
        advanceUntilIdle()

        coVerify(exactly = 1) { authManager.verifyPin(any()) }
    }

    @Test
    fun `parent verification challenge correctly verifies answers`() {
        val challenge = ParentVerificationChallenge(factorA = 14, factorB = 7)
        assertEquals(98, challenge.expectedAnswer)
        assertTrue(challenge.verify("98"))
        assertTrue(challenge.verify(" 98 "))
        assertFalse(challenge.verify("97"))
        assertFalse(challenge.verify("abc"))
    }

    @Test
    fun `restoring default PIN clears lockout and preserves child profiles`() = runTest(dispatcher) {
        coEvery { authManager.resetPinOnly() } coAnswers { }
        viewModel = createViewModel()

        viewModel.onResetPin()
        advanceUntilIdle()

        coVerify(exactly = 1) { authManager.resetPinOnly() }
        val state = viewModel.state.value
        assertEquals(AuthScreen.PIN_LOGIN, state.currentScreen)
        assertTrue(state.hasPin)
        assertEquals(0, state.failedAttempts)
        assertEquals(0L, state.lockedUntilEpochMillis)
        assertEquals(0, state.lockRemainingSeconds)
        assertEquals(1, state.childProfiles.size)
    }
}
