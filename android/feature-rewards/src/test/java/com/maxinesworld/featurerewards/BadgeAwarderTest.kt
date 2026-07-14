package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.DailyChallengeDao
import com.maxinesworld.coredatabase.DailyChallengeEntity
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coremodel.gamification.AttemptQualification
import com.maxinesworld.coremodel.gamification.WildlifeDiscoveryMetadata
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
        CollectibleBadge(id = "mammal_tarsier", biome = "forest_friends", name = "Tarsier", title = "Moon-Eyed", funFact = "Big eyes", emoji = "\uD83D\uDC12"),
        CollectibleBadge(id = "mammal_tamaraw", biome = "forest_friends", name = "Tamaraw", title = "Mini Buffalo", funFact = "Rare", emoji = "\uD83D\uDC03"),
        CollectibleBadge(id = "bird_eagle", biome = "sky_scouts", name = "Eagle", title = "Forest King", funFact = "National bird", emoji = "\uD83E\uDD85"),
    )

    // ── Mocks ──
    private lateinit var dailyChallengeDao: DailyChallengeDao
    private lateinit var collectedBadgeDao: CollectedBadgeDao
    private lateinit var badgeLoader: BadgeLoader

    private var currentChallenge: DailyChallengeEntity? = null

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        dailyChallengeDao = mockk()
        collectedBadgeDao = mockk()
        badgeLoader = mockk()

        coEvery { dailyChallengeDao.getByChildAndDate(childId, todayDate) } answers { currentChallenge }
        coEvery { dailyChallengeDao.getByChildAndDate(childId, not(todayDate)) } answers { null }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            currentChallenge = firstArg()
        }
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns emptyList()
        coEvery { collectedBadgeDao.insertIgnoring(any()) } returns 1L
        coEvery { collectedBadgeDao.countByChild(any()) } returns 0
        coEvery { collectedBadgeDao.countByBiome(any(), any()) } returns 0
        coEvery { badgeLoader.loadAll() } returns allTestBadges
    }

    @After
    fun tearDown() {
        currentChallenge = null
        unmockkAll()
    }

    // ════════════════════════════════════════════════
    // Daily challenge progress tracking (legacy — no wildlife badges)
    // ════════════════════════════════════════════════

    @Test
    fun `single subject completion updates progress but does NOT award badge`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday(todayDate)

        val result = awarder.recordSubjectCompletion(childId, "english")

        assertTrue("english should be completed", result.english)
        assertFalse("filipino should be false", result.filipino)
        assertEquals("completedCount", 1, result.completedCount)
        assertNull("no badge awarded", result.newlyAwardedBadge)

        coVerify(exactly = 1) { dailyChallengeDao.upsert(any()) }
        coVerify(exactly = 0) { collectedBadgeDao.insertIgnoring(any()) }
    }

    @Test
    fun `all 5 subjects tracked but no badge from daily challenge`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday(todayDate)

        for (subj in BadgeAwarder.SUBJECTS) {
            val r = awarder.recordSubjectCompletion(childId, subj)
            assertNull("no badge after $subj", r.newlyAwardedBadge)
        }

        // Progress should show all 5 completed
        val last = awarder.recordSubjectCompletion(childId, "makabansa")
        assertEquals("completedCount", 5, last.completedCount)
        assertNull("still no wildlife badge from daily challenge", last.newlyAwardedBadge)

        // No badge inserts from daily challenge
        coVerify(exactly = 0) { collectedBadgeDao.insertIgnoring(any()) }
    }

    @Test
    fun `same day repeated calls track idempotently`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday(todayDate)

        for (subj in BadgeAwarder.SUBJECTS) {
            awarder.recordSubjectCompletion(childId, subj)
        }

        val repeatResult = awarder.recordSubjectCompletion(childId, "english")

        assertEquals("completedCount stays 5", 5, repeatResult.completedCount)
        assertNull("no badge on repeat", repeatResult.newlyAwardedBadge)
        coVerify(exactly = 0) { collectedBadgeDao.insertIgnoring(any()) }
    }

    @Test
    fun `completed challenge with badgeAwarded true is handled idempotently`() = runTest {
        mockToday(todayDate)

        currentChallenge = DailyChallengeEntity(
            id = "${childId}_$todayDate", childId = childId, challengeDate = todayDate,
            englishCompleted = true, filipinoCompleted = true, mathematicsCompleted = true,
            scienceCompleted = true, makabansaCompleted = true,
            allCompleted = true, badgeAwarded = true, awardedBadgeId = "badge_01"
        )

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        val result = awarder.recordSubjectCompletion(childId, "science")

        assertEquals("completedCount", 5, result.completedCount)
        assertNull("no badge re-awarded", result.newlyAwardedBadge)
        coVerify(exactly = 0) { collectedBadgeDao.insertIgnoring(any()) }
    }

    @Test
    fun `next day partial progress resets to zero`() = runTest {
        val day1 = "2026-07-13"
        val day2 = "2026-07-14"

        val day1ChallengeSlot = mutableListOf<DailyChallengeEntity>()
        coEvery { dailyChallengeDao.getByChildAndDate(childId, day1) } answers { day1ChallengeSlot.lastOrNull() }
        coEvery { dailyChallengeDao.getByChildAndDate(childId, day2) } answers { null }
        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers {
            val date = firstArg<String>()
            if (date == day1) day1ChallengeSlot.lastOrNull() else null
        }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            val entity = firstArg<DailyChallengeEntity>()
            if (entity.challengeDate == day1) day1ChallengeSlot.add(entity)
        }

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        mockToday(day1)
        for (subj in listOf("english", "filipino", "mathematics")) {
            awarder.recordSubjectCompletion(childId, subj)
        }

        mockToday(day2)
        val day2Result = awarder.recordSubjectCompletion(childId, "science")

        assertEquals("day 2 completedCount is 1", 1, day2Result.completedCount)
        assertTrue("day 2 science true", day2Result.science)
        assertFalse("day 2 english false", day2Result.english)
    }

    // ════════════════════════════════════════════════
    // Wildlife badge evaluation (new content-metadata path)
    // ════════════════════════════════════════════════

    @Test
    fun `evaluateAndAwardWildlifeBadge awards matching badge`() = runTest {
        mockToday(todayDate)

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val meta = WildlifeDiscoveryMetadata(
            badgeId = "mammal_tarsier",
            trigger = "QUALIFYING_COMPLETION",
            factActivityId = "english-main-idea-tarsier"
        )
        val facts = AttemptQualification(
            lessonId = "english-g3-m01-d01",
            attemptId = "attempt_1",
            requiredSequenceCompleted = true,
            isPreviewOrDemo = false,
            isParentTestMode = false,
            hasScoredInteraction = true,
            activityTypeIsNonScored = false,
        )

        val result = awarder.evaluateAndAwardWildlifeBadge(childId, meta, facts)

        assertNotNull("badge should be awarded", result)
        assertEquals("mammal_tarsier", result!!.id)
        assertTrue("isCollected", result.isCollected)

        coVerify(exactly = 1) { collectedBadgeDao.insertIgnoring(any()) }
    }

    @Test
    fun `evaluateAndAwardWildlifeBadge returns null when already collected`() = runTest {
        mockToday(todayDate)

        // Tarsier already collected
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns listOf(
            CollectedBadgeEntity(id = "${childId}_mammal_tarsier", childId = childId, badgeId = "mammal_tarsier", biome = "forest_friends", earnedDate = "2026-07-12"),
        )

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val meta = WildlifeDiscoveryMetadata(badgeId = "mammal_tarsier", trigger = "QUALIFYING_COMPLETION", factActivityId = "abc")
        val facts = AttemptQualification(lessonId = "x", attemptId = "y", requiredSequenceCompleted = true, isPreviewOrDemo = false, isParentTestMode = false, hasScoredInteraction = true, activityTypeIsNonScored = false)

        val result = awarder.evaluateAndAwardWildlifeBadge(childId, meta, facts)

        assertNull("already collected — no award", result)
        coVerify(exactly = 0) { collectedBadgeDao.insertIgnoring(any()) }
    }

    @Test
    fun `evaluateAndAwardWildlifeBadge returns null when no mapping`() = runTest {
        mockToday(todayDate)

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val meta = null // no wildlife_discovery in lesson
        val facts = AttemptQualification(lessonId = "x", attemptId = "y", requiredSequenceCompleted = true, isPreviewOrDemo = false, isParentTestMode = false, hasScoredInteraction = true, activityTypeIsNonScored = false)

        val result = awarder.evaluateAndAwardWildlifeBadge(childId, meta, facts)

        assertNull("no mapping — no award", result)
    }

    // ════════════════════════════════════════════════
    // Remaining legacy tests (still valid with new contract)
    // ════════════════════════════════════════════════

    @Test
    fun `unknown subject does not change progress`() = runTest {
        mockToday(todayDate)

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        val result = awarder.recordSubjectCompletion(childId, "history")

        assertEquals("completedCount is 0", 0, result.completedCount)
        assertFalse("english false", result.english)
    }

    @Test
    fun `getTodayProgress returns correct progress`() = runTest {
        mockToday(todayDate)
        currentChallenge = DailyChallengeEntity(
            id = "${childId}_$todayDate", childId = childId, challengeDate = todayDate,
            englishCompleted = true, filipinoCompleted = true, mathematicsCompleted = false,
            scienceCompleted = false, makabansaCompleted = false,
            allCompleted = false, badgeAwarded = false
        )

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        val progress = awarder.getTodayProgress(childId)

        assertEquals("completedCount", 2, progress.completedCount)
        assertTrue("english", progress.english)
        assertTrue("filipino", progress.filipino)
        assertNull("newlyAwardedBadge is always null from getTodayProgress", progress.newlyAwardedBadge)
    }

    @Test
    fun `getCollectedBadges marks isCollected correctly`() = runTest {
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns listOf(
            CollectedBadgeEntity(id = "${childId}_mammal_tarsier", childId = childId, badgeId = "mammal_tarsier", biome = "forest_friends", earnedDate = "2026-07-11"),
        )

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        val badges = awarder.getCollectedBadges(childId)

        assertEquals("all badges returned", 3, badges.size)
        val collected = badges.filter { it.isCollected }
        assertEquals("one collected", 1, collected.size)
        assertEquals("mammal_tarsier is collected", "mammal_tarsier", collected.first().id)
    }

    // ─── helpers ───

    private fun mockToday(dateString: String) {
        mockkStatic(LocalDate::class)
        val parsed = LocalDate.parse(dateString)
        every { LocalDate.now(any<ZoneId>()) } returns parsed
    }
}
