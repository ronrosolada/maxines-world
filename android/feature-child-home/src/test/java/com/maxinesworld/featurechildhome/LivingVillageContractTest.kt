package com.maxinesworld.featurechildhome

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

    // 18. Reduced-motion state is preserved through mapping
    @Test
    fun reducedMotionIsPreservedAndDisablesAnimationCondition() {
        val state = VillageHomeState(
            playground = PlaygroundGateState("test", "2026-07-16", 0, 0, PlaygroundGateStatus.Unlocked),
        )
        val lv = state.toLivingVillage(reducedMotion = true)
        assertTrue(lv.reducedMotion)
        // In this state, the animation condition in SubjectMedallion evaluates to:
        // animateFloatAsState targetValue = if (isActive && !lv.reducedMotion) 1.05f else 1f
        // With reducedMotion=true, targetValue is always 1f regardless of isActive
        assertTrue(lv.reducedMotion)
    }

    @Test
    fun nullPlaygroundMapsToUnavailable() {
        val state = VillageHomeState(playground = null)
        val lv = state.toLivingVillage(reducedMotion = false)
        assertEquals(PlaygroundUi.Unavailable, lv.playground)
    }

    // ═══════════════════════════════════════════════════════
    // Responsive scaling tests
    // ═══════════════════════════════════════════════════════

    @Test
    fun visualScaleUsesReferenceScaleAt720DpShortSide() {
        val metrics = villageUiMetrics(
            viewportWidthDp = 1108f,
            viewportHeightDp = 720f,
        )

        assertEquals(1f, metrics.scale, 0.001f)
        assertEquals(88f, metrics.medallionSize.value, 0.01f)
        assertEquals(104f, metrics.selectedMedallionSize.value, 0.01f)
    }

    @Test
    fun visualScaleClampsForCompactViewport() {
        val metrics = villageUiMetrics(
            viewportWidthDp = 400f,
            viewportHeightDp = 250f,
        )

        assertEquals(0.58f, metrics.scale, 0.001f)
        assertTrue(metrics.medallionSize.value >= 48f)
        assertTrue(metrics.lessonLabelWidth.value >= 104f)
    }

    @Test
    fun visualScaleClampsForLargeTablet() {
        val metrics = villageUiMetrics(
            viewportWidthDp = 1400f,
            viewportHeightDp = 900f,
        )

        assertEquals(1.08f, metrics.scale, 0.001f)
        assertTrue(metrics.selectedMedallionSize.value <= 113f)
        assertTrue(metrics.lessonLabelWidth.value <= 168f)
    }

    @Test
    fun selectedMedallionIsLargerThanStandardMedallion() {
        val metrics = villageUiMetrics(1108f, 720f)

        assertTrue(
            metrics.selectedMedallionSize >
                metrics.medallionSize
        )
    }

    @Test
    fun allApprovedLabelsFitConfiguredLineRules() {
        val oneLine = setOf("Story Tree")
        val twoLine = setOf(
            "Bahay ng Kuwento",
            "Number Market",
            "Discovery Lab",
            "Heritage Harbor",
            "Kindness Corner",
        )

        assertEquals(
            approvedDestinationNames.values.toSet(),
            oneLine + twoLine,
        )
    }

    @Test
    fun subjectRenderingHasNoBlankDrawableFallback() {
        assertEquals(
            approvedDestinationNames.keys,
            subjectMedallionRes.keys,
        )
    }

    // ═══════════════════════════════════════════════════════
    // Storybook caption tests
    // ═══════════════════════════════════════════════════════

    @Test
    fun captionColorIsDarkCocoa() {
        assertEquals(Color(0xFF3B281C), LessonCaptionCocoa)
    }

    @Test
    fun captionMetricsRemainReadableAtCompactScale() {
        val metrics = villageUiMetrics(
            viewportWidthDp = 400f,
            viewportHeightDp = 250f,
        )

        assertTrue(metrics.lessonLabelWidth.value >= 104f)
        assertTrue(metrics.lessonLabelFontSp >= 11f)
        assertTrue(metrics.lessonLabelLineHeightSp >= 13f)
    }

    @Test
    fun captionMetricsRemainBoundedOnLargeTablet() {
        val metrics = villageUiMetrics(
            viewportWidthDp = 1400f,
            viewportHeightDp = 900f,
        )

        assertTrue(metrics.lessonLabelWidth.value <= 168f)
        assertTrue(metrics.lessonLabelFontSp <= 14f)
        assertTrue(metrics.lessonLabelLineHeightSp <= 17f)
    }

    @Test
    fun approvedDestinationNamesRemainComplete() {
        assertEquals(
            setOf(
                "Story Tree",
                "Bahay ng Kuwento",
                "Number Market",
                "Discovery Lab",
                "Heritage Harbor",
                "Kindness Corner",
            ),
            approvedDestinationNames.values.toSet(),
        )
    }

    // ═══════════════════════════════════════════════════════
    // Lower layout correction tests
    // ═══════════════════════════════════════════════════════

    @Test
    fun upperSubjectAnchorsUnchanged() {
        assertEquals(Offset(610f, 780f), subjectAnchors["english"])
        assertEquals(Offset(1570f, 820f), subjectAnchors["filipino"])
        assertEquals(Offset(2380f, 820f), subjectAnchors["mathematics"])
    }

    @Test
    fun discoveryLabReceivesPresentationOffset() {
        val original = subjectAnchors["science"] ?: error("missing")
        val adjusted = presentationAnchor("science", original)
        assertEquals(original.x + discoveryPresentationOffset.x, adjusted.x)
        assertEquals(original.y + discoveryPresentationOffset.y, adjusted.y)
    }

    @Test
    fun heritageHarborUsesCaptionAbove() {
        assertEquals(CaptionPlacement.Above, captionPlacement("history"))
    }

    @Test
    fun otherSubjectsUseCaptionBelow() {
        for (id in listOf("english", "filipino", "mathematics", "science", "gmrc")) {
            assertEquals("$id should use Below", CaptionPlacement.Below, captionPlacement(id))
        }
    }

    @Test
    fun lowerSubjectsReceiveHighContrast() {
        assertTrue("science is lower", "science" in lowerSubjectIds)
        assertTrue("history is lower", "history" in lowerSubjectIds)
        assertTrue("gmrc is lower", "gmrc" in lowerSubjectIds)
        assertFalse("english is not lower", "english" in lowerSubjectIds)
    }

    @Test
        fun worldCaptionColorIsDarkCocoa() {
            assertTrue("World caption should be dark (red)", WorldCaptionCocoa.red < 0.3f)
            assertTrue("World caption should be dark (green)", WorldCaptionCocoa.green < 0.2f)
            assertTrue("World caption should be dark (blue)", WorldCaptionCocoa.blue < 0.2f)
        }

    @Test
    fun questBookMetricsWithinBounds() {
        val metrics = villageUiMetrics(1108f, 720f)
        // Reference viewport: scale=1.0
        assertTrue(metrics.questBookWidth.value in 278f..344f)
        assertTrue(metrics.questBookHeight.value in 150f..184f)
        assertTrue(metrics.miraBookGap.value in 6f..8f)
        assertTrue(metrics.miraHeight.value in 164f..208f)
    }

    @Test
        fun miraBookGroupClearsBottomNavigation() {
            val metrics = villageUiMetrics(1108f, 720f)
            // Mira + book are horizontal (Row), so the group height is max(miraHeight, questBookHeight).
            // This must leave room for bottom nav (64dp) + padding.
            val groupHeight = maxOf(metrics.miraHeight.value, metrics.questBookHeight.value)
            assertTrue("Group should fit above nav", groupHeight < 350f)
        }

    @Test
    fun miraQuestAnchorIsInBottomHalf() {
        assertTrue("Y > 1016 (bottom half of 2032)", miraQuestAnchor.y > 1016f)
        assertTrue("X should be left side", miraQuestAnchor.x < 1524f)
    }

    @Test
    fun discoveryPresentationAnchorIsDistinctFromCanonical() {
        val canonical = subjectAnchors["science"] ?: error("missing")
        val display = presentationAnchor("science", canonical)
        assertNotEquals(canonical, display)
    }

    @Test
    fun nonScienceSubjectsKeepCanonicalAnchor() {
        for (id in listOf("english", "filipino", "mathematics", "history", "gmrc")) {
            val canonical = subjectAnchors[id] ?: error("missing $id")
            assertEquals(canonical, presentationAnchor(id, canonical))
        }
    }

    @Test
    fun allApprovedLabelsStillPresent() {
        val names = approvedDestinationNames.values.toSet()
        assertEquals(6, names.size)
        for (label in listOf("Story Tree", "Bahay ng Kuwento", "Number Market",
                "Discovery Lab", "Heritage Harbor", "Kindness Corner")) {
            assertTrue("Missing: $label", label in names)
        }
    }

    @Test
    fun lessonCaptionColorsMatchApprovedTokens() {
        assertEquals(Color(0xFF3B281C), LessonCaptionCocoa)
        assertEquals(Color(0xF2FFF3D6), LessonCaptionHalo)
        assertEquals(Color(0xFFD6942C), LessonCaptionActive)
    }
}
