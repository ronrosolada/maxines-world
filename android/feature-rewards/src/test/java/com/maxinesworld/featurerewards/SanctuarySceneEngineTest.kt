package com.maxinesworld.featurerewards

import org.junit.Assert.*
import org.junit.Test

class SanctuarySceneEngineTest {
    @Test fun catalogContainsExpandedIconicRosterWithBilingualFacts() {
        assertEquals(51, SanctuarySpeciesCatalog.species.size)
        val expected = mapOf(
            "badge_mammal_tarsier" to "sanctuary-meadow", "badge_mammal_tamaraw" to "sanctuary-meadow",
            "badge_bird_eagle" to "sanctuary-lookout", "badge_bird_peacock_pheasant" to "sanctuary-lookout",
            "badge_reptile_philippine_crocodile" to "sanctuary-wildlife-sign", "badge_fish_sinarapan" to "sanctuary-pond",
        )
        expected.forEach { (badge, habitat) -> assertEquals(habitat, SanctuarySpeciesCatalog.requireByBadgeId(badge).primaryHabitatId) }
        assertTrue(SanctuarySpeciesCatalog.species.all { it.factEnglish.isNotBlank() && it.factFilipino.isNotBlank() && it.signatureBehavior.isNotBlank() })
        val wildlife = SanctuarySpeciesCatalog.species.filterNot { it.badgeId == "badge_milestone_first_steps" }
        assertEquals(wildlife.size, wildlife.map { it.animalAssetId }.distinct().size)
    }

    @Test fun residentRequiresBothBadgeAndCompatibleUnlockedHabitat() {
        val engine = SanctuarySceneEngine()
        assertTrue(engine.build(setOf("sanctuary-lookout"), emptySet(), 7).residents.isEmpty())
        assertTrue(engine.build(emptySet(), setOf("badge_bird_eagle"), 7).residents.isEmpty())
        assertEquals("badge_bird_eagle", engine.build(setOf("sanctuary-lookout"), setOf("badge_bird_eagle"), 7).residents.single().species.badgeId)
    }

    @Test fun sameSeedProducesSameOrderingAndValidDistinctAnchors() {
        val habitats = SanctuarySpeciesCatalog.species.map { it.primaryHabitatId }.toSet()
        val badges = SanctuarySpeciesCatalog.species.map { it.badgeId }.toSet()
        val scene = SanctuarySceneEngine().build(habitats, badges, 20260830)
        assertEquals(scene, SanctuarySceneEngine().build(habitats, badges, 20260830))
        assertEquals(SanctuarySpeciesCatalog.species.size, scene.residents.size)
        assertTrue(scene.residents.all { it.anchor.x in 0f..1f && it.anchor.y in 0f..1f && it.anchor.scale > 0f && it.anchor.layerDepth in 0f..1f })
        assertEquals(scene.residents.size, scene.residents.map { it.anchor.x to it.anchor.y }.distinct().size)
    }

    @Test fun dayNightEligibilityWakesNocturnalAndRestsDiurnalSpecies() {
        assertEquals(SanctuaryTimePeriod.MORNING_DAY, SanctuaryCareEngine.timePeriod(6))
        assertEquals(SanctuaryTimePeriod.DUSK, SanctuaryCareEngine.timePeriod(17))
        assertEquals(SanctuaryTimePeriod.NIGHT, SanctuaryCareEngine.timePeriod(19))
        assertTrue(SanctuaryCareEngine.isAwake(WildlifeActivityPeriod.NOCTURNAL, SanctuaryTimePeriod.NIGHT))
        assertFalse(SanctuaryCareEngine.isAwake(WildlifeActivityPeriod.DIURNAL, SanctuaryTimePeriod.NIGHT))
        assertTrue(SanctuaryCareEngine.isAwake(WildlifeActivityPeriod.DIURNAL, SanctuaryTimePeriod.MORNING_DAY))
    }

    @Test fun feedingAcceptsPreferredTreatAndReturnsBilingualObservation() {
        val fox = SanctuarySpeciesCatalog.requireByBadgeId("badge_mammal_flying_fox")
        assertEquals(SanctuaryTreat.SWEET_NECTAR, SanctuaryCareEngine.preferredTreat(fox))
        val happy = SanctuaryCareEngine.feed(fox, SanctuaryTreat.SWEET_NECTAR)
        assertTrue(happy.accepted); assertTrue(happy.messageEnglish.contains(fox.nameEnglish)); assertTrue(happy.messageFilipino.isNotBlank())
        assertFalse(SanctuaryCareEngine.feed(fox, SanctuaryTreat.RIVER_MINNOW).accepted)
    }
}

class WildlifeHabitatAffinityTest {
    @Test fun badgeAndAssetIdsResolveToSameSpecies() {
        SanctuarySpeciesCatalog.species.filterNot { it.badgeId == "badge_milestone_first_steps" }
            .forEach { assertEquals(it, SanctuarySpeciesCatalog.byAnimalAssetId(it.animalAssetId)) }
    }
    @Test fun alternateHabitatAlsoMakesResidentEligible() {
        val scene = SanctuarySceneEngine().build(setOf("sanctuary-nest"), setOf("badge_bird_eagle"), 9)
        assertEquals(1, scene.residents.size)
    }
}
