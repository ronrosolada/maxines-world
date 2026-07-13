# Maxine's World v0.2.0 Audit and Implementation Handoff

## Objective

Bring the Android app into alignment with `design.md` and implement reliable educational-content loading from the local NAS at `10.10.10.33`, while preserving bundled lessons as an offline fallback.

Audit target: release tag `v0.2.0`, commit `8e9c9ad811f8b580a5cbe9ec6dc011cfe8dafcd3`. 

## Important audit limitation

No `design.md` exists in the audited repository tree. The UI comparison therefore uses:

* The v0.2.0 release description.
* Design references embedded in source comments.
* Architecture documentation.
* The implemented design tokens.

Add the authoritative `design.md` to the repository before accepting visual work. Do not invent missing design requirements.

## Executive findings

### P0: NAS content is not implemented

The failure is not merely a connectivity problem. The application has no functional NAS client:

* `ApiClient` is an empty placeholder with no base URL or HTTP operations. 
* `LessonLoader.loadLesson()` reads only packaged APK assets. It never performs a network request or searches downloaded content. 
* `loadLessonFromFile()` can parse one explicitly supplied file, but nothing downloads, installs, discovers, or activates that file. 
* Navigation passes only hard-coded lesson IDs. It does not pass or resolve a content source, catalog, pack version, or NAS URL. 
* Exceptions are silently converted to `null`, making missing files, invalid JSON, and access failures indistinguishable. 
* The manifest grants internet access but does not configure cleartext HTTP. With target SDK 35, plain `http://10.10.10.33` will normally be blocked unless narrowly permitted. 

### P0: The release overstates visual compliance

The release promises illustrated subject buildings, a Milo mascot, a 12-column grid, and design-compliant presentation. 

The implementation instead uses:

* Emoji characters and generic Material icons rather than production mascot and building artwork. 
* A manually chunked one-, two-, or three-column card layout rather than a 12-column grid. 
* Numerous local font sizes, spacings, radii, colors, opacities, and elevations instead of semantic design tokens. 
* The platform default font rather than a defined brand typeface. 
* Continuously animated character bobbing without reduced-motion handling. 

### P0: Release identity is inconsistent

The GitHub release is `v0.2.0`, but its APK is named `app-debug.apk`, while the tagged build configuration reports `versionName = "0.1.0"`. 

Correct versioning and publish a reproducible release APK before visual comparisons are finalized.

## Required implementation

### 1. Establish an auditable design baseline

* Add the authoritative file as `docs/design.md`.
* Add approved reference images for phone and tablet layouts.
* Create screenshot baselines for compact, medium, expanded, and large-tablet widths.
* Treat `docs/design.md` as authoritative when it conflicts with release prose or existing code.
* Record any unavoidable deviation in `docs/design-deviations.md`.

### 2. Build a complete content-delivery pipeline

Create these responsibilities rather than extending `LessonLoader` into a monolith:

```text
core-network/
  ContentApi.kt
  ContentHttpClient.kt
  ContentSourceConfig.kt

core-content/
  ContentRepository.kt
  ContentPackDownloader.kt
  ContentPackInstaller.kt
  ContentCatalog.kt
  LessonResolver.kt
  ContentResult.kt
```

Required flow:

1. Read a configurable base URL.
2. Fetch a content-pack manifest.
3. Validate schema version and relative paths.
4. Download files into a staging directory.
5. Verify declared SHA-256 checksums.
6. Atomically rename staging to an installed pack directory.
7. Persist the active pack ID, version, and path.
8. Resolve lesson IDs through the active manifest.
9. Fall back to bundled APK assets when installed content is unavailable.
10. Preserve the last known-good pack after every failed update.

Use a source priority similar to:

```text
Installed validated pack
→ Previously validated installed pack
→ Bundled APK assets
→ Typed failure
```

### 3. Configure the NAS endpoint safely

Do not hard-code the private IP in navigation or composables.

Provide the base URL through one of these mechanisms:

