# DeepSeek V4 Pro Front-End Implementation Contract

## Your role

You are the primary Android front-end implementer for Maxine’s World.

Your task is not to reinterpret the design, create a generic educational dashboard, or merely make the current screen compile. Your task is to replace the current prototype-quality child homepage with a polished, illustrated, accessible, gamified learning village that closely follows the supplied design package.

A successful build is not sufficient. A screen that compiles but still looks flat, empty, clipped, generic, or materially different from the reference must be considered a failed implementation.

## Repository

Repository:

```text
https://github.com/ronrosolada/maxines-world
```

The application uses Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, WorkManager, and a modular Android architecture. The child homepage is in the `feature-child-home` module. 

## Files supplied to you

You should receive:

```text
Maxine's World app design.zip
maxines-world-ui-overhaul.zip
```

The design archive contains:

```text
handoff/docs/design.md
handoff/docs/village-home-build-spec.md
handoff/docs/maxines-world-implementation-handoff.md
handoff/maxines-world-screens.html
handoff/assets/graphics/
handoff/assets/buildings/
handoff/assets/characters/
handoff/assets/icon/
```

The UI-overhaul package contains:

```text
HOMEPAGE-AUDIT.md
README-APPLY.md
MANIFEST.json
android/feature-child-home/.../VillageHomeScreen.kt
android/feature-child-home/src/main/res/drawable-nodpi/village_background.webp
android/feature-child-home/src/main/res/drawable-nodpi/location_*.png
android/feature-child-home/src/main/res/drawable-nodpi/character_*.png
```

## Hierarchy of authority

When instructions conflict, use this order:

1. This implementation contract.
2. `handoff/docs/design.md`.
3. `handoff/docs/village-home-build-spec.md`.
4. The corrected Village Home panel in `handoff/maxines-world-screens.html`.
5. Supplied visual reference images.
6. Existing repository implementation.
7. Your own design assumptions.

Do not preserve an existing implementation choice merely because it already compiles.

Do not invent an alternative design when the supplied material answers the question.

## Required preliminary work

Before editing code:

1. Create a new branch from the latest repository revision.
2. Record the current commit SHA.
3. Build the unchanged project.
4. Run existing tests.
5. Capture the current Village Home at the required screenshot sizes.
6. Open `handoff/maxines-world-screens.html` in a browser.
7. Review the corrected Village Home reference.
8. Read all of `design.md`.
9. Read all of `village-home-build-spec.md`.
10. Inspect every building and character PNG against transparency.
11. Inspect the prepared replacement `VillageHomeScreen.kt`.
12. Produce a short implementation checklist in the pull request description.
13. Do not start coding until these steps are complete.

Use these baseline commands where applicable:

```bash
cd android
./gradlew :feature-child-home:compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

If the original build fails, record the failure before making changes. Do not silently attribute a pre-existing failure to your implementation.

## Product goal

The Village Home must immediately communicate:

```text
This is a warm, illustrated adventure village where learning unlocks progress.
```

It must not communicate:

```text
This is a conventional school dashboard with cards placed over a background.
```

The child should be able to understand within a few seconds:

* Who they are.
* Their current level and progress.
* Their Daily Quest.
* Which subject is recommended today.
* Which six subject destinations exist.
* Which destinations are available, in progress, completed, or locked.
* How to open achievements, backpack, profile, or the parent area.

## Current-build critique

The current homepage is not an acceptable visual baseline.

It currently exhibits these defects:

* A large, mostly empty “Daily Challenge” strip consumes prime screen space.
* Subject launchers look like disabled gray circles.
* The illustrated world is replaced with flat geometric scenery.
* Mountains, ground, buildings, and paths resemble debugging placeholders.
* Characters float above buildings instead of standing on the ground.
* Large translucent circles remain behind some characters.
* Destination labels are opaque brown rectangles.
* Destination labels obscure the scenery and do not communicate progress.
* The fifth destination is clipped.
* GMRC is missing.
* English, Mathematics, and Science receive more visual attention than the other subjects.
* A version number appears in the child interface.
* The profile, level, and XP hierarchy is incomplete.
* The page resembles a dashboard prototype rather than an immersive village.
* Existing artwork and native UI do not share one coherent illustration style.

Do not attempt to solve these problems by changing only colors, corner radii, or typography. The composition and layering must change.

## Non-negotiable six-subject parity

The village must include exactly these six destinations:

| Subject | Destination | Guide | Color |
|---|---|---|---|
| English | Story Tree | Mira | `#7653B5` |
| Filipino | Bahay ng Kuwento | Mira | `#F47C6B` |
| Mathematics | Number Market | Milo | `#3C9DDB` |
| Science | Discovery Lab | Niko | `#66A83E` |
| Makabansa / Philippine History | Heritage Harbor | Lakan | `#B87916` |
| GMRC | Kindness Corner | Duke | `#087F83` |

