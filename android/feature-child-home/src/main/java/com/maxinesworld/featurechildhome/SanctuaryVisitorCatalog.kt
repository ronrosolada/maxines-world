package com.maxinesworld.featurechildhome

import com.maxinesworld.featurerewards.SanctuaryCatalog

/**
 * Deterministic visitor registry for Milo's Wildlife Sanctuary.
 * As habitats are unlocked in the sanctuary, visiting native Philippine animals
 * appear in their corresponding habitat slots.
 */
object SanctuaryVisitorCatalog {

    val allVisitors = listOf(
        SanctuaryVisitorUi(
            id = "visitor-tarsier",
            name = "Philippine Tarsier",
            localName = "Mawmag / Tarsier",
            slotId = "sanctuary-tree",
            drawableResName = "animal_philippine_tarsier",
            isNocturnal = true,
            favoriteTreat = "Sweet Fig",
            nativeRegion = "Bohol, Samar, Leyte",
            funFact = "One of the world's smallest primates! Can rotate its head 180° and jump 40 times its body length.",
        ),
        SanctuaryVisitorUi(
            id = "visitor-eagle",
            name = "Philippine Eagle",
            localName = "Haribon / Haring Ibon",
            slotId = "sanctuary-lookout",
            drawableResName = "animal_philippine_eagle",
            isNocturnal = false,
            favoriteTreat = "Mountain Fruit",
            nativeRegion = "Sierra Madre, Mindanao",
            funFact = "The majestic national bird of the Philippines with a 2-meter wingspan, nesting in giant forest trees.",
        ),
        SanctuaryVisitorUi(
            id = "visitor-colugo",
            name = "Philippine Flying Lemur",
            localName = "Kagwang",
            slotId = "sanctuary-nest",
            drawableResName = "animal_philippine_colugo",
            isNocturnal = true,
            favoriteTreat = "Young Leaves",
            nativeRegion = "Mindanao, Bohol, Samar",
            funFact = "It glides effortlessly over 100 meters between jungle trees using its wide gliding membrane (patagium).",
        ),
        SanctuaryVisitorUi(
            id = "visitor-tamaraw",
            name = "Mindoro Tamaraw",
            localName = "Tamaraw",
            slotId = "sanctuary-meadow",
            drawableResName = "animal_tamaraw",
            isNocturnal = false,
            favoriteTreat = "Fresh Grass",
            nativeRegion = "Mindoro Island",
            funFact = "A rare and strong dwarf buffalo found only on Mindoro island with distinct V-shaped horns.",
        ),
        SanctuaryVisitorUi(
            id = "visitor-peacock",
            name = "Palawan Peacock-Pheasant",
            localName = "Tandikan",
            slotId = "sanctuary-garden",
            drawableResName = "animal_palawan_peacock_pheasant",
            isNocturnal = false,
            favoriteTreat = "Wild Berries",
            nativeRegion = "Palawan",
            funFact = "Known as Tandikan, the male displays stunning metallic blue and green eye-spots on its tail feathers.",
        ),
        SanctuaryVisitorUi(
            id = "visitor-pig",
            name = "Visayan Warty Pig",
            localName = "Baboy Damo",
            slotId = "sanctuary-path",
            drawableResName = "animal_visayan_warty_pig",
            isNocturnal = false,
            favoriteTreat = "Jungle Roots",
            nativeRegion = "Panay, Negros",
            funFact = "A forest-dwelling wild pig with an impressive tufted mane that helps plant new seeds in the jungle!",
        ),
    )

    fun getVisitorsForUnlockedPieces(earnedPieceIds: Set<String>): List<SanctuaryVisitorUi> {
        return allVisitors.filter { visitor ->
            visitor.slotId in earnedPieceIds
        }
    }
}
