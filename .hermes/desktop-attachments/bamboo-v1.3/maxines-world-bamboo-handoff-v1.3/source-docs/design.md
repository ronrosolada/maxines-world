
# Maxine’s World Design System

## 1. Purpose

This document defines the visual language, user-interface rules, character system, animation principles, accessibility requirements, and asset-production standards for Maxine’s World.

It is the design source of truth for designers, illustrators, animators, Android developers, and implementation agents.

The application should feel:

* Warm
* Curious
* Safe
* Playful
* Encouraging
* Animal-filled
* Cat-centered
* Appropriate for an eight-year-old
* More sophisticated than a preschool application

## 2. Design Principles

### Learning remains visually dominant

The learning objective, prompt, activity, and feedback must remain clearer than characters, scenery, rewards, or decorative animation.

### Animals support comprehension

Animals should:

* Introduce lessons
* Demonstrate interactions
* Provide hints
* React to effort
* Represent subject areas
* Help communicate progress

Animals must not crowd controls or distract during reading and assessment.

### The village creates continuity

Lessons occur in connected locations rather than unrelated screens. Progress improves and expands the village.

### Errors feel recoverable

Incorrect responses should produce:

* A calm character reaction
* A specific clue
* A visual demonstration
* A retry opportunity

Avoid red full-screen failure states, buzzers, shame, or loss of rewards.

### Child and parent experiences are related but distinct

The child experience uses illustrated environments, characters, and large controls. The parent experience uses cleaner cards, restrained decoration, and stronger information density.

## 3. Visual Reference Board

![Maxine’s World character and visual identity sheet](assets/graphics/character-identity-sheet.png)

*Figure 1: Primary reference for characters, expressions, subject icons, rewards, and the rounded illustration style.*

![Maxine’s World child home screen](assets/graphics/child-village-home.png)

*Figure 2: Reference for the child village, subject destinations, Daily Quest, and layered animal scenery.*

![Maxine’s World progression map](assets/graphics/learning-progression-map.png)

*Figure 3: Reference for world progression, unlockable destinations, paths, and location states.*

![Maxine’s World multiplication lesson](assets/graphics/math-lesson-equal-groups.png)

*Figure 4: Reference for Mathematics activities, draggable objects, answer controls, and guide placement.*

![Maxine’s World English lesson](assets/graphics/english-lesson-main-idea.png)

*Figure 5: Reference for illustrated stories, read-aloud controls, and comprehension activities.*

![Maxine’s World Science investigation](assets/graphics/science-lesson-plant-growth.png)

*Figure 6: Reference for virtual investigations, predictions, variables, notebooks, and observation tools.*

![Maxine’s World parent dashboard](assets/graphics/parent-dashboard.png)

*Figure 7: Reference for the cleaner adult interface and restrained use of animal decoration.*

Generated images are conceptual references. Recreate text and interactive controls natively rather than embedding screenshots into the application.

## 4. Brand Identity

### Working name

Maxine’s World

### Tagline

Learn. Explore. Grow.

### Logo structure

The primary logo should contain:

* A rounded Maxine’s World wordmark
* A paw-print detail
* A small village or nature element
* Optional tagline
* A silhouette readable at small sizes

### Logo variants

Produce:

* Full horizontal logo
* Compact stacked logo
* App icon
* Single-color mark
* Dark-background version
* Light-background version
* Small watermark

### App icon

The approved app icon uses Maxine’s orange-tabby face as the focal mark, framed by subtle bamboo and village cues. Maxine must wear bubblegum-pink glasses, a pink backpack, and a pink necktie. These three pink accessories are intentional identity features and must remain consistent in every launcher export.

Requirements:

* No text
* Maxine remains the dominant silhouette
* Pink glasses, pink backpack, and pink necktie remain visible at launcher size
* Bamboo and village details remain secondary and do not crowd the face
* Strong silhouette
* Limited micro-detail
* Clear at 48 pixels
* Adaptive Android foreground and background layers
* Essential facial features and pink accessories remain inside the adaptive-icon safe zone
* Verify circle, squircle, rounded-square, and teardrop masks
* Supply a separate monochrome themed-icon layer; do not automatically flatten the full-color artwork into monochrome

## 5. Character System

### Primary cast

| Character | Appearance | Design role |
|---|---|---|
| Milo | Orange tabby, teal backpack | Main guide and Mathematics companion |
| Niko | Gray cat, glasses or goggles | Science companion |
| Mira | Calico cat, book or scarf | English and Filipino companion |
| Lakan | Philippine forest cat, heritage satchel | Philippine History companion |
| Ollie | Owl with small scholar accessory | Review guide |
| Shelly | Green pawikan or turtle | Persistence guide |
| Duke | Friendly aspin with blue scarf | Teamwork guide |
| Tilly | Otter with discovery pouch | Investigation guide |
| Poppy | Rabbit with flower accessory | Practice guide |
| Flynn | Fox with puzzle satchel | Strategy guide |

The launcher-icon character is Maxine and uses the approved pink glasses, backpack, and necktie. In lesson and village-guide contexts, preserve the repository’s established character names and do not rename files or routes solely to match old concept-sheet captions. If character naming conflicts remain, resolve them in one central character manifest rather than duplicating identities across screens.

