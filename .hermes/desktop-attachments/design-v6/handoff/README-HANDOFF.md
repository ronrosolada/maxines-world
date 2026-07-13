# Maxine's World — Implementation Handoff Package

A private, offline-first Android learning app for an 8-year-old Grade 3 learner. Animal-village theme, cats as primary companions. This package is everything an implementing engineer (human or LLM) needs to build it.

---

## Paste this to your implementation LLM to start

> Implement **Maxine's World** using `docs/maxines-world-implementation-handoff.md` as the source of truth for product/architecture and `docs/design.md` as the source of truth for visual design, components, characters, animation, accessibility, and asset production.
>
> Open `maxines-world-screens.html` in a browser first — it is the native, implementation-ready UI reference (8 tablet-landscape screens at 1280×800 + the app icon). Recreate every control natively in Jetpack Compose; the illustrations are background art only. Emoji in the reference are placeholders for produced SVG/Rive assets — do not ship emoji.
>
> Build one production-quality vertical slice first: (1) parent PIN login + one child profile, (2) child village home, (3) offline lesson engine, (4) one pilot lesson per subject, (5) progress + mastery storage, (6) parent dashboard, (7) basic learning-linked rewards, (8) automated tests.
>
> Stack: Kotlin, Jetpack Compose, Material 3 + custom tokens, Room, DataStore, Hilt, WorkManager, Navigation Compose, Media3, Rive, Lottie. Keep curriculum + lessons in versioned external JSON, not hard-coded. Before coding, produce architecture decisions, repo structure, milestones, and risks; then implement milestone by milestone, running the Android build + tests after each.
>
> Do not claim curriculum certification or invent DepEd competency codes. Preserve child privacy: no ads, no public profiles, no public chat, no behavioral tracking. First milestone = a reliable offline 10-minute learning session.

---

## What's in this package

```
handoff/
├── README-HANDOFF.md                 ← you are here (start + kickoff prompt)
├── maxines-world-screens.html        ← NATIVE UI REFERENCE — open in any browser, works offline
├── docs/
│   ├── maxines-world-implementation-handoff.md   ← product, architecture, curriculum, engines, mastery
│   ├── design.md                                 ← design system (see §24 for the v2 native-screen iteration)
│   ├── village-home-build-spec.md                ← ⚠ READ FIRST: verbose fix guide for the Village Home drift
│   ├── grade-3-learning-app-plan.md              ← product rationale + rollout
│   └── original-README.md                        ← original Hermes kickoff notes
└── assets/
    ├── graphics/     ← 7 concept illustrations (art references, recreate UI natively over them)
    ├── characters/   ← 5 guide companions, SVG + 400px PNG, transparent (see below)
    ├── buildings/    ← 6 destination buildings, each with its grounded guide + subject sign (see below)
    └── icon/         ← adaptive Android launcher icon, ready to use
```

## The UI reference — `maxines-world-screens.html`

Eight tablet-landscape screens (1280×800), every control native and mapped to the Compose component library:

1. **Village Home** — profile/level, streak, Daily Quest, subject destinations, bottom nav
2. **World Map** — unlocked / current / locked location states, quest path, Parent Gate
3. **Math · Equal Groups** — objective, draggable counters, answer tiles, guide bubble
4. **English · Main Idea** — read-aloud story, drag main-idea + supporting-detail cards
5. **Science · Investigation** — predictions, variables, day slider, observation notebook
6. **Parent Dashboard** — restrained teal rail, progress cards, weekly chart, screen-time
7. **Reward Moment** — short, skippable celebration with stars / paw coins / one badge
8. **App Icon** — adaptive foreground + background layers, mask previews, variants

Design decisions and the component→screen map are documented in **`docs/design.md` §24 (Design Iteration v2)**.

## Design tokens (authoritative — full detail in design.md §7–§11, §21)

