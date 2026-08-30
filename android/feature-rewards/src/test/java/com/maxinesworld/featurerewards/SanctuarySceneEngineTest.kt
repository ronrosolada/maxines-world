package com.maxinesworld.featurerewards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SanctuarySceneEngineTest {
    @Test fun catalogMapsFourSignatureSpeciesToRequiredHabitats() {
        val expected = mapOf(
            "badge_philippine_eagle" to "sanctuary-lookout",
            "badge_philippine_tarsier" to "sanctuary-tree",
            "badge_tamaraw" to "sanctuary-meadow",
            "badge_sinarapan" to "sanctuary-pond",
        )
        assertEquals(expected, SanctuarySpeciesCatalog.species.associate { it.badgeId to it.primaryHabitatId })
        assertTrue(SanctuarySpeciesCatalog.species.all { it.factEnglish.isNotBlank() && it.factFilipino.isNotBlank() })
    }

    @Test fun residentRequiresBothBadgeAndCompatibleUnlockedHabitat() {
        val engine = SanctuarySceneEngine()
        assertTrue(engine.build(setOf("sanctuary-lookout"), emptySet(), 7).residents.isEmpty())
        assertTrue(engine.build(emptySet(), setOf("badge_philippine_eagle"), 7).residents.isEmpty())
        assertEquals("badge_philippine_eagle", engine.build(setOf("sanctuary-lookout"), setOf("badge_philippine_eagle"), 7).residents.single().species.badgeId)
    }

    @Test fun sameSeedProducesSameOrderingAndAnchors() {
        val habitats = SanctuarySpeciesCatalog.species.map { it.primaryHabitatId }.toSet()
        val badges = SanctuarySpeciesCatalog.species.map { it.badgeId }.toSet()
        assertEquals(engine().build(habitats, badges, 20260830), engine().build(habitats, badges, 20260830))
    }

    @Test fun anchorsAreNormalizedAndLayered() {
        val scene = engine().build(
            SanctuarySpeciesCatalog.species.map { it.primaryHabitatId }.toSet(),
            SanctuarySpeciesCatalog.species.map { it.badgeId }.toSet(), 1,
        )
        assertTrue(scene.residents.all { it.anchor.x in 0f..1f && it.anchor.y in 0f..1f })
        assertTrue(scene.residents.all { it.anchor.scale > 0f && it.anchor.zIndex >= 0f && it.anchor.layerDepth in 0f..1f })
    }

    private fun engine() = SanctuarySceneEngine()
}

class WildlifeHabitatAffinityTest {
    @Test fun badgeAndAssetIdsResolveToSameSpecies() {
        val eagle = SanctuarySpeciesCatalog.requireByBadgeId("badge_philippine_eagle")
        assertEquals(eagle, SanctuarySpeciesCatalog.byAnimalAssetId("bird_philippine_eagle"))
    }

    @Test fun alternateHabitatAlsoMakesResidentEligible() {
        val scene = SanctuarySceneEngine().build(
            unlockedHabitats = setOf("habitat-tall-trees"),
            earnedBadges = setOf("badge_philippine_eagle"),
            dateSeed = 9,
        )
        assertEquals(1, scene.residents.size)
    }
}