### Character proportions

* Head: approximately 40–45% of total height
* Eyes: expressive but not oversized to preschool proportions
* Paws and hands: large enough to demonstrate gestures
* Limbs: simple and readable during animation
* Accessories: limited to one or two recognizable items
* Silhouette: identifiable without internal detail

### Required poses

Every primary guide needs:

* Neutral
* Greeting
* Pointing left
* Pointing right
* Thinking
* Explaining
* Celebrating
* Encouraging
* Surprised
* Gentle correction
* Reading
* Listening
* Walking
* Idle
* Reduced-motion static pose

### Expression rules

Positive feedback:

* Open posture
* Gentle smile
* Small upward movement
* Bright eyes

Incorrect response:

* Thoughtful expression
* No tears or distress
* No disappointment aimed at the learner
* Character should look toward the clue or learning object

### Animal density

| Screen | Recommended visible animals |
|---|---:|
| Village home | 8–14 |
| Progression map | 12–25 |
| Lesson introduction | 2–5 |
| Active question | 1–3 |
| Assessment | 1–2 |
| Parent dashboard | 0–3 |
| Celebration | 4–10 |

Keep only one animated focal character during reading or problem-solving.

## 6. Subject Worlds

### English: Story Tree

* Primary color: story purple
* Guide: Mira
* Visual motifs: books, lanterns, treehouses, paper, speech bubbles
* Main interaction tone: storytelling and investigation

### Filipino: Bahay ng Kuwento

* Primary color: warm coral
* Guide: Mira
* Visual motifs: bahay-inspired reading house, woven patterns, story cards, speech ribbons
* Main interaction tone: communication, storytelling, and cultural familiarity

Avoid using sacred, ceremonial, or culturally specific motifs as generic decoration without review.

### Mathematics: Number Market

* Primary color: sky blue
* Guide: Milo
* Visual motifs: market stalls, counters, baskets, blocks, clocks, tiles, graphs
* Main interaction tone: building, grouping, measuring, and solving

### Science: Discovery Lab

* Primary color: leaf green
* Guide: Niko
* Visual motifs: plants, magnifiers, notebooks, safe laboratory tools, habitats
* Main interaction tone: prediction, observation, testing, and explanation

### Philippine History: Heritage Harbor

* Primary color: heritage gold
* Guide: Lakan
* Visual motifs: maps, timelines, community albums, landmarks, boats, archival objects
* Main interaction tone: discovery, comparison, storytelling, and preservation

Clearly distinguish verified history, oral history, legends, and fictional narrative.

## 7. Color System

### Core palette

| Token | Hex | Usage |
|---|---|---|
| `village_teal` | `#087F83` | Navigation, parent interface, brand anchor |
| `coral` | `#F47C6B` | Filipino, warmth, secondary actions |
| `sunshine_gold` | `#F5B82E` | Rewards, History, highlights |
| `leaf_green` | `#66A83E` | Science and success |
| `story_purple` | `#7653B5` | English and imaginative content |
| `sky_blue` | `#3C9DDB` | Mathematics and information |
| `ink` | `#183B4A` | Primary text |
| `cream` | `#FFF7E8` | Child-card surfaces |
| `white` | `#FFFFFF` | Clean surfaces |
| `success` | `#2F9E62` | Correct and completed states |
| `warning` | `#D98716` | Attention states |
| `review` | `#D9534F` | Needs-review indicators |

### Color rules

* Never communicate correctness through color alone.
* Pair color with icons, labels, or shape.
* Use cream rather than pure white for large child-facing surfaces.
* Reserve saturated colors for actions and focal elements.
* Use darker teal and ink for adult-facing navigation.
* Verify text and control contrast against WCAG AA targets.

### Subject tokens

```yaml
subjects:
  english:
    primary: "#7653B5"
    surface: "#F1EBFA"
  filipino:
    primary: "#D96555"
    surface: "#FCEBE7"
  mathematics:
    primary: "#218CC8"
    surface: "#E6F4FC"
  science:
    primary: "#57943B"
    surface: "#EDF7E8"
  history:
    primary: "#B87916"
    surface: "#FFF3D7"
```

## 8. Typography

### Typeface requirements

Use a rounded, highly readable sans-serif family supporting:

* English
* Filipino
* Numerals
* Mathematical symbols
* Diacritics
* Android font scaling

Avoid novelty fonts for paragraphs or questions.

### Type scale

| Style | Child size | Parent size |
|---|---:|---:|
| Display | 36–44sp | 30–36sp |
| Screen title | 28–34sp | 24–30sp |
| Section heading | 22–28sp | 18–22sp |
| Question | 24–32sp | Not applicable |
| Body | 18–22sp | 14–18sp |
| Button | 18–22sp | 14–18sp |
| Caption | 15–18sp | 12–14sp |

### Typography rules

* Keep child instructions under two short sentences.
* Use sentence case.
* Avoid all-caps instructions.
* Do not use condensed type.
* Allow Android text scaling without clipping.
* Use tabular numerals in reports and progress cards.
* Render mathematical notation through native text where practical.

