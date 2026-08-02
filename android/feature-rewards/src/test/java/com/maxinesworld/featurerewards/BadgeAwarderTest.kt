package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.WildlifeExpeditionDao
import com.maxinesworld.coredatabase.WildlifeExpeditionEntity
import com.maxinesworld.coremodel.CollectibleBadge
import io.mockk.MockKAnnotations
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BadgeAwarderTest {
    private val childId = "child_test_1"
    private val badges = listOf(
        CollectibleBadge("badge_01", "forest_friends", "Tarsier", "Moon-Eyed", "Big eyes", "🐒"),
        CollectibleBadge("badge_02", "forest_friends", "Tamaraw", "Mini Buffalo", "Rare", "🐃"),
        CollectibleBadge("badge_03", "sky_scouts", "Eagle", "Forest King", "National bird", "🦅"),
    )

    private lateinit var expeditionDao: WildlifeExpeditionDao
    private lateinit var collectedBadgeDao: CollectedBadgeDao
    private lateinit var badgeLoader: BadgeLoader
    private var current: WildlifeExpeditionEntity? = null

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        expeditionDao = mockk()
        collectedBadgeDao = mockk()
        badgeLoader = mockk()
        coEvery { expeditionDao.getByChildAndWeek(childId, any()) } answers { current }
        coEvery { expeditionDao.upsert(any()) } answers { current = firstArg() }
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns emptyList()
        coEvery { collectedBadgeDao.insert(any()) } just runs
        coEvery { badgeLoader.loadAll() } returns badges
        mockDate("2026-08-03") // Monday; all following dates are the same week.
    }

    @After
    fun tearDown() {
        current = null
        unmockkAll()
    }

    @Test
    fun `one lesson is progress but not a reward`() = runTest {
        val result = awarder().recordLessonCompletion(childId, "english", "english-g3-lesson-01")

        assertEquals(1, result.completedCount)
        assertEquals(1, result.subjectCount)
        assertTrue(result.english)
        assertFalse(result.expeditionComplete)
        assertNull(result.newlyAwardedBadge)
        coVerify(exactly = 1) { expeditionDao.upsert(any()) }
        coVerify(exactly = 0) { collectedBadgeDao.insert(any()) }
    }

    @Test
    fun `three distinct lessons across two areas award the next badge`() = runTest {
        awarder().recordLessonCompletion(childId, "english", "english-g3-lesson-01")
        awarder().recordLessonCompletion(childId, "gmrc", "gmrc-g3-lesson-01")
        val result = awarder().recordLessonCompletion(childId, "science", "science-g3-lesson-01")

        assertEquals(3, result.completedCount)
        assertEquals(3, result.subjectCount)
        assertTrue(result.gmrc)
        assertTrue(result.expeditionComplete)
        assertEquals("badge_01", result.newlyAwardedBadge?.id)
        coVerify(exactly = 1) { collectedBadgeDao.insert(any()) }
    }

    @Test
    fun `same lesson replay does not create progress or a second reward`() = runTest {
        val awarder = awarder()
        awarder.recordLessonCompletion(childId, "english", "lesson-1")
        awarder.recordLessonCompletion(childId, "gmrc", "lesson-2")
        val first = awarder.recordLessonCompletion(childId, "science", "lesson-3")
        val replay = awarder.recordLessonCompletion(childId, "science", "lesson-3")

        assertNotNull(first.newlyAwardedBadge)
        assertNull(replay.newlyAwardedBadge)
        assertEquals(3, replay.completedCount)
        coVerify(exactly = 1) { collectedBadgeDao.insert(any()) }
    }

    @Test
    fun `progress persists across days without a reset`() = runTest {
        val awarder = awarder()
        awarder.recordLessonCompletion(childId, "english", "lesson-1")
        mockDate("2026-08-04") // Tuesday, same local week.
        val result = awarder.recordLessonCompletion(childId, "gmrc", "lesson-2")

        assertEquals(2, result.completedCount)
        assertTrue(result.english)
        assertTrue(result.gmrc)
        assertFalse(result.expeditionComplete)
    }

    @Test
    fun `araling panlipunan is normalized and unknown subjects are ignored`() = runTest {
        val awarder = awarder()
        awarder.recordLessonCompletion(childId, "araling-panlipunan", "ap-1")
        val result = awarder.recordLessonCompletion(childId, "history", "history-1")

        assertEquals(1, result.completedCount)
        assertEquals(1, result.subjectCount)
        assertTrue(result.makabansa)
        assertFalse(result.english)
        assertNull(result.newlyAwardedBadge)
    }

    @Test
    fun `already awarded expedition cannot award again`() = runTest {
        current = WildlifeExpeditionEntity(
            id = "${childId}_2026-08-03",
            childId = childId,
            weekKey = "2026-08-03",
            completedLessonIds = "one|two|three",
            subjectKeys = "english|gmrc",
            badgeAwarded = true,
            awardedBadgeId = "badge_01",
        )
        val result = awarder().recordLessonCompletion(childId, "english", "one")

        assertTrue(result.expeditionComplete)
        assertNull(result.newlyAwardedBadge)
        coVerify(exactly = 0) { collectedBadgeDao.insert(any()) }
    }

    @Test
    fun `collected badges include persisted collection metadata`() = runTest {
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns listOf(
            CollectedBadgeEntity(
                id = "${childId}_badge_01",
                childId = childId,
                badgeId = "badge_01",
                biome = "forest_friends",
                earnedDate = "2026-08-03",
                earnedAtEpochMillis = 42L,
            )
        )

        val result = awarder().getCollectedBadges(childId)
        assertEquals(3, result.size)
        assertTrue(result.first { it.id == "badge_01" }.isCollected)
        assertEquals(42L, result.first { it.id == "badge_01" }.collectedAtEpochMillis)
    }

    private fun awarder() = BadgeAwarder(expeditionDao, collectedBadgeDao, badgeLoader)

    private fun mockDate(value: String) {
        mockkStatic(LocalDate::class)
        every { LocalDate.now(any<ZoneId>()) } returns LocalDate.parse(value)
    }
}
