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

## Design.md improvement specification

This section reviews the authoritative archive file `handoff/docs/design.md` as both a UX specification and an engineering contract. It should be used to revise that file before asking another LLM to implement the interface.

### What the design package already does well

The document establishes a strong learning-first principle, calm feedback, separate child and parent experiences, subject-specific worlds, a coherent palette, child-sized controls, reduced-motion requirements, localization guidance, and a useful visual-reference board.

The archive also provides unusually useful supporting material:

* `handoff/assets/graphics/character-identity-sheet.png`
* `handoff/assets/graphics/child-village-home.png`
* `handoff/assets/graphics/learning-progression-map.png`
* `handoff/assets/graphics/math-lesson-equal-groups.png`
* `handoff/assets/graphics/english-lesson-main-idea.png`
* `handoff/assets/graphics/science-lesson-plant-growth.png`
* `handoff/assets/graphics/parent-dashboard.png`
* `handoff/assets/characters/` with Milo, Mira, Niko, Lakan, and Duke in PNG and SVG form
* `handoff/assets/icon/` with launcher foreground, background, monochrome, 48px, and 512px outputs
* `handoff/maxines-world-screens.html` with native-screen references

These files are useful references, but the package currently leaves too much interpretation to the implementing agent.

### Critical problems to correct in design.md

#### 1. Source-of-truth and precedence are unclear

The most important implementation decisions are scattered throughout the document. Section 24 changes the typography decision made in Section 8, but a weak agent may implement the earlier generic-font direction and never reach the superseding instruction.

Add a mandatory section at the beginning:

```markdown
## 0. Normative status and precedence

This file is the normative design source of truth.

MUST means required for acceptance.
MUST NOT means prohibited.
SHOULD means preferred unless a documented exception exists.
MAY means optional.

When sections conflict, the following precedence applies:
1. Section 0 and the acceptance criteria
2. Explicit v2 decisions
3. Screen contracts
4. Component contracts
5. Token tables
6. Concept images

Concept images communicate mood and hierarchy only. They are not pixel-perfect specifications and must not be embedded as complete application screens.

Baloo 2 and Nunito are the final typography decision and replace earlier generic-font language.
```

Rename Section 24 from an appended iteration to a normative section integrated into the relevant typography, component, screen, and asset sections. Do not leave superseding requirements at the end of the file.

#### 2. The archive and repository use inconsistent names

The document refers to `Maxines World Screens.dc.html`, while the archive contains `handoff/maxines-world-screens.html`. Correct the reference and define exact repository destinations.

Add this table:

| Source file | Repository destination | Status |
|---|---|---|
| `handoff/docs/design.md` | `docs/design.md` | Normative specification |
| `handoff/maxines-world-screens.html` | `docs/design/maxines-world-screens.html` | Native layout reference |
| `handoff/assets/graphics/*` | `docs/design/reference/` | Concept reference only |
| `handoff/assets/characters/*.svg` | `android/core-design-system/src/main/res/drawable/` after review | Candidate production assets |
| `handoff/assets/characters/*.png` | `docs/design/previews/` or approved raster fallback | Preview or fallback |
| `handoff/assets/icon/*` | Android launcher resources after mask validation | Candidate production assets |

Do not duplicate the same character file in `app`, `feature-child-home`, and game modules. Establish one canonical resource owner.

#### 3. The subject model is contradictory

The design defines five subject worlds, while the current application includes English, Mathematics, Science, Filipino, Makabansa, and GMRC. Current code also uses both History and Makabansa naming. 

Add one canonical table and require every implementation to derive UI labels and routing from it:

| Stable ID | Curriculum label | Village location | Guide | Primary token | Included in first release |
|---|---|---|---|---|---|
| `english` | English | Story Tree | Mira | `subject_english` | Yes |
| `filipino` | Filipino | Bahay ng Kuwento | Mira | `subject_filipino` | Yes |
| `mathematics` | Mathematics | Number Market | Milo | `subject_mathematics` | Yes |
| `science` | Science | Discovery Lab | Niko | `subject_science` | Yes |
| `makabansa` | Makabansa | Heritage Harbor | Lakan | `subject_makabansa` | Yes |
| `gmrc` | GMRC | Kindness Corner | Duke | `subject_gmrc` | Yes |