All six must receive:

* Equal permanent destination footprint.
* Comparable building scale.
* Comparable illustration detail.
* Equal-quality character treatment.
* Equal touch-target size.
* The same label structure.
* The same progress-state model.
* The same opportunity to become the recommended destination.
* Equal representation in progression and reward systems.
* Equal prominence over time.

“Recommended today” may temporarily highlight one destination. It must not permanently enlarge English, Mathematics, or Science while leaving Filipino, History, and GMRC as minor secondary options.

If GMRC does not have a valid route yet:

* Show Kindness Corner at full visual prominence.
* Show a lock icon.
* Show native text such as `Opening soon`.
* Disable its click action.
* Expose the disabled state to TalkBack.
* Do not route it to an English or unrelated lesson.
* Do not omit it.

## Important prepared-patch warning

The supplied `location_*.png` files are composite images. Each already contains:

* A building.
* Its guide character.
* A subject sign.
* A contact shadow or ground treatment.

The prepared `VillageHomeScreen.kt` also contains a `guideRes` field and renders a separate `character_*.png` over each composite.

This would duplicate the character.

Before accepting the patch, choose one of these approaches.

### Required approach for this iteration

Use each `location_*.png` as one building-plus-guide composite.

Then:

1. Remove `guideRes` from `SubjectDestination`.
2. Remove all `guideRes = ...` assignments.
3. Remove the second character `Image` from `DestinationMarker`.
4. Remove the second character `Image` from `CompactDestinationCard`.
5. Retain separate character assets only for the Daily Quest, welcome panel, lesson guide, or future animation.
6. Verify that every destination displays exactly one guide.

Do not render both a composite building image and a separate copy of the guide.

A future iteration may use building-only SVGs plus independent Rive character rigs. That refactor is outside the minimum homepage milestone.

## Art direction

### Desired appearance

The visual style should feel:

* Warm.
* Colorful.
* Dimensional.
* Friendly.
* Animal-filled.
* Playful without appearing preschool-oriented.
* Rich enough to invite exploration.
* Calm enough for reading and learning.
* Clearly designed for an eight-year-old.
* Consistent across all six subjects.

### Avoid

Do not ship:

* Flat Canvas mountains.
* Flat rectangles standing in for buildings.
* Generic Material icons as primary destination artwork.
* Emoji.
* Clip-art mixtures.
* Photorealistic imagery.
* Harsh neon gradients.
* Excessive glass effects.
* Floating characters.
* Large opaque label blocks.
* Baked interactive text.
* Debug labels.
* Version numbers on the child homepage.
* Empty click handlers.
* Fake subject routes.
* Random decorative elements covering controls.
* Unequal treatment of Filipino, History, or GMRC.

## Layer model

Implement the tablet Village Home as a `Box` with explicit layers.

The z-order must be:

```text
Layer 1: full-screen illustrated background
Layer 2: six destination composites
Layer 3: subtle legibility scrims
Layer 4: native destination state and hit targets
Layer 5: native profile, quest, streak, menu, and navigation
Layer 6: temporary animation and celebration effects
```

### Layer 1: illustrated background

Use:

```text
village_background.webp
```

Requirements:

* Render as a full-bleed image.
* Use `ContentScale.Crop`.
* Never use `FillBounds`.
* Preserve the source aspect ratio.
* Do not place it inside a white or cream dashboard card.
* Do not add a rounded rectangular frame around the entire village.
* Do not expose blank bands.
* Do not redraw scenery with Compose `Canvas`.
* Mark it decorative for accessibility.
* Crop consistently across devices.
* Keep the six intended staging areas visible at the reference tablet size.

The background must remain free of:

* Text.
* Buttons.
* Progress.
* Subject labels.
* Characters.
* Buildings.
* Embedded interactive controls.

### Layer 2: destination composites

Use exactly one resource for each destination:

```text
location_story_tree.png
location_bahay_kuwento.png
location_number_market.png
location_discovery_lab.png
location_heritage_harbor.png
location_kindness_corner.png
```

Requirements:

* Preserve intrinsic aspect ratio.
* Do not tint the images.
* Do not mask them into cards.
* Do not place an opaque rectangular background behind them.
* Do not clip roofs, trees, signs, characters, or contact shadows.
* Bottom-anchor each image to its staging position.
* Calibrate each location individually.
* Keep displayed scale comparable across all six.
* Do not rely on a generic weighted `Row` as the final tablet placement.
* Ensure transparent pixels remain transparent.
* Verify no matte, fringe, checkerboard, or halo appears.

Use a single reference coordinate system instead of unrelated hard-coded offsets.

A suitable model is:

```kotlin
data class DestinationPlacement(
    val normalizedX: Float,
    val normalizedY: Float,
    val scale: Float,
    val offsetXDp: Dp = 0.dp,
    val offsetYDp: Dp = 0.dp,
)
```

Start with approximate bottom-center anchors:

```text
Story Tree:          x 0.27, y 0.55
Number Market:       x 0.50, y 0.54
Discovery Lab:       x 0.73, y 0.55
Bahay ng Kuwento:    x 0.27, y 0.79
Heritage Harbor:     x 0.50, y 0.79
Kindness Corner:     x 0.73, y 0.79
```

These are starting points, not permission to skip visual tuning. Measure the rendered scene and adjust the anchors so each destination visibly sits on the corresponding ground pad.

### Layer 3: legibility scrims

Use subtle gradients, not opaque panels.

Top scrim:

```text
#0B2A36 at approximately 30% opacity
fade to transparent by approximately 18% of screen height
```

Bottom scrim:

```text
transparent until approximately 62% of screen height
fade to #0B2A36 at approximately 52% opacity at the bottom
```

The purpose is to improve contrast behind native controls. The gradients must not make the village look dim, muddy, or nighttime-themed.

### Layer 4: destination state

All interactive state must be native Compose.

Each destination must expose:

* Destination name.
* Subject name.
* Progress percentage or completed count.
* Available, recommended, completed, or locked state.
* One semantic click target.
* Native state description.
* A visible focus and pressed state.

Artwork must not be the only source of the destination label.

The sign painted into the composite is decorative. The accessible native label remains mandatory.

## Recommended tablet composition

Reference canvas:

```text
1280 × 800 dp
```

### Profile card

Position:

```text
Top-left
24–32dp from safe edges
```

Content:

* 56dp circular avatar.
* Learner name.
* Current level.
* Gold XP progress bar.
* Numeric XP such as `660 / 900`.

Style:

* Cream surface `#FFF7E8`.
* Radius approximately 22dp.
* Ink text `#183B4A`.
* Tactile lower edge or restrained shadow.
* No version number.
* No debug build text.

### Streak and menu

Position:

```text
Top-right
```

Streak content:

* Flame icon asset, not emoji.
* Streak count.
* Short encouragement.
* Optional weekly star row.

Menu:

* Approximately 56×56dp.
* Sky-blue surface.
* Rounded corners.
* Native icon.
* Pressed state.
* Real callback or visibly disabled state.

### Daily Quest

Position it below the profile card in the upper-left area.

The quest must not cover any destination.

Required content:

* Native quest icon.
* `Daily Quest` heading.
* Short learning objective.
* Estimated duration.
* Progress such as `3 of 5`.
* Reward preview.
* Start or Continue button.

Example:

```text
Daily Quest
Read a story and discover 5 new words
5 min
3 of 5
Reward: 25 stars and 10 paw coins
Continue
```

Daily Quest is the strongest action on the page, but it must not dominate more than roughly one quarter of the usable screen.

### Destination labels

Use compact native overlays near the base of each destination.

Each label must include:

* Destination name.
* Subject.
* Progress or lock explanation.
* Subject-color accent.
* Optional `TODAY` indicator.

Do not use large opaque brown rectangles.

Prefer:

* Cream at approximately 95–97% opacity.
* Radius 16–22dp.
* Compact height.
* Clear ink text.
* Subject-colored top edge, side edge, badge, or progress bar.
* A restrained shadow that separates the label from the art.

Labels must not cover most of a building or character.

### Today treatment

The recommended destination must receive:

* A gold `TODAY` ribbon or badge.
* A subtle glow or raised treatment.
* Native accessible state text.
* No permanent size advantage.

Do not use animation as the only indication.

### Bottom navigation

Use these destinations:

```text
My Profile
Achievements
My Backpack
Parents
```

Requirements:

* Cream floating navigation surface.
* Rounded container.
* Minimum 48dp targets.
* Prefer 56dp or larger child targets.
* Native vector icons.
* Native labels.
* Active-state styling.
* No emoji.
* No enabled no-op action.

If a route is unavailable:

* Render that item disabled.
* Provide a disabled semantic state.
* Do not leave an enabled empty callback.

Parent access must lead to a parent gate rather than directly exposing adult controls.

## Compact and phone behavior

Do not scale the entire 1280×800 tablet scene down until everything becomes tiny.

For compact widths:

1. Preserve the illustrated identity.
2. Use the village background as a header or ambient page backdrop.
3. Show Daily Quest near the top.
4. Show all six illustrated destination cards.
5. Use a two-column grid when space allows.
6. Use one column when font scaling or width requires it.
7. Allow vertical scrolling.
8. Keep each destination’s artwork prominent.
9. Keep every target at least 48dp.
10. Keep primary actions at least 56dp high.
11. Avoid horizontal overflow.
12. Do not clip destination names.
13. Do not omit a subject.
14. Do not convert destinations into generic Material list rows.

Recommended logic:

```text
Width under 480dp:
    one or two columns based on available width and font scale

Width 480–839dp:
    two-column illustrated destination grid

Width 840dp and above with suitable landscape ratio:
    full village scene composition
```

Use `WindowSizeClass` or a shared responsive-profile abstraction. Do not create device-model checks.

At 200% font scale, favor vertical growth and scrolling over reducing text size.

## Typography

Bundle fonts locally because the app is offline-first.

Use:

```text
Baloo 2:
    screen titles
    destination names
    large numerals
    primary buttons
    reward counts

Nunito:
    body copy
    instructions
    Filipino text
    progress labels
    parent UI
```

Do not use downloadable fonts.

Do not silently fall back to default typography and claim visual completion.

Minimum child-facing guidance:

```text
Destination name: approximately 18–22sp
Body and progress text: approximately 15–18sp
Primary button: approximately 18–22sp
Screen title: approximately 28–34sp
```

All text must support Android font scaling.

## Color tokens

Use centralized semantic tokens.

```kotlin
VillageTeal = Color(0xFF087F83)
Coral = Color(0xFFF47C6B)
SunshineGold = Color(0xFFF5B82E)
LeafGreen = Color(0xFF66A83E)
StoryPurple = Color(0xFF7653B5)
SkyBlue = Color(0xFF3C9DDB)
HistoryGold = Color(0xFFB87916)
Ink = Color(0xFF183B4A)
Cream = Color(0xFFFFF7E8)
Success = Color(0xFF2F9E62)
```

Do not scatter raw hex values throughout `VillageHomeScreen.kt`.

Every state must use more than color alone.

Examples:

```text
Completed:
    green accent + check icon + Completed text

Locked:
    muted treatment + lock icon + unlock text

Recommended:
    gold treatment + TODAY text

In progress:
    progress bar + numeric count
```

## Tactile interaction

Primary child controls should look physically pressable.

Required behavior:

```text
Resting:
    hard lower shadow of approximately 5–6dp

Pressed:
    control moves downward approximately 4dp
    lower shadow reduces to approximately 1dp

Released:
    spring back quickly

Disabled:
    visibly muted
    no tactile animation
```

Suggested timings:

```text
Press response: 80–160ms
Card transition: 180–280ms
Screen transition: 250–400ms
```

Respect reduced-motion preferences.

When reduced motion is enabled:

* Replace bounce with a color or elevation change.
* Use static character poses.
* Disable parallax.
* Retain all instructional and state information.

## Gamification requirements

Gamification must reinforce learning rather than distract from it.

Include:

* Level and XP.
* Daily streak.
* Daily Quest.
* Stars.
* Paw coins.
* Subject progress.
* Named achievements.
* Visible destination unlocks.
* Short completion celebrations.
* Clear “today” recommendation.
* Village progression over time.

Do not include:

* Paid rewards.
* Loot boxes.
* Randomized reward mechanics.
* Punitive streak loss messaging.
* Harsh failure sounds.
* Public leaderboards.
* Public child profiles.
* Advertising.
* Infinite distracting animations.

Rewards must be deterministic and linked to meaningful learning activity.

Example:

```text
Complete the five-word story activity
Earn 25 stars and 10 paw coins
Advance English progress from 3/5 to 4/5
```

Do not award progress merely for opening a screen.

## Component structure

Do not leave the whole homepage as one large composable.

Create or reuse:

```text
VillageHomeRoute
VillageHomeScreen
VillageScene
VillageHud
PlayerProfileCard
StreakCard
DailyQuestCard
SubjectDestination
SubjectDestinationLabel
VillageBottomNavigation
TactilePrimaryButton
ProgressBar
RewardChip
```

