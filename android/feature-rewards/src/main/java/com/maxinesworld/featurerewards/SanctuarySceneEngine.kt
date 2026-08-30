package com.maxinesworld.featurerewards

/** Normalized placement independent of display size or Compose. */
data class HabitatAnchor(
    val x: Float,
    val y: Float,
    val scale: Float,
    val zIndex: Float,
    val layerDepth: Float,
)

data class SanctuaryResident(
    val species: WildlifeHabitatAffinity,
    val habitatId: String,
    val anchor: HabitatAnchor,
)

data class SanctuaryScene(val residents: List<SanctuaryResident>)

/** Pure deterministic eligibility and placement algorithm. */
class SanctuarySceneEngine {
    fun build(
        unlockedHabitats: Set<String>,
        earnedBadges: Set<String>,
        dateSeed: Int,
    ): SanctuaryScene {
        val eligible = SanctuarySpeciesCatalog.species.filter { species ->
            species.badgeId in earnedBadges &&
                (species.primaryHabitatId in unlockedHabitats || species.alternateHabitatId in unlockedHabitats)
        }
        val ordered = eligible.sortedWith(compareBy<WildlifeHabitatAffinity> { stableHash(it.badgeId, dateSeed) }.thenBy { it.badgeId })
        return SanctuaryScene(ordered.mapIndexed { index, species ->
            val habitat = if (species.primaryHabitatId in unlockedHabitats) species.primaryHabitatId else species.alternateHabitatId
            SanctuaryResident(species, habitat, anchorFor(species, index, dateSeed))
        })
    }

    private fun anchorFor(species: WildlifeHabitatAffinity, index: Int, seed: Int): HabitatAnchor {
        val base = when (species.badgeId) {
            "badge_philippine_eagle" -> HabitatAnchor(.76f, .24f, .20f, 2f, .18f)
            "badge_philippine_tarsier" -> HabitatAnchor(.61f, .42f, .17f, 3f, .38f)
            "badge_tamaraw" -> HabitatAnchor(.48f, .69f, .25f, 5f, .72f)
            else -> HabitatAnchor(.23f, .76f, .14f, 4f, .78f)
        }
        val jitter = ((stableHash(species.badgeId, seed) ushr 8) % 9 - 4) / 500f
        return base.copy(x = (base.x + jitter).coerceIn(0f, 1f), zIndex = base.zIndex + index / 100f)
    }

    private fun stableHash(value: String, seed: Int): Int {
        var hash = seed xor 0x5f3759df
        value.forEach { hash = 31 * hash + it.code }
        return hash and Int.MAX_VALUE
    }
}