Add explicit rules:

* `history`, `philippine-history`, and `araling-panlipunan` are legacy or content-domain aliases. They must be mapped to the canonical product ID in one domain layer.
* UI composables must never infer routes from displayed labels.
* GMRC must receive its own palette, subject-world definition, icon, location art, and lesson-screen examples.
* Content unavailable for a subject must produce an unavailable state with an explanation, not a broken navigation route.

#### 4. The home screen has no enforceable information hierarchy

The concept art contains XP, streaks, rewards, a Daily Quest, several destinations, navigation, characters, and decorative animals. Without stricter rules, all of these can compete with the learning action.

Add a `Home information hierarchy` section:

```text
Priority 1: Resume the current lesson or start the recommended lesson.
Priority 2: Choose a subject destination.
Priority 3: See today's learning goal and progress.
Priority 4: View rewards and collectibles.
Priority 5: Access utilities and Parent Gate.
```

Rules:

* Exactly one primary learning action may appear above the fold.
* Daily Challenge and Daily Quest must not appear as separate competing cards. Use one canonical daily-learning model.
* XP, streaks, stars, coins, badges, and decorative animals must remain visually subordinate to the learning action.
* Parent Gate must be separated from ordinary child navigation and protected before any parent content is shown.
* A first-time learner sees `Start today's adventure`; a returning learner sees `Continue [lesson title]`.
* An offline learner with cached content sees the same learning action plus a small offline indicator.
* A learner without available content sees a friendly explanation and a retry action; zero progress must not be used to represent an error.

#### 5. Required user flows are missing

Add deterministic flow definitions. Each flow must list entry condition, visible state, primary action, secondary action, persistence behavior, error behavior, and exit destination.

Required child flows:

1. First launch and profile setup.
2. Returning child launch.
3. Resume interrupted lesson.
4. Start a recommended lesson.
5. Enter a subject from the village.
6. Open a locked or unavailable subject.
7. Complete an explanation step.
8. Answer correctly.
9. Answer incorrectly once.
10. Answer incorrectly twice.
11. Receive a worked example after repeated difficulty.
12. Request a hint.
13. Replay narration.
14. Pause a lesson.
15. Exit a lesson with unsaved interaction state.
16. Resume after process death.
17. Complete a lesson.
18. Receive a reward.
19. Use the app offline.
20. Recover from corrupt or unavailable content.
21. Enter and cancel Parent Gate.
22. Return from the parent experience to the child home.

For every lesson, use this mandatory sequence unless a documented subject exception applies:

```text
Orientation
→ objective
→ short explanation or model
→ guided practice
→ independent practice
→ review
→ completion and next action
```

#### 6. Screen contracts are too conceptual

Create one normative contract per screen. Each contract must include:

* Purpose.
* Entry conditions.
* Data dependencies.
* Content hierarchy.
* Component tree.
* Primary and secondary actions.
* Loading, empty, offline, stale, partial, error, success, and disabled states.
* Phone layout.
* Tablet layout.
* Landscape behavior.
* 200% font-scale behavior.
* TalkBack reading order.
* Reduced-motion behavior.
* Back behavior.
* Persistence checkpoints.
* Screenshot-test names.

Use the following template:

```markdown
## Screen contract: Village Home

Purpose: Help the child resume learning or choose a subject.

Primary action: Continue the current lesson. If no lesson exists, start the recommended lesson.

Data:
* Child display name
* Resume target
* Subject availability and progress
* Daily goal
* Reward summary
* Sync state

State order:
1. Loading cached state
2. Ready with resume target
3. Ready without resume target
4. First use
5. Offline with cached lessons
6. Offline without cached lessons
7. Partial progress data
8. Recoverable error

Compact layout:
* Top app bar
* Resume card
* Two-column subject grid
* Daily goal
* Reward summary
* Bottom navigation

Expanded layout:
* Centered content with documented maximum width
* Resume and daily goal in a 7/5 or 8/4 grid split
* Six subject destinations in documented spans
* Navigation rail or bottom navigation as specified

TalkBack order:
1. Screen heading
2. Resume action
3. Daily goal summary
4. Subject destinations in reading order
5. Reward summary
6. Utility navigation
```