Suggested state model:

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
    val isLoading: Boolean,
)
```

Destination model:

```kotlin
@Immutable
data class SubjectDestinationUiState(
    val id: String,
    val destinationName: String,
    val subjectName: String,
    val artworkRes: Int,
    val subjectColor: Color,
    val completedLessons: Int,
    val totalLessons: Int,
    val state: DestinationState,
    val placement: DestinationPlacement,
)
```

State:

```kotlin
sealed interface DestinationState {
    data object Available : DestinationState
    data object Recommended : DestinationState
    data object Completed : DestinationState
    data class Locked(val reason: String) : DestinationState
}
```

Do not duplicate subject definitions in multiple lists.

Do not permanently hard-code sample progress in the composable. Temporary preview fixtures are acceptable only inside preview or test data.

## Interaction mapping

Every destination ID must map explicitly to the correct route.

Example:

```kotlin
when (destinationId) {
    "english" -> openEnglish()
    "filipino" -> openFilipino()
    "mathematics" -> openMathematics()
    "science" -> openScience()
    "philippine-history" -> openHistory()
    "gmrc" -> openGmrc()
    else -> reportUnknownDestination(destinationId)
}
```

Do not infer routes from list position.

Do not route unavailable content to another subject.

Do not use empty callbacks for enabled controls.

## Accessibility

Every destination must expose one logical TalkBack node.

Example description:

```text
Story Tree, English, 5 of 12 lessons complete, recommended today, button
```

Locked example:

```text
Kindness Corner, GMRC, opening soon, disabled
```

Requirements:

* Minimum 48×48dp targets.
* Preferred child targets of at least 56dp.
* Logical focus order.
* Decorative background omitted from accessibility.
* Decorative composite artwork omitted when a native semantic node describes it.
* Progress communicated numerically.
* Lock communicated through text and icon.
* State not communicated only by color.
* No essential drag-only interaction.
* No essential timed action.
* Font scaling support.
* Reduced-motion support.
* Left-handed testing.
* Landscape and portrait testing.

Do not expose every decorative animal or leaf as a separate TalkBack element.

## Localization

Move visible production strings into `strings.xml`.

Support at least 30% expansion.

Do not embed these in images:

* Subject names.
* Destination names used for interaction.
* Progress.
* Quest objectives.
* Rewards.
* Navigation labels.
* Lock explanations.
* Button labels.

Test English and Filipino text.

Do not abbreviate Bahay ng Kuwento merely to make a poor layout fit.

## Performance

The homepage must not introduce slow first render or memory pressure.

Requirements:

* Use appropriately compressed WebP for the large background.
* Keep transparent composites as PNG or lossless WebP when alpha is required.
* Avoid decoding unnecessarily oversized assets repeatedly.
* Use `painterResource` or an appropriate image loader consistently.
* Avoid recomputing placement and destination lists on every frame.
* Use stable immutable models.
* Avoid infinite animations on all six destinations.
* Allow only one subtle focal animation at a time.
* Confirm release-build resource loading.
* Confirm no visible asset pop-in after the screen is ready.

## Implementation phases

### Phase 1: establish the visual baseline

Deliver:

* Current-build screenshot matrix.
* Target-reference screenshot.
* Written list of differences.
* Confirmation of the six-subject mapping.
* Confirmation of the selected asset composition method.

Do not change unrelated lesson logic in this phase.

### Phase 2: install the visual foundation

Implement:

* Full-bleed village background.
* Correct z-order.
* Legibility scrims.
* Six destination composites.
* Removal of flat Canvas scenery.
* Removal of duplicated character rendering.
* Individual destination anchors.

Checkpoint:

All six destinations must appear grounded before adding detailed labels.

### Phase 3: implement native HUD and states

Implement:

* Profile and XP.
* Streak.
* Menu.
* Daily Quest.
* Native destination labels.
* Progress.
* TODAY state.
* Locked state.
* Bottom navigation.

Checkpoint:

There must be no overlap at 1280×800.

### Phase 4: responsive implementation

Implement:

* Tablet landscape scene.
* Medium two-column layout.
* Compact one- or two-column layout.
* Scrolling.
* Insets.
* Font scaling.
* Localization expansion.

Checkpoint:

All six subjects remain visible or intentionally reachable.

### Phase 5: accessibility and interaction

Implement:

* TalkBack semantics.
* Focus order.
* Pressed states.
* Disabled states.
* Reduced motion.
* Real callback verification.
* Minimum target verification.

### Phase 6: visual QA

Capture all required screenshots.

Do not request approval without them.

### Phase 7: cleanup

Remove:

* Unused Canvas scenery.
* Duplicate character resources from destination rendering.
* Emoji.
* Version label.
* Dead code.
* Hard-coded prototype lists.
* Enabled no-op actions.
* Temporary debug text.
* Unused imports.

## Required screenshot matrix

Capture the Village Home at:

```text
360 × 800
600 × 960
1280 × 800
1672 × 941
```

Also capture:

```text
1280 × 800 at 200% font scale
360 × 800 at 200% font scale
1280 × 800 with GMRC locked
1280 × 800 with a different TODAY destination
1280 × 800 with TalkBack focus visible on a destination
```

Use consistent app state for before-and-after comparisons.

## Objective pass/fail checks

The homepage fails review if any of these are false:

* The first impression is an illustrated village rather than a dashboard.
* Exactly six subject destinations exist.
* Every subject receives equal permanent prominence.
* Every destination contains exactly one guide character.
* No destination is clipped.
* No character floats.
* No ghost circle appears.
* No flat prototype building remains.
* No emoji appears.
* No child-facing version number appears.
* No visible enabled control has an empty callback.
* The background is full-bleed.
* The background preserves aspect ratio.
* No blank bands appear.
* No destination image is stretched.
* Every destination is grounded on a staging pad.
* Daily Quest overlaps no destination.
* Native labels obscure no major building detail.
* Every destination has native progress or lock text.
* The recommended destination has a native TODAY state.
* GMRC is visible even if locked.
* GMRC does not route to unrelated content.
* Every enabled destination opens the correct subject.
* Every target is at least 48dp.
* Primary buttons are at least 56dp high.
* Baloo 2 and Nunito are bundled locally.
* Color tokens are centralized.
* At 200% font scale, controls remain reachable.
* At compact width, all subjects remain discoverable.
* TalkBack reads name, subject, progress, and state.
* Decorative scenery does not pollute focus order.
* Reduced-motion mode retains all information.
* Release resources render without placeholders.

## Pixel and composition review

At 1280×800, compare the implementation side by side with the corrected reference.

Review:

* Overall warmth.
* Scene depth.
* Building scale.
* Character grounding.
* Balance across six subjects.
* Screen density.
* Quest prominence.
* HUD size.
* Label size.
* Empty space.
* Cropping.
* Contrast.
* Native-versus-baked text.
* Child appeal.

A destination anchor should generally be within approximately 2% of its intended normalized position after tuning.

Asset aspect-ratio deviation should be effectively zero. Visible stretching is an automatic failure.

## Prohibited shortcuts

Do not:

* Keep the current dashboard and merely add a background behind it.
* Rebuild scenery with Canvas primitives.
* Use the reference screenshot as one giant interactive image.
* Bake controls or progress into artwork.
* Render a separate guide over a composite containing that guide.
* Use one generic scale without inspecting asset bounds.
* Hide a subject to make the layout fit.
* Reduce Filipino, History, or GMRC to secondary chips.
* Ship emoji.
* Ship debug text.
* Ship default Material styling as the final child design.
* Claim completion because the app compiles.
* Claim completion without screenshots.
* Use one emulator size as proof of responsiveness.
* shrink the complete tablet composition until touch targets become unreadable.
* Enable a control that performs no action.
* Route GMRC to an unrelated lesson.
* Put hard-coded strings directly in production composables.
* make animation necessary to understand state.
* use color alone for progress, correctness, or locking.
* remove accessibility semantics to simplify the code.

## Pull request requirements

The pull request description must contain:

```text
1. Before screenshot
2. After screenshot matrix
3. Summary of composition changes
4. List of added assets
5. List of removed prototype elements
6. Six-subject route mapping
7. Locked-route behavior
8. Accessibility verification
9. Font-scale verification
10. Build and test results
11. Known limitations
12. Follow-up work
```

Include exact commands and results.

Example:

```text
./gradlew :feature-child-home:compileDebugKotlin
PASS