Colors: `village_teal #087F83` · `coral #F47C6B` · `sunshine_gold #F5B82E` · `leaf_green #66A83E` · `story_purple #7653B5` · `sky_blue #3C9DDB` · `ink #183B4A` · `cream #FFF7E8`.
Subjects: English=purple, Filipino=coral, Math=sky blue, Science=leaf green, History=heritage gold.
Type: **Baloo 2** (display/headings/numerals/buttons) + **Nunito** (body/Filipino/parent). Radius 12/18/24/32dp. Touch target ≥48dp, preferred child 56dp, primary 64dp.

## App icon — `assets/icon/`

| File | Use |
|---|---|
| `app-icon-512.png` | Play Store / composed master |
| `ic_launcher_foreground.png` | adaptive foreground layer (transparent) |
| `ic_launcher_background.png` | adaptive background layer |
| `app-icon-monochrome.png` | themed-icon / single-color mark |
| `app-icon-48.png` | 48px legibility check |

Wire up via `mipmap-anydpi-v26/ic_launcher.xml` (foreground + background); supply the monochrome layer for Android 13+ themed icons. Recreate as `VectorDrawable` for crisp shipping.

## Character guides — `assets/characters/`

Five companion characters, each SVG (source of truth — recreate as `VectorDrawable`/Rive rig) + 400×400 transparent PNG. Seated front-facing base pose; animate per the expressions/rigs in design.md §13.

| File | Character | Appearance | Role / Subject |
|---|---|---|---|
| `milo.*` | Milo | Orange tabby, teal backpack | Main guide + Math (Number Market) — also the hero/welcome character |
| `mira.*` | Mira | Calico, purple scarf + book | English (Story Tree) & Filipino |
| `niko.*` | Niko | Gray tabby, green goggles + lab coat | Science (Discovery Lab) |
| `lakan.*` | Lakan | Brown forest cat, heritage satchel + scarf | Makabansa / Philippine History (Heritage Harbor) |
| `duke.*` | Duke | Friendly aspin (dog), blue scarf + heart | GMRC / Kindness Corner (teamwork) |

`_lineup.png` shows all five together for quick review. Colors and scarf/accessory hues are drawn from the brand tokens; swap PNGs for produced Rive rigs when available. (Milo replaces the "Maxine" cat labeled in the concept art — Maxine is the learner, per design.md §13.)

## Destination buildings — `assets/buildings/`

Each of the six subject destinations is delivered as a **composed building** — the building artwork with its guide character already seated in front (grounded, with a contact shadow) and a hanging subject sign. This is what the implementing LLM asked for: place these over the village background at the marker positions instead of compositing characters and buildings separately.

Two files per destination: `<key>.png` (composite, ~520×560, transparent) and `<key>-building.svg` (building only, no character — use if you want to animate the guide separately). `_lineup.png` shows all six.

| Key | Destination | Subject | Color | Guide inside |
|---|---|---|---|---|
| `story_tree` | Story Tree | English | story_purple `#7653B5` | Mira |
| `number_market` | Number Market | Math | sky_blue `#3C9DDB` | Milo |
| `discovery_lab` | Discovery Lab | Science | leaf_green `#66A83E` | Niko |
| `bahay` | Bahay ng Kuwento | Filipino | coral `#F47C6B` | Mira |
| `heritage_harbor` | Heritage Harbor | Makabansa | heritage gold `#B8862B` | Lakan |
| `kindness_corner` | Kindness Corner | GMRC | village_teal `#087F83` | Duke |

**How to use them (matches the Village Home build spec):** the village background (`location_village_day.webp` / `child-village-home.png`) is Layer 1; drop each building PNG at its stop position as Layer 2, anchored by its bottom edge to the path so the guide's feet sit on the ground; then the native destination tag card (name + progress + lock) floats as Layer 4. The subject sign baked into the PNG is decorative — the tappable name/progress still comes from the native card. These are placeholder art for final produced building + Rive-rig assets; swap when ready.

## Hard constraints (do not violate)

- Concept PNGs are references; recreate all controls + text natively.
- Validate curriculum mappings with qualified Philippine educators before release.
- No ads, public profiles, public chat, or behavioral tracking.
- Never communicate correctness by color alone — always pair with icon + label.
- First milestone: a reliable offline 10-minute learning session.
