# Maxine’s World Village Home UI Overhaul

## Controlling implementation contract for DeepSeek V4 Pro

This document is an executable front-end specification. Follow it literally. Do not replace it with a shorter interpretation, a generic Material 3 dashboard, or a visually simpler design merely because the simpler design compiles.

The implementation is accepted only when it both works and visibly resembles a cohesive illustrated learning game. Compilation is necessary but not sufficient.

## 1. Repository and scope

Repository:

```text
https://github.com/ronrosolada/maxines-world
```

The repository describes an Android-first, offline-first Grade 3 learning app using Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, WorkManager, and modular feature modules. The Village Home belongs in `feature-child-home`; shared tokens and reusable components belong in `core-design-system`.

This milestone covers:

* Village Home visual redesign.
* Six-subject parity.
* Responsive phone and tablet behavior.
* Native gamification HUD.
* Accessibility and interaction states.
* Correct use of the supplied scene and vector assets.
* Screenshot and automated verification.

Do not refactor lesson logic, persistence, networking, curriculum data, parent reporting, or unrelated navigation unless a minimal change is required to connect a real callback.

## 2. Authority order

When two sources differ, use this order:

1. This document.
2. `LANDMARK_LAYOUT.json`.
3. `source-design/design.md`.
4. `source-design/village-home-build-spec.md`.
5. `references/original_target_board.png` for mood, richness, and delight only.
6. Existing repository code.
7. Your own assumptions.

The latest screenshot in `references/current_build.png` is a defect reference, not a target.

Do not ask the screenshot to define the final appearance. It shows what must be corrected.

## 3. Product outcome

Within three seconds, a child should understand:

* This is a colorful learning village.
* There are six equally important subject destinations.
* One destination or quest is recommended today.
* Their level, progress, streak, stars, and paw coins matter.
* Tapping a landmark starts or continues learning.
* Locked content explains how it will open.

The screen must feel like a navigable illustrated game map with lightweight native controls. It must not feel like six cards placed on a wallpaper.

## 4. Defects in the current build

The current screenshot has improved information coverage but is not the desired final UX.

Correct every item below:

1. The background is richly painted, but the six landmark overlays are tiny, flat, and stylistically unrelated.
2. Every destination is labeled twice: a colored badge above and a large white card below.
3. The duplicate labels consume scene space and create dashboard-like visual noise.
4. Characters look pasted onto doorways and do not share the background’s rendering style.
5. The large Daily Quest card blocks the central path and competes with bottom navigation.
6. Six white destination cards, top HUD, quest, and bottom navigation all demand equal attention.
7. The top bar is a continuous adult-looking white strip rather than a light game HUD.
8. Reward counters are visually small and show unrewarding zero balances without context.
9. Subject identity is carried mostly by label color rather than environmental storytelling.
10. The world appears static; the recommended location lacks a controlled focal treatment.
11. The landmark art gives insufficient visual reason to explore each subject.
12. The current composition can regress into clipping or overlap at alternate sizes.

Do not solve these issues by changing only colors, typography, shadows, or corner radii. Replace the composition.

## 5. Asset policy

### 5.1 Required runtime scene

Use:

```text
assets/background/village_home_six_landmarks_master.png
```

Copy it to:

```text
android/feature-child-home/src/main/res/drawable-nodpi/village_home_six_landmarks_master.png
```

The supplied master is:

```text
1672 × 941 px
Opaque RGB PNG
16:9-compatible scene plate
Exactly six integrated landmarks
No baked labels, UI, progress, or characters
```

This single image replaces:

* The current painted empty-pad background.
* All flat `location_*.png` village overlays.
* Any Compose Canvas mountains, paths, houses, trees, or ground geometry.

Do not layer legacy flat buildings on top of this scene. The six landmarks are already integrated.

Do not stretch the image. Use `ContentScale.Crop` and preserve its aspect ratio.

