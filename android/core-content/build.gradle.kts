plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.maxinesworld.corecontent"
    compileSdk = 35
    defaultConfig { minSdk = 26; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core-model"))
    implementation(libs.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.test.runner)
}

// ContentPackIntegrityTest reads the bundled lesson pack via relative
// paths; declaring it as a task input makes the test re-run whenever the
// pack changes, so Gradle's build cache can never serve a stale pass.
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("app/src/main/assets/content-pack/month-01/lessons"))
        .withPropertyName("bundledLessonPack")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