* A dedicated `lan` product flavor using a BuildConfig field.
* A parent-only settings screen.
* A debug/local Gradle property excluded from source control.

The configuration must include scheme, host, port, and base path, for example:

```text
http://10.10.10.33:PORT/maxines-world/
```

Prefer HTTPS with a certificate trusted by the Android device. If HTTP is required, create a narrowly scoped network-security configuration for `10.10.10.33`; do not enable unrestricted cleartext traffic globally.

For development, place the cleartext exception in the `lan` or debug manifest overlay. A production flavor should require HTTPS.

### 4. Replace path guessing with manifest resolution

The current loader derives paths from lesson-ID naming conventions and searches several hard-coded asset locations. 

Replace this with:

```kotlin
catalog.lessonById(lessonId)?.relativePath
```

Requirements:

* Reject absolute paths.
* Reject `..` traversal.
* Validate supported schema and curriculum versions.
* Validate that every catalog entry references an existing file.
* Keep subject-to-lesson selection out of `MaxinesNavGraph`.
* Populate available subjects and lessons from the active catalog.

### 5. Introduce typed diagnostics

Replace nullable loading results with a sealed result model containing at least:

```text
Success
NotFound
NetworkUnavailable
CleartextBlocked
Timeout
HttpError
InvalidManifest
ChecksumMismatch
ParseError
InstallError
```

Log sanitized diagnostic fields:

* Content source.
* Resolved relative path.
* Active pack ID and version.
* HTTP status or exception category.
* Whether bundled fallback was used.

Do not log child information, authentication data, or complete content payloads.

### 6. Rebuild the design system around semantic tokens

Add central tokens for:

* Typography roles.
* Spacing.
* Shapes and radii.
* Elevation.
* Icon and illustration sizes.
* Motion duration and reduced-motion behavior.
* Content maximum widths.
* Subject palettes, including GMRC.
* Minimum touch targets.

Replace screen-level numeric styling with reusable components:

```text
MaxineScreenContainer
VillageHero
DailyQuestCard
SubjectDestinationCard
VillageStatCard
MaxineTopBar
MaxineNavigationBar
MascotIllustration
```

### 7. Correct responsive layout behavior

Use a centered container such as:

```kotlin
Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1440.dp)
            .padding(horizontal = profile.pageMargin)
    ) {
        // Content
    }
}
```

Do not place `fillMaxSize()` on the same content node before `widthIn(max = 1440.dp)`.

Implement the grid described by `design.md`. If it genuinely requires 12 columns, model column spans explicitly rather than calling a three-column row a 12-column grid.

Ensure subject cards have equal visual height at every breakpoint and at 200% font scale.

### 8. Replace placeholder visuals

Replace all mascot and building emoji with approved vector or raster assets:

* Milo.
* Mira.
* Niko.
* Lakan.
* Duke.
* Six subject destinations.
* Village path and decorative elements.
* Empty, loading, success, and error states.

Mark purely decorative assets as excluded from accessibility semantics. Give meaningful illustrations contextual content descriptions.

### 9. Fix interaction and accessibility defects

* Remove empty click handlers or make those elements noninteractive.
* Do not use default `{}` callbacks for required navigation actions.
* Ensure every enabled control has a minimum 48×48dp target.
* Make the visible profile avatar open the profile destination if `design.md` indicates it is interactive.
* Add TalkBack labels for destinations, quest progress, rewards, and lesson state.
* Disable or substantially reduce infinite animation when reduced motion is enabled.
* Verify text contrast and progress-indicator contrast.
* Test at 100%, 150%, and 200% font scale.

### 10. Correct release engineering

* Set `versionName` to the release version.
* Increment `versionCode`.
* Produce a signed release artifact rather than publishing only `app-debug.apk`.
* Display the version from `BuildConfig.VERSION_NAME`.
* Add a CI check that fails when the Git tag and `versionName` differ.
* Generate screenshot-test results and content-integration-test results before publishing.

## Required tests

### Content unit tests

