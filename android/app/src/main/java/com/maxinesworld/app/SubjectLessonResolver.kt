package com.maxinesworld.app

internal object SubjectLessonResolver {
    private val lessonByDestination = mapOf(
        "english" to "english-g3-m01-d01",
        "filipino" to "filipino-g3-m01-d01",
        "mathematics" to "mathematics-g3-m01-d01",
        "science" to "science-g3-m01-d01",
        "history" to "mkb-g3-m01-l01",
        "philippine-history" to "mkb-g3-m01-l01",
        "makabansa" to "mkb-g3-m01-l01",
        "gmrc" to "gmrc-g3-m01-l01",
    )

    fun resolve(destinationId: String): String? = lessonByDestination[destinationId]
}