## 9. Shape and Elevation

```yaml
radius:
  small: 12dp
  medium: 18dp
  large: 24dp
  hero: 32dp

touch_target:
  minimum: 48dp
  preferred_child: 56dp
  primary_child: 64dp

elevation:
  resting: 2dp
  raised: 6dp
  modal: 12dp
```

Child controls should appear soft and tactile. Parent controls may use flatter surfaces and smaller radii.

Avoid excessive inner shadows, glass effects, or texture that reduces clarity.

## 10. Layout System

### Spacing scale

```text
4dp, 8dp, 12dp, 16dp, 24dp, 32dp, 48dp, 64dp
```

### Tablet child layout

Use a 12-column conceptual grid:

* Outer safe margin: 24–32dp
* Main activity: 7–8 columns
* Guide or supporting panel: 3–4 columns
* Gap: 16–24dp

### Phone child layout

* Stack supporting content below the activity.
* Keep primary prompt above the fold.
* Avoid more than four answer choices visible simultaneously.
* Use scrolling only when the activity naturally requires it.

### Parent layout

* Tablet: navigation rail and two- or three-column content
* Phone: bottom navigation and stacked cards
* Maximum readable text width: approximately 720dp
* Charts must include labels and summaries

## 11. Child Navigation

Primary navigation destinations:

* Village
* Daily Quest
* Backpack
* Achievements
* Profile
* Parent Gate

Subject locations should be entered through the village rather than a dense academic menu.

### Navigation rules

* Always provide a visible home or back control.
* Preserve lesson state when interrupted.
* Confirm only destructive exits.
* Do not hide essential navigation behind gestures.
* Parent Gate must require a PIN, password, or biometric action.

## 12. Component Library

### Primary child button

* Height: 56–64dp
* Large rounded corners
* High-contrast label
* Optional leading icon
* Pressed and disabled states
* Short scale or bounce response
* No text smaller than 18sp

### Answer card

States:

* Default
* Focused
* Selected
* Correct
* Needs another try
* Disabled

Incorrect selections remain readable while the clue appears.

### Daily Quest card

Contains:

* Subject icon
* Activity title
* Estimated minutes
* Progress
* Reward preview
* Start or continue action

### Subject destination

Contains:

* Illustrated location
* Subject icon and label
* Guide character
* Progress state
* Lock or availability state
* Optional recommended indicator

### Parent progress card

Contains:

* Subject
* Percentage or mastery summary
* Completed lesson count
* Plain-language interpretation
* Details action

### Hint control

* Consistent lightbulb symbol
* Text label for early learners
* Never positioned next to destructive navigation
* Show hints in progressively stronger levels

### Audio control

* Consistent speaker icon
* Replay permitted
* Visually indicate playback state
* Captions and transcript available

## 13. Lesson Screen Structure

Recommended order:

1. Subject and lesson identity
2. Progress indicator
3. Short objective or prompt
4. Main interactive workspace
5. Answer or action controls
6. Hint and audio
7. Guide character
8. Pause or exit

The guide must not obscure the workspace.

### Mathematics layout

![Mathematics lesson layout reference](assets/graphics/math-lesson-equal-groups.png)

*Figure 8: Mathematics layout with concrete objects, symbolic representation, answers, and guide characters.*

Use concrete-pictorial-symbolic progression:

1. Manipulable objects
2. Visual grouping or model
3. Mathematical symbols
4. New transfer problem

### Reading layout

![Reading lesson layout reference](assets/graphics/english-lesson-main-idea.png)

*Figure 9: Reading layout with illustration, highlighted text, audio, and comprehension workspace.*

Requirements:

* Adjustable narration
* Synchronized word or sentence highlighting
* Clear separation between story and questions
* Maximum readable line length
* Optional vocabulary definitions

### Science layout

![Science investigation layout reference](assets/graphics/science-lesson-plant-growth.png)

*Figure 10: Investigation layout with variables, predictions, observations, and time progression.*

Science activities should visually separate:

* Question
* Prediction
* Variables
* Test
* Observation
* Evidence
* Conclusion

### History layout

Use:

* Illustrated maps
* Timelines
* Before-and-after comparisons
* Object investigation
* Community albums
* Source-type labels

Every source card should identify itself as:

```text
Verified historical information
Oral-history account
Legend or traditional story
Fictional learning story
Reconstruction or illustration
```

## 14. Feedback Design

### Correct response

* Confirm what was correct.
* Show a small success animation.
* Connect the answer to the learning objective.
* Continue automatically only after the learner can see the result.

Example:

```text
Correct! Four groups of three make twelve.
```

### Incorrect response

First attempt:

```text
Let’s look again. Count the fish in every basket.
```

Second attempt:

* Demonstrate one part.
* Reduce irrelevant options.
* Allow another response.

Final support:

* Show the worked solution.
* Ask the learner to complete a similar, easier item.
* Mark the skill for review rather than punishment.

## 15. Motion Design

### Motion categories

