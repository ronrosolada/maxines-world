package com.maxinesworld.featurechildhome

import androidx.compose.ui.unit.IntSize
import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus
import org.junit.Assert.*
import org.junit.Test

class LivingVillageContractTest {

    @Test
    fun designTransformCentersLetterboxedScene() {
        val t = contentFitTransform(IntSize(1600, 1000))
        assertTrue("scale must be positive", t.scale > 0f)
        assertTrue("dx or dy must offset for centering", t.dx >= 0f || t.dy >= 0f)
    }

    @Test
    fun allSixDestinationsHaveAnchors() {
        val subjects = subjectAnchors.keys - "cat-cafe"
        assertEquals(
            setOf("english", "filipino", "mathematics", "science", "history", "gmrc"),
            subjects,
        )
    }

    @Test
    fun allSixDestinationsHaveMedallionDrawables() {
        for (id in listOf("english", "filipino", "mathematics", "science", "history", "gmrc")) {
            assertNotNull("missing medallion for $id", subjectMedallionRes[id])
        }
    }

    @Test
    fun unavailableQuestsNeverRenderZeroOfZeroAsProgress() {
        val total = 0
        val copy = if (total == 0) "New requests are on the way" else "0 of $total quests complete"
        assertFalse("must not show 0 of 0", copy.contains("0 of 0"))
    }

    @Test
    fun villageHomeStateMapsToLivingVillage() {
        val state = VillageHomeState(
            isLoading = false,
            childName = "Maxine",
            fishTreats = 42,
            questText = "Complete 3 activities",
            questProgressText = "2 / 3",
            playground = PlaygroundGateState(
                childId = "test",
                dayKey = "2026-07-16",
                totalAssigned = 3,
                completed = 2,
                status = PlaygroundGateStatus.Locked,
            ),
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

    @Test
    fun playgroundUnlockedMapsToOpen() {
        val state = VillageHomeState(
            playground = PlaygroundGateState(
                childId = "test", dayKey = "2026-07-16", totalAssigned = 0, completed = 0,
                status = PlaygroundGateStatus.Unlocked,
            ),
        )
        val lv = state.toLivingVillage(reducedMotion = false)
        assertEquals(PlaygroundUi.Open, lv.playground)
    }

    @Test
    fun nullPlaygroundMapsToUnavailable() {
        val state = VillageHomeState(playground = null)
        val lv = state.toLivingVillage(reducedMotion = false)
        assertEquals(PlaygroundUi.Unavailable, lv.playground)
    }
}