./gradlew testDebugUnitTest
PASS

./gradlew assembleDebug
PASS
```

Do not write `tests pass` without listing what ran.

## Definition of done

The Village Home milestone is complete only when:

1. The current flat prototype has been fully replaced.
2. The illustrated background is used correctly.
3. All six destination composites are grounded.
4. Duplicate character rendering has been removed.
5. All six subjects have equal structural importance.
6. Daily Quest is actionable and visually dominant without obstructing the village.
7. Player level, XP, streak, stars, and paw coins are visible.
8. Native progress and lock states are implemented.
9. Navigation callbacks are real or explicitly disabled.
10. Compact layouts preserve illustrated destinations.
11. Accessibility requirements pass.
12. Screenshot comparisons pass.
13. Build and tests pass.
14. No prohibited shortcut remains.
15. The reviewer agrees that the result feels graphical, fun, cohesive, and inviting to a child.

## Final instruction

Do not optimize for speed at the expense of the visual target.

Do not return with only code changes.

Return with:

* The implementation.
* The screenshot matrix.
* Build output.
* Test output.
* A checklist showing every acceptance criterion as pass or fail.
* A short list of any remaining visual differences.

If any required visual asset cannot be used correctly, stop and document the exact missing or defective asset. Do not silently substitute flat geometry, emoji, or generic Material icons.


---

## Sources

- [GitHub - ronrosolada/maxines-world: Maxine's World — Private Android learning app for Grade 3 (MATATAG-aligned). Offline-first, animal-village themed. Kotlin, Jetpack Compose, Room, Hilt. · GitHub](https://github.com/ronrosolada/maxines-world)