Write equivalent contracts for Daily Trail, Subject Overview, Lesson Player, Reward Moment, Achievements, Backpack, Profile, Parent Gate, and Parent Dashboard.

#### 7. Responsive guidance is not deterministic

The phrase `12-column conceptual grid` is insufficient for implementation. Define exact breakpoints and calculations in one place.

Recommended defaults:

| Profile | Width | Outer margin | Gutter | Columns | Maximum content width |
|---|---:|---:|---:|---:|---:|
| Compact | `<600dp` | 16dp | 12dp | 4 | Full width |
| Medium | `600–839dp` | 24dp | 16dp | 8 | Full width |
| Expanded | `840–1199dp` | 32dp | 20dp | 12 | 1200dp |
| Large tablet | `≥1200dp` | 32dp | 24dp | 12 | 1440dp |

Add the formula:

```text
availableWidth = min(windowWidth - leftInset - rightInset, maximumContentWidth)
columnWidth = (availableWidth - outerMargin * 2 - gutter * (columnCount - 1)) / columnCount
```

Required tests must cover 599, 600, 839, 840, 1199, and 1200dp exactly. The current repository defines breakpoints but individual screens still use ad hoc conditions such as `maxWidth > 600.dp` and fixed dimensions. 

Prohibit:

* Feature-specific breakpoints.
* Hidden horizontal scrolling as the only way to reach a primary destination.
* Fixed-height containers for essential instructional text.
* Shrinking text to make a layout fit.
* Placing `fillMaxSize()` before a maximum-width constraint on the same content node.

#### 8. Typography must be rewritten as a final mapping

Move the Baloo 2 and Nunito decision into the main typography section.

Use this exact assignment unless the design owner changes it:

| Role | Family | Weight | Compact size | Expanded size | Line height |
|---|---|---:|---:|---:|---:|
| Display | Baloo 2 | 700–800 | 36sp | 44sp | 1.15× |
| Screen title | Baloo 2 | 700–800 | 30sp | 34sp | 1.2× |
| Section heading | Baloo 2 | 700 | 24sp | 28sp | 1.25× |
| Lesson question | Baloo 2 | 700 | 26sp | 32sp | 1.25× |
| Button label | Baloo 2 | 700 | 18sp | 20sp | 1.2× |
| Child body | Nunito | 600–700 | 18sp | 22sp | 1.4× |
| Child caption | Nunito | 600–700 | 16sp | 18sp | 1.35× |
| Parent title | Baloo 2 | 700 | 24sp | 30sp | 1.2× |
| Parent body | Nunito | 600 | 14sp | 18sp | 1.4× |
| Parent data | Nunito | 700–800 | 14sp | 18sp | Tabular numerals |

Rules:

* Child-facing actionable text must not be smaller than 18sp.
* Non-actionable child metadata may use 16sp only when contrast and scaling tests pass.
* Essential text must wrap rather than clip or ellipsize.
* Test font scales 1.0, 1.3, 1.5, and 2.0.
* Release builds must not silently fall back to `FontFamily.Default`.
* Theme typography must be immutable. The current mutable global font variables should be replaced with resource-backed immutable families. 

#### 9. Tokens need executable names and ownership

Add a single token registry. Each row must contain token name, exact value, semantic use, prohibited use, Kotlin symbol, and owning file.

Token families:

* Brand colors.
* Subject colors, including GMRC and Makabansa.
* Content and status colors.
* Typography.
* Spacing.
* Radius.
* Elevation.
* Opacity.
* Icon sizes.
* Illustration sizes.
* Control heights.
* Motion duration and easing.
* Layout breakpoints, margins, gutters, spans, and maximum widths.

Add this rule:

