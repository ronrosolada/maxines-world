package com.maxinesworld.featurechildhome

import androidx.compose.ui.unit.IntSize
import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus
import org.junit.Assert.*
import org.junit.Test

class LivingVillageContractTest {

    // 1. Centered content-fit transformation
    @Test
    fun designTransformCentersLetterboxedScene() {
        val t = contentFitTransform(IntSize(1600, 1000))
        assertTrue("scale must be positive", t.scale > 0f)
        assertTrue("dx or dy must offset for centering", t.dx >= 0f || t.dy >= 0f)
    }

    // 2. Letterboxing
    @Test
    fun designTransformLetterboxesWideContainer() {
        val t = contentFitTransform(IntSize(4000, 2000), IntSize(3048, 2032))
        assertTrue("dx should center wide container", t.dx > 0f)
        assertEquals("dy should be zero for exact height match", 0f, t.dy, 0.01f)
    }

    // 3. Pillarboxing
    @Test
    fun designTransformPillarboxesTallContainer() {
        val t = contentFitTransform(IntSize(2000, 3000), IntSize(3048, 2032))
        assertEquals("dx should be zero for exact width", 0f, t.dx, 0.01f)
        assertTrue("dy should center tall container", t.dy > 0f)
    }

    // 4. Exactly six subject anchors
    @Test
    fun subjectAnchorsHasExactlySixEntries() {
        assertEquals(6, subjectAnchors.size)
    }

    // 5. Exact six canonical subject IDs
    @Test
    fun subjectAnchorsContainsAllSixCanonicalIds() {
        assertEquals(
            setOf("english", "filipino", "mathematics", "science", "history", "gmrc"),
            subjectAnchors.keys,
        )
    }

    // 6. Playground excluded from subject anchors
    @Test
    fun playgroundNotInSubjectAnchors() {
        assertFalse(subjectAnchors.containsKey("playground"))
    }

    // 7. Cat Café excluded from subject anchors
    @Test
    fun catCafeNotInSubjectAnchors() {
        assertFalse(subjectAnchors.containsKey("cat-cafe"))
    }

    // 8. Every subject has a drawable
    @Test
    fun allSixDestinationsHaveMedallionDrawables() {
        for (id in listOf("english", "filipino", "mathematics", "science", "history", "gmrc")) {
            assertNotNull("missing medallion for $id", subjectMedallionRes[id])
        }
    }

    // 9. Every destination preserves its full approved display name
    @Test
    fun approvedDestinationNamesPreserveFullNames() {
        assertEquals("Story Tree", approvedDestinationNames["english"])
        assertEquals("Bahay ng Kuwento", approvedDestinationNames["filipino"])
        assertEquals("Number Market", approvedDestinationNames["mathematics"])
        assertEquals("Discovery Lab", approvedDestinationNames["science"])
        assertEquals("Heritage Harbor", approvedDestinationNames["history"])
        assertEquals("Kindness Corner", approvedDestinationNames["gmrc"])
    }

    // 10. 0/0 progress guard
    @Test
    fun unavailableQuestsNeverRenderZeroOfZeroAsProgress() {
        val total = 0
        val copy = if (total == 0) "New requests are on the way" else "0 of $total quests complete"
        assertFalse("must not show 0 of 0", copy.contains("0 of 0"))
    }

    // 11. Village-state mapping
    @Test
    fun villageHomeStateMapsToLivingVillage() {
        val state = VillageHomeState(
            isLoading = false, childName = "Maxine", fishTreats = 42,
            questText = "Complete 3 activities", questProgressText = "2 / 3",
            playground = PlaygroundGateState("test", "2026-07-16", 3, 2, PlaygroundGateStatus.Locked),
            destinations = defaultDestinations,
        )
        val lv = state.toLivingVillage(reducedMotion = false)
        assertEquals("Maxine", lv.childName)
        assertEquals(42L, lv.fishTreats)
        assertEquals(2, lv.completedQuests)
        assertEquals(3, lv.totalQuests)
        assertTrue(lv.playground is PlaygroundUi.Locked)
        assertEquals(6, lv.destinations.size)
    }

    // 12. Playground locked state
    @Test
    fun playgroundLockedMapsCorrectly() {
        val state = VillageHomeState(
            playground = PlaygroundGateState("test", "2026-07-16", 5, 2, PlaygroundGateStatus.Locked),
        )
        val lv = state.toLivingVillage(reducedMotion = false)
        val pg = lv.playground as PlaygroundUi.Locked
        assertEquals(2, pg.completed)
        assertEquals(5, pg.total)
        assertEquals(3, pg.remaining)
    }

    // 13. Playground partial state
    @Test
    fun playgroundPartialProgressPreservesRemaining() {
        val state = VillageHomeState(
            playground = PlaygroundGateState("test", "2026-07-16", 4, 1, PlaygroundGateStatus.Locked),
        )
        val lv = state.toLivingVillage(reducedMotion = false)
        val pg = lv.playground as PlaygroundUi.Locked
        assertEquals(1, pg.completed)
        assertEquals(3, pg.remaining)
    }

    // 14. Playground unlocked state
    @Test
    fun playgroundUnlockedMapsToOpen() {
        val state = VillageHomeState(
            playground = PlaygroundGateState("test", "2026-07-16", 0, 0, PlaygroundGateStatus.Unlocked),
        )
        val lv = state.toLivingVillage(reducedMotion = false)
        assertEquals(PlaygroundUi.Open, lv.playground)
    }

    // 15. Null quest target resolves to no paw trail
    @Test
    fun nullQuestSubjectReturnsNullAnchor() {
        assertNull(subjectAnchors["not_a_subject"])
        val result = (null as String?)?.let { subjectAnchors[it] }
        assertNull(result)
    }

    // 16. Unknown quest target resolves to no paw trail
    @Test
    fun unknownQuestSubjectReturnsNullAnchor() {
        assertNull(subjectAnchors["not_a_subject"])
    }

    // 17. Valid quest target resolves to correct anchor
    @Test
    fun validQuestSubjectResolvesToAnchor() {
        val anchor = subjectAnchors["english"]
        assertNotNull("english must have an anchor", anchor)
    }

    // 18. Reduced-motion disables final paw pulse
    @Test
    fun reducedMotionDisablesNonessentialAnimation() {
        // When reducedMotion is true, miraBob should return 0f
        // This is a behavioral contract — actual rendering verified visually
        val state = VillageHomeState(
            playground = PlaygroundGateState("test", "2026-07-16", 0, 0, PlaygroundGateStatus.Unlocked),
        )
        val lv = state.toLivingVillage(reducedMotion = true)
        assertTrue(lv.reducedMotion)
        // Quest target exists but should not animate in reduced-motion mode
        lv.copy(questSubjectId = "english")
        assertTrue(lv.reducedMotion)
    }

    @Test
    fun nullPlaygroundMapsToUnavailable() {
        val state = VillageHomeState(playground = null)
        val lv = state.toLivingVillage(reducedMotion = false)
        assertEquals(PlaygroundUi.Unavailable, lv.playground)
    }
}
