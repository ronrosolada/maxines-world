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
        species("badge_philippine_eagle", "bird_philippine_eagle", "Philippine Eagle", "Agilang Pilipino", "sanctuary-lookout", "habitat-tall-trees", "Canopy Lookout / Tall Trees", "Tanawan sa Tuktok / Matataas na Puno", "The Philippine Eagle uses powerful eyes to spot prey through the forest canopy.", "Ginagamit ng Agilang Pilipino ang matalas nitong paningin upang makakita sa kagubatan.", "Perches high and gently rustles its wings", WildlifeActivityPeriod.DIURNAL),
        species("badge_philippine_tarsier", "mammal_philippine_tarsier", "Philippine Tarsier", "Mawmag", "sanctuary-tree", "habitat-bamboo-grove", "Story Tree / Bamboo Grove", "Puno ng Kuwento / Kawayanan", "A tarsier's enormous eyes help it see and leap safely at night.", "Tinutulungan ng malalaking mata ang mawmag na makakita at tumalon sa gabi.", "Clings to a branch and slowly blinks", WildlifeActivityPeriod.NOCTURNAL),
        species("badge_tamaraw", "mammal_tamaraw", "Tamaraw", "Tamaraw", "sanctuary-meadow", "habitat-pasture", "Sunny Meadow / Pasture", "Maaraw na Damuhan / Pastulan", "The tamaraw is a small wild buffalo found only on Mindoro.", "Ang tamaraw ay maliit na mailap na kalabaw na matatagpuan lamang sa Mindoro.", "Grazes calmly and twitches its ears", WildlifeActivityPeriod.CREPUSCULAR),
        species("badge_sinarapan", "fish_sinarapan", "Sinarapan", "Sinarapan", "sanctuary-pond", "habitat-stream", "Little Pond / Stream", "Munting Lawa / Sapa", "Sinarapan is one of the world's smallest commercially harvested fish.", "Ang sinarapan ay isa sa pinakamaliit na isdang hinuhuli para sa pagkain sa mundo.", "Darts through clear water and makes tiny ripples", WildlifeActivityPeriod.DIURNAL),
        species("badge_peacock_pheasant", "bird_peacock_pheasant", "Palawan Peacock-Pheasant", "Palawan Peacock-Pheasant", "habitat-forest-floor", "sanctuary-flower-bed", "Forest Floor / Flower Bed", "Sahig ng Gubat / Taniman ng Bulaklak", "The male fans shimmering eye-spotted feathers during its forest courtship dance.", "Ibinubuka ng lalaki ang makinang na balahibong may tila mga mata sa sayaw-panliligaw nito.", "Forages softly among fallen leaves", WildlifeActivityPeriod.DIURNAL),
        species("badge_cebu_flowerpecker", "bird_cebu_flowerpecker", "Cebu Flowerpecker", "Cebu Flowerpecker", "habitat-canopy", "habitat-flowering-trees", "Canopy / Flowering Trees", "Tuktok ng Gubat / Namumulaklak na Puno", "This tiny Cebu endemic helps flowering trees by carrying pollen and seeds.", "Tumutulong ang munting ibong likas sa Cebu sa pagkalat ng polen at mga buto.", "Flits between blossoms and sips nectar", WildlifeActivityPeriod.DIURNAL),
        species("badge_spotted_deer", "mammal_spotted_deer", "Visayan Spotted Deer", "Usang Batik-batik ng Visayas", "sanctuary-meadow", "habitat-forest-edge", "Sunny Meadow / Forest Edge", "Maaraw na Damuhan / Gilid ng Gubat", "Its pale spots help this rare Visayan deer blend into dappled forest light.", "Tumutulong ang mapuputing batik nito upang magtago sa salit-salit na liwanag ng gubat.", "Browses fresh leaves and listens carefully", WildlifeActivityPeriod.CREPUSCULAR),
        species("badge_pangolin", "mammal_pangolin", "Palawan Pangolin", "Balintong ng Palawan", "sanctuary-tree", "habitat-forest-floor", "Story Tree / Forest Floor", "Puno ng Kuwento / Sahig ng Gubat", "The Palawan pangolin uses strong claws and a long sticky tongue to eat ants and termites.", "Ginagamit ng balintong ng Palawan ang malalakas na kuko at mahabang malagkit na dila sa pagkain ng langgam at anay.", "Snuffles under logs and curls up when shy", WildlifeActivityPeriod.NOCTURNAL),
        species("badge_flying_fox", "mammal_flying_fox", "Philippine Flying Fox", "Kabog ng Pilipinas", "habitat-tall-trees", "habitat-canopy", "Tall Trees / Canopy", "Matataas na Puno / Tuktok ng Gubat", "Flying foxes pollinate forest flowers and spread seeds while feeding on fruit at night.", "Nagkakalat ng polen at buto ang kabog habang kumakain ng prutas sa gabi.", "Hangs upside down, then glides at dusk", WildlifeActivityPeriod.NOCTURNAL),
        species("badge_philippine_crocodile", "reptile_philippine_crocodile", "Philippine Crocodile", "Buwayang Pilipino", "sanctuary-pond", "habitat-river-bank", "Little Pond / River Bank", "Munting Lawa / Pampang ng Ilog", "This rare freshwater crocodile helps keep Philippine river ecosystems balanced.", "Tumutulong ang pambihirang buwayang-tabang na panatilihing balanse ang mga ilog sa Pilipinas.", "Basks quietly, then slips into the water", WildlifeActivityPeriod.DIURNAL),
    )

    fun byBadgeId(id: String) = species.firstOrNull { it.badgeId == id }
    fun requireByBadgeId(id: String) = requireNotNull(byBadgeId(id)) { "Unknown sanctuary badge: $id" }
    fun byAnimalAssetId(id: String) = species.firstOrNull { it.animalAssetId == id }

    private fun species(badgeId: String, assetId: String, en: String, fil: String, primary: String, alternate: String, habitatEn: String, habitatFil: String, factEn: String, factFil: String, behavior: String, period: WildlifeActivityPeriod) =
        WildlifeHabitatAffinity(badgeId, assetId, en, fil, primary, alternate, habitatEn, habitatFil, factEn, factFil, behavior, period)
}
