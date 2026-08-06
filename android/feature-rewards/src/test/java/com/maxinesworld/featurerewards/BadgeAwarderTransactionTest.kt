package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.WildlifeExpeditionDao
import com.maxinesworld.coredatabase.WildlifeExpeditionEntity
import com.maxinesworld.coremodel.CollectibleBadge
import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Concurrency and restart coverage for the persistent weekly expedition. */
class BadgeAwarderTransactionTest {
    private val childId = "child_txn_1"
    private val allTestBadges = (1..10).map { i ->
        CollectibleBadge(
            id = "badge_%02d".format(i),
            biome = if (i <= 5) "forest_friends" else "sky_scouts",
            name = "Badge $i", title = "Title $i", funFact = "Fact $i",
        )
    }

    private lateinit var expeditionDao: WildlifeExpeditionDao
    private lateinit var collectedBadgeDao: CollectedBadgeDao
    private lateinit var badgeLoader: BadgeLoader
    private val expeditions = mutableMapOf<String, WildlifeExpeditionEntity>()
    private val collectedBadges = mutableListOf<CollectedBadgeEntity>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        expeditionDao = mockk()
        collectedBadgeDao = mockk()
        badgeLoader = mockk()
        expeditions.clear()
        collectedBadges.clear()
        coEvery { expeditionDao.getByChildAndWeek(childId, any()) } answers {
            expeditions[secondArg()]
        }
        coEvery { expeditionDao.upsert(any()) } answers {
            val value = firstArg<WildlifeExpeditionEntity>()
            expeditions[value.weekKey] = value
        }
        coEvery { collectedBadgeDao.getAllByChild(childId) } answers { collectedBadges.toList() }
        coEvery { collectedBadgeDao.insert(any()) } answers { collectedBadges.add(firstArg()) }
        coEvery { badgeLoader.loadAll() } returns allTestBadges
        mockDate("2026-08-03")
    }

    @After
    fun tearDown() { unmockkAll() }

    @Test
    fun `concurrent lesson callbacks produce one badge`() = runTest {
        val awarder = awarder()
        val results = listOf(
            async { awarder.recordLessonCompletion(childId, "english", "lesson-1") },
            async { awarder.recordLessonCompletion(childId, "gmrc", "lesson-2") },
            async { awarder.recordLessonCompletion(childId, "science", "lesson-3") },
        ).awaitAll()

        assertEquals(1, results.count { it.newlyAwardedBadge != null })
        assertEquals(1, collectedBadges.size)
        assertTrue(expeditions.values.single().badgeAwarded)
    }

    @Test
    fun `same lesson replay cannot inflate progress`() = runTest {
        val awarder = awarder()
        repeat(5) { awarder.recordLessonCompletion(childId, "english", "same-lesson") }

        val progress = awarder.getExpeditionProgress(childId)
        assertEquals(1, progress.completedCount)
        assertEquals(1, progress.subjectCount)
        assertEquals(0, collectedBadges.size)
    }

    @Test
    fun `new week starts a new expedition but retains old sticker`() = runTest {
        val awarder = awarder()
        awarder.recordLessonCompletion(childId, "english", "week-one-1")
        awarder.recordLessonCompletion(childId, "gmrc", "week-one-2")
        awarder.recordLessonCompletion(childId, "science", "week-one-3")
        assertEquals(1, collectedBadges.size)

        mockDate("2026-08-10")
        val newWeek = awarder.recordLessonCompletion(childId, "english", "week-two-1")
        assertEquals(1, newWeek.completedCount)
        assertFalse(newWeek.expeditionComplete)
        assertEquals(1, collectedBadges.size)
    }

    @Test
    fun `awarded state survives a fresh awarder instance`() = runTest {
        val awarder1 = awarder()
        awarder1.recordLessonCompletion(childId, "english", "one")
        awarder1.recordLessonCompletion(childId, "gmrc", "two")
        awarder1.recordLessonCompletion(childId, "science", "three")

        val awarder2 = awarder()
        val result = awarder2.recordLessonCompletion(childId, "science", "three")
        assertTrue(result.expeditionComplete)
        assertNull(result.newlyAwardedBadge)
        assertEquals(1, collectedBadges.size)
    }

    private fun awarder() = BadgeAwarder(expeditionDao, collectedBadgeDao, badgeLoader)

    private fun mockDate(value: String) {
        mockkStatic(LocalDate::class)
        every { LocalDate.now(any<ZoneId>()) } returns LocalDate.parse(value)
    }
}
