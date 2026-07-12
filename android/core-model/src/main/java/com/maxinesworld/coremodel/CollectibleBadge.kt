package com.maxinesworld.coremodel

/**
 * A collectible badge awarded for completing lesson milestones.
 * 50 badges across 5 biomes featuring Philippine endemic animals.
 */
data class CollectibleBadge(
    val id: String,
    val biome: String,
    val name: String,
    val title: String,
    val funFact: String,
    val emoji: String,
    val isCollected: Boolean = false,
    val collectedAtEpochMillis: Long = 0L
)

enum class BadgeBiome(val displayName: String, val colorHex: Long, val description: String) {
    FOREST_FRIENDS("Forest Friends", 0xFF2E7D32, "Mammals of the Philippine forests"),
    SKY_SCOUTS("Sky Scouts", 0xFF1976D2, "Birds of prey and the open skies"),
    SONGBIRD_GROVE("Songbird Grove", 0xFF7B1FA2, "Colorful songbirds of the islands"),
    RIVER_GUARDIANS("River Guardians", 0xFFE65100, "Reptiles that guard our rivers"),
    CREEK_CORAL("Creek & Coral", 0xFF00838F, "Amphibians, fish, and butterflies");

    companion object {
        fun fromId(id: String): BadgeBiome = when (id) {
            "forest_friends" -> FOREST_FRIENDS
            "sky_scouts" -> SKY_SCOUTS
            "songbird_grove" -> SONGBIRD_GROVE
            "river_guardians" -> RIVER_GUARDIANS
            "creek_coral" -> CREEK_CORAL
            else -> FOREST_FRIENDS
        }
    }
}