| Category | Duration |
|---|---:|
| Button response | 80–160ms |
| Card transition | 180–280ms |
| Screen transition | 250–400ms |
| Character reaction | 400–900ms |
| Lesson explanation | Variable and skippable |
| Celebration | 1–3 seconds |

### Character state machine

```text
idle
greet
explain
point
listen
think
encourage
celebrate
gentle_correct
sleep
```

### Reduced-motion behavior

* Replace bouncing with fades.
* Replace path travel with location-to-location dissolve.
* Use static character poses.
* Disable background parallax.
* Retain all instructional information.

## 16. Sound Design

### Sound categories

* Soft interface taps
* Subject-location ambience
* Character vocal reactions
* Narration
* Correct-response cue
* Gentle retry cue
* Reward cue
* Village ambience

### Sound rules

* Never use harsh failure sounds.
* Narration must remain intelligible over music.
* Music and effects need separate volume controls.
* All speech requires transcripts.
* Do not autoplay prolonged audio after returning to the app.
* Allow complete muting without losing instructions.

## 17. Illustration Production

### Recommended asset formats

| Asset | Format |
|---|---|
| Character animation | Rive |
| Small UI animation | Lottie |
| Icons | SVG or Android vector |
| Backgrounds | WebP |
| Complex illustrations | WebP or PNG source master |
| Thumbnails | WebP |
| Audio | AAC or Opus where supported |
| Bamboo UI surfaces | Native Compose container assembled from standard PNG primitives |
| Endemic animal discoveries | True-alpha RGBA PNG or lossless WebP with alpha |

### Background layers

Create separate layers for:

1. Distant environment
2. Buildings
3. Large vegetation
4. Interactive locations
5. Character layer
6. Foreground decoration
7. UI overlay

This supports parallax, responsive composition, and efficient reuse.

### Asset naming

```text
character_milo_idle.riv
character_mira_reading.riv
character_lakan_pointing.riv
location_story_tree_day.webp
location_bahay_kuwento_day.webp
location_number_market_day.webp
location_discovery_lab_day.webp
location_heritage_harbor_day.webp
icon_subject_filipino.svg
badge_heritage_keeper.svg
```

### Required resolutions

Produce source masters at two to four times the intended display size. Export Android-appropriate density assets or use scalable vectors where possible.

Keep essential content inside a central safe area to support multiple screen ratios.

## 18. Parent Experience

The parent interface should be calmer and more utilitarian than the child interface.

![Parent dashboard reference](assets/graphics/parent-dashboard.png)

*Figure 11: Parent design direction with structured cards, restrained animal illustration, and readable progress data.*

### Parent design rules

* Use animal art as accents rather than backgrounds.
* Prefer plain-language summaries over educational jargon.
* Never rely only on percentages.
* Explain why a skill needs review.
* Show active learning time separately from total app-open time.
* Include chart labels and textual summaries.
* Keep assignment actions prominent.
* Clearly identify unsynchronized progress.

## 19. Accessibility

Required:

* WCAG AA target contrast
* Minimum 48dp controls
* Preferred child control size of 56dp or larger
* Android font scaling
* TalkBack labels
* Logical focus order
* Captions and transcripts
* Reduced motion
* Color-independent status
* Alternatives to drag-and-drop
* No essential timed activity
* Replayable instructions
* Left-handed interaction testing
* Large-screen and phone testing

Decorative animals must be hidden from accessibility focus unless they communicate information.

## 20. Localization

Design all controls for expansion of at least 30% beyond the English label length.

Support:

* English
* Filipino
* Future regional-language content
* Mixed-language proper nouns
* Philippine currency
* Metric units
* Locale-aware dates and times

Do not place important text permanently inside illustrations.

## 21. Android Compose Mapping

```kotlin
object MaxinesColors {
    val VillageTeal = Color(0xFF087F83)
    val Coral = Color(0xFFF47C6B)
    val SunshineGold = Color(0xFFF5B82E)
    val LeafGreen = Color(0xFF66A83E)
    val StoryPurple = Color(0xFF7653B5)
    val SkyBlue = Color(0xFF3C9DDB)
    val Ink = Color(0xFF183B4A)
    val Cream = Color(0xFFFFF7E8)
}
```

Recommended Compose components:

```text
MaxinesPrimaryButton
MaxinesAnswerCard
MaxinesQuestCard
MaxinesSubjectDestination
MaxinesProgressBar
MaxinesRewardChip
MaxinesGuideBubble
MaxinesAudioButton
MaxinesHintButton
MaxinesParentMetricCard
MaxinesSkillStatusChip
MaxinesLessonScaffold
```

Every component should include:

* Enabled state
* Disabled state
* Pressed state
* Focus state
* Accessibility semantics
* Font-scale test
* Phone and tablet previews

## 22. Design QA Checklist

Before accepting a screen:

* Is the learning objective visually clear?
* Is there one obvious primary action?
* Are all touch targets large enough?
* Can the screen work without animation?
* Can the screen work without sound?
* Does TalkBack follow a logical sequence?
* Does text remain readable at increased scale?
* Are animals supporting rather than obstructing learning?
* Is incorrect feedback calm and actionable?
* Are status meanings available without color?
* Are cultural elements reviewed for accuracy?
* Are all controls native rather than embedded in artwork?
* Does the layout work on phone and tablet?
* Is the parent experience clear without educational expertise?

