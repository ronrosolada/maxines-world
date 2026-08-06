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
        versionCode = 20
        versionName = "0.20.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release keystore, configured via USER-LEVEL properties file
        // (~/.gradle/maxines-world-signing.properties) — never committed.
        // Absent file → signingConfig stays null → unsigned release build,
        // so CI/contributor builds keep working without a keystore.
        create("release") {
            val props = Properties().apply {
                val f = File(System.getProperty("user.home"), ".gradle/maxines-world-signing.properties")
                if (f.isFile) f.inputStream().use { load(it) }
            }
            storeFile = props.getProperty("MW_KEYSTORE_PATH")?.let { File(it) }
            storePassword = props.getProperty("MW_KEYSTORE_PASS")
            keyAlias = props.getProperty("MW_KEY_ALIAS")
            keyPassword = props.getProperty("MW_KEY_PASS")
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
    description = "Validate educator metadata and enforce release approval"
    val packDir = project.layout.projectDirectory.dir("src/main/assets/content-pack/month-01/lessons")
    doLast {
        val slurper = groovy.json.JsonSlurper()
        var total = 0
        var unreviewed = 0
        val bad = mutableListOf<String>()
        val invalidMetadata = mutableListOf<String>()
        packDir.asFileTree.matching { include("*.json") }.forEach { file ->
            total++
            @Suppress("UNCHECKED_CAST")
            val lesson = slurper.parse(file) as Map<String, Any?>
            val validated = lesson["educatorValidated"] as? Boolean ?: false
            val releaseStatus = lesson["releaseStatus"] as? String
            val released = validated && releaseStatus == "RELEASED"
            val metadataConsistent = released ||
                (!validated && releaseStatus == "REQUIRES_EDUCATOR_REVIEW")
            if (!metadataConsistent) {
                invalidMetadata += file.name
            }
            if (!(validated && released)) {
                unreviewed++
                bad += file.name
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