Do not apply a heavy blur, dark wash, or cream panel over the whole image.

### 5.2 Character policy

The package includes:

```text
assets/characters/milo.png
assets/characters/mira.png
assets/characters/niko.png
assets/characters/lakan.png
assets/characters/duke.png
```

These legacy vector-style characters do not match the painted scene closely enough to stand beside the landmarks at large size.

For this milestone:

* Use one character only inside the compact Daily Quest card, a circular guide portrait, or a small speech bubble.
* Use the child avatar in the profile HUD.
* Do not position all five guides as full-body scene sprites.
* Do not place characters over doors.
* Do not place colored circles behind full-body characters.
* Do not duplicate Mira for English and Filipino simultaneously on the scene.
* If a guide is shown, cap it at a visually secondary size and give it a contained native surface.

A later art sprint may replace these with painterly character rigs. Do not block this milestone on that future work.

### 5.3 Vector symbols

Copy `assets/vectors/*.xml` into an Android drawable resource folder.

These are native supporting symbols for:

* Quest.
* Streak.
* Stars.
* Paw coins.
* Profile.
* Achievements.
* Backpack.
* Parent Gate.
* English.
* Filipino.
* Mathematics.
* Science.
* Philippine History.
* GMRC.

Use them inside native Compose components. Apply semantic tint through `Icon`, not by editing path data per screen.

Do not use emoji as icons.

### 5.4 App icon

The package includes adaptive-icon source PNGs in `assets/app-icon/`. Do not change launcher branding during the Village Home milestone unless the repository is still missing these assets.

### 5.5 Reference-only files

Never ship these as UI:

```text
references/current_build.png
references/previous_build.png
references/original_target_board.png
references/legacy_flat_building_lineup.png
references/legacy_character_lineup.png
LAYOUT_MAP.svg
```

The original target board contains baked text and controls. It is a mood reference only.

## 6. Six-subject parity

Render exactly these six destinations:

| ID | Destination | Subject | Color | Icon |
| --- | --- | --- | --- | --- |
| `english` | Story Tree | English | `#7653B5` | `ic_book` |
| `filipino` | Bahay ng Kuwento | Filipino | `#F47C6B` | `ic_story` |
| `mathematics` | Number Market | Mathematics | `#3C9DDB` | `ic_math` |
| `science` | Discovery Lab | Science | `#66A83E` | `ic_flask` |
| `history` | Heritage Harbor | Philippine History | `#B87916` | `ic_anchor` |
| `gmrc` | Kindness Corner | GMRC | `#087F83` | `ic_heart` |

Every destination receives:

* The same permanent hit-zone size.
* The same label structure.
* The same state model.
* Comparable contrast.
* Comparable opportunity to become Today’s Focus.
* Comparable quest and reward representation over time.
* A correct route or an honest disabled state.

English, Mathematics, and Science must not receive permanent size or placement advantages.

The scene art already gives all six equal detail. Do not undo that parity with native UI.

## 7. Expanded landscape composition

Use the illustrated scene when all conditions are true:

```text
width >= 840dp
landscape orientation
usable height >= 600dp
```

Use a root `BoxWithConstraints` or responsive profile abstraction.

Recommended structure:

```kotlin
@Composable
fun VillageHomeScreen(
    state: VillageHomeUiState,
    onDestinationClick: (SubjectId) -> Unit,
    onQuestClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onBackpackClick: () -> Unit,
    onParentGateClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (isExpandedLandscape()) {
            ExpandedVillageHome(...)
        } else {
            CompactVillageHome(...)
        }
    }
}
```

Expanded layer order:

```text
1. Scene image
2. Top and bottom legibility gradients
3. Six transparent semantic hit zones
4. Six compact native destination labels
5. Profile, currencies, streak, and menu HUD
6. Collapsed or expanded Daily Quest
7. Floating bottom navigation
8. Temporary celebration effects
```

