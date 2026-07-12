package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.DailyChallengeDao
import com.maxinesworld.coredatabase.DailyChallengeEntity
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

/**
 * Transaction-level tests for BadgeAwarder: idempotency, concurrency,
 * date boundaries, and restart survival.
 */
class BadgeAwarderTransactionTest {

    private val childId = "child_txn_1"

    // 22 badges to support the 20-day test
    private val allTestBadges = (1..22).map { i ->
        CollectibleBadge(
            id = "badge_%02d".format(i),
            biome = when {
                i <= 10 -> "forest_friends"
                i <= 20 -> "sky_scouts"
                else -> "songbird_grove"
            },
            name = "Badge $i",
            title = "Title $i",
            funFact = "Fact $i",
            emoji = "\uD83C\uDF1F"
        )
    }

    // ── Mocks ──
    private lateinit var dailyChallengeDao: DailyChallengeDao
    private lateinit var collectedBadgeDao: CollectedBadgeDao
    private lateinit var badgeLoader: BadgeLoader

    // Track current-challenge state across calls (in-memory persistence simulation)
    private var currentChallenge: DailyChallengeEntity? = null
    private val collectedBadges = mutableListOf<CollectedBadgeEntity>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        dailyChallengeDao = mockk()
        collectedBadgeDao = mockk()
        badgeLoader = mockk()

        currentChallenge = null
        collectedBadges.clear()

