# Maxine’s World Village Home Correction v1.4

## Purpose

Correct the latest Village Home implementation without redesigning the underlying illustrated village.

The current build has a strong background, six subject areas, bamboo styling, gamification data, and endemic-animal details. The implementation fails because bamboo destination plaques were rendered as enormous content cards. These cards obscure most of the village and disconnect the labels from their buildings. The profile and Daily Quest surfaces have the same expansion problem. 



[Image: Latest Village Home build showing oversized bamboo cards](https://purestorage-be.glean.com/api/v1/downloadchatfile/a88fa185a6284f5d9a85336a9e44d659)



*Figure 1: Latest build. The cream surfaces cover most of the illustrated village and contain excessive empty space.*

This is primarily a Compose layout correction. Do not generate another village background or replace the landmark artwork.

## Visual objective

The finished screen must look like:

```text
An illustrated Filipino learning village with small physical bamboo signposts
positioned beside six building entrances, plus a compact game HUD.
```

It must not look like:

```text
A three-column dashboard of cream cards placed over a village wallpaper.
```

The illustrated village must remain the dominant visual element.

## Immediate diagnosis

The current implementation appears to have these layout defects:

* Destination components are arranged in a grid instead of being anchored to buildings.
* Destination surfaces use `fillMaxHeight`, `weight`, fixed large heights, or matching parent constraints.
* The sawali fill uses `matchParentSize` inside a parent that receives excessive height.
* The profile area contains an unintended cream child that expands vertically.
* Daily Quest uses almost the entire screen width.
* Bottom navigation overlays destination cards.
* Animals are positioned relative to cards instead of stable scene coordinates.
* The destination artwork and native controls compete for the same space.
* The screen has no explicit collision or safe-area contract.

Do not attempt to improve this by changing opacity, font size, or color. Correct measurement, positioning, and hierarchy first.

## Required result at tablet landscape size

Use `1280 × 800dp` as the primary visual reference.

The corrected composition must contain:

* One full-bleed illustrated village.
* One compact profile HUD in the upper-left.
* One compact Daily Quest panel below the profile HUD.
* Three compact currency or streak indicators in the upper-right.
* Exactly six uniform bamboo destination plaques.
* Each plaque immediately beside its associated building entrance.
* One bottom navigation surface.
* Small endemic animals placed in the scenery.
* No large empty cream surfaces.
* No destination grid.

At least 70% of the village scene must remain unobscured by opaque UI.

## Mandatory deletion pass

Before building the corrected layout, remove or disable the code responsible for the failed composition.

Search for and remove these patterns from destination plaque parents:

```kotlin
fillMaxHeight()
fillMaxSize()
weight(...)
requiredHeight(...)
heightIn(min = largeValue)
matchParentSize()
```

`matchParentSize()` may remain only on decorative children after the parent has been constrained to the correct compact plaque size.

Remove:

* `LazyVerticalGrid` used for tablet destination placement.
* Three-column or two-row destination layouts on tablet landscape.
* Any cream destination container taller than its actual content.
* Any large blank avatar/profile background.
* Any destination card extending beneath bottom navigation.
* Any animal included inside destination-card measurement.
* Any `Spacer` used to force card height.
* Any enabled no-op callback.
* Any text baked into bitmap assets.

Run the app after this deletion pass. It is acceptable for the destinations to disappear temporarily. Do not preserve failed layout code for convenience.

## Root screen structure

Use one `Scaffold` and one scene `Box`.

```kotlin
@Composable
fun VillageHomeScreen(
    state: VillageHomeUiState,
    onDestinationClick: (SubjectId) -> Unit,
    onQuestClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigationClick: (VillageNavigationItem) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            VillageBottomNavigation(
                selectedItem = state.selectedNavigationItem,
                onItemClick = onNavigationClick,
            )
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .clipToBounds(),
        ) {
            VillageBackground()
            VillageDiscoveryAnimals(state.animals)
            VillageDestinationLayer(
                destinations = state.destinations,
                onDestinationClick = onDestinationClick,
            )
            VillageHud(
                state = state,
                onQuestClick = onQuestClick,
                onProfileClick = onProfileClick,
            )
        }
    }
}
```

Rules:

* Apply `Scaffold` padding exactly once.
* Do not draw destination plaques behind the bottom bar.
* Do not apply navigation-bar padding again inside the scene.
* Background z-index: `0f`.
* Animals z-index: `0.5f`.
* Destination plaques z-index: `1f`.
* HUD z-index: `2f`.
* Temporary celebration effects z-index: `3f`.
* Do not use a root `Column` to stack the village, quest, destinations, and navigation.

## Background

Keep the existing illustrated village background.

```kotlin
Image(
    painter = painterResource(R.drawable.village_background),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxSize(),
)
```

Requirements:

* Full bleed.
* Preserve aspect ratio.
* No cream frame around the village.
* No dashboard background.
* No `Canvas` replacement.
* No interactive text baked into the image.
* Decorative accessibility semantics only.

## Destination placement system

### Do not use a grid on tablet

The six plaques must be independent overlays in the scene `Box`.

Define normalized anchors:

```kotlin
@Immutable
data class DestinationAnchor(
    val centerX: Float,
    val centerY: Float,
)
```

Both values use the range `0f..1f`.

Place a plaque by measuring the scene and converting its normalized anchor to pixels. Clamp the result inside the safe scene bounds.

```kotlin
Modifier.layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = 0,
            minHeight = 0,
        ),
    )

    val desiredX = (constraints.maxWidth * anchor.centerX).roundToInt()
    val desiredY = (constraints.maxHeight * anchor.centerY).roundToInt()

    val x = (desiredX - placeable.width / 2)
        .coerceIn(edgeInsetPx, constraints.maxWidth - placeable.width - edgeInsetPx)

    val y = (desiredY - placeable.height / 2)
        .coerceIn(edgeInsetPx, constraints.maxHeight - placeable.height - edgeInsetPx)

    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeRelative(x, y)
    }
}
```

A custom parent layout is also acceptable.

Do not use fixed emulator pixels as the only positioning system.

### Initial anchor calibration

Start with these approximate normalized centers and tune them against the actual building entrances:

| Destination | Initial center X | Initial center Y |
|---|---:|---:|
| Story Tree | 0.17 | 0.49 |
| Bahay ng Kuwento | 0.49 | 0.48 |
| Number Market | 0.81 | 0.49 |
| Discovery Lab | 0.17 | 0.77 |
| Heritage Harbor | 0.49 | 0.77 |
| Kindness Corner | 0.81 | 0.77 |

These are starting coordinates, not automatic acceptance.

For each destination:

* Place the plaque beside or just below the entrance.
* Keep the nearest plaque edge approximately `8–20dp` from the entrance or its path.
* Do not cover a doorway.
* Do not cover a guide character.
* Do not cover a subject-defining prop.
* Do not cover an endemic animal.
* Keep the plaque’s visual center within approximately `24dp` of the intended signpost position.
* Tune each anchor individually.

## Uniform destination plaque

### Dimensions

At tablet landscape size:

```text
Preferred width: 176dp
Allowed width: 164–196dp
Preferred height: 72dp
Allowed height: 68–80dp
```

All six plaques must use the same width and height.

Maximum allowed difference between the tallest and shortest plaque:

```text
4dp
```

Do not allow labels to become wider based on destination-name length.

At compact widths:

```text
Width: available column width
Minimum height: 82dp
Maximum content-driven height: 116dp at 200% font scale
```

### Content

Each plaque contains only:

```text
Destination name
Subject and compact progress
Optional TODAY tab or lock state
```

Example:

```text
Number Market
Mathematics · 67%
```

Do not add lesson descriptions to destination plaques.

Do not place animals inside plaques.

Do not duplicate the destination name in a separate badge.

### Bamboo surface assembly

Do not restore invalid `.9.png` files.

Build `BambooPlaqueSurface` from standard assets:

* Sawali fill in the center.
* Horizontal bamboo rail at the top and bottom.
* Vertical bamboo rail at the left and right.
* Four rattan corner joints.
* Native Compose text above the decoration.

Suggested structure:

```kotlin
@Composable
fun BambooPlaqueSurface(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(width = 176.dp, height = 72.dp)
            .semantics {
                this.contentDescription = contentDescription
                if (!enabled) disabled()
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.sawali_fill),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 7.dp, vertical = 7.dp),
        )

        BambooRails()
        RattanCorners()

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(
                    start = 16.dp,
                    top = 11.dp,
                    end = 14.dp,
                    bottom = 10.dp,
                ),
            content = content,
        )
    }
}
```

Rules:

* The outer component, not its children, defines plaque size.
* `matchParentSize()` is allowed only for decoration inside the constrained plaque.
* Do not use `fillMaxSize()` on the plaque when the parent is the whole scene.
* Do not use a generic white `Card`, `Surface`, or `ElevatedCard` behind the bamboo frame.
* Use an opaque warm sawali center for reliable contrast.
* Keep text native, localized, and accessible.

### Typography

Destination name:

```text
Baloo 2 SemiBold
17sp
Ink #183B4A
Maximum one line at normal scale
```

Subject and progress:

```text
Nunito Bold
12–13sp
Subject color
```

At increased font scale:

* Permit the destination name to wrap to two lines.
* Expand the compact-layout plaque vertically.
* Do not shrink text below the specified base size.
* Do not use ellipsis to conceal a required destination name.

### Click behavior

The entire plaque is one click target.

Minimum semantic target:

```text
176 × 72dp tablet
full card width × at least 82dp compact
```

TalkBack example:

```text
Number Market, Mathematics, 67 percent complete, recommended today, button
```

Locked example:

```text
Kindness Corner, GMRC, opening soon, disabled
```

Do not create separate click actions for the progress bar, title, or decorative corners.

### States

Available:

* Normal bamboo surface.
* Subject-colored progress.
* Standard elevation or contact shadow.

Recommended:

* Small gold `TODAY` tab attached to the top-right rail.
* Subtle warm glow.
* No permanent size increase.
* No constant bouncing.

Completed:

* Native check icon.
* `Completed` text.
* Green state accent.

Locked:

* Native lock icon.
* `Opening soon` or explicit unlock requirement.
* Reduced saturation.
* Disabled click.
* Do not route to another subject.

## Profile HUD correction

The current profile section contains a large unintended cream block extending far below its visible content. This must be removed. 

Use:

```text
Width: 248–280dp
Height: 84–96dp
Top inset: 20–28dp
Start inset: 20–28dp
```

Content:

* Actual Maxine avatar.
* `Hi, Maxine!`
* Level.
* XP bar.
* Numeric XP.

Do not include:

* A blank panel below the content.
* A solid coral avatar placeholder.
* App version information.
* Build information.
* Large decorative empty areas.

Implementation:

```kotlin
BambooHudSurface(
    modifier = Modifier
        .widthIn(min = 248.dp, max = 280.dp)
        .wrapContentHeight(),
) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(...)
        Spacer(Modifier.width(10.dp))
        ProfileProgress(...)
    }
}
```

Prohibited:

```kotlin
.fillMaxHeight()
.weight(1f)
.height(400.dp)
.matchParentSize() // on the outer profile component
```

## Daily Quest correction

The current quest panel spans almost the entire screen and obscures the village. 

Tablet target:

```text
Width: 340–420dp
Height: 76–92dp
Position: below the profile HUD
Gap from profile: 10–14dp
```

Do not span the full viewport width.

Required content:

* Quest icon.
* `Daily Quest`.
* One-line objective.
* Compact progress.
* Small tactile Continue button.

Recommended layout:

```text
[Icon] [Daily Quest                         ]
       [Read a story · 3/5]       [Continue]
```

If the objective is too long:

* Wrap to a second line.
* Grow to no more than `108dp`.
* Do not expand horizontally across the village.
* Do not reduce text below accessibility size.

Use the same bamboo-and-sawali visual language as the plaques, but give the quest slightly greater width and hierarchy.

## Currency and streak HUD

Keep the upper-right indicators compact.

Requirements:

* Maintain at least `16dp` safe-edge clearance.
* Use one coherent bamboo, parchment, or warm-chip treatment.
* Do not use pure white pills.
* Keep icon and number readable.
* Do not let the HUD overlap the Number Market area.
* Combine currencies into one compact rail if three separate chips become visually noisy.

Target total width:

```text
240–310dp
```

Target height:

```text
44–52dp
```

## Bottom navigation

The bottom navigation currently overlaps the destination-card region. The corrected destination layer must stop above it. 

Requirements:

* Render navigation through `Scaffold.bottomBar`.
* Apply system navigation insets once.
* Height: approximately `64–76dp`, excluding system inset.
* Maintain at least `12–16dp` between the navigation top and any plaque.
* Keep four destinations: Profile, Achievements, Backpack, Parents.
* Every enabled item must perform a real action.
* Parent access must lead to a parent gate.
* Do not place destination plaques behind this surface.

## Endemic Philippine animals

Keep the animals as subtle discoveries.

Rules:

* Anchor animals to normalized scene coordinates.
* Do not include them in plaque content.
* Do not allow animals to change plaque size.
* Keep each animal approximately 1–3% of screen width.
* Maintain at least `8dp` clearance from labels and controls.
* Keep at least 90% of each animal visible.
* Do not place an animal on a cream or sawali plaque.
* Do not add labels to the homepage.
* Decorative animals should normally have no accessibility node.
* Use only genuine transparent PNGs.
* Reject any asset containing a baked checkerboard.

The latest screenshot shows animals visually attached to or overlapping large cards. Reposition them into vegetation, roof lines, paths, or shoreline after the cards are corrected. 

## Responsive behavior

### Expanded landscape

At widths of `840dp` and above with sufficient height:

* Use the illustrated overlay composition.
* Use six normalized plaque anchors.
* Keep all six destinations visible simultaneously.
* Do not use a grid.

### Medium and compact layouts

At widths below `840dp`:

* Do not squeeze the complete village map until labels become unreadable.
* Retain an illustrated village header.
* Place the Daily Quest below the header.
* Use six compact illustrated destination cards in a two-column grid.
* Use one column when width or font scale requires it.
* Allow vertical scrolling.
* Keep all six subjects equally prominent.
* Continue using bamboo/sawali components.
* Do not use generic white Material cards.

### Font scaling

Test at:

```text
100%
130%
200%
```

At 200%:

* Permit vertical scrolling.
* Permit plaques in compact layouts to grow.
* Keep every destination name readable.
* Do not clip buttons.
* Do not reduce touch targets.
* Do not reduce font size to force the original dimensions.

## Six-subject parity

Exactly six destinations must remain:

| Destination | Subject |
|---|---|
| Story Tree | English |
| Bahay ng Kuwento | Filipino |
| Number Market | Mathematics |
| Discovery Lab | Science |
| Heritage Harbor | Philippine History |
| Kindness Corner | GMRC |

All six must use:

* The same plaque dimensions.
* The same padding.
* The same information hierarchy.
* The same click-target size.
* The same progress structure.
* The same typography.
* The same state system.
* Comparable entrance proximity.

Only subject accent color, text, progress, and state may differ.

## Implementation sequence

DeepSeek must implement in this order.

### Step 1: preserve a baseline

Capture the current failed build at:

```text
1280 × 800
1333 × 985 emulator window
600 × 960
360 × 800
```

### Step 2: remove expanding surfaces

Delete the destination grid and all height-expansion modifiers.

Run the build.

Confirm no destination cream rectangles remain.

### Step 3: correct profile and quest

Implement intrinsically measured profile and quest surfaces.

Run the app.

Confirm:

* No empty profile block.
* No full-width quest.
* The village center is visible.

### Step 4: implement one plaque

Implement Story Tree only.

Confirm:

* `176 × 72dp`.
* Native text.
* Correct bamboo assembly.
* Full plaque click target.
* No empty space.
* Correct entrance anchoring.

Do not implement the remaining five until Story Tree passes visual review.

### Step 5: replicate the component

Render all six destinations from one data-driven component.

Do not copy and paste six separate plaque implementations.

### Step 6: tune anchors

Tune all six placements against the actual background.

Record final normalized coordinates in one source file.

### Step 7: reposition animals

Move animals into scene micro-habitats after plaque positions are final.

### Step 8: verify bottom navigation and insets

Test gesture navigation and three-button navigation.

### Step 9: run accessibility and font-scale checks

Verify native semantics and reachability.

### Step 10: capture final screenshot matrix

Do not claim completion without screenshots.

## Required code inspection

The final pull request must contain no destination-layer occurrence of:

```text
LazyVerticalGrid
fillMaxHeight
requiredHeight
weight
```

Exceptions require a written explanation.

The following must exist:

```text
BambooPlaqueSurface
DestinationAnchor
VillageDestinationLayer
VillageHud
DailyQuestPanel
PlayerProfilePanel
```

All visible strings must come from resources.

All destinations must come from one state list.

## Screenshot acceptance criteria

### Primary tablet screenshot

At `1280 × 800dp`:

* Exactly six destination plaques are visible.
* Every plaque is `164–196dp` wide.
* Every plaque is `68–80dp` high.
* Height variance is no more than `4dp`.
* No plaque contains more than `12dp` of unexplained trailing or bottom empty space.
* Every plaque is within `8–20dp` of its associated entrance or path marker.
* No plaque overlaps another plaque.
* No plaque overlaps the HUD.
* No plaque overlaps bottom navigation.
* No doorway is obscured.
* No guide character is obscured.
* No animal is placed on a plaque.
* No opaque cream UI region is wider than `420dp`.
* At least 70% of the village remains unobscured.
* Daily Quest is no wider than `420dp`.
* Profile is no wider than `280dp`.
* No blank profile extension exists.
* No full-width quest panel exists.
* No large grid-card structure is visible.

### Compact screenshots

Capture:

```text
360 × 800
412 × 915
600 × 960
```

Pass when:

* All six subjects remain reachable.
* No horizontal overflow exists.
* No text is clipped.
* No required text is ellipsized.
* Every target is at least `48 × 48dp`.
* Bottom content remains above navigation insets.
* Scrolling reaches the final destination.
* The visual system remains bamboo-and-sawali.

### Accessibility screenshots and tests

Capture:

```text
1280 × 800 with TalkBack focus on Number Market
360 × 800 at 200% font scale
600 × 960 at 200% font scale
```

Pass when:

* Each plaque is one logical accessibility node.
* Destination, subject, progress, state, and button role are announced.
* Locked destinations are announced as disabled.
* Decorative background and animals do not pollute traversal.
* Focus indication is visible.
* Focus order follows the visual reading order.
* Text contrast meets `4.5:1`.
* Large text and meaningful icons meet `3:1`.

## Automated assertions

Add Compose UI tests that verify:

```kotlin
onNodeWithTag("destination_english").assertExists().assertHasClickAction()
onNodeWithTag("destination_filipino").assertExists().assertHasClickAction()
onNodeWithTag("destination_mathematics").assertExists().assertHasClickAction()
onNodeWithTag("destination_science").assertExists().assertHasClickAction()
onNodeWithTag("destination_history").assertExists().assertHasClickAction()
onNodeWithTag("destination_gmrc").assertExists().assertIsNotEnabled()
```

Also assert:

* Exactly six destination nodes.
* Every enabled node opens the correct subject.
* No enabled node has an empty callback.
* GMRC does not route to another subject.
* Plaque bounds remain above bottom navigation.
* Plaque dimensions are uniform within tolerance.
* Daily Quest and profile bounds stay within their maximum dimensions.

## Build commands

Run and report exact output for:

```bash
cd android
./gradlew :feature-child-home:compileDebugKotlin
./gradlew :feature-child-home:testDebugUnitTest
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Do not report only “build passed.”

## Automatic rejection conditions

Reject the implementation if any of these remain:

* Large cream destination cards.
* A three-column tablet dashboard.
* Mostly empty plaque interiors.
* A full-width Daily Quest.
* An empty profile extension.
* Labels far from their buildings.
* Labels covering doors or guide characters.
* Plaques behind bottom navigation.
* Different plaque sizes for different subjects.
* Generic white Material cards.
* Invalid NinePatch assets.
* Text baked into imagery.
* Animals sitting on cards.
* Fewer than six destinations.
* Enabled no-op controls.
* Incorrect subject routing.
* A compiling build without the required screenshot matrix.

## Definition of done

This correction is complete only when:

1. The illustrated village is once again the dominant visual.
2. Six compact bamboo signposts are visibly attached to their buildings.
3. All six signposts use one uniform component and dimensions.
4. The profile HUD contains no empty extension.
5. Daily Quest is compact and localized to the upper-left.
6. Bottom navigation obstructs no content.
7. Endemic animals appear as subtle environmental discoveries.
8. All interactive text remains native Compose.
9. Every route and disabled state is truthful.
10. Tablet, compact, font-scale, inset, and accessibility checks pass.
11. Build and tests pass.
12. The final screenshot no longer resembles a card grid.

## Required DeepSeek response

DeepSeek must return:

* Files changed.
* Explanation of the original expansion bug.
* Final destination-anchor table.
* Before and after screenshots.
* Complete screenshot matrix.
* Build command output.
* Test command output.
* Accessibility results.
* Pass/fail copy of every acceptance criterion.
* Remaining known differences.

Do not return only a code summary or compilation report.


---

## Sources

- [image.png](https://purestorage-be.glean.com/api/v1/downloadchatfile/a88fa185a6284f5d9a85336a9e44d659)
