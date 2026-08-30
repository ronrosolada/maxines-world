package com.maxinesworld.featurerewards

/** Normalized placement independent of display size or Compose. */
data class HabitatAnchor(val x: Float, val y: Float, val scale: Float, val zIndex: Float, val layerDepth: Float)
data class SanctuaryResident(val species: WildlifeHabitatAffinity, val habitatId: String, val anchor: HabitatAnchor)
data class SanctuaryScene(val residents: List<SanctuaryResident>)

enum class SanctuaryTimePeriod { MORNING_DAY, DUSK, NIGHT }
enum class SanctuaryTreat(val labelEnglish: String, val labelFilipino: String) {
    WILD_FIGS("Wild Figs", "Prutas"), SWEET_NECTAR("Sweet Nectar", "Nektar"),
    RIVER_MINNOW("River Minnow", "Isda"), FRESH_LEAVES("Fresh Leaves", "Dahon")
}
data class FeedingResult(val accepted: Boolean, val messageEnglish: String, val messageFilipino: String)

object SanctuaryCareEngine {
    fun timePeriod(hour: Int): SanctuaryTimePeriod {
        require(hour in 0..23)
        return when (hour) { in 6..16 -> SanctuaryTimePeriod.MORNING_DAY; in 17..18 -> SanctuaryTimePeriod.DUSK; else -> SanctuaryTimePeriod.NIGHT }
    }

    fun isAwake(period: WildlifeActivityPeriod, time: SanctuaryTimePeriod): Boolean = when (period) {
        WildlifeActivityPeriod.NOCTURNAL -> time == SanctuaryTimePeriod.NIGHT || time == SanctuaryTimePeriod.DUSK
        WildlifeActivityPeriod.DIURNAL -> time != SanctuaryTimePeriod.NIGHT
        WildlifeActivityPeriod.CREPUSCULAR -> time != SanctuaryTimePeriod.MORNING_DAY
    }

    fun preferredTreat(species: WildlifeHabitatAffinity): SanctuaryTreat = when {
        species.animalAssetId.contains("flowerpecker") || species.animalAssetId.contains("flying_fox") -> SanctuaryTreat.SWEET_NECTAR
        species.animalAssetId.contains("crocodile") || species.animalAssetId.contains("fish") || species.animalAssetId.contains("eagle") -> SanctuaryTreat.RIVER_MINNOW
        species.animalAssetId.contains("deer") || species.animalAssetId.contains("tamaraw") || species.animalAssetId.contains("pangolin") -> SanctuaryTreat.FRESH_LEAVES
        else -> SanctuaryTreat.WILD_FIGS
    }

    fun feed(species: WildlifeHabitatAffinity, treat: SanctuaryTreat): FeedingResult {
        val accepted = treat == preferredTreat(species)
        return if (accepted) FeedingResult(true, "Yum! The ${species.nameEnglish} loved the treat!", "Sarap! Nagustuhan ng ${species.nameFilipino} ang pagkain!")
        else FeedingResult(false, "Let's try a treat that matches its natural diet.", "Subukan natin ang pagkaing tugma sa likas nitong kinakain.")
    }
}

/** Pure deterministic eligibility and placement algorithm. */
class SanctuarySceneEngine {
    fun build(unlockedHabitats: Set<String>, earnedBadges: Set<String>, dateSeed: Int): SanctuaryScene {
        val eligible = SanctuarySpeciesCatalog.species.filter { it.badgeId in earnedBadges && (it.primaryHabitatId in unlockedHabitats || it.alternateHabitatId in unlockedHabitats) }
        val ordered = eligible.sortedWith(compareBy<WildlifeHabitatAffinity> { stableHash(it.badgeId, dateSeed) }.thenBy { it.badgeId })
        return SanctuaryScene(ordered.mapIndexed { index, species ->
            val habitat = if (species.primaryHabitatId in unlockedHabitats) species.primaryHabitatId else species.alternateHabitatId
            SanctuaryResident(species, habitat, anchorFor(species, index, dateSeed))
        })
    }

    private fun anchorFor(species: WildlifeHabitatAffinity, index: Int, seed: Int): HabitatAnchor {
        val base = when (species.badgeId) {
            "badge_bird_eagle" -> HabitatAnchor(.76f, .24f, .20f, 2f, .18f)
            "badge_mammal_tarsier" -> HabitatAnchor(.61f, .42f, .17f, 3f, .38f)
            "badge_mammal_tamaraw" -> HabitatAnchor(.48f, .69f, .25f, 5f, .72f)
            "badge_fish_sinarapan" -> HabitatAnchor(.23f, .76f, .14f, 4f, .78f)
            "badge_bird_peacock_pheasant" -> HabitatAnchor(.34f, .72f, .18f, 5f, .74f)
            "badge_bird_cebu_flowerpecker" -> HabitatAnchor(.43f, .29f, .12f, 2f, .24f)
            "badge_mammal_spotted_deer" -> HabitatAnchor(.68f, .66f, .24f, 5f, .69f)
            "badge_mammal_pangolin" -> HabitatAnchor(.54f, .73f, .16f, 5f, .76f)
            "badge_mammal_flying_fox" -> HabitatAnchor(.83f, .31f, .17f, 2f, .27f)
            else -> {
                val hash = stableHash(species.badgeId, seed)
                val column = hash % 11
                val row = (hash / 11) % 7
                HabitatAnchor(.12f + column * .07f, .20f + row * .10f, .14f + (hash % 5) * .02f, 2f + row, .20f + row * .10f)
            }
        }
        val jitter = ((stableHash(species.badgeId, seed) ushr 8) % 9 - 4) / 500f
        return base.copy(
            x = (base.x + jitter + index / 10_000f).coerceIn(0f, 1f),
            zIndex = base.zIndex + index / 100f,
        )
    }

    private fun stableHash(value: String, seed: Int): Int { var hash = seed xor 0x5f3759df; value.forEach { hash = 31 * hash + it.code }; return hash and Int.MAX_VALUE }
}
