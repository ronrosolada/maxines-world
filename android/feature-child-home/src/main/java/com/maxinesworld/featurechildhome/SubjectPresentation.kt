package com.maxinesworld.featurechildhome

/** Island/subject ID → the friendly display name used in child-facing cards. */
fun subjectDisplayName(subject: String): String = when (subject) {
    "english" -> "Story Time"
    "mathematics" -> "Number Fun"
    "filipino" -> "Kwentuhan"
    "science" -> "Discovery"
    "heritage-harbor" -> "Heritage"
    "gmrc" -> "Kindness"
    "araling-panlipunan" -> "Araling Panlipunan"
    "makabansa" -> "Makabansa"
    else -> subject.replaceFirstChar { it.uppercase() }
}

/** Normalize an island ID to the bundled content-pack subject used by compatibility tests. */
fun subjectForPack(subject: String): String? = when (subject) {
    "heritage-harbor", "philippine-history", "araling_panlipunan" -> "araling-panlipunan"
    "makabansa", "mathematics", "english", "science", "filipino", "gmrc" -> subject
    else -> null
}