## 23. First Production Asset Sprint

Create these assets first:

1. Primary logo and Android app icon
2. Milo character rig and required expressions
3. Mira, Niko, and Lakan static lesson poses
4. Five subject icons
5. Village home background layers
6. Five subject-location illustrations
7. Daily Quest card assets
8. Answer-card states
9. Hint, audio, pause, home, and parent icons
10. Stars, paw coins, and five pilot badges
11. Correct, retry, and completion animations
12. Parent-dashboard decorative assets

The first design milestone is complete when the five pilot lessons can be implemented using a consistent component library without creating one-off controls.

---

## 24. Design Iteration v2 — Native Screen Reference

The seven original PNGs are AI concept art with UI text baked into the illustration. This iteration replaces that baked UI with a **native, implementation-ready screen set** that a developer or implementation agent can translate directly into Jetpack Compose.

**Reference file:** `Maxines World Screens.dc.html` (open in a browser). It contains seven tablet-landscape screens at the production canvas size (1280 × 800): Village Home, World Map, Math lesson (equal groups), English lesson (main idea), Science investigation, Parent Dashboard, and a Reward Moment.

### 24.1 Core principle applied

The illustration is demoted to an **ambient art layer** (with a scrim, or blur + cream scrim on lesson screens) so the native learning workspace and controls are unmistakably the real, dominant interface. On lesson screens a cream `#FFF7E8` workspace panel floats over the softened scene — this is the concrete expression of “learning remains visually dominant.” In production, replace each backdrop with the named `location_*_day.webp` asset; the native layer sits on top unchanged.

### 24.2 Typography decision

* **Display / headings / numerals / buttons:** **Baloo 2** (weights 600–800) — rounded, tactile, kid-appropriate without being preschool.
* **Body / instructions / Filipino text / parent UI:** **Nunito** (weights 600–900) — highly readable, full Latin diacritic coverage for Filipino, tabular figures for parent reports.

Both are open-license and android-shippable. Neither is on the house avoid-list. This pairing supersedes the generic “rounded sans” placeholder in §8.

### 24.3 Improvements over the concept art (delight-focused)

* **Tactile buttons:** every primary control uses a chunky bottom-shadow (`box-shadow: 0 5–6px 0 <darker tint>`) so it reads as physically pressable — the child-facing “bounce” affordance. Maps to `MaxinesPrimaryButton` pressed/disabled states.
* **Learning-first hierarchy:** the objective banner and question (e.g. “4 groups of 3 = ?”) are the largest elements on the lesson screens; guide characters and scenery never sit above them.
* **Color + icon + label, never color alone:** correct = green tile **plus** a check badge; locations carry ✓ / 🔒 glyphs and text; skill status on the parent side reads “2 to review”, not just a red bar. Satisfies §7 and §19.
* **One focal companion per activity:** a single native guide bubble (“Niko says…”, “Milo says…”) carries the hint voice; the rest of the animals stay in the ambient art. Honors the animal-density table in §5.
* **Step progress dots** on lesson headers (done ✓ / current / upcoming) give an 8-year-old a clear, countable sense of “how far to the reward.”
* **Reward moment** is short, celebratory, and skippable (Village / Next buttons) and shows exactly what was earned: stars, paw coins, and one named badge — no randomized or paid rewards (§14 safeguards).
* **Restrained parent surface:** teal navigation rail, flat white cards, labeled bar chart with a plain-language summary line (“Sat was her best day”), active-learning-time separated from screen-time. Follows §18.

### 24.4 Component → screen map

| Component (`core-design-system`) | Seen on |
|---|---|
| `MaxinesPrimaryButton` | Continue, Play, Check, Next lesson |
| `MaxinesQuestCard` | Village Home Daily Quest |
| `MaxinesSubjectDestination` | Village Home + World Map location pins (unlocked / current / locked states) |
| `MaxinesProgressBar` | Level XP, subject progress, screen-time |
| `MaxinesAnswerCard` | Math answer tiles, English detail cards (default / selected / correct) |
| `MaxinesGuideBubble` | Niko / Milo / Mira hint bubbles |
| `MaxinesHintButton`, `MaxinesAudioButton` | Lesson headers |
| `MaxinesRewardChip` | Reward Moment stars / coins / badge |
| `MaxinesParentMetricCard`, `MaxinesSkillStatusChip` | Parent Dashboard |
| `MaxinesLessonScaffold` | Header + progress + workspace + controls frame on every lesson |

### 24.5 Notes for the implementing agent

* Emoji glyphs in the reference (🐟 ☀️ 💧 🐱 ⭐ 🐾) are **placeholders** for produced SVG/Rive assets (`icon_subject_*`, counters, coins, character rigs). Do not ship emoji.
* All hex values, radii, spacing, and touch-target sizes come from §7, §9, §10 — the reference uses them verbatim.
* Recreate every control natively (§2 of the implementation handoff); the illustration is a background layer only.

### 24.6 App icon

