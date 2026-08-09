package com.maxinesworld.featurerewards

/** A small, deterministic collection of sanctuary pieces earned through learning. */
data class SanctuaryPiece(
    val id: String,
    val name: String,
    val description: String,
    val iconKey: String,
)

object SanctuaryCatalog {
    val pieces: List<SanctuaryPiece> = listOf(
        SanctuaryPiece("sanctuary-meadow", "Sunny Meadow", "A bright place for Milo's friends to rest.", "meadow"),
        SanctuaryPiece("sanctuary-pond", "Little Pond", "A quiet pond where animals can drink safely.", "pond"),
        SanctuaryPiece("sanctuary-tree", "Story Tree", "A shady tree for curious explorers.", "tree"),
        SanctuaryPiece("sanctuary-nest", "Bird Nest", "A cozy home for feathered friends.", "nest"),
        SanctuaryPiece("sanctuary-garden", "Kindness Garden", "A garden grown by thoughtful learners.", "garden"),
        SanctuaryPiece("sanctuary-path", "Forest Path", "A path leading to the next discovery.", "path"),
        SanctuaryPiece("sanctuary-shelter", "Animal Shelter", "A safe place for every animal to belong.", "shelter"),
        SanctuaryPiece("sanctuary-butterfly", "Butterfly Corner", "A gentle corner full of color and movement.", "butterfly"),
        SanctuaryPiece("sanctuary-lookout", "Canopy Lookout", "A high lookout for spotting new ideas.", "lookout"),
        SanctuaryPiece("sanctuary-reading-nest", "Reading Nest", "A peaceful place to read and wonder.", "reading"),
        SanctuaryPiece("sanctuary-flower-bed", "Flower Bed", "Small acts of care help this bed bloom.", "flower"),
        SanctuaryPiece("sanctuary-wildlife-sign", "Wildlife Sign", "A reminder that learning helps protect living things.", "sign"),
    )

    fun byId(id: String): SanctuaryPiece? = pieces.firstOrNull { it.id == id }

    fun pieceAt(index: Int): SanctuaryPiece = pieces[index.coerceAtLeast(0) % pieces.size]
}
