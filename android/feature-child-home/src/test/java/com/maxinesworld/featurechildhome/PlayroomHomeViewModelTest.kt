package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.coremodel.ChildLevelPolicy
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredatabase.LessonCompletionDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayroomHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(completedLessons: Int): PlayroomHomeViewModel {
        val profileDao = mockk<ChildProfileDao>()
        coEvery { profileDao.observeById("child_1") } returns flowOf(
            ChildProfileEntity(
                id = "child_1", parentId = "parent_1", name = "Maxine",
                avatarId = "cat_orange_default", grade = 3, curriculum = "ph-matatag",
                createdAt = 0L
            )
        )
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.observeDistinctLessonCount("child_1") } returns flowOf(completedLessons)
        return PlayroomHomeViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to "child_1")),
            childProfileDao = profileDao,
            lessonCompletionDao = completionDao,
        )
    }

    private fun kindness(vm: PlayroomHomeViewModel) =
        vm.state.value.islands.first { it.id == "gmrc" }

    @Test
    fun `kindness island locked at level 1`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = 0)
        advanceUntilIdle()
        assertTrue("kindness locked at 0 lessons", kindness(vm).locked)
        assertEquals(4, kindness(vm).lockLevel)
        assertEquals(1, ChildLevelPolicy.levelFor(0))
    }

    @Test
    fun `kindness locked below level 4 with lessons-remaining subtitle`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = 5)
        advanceUntilIdle()
        assertTrue(kindness(vm).locked)
        assertEquals(2, ChildLevelPolicy.levelFor(5))
        assertEquals("Unlocks at Level 4 · 7 lessons to go", kindness(vm).subtitle)
    }

    @Test
    fun `kindness unlocked at level 4`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = 12)
        advanceUntilIdle()
        assertFalse("kindness unlocked at 12 lessons", kindness(vm).locked)
        assertEquals("Kindness awaits!", kindness(vm).subtitle)
    }

    @Test
    fun `child name comes from profile`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = 3)
        advanceUntilIdle()
        assertEquals("Maxine", vm.state.value.childName)
    }

    @Test
    fun `non-gmrc islands stay unlocked`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = 0)
        advanceUntilIdle()
        val nonGmrc = vm.state.value.islands.filter { it.id != "gmrc" }
        assertTrue(nonGmrc.isNotEmpty())
        assertTrue("all non-gmrc islands unlocked", nonGmrc.none { it.locked })
    }
}
