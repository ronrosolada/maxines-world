package com.maxinesworld.featurerewards

enum class WildlifeActivityPeriod { DIURNAL, NOCTURNAL, CREPUSCULAR }

data class WildlifeHabitatAffinity(
    val badgeId: String,
    val animalAssetId: String,
    val nameEnglish: String,
    val nameFilipino: String,
    val primaryHabitatId: String,
    val alternateHabitatId: String,
    val habitatEnglish: String,
    val habitatFilipino: String,
    val factEnglish: String,
    val factFilipino: String,
    val signatureBehavior: String,
    val activityPeriod: WildlifeActivityPeriod,
)

object SanctuarySpeciesCatalog {
    val species = listOf(
        WildlifeHabitatAffinity(
            "badge_philippine_eagle", "bird_philippine_eagle", "Philippine Eagle", "Agilang Pilipino",
            "sanctuary-lookout", "habitat-tall-trees", "Canopy Lookout / Tall Trees", "Tanawan sa Tuktok / Matataas na Puno",
            "The Philippine Eagle uses its powerful eyes to spot prey through the forest canopy.",
            "Ginagamit ng Agilang Pilipino ang matalas nitong paningin upang makakita sa kagubatan.",
            "Perches high and gently rustles its wings", WildlifeActivityPeriod.DIURNAL,
        ),
        WildlifeHabitatAffinity(
            "badge_philippine_tarsier", "mammal_philippine_tarsier", "Philippine Tarsier", "Mawmag",
            "sanctuary-tree", "habitat-bamboo-grove", "Story Tree / Bamboo Grove", "Puno ng Kuwento / Kawayanan",
            "A tarsier's enormous eyes help it see and leap safely at night.",
            "Tinutulungan ng malalaking mata ang mawmag na makakita at tumalon sa gabi.",
            "Clings to a branch and slowly blinks", WildlifeActivityPeriod.NOCTURNAL,
        ),
        WildlifeHabitatAffinity(
            "badge_tamaraw", "mammal_tamaraw", "Tamaraw", "Tamaraw",
            "sanctuary-meadow", "habitat-pasture", "Sunny Meadow / Pasture", "Maaraw na Damuhan / Pastulan",
            "The tamaraw is a small wild buffalo found only on Mindoro.",
            "Ang tamaraw ay maliit na mailap na kalabaw na matatagpuan lamang sa Mindoro.",
            "Grazes calmly and twitches its ears", WildlifeActivityPeriod.CREPUSCULAR,
        ),
        WildlifeHabitatAffinity(
            "badge_sinarapan", "fish_sinarapan", "Sinarapan", "Sinarapan",
            "sanctuary-pond", "habitat-stream", "Little Pond / Stream", "Munting Lawa / Sapa",
            "Sinarapan is one of the world's smallest commercially harvested fish.",
            "Ang sinarapan ay isa sa pinakamaliit na isdang hinuhuli para sa pagkain sa mundo.",
            "Darts through clear water and makes tiny ripples", WildlifeActivityPeriod.DIURNAL,
        ),
    )

    fun byBadgeId(id: String) = species.firstOrNull { it.badgeId == id }
    fun requireByBadgeId(id: String) = requireNotNull(byBadgeId(id)) { "Unknown sanctuary badge: $id" }
    fun byAnimalAssetId(id: String) = species.firstOrNull { it.animalAssetId == id }
}