```text
No feature or game module may declare a raw production Color, hex value, text size, radius, elevation, alpha, breakpoint, or animation duration when an equivalent design token exists.
```

Games currently redeclare core palette colors and use local spacing and typography values. 

#### 10. Component contracts need valid state models

Every component section must contain:

* Public Kotlin signature.
* Required slots.
* Allowed variants.
* Exact tokens.
* State priority.
* Interaction behavior.
* Semantics.
* Reduced-motion behavior.
* Compact and expanded previews.
* Unit and screenshot tests.

Use explicit state types instead of unrelated booleans.

Example:

```kotlin
enum class AnswerCardState {
    Idle,
    Selected,
    Correct,
    Incorrect,
    Disabled
}

@Composable
fun MaxinesAnswerCard(
    state: AnswerCardState,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    feedbackLocked: Boolean = false,
    leadingVisual: (@Composable () -> Unit)? = null
)
```

The current `selected: Boolean` and nullable `correct: Boolean?` contract permits ambiguous combinations. 

Required state priority:

```text
Disabled
→ Loading
→ Correct or Incorrect
→ Pressed
→ Focused
→ Selected
→ Idle
```

Define complete contracts for:

* `MaxinesPrimaryButton`
* `MaxinesSecondaryButton`
* `MaxinesAnswerCard`
* `MaxinesQuestCard`
* `MaxinesSubjectDestination`
* `MaxinesProgressBar`
* `MaxinesRewardChip`
* `MaxinesGuideBubble`
* `MaxinesAudioButton`
* `MaxinesHintButton`
* `MaxinesParentMetricCard`
* `MaxinesSkillStatusChip`
* `MaxinesLessonScaffold`
* `MaxinesErrorState`
* `MaxinesOfflineBanner`
* `MaxinesLoadingState`

A weak LLM must not be allowed to create local look-alike versions inside feature modules.

#### 11. Feedback must specify a full retry algorithm

Replace prose-only feedback with an implementable state machine:

```text
Attempt 1 incorrect:
* Preserve the selected answer.
* Show a calm, specific clue.
* Keep all reasonable options.
* Allow another attempt.

Attempt 2 incorrect:
* Demonstrate one relevant step.
* Remove only clearly irrelevant distractors if pedagogically valid.
* Preserve earned progress.
* Allow another attempt.

Attempt 3 incorrect:
* Show a worked example.
* Ask one similar, easier transfer item.
* Mark the skill for review.
* Do not reduce stars already earned.

Correct after support:
* Confirm the exact reasoning.
* Continue after the learner can perceive the result.
```

Prohibit generic feedback such as `Wrong`, `Try Next`, or automatic passing. Do not auto-pass sorting or sentence-building activities.

#### 12. Accessibility needs screen-level contracts

The existing checklist is directionally correct but not testable enough.

Add requirements:

* Every screen has an explicit TalkBack order.
* Each subject destination is exposed as one semantic node containing subject name, world name, progress or lock state, and action.
* Decorative animals and duplicated labels are hidden from accessibility focus.
* Progress indicators announce current value, maximum, and meaning.
* Correct, incorrect, locked, selected, and mastered states use an icon or label as well as color.
* Keyboard, D-pad, Switch Access, and external keyboard activation work for every action.
* Drag-and-drop always has tap-to-select or move-up/move-down alternatives.
* No essential activity is timed.
* All narration has a transcript.
* All sound cues have equivalent visual and textual feedback.
* Minimum text contrast is 4.5:1 for normal text; meaningful UI components and large text meet 3:1.
* Touch targets are at least 48×48dp; primary child actions default to 56–64dp.
* Focus indicators must remain visible over illustrated backgrounds.

#### 13. Motion behavior needs one policy abstraction

Specify a single `MotionPolicy` controlled by the system animation scale and the parent setting.

Normal mode:

* Button response: 80–160ms.
* Card transition: 180–280ms.
* Screen transition: 250–400ms.
* Character reaction: 400–900ms.
* Celebration: 1–3 seconds and skippable.

Reduced-motion mode:

* No infinite bobbing.
* No parallax.
* No automatic path travel.
* No looping confetti.
* Use static character poses.
* Use immediate state changes or opacity transitions no longer than 150ms.
* Retain every instructional and status cue.

The current home animates every building character indefinitely. 

#### 14. Define graphical-asset production as a manifest

Add `assets/asset-manifest.json` as the machine-readable source of truth.

Required fields:

```json
{
  "id": "character.milo.guide.thinking",
  "file": "characters/milo/milo--guide--thinking.svg",
  "category": "character",
  "status": "approved",
  "format": "svg",
  "width": 1024,
  "height": 1024,
  "states": ["thinking"],
  "decorative": false,
  "contentDescriptionKey": "character_milo_thinking_description",
  "longDescriptionKey": null,
  "localeScope": "global",
  "culturalReview": "not-required",
  "license": "project-owned",
  "fallbackId": "character.milo.guide.reduced-motion",
  "sha256": "...",
  "reusedBy": ["village-home", "math-lesson"]
}
```

The manifest must also include owner, source master, version, view box, density behavior, safe area, pivot or baseline for animation, provenance, and review date.

#### 15. Prioritized graphical-asset plan

##### P0: Finish before further screen polish

| Asset family | Required output |
|---|---|
| Logo | Horizontal, stacked, mark, light, dark, monochrome, watermark |
| Launcher icon | Foreground, background, monochrome, mask previews, 48px legibility preview |
| Milo, Mira, Niko, Lakan, Duke | Fifteen normalized poses per guide |
| Village | Seven aligned layers plus reduced-motion static composition |
| Subject locations | Wide and card art for six canonical subjects |
| Location states | Locked, available, active, complete, mastered overlays |
| Semantic icons | Home, back, close, play, pause, replay, audio, hint, settings, parent, lock, check, retry, next, six subjects |
| Feedback | Correct, retry, hint, completion, badge earned, loading, with static fallbacks |
| Reward tokens | Star, paw coin, badge frame, locked badge overlay, new badge overlay |

Required guide poses:

```text
neutral
greeting
point-left
point-right
thinking
explaining
celebrating
encouraging
surprised
gentle-correction
reading
listening
walking
idle
reduced-motion
```

All five guide sets must share:

* A 1024×1024 source view box.
* A common ground line.
* Consistent apparent scale.
* Consistent transparent padding.
* Stable accessory placement.
* A documented default facing direction.
* Safe mirroring rules.

Do not count game-specific Milo running or jumping frames as replacements for shared lesson-guide poses.

##### P1: Improve engagement after the P0 system is stable

* Create day, overcast, and evening color treatments for village layers without changing interaction positions.
* Create subtle ambient village loops: clouds, leaves, water, chimney smoke, and one focal character. Supply static fallbacks.
* Add one small interactive micro-story per subject location, such as a book opening, market counter moving, plant sprouting, map unfolding, or kindness-heart lighting up.
* Create a reusable badge system with one frame plus species illustrations and state overlays rather than a separate flattened badge for every state.
* Layer Cat Cafe into back wall, counter, interaction plane, and foreground.
* Layer Parkour into sky, distant scenery, track, obstacles, and foreground.
* Deduplicate stars, paw tokens, companion animals, and Milo poses across game modules.
* Audit all lesson SVGs for semantic uniqueness; do not accept 100 generic or repeated diagrams merely because the filenames differ.

##### P2: Expand the world after usability validation

* Complete Ollie, Shelly, Tilly, Poppy, and Flynn using the same fifteen-pose contract.
* Add seasonal village decorations that never alter button locations or obscure labels.
* Add culturally reviewed regional environmental details based on the lesson location.
* Add optional collectible room or backpack illustrations.
* Add mastered-state visual restoration for village locations.

#### 16. Asset format and size rules

Recommended production defaults:

| Asset | Master | Runtime format | Default dimensions |
|---|---|---|---|
| Character static pose | SVG | VectorDrawable or WebP fallback | 1024×1024 view box |
| Character animation | Rive | `.riv` plus static fallback | Common rig and canvas |
| UI icon | SVG | VectorDrawable | 24×24 view box |
| Village layer | Layered source | WebP | 2048×1152 |
| Subject location wide | Layered source | WebP | 1600×900 |
| Subject location card | Derived source | WebP | 800×600 |
| Lesson diagram | SVG | VectorDrawable or safe SVG renderer | 1200×900 view box |
| Badge | SVG or layered source | WebP or vector | 256×256 |
| Small UI animation | Rive or Lottie | `.riv` or `.json` plus static fallback | Under documented budget |

Initial-screen budget defaults:

* Total compressed graphical payload needed for first render: no more than 1.5MB.
* Individual background: preferably below 400KB.
* Static transparent object: preferably below 120KB.
* Rive or Lottie file: preferably below 250KB.
* SVG: preferably below 100KB.

An exception must include reason, owner, measured impact, and expiry or follow-up issue.

#### 17. Asset naming and reuse rules

Use lowercase ASCII kebab-case:

```text
characters/milo/milo--guide--thinking.svg
characters/milo/milo--guide--reduced-motion.svg
locations/english/story-tree--wide--base.webp
locations/english/story-tree--card--base.webp
locations/shared/location--overlay--locked.svg
feedback/feedback--lesson-complete--enter.riv
rewards/reward--paw-coin--default.svg
```

Prohibit:

* Spaces.
* Uppercase letters.
* `final`, `new`, `latest`, or revision numbers in filenames.
* Duplicate files under different names.
* State inferred only from a filename.
* Text, numerals, answer labels, instructions, or buttons embedded in art.
* Feature-local copies of canonical assets.
* Mirroring a pose when accessories, maps, writing, gestures, or cultural meaning would become incorrect.

#### 18. Cultural and historical review gate

Add a required manifest field with these values:

```text
not-required
pending
approved
rejected
```

Require named human review and provenance for:

* Sacred or ceremonial objects.
* Indigenous or ethnic motifs.
* Traditional clothing.
* Regional textiles.
* Official seals and heraldry.
* Historical maps.
* Religious imagery.
* Baybayin or Baybayin-like marks.
* Flags used beyond factual display.
* Reconstructions of historical places or people.

A `pending` or `rejected` asset must not enter the release manifest. Do not use a generic pan-Filipino decorative style or official cultural symbols as surface patterns.

#### 19. Documentation for a less capable LLM

Add a `Do this in order` section near the beginning of `design.md`:

```text
1. Read the entire design.md before editing code.
2. List all affected screens, states, components, tokens, and assets.
3. Implement or correct tokens first.
4. Implement shared components second.
5. Add all component states and previews.
6. Implement one screen contract at a time.
7. Add loading, empty, offline, partial, error, success, and reduced-motion states.
8. Add accessibility semantics and localized strings.
9. Add unit, UI, screenshot, and accessibility tests.
10. Compare against the native reference and acceptance criteria.
11. Run prohibited-pattern scans.
12. Do not start the next screen until the current screen passes.
```

Add a mandatory implementation report format:

```markdown
## Implemented
* Files changed
* Components added or changed
* Screen states completed
* Assets added or reused

## Tests
* Unit tests
* Compose UI tests
* Screenshot tests
* Accessibility checks
* Device and font-scale matrix

## Deviations
* Requirement
* Reason
* User-visible impact
* Follow-up issue

## Not implemented
* Explicit remaining work
```

#### 20. Prohibited shortcuts

Place this list prominently in `design.md`:

* Do not ship emoji as final character, subject, reward, or feedback artwork.
* Do not flatten concept images into interactive screens.
* Do not embed copy or controls in artwork.
* Do not invent missing token values.
* Do not add raw colors and dimensions in feature code.
* Do not create component look-alikes outside the design-system module.
* Do not use booleans to represent mutually exclusive multi-state components.
* Do not silently convert loading or errors into empty or zero-progress states.
* Do not auto-pass unfinished activity engines.
* Do not run infinite animation under reduced motion.
* Do not reduce font size to solve overflow.
* Do not use hidden horizontal scrolling for essential navigation.
* Do not expose parent content before the adult gate succeeds.
* Do not punish mistakes with lost rewards, alarms, shaking, ridicule, or forced restart.
* Do not claim completion when a required state is represented by a TODO or no-op callback.
* Do not treat the concept references as culturally or historically verified production art.

