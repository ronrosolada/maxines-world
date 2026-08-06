package com.maxinesworld.featureparent

import com.maxinesworld.featureauth.ParentAuthManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentGateLockoutTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var authManager: ParentAuthManager

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `persisted lockout is live immediately and blocks PIN input`() = runTest(dispatcher) {
        val lockedUntil = System.currentTimeMillis() + 30_000L
        coEvery { authManager.getLockedUntilEpochMillis() } returns lockedUntil

        val viewModel = ParentGateViewModel(authManager)
        runCurrent()

        assertTrue(viewModel.state.value.lockRemainingSeconds in 29..30)
        viewModel.onPinDigit("1")
        assertEquals("", viewModel.state.value.pinInput)
    }

    @Test
    fun `remaining lockout seconds round up and reach zero at the deadline`() {
        assertEquals(30, parentGateRemainingSeconds(130_000L, 100_001L))
        assertEquals(1, parentGateRemainingSeconds(130_000L, 129_999L))
        assertEquals(0, parentGateRemainingSeconds(130_000L, 130_000L))
    }
}
