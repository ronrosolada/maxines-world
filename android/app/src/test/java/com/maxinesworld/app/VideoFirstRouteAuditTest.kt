package com.maxinesworld.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Static navigation audit for the video-first child-home contract.
 *
 * The text-lesson stack (bundled lesson JSON, ContentLessonLoader, activity
 * renderers, and the three activity engines) has been fully removed; only
 * video, Assessment Arena, and reward surfaces remain. These tests enforce
 * that the text-lesson system never returns.
 */
class VideoFirstRouteAuditTest {

    private fun repoRoot(): File {
        val start = File(System.getProperty("user.dir") ?: ".").canonicalFile
        val candidates = generateSequence(start) { it.parentFile }
            .flatMap { directory ->
                sequenceOf(
                    directory.takeIf { File(it, ".git").exists() },
                    directory.parentFile.takeIf {
                        File(directory, "settings.gradle.kts").isFile && directory.name == "android"
                    },
                ).filterNotNull()
            }
            .distinct()

        return candidates.firstOrNull { candidate ->
            File(candidate, "android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt").isFile
        } ?: error("Could not locate repository root from ${start.path}")
    }

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
    fun `text lesson stack is fully removed from the repo`() {
        val root = repoRoot()
        val removedArtifacts = listOf(
            "android/core-content/src/main/java/com/maxinesworld/corecontent/ContentLessonLoader.kt",
            "android/core-content/src/main/java/com/maxinesworld/corecontent/ModuleCatalog.kt",
            "android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/LessonPlayerViewModel.kt",
            "android/engine-activity",
            "android/engine-assessment",
            "android/engine-mastery",
            "android/app/src/main/assets/content-pack/month-01",
            "archive/legacy-text-lessons",
        )
        removedArtifacts.forEach { relative ->
            assertFalse("text-lesson artifact must be removed: $relative", File(root, relative).exists())
        }
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

        assertTrue(childHomeBlock.contains("QuestPlayRouter.intentForQuestAction"))
        assertTrue(childHomeBlock.contains("QuestPlayRouter.intentForTarget"))
        assertTrue(source.contains("Routes.videoLibrary(childId, intent.subjectId, intent.mediaId)"))
        assertTrue(
            source.contains(
                "Routes.assessmentArena(childId, intent.subjectId, intent.packId)",
            ),
        )
        assertTrue(source.contains("assessment_arena/{childId}?subject={subject}&packId={packId}"))
        assertTrue(source.contains("video_library/{childId}?subject={subject}&mediaId={mediaId}"))
        assertTrue(source.contains("packId: String? = null"))
        assertTrue(source.contains("mediaId: String? = null"))
    }

    @Test
    fun `video library builder carries an assigned mediaId when present`() {
        val source = navGraph().readText()
        assertTrue(
            source.contains(
                "fun videoLibrary(childId: String, subject: String? = null, mediaId: String? = null)",
            ),
        )
        assertTrue(source.contains("&mediaId=\${segment(mediaId.orEmpty())}"))
    }
}
