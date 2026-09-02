package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.corecontent.AssessmentRepository
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coremodel.AssessmentCatalog
import com.maxinesworld.coremodel.AssessmentPack
import com.maxinesworld.coremodel.AssessmentPackMetadata
import com.maxinesworld.coremodel.AssessmentQuestionItem
import com.maxinesworld.coremodel.AssessmentQuestionOption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssessmentArenaViewModelTest {

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    private val assessmentRepository = mockk<AssessmentRepository>()
    private val rewardDao = mockk<RewardDao>(relaxed = true)

    private val sampleItems = (1..10).map { seq ->
        AssessmentQuestionItem(
            sequence = seq,
            prompt = "Question $seq",
            options = listOf(
                AssessmentQuestionOption("a", "Option A"),
                AssessmentQuestionOption("b", "Option B"),
                AssessmentQuestionOption("c", "Option C"),
                AssessmentQuestionOption("d", "Option D"),
            ),
            correctOptionIds = listOf("a"),
            explanation = "Explanation $seq",
            hint = "Hint $seq",
        )
    }

    private val samplePack = AssessmentPack(
        id = "math-g3-ph",
        subjectId = "mathematics",
        curriculum = "ph",
        curriculumName = "Philippine DepEd",
        flagEmoji = "🇵🇭",
        title = "Grade 3 Math: PH",
        description = "Test Description",
        badgeKey = "badge_math_ph",
        items = sampleItems,
    )

    private val sampleMetadata = AssessmentPackMetadata(
        id = "math-g3-ph",
        subjectId = "mathematics",
        curriculum = "ph",
        curriculumName = "Philippine DepEd",
        flagEmoji = "🇵🇭",
        title = "Grade 3 Math: PH",
        description = "Test Description",
        badgeKey = "badge_math_ph",
        questionCount = 10,
        passingCount = 8,
        file = "assessment-packs/math-g3-ph.json",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { assessmentRepository.getCatalog() } returns AssessmentCatalog(
            schemaVersion = 1,
            packs = listOf(sampleMetadata),
        )
        coEvery { assessmentRepository.getPack("math-g3-ph") } returns samplePack
        every { rewardDao.observeByChild("child_maxine") } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(childId: String = "child_maxine", subject: String = "mathematics"): AssessmentArenaViewModel {
        return AssessmentArenaViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to childId, "subject" to subject)),
            assessmentRepository = assessmentRepository,
            rewardDao = rewardDao,
        )
    }

    @Test
    fun `loadCatalog sets packs and initial subject correctly`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals(1, vm.state.value.packs.size)
        assertEquals("mathematics", vm.state.value.selectedSubjectId)
    }

    @Test
    fun `startQuiz initializes active quiz state`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startQuiz("math-g3-ph")
        testDispatcher.scheduler.advanceUntilIdle()

        val quiz = vm.state.value.activeQuiz
        assertNotNull(quiz)
        assertEquals("math-g3-ph", quiz?.packId)
        assertEquals(10, quiz?.items?.size)
        assertEquals(0, quiz?.currentIndex)
        assertFalse(quiz?.isAnswerSubmitted == true)
        assertEquals(
            listOf("a", "b", "c", "d").toSet(),
            quiz?.displayedOptions?.map { it.id }?.toSet(),
        )
        assertEquals(
            listOf("a", "b", "c", "d"),
            quiz?.items?.first()?.options?.map { it.id },
        )
    }

    @Test
    fun `presenting the same arena item twice can yield different option orders`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val orders = (0..40).map { seed ->
            vm.optionRandom = kotlin.random.Random(seed.toLong())
            vm.startQuiz("math-g3-ph")
            testDispatcher.scheduler.advanceUntilIdle()
            vm.state.value.activeQuiz!!.displayedOptions.map { it.id }
        }.toSet()

        assertTrue("arena retries must be allowed to show a new slot order, got $orders", orders.size >= 2)
    }

    @Test
    fun `selecting the keyed id scores correct regardless of display slot`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.optionRandom = kotlin.random.Random(7)
        vm.startQuiz("math-g3-ph")
        testDispatcher.scheduler.advanceUntilIdle()

        val quiz = vm.state.value.activeQuiz!!
        assertTrue(quiz.displayedOptions.map { it.id }.contains("a"))
        vm.selectOption("a")
        vm.submitAnswer()

        val scored = vm.state.value.activeQuiz!!
        assertTrue(scored.isAnswerSubmitted)
        assertTrue(scored.isCorrect)
        assertEquals(1, scored.correctCount)
    }

    @Test
    fun `selecting a distractor id scores wrong regardless of display slot`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.optionRandom = kotlin.random.Random(7)
        vm.startQuiz("math-g3-ph")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectOption("b")
        vm.submitAnswer()

        val scored = vm.state.value.activeQuiz!!
        assertTrue(scored.isAnswerSubmitted)
        assertFalse(scored.isCorrect)
        assertEquals(0, scored.correctCount)
    }

    @Test
    fun `missing selected id fails closed`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.startQuiz("math-g3-ph")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectOption("z")
        vm.submitAnswer()

        val scored = vm.state.value.activeQuiz!!
        assertTrue(scored.isAnswerSubmitted)
        assertFalse(scored.isCorrect)
        assertEquals(0, scored.correctCount)
    }

    @Test
    fun `toggleHint changes hint visibility`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.startQuiz("math-g3-ph")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.activeQuiz?.isHintVisible == true)
        vm.toggleHint()
        assertTrue(vm.state.value.activeQuiz?.isHintVisible == true)
    }

    @Test
    fun `selectOption and submitAnswer updates score and feedback`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.startQuiz("math-g3-ph")
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectOption("a")
        assertEquals("a", vm.state.value.activeQuiz?.selectedOptionId)

        vm.submitAnswer()
        val quiz = vm.state.value.activeQuiz
        assertTrue(quiz?.isAnswerSubmitted == true)
        assertTrue(quiz?.isCorrect == true)
        assertEquals(1, quiz?.correctCount)
    }

    @Test
    fun `complete quiz with 10 correct awards 15 stars and 2 tokens once`() = runTest(testDispatcher) {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.startQuiz("math-g3-ph")
        testDispatcher.scheduler.advanceUntilIdle()

        for (i in 0 until 10) {
            vm.selectOption("a")
            vm.submitAnswer()
            vm.nextQuestion()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val quiz = vm.state.value.activeQuiz
        assertTrue(quiz?.isFinished == true)
        assertTrue(quiz?.isPassed == true)
        assertEquals(10, quiz?.correctCount)
        assertEquals(15, quiz?.earnedStars)
        assertEquals(2, quiz?.earnedTokens)
        assertTrue(vm.state.value.showCelebrationDialog)

        coVerify {
            rewardDao.insertIgnoring(match {
                it.childId == "child_maxine" && it.type == "STAR" && it.amount == 15
            })
            rewardDao.insertIgnoring(match {
                it.childId == "child_maxine" && it.type == "COIN" && it.amount == 2
            })
        }
    }

    @Test
    fun `reviewClues after a failed quiz does not start a new scored attempt`() = runTest(testDispatcher) {
        val vm = createViewModel()
        finishQuiz(vm, correct = false)
        val before = vm.state.value.activeQuiz!!
        assertTrue(before.isFinished)
        assertFalse(before.isPassed)
        assertFalse(before.isReviewingClues)
        coVerify(exactly = 1) { assessmentRepository.getPack("math-g3-ph") }

        vm.reviewClues()
        testDispatcher.scheduler.advanceUntilIdle()

        val after = vm.state.value.activeQuiz!!
        assertTrue(after.isReviewingClues)
        assertTrue(after.isFinished)
        assertFalse(after.isPassed)
        assertEquals(before.correctCount, after.correctCount)
        assertEquals(0, after.earnedStars)
        assertEquals(0, after.earnedTokens)
        assertEquals(before.items, after.items)
        assertEquals(sampleItems.map { it.explanation }, arenaClueReviewItems(after.items).map { it.explanation })
        coVerify(exactly = 1) { assessmentRepository.getPack("math-g3-ph") }
    }

    @Test
    fun `reviewClues is a no-op after a passed quiz`() = runTest(testDispatcher) {
        val vm = createViewModel()
        finishQuiz(vm, correct = true)
        assertTrue(vm.state.value.activeQuiz?.isPassed == true)

        vm.reviewClues()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.activeQuiz?.isReviewingClues == true)
        coVerify(exactly = 1) { assessmentRepository.getPack("math-g3-ph") }
    }

    @Test
    fun `exitClueReview returns to the fail summary without restarting`() = runTest(testDispatcher) {
        val vm = createViewModel()
        finishQuiz(vm, correct = false)
        vm.reviewClues()
        assertTrue(vm.state.value.activeQuiz?.isReviewingClues == true)

        vm.exitClueReview()

        val quiz = vm.state.value.activeQuiz!!
        assertFalse(quiz.isReviewingClues)
        assertTrue(quiz.isFinished)
        assertFalse(quiz.isPassed)
        coVerify(exactly = 1) { assessmentRepository.getPack("math-g3-ph") }
    }

    @Test
    fun `restartQuiz from clue review starts a new scored attempt`() = runTest(testDispatcher) {
        val vm = createViewModel()
        finishQuiz(vm, correct = false)
        vm.reviewClues()

        vm.restartQuiz()
        testDispatcher.scheduler.advanceUntilIdle()

        val quiz = vm.state.value.activeQuiz!!
        assertFalse(quiz.isReviewingClues)
        assertFalse(quiz.isFinished)
        assertEquals(0, quiz.correctCount)
        assertEquals(0, quiz.currentIndex)
        assertFalse(quiz.isAnswerSubmitted)
        coVerify(exactly = 2) { assessmentRepository.getPack("math-g3-ph") }
    }

    @Test
    fun `selectOption and submitAnswer are ignored while reviewing clues`() = runTest(testDispatcher) {
        val vm = createViewModel()
        finishQuiz(vm, correct = false)
        val finishedCount = vm.state.value.activeQuiz!!.correctCount
        vm.reviewClues()

        vm.selectOption("a")
        vm.submitAnswer()
        vm.nextQuestion()
        vm.toggleHint()

        val quiz = vm.state.value.activeQuiz!!
        assertTrue(quiz.isReviewingClues)
        assertTrue(quiz.isFinished)
        assertEquals(finishedCount, quiz.correctCount)
        assertEquals(0, quiz.earnedStars)
    }

    private fun finishQuiz(vm: AssessmentArenaViewModel, correct: Boolean) {
        vm.startQuiz("math-g3-ph")
        testDispatcher.scheduler.advanceUntilIdle()
        repeat(10) {
            vm.selectOption(if (correct) "a" else "b")
            vm.submitAnswer()
            vm.nextQuestion()
        }
        testDispatcher.scheduler.advanceUntilIdle()
    }
}
