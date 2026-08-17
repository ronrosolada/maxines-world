plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

import java.io.File
import java.util.Properties

android {
    namespace = "com.maxinesworld.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.maxinesworld.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 59
        versionName = "0.47.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release keystore, configured via USER-LEVEL properties file
        // (~/.gradle/maxines-world-signing.properties) — never committed.
        // Absent file → no release signing config is registered, so CI and
        // contributor builds produce an unsigned release candidate.
        val signingFile = File(System.getProperty("user.home"), ".gradle/maxines-world-signing.properties")
        if (signingFile.isFile) {
            create("release") {
                val props = Properties().apply {
                    signingFile.inputStream().use { load(it) }
                }
                storeFile = props.getProperty("MW_KEYSTORE_PATH")?.let { File(it) }
                storePassword = props.getProperty("MW_KEYSTORE_PASS")
                keyAlias = props.getProperty("MW_KEY_ALIAS")
                keyPassword = props.getProperty("MW_KEY_PASS")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // All feature and engine modules
    implementation(project(":core-model"))
    implementation(project(":core-network"))
    implementation(project(":core-database"))
    implementation(project(":core-design-system"))
    implementation(project(":core-content"))
    implementation(project(":feature-auth"))
    implementation(project(":feature-child-home"))
    implementation(project(":feature-lesson-player"))
    implementation(project(":feature-progress"))
    implementation(project(":feature-parent"))
    implementation(libs.okhttp)
    implementation(project(":feature-rewards"))
    implementation(project(":engine-activity"))
    implementation(project(":engine-assessment"))
    implementation(project(":engine-mastery"))
    implementation(project(":engine-minigame"))
    implementation(project(":game-cat-cafe"))
    implementation(project(":game-pawprint-parkour"))
    implementation(project(":game-kitten-match"))

    // Material Components (for XML theme)
    implementation("com.google.android.material:material:1.12.0")

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Activity
    implementation(libs.activity.compose)

    // Core
    implementation(libs.core.ktx)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

// ─── Educator metadata gate ────────────────────────────────────────────────
// By default, fails if any playable lesson in the bundled pack is not
// educator-approved (educatorValidated=true AND releaseStatus=RELEASED).
// PR CI may explicitly allow review-gated drafts so content can be validated
// before human approval. The tag-based release workflow never sets this flag.
// Approval is performed deliberately via tools/mark_lessons_reviewed.py
// after a human curriculum review — the strict release gate exists so a
// release can never accidentally ship draft curriculum to a child.
val allowUnreviewedContent = providers.gradleProperty("allowUnreviewedContent")
    .map { it.toBoolean() }
    .orElse(false)

val verifyPlayableContent by tasks.registering {
    group = "verification"
    description = "Validate educator metadata and enforce release approval across EVERY lesson-bearing asset directory"
    // Scan the whole assets tree, not just content-pack/month-01: any JSON that
    // has the lesson shape (an `activities` list) is playable content, and no
    // unreviewed lesson may ship in the APK (external review finding C3).
    val assetsDir = project.layout.projectDirectory.dir("src/main/assets")
    doLast {
        val slurper = groovy.json.JsonSlurper()
        var total = 0
        var unreviewed = 0
        val bad = mutableListOf<String>()
        val invalidMetadata = mutableListOf<String>()
        val files = assetsDir.asFileTree.matching {
            include("**/*.json")
            exclude("**/mini-games/**")
        }.files.filter { file ->
            // Lesson-like shape only: parse is cheap relative to a false positive
            // on non-lesson JSON (badge_catalog, mini-game configs, manifests).
            runCatching {
                @Suppress("UNCHECKED_CAST")
                val candidate = slurper.parse(file) as Map<String, Any?>
                candidate["activities"] is List<*>
            }.getOrDefault(false)
        }
        files.forEach { file ->
            total++
            @Suppress("UNCHECKED_CAST")
            val lesson = slurper.parse(file) as Map<String, Any?>
            val validated = lesson["educatorValidated"] as? Boolean ?: false
            val releaseStatus = lesson["releaseStatus"] as? String
            val released = validated && releaseStatus == "RELEASED"
            val metadataConsistent = released ||
                (!validated && releaseStatus == "REQUIRES_EDUCATOR_REVIEW")
            if (!metadataConsistent) {
                invalidMetadata += file.relativeTo(assetsDir.asFile).path
            }
            if (!(validated && released)) {
                unreviewed++
                bad += file.relativeTo(assetsDir.asFile).path
            }
        }
        if (invalidMetadata.isNotEmpty()) {
            throw GradleException(
                "Educator metadata INVALID for ${invalidMetadata.size}/$total lessons " +
                    "(e.g. ${invalidMetadata.take(5)}). Use educatorValidated=false " +
                    "with REQUIRES_EDUCATOR_REVIEW, or educatorValidated=true with RELEASED."
            )
        }
        if (unreviewed > 0 && !allowUnreviewedContent.get()) {
            throw GradleException(
                "Release gate FAILED: $unreviewed/$total playable lessons are not " +
                    "educator-reviewed (e.g. ${bad.take(5)}). " +
                    "Run tools/mark_lessons_reviewed.py after a human curriculum review."
            )
        }
        if (unreviewed > 0) {
            println(
                "Educator metadata OK: $total playable lessons parsed; " +
                    "$unreviewed remain explicitly gated for human review."
            )
        } else {
            println("Release gate OK: $total playable lessons are educator-reviewed.")
        }
    }
}

// ─── Offline mini-game gate ────────────────────────────────────────────────
// Reward-break games are bundled HTML. Keep the catalog complete and prevent
// accidental network-capable content from entering the child-facing WebView.
val verifyOfflineMiniGames by tasks.registering {
    group = "verification"
    description = "Validate bundled mini-game count, CSP, and offline isolation"
    val gamesDir = project.layout.projectDirectory.dir("src/main/assets/mini-games/games")
    val requiredCspDirectives = listOf(
        "default-src 'none'",
        "base-uri 'none'",
        "form-action 'none'",
        "frame-src 'none'",
        "object-src 'none'",
        "img-src data: blob:",
        "style-src 'unsafe-inline'",
        "script-src 'unsafe-inline'",
        "connect-src 'none'",
    )
    val externalUrl = Regex("""(?i)https?://""")
    val networkApi = Regex("""(?i)\b(fetch|XMLHttpRequest|WebSocket|EventSource|sendBeacon)\b""")
    val htmlComment = Regex("""<!--(?s:.*?)-->""")
    val cssComment = Regex("""/\*(?s:.*?)\*/""")

    doLast {
        val htmlFiles = gamesDir.asFileTree.matching { include("*.html") }
            .files
            .sortedBy { it.name }
        if (htmlFiles.size != 29) {
            throw GradleException(
                "Offline mini-game gate FAILED: expected 29 HTML games, found ${htmlFiles.size}."
            )
        }

        val failures = mutableListOf<String>()
        htmlFiles.forEach { file ->
            val source = file.readText()
            val missingCsp = requiredCspDirectives.filterNot(source::contains)
            if (missingCsp.isNotEmpty()) {
                failures += "${file.name}: missing CSP ${missingCsp.joinToString()}."
            }

            // License/attribution comments may contain source URLs, but are
            // not executable or loadable resources. Inspect active markup/code.
            val activeSource = source.replace(htmlComment, "").replace(cssComment, "")
            if (externalUrl.containsMatchIn(activeSource)) {
                failures += "${file.name}: active external URL found."
            }
            if (networkApi.containsMatchIn(activeSource)) {
                failures += "${file.name}: browser network API found."
            }
        }
        if (failures.isNotEmpty()) {
            throw GradleException(
                "Offline mini-game gate FAILED:\n${failures.joinToString("\n")}"
            )
        }
        println("Offline mini-game gate OK: ${htmlFiles.size}/29 pages are CSP-protected and offline.")
    }
}

// The educator gate must run on every verification pass AND on the
// release build itself — a release can never ship draft curriculum.
// (2026-08-06: previously registered but never wired into any task.)
tasks.named("check") {
    dependsOn(verifyPlayableContent)
    dependsOn(verifyOfflineMiniGames)
}
// assembleRelease is created by AGP after project evaluation, so hook
// via matching/configureEach rather than named().
tasks.matching { it.name == "assembleRelease" }.configureEach {
    dependsOn(verifyPlayableContent)
    dependsOn(verifyOfflineMiniGames)
}
