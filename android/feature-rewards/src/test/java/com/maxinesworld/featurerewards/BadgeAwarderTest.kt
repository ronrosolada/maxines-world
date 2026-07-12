package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.DailyChallengeDao
import com.maxinesworld.coredatabase.DailyChallengeEntity
import com.maxinesworld.coremodel.CollectibleBadge
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BadgeAwarderTest {

    // ── Test fixtures ──
    private val childId = "child_test_1"
    private val todayDate = "2026-07-13"

    private val allTestBadges = listOf(
        CollectibleBadge(id = "badge_01", biome = "forest_friends", name = "Tarsier", title = "Moon-Eyed", funFact = "Big eyes", emoji = "\uD83D\uDC12"),
        CollectibleBadge(id = "badge_02", biome = "forest_friends", name = "Tamaraw", title = "Mini Buffalo", funFact = "Rare", emoji = "\uD83D\uDC03"),
        CollectibleBadge(id = "badge_03", biome = "sky_scouts", name = "Eagle", title = "Forest King", funFact = "National bird", emoji = "\uD83E\uDD85"),
        CollectibleBadge(id = "badge_04", biome = "sky_scouts", name = "Cockatoo", title = "Red-Tail", funFact = "Red flash", emoji = "\uD83E\uDD9C"),
        CollectibleBadge(id = "badge_05", biome = "songbird_grove", name = "Flowerpecker", title = "Scarlet", funFact = "Thought extinct", emoji = "\uD83D\uDC26"),
    )

    // ── Mocks ──
    private lateinit var dailyChallengeDao: DailyChallengeDao
    private lateinit var collectedBadgeDao: CollectedBadgeDao
    private lateinit var badgeLoader: BadgeLoader

    // Track the current-challenge state to simulate DAO persistence across calls
    private var currentChallenge: DailyChallengeEntity? = null

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        dailyChallengeDao = mockk()
        collectedBadgeDao = mockk()
        badgeLoader = mockk()

        // Wire mock DAO to a mutable challenge slot so successive calls see prior upserts
        coEvery { dailyChallengeDao.getByChildAndDate(childId, todayDate) } answers { currentChallenge }
        coEvery { dailyChallengeDao.getByChildAndDate(childId, not(todayDate)) } answers { null }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            currentChallenge = firstArg()
        }
        // By default no badges collected
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns emptyList()
        coEvery { collectedBadgeDao.insert(any()) } just runs
        coEvery { badgeLoader.loadAll() } returns allTestBadges
    }

    @After
    fun tearDown() {
        currentChallenge = null
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // TEST: single-subject completion (no badge)
    // ─────────────────────────────────────────────
    @Test
    fun `single subject completion updates progress but does NOT award badge`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday(todayDate)

        val result = awarder.recordSubjectCompletion(childId, "english")

        // Progress
        assertTrue("english should be completed", result.english)
        assertFalse("filipino should be false", result.filipino)
        assertFalse("mathematics should be false", result.mathematics)
        assertFalse("science should be false", result.science)
        assertFalse("makabansa should be false", result.makabansa)
        assertEquals("completedCount", 1, result.completedCount)
        assertNull("no badge awarded", result.newlyAwardedBadge)

        // DAO upsert was called
        coVerify(exactly = 1) { dailyChallengeDao.upsert(any()) }
        // No badge insert
        coVerify(exactly = 0) { collectedBadgeDao.insert(any()) }
    }

    // ─────────────────────────────────────────────
    // TEST: all 5 subjects awards badge
    // ─────────────────────────────────────────────
    @Test
    fun `completing all 5 subjects in one day awards next unearned badge`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday(todayDate)

        // Complete first 4 subjects — no badge
        for (subj in listOf("english", "filipino", "mathematics", "science")) {
            val r = awarder.recordSubjectCompletion(childId, subj)
            assertNull("no badge after $subj", r.newlyAwardedBadge)
        }

        // Complete 5th subject — badge should be awarded
        val result = awarder.recordSubjectCompletion(childId, "makabansa")

        assertNotNull("badge should be awarded", result.newlyAwardedBadge)
        assertEquals("first badge id", "badge_01", result.newlyAwardedBadge!!.id)
        assertEquals("completedCount", 5, result.completedCount)
        assertTrue("all subjects completed", result.english && result.filipino && result.mathematics && result.science && result.makabansa)

        // Verify persistence: one insert call
        coVerify(exactly = 1) { collectedBadgeDao.insert(any()) }
        // Verify upsert was called at least 5 times
        coVerify(atLeast = 5) { dailyChallengeDao.upsert(any()) }
    }

    // ─────────────────────────────────────────────
    // TEST: idempotency — same-day repeat does not double-award
    // ─────────────────────────────────────────────
    @Test
    fun `same day repeated calls do NOT double-award badge`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday(todayDate)

        // Complete all 5 subjects → badge awarded
        for (subj in BadgeAwarder.SUBJECTS) {
            awarder.recordSubjectCompletion(childId, subj)
        }

        // Now repeat a subject completion on the SAME day
        val repeatResult = awarder.recordSubjectCompletion(childId, "english")

        assertNull("no badge on repeat call", repeatResult.newlyAwardedBadge)
        // Still only one insert
        coVerify(exactly = 1) { collectedBadgeDao.insert(any()) }
    }

    // ─────────────────────────────────────────────
    // TEST: idempotency — after badgeAwarded flag is set, no re-award
    // ─────────────────────────────────────────────
    @Test
    fun `badge not re-awarded when challenge already has badgeAwarded true`() = runTest {
        mockToday(todayDate)

        // Simulate existing completed challenge with badge already awarded
        currentChallenge = DailyChallengeEntity(
            id = "${childId}_$todayDate",
            childId = childId,
            challengeDate = todayDate,
            englishCompleted = true,
            filipinoCompleted = true,
            mathematicsCompleted = true,
            scienceCompleted = true,
            makabansaCompleted = true,
            allCompleted = true,
            badgeAwarded = true,
            awardedBadgeId = "badge_01"
        )

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        // Call record for any subject — it's already all-done
        val result = awarder.recordSubjectCompletion(childId, "science")

        assertNull("no badge re-awarded", result.newlyAwardedBadge)
        coVerify(exactly = 0) { collectedBadgeDao.insert(any()) }
    }

    // ─────────────────────────────────────────────
    // TEST: next-day partial progress resets
    // ─────────────────────────────────────────────
    @Test
    fun `next day partial progress resets to zero for the new day`() = runTest {
        val day1 = "2026-07-13"
        val day2 = "2026-07-14"

        // Wire the DAO mock for both dates explicitly
        val day1ChallengeSlot = mutableListOf<DailyChallengeEntity>()
        coEvery { dailyChallengeDao.getByChildAndDate(childId, day1) } answers { day1ChallengeSlot.lastOrNull() }
        coEvery { dailyChallengeDao.getByChildAndDate(childId, day2) } answers { null }
        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers {
            val date = firstArg<String>()
            if (date == day1) day1ChallengeSlot.lastOrNull() else null
        }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            val entity = firstArg<DailyChallengeEntity>()
            if (entity.challengeDate == day1) {
                day1ChallengeSlot.add(entity)
            }
        }

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        // ── Day 1: complete 3 subjects ──
        mockToday(day1)
        for (subj in listOf("english", "filipino", "mathematics")) {
            val r = awarder.recordSubjectCompletion(childId, subj)
            assertNull("no badge on day 1", r.newlyAwardedBadge)
        }

        // ── Day 2: fresh start, complete 1 subject ──
        mockToday(day2)
        val day2Result = awarder.recordSubjectCompletion(childId, "science")

        assertEquals("day 2 completedCount is 1 (not 4)", 1, day2Result.completedCount)
        assertTrue("day 2 science true", day2Result.science)
        assertFalse("day 2 english false", day2Result.english)
        assertFalse("day 2 filipino false", day2Result.filipino)
        assertFalse("day 2 mathematics false", day2Result.mathematics)
        assertFalse("day 2 makabansa false", day2Result.makabansa)
        assertNull("no badge on day 2", day2Result.newlyAwardedBadge)
    }

    // ─────────────────────────────────────────────
    // TEST: badge ID is the correct next-unearned badge
    // ─────────────────────────────────────────────
    @Test
    fun `awarded badge is the first badge not yet collected`() = runTest {
        mockToday(todayDate)

        // Use a mutable list to track collected badges dynamically
        val collectedBadges = mutableListOf(
            CollectedBadgeEntity(id = "${childId}_badge_01", childId = childId, badgeId = "badge_01", biome = "forest_friends", earnedDate = "2026-07-11"),
            CollectedBadgeEntity(id = "${childId}_badge_02", childId = childId, badgeId = "badge_02", biome = "forest_friends", earnedDate = "2026-07-12"),
        )
        coEvery { collectedBadgeDao.getAllByChild(childId) } answers { collectedBadges.toList() }
        coEvery { collectedBadgeDao.insert(any()) } answers {
            collectedBadges.add(firstArg())
        }

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        // Complete all 5 subjects — 5th subject should trigger award
        var lastResult: ChallengeProgress? = null
        for (subj in BadgeAwarder.SUBJECTS) {
            lastResult = awarder.recordSubjectCompletion(childId, subj)
        }

        // The last result (makabansa) should have awarded badge_03 (next unearned)
        assertNotNull("badge awarded on 5th subject", lastResult?.newlyAwardedBadge)
        assertEquals("badge_03 is next unearned", "badge_03", lastResult?.newlyAwardedBadge?.id)
        assertEquals("badge_03 biome", "sky_scouts", lastResult?.newlyAwardedBadge?.biome)

        // Verify total collected badges increased by 1
        assertEquals("3 badges now collected", 3, collectedBadges.size)
        assertEquals("badge_03 was inserted", "badge_03", collectedBadges.last().badgeId)
    }

    // ─────────────────────────────────────────────
    // TEST: collected badge is persisted with correct fields
    // ─────────────────────────────────────────────
    @Test
    fun `awarded badge is persisted via CollectedBadgeDao insert`() = runTest {
        mockToday(todayDate)

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        // Capture the insert argument
        val badgeSlot = slot<CollectedBadgeEntity>()
        coEvery { collectedBadgeDao.insert(capture(badgeSlot)) } just runs

        // Complete all 5 subjects
        for (subj in BadgeAwarder.SUBJECTS) {
            awarder.recordSubjectCompletion(childId, subj)
        }

        assertTrue("insert slot captured", badgeSlot.isCaptured)
        val inserted = badgeSlot.captured
        assertEquals("childId", childId, inserted.childId)
        assertEquals("badgeId", "badge_01", inserted.badgeId)
        assertEquals("biome", "forest_friends", inserted.biome)
        assertEquals("earnedDate", todayDate, inserted.earnedDate)
        assertEquals("id", "${childId}_badge_01", inserted.id)
    }

    // ─────────────────────────────────────────────
    // TEST: unknown subject does nothing
    // ─────────────────────────────────────────────
    @Test
    fun `unknown subject does not change progress`() = runTest {
        mockToday(todayDate)

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val result = awarder.recordSubjectCompletion(childId, "history")

        assertEquals("completedCount is 0", 0, result.completedCount)
        assertFalse("english false", result.english)
    }

    // ─────────────────────────────────────────────
    // TEST: getTodayProgress reflects current state
    // ─────────────────────────────────────────────
    @Test
    fun `getTodayProgress returns correct progress after completions`() = runTest {
        mockToday(todayDate)
        currentChallenge = DailyChallengeEntity(
            id = "${childId}_$todayDate",
            childId = childId,
            challengeDate = todayDate,
            englishCompleted = true,
            filipinoCompleted = true,
            mathematicsCompleted = false,
            scienceCompleted = false,
            makabansaCompleted = false,
            allCompleted = false,
            badgeAwarded = false
        )

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        val progress = awarder.getTodayProgress(childId)

        assertEquals("completedCount", 2, progress.completedCount)
        assertTrue("english", progress.english)
        assertTrue("filipino", progress.filipino)
        assertFalse("mathematics", progress.mathematics)
        assertNull("newlyAwardedBadge is always null from getTodayProgress", progress.newlyAwardedBadge)
    }

    // ─────────────────────────────────────────────
    // TEST: getCollectedBadges marks isCollected correctly
    // ─────────────────────────────────────────────
    @Test
    fun `getCollectedBadges marks isCollected flag correctly`() = runTest {
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns listOf(
            CollectedBadgeEntity(id = "${childId}_badge_01", childId = childId, badgeId = "badge_01", biome = "forest_friends", earnedDate = "2026-07-11"),
        )

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        val badges = awarder.getCollectedBadges(childId)

        assertEquals("all badges returned", 5, badges.size)
        val collected = badges.filter { it.isCollected }
        assertEquals("one collected", 1, collected.size)
        assertEquals("badge_01 is collected", "badge_01", collected.first().id)
    }

    // ─── helpers ───

    private fun mockToday(dateString: String) {
        mockkStatic(LocalDate::class)
        val parsed = LocalDate.parse(dateString)
        every { LocalDate.now(any<ZoneId>()) } returns parsed
    }
}
