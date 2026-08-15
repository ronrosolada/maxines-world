package com.maxinesworld.featurerewards

/** A small, deterministic collection of sanctuary pieces earned through learning. */
data class SanctuaryPiece(
    val id: String,
    val name: String,
    val description: String,
    val iconKey: String,
    val residentWildlife: List<String> = emptyList(),
    val funFact: String = "",
)

object SanctuaryCatalog {
    val pieces: List<SanctuaryPiece> = listOf(
        SanctuaryPiece(
            id = "sanctuary-meadow",
            name = "Sunny Meadow",
            description = "A bright place for Milo's friends to rest.",
            iconKey = "meadow",
            residentWildlife = listOf("Tamaraw", "Calamian Deer", "Visayan Spotted Deer"),
            funFact = "The sunny meadow is open and warm, giving grazing animals plenty of fresh grass!",
        ),
        SanctuaryPiece(
            id = "sanctuary-pond",
            name = "Little Pond",
            description = "A quiet pond where animals can drink safely.",
            iconKey = "pond",
            residentWildlife = listOf("Sinarapan", "Silver Therapon", "Philippine Box Turtle"),
            funFact = "Freshwater ponds in the Philippines are home to Sinarapan, the world's smallest commercial fish!",
        ),
        SanctuaryPiece(
            id = "sanctuary-tree",
            name = "Story Tree",
            description = "A shady tree for curious explorers.",
            iconKey = "tree",
            residentWildlife = listOf("Philippine Tarsier", "Flying Fox", "Visayan Hornbill"),
            funFact = "Ancient Philippine rainforest trees provide safe hollows and fruit for hornbills and nocturnal tarsiers.",
        ),
        SanctuaryPiece(
            id = "sanctuary-nest",
            name = "Bird Nest",
            description = "A cozy home for feathered friends.",
            iconKey = "nest",
            residentWildlife = listOf("Cebu Flowerpecker", "Whiskered Pitta", "Rufous Hornbill"),
            funFact = "Birds weave twigs and soft moss together to keep their baby chicks safe and warm.",
        ),
        SanctuaryPiece(
            id = "sanctuary-garden",
            name = "Kindness Garden",
            description = "A garden grown by thoughtful learners.",
            iconKey = "garden",
            residentWildlife = listOf("Luzon Peacock Swallowtail", "Philippine Box Turtle"),
            funFact = "Gardens with local flowering plants give sweet nectar to native Philippine butterflies.",
        ),
        SanctuaryPiece(
            id = "sanctuary-path",
            name = "Forest Path",
            description = "A path leading to the next discovery.",
            iconKey = "path",
            residentWildlife = listOf("Palawan Pangolin", "Philippine Mouse Deer", "Visayan Warty Pig"),
            funFact = "Forest paths help small mammals like the shy Mouse Deer move quietly under the trees.",
        ),
        SanctuaryPiece(
            id = "sanctuary-shelter",
            name = "Animal Shelter",
            description = "A safe place for every animal to belong.",
            iconKey = "shelter",
            residentWildlife = listOf("Philippine Crocodile", "Palawan Pangolin"),
            funFact = "Safe shelters protect vulnerable wildlife during monsoon rains and storms.",
        ),
        SanctuaryPiece(
            id = "sanctuary-butterfly",
            name = "Butterfly Corner",
            description = "A gentle corner full of color and movement.",
            iconKey = "butterfly",
            residentWildlife = listOf("Luzon Peacock Swallowtail"),
            funFact = "The rare Luzon Peacock Swallowtail has vibrant emerald green scales on its wings!",
        ),
        SanctuaryPiece(
            id = "sanctuary-lookout",
            name = "Canopy Lookout",
            description = "A high lookout for spotting new ideas.",
            iconKey = "lookout",
            residentWildlife = listOf("Philippine Eagle", "Flying Fox", "Rufous Hornbill"),
            funFact = "From the high canopy lookout, the mighty Philippine Eagle can see for miles across the forest!",
        ),
        SanctuaryPiece(
            id = "sanctuary-reading-nest",
            name = "Reading Nest",
            description = "A peaceful place to read and wonder.",
            iconKey = "reading",
            residentWildlife = listOf("Philippine Trogon", "Cebu Flowerpecker"),
            funFact = "The colorful Philippine Trogon loves quiet, shaded perches where it sits softly between songs.",
        ),
        SanctuaryPiece(
            id = "sanctuary-flower-bed",
            name = "Flower Bed",
            description = "Small acts of care help this bed bloom.",
            iconKey = "flower",
            residentWildlife = listOf("Luzon Peacock Swallowtail", "Scarlet-collared Flowerpecker"),
            funFact = "Flowerpeckers are tiny birds that help forest flowers by spreading seeds and pollen.",
        ),
        SanctuaryPiece(
            id = "sanctuary-wildlife-sign",
            name = "Wildlife Sign",
            description = "A reminder that learning helps protect living things.",
            iconKey = "sign",
            residentWildlife = listOf("Panay Monitor", "Sailfin Lizard"),
            funFact = "Caring for our sanctuary reminds us that every plant and animal in the Philippines is special!",
        ),
    )

    fun byId(id: String): SanctuaryPiece? = pieces.firstOrNull { it.id == id }

    fun pieceAt(index: Int): SanctuaryPiece = pieces[index.coerceAtLeast(0) % pieces.size]
}