Use the approved bamboo-framed Maxine launcher icon. Maxine is an orange tabby wearing bubblegum-pink glasses, a pink backpack, and a pink necktie. The face is the primary mark; bamboo, sky, and village details are secondary. There is no text.

The prior Milo launcher exports in the original design archive are superseded. Do not copy them into the production app and do not recolor them automatically. Export a fresh adaptive-icon set from the approved full-resolution icon master:

| Required file | Use | Acceptance gate |
|---|---|---|
| `app_icon_maxine_1024.png` | Play Store source master | 1024×1024, opaque, no mask baked in |
| `ic_launcher_foreground.png` | Adaptive foreground | Transparent outside artwork; face and pink accessories inside safe zone |
| `ic_launcher_background.xml` or `.png` | Adaptive background | Simple teal/sky field; no essential detail |
| `ic_launcher_monochrome.xml` | Android 13+ themed icon | Single-color silhouette tested on light and dark themes |
| `app_icon_maxine_48.png` | Legibility proof | Glasses, eyes, and necktie remain distinguishable |

Build `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` from foreground and background layers. Add the monochrome layer where supported. Test circle, squircle, rounded-square, and teardrop masks in Android Studio Image Asset Studio. The mask must not crop the ears, glasses, necktie, or recognizable backpack edge.

Do not claim the icon task complete until the approved source artwork has been exported into all required Android layers. The handoff package intentionally does not substitute the older generic icon files.

## 25. Endemic Philippine Animal Discovery Details

### 25.1 Intent

The Village Home includes six very small endemic Philippine animals as optional visual discoveries. They should feel like a “hidden Mickey” detail: noticeable only after a child studies the village, rewarding curiosity without becoming a task, badge, advertisement, or competing focal point.

These animals are ambient world-building. They do not replace Milo, Mira, Niko, Lakan, or Duke as learning guides.

### 25.2 Required animal set and subject-zone balance

Include exactly one animal near each of the six equally prominent subject destinations:

| Subject zone | Animal | Required identifying features | Natural placement |
|---|---|---|---|
| English / Story Tree | Philippine eagle | Dark brown body, pale belly, shaggy cream-brown crest, large hooked bill | Perched high in a distant tree, visually separated from the Story Tree label |
| Filipino / Bahay ng Kuwento | Philippine tarsier | Tiny brown-gray body, very large round eyes, broad ears, long fingers | Clinging to bamboo or a shaded branch beside the building |
| Mathematics / Number Market | Tamaraw | Small stocky dark buffalo, short sturdy legs, short close V-shaped horns | Partly concealed in tall grass beyond the market stalls |
| Science / Discovery Lab | Philippine colugo | Mottled brown-gray coat, large eyes, folded gliding membrane | Clinging vertically to a shaded tree trunk near the laboratory vegetation |
| Philippine History / Heritage Harbor | Palawan peacock-pheasant | Dark compact bird, restrained blue-green eyespots, crest, white facial markings | Walking in leaf litter or beside rocks near the harbor path |
| GMRC / Kindness Corner | Visayan warty pig | Small dark bristly body, pale snout band, short legs | Peeking from low shrubs at the outer edge of the destination |

Do not assign multiple hidden animals to one subject while leaving another subject without one. The animal details must reinforce the six-subject parity rule.

### 25.3 Visual scale and hierarchy

At the 1280 × 800 tablet reference size:

* Each animal occupies approximately 1–3% of screen width.
* Suggested rendered widths are 28–52dp; the eagle may reach 56dp only when placed high in the scene.
* No animal may be larger than a destination icon, destination plaque, guide character, quest icon, or primary action.
* Animals use slightly lower contrast and saturation than guide characters.
* Partially conceal 15–35% of ground animals behind leaves, grass, rocks, or bamboo when this remains anatomically plausible.
* Do not add glows, outlines, sparkles, badges, arrows, labels, speech bubbles, or pulsing animation.
* A child should notice zero to two animals on first glance and discover the others by inspecting the scene.

### 25.4 Placement contract

Use the normalized starting placements in `ENDemic_ANIMAL_PLACEMENT.json`. Coordinates are measured from the top-left of the complete village scene after `ContentScale.Crop` has been resolved.

Placement requirements:

* Keep every animal outside destination-plaque bounds and expanded 48dp click targets.
* Keep every animal outside the Profile, Daily Quest, counters, menu, and bottom-navigation safe regions.
* Keep animals away from building entrances and primary paths.
* Keep at least 12dp of visual separation from native text.
* Never place an animal where it appears trapped under, standing on, or emerging from a clickable plaque.
* Mirror an asset only when anatomy and identifying features remain correct.
* Tune placements separately for tablet scene mode and compact card mode; do not blindly reuse tablet pixel offsets on phones.

For compact layouts, show at most three animal discoveries in the visible header scene. The remaining animals may appear as quiet decoration inside their corresponding illustrated subject card. Never shrink all six into illegible specks.

### 25.5 Interaction and accessibility

The first release treats all six animals as noninteractive decorative art:

