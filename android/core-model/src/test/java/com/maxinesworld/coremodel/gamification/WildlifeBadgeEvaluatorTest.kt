package com.maxinesworld.coremodel.gamification

import com.maxinesworld.coremodel.CollectibleBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WildlifeBadgeEvaluatorTest {

    private val sampleCatalog = listOf(
        CollectibleBadge(
            id = "badge-tamaraw",
            biome = "forest_friends",
            name = "Tamaraw",
            title = "Forest Guardian",
            funFact = "The tamaraw is found only on Mindoro.",
            emoji = "🦬",
        ),
        CollectibleBadge(
            id = "badge-eagle",
            biome = "sky_scouts",
            name = "Philippine Eagle",
            title = "Sky King",
            funFact = "One of the largest eagles in the world.",
            emoji = "🦅",
        ),
        CollectibleBadge(
            id = "badge-tarsier",
            biome = "forest_friends",
            name = "Tarsier",
            title = "Night Watcher",
            funFact = "Tarsiers can rotate their heads 180 degrees.",
            emoji = "🐒",
        ),
    )

    private fun qualifyingAttempt(
        lessonId: String = "lesson-1",
        attemptId: String = "att-1",
    ) = AttemptQualification(
        lessonId = lessonId,
        attemptId = attemptId,
        requiredSequenceCompleted = true,
        isPreviewOrDemo = false,
        isParentTestMode = false,
        hasScoredInteraction = true,
        activityTypeIsNonScored = false,
    )

    // ─── 1. No Mapping ───────────────────────────────────────────────

    @Test
    fun `no mapping when metadata is null`() {
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = null,
            attemptFacts = qualifyingAttempt(),
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertEquals(BadgeEvaluation.NoMapping, result)
    }

    // ─── 2. Invalid Badge ID ─────────────────────────────────────────

    @Test
    fun `invalid mapping when badgeId not in catalog`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-unicorn")
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = qualifyingAttempt(),
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertTrue("Expected InvalidMapping", result is BadgeEvaluation.InvalidMapping)
        val invalid = result as BadgeEvaluation.InvalidMapping
        assertEquals("badge-unicorn", invalid.badgeId)
    }

    // ─── 3. Already Collected ────────────────────────────────────────

    @Test
    fun `already collected when badge in collected set`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tamaraw")
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = qualifyingAttempt(),
            alreadyCollectedBadgeIds = setOf("badge-tamaraw", "badge-eagle"),
        )
        assertTrue("Expected AlreadyCollected", result is BadgeEvaluation.AlreadyCollected)
        val already = result as BadgeEvaluation.AlreadyCollected
        assertEquals("badge-tamaraw", already.badgeId)
    }

    // ─── 4. Qualifying Completion → Award ────────────────────────────

    @Test
    fun `award when qualifying completion`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-eagle")
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = qualifyingAttempt(lessonId = "lesson-eagle-1"),
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertTrue("Expected Award", result is BadgeEvaluation.Award)
        val award = result as BadgeEvaluation.Award
        assertEquals("badge-eagle", award.badgeId)
        assertEquals("lesson-eagle-1", award.sourceLessonId)
    }

    // ─── 5. Preview Mode → No Award ──────────────────────────────────

    @Test
    fun `no award when preview mode`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tamaraw")
        val attempt = qualifyingAttempt().copy(isPreviewOrDemo = true)
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = attempt,
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertEquals(BadgeEvaluation.NoMapping, result)
    }

    // ─── 6. Parent Test Mode → No Award ──────────────────────────────

    @Test
    fun `no award when parent test mode`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tamaraw")
        val attempt = qualifyingAttempt().copy(isParentTestMode = true)
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = attempt,
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertEquals(BadgeEvaluation.NoMapping, result)
    }

    // ─── 7. Missing Scored Interaction + Non-Scored Type → No Award ──

    @Test
    fun `no award when no scored interaction and activity not non-scored`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tarsier")
        val attempt = qualifyingAttempt().copy(
            hasScoredInteraction = false,
            activityTypeIsNonScored = false,
        )
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = attempt,
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertEquals(BadgeEvaluation.NoMapping, result)
    }

    // ─── 7b. Non-Scored Activity Still Qualifies ─────────────────────

    @Test
    fun `award when non-scored activity type even without scored interaction`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tarsier")
        val attempt = qualifyingAttempt().copy(
            hasScoredInteraction = false,
            activityTypeIsNonScored = true,
        )
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = attempt,
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertTrue("Expected Award for non-scored activity", result is BadgeEvaluation.Award)
    }

    // ─── 8. Two Children Earn Same Badge Independently ───────────────

    @Test
    fun `two children earn same badge independently`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tamaraw")

        // Child A earns it
        val resultA = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = qualifyingAttempt(lessonId = "lesson-a", attemptId = "att-a"),
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertTrue("Child A should earn badge", resultA is BadgeEvaluation.Award)

        // Child B also earns it — evaluator is stateless, no shared ownership
        val resultB = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = qualifyingAttempt(lessonId = "lesson-b", attemptId = "att-b"),
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertTrue("Child B should also earn badge", resultB is BadgeEvaluation.Award)
        assertEquals("badge-tamaraw", (resultB as BadgeEvaluation.Award).badgeId)
    }

    // ─── Bonus: Edge Cases for Qualifying Logic ──────────────────────

    @Test
    fun `not qualifying when attemptId is blank`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tamaraw")
        val attempt = qualifyingAttempt().copy(attemptId = "")
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = attempt,
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertEquals(BadgeEvaluation.NoMapping, result)
    }

    @Test
    fun `not qualifying when lessonId is blank`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tamaraw")
        val attempt = qualifyingAttempt().copy(lessonId = "")
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = attempt,
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertEquals(BadgeEvaluation.NoMapping, result)
    }

    @Test
    fun `not qualifying when required sequence not completed`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tamaraw")
        val attempt = qualifyingAttempt().copy(requiredSequenceCompleted = false)
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = attempt,
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertEquals(BadgeEvaluation.NoMapping, result)
    }

    @Test
    fun `already collected takes priority over qualifying attempt`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-tamaraw")
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = qualifyingAttempt(),
            alreadyCollectedBadgeIds = setOf("badge-tamaraw"),
        )
        assertTrue("AlreadyCollected should take priority", result is BadgeEvaluation.AlreadyCollected)
    }

    @Test
    fun `invalid mapping takes priority over attempt qualification`() {
        val metadata = WildlifeDiscoveryMetadata(badgeId = "badge-fake")
        val result = WildlifeBadgeEvaluator.evaluate(
            catalog = sampleCatalog,
            lessonMetadata = metadata,
            attemptFacts = qualifyingAttempt(),
            alreadyCollectedBadgeIds = emptySet(),
        )
        assertTrue("InvalidMapping should be checked before qualification", result is BadgeEvaluation.InvalidMapping)
    }
}