        // Wire DAO to in-memory slot
        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers {
            val date = secondArg<String>()
            if (currentChallenge?.challengeDate == date) currentChallenge else null
        }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            currentChallenge = firstArg()
        }

        // Collected badges tracked in mutable list
        coEvery { collectedBadgeDao.getAllByChild(childId) } answers { collectedBadges.toList() }
        coEvery { collectedBadgeDao.insert(any()) } answers {
            collectedBadges.add(firstArg())
        }
        coEvery { collectedBadgeDao.countByChild(childId) } answers { collectedBadges.size }

        coEvery { badgeLoader.loadAll() } returns allTestBadges
    }

    @After
    fun tearDown() {
        currentChallenge = null
        collectedBadges.clear()
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // 1. 5 distinct subjects award 1 badge
    // ─────────────────────────────────────────────
    @Test
    fun `five distinct subjects award exactly one badge`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday("2026-07-13")

        var awarded: CollectibleBadge? = null
        for (subj in BadgeAwarder.SUBJECTS) {
            val result = awarder.recordSubjectCompletion(childId, subj)
            if (result.newlyAwardedBadge != null) {
                awarded = result.newlyAwardedBadge
            }
        }

        assertNotNull("a badge should be awarded after all 5 subjects", awarded)
        assertEquals("exactly one badge awarded", 1, collectedBadges.size)
        assertEquals("correct badge id", "badge_01", awarded!!.id)
        assertTrue("challenge marked allCompleted", currentChallenge?.allCompleted == true)
        assertTrue("challenge marked badgeAwarded", currentChallenge?.badgeAwarded == true)
    }

    // ─────────────────────────────────────────────
    // 2. 5 lessons in one subject award NO badge
    // ─────────────────────────────────────────────
    @Test
    fun `five lessons in a single subject award zero badges`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday("2026-07-13")

        // Complete "english" 5 times — same subject, same day
        for (i in 1..5) {
            val result = awarder.recordSubjectCompletion(childId, "english")
            assertNull("no badge on call $i", result.newlyAwardedBadge)
            assertEquals("completedCount stays at 1", 1, result.completedCount)
        }

        assertEquals("zero badges collected", 0, collectedBadges.size)

        // Progress only shows 1 subject done
        val progress = awarder.getTodayProgress(childId)
        assertEquals("one subject done", 1, progress.completedCount)
        assertTrue("only english is true", progress.english && !progress.filipino
                && !progress.mathematics && !progress.science && !progress.makabansa)
    }

    // ─────────────────────────────────────────────
    // 3. Replay creates no duplicate credit
    // ─────────────────────────────────────────────
    @Test
    fun `replay of same completion does not duplicate badge credit`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday("2026-07-13")

        // Complete all 5 → badge awarded
        for (subj in BadgeAwarder.SUBJECTS) {
            awarder.recordSubjectCompletion(childId, subj)
        }
        assertEquals("one badge earned", 1, collectedBadges.size)

        // Replay all 5 (e.g., app data reload from server)
        for (subj in BadgeAwarder.SUBJECTS) {
            val result = awarder.recordSubjectCompletion(childId, subj)
            assertNull("no badge on replay of $subj", result.newlyAwardedBadge)
        }

        assertEquals("still exactly one badge", 1, collectedBadges.size)
    }

    // ─────────────────────────────────────────────
    // 4. Concurrent callbacks create one award
    // ─────────────────────────────────────────────
    @Test
    fun `concurrent subject completions result in exactly one badge`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday("2026-07-13")

        // Simulate 5 parallel callbacks — all subjects complete at once
        val results = BadgeAwarder.SUBJECTS.map { subj ->
            async { awarder.recordSubjectCompletion(childId, subj) }
        }.awaitAll()

        val awardedCount = results.count { it.newlyAwardedBadge != null }
        assertEquals("exactly one call returns a badge", 1, awardedCount)
        assertEquals("exactly one badge collected", 1, collectedBadges.size)
        assertTrue("challenge marked badgeAwarded", currentChallenge?.badgeAwarded == true)
    }

    // ─────────────────────────────────────────────
    // 5. Midnight separates daily progress
    // ─────────────────────────────────────────────
    @Test
    fun `midnight boundary resets progress for the new day`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val day1 = "2026-07-13"
        val day2 = "2026-07-14"

        // Override DAO mock to handle two distinct dates
        val day1Challenge = mutableListOf<DailyChallengeEntity>()
        coEvery { dailyChallengeDao.getByChildAndDate(childId, day1) } answers { day1Challenge.lastOrNull() }
        coEvery { dailyChallengeDao.getByChildAndDate(childId, day2) } answers { null }
        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers {
            val date = secondArg<String>()
            when (date) {
                day1 -> day1Challenge.lastOrNull()
                else -> null
            }
        }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            val entity = firstArg<DailyChallengeEntity>()
            if (entity.challengeDate == day1) day1Challenge.add(entity)
            else currentChallenge = entity
        }

        // Day 1: complete all 5 subjects → badge 01
        mockToday(day1)
        for (subj in BadgeAwarder.SUBJECTS) {
            awarder.recordSubjectCompletion(childId, subj)
        }
        assertEquals("day 1 badge", 1, collectedBadges.size)

        // Day 2: different date, progress starts from scratch
        mockToday(day2)
        val day2First = awarder.recordSubjectCompletion(childId, "english")
        assertEquals("day 2 starts fresh — completedCount is 1", 1, day2First.completedCount)
        assertNull("no badge on first subject of day 2", day2First.newlyAwardedBadge)
        assertEquals("day 1 badge still the only one", 1, collectedBadges.size)
    }

    // ─────────────────────────────────────────────
    // 6. Next day partial progress resets
    // ─────────────────────────────────────────────
    @Test
    fun `partial progress does not leak into next day`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val day1 = "2026-07-13"
        val day2 = "2026-07-14"

        val dayChallenges = mutableMapOf<String, DailyChallengeEntity?>()
        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers {
            dayChallenges[secondArg()]
        }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            val entity = firstArg<DailyChallengeEntity>()
            dayChallenges[entity.challengeDate] = entity
        }

        // Day 1: only complete 3 subjects (english, filipino, mathematics)
        mockToday(day1)
        val day1Subjects = listOf("english", "filipino", "mathematics")
        for (subj in day1Subjects) {
            val r = awarder.recordSubjectCompletion(childId, subj)
            assertNull("no badge on day 1", r.newlyAwardedBadge)
            assertEquals(day1Subjects.indexOf(subj) + 1, r.completedCount)
        }

        // Day 2: fresh start
        mockToday(day2)
        val day2Result = awarder.recordSubjectCompletion(childId, "science")
        assertEquals("day 2 count is 1, not 4", 1, day2Result.completedCount)
        assertTrue("day 2 science", day2Result.science)
        assertFalse("day 2 english should be false", day2Result.english)
        assertFalse("day 2 filipino should be false", day2Result.filipino)
        assertFalse("day 2 mathematics should be false", day2Result.mathematics)
        assertFalse("day 2 makabansa should be false", day2Result.makabansa)
        assertEquals("no badges yet", 0, collectedBadges.size)
    }

    // ─────────────────────────────────────────────
    // 7. 20 days produce exactly 20 badges (first 20 catalog positions)
    // ─────────────────────────────────────────────
    @Test
    fun `twenty days of completion produce twenty distinct badges in catalog order`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        // Setup: track challenges per date
        val dayChallenges = mutableMapOf<String, DailyChallengeEntity?>()
        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers {
            dayChallenges[secondArg()]
        }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            val entity = firstArg<DailyChallengeEntity>()
            dayChallenges[entity.challengeDate] = entity
        }

        // Simulate 20 consecutive days
        for (day in 1..20) {
            val dayStr = "2026-07-%02d".format(day)
            mockToday(dayStr)

            for (subj in BadgeAwarder.SUBJECTS) {
                awarder.recordSubjectCompletion(childId, subj)
            }
        }

        assertEquals("20 badges collected", 20, collectedBadges.size)

        // Verify badges are in catalog order (first 20 positions)
        collectedBadges.forEachIndexed { index, badge ->
            val expectedId = "badge_%02d".format(index + 1)
            assertEquals("badge at position $index should be $expectedId", expectedId, badge.badgeId)
        }

        // Verify all 20 are distinct
        val distinctIds = collectedBadges.map { it.badgeId }.toSet()
        assertEquals("all 20 badges are distinct", 20, distinctIds.size)
    }

    // ─────────────────────────────────────────────
    // 8. Badge reveal survives restart (pending flag)
    // ─────────────────────────────────────────────
    @Test
    fun `badge awarded flag survives app restart simulation`() = runTest {
        mockToday("2026-07-13")

        // Phase 1: Complete all subjects and award badge
        val awarder1 = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        for (subj in BadgeAwarder.SUBJECTS) {
            awarder1.recordSubjectCompletion(childId, subj)
        }
        assertTrue("badge awarded flag is set", currentChallenge?.badgeAwarded == true)
        assertEquals("one badge collected", 1, collectedBadges.size)

        // Phase 2: "Restart" — create a fresh BadgeAwarder instance
        // The in-memory challenge slot persists (simulating Room persistence)
        val savedChallenge = currentChallenge
        val savedBadges = collectedBadges.toList()

        // Reset DAO to simulate fresh app start reading from DB
        coEvery { dailyChallengeDao.getByChildAndDate(childId, "2026-07-13") } returns savedChallenge
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns savedBadges

        val awarder2 = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        // The challenge should still show badgeAwarded=true
        val repeated = awarder2.recordSubjectCompletion(childId, "english")
        assertNull("no badge re-awarded after restart", repeated.newlyAwardedBadge)

        // Verify badge collection is still intact
        val collected = awarder2.getCollectedBadges(childId)
        val earnedCount = collected.count { it.isCollected }
        assertEquals("earned count still 1 after restart", 1, earnedCount)
        assertEquals("badge_01 is still collected", "badge_01", collected.first { it.isCollected }.id)
    }

    // ─── helpers ───

    private fun mockToday(dateString: String) {
        mockkStatic(LocalDate::class)
        val parsed = LocalDate.parse(dateString)
        every { LocalDate.now(any<ZoneId>()) } returns parsed
    }
}
