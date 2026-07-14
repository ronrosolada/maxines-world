package com.maxinesworld.featurerewards

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.DailyChallengeDao
import com.maxinesworld.coredatabase.DailyChallengeEntity
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coremodel.gamification.AttemptQualification
import com.maxinesworld.coremodel.gamification.WildlifeDiscoveryMetadata
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
 * Transaction tests: daily challenge tracking (legacy, no wildlife badges)
 * and wildlife badge evaluation (content-metadata path).
 */
class BadgeAwarderTransactionTest {

    private val childId = "child_txn_1"

    private val allTestBadges = (1..10).map { i ->
        CollectibleBadge(
            id = "badge_%02d".format(i),
            biome = if (i <= 5) "forest_friends" else "sky_scouts",
            name = "Badge $i", title = "Title $i", funFact = "Fact $i",
            emoji = "\uD83C\uDF1F"
        )
    }

    private lateinit var dailyChallengeDao: DailyChallengeDao
    private lateinit var collectedBadgeDao: CollectedBadgeDao
    private lateinit var badgeLoader: BadgeLoader

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

        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers {
            val date = secondArg<String>()
            if (currentChallenge?.challengeDate == date) currentChallenge else null
        }
        coEvery { dailyChallengeDao.upsert(any()) } answers { currentChallenge = firstArg() }
        coEvery { collectedBadgeDao.getAllByChild(childId) } answers { collectedBadges.toList() }
        coEvery { collectedBadgeDao.insert(any()) } answers { collectedBadges.add(firstArg()) }
        coEvery { collectedBadgeDao.countByChild(childId) } answers { collectedBadges.size }
        coEvery { collectedBadgeDao.countByBiome(any(), any()) } returns 0
        coEvery { badgeLoader.loadAll() } returns allTestBadges
    }

    @After
    fun tearDown() {
        currentChallenge = null
        collectedBadges.clear()
        unmockkAll()
    }

    // ════════════════════════════════════════════════
    // Daily challenge tracking (legacy — no badges awarded)
    // ════════════════════════════════════════════════

    @Test
    fun `five distinct subjects track all five but award no badge`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday("2026-07-13")

        for (subj in BadgeAwarder.SUBJECTS) {
            val result = awarder.recordSubjectCompletion(childId, subj)
            assertNull("no wildlife badge from daily challenge for $subj", result.newlyAwardedBadge)
        }

        assertEquals("zero badges from daily challenge", 0, collectedBadges.size)
        assertTrue("challenge marked allCompleted", currentChallenge?.allCompleted == true)
    }

    @Test
    fun `five lessons in a single subject award zero badges`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday("2026-07-13")

        for (i in 1..5) {
            val result = awarder.recordSubjectCompletion(childId, "english")
            assertNull("no badge on call $i", result.newlyAwardedBadge)
            assertEquals("completedCount stays at 1", 1, result.completedCount)
        }

        assertEquals("zero badges collected", 0, collectedBadges.size)

        val progress = awarder.getTodayProgress(childId)
        assertEquals("one subject done", 1, progress.completedCount)
        assertTrue("only english", progress.english && !progress.filipino)
    }

    @Test
    fun `replay of same day does not duplicate badge tracking`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday("2026-07-13")

        for (subj in BadgeAwarder.SUBJECTS) {
            awarder.recordSubjectCompletion(childId, subj)
        }

        for (subj in BadgeAwarder.SUBJECTS) {
            val result = awarder.recordSubjectCompletion(childId, subj)
            assertNull("no badge on replay of $subj", result.newlyAwardedBadge)
            assertEquals("completedCount stays 5", 5, result.completedCount)
        }

        assertEquals("no badges from daily challenge", 0, collectedBadges.size)
    }

    @Test
    fun `concurrent subject completions track without duplicate award`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        mockToday("2026-07-13")

        val results = BadgeAwarder.SUBJECTS.map { subj ->
            async { awarder.recordSubjectCompletion(childId, subj) }
        }.awaitAll()

        val awardedCount = results.count { it.newlyAwardedBadge != null }
        assertEquals("zero badges from daily challenge", 0, awardedCount)
        assertEquals("no badges inserted", 0, collectedBadges.size)
    }

    @Test
    fun `midnight boundary resets daily progress`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val day1 = "2026-07-13"
        val day2 = "2026-07-14"

        val dayChallenges = mutableMapOf<String, DailyChallengeEntity?>()
        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers { dayChallenges[secondArg()] }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            val entity = firstArg<DailyChallengeEntity>()
            dayChallenges[entity.challengeDate] = entity
        }

        mockToday(day1)
        for (subj in BadgeAwarder.SUBJECTS) awarder.recordSubjectCompletion(childId, subj)

        mockToday(day2)
        val day2First = awarder.recordSubjectCompletion(childId, "english")
        assertEquals("day 2 starts fresh", 1, day2First.completedCount)
    }

    @Test
    fun `partial progress does not leak into next day`() = runTest {
        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val day1 = "2026-07-13"
        val day2 = "2026-07-14"

        val dayChallenges = mutableMapOf<String, DailyChallengeEntity?>()
        coEvery { dailyChallengeDao.getByChildAndDate(childId, any()) } answers { dayChallenges[secondArg()] }
        coEvery { dailyChallengeDao.upsert(any()) } answers {
            dayChallenges[firstArg<DailyChallengeEntity>().challengeDate] = firstArg()
        }

        mockToday(day1)
        for (subj in listOf("english", "filipino", "mathematics")) {
            awarder.recordSubjectCompletion(childId, subj)
        }

        mockToday(day2)
        val day2Result = awarder.recordSubjectCompletion(childId, "science")
        assertEquals("day 2 count is 1", 1, day2Result.completedCount)
        assertFalse("day 2 english false", day2Result.english)
    }

    // ════════════════════════════════════════════════
    // Wildlife badge evaluation (content-metadata)
    // ════════════════════════════════════════════════

    @Test
    fun `evaluateAndAwardWildlifeBadge awards badge exactly once`() = runTest {
        mockToday("2026-07-13")

        val awarder = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val meta = WildlifeDiscoveryMetadata("badge_01", "QUALIFYING_COMPLETION", "abc")
        val facts = AttemptQualification(lessonId = "l1", attemptId = "a1", requiredSequenceCompleted = true, isPreviewOrDemo = false, isParentTestMode = false, hasScoredInteraction = true, activityTypeIsNonScored = false)

        val first = awarder.evaluateAndAwardWildlifeBadge(childId, meta, facts)
        assertNotNull("badge awarded", first)
        assertEquals("badge_01", first!!.id)
        assertEquals("one badge", 1, collectedBadges.size)

        // Duplicate call — already collected
        val second = awarder.evaluateAndAwardWildlifeBadge(childId, meta, facts)
        assertNull("already collected — no re-award", second)
        assertEquals("still one badge", 1, collectedBadges.size)
    }

    @Test
    fun `badge survives restart`() = runTest {
        mockToday("2026-07-13")

        val awarder1 = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)

        val meta = WildlifeDiscoveryMetadata("badge_01", "QUALIFYING_COMPLETION", "abc")
        val facts = AttemptQualification(lessonId = "l1", attemptId = "a1", requiredSequenceCompleted = true, isPreviewOrDemo = false, isParentTestMode = false, hasScoredInteraction = true, activityTypeIsNonScored = false)
        awarder1.evaluateAndAwardWildlifeBadge(childId, meta, facts)
        assertEquals("one badge", 1, collectedBadges.size)

        val savedBadges = collectedBadges.toList()
        coEvery { collectedBadgeDao.getAllByChild(childId) } returns savedBadges

        val awarder2 = BadgeAwarder(dailyChallengeDao, collectedBadgeDao, badgeLoader)
        val collected = awarder2.getCollectedBadges(childId)
        assertEquals("earned count 1 after restart", 1, collected.count { it.isCollected })
    }

    // ─── helpers ───

    private fun mockToday(dateString: String) {
        mockkStatic(LocalDate::class)
        val parsed = LocalDate.parse(dateString)
        every { LocalDate.now(any<ZoneId>()) } returns parsed
    }
}