### 7.1 Scene image

```kotlin
Image(
    painter = painterResource(R.drawable.village_home_six_landmarks_master),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    alignment = Alignment.Center,
    modifier = Modifier.fillMaxSize(),
)
```

Do not use `FillBounds`.

Do not put the scene inside a rounded white card.

Do not show blank bands.

Do not draw replacement scenery with `Canvas`.

### 7.2 Scrims

Top gradient:

```text
#0B2A36 at 24–30% opacity at the top
fade to transparent by 18% of screen height
```

Bottom gradient:

```text
transparent until approximately 72%
fade to #0B2A36 at 42–50% opacity at the bottom
```

Keep the village sunny. Scrims exist only for control contrast.

### 7.3 Landmark zones

Read initial normalized rectangles from `LANDMARK_LAYOUT.json`.

Map a normalized rectangle into the available scene bounds. Account for `ContentScale.Crop`; do not assume the uncropped bitmap coordinates equal screen coordinates.

Create a helper such as:

```kotlin
@Immutable
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

fun NormalizedRect.toDpRect(containerWidth: Dp, containerHeight: Dp): DpRect
```

If the crop changes the image viewport, create an explicit `SceneViewport` that calculates the displayed source rectangle. Use the same viewport for hit zones and labels.

Each destination has one large invisible hit target covering its landmark and clear pad. Do not create separate clickable building and label actions.

The semantic node must include:

```text
Destination name
Subject name
Progress
Availability state
Recommended state when applicable
Button role
```

Example:

```text
Story Tree, English, 5 of 12 lessons complete, recommended today, button
```

### 7.4 Destination label

Render one compact native label per destination. Remove the colored top badges and remove the large duplicate cards.

Label contents:

```text
[subject icon] Destination name
Subject · progress or lock reason
```

Example:

```text
[book] Story Tree
English · 5 of 12
```

Specifications:

```text
Width: 168–210dp based on available size
Minimum height: 56dp
Maximum ordinary height: 72dp
Fill: Cream #FFF7E8 at 94–97% opacity
Radius: 18dp
Internal padding: 10–12dp
Subject accent: 4dp side rail or 4dp top edge
Destination: Baloo 2 SemiBold/Bold, 17–19sp
Secondary line: Nunito ExtraBold, 12–14sp
Shadow: one restrained hard lower shadow plus a soft ambient shadow
```

Place the label within the clear interaction pad associated with each landmark. It may overlap the empty pad but must not cover the landmark’s defining silhouette.

Do not use opaque brown rectangles.

Do not render destination names twice.

### 7.5 Today’s Focus

Exactly zero or one destination may be recommended.

Recommended treatment:

* Gold `TODAY` capsule attached to the native label.
* Soft 2–4dp gold outer glow around the label.
* Optional 2% scale breathing animation no more than once every 2.4 seconds.
* A small path sparkle or lantern pulse may animate near that landmark.
* Accessible state description includes `recommended today`.

Do not permanently enlarge the recommended landmark.

Do not animate all six locations.

When reduced motion is enabled, retain the badge and glow but disable pulsing and movement.

### 7.6 Locked state

A locked destination remains fully visible in the scene.

Native label must show:

```text
Lock icon
Destination name
Concrete unlock rule or “Opening soon”
```

Never route a locked destination to an unrelated subject.

Never use opacity alone to convey locked state.

### 7.7 Profile HUD

Use a floating cream HUD card rather than a full-width top bar.

Contents:

* 48–56dp avatar.
* Child first name.
* `Level N`.
* XP progress bar.
* Numeric XP such as `660 / 900`.

Specifications:

```text
Top-left margin: 20–28dp plus system inset
Width: 220–280dp
Height: 68–82dp
Cream surface
20–24dp radius
Ink text
Gold XP fill
```

Do not display an app version on a child-facing screen.

### 7.8 Reward and streak HUD

