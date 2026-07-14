package com.maxinesworld.coremodel

/**
 * Typed subject model for safe routing.
 *
 * All supported subjects. Unknown or unrecognized subject IDs resolve to null.
 * There is NO default fallback — callers must handle the null case explicitly.
 *
 * History/Makabansa aliases: [history], [philippine-history], and [makabansa]
 * all resolve to [MAKABANSA].
 */
enum class Subject(val id: String, val lessonId: String, val displayName: String) {
    ENGLISH("english", "english-g3-m01-d01", "English"),
    FILIPINO("filipino", "filipino-g3-m01-d01", "Filipino"),
    MATHEMATICS("mathematics", "mathematics-g3-m01-d01", "Mathematics"),
    SCIENCE("science", "science-g3-m01-d01", "Science"),
    MAKABANSA("makabansa", "mkb-g3-m01-l01", "Makabansa"),
    GMRC("gmrc", "gmrc-g3-m01-l01", "GMRC");

    companion object {
        private val byId: Map<String, Subject> = entries.associateBy { it.id }
        private val historyAliases = setOf("history", "philippine-history", "makabansa")

        /**
         * Resolve a subject ID string to a [Subject], or null for unknown IDs.
         *
         * Handles all accepted History/Makabansa aliases consistently.
         * Returns null for null, blank, or unrecognized values — never defaults.
         */
        fun fromId(id: String?): Subject? {
            if (id.isNullOrBlank()) return null
            val normalized = id.trim().lowercase()
            if (normalized in historyAliases) return MAKABANSA
            return byId[normalized]
        }

        /** All supported subject ID strings. */
        val allIds: Set<String> = entries.map { it.id }.toSet()
    }
}