### Deterministic implementation sequence

#### Phase 1: Make the specification executable

1. Commit the archive design to `docs/design.md`.
2. Integrate the v2 typography and native-screen decisions into the main sections.
3. Add normative language and precedence.
4. Add canonical subject and route tables.
5. Add screen contracts.
6. Add token and component registries.
7. Add the asset manifest schema.
8. Resolve every contradiction before UI work begins.

Exit criteria:

* No unresolved naming or precedence ambiguity remains.
* Every screen and component has objective acceptance criteria.
* Every required asset has a manifest entry or explicit `planned` status.

#### Phase 2: Stabilize the design system

1. Replace mutable typography globals with immutable Baloo 2 and Nunito resource families.
2. Implement semantic token groups.
3. Implement the shared responsive grid and content container.
4. Replace boolean component-state combinations with exhaustive state types.
5. Complete shared components in document order.
6. Add all-state previews for compact and expanded layouts.
7. Add reduced-motion and semantics behavior to every component.

Exit criteria:

* Feature modules require no raw production styling.
* Component tests pass for every documented state.
* Font scaling and breakpoint tests pass.

#### Phase 3: Rebuild the learning home

1. Implement the Home screen contract.
2. Make Resume learning the primary action.
3. Replace the horizontally scrolling village as the only navigation mechanism.
4. Retain village art as an engaging background or visible destination grid.
5. Combine duplicate daily-learning concepts.
6. Move Parent Gate to a separated gated utility.
7. Implement all data states.
8. Add TalkBack and reduced-motion behavior.

Exit criteria:

* A child can resume learning within five seconds.
* Every subject is reachable without undisclosed scrolling.
* No reward element competes with the primary learning action.

#### Phase 4: Standardize lessons

1. Implement `MaxinesLessonScaffold`.
2. Implement Math, Reading, Science, Makabansa, Filipino, and GMRC templates.
3. Implement the retry state machine.
4. Implement checkpoint persistence and recovery.
5. Implement hint, narration, transcript, pause, and exit behavior.
6. Remove placeholder and auto-pass engines.

Exit criteria:

* Every subject passes introduction, guided practice, independent practice, hint, repeated-error, completion, exit, and resume tests.

#### Phase 5: Produce and integrate assets

1. Inventory current assets by SHA-256.
2. Mark each asset `keep`, `replace`, `deduplicate`, or `reference-only`.
3. Complete the P0 manifest.
4. Produce normalized guide poses.
5. Produce six subject locations and state overlays.
6. Produce the shared icon, reward, and feedback sets.
7. Add static fallbacks for animation.
8. Deduplicate game and feature assets.
9. Run cultural and factual review.

Exit criteria:

* No missing or orphan production asset exists.
* No unapproved duplicate remains.
* No production art contains essential text.
* Every meaningful asset has accessibility metadata.

#### Phase 6: Validate and release

1. Run unit tests.
2. Run Compose UI tests.
3. Run screenshot tests.
4. Run TalkBack and accessibility-scanner checks.
5. Run font-scale and localization tests.
6. Run breakpoint and orientation tests.
7. Run reduced-motion tests.
8. Run asset-schema and budget validation.
9. Conduct a moderated usability test with Grade 3 learners.
10. Publish only after release identity and signing checks pass.

### Expanded acceptance matrix

The implementation is not complete until all of these pass:

#### Home and navigation

* A returning learner can activate Resume learning within five seconds without scrolling.
* A first-time learner sees one Start learning action in the same location.
* Every navigation destination has a label and correct selected state.
* Parent content cannot open without successful adult authentication.
* Back behavior is deterministic from every screen.
* No primary subject depends on hidden horizontal scrolling.

#### Layout