Use compact individual pills, not a continuous white top strip.

Show:

* Flame and streak count.
* Star balance.
* Paw-coin balance.

Each pill must have a minimum 48dp touch target if clickable.

If a balance is zero, still explain the currency on tap or through content description. Do not make zero values feel like errors.

### 7.9 Daily Quest

Default state on expanded layout: collapsed mission pill or compact card.

Collapsed example:

```text
Daily Quest · Read and discover 5 words · 3/5
[Continue]
```

Expanded card appears only after tapping the quest area or when no destination would be obscured.

Collapsed specification:

```text
Width: 250–340dp
Height: 72–92dp
Position: upper-left below profile or upper-center clear region
Cream surface
Quest icon
One-line objective
Progress
One primary action
```

Expanded specification:

* Subject.
* Objective.
* Estimated minutes.
* Progress.
* Reward preview.
* Start or Continue.
* Close/collapse control.

The quest must not cover the central path, any landmark label, or bottom navigation.

### 7.10 Bottom navigation

Use a floating cream navigation capsule containing:

```text
Profile
Achievements
Backpack
Parents
```

Specifications:

```text
Bottom margin: 12–20dp plus navigation inset
Height: 64–72dp
Width: content-driven, normally 56–70% of tablet width
Radius: 22–28dp
Minimum target per item: 56×56dp
```

Use supplied vectors. Do not use emoji.

Do not enable an item with an empty callback.

Parent access must open a parent gate rather than adult controls directly.

## 8. Compact and portrait composition

Do not shrink the entire tablet scene until labels and targets become tiny.

For widths below 840dp or unsuitable landscape height:

1. Use the scene image as a 180–240dp hero crop at the top.
2. Overlay only the profile summary and Today’s Focus callout on the hero.
3. Place Daily Quest immediately below the hero.
4. Render six illustrated destination rows or cards in a two-column grid when width permits.
5. Use one column at narrow width or 200% font scale.
6. Preserve all six subjects.
7. Allow vertical scrolling.
8. Keep bottom navigation fixed or use a standard compact navigation bar.

Do not use the deprecated flat building sprites for compact cards.

A compact destination card may use:

* Subject-color icon badge.
* Destination name.
* Subject name.
* Progress.
* State.
* A subtle scenic crop from the shared background if implemented reliably.

If a scene crop is implemented, crop the bitmap through Compose layout and clipping. Do not create a stretched thumbnail. A plain native card is preferable to a distorted landmark.

## 9. Design tokens

Centralize tokens in `core-design-system`.

Colors:

```kotlin
object MaxinesColors {
    val VillageTeal = Color(0xFF087F83)
    val Coral = Color(0xFFF47C6B)
    val SunshineGold = Color(0xFFF5B82E)
    val LeafGreen = Color(0xFF66A83E)
    val StoryPurple = Color(0xFF7653B5)
    val SkyBlue = Color(0xFF3C9DDB)
    val HistoryGold = Color(0xFFB87916)
    val Ink = Color(0xFF183B4A)
    val Cream = Color(0xFFFFF7E8)
    val Success = Color(0xFF2F9E62)
    val MutedInk = Color(0xFF536870)
}
```

Spacing:

```text
4, 8, 12, 16, 24, 32, 48, 64dp
```

Shapes:

```text
Small control: 12dp
Pill: 50%
Child card: 18–24dp
Large sheet: 28–32dp
```

Typography:

```text
Baloo 2: display, destination name, large numerals, primary actions
Nunito: body, progress, Filipino copy, parent UI
```

Bundle fonts locally. Do not use downloadable fonts.

Do not claim completion while falling back to the device default typeface.

## 10. Tactile interaction

Primary child controls must look pressable.

Resting:

```text
5–6dp hard lower shadow
soft ambient shadow
```

Pressed:

```text
translate down approximately 4dp
lower shadow reduces to approximately 1dp
```

Release:

```text
spring back within approximately 160–220ms
```

Disabled:

```text
muted surface
no bounce
clear disabled semantics
```

Every action must show pressed feedback within 100ms.

Use one focal ambient animation at a time. Avoid a screen full of continuously bouncing objects.

## 11. Gamification model

Display real state from the feature/view-model layer:

* Level.
* XP.
* Daily streak.
* Stars.
* Paw coins.
* Daily Quest progress.
* Per-subject progress.
* Today’s Focus.
* Lock state.

Rewards must be deterministic and tied to meaningful learning activity.

Example:

```text
Complete 5 vocabulary interactions
Earn 25 stars and 10 paw coins
Advance English from 3/5 to 4/5
```

Do not award progress merely for opening a destination.

Do not add loot boxes, randomized rewards, paid currency, public leaderboards, punitive streak messaging, ads, or public child profiles.

## 12. State and component architecture

Do not keep all UI and sample data inside one large `VillageHomeScreen.kt`.

Recommended files:

```text
feature-child-home/
  VillageHomeRoute.kt
  VillageHomeScreen.kt
  VillageHomeViewModel.kt
  VillageHomeUiState.kt
  ExpandedVillageHome.kt
  CompactVillageHome.kt
  VillageSceneViewport.kt
  DestinationPlacement.kt

core-design-system/
  MaxinesProfileHud.kt
  MaxinesQuestCard.kt
  MaxinesDestinationLabel.kt
  MaxinesRewardPill.kt
  MaxinesBottomNavigation.kt
  MaxinesTactileButton.kt
  MaxinesProgressBar.kt
  MaxinesTheme.kt
```

State model:

```kotlin
@Immutable
data class VillageHomeUiState(
    val childName: String,
    val avatarRes: Int,
    val level: Int,
    val currentXp: Int,
    val targetXp: Int,
    val streakDays: Int,
    val stars: Int,
    val pawCoins: Int,
    val dailyQuest: DailyQuestUiState?,
    val destinations: List<SubjectDestinationUiState>,
    val selectedNavigationItem: VillageNavigationItem,
    val questExpanded: Boolean,
    val isLoading: Boolean,
)
```

Destination model:

```kotlin
@Immutable
data class SubjectDestinationUiState(
    val id: SubjectId,
    val destinationName: String,
    val subjectName: String,
    val iconRes: Int,
    val color: Color,
    val completedLessons: Int,
    val totalLessons: Int,
    val state: DestinationState,
    val normalizedRect: NormalizedRect,
)

sealed interface DestinationState {
    data object Available : DestinationState
    data object Recommended : DestinationState
    data object Completed : DestinationState
    data class Locked(val explanation: String) : DestinationState
}
```

Create one canonical destination list. Do not repeat subject mappings in multiple composables.

Preview fixtures belong in preview/test source, not production UI state.

## 13. Navigation contract

Map IDs explicitly:

```kotlin
when (subjectId) {
    SubjectId.ENGLISH -> onEnglishClick()
    SubjectId.FILIPINO -> onFilipinoClick()
    SubjectId.MATHEMATICS -> onMathematicsClick()
    SubjectId.SCIENCE -> onScienceClick()
    SubjectId.HISTORY -> onHistoryClick()
    SubjectId.GMRC -> onGmrcClick()
}
```

If a route is unavailable:

* Set state to `Locked`.
* Disable click behavior.
* Display a concrete explanation.
* Report disabled semantics.
* Never redirect to another subject.

Do not use `{}` as the default callback for an enabled production control.

## 14. Accessibility

Requirements:

* Minimum touch target: 48×48dp.
* Preferred child target: 56×56dp or larger.
* One logical TalkBack node per destination.
* Background is decorative and has no content description.
* Native label and hit area merge semantics.
* State uses icon, text, and color.
* Progress is announced numerically.
* Logical traversal: profile, quest, top HUD, six destinations in reading order, bottom navigation.
* Font scaling works at 100%, 150%, and 200%.
* Reduced motion works.
* No essential drag-only interaction.
* No timed response required.
* Focus indicator remains visible against the scene.

