package com.maxinesworld.featurerewards

import com.maxinesworld.coremodel.BadgeBiome
import com.maxinesworld.coremodel.CollectibleBadge
import org.junit.Assert.assertEquals
import org.junit.Test

class WildlifeFieldGuideOrderTest {
    @Test
    fun `collected milestone biome is shown before empty wildlife biomes`() {
        val badges = listOf(
            CollectibleBadge("first", "milestone", "First Steps", "Bright Beginning", "", isCollected = true),
            CollectibleBadge("forest", "forest_friends", "Tarsier", "Night Eyes", ""),
        )

        assertEquals(BadgeBiome.MILESTONE, orderBiomesWithCollectedFirst(badges).first())
    }

    @Test
    fun `empty guide preserves canonical biome order`() {
        assertEquals(BadgeBiome.entries, orderBiomesWithCollectedFirst(emptyList()))
    }
}