* Tests pass at 360×640dp, 412×915dp, 600×960dp, 800×1280dp, and the target Xiaomi tablet dimensions.
* Tests pass in portrait, landscape, split screen, and with system insets.
* Boundary tests pass at 599, 600, 839, 840, 1199, and 1200dp.
* Essential controls remain visible at 200% font scale.
* Essential instructional text does not clip or become ellipsized.

#### Components

* Every component has enabled, disabled, pressed, focused, loading where relevant, and semantic states.
* Correct and incorrect states differ by icon or label as well as color.
* Every interactive semantic node is at least 48×48dp.
* Primary child controls are at least 56dp high.
* Invalid state combinations cannot be constructed.

#### Accessibility

* TalkBack reads each subject destination once with name, state, progress, and action.
* Decorative animals are skipped.
* Focus order matches visual order.
* Drag activities have a non-drag alternative.
* The interface remains understandable in grayscale and when muted.
* Reduced motion eliminates infinite and nonessential movement.

#### Learning and recovery

* First, second, and third incorrect attempts produce the documented support tiers.
* Work and lesson position survive Back, rotation, process death, and offline transitions.
* Network or content failure never erases valid cached progress.
* Missing content produces a typed, actionable state rather than a generic null or zero.
* No activity engine awards success without evaluating the learner's action.

#### Localization

* Every visible string comes from Android resources.
* English, Filipino, and pseudo-localized builds pass.
* Layout tolerates at least 40% text expansion.
* Plurals, dates, numbers, Philippine currency, and metric units are locale-aware.
* No production illustration contains localized copy.

#### Assets

* Every shipped asset exists exactly once in the production manifest.
* All filenames follow the naming contract.
* The launcher icon passes circle, squircle, rounded-square, and teardrop masks.
* Each primary guide has all fifteen poses and a static reduced-motion pose.
* Each subject location has wide, card, and five state-overlay variants.
* Every animation has a static fallback.
* Lesson SVGs contain no scripts, external URLs, embedded controls, or essential text.
* Cultural assets have documented approval and provenance.

#### Usability

Conduct moderated tests with at least five Grade 3 learners. At least 80% should complete each task without adult prompting:

1. Resume the current lesson.
2. Find a named subject.
3. Request a hint.
4. Recover after one incorrect answer.
5. Pause and resume.
6. Return to the village.

Record confusion, mis-taps, reading difficulty, requests for help, and distraction caused by art or animation. Revise the design when repeated confusion appears, even if the implementation technically matches the reference.

## Prompt for the implementing LLM

Use this prompt after the revised `docs/design.md` and asset manifest are committed:

```text
You are implementing Maxine's World for an eight-year-old Grade 3 learner.

Read docs/design.md completely before changing code. Treat MUST and MUST NOT statements as acceptance requirements. Concept images communicate mood and hierarchy only; never embed a complete concept image as a screen.

Work in this order:
1. Produce a gap list mapping every requested change to a design.md requirement.
2. Update immutable tokens and typography.
3. Update shared design-system components and exhaustive state models.
4. Add previews and tests for every component state.
5. Implement one screen contract at a time.
6. Implement loading, empty, offline, partial, error, success, disabled, and reduced-motion states.
7. Add localized resources and accessibility semantics.
8. Add unit, UI, screenshot, accessibility, breakpoint, and font-scale tests.
9. Run prohibited-pattern scans.
10. Stop and report any missing or contradictory requirement instead of inventing a value.

Do not:
* Add raw production colors, dimensions, font sizes, radii, elevations, alphas, breakpoints, or motion durations outside token-definition files.
* Create local versions of shared design-system components.
* Ship emoji as final artwork.
* Embed text or controls in illustrations.
* Turn errors into zero or empty progress.
* Auto-pass an unfinished activity.
* expose parent content without the adult gate.
* claim completion while callbacks are empty, states are missing, tests fail, or TODOs replace behavior.

For every completed screen, report:
* Files changed
* Requirements implemented
* States implemented
* Assets used
* Accessibility behavior
* Tests added
* Test results
* Deviations
* Remaining work

Do not begin the next screen until the current screen passes its acceptance criteria.
```

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