* Use `contentDescription = null` in Compose.
* Clear semantics from their parent decorative layer.
* Do not add invisible click targets.
* Do not insert them into TalkBack traversal.
* Do not make lesson access depend on finding an animal.
* Do not count them toward Daily Quest completion or subject progress.

A future optional “Nature Notebook” feature may make them discoverable collectibles, but only after separate product, accessibility, privacy, and reward-system design. Do not implement collection behavior speculatively.

### 25.6 Motion rules

Default state is static. If ambient motion is later approved:

* Animate only one animal at a time.
* Use one low-amplitude behavior: blink, ear twitch, tail movement, or brief head turn.
* Delay 8–20 seconds between motions.
* Keep each motion under 900ms.
* Do not autoplay animal calls.
* Disable all animal animation under reduced-motion settings.
* Do not use motion to reveal an animal or communicate required state.

### 25.7 Cultural and biological accuracy

Before release, a knowledgeable Philippine wildlife reviewer must verify:

* Species identity and identifying markings.
* Plausible habitat and pose.
* No mixing of non-endemic look-alike species.
* No depiction of wild animals as pets, mounts, market goods, or domesticated village livestock.
* No misleading implication that all six species naturally occupy the same local ecosystem.

The village is a symbolic learning world. If an educational tooltip is added later, explain that the animals come from different Philippine islands and habitats.

### 25.8 Asset-production requirements

Runtime animal assets must be true RGBA PNG, lossless WebP with alpha, SVG, or Rive. A checkerboard pattern drawn into RGB pixels is not transparency and must fail asset review.

Each runtime asset must pass all of these gates:

* Alpha channel exists and includes both transparent and opaque pixels.
* No white, gray, or dark matte fringe at 1× and 2× scale.
* No baked background, vegetation, text, badge, shadow, frame, or UI.
* Full silhouette is contained with 8–12% transparent padding.
* Identifying features remain legible at the intended 28–52dp rendered size.
* The file is tested over sky, foliage, bamboo, water, and dark roof colors.
* The source master is at least 512px on its longest edge.
* Android runtime copy is resized and compressed only after the final placement is approved.

The generated animal sheets supplied in `reference-only/endemic-animals/` are visual references only because their checkerboard is baked into RGB pixels. Do not copy them into `res/drawable` and do not attempt to hide the checkerboard by blending or clipping. Re-export clean alpha assets or paint the animals directly into a dedicated decorative scene layer.

### 25.9 Visual reference

Use `references/bamboo-concept-2-with-endemic-animals.png` as the composition reference. It demonstrates intended subtlety and approximate habitat placement; it is not a shippable screenshot and must not be used as one giant interactive image.

### 25.10 QA acceptance criteria

The implementation fails review if any statement is false:

* Exactly six endemic animal details are represented across the full tablet village.
* Each subject zone receives exactly one.
* No animal overlaps or blocks text, labels, buildings, entrances, progress, or controls.
* No animal is a TalkBack focus node.
* No animal has a mandatory click target.
* The animals are not among the three strongest visual elements on first glance.
* All six species remain visually identifiable when zooming into the screenshot.
* No checkerboard, matte, rectangular background, or alpha fringe is visible.
* At 200% font scale, repositioned native controls do not overlap animals.
* At compact width, the design does not attempt to display six illegibly small animals simultaneously.
* Reduced-motion mode presents the same information and disables optional ambient animation.
* A reviewer has checked biological and cultural presentation before release.

## 26. DeepSeek Compile Feedback and Canonical Asset Status

### 26.1 Report incorporated

DeepSeek reported the following after integrating the Bamboo v1.2 package:

| Asset category | Reported status | Design-contract decision |
|---|---|---|
| Four rattan corner PNGs | Compiles and is present in `drawable-nodpi` | Approved runtime primitives |
| Sawali fill texture | Compiles and is ready for backgrounds | Approved runtime primitive |
| Horizontal and vertical bamboo rails | Compile and are ready for borders/dividers | Approved runtime primitives |
| Endemic-animal placement JSON | Integrated | Approved source of initial normalized coordinates |
| Six endemic-animal reference PNGs | Present but reference-only | Never copy to `res/drawable` |
| Bamboo `.9.png` files | Failed AAPT compilation and were removed | Permanently superseded by Compose assembly |
| Clean-alpha endemic-animal PNGs | Not yet supplied | Open production blocker for animal rendering |

This status is authoritative for Bamboo v1.3. Do not restore the removed `.9.png` resources from an older ZIP or commit.

### 26.2 No NinePatch dependency

The Bamboo UI must not depend on NinePatch files.

Prohibited resource patterns:

```text
bg_bamboo_destination.9.png
bg_bamboo_profile.9.png
bg_bamboo_quest.9.png
bg_bamboo_nav.9.png
bg_bamboo_counter.9.png
bg_bamboo_button.9.png
```

Delete any remaining copies from `res/drawable*`, generated-source folders, test fixtures, and stale package extractions. Search the source tree for both the filenames and `NinePatchDrawable` references.

Use one native Compose component named `BambooSurface` or an equivalently clear name. Assemble it from:

