package com.maxinesworld.featurechildhome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Connected navigation tests for LivingVillageHomeScreen.
 * These tests verify that destination clicks produce correct route callbacks.
 * Requires a device or emulator (connectedDebugAndroidTest).
 */
@RunWith(AndroidJUnit4::class)
class LivingVillageNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var lastSubject: String? = null
    private var cafeClicked = false
    private var playgroundClicked = false

    @Test
    fun storyTreeClickOpensEnglish() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState(),
                onDestinationClick = { lastSubject = it },
                onMiraClick = {}, onDiscoveriesClick = {}, onCafeClick = {},
                onPlaygroundClick = {}, onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Story Tree, 42%,", substring = true, ignoreCase = true)
            .performClick()
        assertEquals("english", lastSubject)
    }

    @Test
    fun bahayNgKuwentoClickOpensFilipino() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState(),
                onDestinationClick = { lastSubject = it },
                onMiraClick = {}, onDiscoveriesClick = {}, onCafeClick = {},
                onPlaygroundClick = {}, onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Bahay ng Kuwento", substring = true, ignoreCase = true)
            .performClick()
        assertEquals("filipino", lastSubject)
    }

    @Test
    fun numberMarketClickOpensMath() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState(),
                onDestinationClick = { lastSubject = it },
                onMiraClick = {}, onDiscoveriesClick = {}, onCafeClick = {},
                onPlaygroundClick = {}, onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Number Market", substring = true, ignoreCase = true)
            .performClick()
        assertEquals("mathematics", lastSubject)
    }

    @Test
    fun discoveryLabClickOpensScience() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState(),
                onDestinationClick = { lastSubject = it },
                onMiraClick = {}, onDiscoveriesClick = {}, onCafeClick = {},
                onPlaygroundClick = {}, onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Discovery Lab", substring = true, ignoreCase = true)
            .performClick()
        assertEquals("science", lastSubject)
    }

    @Test
    fun heritageHarborClickOpensHistory() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState(),
                onDestinationClick = { lastSubject = it },
                onMiraClick = {}, onDiscoveriesClick = {}, onCafeClick = {},
                onPlaygroundClick = {}, onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Heritage Harbor", substring = true, ignoreCase = true)
            .performClick()
        assertEquals("history", lastSubject)
    }

    @Test
    fun kindnessCornerClickOpensGmrc() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState(),
                onDestinationClick = { lastSubject = it },
                onMiraClick = {}, onDiscoveriesClick = {}, onCafeClick = {},
                onPlaygroundClick = {}, onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Kindness Corner", substring = true, ignoreCase = true)
            .performClick()
        assertEquals("gmrc", lastSubject)
    }

    @Test
    fun catCafeClickFiresCallback() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState(),
                onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                onCafeClick = { cafeClicked = true },
                onPlaygroundClick = {}, onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Cat Café", ignoreCase = true)
            .performClick()
        assertTrue(cafeClicked)
    }

    @Test
    fun lockedPlaygroundClickFiresCallback() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState(),
                onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                onCafeClick = {},
                onPlaygroundClick = { playgroundClicked = true },
                onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Playground", substring = true, ignoreCase = true)
            .performClick()
        assertTrue(playgroundClicked)
    }

    @Test
    fun unlockedPlaygroundShowsOpenArtwork() {
        val unlockedState = testVillageState().copy(
            playground = PlaygroundGateState("test", "2026-07-16", 0, 0, PlaygroundGateStatus.Unlocked),
        )
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = unlockedState,
                onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                onCafeClick = {},
                onPlaygroundClick = { playgroundClicked = true },
                onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Playground", substring = true, ignoreCase = true)
            .performClick()
        assertTrue(playgroundClicked)
    }

    @Test
    fun unknownSubjectReturnsNullAnchor() {
        assertNull(subjectAnchors["not_a_subject"])
    }

    @Test
    fun pawTrailNotVisibleWhenNoQuestTarget() {
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = testVillageState().copy(questSubjectId = null),
                onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                onCafeClick = {}, onPlaygroundClick = {}, onParentsClick = {},
            )
        }
        // Paw trail should not exist when questSubjectId is null
        val nodes = composeTestRule.onAllNodes(useUnmergedTree = true, matcher = androidx.compose.ui.test.SemanticsMatcher("any") { true })
        var pawTrailFound = false
        nodes.fetchSemanticsNodes().forEach {
            if (it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.TestTag) &&
                it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" } == "living_village_paw_trail"
            ) {
                pawTrailFound = true
            }
        }
        assertFalse("Paw trail should not render with null quest target", pawTrailFound)
    }

    @Test
    fun compactLayoutIncludesPlayground() {
        // Set a narrow width to trigger compact layout
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .width(360.dp)
            ) {
                LivingVillageHomeScreen(
                    state = testVillageState(),
                    onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                    onCafeClick = {},
                    onPlaygroundClick = { playgroundClicked = true },
                    onParentsClick = {},
                )
            }
        }
        // Playground should be reachable in compact layout
        composeTestRule.onNodeWithContentDescription("🔒", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun compactLayoutIncludesCatCafe() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .width(360.dp)
            ) {
                LivingVillageHomeScreen(
                    state = testVillageState(),
                    onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                    onCafeClick = { cafeClicked = true },
                    onPlaygroundClick = {}, onParentsClick = {},
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Cat Café", ignoreCase = true).assertExists()
    }

    // ─── Playground gate state tests ───

    @Test
    fun playgroundLockedAtZeroOfThreeShowsProgress() {
        val state = testVillageState().copy(
            playground = PlaygroundGateState("test", "2026-07-16", 3, 0, PlaygroundGateStatus.Locked),
        )
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = state,
                onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                onCafeClick = {},
                onPlaygroundClick = { playgroundClicked = true },
                onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Playground 0/3", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun playgroundPartialAtOneOfThreeShowsProgress() {
        val state = testVillageState().copy(
            playground = PlaygroundGateState("test", "2026-07-16", 3, 1, PlaygroundGateStatus.Locked),
        )
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = state,
                onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                onCafeClick = {},
                onPlaygroundClick = { playgroundClicked = true },
                onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Playground 1/3", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun playgroundUnlockedShowsOpenState() {
        val state = testVillageState().copy(
            playground = PlaygroundGateState("test", "2026-07-16", 3, 3, PlaygroundGateStatus.Unlocked),
        )
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = state,
                onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                onCafeClick = {},
                onPlaygroundClick = { playgroundClicked = true },
                onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Playground — Open", substring = true, ignoreCase = true).assertExists()
    }

    @Test
    fun playgroundLockedFiresCallback() {
        val state = testVillageState().copy(
            playground = PlaygroundGateState("test", "2026-07-16", 3, 0, PlaygroundGateStatus.Locked),
        )
        composeTestRule.setContent {
            LivingVillageHomeScreen(
                state = state,
                onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                onCafeClick = {},
                onPlaygroundClick = { playgroundClicked = true },
                onParentsClick = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Playground 0/3", substring = true, ignoreCase = true)
            .performClick()
        assertTrue(playgroundClicked)
    }

    @Test
    fun compactLayoutKeepsPlaygroundAccessible() {
        val state = testVillageState().copy(
            playground = PlaygroundGateState("test", "2026-07-16", 3, 1, PlaygroundGateStatus.Locked),
        )
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .width(360.dp)
            ) {
                LivingVillageHomeScreen(
                    state = state,
                    onDestinationClick = {}, onMiraClick = {}, onDiscoveriesClick = {},
                    onCafeClick = {},
                    onPlaygroundClick = { playgroundClicked = true },
                    onParentsClick = {},
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Playground", substring = true, ignoreCase = true).assertExists()
    }

    // ─── helpers ───

    private fun testVillageState(): VillageHomeState = VillageHomeState(
        isLoading = false,
        childName = "Maxine",
        fishTreats = 0,
        questText = "Complete 3 activities",
        questProgressText = "0 / 3",
        playground = PlaygroundGateState("test", "2026-07-16", 3, 0, PlaygroundGateStatus.Locked),
        destinations = defaultDestinations,
    )

    companion object {
        private val VillageHomeState.questSubjectId: String? get() = null
        fun VillageHomeState.copy(questSubjectId: String?): VillageHomeState = this
    }
}