Locked example:

```text
Kindness Corner, GMRC, opening soon, disabled
```

Do not expose every flower, building, or decorative object as a TalkBack node.

## 15. Localization

Move all production strings to resources.

Support at least 30% text expansion.

Test both English and Filipino.

Do not shorten `Bahay ng Kuwento` to compensate for an inflexible layout. Fix the layout.

Never bake destination names, progress, rewards, quest text, lock reasons, or navigation labels into art.

## 16. Performance

* Keep the master in `drawable-nodpi` to avoid density resampling.
* Decode the scene once per screen composition.
* Do not create six duplicate full-resolution bitmap instances.
* Do not allocate placement lists on every frame.
* Use immutable stable state.
* Limit infinite animations to one subtle focus treatment.
* Test cold launch and screen re-entry.
* Verify no visible asset pop-in after the screen is interactive.
* Consider lossless or visually equivalent WebP only after screenshot comparison; keep the PNG source master.

## 17. Required implementation sequence

### Phase A: baseline

1. Create a branch.
2. Record current commit SHA.
3. Run baseline build and tests.
4. Capture current screenshots.
5. Copy the package into a local working folder.
6. Verify `ASSET_MANIFEST.json` hashes.
7. Read this document completely.

Do not edit before completing the baseline record.

### Phase B: scene foundation

1. Add the master scene resource.
2. Remove old flat building overlays from the expanded screen.
3. Remove large scene characters.
4. Remove duplicate colored destination badges.
5. Remove the full-width white top bar.
6. Render the scene full bleed with correct crop.
7. Add scrims.
8. Add temporary debug outlines for the six normalized zones.
9. Capture a screenshot and verify alignment.
10. Remove debug outlines only after all zones align.

### Phase C: native destination layer

1. Implement the canonical six-destination model.
2. Implement one reusable destination label.
3. Attach one semantic hit target per landmark.
4. Add progress, recommended, completed, and locked states.
5. Connect real routes or honest disabled states.
6. Verify every destination individually.

### Phase D: HUD and quest

1. Implement floating profile HUD.
2. Implement streak, star, and paw-coin pills.
3. Implement menu.
4. Implement collapsed Daily Quest.
5. Implement expansion behavior.
6. Confirm quest never overlaps landmarks or bottom navigation.

### Phase E: navigation and responsive behavior

1. Implement floating tablet navigation.
2. Implement compact layout.
3. Test insets.
4. Test portrait and landscape.
5. Test 200% font scale.
6. Test long Filipino strings.

### Phase F: polish

1. Add tactile pressed states.
2. Add one recommended-location treatment.
3. Add reduced-motion behavior.
4. Add accessibility semantics.
5. Remove obsolete prototype code and assets only after references are no longer required.

### Phase G: verification

1. Run build and tests.
2. Capture screenshot matrix.
3. Compare against acceptance criteria.
4. Fix every failed criterion.
5. Submit results with the pull request.

## 18. Required tests

Unit tests:

* Six destinations are always present in canonical order.
* Every subject ID maps to the correct route.
* Locked destinations do not invoke navigation.
* XP and progress values are clamped safely.
* Exactly zero or one destination is recommended.
* Accessibility state strings match destination state.

Compose UI tests:

* All six destination names exist.
* Each destination has one click target.
* Locked GMRC reports disabled when configured locked.
* Daily Quest collapse and expand works.
* Bottom navigation actions are not no-ops.
* 200% font scale does not hide primary actions.
* Compact mode exposes all six subjects through scrolling.

Screenshot tests:

```text
360 × 800
600 × 960
1280 × 800
1672 × 941
1280 × 800 at 200% font scale
360 × 800 at 200% font scale
1280 × 800 with one locked destination
1280 × 800 with each possible Today’s Focus state
1280 × 800 reduced motion
```

