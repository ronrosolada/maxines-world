package com.maxinesworld.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Static navigation audit for the video-first child-home contract.
 *
 * The lesson JSON, ContentLessonLoader, and renderer/conversion tests remain
 * intentionally supported for offline compatibility and assessment/reward
 * integration. They are not child-home navigation targets.
 */
class VideoFirstRouteAuditTest {

    private fun repoRoot(): File = listOf(
        File("."),
        File("android"),
        File("/home/ron/projects/maxines-world"),
    ).first { File(it, "android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt").isFile }

    private fun navGraph(): File = File(
        repoRoot(),
        "android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt",
    )

    private fun oldTextRouteTokens(): List<String> = listOf(
        "Routes." + "lessonPlayer(",
        "Routes." + "subjectModules(",
        "Routes." + "MODULE_" + "LESSONS",
        "Routes." + "LESSON_" + "PLAYER",
    )

    @Test
    fun `child home subject exploration targets video library only`() {
        val source = navGraph().readText()
        val childHomeStart = source.indexOf("route = Routes.CHILD_HOME")
        val nextRoute = source.indexOf("route = Routes.ASSESSMENT_ARENA", childHomeStart)
        assertTrue("child-home route must remain in MaxinesNavGraph", childHomeStart >= 0)
        assertTrue("child-home block must have a following route", nextRoute > childHomeStart)

        val childHomeBlock = source.substring(childHomeStart, nextRoute)
        assertTrue("child-home must expose video-library navigation", childHomeBlock.contains("Routes.videoLibrary("))
        oldTextRouteTokens().forEach { token ->
            assertFalse("child-home must not target retired text route $token", childHomeBlock.contains(token))
        }
    }

    @Test
    fun `retired text lesson route references are absent from production Kotlin`() {
        val sourceRoot = File(repoRoot(), "android")
        val productionFiles = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.path }
            .toList()
        assertTrue("production Kotlin sources must be present", productionFiles.isNotEmpty())

        val offenders = productionFiles.flatMap { file ->
            val text = file.readText()
            oldTextRouteTokens().filter { token -> token in text }.map { token ->
                "${file.relativeTo(repoRoot()).path}: $token"
            }
        }
        assertTrue("retired text route references remain: $offenders", offenders.isEmpty())
    }

    @Test
    fun `intentional lesson compatibility surfaces remain without a child route`() {
        val root = repoRoot()
        val intentionalFiles = listOf(
            "android/core-content/src/main/java/com/maxinesworld/corecontent/ContentLessonLoader.kt",
            "android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/LessonPlayerViewModel.kt",
            "android/engine-activity/src/main/java/com/maxinesworld/engineactivity/renderers/ActivityRenderer.kt",
            "android/app/src/androidTest/java/com/maxinesworld/app/OfflineLessonLoadTest.kt",
        )
        intentionalFiles.forEach { relative ->
            assertTrue("intentional compatibility file is missing: $relative", File(root, relative).isFile)
        }
        assertTrue(
            "bundled lesson assets remain for offline/content-loader compatibility",
            File(root, "android/app/src/main/assets/content-pack").isDirectory,
        )
    }

    @Test
    fun `assessment arena remains a separate intentional child destination`() {
        val source = navGraph().readText()
        assertTrue(source.contains("route = Routes.ASSESSMENT_ARENA"))
        assertTrue(source.contains("AssessmentArenaRoute("))
        assertTrue(source.contains("route = Routes.VIDEO_LIBRARY"))
    }

    @Test
    fun `mixed mission targets preserve distinct video and arena destinations`() {
        val source = navGraph().readText()
        val childHomeStart = source.indexOf("route = Routes.CHILD_HOME")
        val arenaRoute = source.indexOf("route = Routes.ASSESSMENT_ARENA", childHomeStart)
        val childHomeBlock = source.substring(childHomeStart, arenaRoute)

        assertTrue(childHomeBlock.contains("Routes.videoLibrary(childId, target.subjectId)"))
        assertTrue(
            childHomeBlock.contains(
                "Routes.assessmentArena(childId, target.subjectId, target.arenaPackId)",
            ),
        )
        assertTrue(source.contains("assessment_arena/{childId}?subject={subject}&packId={packId}"))
        assertTrue(source.contains("packId: String? = null"))
    }
}
