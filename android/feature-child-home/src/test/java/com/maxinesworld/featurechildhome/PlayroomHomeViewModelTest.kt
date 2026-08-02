package com.maxinesworld.featurechildhome

import androidx.lifecycle.SavedStateHandle
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coremodel.ChildLevelPolicy
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import io.mockk.coEvery
import io.mockk.every
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    private fun buildViewModel(
        completedLessons: List<String> = emptyList(),
        completionDays: List<String> = emptyList(),
        quest: ChallengeProgress = ChallengeProgress(),
        badges: List<com.maxinesworld.coremodel.CollectibleBadge> = emptyList(),
        childName: String? = "Maxine",
    ): PlayroomHomeViewModel {
        val catalog = mockk<ModuleCatalog>()
        coEvery { catalog.modulesFor(any()) } returns emptyList()
        val profileDao = mockk<ChildProfileDao>()
        coEvery { profileDao.observeById("child_1") } returns flowOf(
            childName?.let {
                ChildProfileEntity(
                    id = "child_1", parentId = "parent_1", name = it,
                    avatarId = "cat_orange_default", grade = 3, curriculum = "ph-matatag",
                    createdAt = 0L
                )
            }
        )
        val completionDao = mockk<LessonCompletionDao>()
        coEvery { completionDao.observeDistinctLessonIds("child_1") } returns flowOf(completedLessons)
        coEvery { completionDao.observeCompletionDays("child_1") } returns flowOf(completionDays)
        val awarder = mockk<BadgeAwarder>()
        coEvery { awarder.getTodayProgress("child_1") } returns quest
        coEvery { awarder.getCollectedBadges("child_1") } returns badges
        return PlayroomHomeViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to "child_1")),
            catalog = catalog,
            childProfileDao = profileDao,
            lessonCompletionDao = completionDao,
            badgeAwarder = awarder,
        )
    }

    private fun content(vm: PlayroomHomeViewModel): PlayroomHomeUiState.Content {
        val s = vm.state.value
        assertTrue("expected Content, got $s", s is PlayroomHomeUiState.Content)
        return s as PlayroomHomeUiState.Content
    }

    private fun kindness(c: PlayroomHomeUiState.Content): SubjectCardUi =
        c.subjects.first { it.id == "gmrc" }

    @Test
    fun `kindness locked at level 1`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val k = kindness(content(vm))
        assertTrue("kindness locked at 0 lessons", k.availability == SubjectAvailability.Locked)
        assertEquals(1, ChildLevelPolicy.levelFor(0))
    }

    @Test
    fun `kindness locked below level 4 with lessons-remaining lock reason`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = listOf("a", "b", "c", "d", "e"))
        advanceUntilIdle()
        val k = kindness(content(vm))
        assertTrue(k.availability == SubjectAvailability.Locked)
        assertEquals(2, ChildLevelPolicy.levelFor(5))
        assertTrue("lock reason mentions remaining lessons", k.lockReason!!.contains("7 lessons to go"))
    }

    @Test
    fun `kindness locked exactly at 11 distinct lessons with singular`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = (1..11).map { "lesson-$it" })
        advanceUntilIdle()
        val k = kindness(content(vm))
        assertTrue(k.availability == SubjectAvailability.Locked)
        assertEquals(3, ChildLevelPolicy.levelFor(11))
        assertTrue(k.lockReason!!.contains("1 lesson to go"))
    }

    @Test
    fun `kindness unlocked exactly at 12 distinct lessons`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = (1..12).map { "lesson-$it" })
        advanceUntilIdle()
        val k = kindness(content(vm))
        assertTrue(k.availability == SubjectAvailability.Available)
        assertNull(k.lockReason)
    }

    @Test
    fun `kindness stays unlocked above threshold`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = (1..20).map { "lesson-$it" })
        advanceUntilIdle()
        assertTrue(kindness(content(vm)).availability == SubjectAvailability.Available)
        assertEquals(6, ChildLevelPolicy.levelFor(20))
    }

    @Test
    fun `child name comes from profile`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals("Maxine", content(vm).childName)
    }

    @Test
    fun `empty profile name falls back to greeting fallback text`() = runTest(dispatcher) {
        val vm = buildViewModel(childName = null)
        advanceUntilIdle()
        assertEquals("", content(vm).childName)
    }

    @Test
    fun `non-gmrc subjects stay available`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val c = content(vm)
        val nonGmrc = c.subjects.filter { it.id != "gmrc" }
        assertEquals(5, nonGmrc.size)
        assertTrue("all non-gmrc available", nonGmrc.all { it.isAvailable })
    }

    @Test
    fun `six canonical subjects in fixed order`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val ids = content(vm).subjects.map { it.id }
        assertEquals(
            listOf("mathematics", "english", "science", "filipino", "araling_panlipunan", "gmrc"),
            ids,
        )
    }

    @Test
    fun `per-subject progress divides completed by catalog total`() = runTest(dispatcher) {
        // 3 of 6 math lessons completed → 50%
        val completed = (1..3).map { "mathematics-g3-q1-w01-d0$it" }
        val vm = PlayroomHomeViewModel(
            savedStateHandle = SavedStateHandle(mapOf("childId" to "child_1")),
            catalog = catalogWithTotals(mapOf("mathematics" to 6)),
            childProfileDao = profileDaoOf("Maxine"),
            lessonCompletionDao = completionDaoOf(completed, emptyList()),
            badgeAwarder = awarderOf(),
        )
        advanceUntilIdle()
        val math = content(vm).subjects.first { it.id == "mathematics" }
        assertEquals(50, math.progressPercent)
    }

    @Test
    fun `no completions yields null progress`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val math = content(vm).subjects.first { it.id == "mathematics" }
        assertNull("no progress on fresh account", math.progressPercent)
    }

    @Test
    fun `streak counts consecutive days ending today`() = runTest(dispatcher) {
        val vm = buildViewModel()
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val yesterday = java.time.LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        assertEquals(2, vm.computeStreak(listOf(today, yesterday)))
        assertEquals(1, vm.computeStreak(listOf(yesterday)))
        assertEquals(0, vm.computeStreak(emptyList()))
        assertEquals(0, vm.computeStreak(listOf(java.time.LocalDate.now().minusDays(10).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))))
    }

    @Test
    fun `xp derives from completed lessons`() = runTest(dispatcher) {
        val vm = buildViewModel(completedLessons = (1..7).map { "lesson-$it" })
        advanceUntilIdle()
        assertEquals(7 * PlayroomHomeViewModel.XP_PER_LESSON, content(vm).xp)
    }

    @Test
    fun `quest shows real daily challenge progress`() = runTest(dispatcher) {
        val vm = buildViewModel(quest = ChallengeProgress(english = true, mathematics = true, completedCount = 2))
        advanceUntilIdle()
        val q = content(vm).quest
        assertEquals(2, q.pawPrintsCompleted)
        assertFalse(q.isComplete)
        assertEquals("Continue", q.buttonLabel)
        assertNotNull("recommends an available subject", q.recommendedSubjectId)
    }

    @Test
    fun `completed quest shows view reward`() = runTest(dispatcher) {
        val quest = ChallengeProgress(
            english = true, filipino = true, mathematics = true,
            science = true, makabansa = true, completedCount = 5,
        )
        val vm = buildViewModel(quest = quest)
        advanceUntilIdle()
        val q = content(vm).quest
        assertTrue(q.isComplete)
        assertEquals("View reward", q.buttonLabel)
        assertEquals(QuestAction.ViewReward, q.buttonAction)
    }

    @Test
    fun `sticker book reflects collected badges`() = runTest(dispatcher) {
        val badges = listOf(
            badge("b1", collected = true),
            badge("b2", collected = false),
            badge("b3", collected = false),
        )
        val vm = buildViewModel(badges = badges)
        advanceUntilIdle()
        val sb = content(vm).stickerBook
        assertEquals(1, sb.collectedCount)
        assertEquals(3, sb.totalCount)
        assertEquals(listOf(true, false, false), sb.stickers.map { it.won })
    }

    @Test
    fun `opening subject rejects repeated activation and clears`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onSubjectSelected("mathematics")
        assertTrue(content(vm).openingSubjectId == "mathematics")
        vm.onSubjectSelected("english") // ignored while opening
        assertTrue(content(vm).openingSubjectId == "mathematics")
        vm.onOpenFinished()
        assertNull(content(vm).openingSubjectId)
    }

    @Test
    fun `locked subject cannot open`() = runTest(dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onSubjectSelected("gmrc")
        assertNull(content(vm).openingSubjectId)
    }

    private fun badge(id: String, collected: Boolean) = com.maxinesworld.coremodel.CollectibleBadge(
        id = id, biome = "test", name = "Badge $id", title = "T", funFact = "F",
        emoji = "★", isCollected = collected,
    )

    // ─── Mock builders ────────────────────────────────────────────────

    private fun catalogWithTotals(lessonCounts: Map<String, Int>): ModuleCatalog {
        val catalog = mockk<ModuleCatalog>()
        coEvery { catalog.modulesFor(any<String>()) } answers { call ->
            val subject = call.invocation.args[0] as String
            val count = lessonCounts[subject] ?: 0
            listOf(
                com.maxinesworld.corecontent.ContentModule(
                    key = "m01",
                    title = "Module",
                    lessons = (1..count).map {
                        com.maxinesworld.corecontent.ContentModuleLesson(
                            lessonId = "${subject}-g3-m01-d%02d".format(it),
                            title = "Lesson $it",
                            day = it,
                        )
                    },
                )
            )
        }
        return catalog
    }

    private fun profileDaoOf(name: String?): ChildProfileDao {
        val dao = mockk<ChildProfileDao>()
        coEvery { dao.observeById("child_1") } returns flowOf(
            name?.let {
                ChildProfileEntity(
                    id = "child_1", parentId = "parent_1", name = it,
                    avatarId = "cat_orange_default", grade = 3, curriculum = "ph-matatag",
                    createdAt = 0L
                )
            }
        )
        return dao
    }

    private fun completionDaoOf(lessonIds: List<String>, days: List<String>): LessonCompletionDao {
        val dao = mockk<LessonCompletionDao>()
        coEvery { dao.observeDistinctLessonIds("child_1") } returns flowOf(lessonIds)
        coEvery { dao.observeCompletionDays("child_1") } returns flowOf(days)
        return dao
    }

    private fun awarderOf(quest: ChallengeProgress = ChallengeProgress(), badges: List<com.maxinesworld.coremodel.CollectibleBadge> = emptyList()): BadgeAwarder {
        val awarder = mockk<BadgeAwarder>()
        coEvery { awarder.getTodayProgress("child_1") } returns quest
        coEvery { awarder.getCollectedBadges("child_1") } returns badges
        return awarder
    }
}