## 19. Visual pass/fail gate

The build fails if any statement below is false:

* The first impression is an illustrated game village, not a dashboard.
* Exactly six landmarks are visible.
* All six landmarks have comparable prominence.
* No flat placeholder building is layered over the scene.
* No guide floats over a door or ground pad.
* No destination name is displayed twice.
* No opaque brown label remains.
* No destination is clipped.
* The scene is not stretched.
* There are no blank bands.
* The quest does not cover the central path.
* The quest does not collide with bottom navigation.
* HUD elements float lightly rather than creating a full-width white bar.
* The child-facing version label is gone.
* GMRC is visible.
* A locked destination has text and icon, not opacity alone.
* Every enabled control has a real callback.
* Every touch target is at least 48dp.
* All six subjects remain discoverable on phone.
* 200% font scale leaves controls reachable.
* TalkBack announces name, subject, progress, and state.
* Reduced-motion mode preserves all information.
* No emoji is shipped.
* Baloo 2 and Nunito are actually used.
* Subject colors come from central tokens.
* The screenshot matrix is attached.

## 20. Prohibited shortcuts

Do not:

* Keep the current UI and merely swap the background.
* Add the new scene while retaining old building sprites.
* Draw replacement landmarks with basic Compose geometry.
* Use the target board as one giant clickable bitmap.
* Bake interactive text into images.
* Use emoji.
* Hide Filipino, History, or GMRC.
* Make the first three subjects permanently larger.
* Route unavailable subjects to unrelated lessons.
* Enable empty callbacks.
* Claim completion because compilation succeeds.
* Test only one emulator size.
* Shrink tablet UI until it technically fits.
* Ignore TalkBack or font scaling.
* Put preview sample state in production code.
* add multiple continuous bouncing animations.
* improvise a different art direction.

## 21. Build commands

Run the relevant commands from `android/`:

```bash
./gradlew :feature-child-home:compileDebugKotlin
./gradlew :feature-child-home:testDebugUnitTest
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

If screenshot tests exist, run them. If they do not exist, add the smallest practical screenshot-test harness or provide deterministic emulator captures.

Report each exact command and result. Do not say only “tests pass.”

## 22. Pull request evidence

The pull request must include:

1. Baseline commit SHA.
2. Before screenshot.
3. Full after screenshot matrix.
4. File-by-file summary.
5. Added and removed assets.
6. Six-subject route table.
7. Locked-state behavior.
8. Accessibility verification.
9. Font-scale verification.
10. Reduced-motion verification.
11. Build commands and output.
12. Tests and output.
13. Known limitations.
14. Remaining visual differences.

## 23. Stop conditions

Stop and report instead of inventing a workaround when:

* The master scene file is missing or corrupt.
* The scene cannot be added because of repository constraints.
* A subject route cannot be identified.
* Required state does not exist and cannot be safely inferred.
* Font files are unavailable.
* A build failure predates your changes.
* Screenshot tooling is unavailable.

A stop report must identify the exact file, symbol, route, or command that is blocked and propose the smallest next action.

Do not silently replace missing art with flat shapes or generic icons.

## 24. Definition of done

The milestone is done only when:

* The scene plate is the expanded-layout visual foundation.
* Old flat building overlays are absent.
* Labels are singular, compact, native, and readable.
* The quest no longer obstructs the village.
* HUD chrome is lighter and game-like.
* All six subjects are equally represented.
* Gamification values are real state.
* Interactions and routes are honest.
* Compact and expanded layouts pass.
* Accessibility passes.
* Screenshot comparison passes.
* Build and tests pass.
* The result is visibly more graphical, cohesive, playful, and inviting than `references/current_build.png`.

Return the implementation, evidence, and pass/fail checklist. Do not return only a description of intended changes.