* Resolve every lesson ID through the manifest.
* Reject malformed IDs and unsupported schemas.
* Reject absolute and traversal paths.
* Detect malformed JSON.
* Detect checksum mismatch.
* Preserve the previous pack after failed installation.
* Select installed content before bundled fallback.
* Return typed failures rather than `null`.

### Content integration tests

* Download and activate a valid pack from a mock server.
* Recover from interrupted and partial downloads.
* Handle redirects, 404, 500, timeout, and unreachable host.
* Confirm persistence of the active pack after process restart.
* Confirm operation in airplane mode using bundled content.
* Confirm that an unavailable NAS never prevents app startup.
* Confirm scoped HTTP behavior for the LAN flavor.
* Confirm unrelated cleartext destinations remain blocked.

### UI tests

* Capture approved screenshots for phone and tablet, portrait and landscape.
* Test the Xiaomi Pad 6S Pro landscape dimensions.
* Verify content remains centered and no wider than 1440dp.
* Verify all expected grid breakpoints.
* Verify equal card heights with English and Filipino copy.
* Verify every interactive target is at least 48×48dp.
* Verify all visible enabled controls invoke real actions.
* Verify TalkBack descriptions and traversal order.
* Verify reduced-motion behavior.
* Verify 200% font scale without clipping or hidden controls.

## Runtime checks requiring the local environment

The implementing LLM must not claim the NAS is working until these are verified on an authorized Android device:

```text
1. Confirm the device is on a network that can route to 10.10.10.33.
2. Confirm the NAS service is bound beyond localhost.
3. Confirm the actual port and base path.
4. Confirm firewall and Wi-Fi client-isolation settings.
5. Request the manifest URL from the device.
6. Capture HTTP status, redirect behavior, and MIME type.
7. Confirm case-sensitive filenames match the manifest.
8. Confirm JSON matches the app's LessonManifest schema.
9. Confirm HTTPS certificate trust if HTTPS is used.
10. Capture sanitized Logcat output during download and activation.
```

## Definition of done

The work is complete only when:

* `docs/design.md` is committed and used as the screenshot-test baseline.
* Production artwork replaces emoji and generic-icon placeholders.
* All screens use shared semantic design tokens.
* The documented responsive layout passes screenshot tests.
* No enabled UI control has a no-op callback.
* The NAS manifest and lesson files can be fetched on the target device.
* Valid packs install atomically and survive restart.
* Corrupt or unavailable NAS content falls back safely.
* Content failures produce actionable typed diagnostics.
* The app remains fully usable offline.
* The Git tag, displayed version, and build configuration match.
* A signed release APK is generated and tested.

## Implementation constraints

* Preserve Kotlin, Jetpack Compose, Hilt, Room, Kotlin Serialization, and the modular architecture.
* Do not expose `10.10.10.33` in user-visible child UI.
* Do not weaken network security globally.
* Do not remove bundled-content fallback.
* Do not silently swallow exceptions.
* Do not fabricate curriculum mappings or mark content educator-validated without review.
* Do not declare visual compliance until screenshot baselines pass.


---

## Sources

- [v0.2.0](https://api.github.com/repos/ronrosolada/maxines-world/git/ref/tags/v0.2.0)
- [Apiclient.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/v0.2.0/android/core-network/src/main/java/com/maxinesworld/corenetwork/ApiClient.kt)
- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/v0.2.0/android/core-content/src/main/java/com/maxinesworld/corecontent/LessonLoader.kt)
- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/v0.2.0/android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt)
- [Androidmanifest.xml](https://raw.githubusercontent.com/ronrosolada/maxines-world/v0.2.0/android/app/src/main/AndroidManifest.xml)
- [.kts](https://raw.githubusercontent.com/ronrosolada/maxines-world/v0.2.0/android/app/build.gradle.kts)
- [api.github.com](https://api.github.com/repos/ronrosolada/maxines-world/releases/latest)
- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/v0.2.0/android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/VillageHomeScreen.kt)
- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/v0.2.0/android/core-design-system/src/main/java/com/maxinesworld/coredesignsystem/theme/Theme.kt)
