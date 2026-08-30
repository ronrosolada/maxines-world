package com.maxinesworld.gametarsiercanopy

/**
 * Hand-built deterministic canopy courses (normalized coordinates).
 *
 * Vine × firefly placement is tuned so a hop started ~0.4–0.6 units before a LOW
 * vine (or 0.8–1.0 before a HIGH vine) clears it cleanly, and fireflies sit on
 * reachable hop arcs for manual collection.
 */
object CanopyCourses {
    val all: List<CanopyCourse> = listOf(
        CanopyCourse(
            id = "canopy-morning",
            title = "Dawn Canopy",
            length = 10.2f,
            vines = listOf(
                CanopyVine("v1", 1.6f, VineKind.LOW),
                CanopyVine("v2", 5.4f, VineKind.HIGH),
                CanopyVine("v3", 8.6f, VineKind.LOW),
            ),
            fireflies = listOf(
                CanopyFirefly("f1", 0.9f, 0.60f),
                CanopyFirefly("f2", 3.4f, 0.75f),
                CanopyFirefly("f3", 6.6f, 0.80f),
                CanopyFirefly("f4", 8.0f, 0.55f),
                CanopyFirefly("f5", 9.7f, 0.70f),
            ),
        ),
        CanopyCourse(
            id = "canopy-moonlight",
            title = "Moonlight Canopy",
            length = 11f,
            vines = listOf(
                CanopyVine("v1", 2.0f, VineKind.LOW),
                CanopyVine("v2", 5.8f, VineKind.HIGH),
                CanopyVine("v3", 9.0f, VineKind.HIGH),
            ),
            fireflies = listOf(
                CanopyFirefly("f1", 1.1f, 0.65f),
                CanopyFirefly("f2", 3.9f, 0.80f),
                CanopyFirefly("f3", 7.2f, 0.60f),
                CanopyFirefly("f4", 9.9f, 0.75f),
                CanopyFirefly("f5", 10.5f, 0.55f),
            ),
        ),
        CanopyCourse(
            id = "canopy-sunset",
            title = "Sunset Canopy",
            length = 10.5f,
            vines = listOf(
                CanopyVine("v1", 1.7f, VineKind.LOW),
                CanopyVine("v2", 5.1f, VineKind.HIGH),
                CanopyVine("v3", 8.9f, VineKind.LOW),
            ),
            fireflies = listOf(
                CanopyFirefly("f1", 0.8f, 0.60f),
                CanopyFirefly("f2", 3.3f, 0.82f),
                CanopyFirefly("f3", 5.7f, 0.50f),
                CanopyFirefly("f4", 7.1f, 0.86f),
                CanopyFirefly("f5", 9.8f, 0.60f),
            ),
        ),
    )
}