```text
fill_sawali.png
rail_bamboo_horizontal.png
rail_bamboo_vertical.png
corner_rattan_tl.png
corner_rattan_tr.png
corner_rattan_bl.png
corner_rattan_br.png
```

The sawali texture may tile or crop inside the component. The rails may tile or stretch only along their long axis. Never stretch a rail across its thickness. The four corner images retain fixed square dimensions and sit above the rails in z-order. Native Compose content is rendered above the decorative surface.

The component must expose at least:

```kotlin
@Composable
fun BambooSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    railThickness: Dp = 12.dp,
    cornerSize: Dp = 22.dp,
    subjectAccent: Color? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
)
```

Implementation requirements:

* Clip the sawali field to the component shape before drawing decorative rails.
* Keep the rails and rattan corners decorative and excluded from semantics.
* Apply click handling and semantics to one outer native container, not to individual image pieces.
* Keep text, progress, lock state, icons, and pressed state native.
* Use one geometry for all six destination labels.
* Allow profile, Daily Quest, counters, and navigation variants through parameters rather than duplicated composables.
* Cache painters and avoid rebuilding bitmaps per frame.
* Do not use runtime blur.
* Do not rasterize text into the bamboo surface.
* Do not use the preview composites as runtime backgrounds.

### 26.3 Standard PNG install locations

Copy only these standard PNG primitives into the Android module:

```text
feature-child-home/src/main/res/drawable-nodpi/fill_sawali.png
feature-child-home/src/main/res/drawable-nodpi/rail_bamboo_horizontal.png
feature-child-home/src/main/res/drawable-nodpi/rail_bamboo_vertical.png
feature-child-home/src/main/res/drawable-nodpi/corner_rattan_tl.png
feature-child-home/src/main/res/drawable-nodpi/corner_rattan_tr.png
feature-child-home/src/main/res/drawable-nodpi/corner_rattan_bl.png
feature-child-home/src/main/res/drawable-nodpi/corner_rattan_br.png
```

Use lowercase ASCII resource names. Do not place density-neutral primitives into a density-specific folder unless a measured density export strategy is introduced.

### 26.4 Endemic-animal runtime blocker

`ENDEMIC_ANIMAL_PLACEMENT.json` is approved for initial positions, but coordinate data alone does not make the feature shippable.

The six reference files contain a visually rendered checkerboard and are not transparent assets. They must remain under `reference-only/`. Do not use color-key removal at runtime and do not conceal the checkerboard with blending.

Produce these final files:

```text
animal_philippine_eagle.png
animal_philippine_tarsier.png
animal_tamaraw.png
animal_palawan_peacock_pheasant.png
animal_philippine_colugo.png
animal_visayan_warty_pig.png
```

Every final animal must pass all of these gates:

* PNG color type includes a real alpha channel.
* At least 5% of pixels have alpha 0.
* No checkerboard-colored matte remains in opaque pixels.
* No white, gray, or dark fringe appears over sky, foliage, bamboo, water, or roof colors.
* Transparent RGB is edge-cleaned to avoid bilinear halos.
* The longest dimension is appropriate for the displayed 1–3% screen-width target; do not decode multi-megapixel sprites for tiny display.
* The animal remains identifiable at its actual displayed size.
* The illustration style matches the village scene.
* Biological identifiers listed in section 25 remain recognizable.
* The file is visually reviewed by a Philippine cultural or biological subject-matter reviewer before release.

Until all six pass, choose one of these safe behaviors:

1. Hide the endemic-animal layer completely in production, or
2. Bake reviewed animals into a new village-background master and retest all overlay coordinates.

Do not show a partial subset in production because one animal per subject zone is required for parity.

### 26.5 Build verification

DeepSeek must run and report the exact result of:

```bash
./gradlew :feature-child-home:compileDebugKotlin
./gradlew :feature-child-home:processDebugResources
./gradlew assembleDebug
```

Also run a repository search proving that no runtime `.9.png` reference remains:

```bash
find . -type f -name '*.9.png'
grep -R "bg_bamboo_.*\\.9\\.png\|NinePatchDrawable" . --exclude-dir=.git --exclude-dir=build
```

Expected result for this design system: no Bamboo NinePatch resource and no Bamboo NinePatch code reference.

### 26.6 Asset completion statuses

Use only these status labels in implementation reports:

```text
PRODUCTION_READY
REFERENCE_ONLY
PENDING_EXPORT
BLOCKED
REMOVED
```

Current canonical status:

| Asset | Status |
|---|---|
| Sawali fill | `PRODUCTION_READY` |
| Bamboo horizontal rail | `PRODUCTION_READY` |
| Bamboo vertical rail | `PRODUCTION_READY` |
| Four rattan corners | `PRODUCTION_READY` |
| Bamboo NinePatch files | `REMOVED` |
| Endemic-animal placement JSON | `PRODUCTION_READY` |
| Six checkerboard animal images | `REFERENCE_ONLY` |
| Six clean-alpha animal sprites | `PENDING_EXPORT` |
| Approved Maxine app-icon artwork | `PENDING_EXPORT` until adaptive layers are committed |
| Legacy generic launcher icon | `REMOVED` after the approved adaptive set is installed |

