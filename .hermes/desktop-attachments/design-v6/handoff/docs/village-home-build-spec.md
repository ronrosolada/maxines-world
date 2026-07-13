# Village Home — Verbose Build Spec (read this before touching the home screen)

> **Audience:** the engineer/LLM implementing the Android app.
> **Why this doc exists:** the current build of the Village Home screen drifted a long way from the intended design. This document explains, in plain language and with exact numbers, what went wrong and how to build it correctly. Follow it literally. When in doubt, open `../maxines-world-screens.html` and look at the panel labeled **2a — Village Home — corrected target**; that is the screen you are trying to reproduce natively.

---

## 0. The one idea you must understand first

The Village Home is built in **layers stacked on top of each other**, like sheets of glass:

```
TOP     ┌─────────────────────────────────────────┐
        │  LAYER 4  Native UI chrome              │  ← Compose views: top bar, Daily Quest,
        │           (buttons, cards, text)        │     destination tags, bottom nav
        ├─────────────────────────────────────────┤
        │  LAYER 3  Legibility scrims             │  ← two soft dark gradients (top + bottom)
        ├─────────────────────────────────────────┤
        │  LAYER 2  Grounded sprites (optional)   │  ← building + guide-character images,
        │                                         │     ONLY if not already painted into Layer 1
        ├─────────────────────────────────────────┤
BOTTOM  │  LAYER 1  Illustrated background image  │  ← ONE produced artwork file, full-screen
        └─────────────────────────────────────────┘
```

**The single most important rule:** the village scenery (sky, hills, trees, cobblestone path, buildings, ambient animals) is a **painted image asset**. You do **not** rebuild it out of rectangles, colored bands, and shapes in code. The current build tried to reconstruct the scene from primitives and it looks flat and broken. Load the art. Put the UI on top.

---

## 1. What the current build got wrong (five root causes)

Compare the current build screenshot to target **2a**. These are the five defects, in priority order:

### Defect 1 — The scene was rebuilt from flat shapes instead of using the artwork
- **Symptom:** the background is flat purple/green mountain bands; the "buildings" are a crude tree blob, a red-striped box, a plain brown house, and a boxy castle.
- **Cause:** no illustrated background asset was loaded; the scene was drawn with solid-color shapes.
- **Fix:** render the produced artwork `location_village_day.webp` as a single full-bleed image that fills the whole screen (`ContentScale.Crop`). Delete all the hand-drawn mountain/building shape code. If final art is not ready yet, use the placeholder `assets/graphics/child-village-home.png` from this package as a temporary stand-in — but still load it *as an image*, do not imitate it with shapes.

### Defect 2 — Guide characters float in the sky with "ghost blob" shadows behind them
- **Symptom:** the cats hover in mid-air above the buildings; behind several of them is a large semi-transparent gray/green circle (a leftover placeholder).
- **Cause:** character sprites were positioned by arbitrary Y offsets with a placeholder background circle that was never removed; nothing anchors them to the ground.
- **Fix — pick ONE of these two approaches and be consistent:**
  - **(A) Bake the guides into the scene art** (recommended for v1). The characters are part of `location_village_day.webp`. Then you place **zero** character sprites in code on this screen. Simplest and always looks right.
  - **(B) Composite grounded sprites.** If guides must be separate (e.g. to animate them), anchor each sprite by its **bottom edge** to the ground line of its building, add a soft elliptical **contact shadow** directly under its feet (a blurred dark ellipse at ~12% opacity), and **delete the round placeholder blob entirely**. A character must never appear to hover.

### Defect 3 — Subject launchers render as dead gray circles
- **Symptom:** the top "Daily Challenge" strip is five flat gray circles with faint icons; they look disabled.
- **Cause:** the subject launcher was built as an outline circle with no fill color and no card container.
- **Fix:** each subject is a **cream card** (see §3). If you keep a compact top strip, each item is still a small rounded cream chip with a **colored** icon badge and a label in ink — never a gray ring. Better: drop the redundant top strip and use the destination tags at the building bases (§3), which already list every subject.

### Defect 4 — Destination tags are dark-brown, cramped, low-contrast, and lack progress
- **Symptom:** "Story Tree / Number Market / …" tags are dark brown rectangles with small white text; no progress shown; you cannot tell which is done, current, or locked.
- **Cause:** tags used a dark fill with undersized text and omitted the progress bar and lock state.
- **Fix:** rebuild each tag as the **destination card** in §3 — cream fill, ink title, colored subject label, colored progress bar, and an explicit locked state.

### Defect 5 — The top bar lost the profile/level card and shipped a bare version string
- **Symptom:** top-right shows a streak pill, the literal text `v0.10.0`, and a tiny avatar. The profile card with the level and XP bar is gone.
- **Cause:** the profile/XP component was dropped; a debug version label was left in the shipping UI.
- **Fix:** restore the **profile card** (avatar + name + `Lv 12` + gold XP progress bar) at top-left (§2). Remove the version string from the child UI — if you need a build number, put it in the Parent → Settings screen, not on the child home.

---

## 2. Layer-by-layer build instructions

Design canvas is **1280 × 800 dp** (tablet landscape). All numbers below are in dp. Scale proportionally for other sizes. Everything uses the shared design tokens (colors, radii, type) — see `design.md §7–§11`.

### Layer 1 — Illustrated background (required)
- One `Image`, `Modifier.fillMaxSize()`, `ContentScale.Crop`, aligned to show the village (bias the crop slightly upward so the path/buildings sit in the lower two-thirds).
- Asset: `location_village_day.webp` (final) or `child-village-home.png` (placeholder in this package).
- Nothing else goes in this layer.

### Layer 2 — Grounded sprites (only if not baked into Layer 1)
- Skip entirely if you chose Defect-2 approach (A).
- If used: one building sprite + one seated guide per destination, each anchored bottom-to-ground with a contact shadow. No floating. No placeholder circles.

### Layer 3 — Legibility scrims
- Two vertical gradients over the whole screen so white cards and text never sit on a busy, low-contrast area:
  - Top scrim: `#0B2A36` at 30% opacity fading to transparent by ~18% down.
  - Bottom scrim: transparent until ~62% down, fading to `#0B2A36` at ~52% opacity at the bottom.

### Layer 4 — Native UI chrome (all real Compose views)

**4a. Profile card — top-left**
- Cream card `#FFF7E8`, corner radius 22, padding 10/18, drop "step" shadow `y=+5, blur=0, color #183B4A @14%`.
- Contents left→right: circular avatar 56×56 (3dp white ring) · then a column: name in Baloo 2 800 / 21sp ink `#183B4A`; below it a row of `Lv 12` (Nunito 800, story-purple), a gold XP bar (132×13, track `#E6D9C2`, fill gradient `#F5B82E→#F0A21E`, here 73%), and `660/900` in muted brown.

**4b. Streak pill + menu — top-right**
- Streak: cream pill, 🔥 flame + "7-day streak" (Baloo 2 800/19) + "Keep it going!" (Nunito 700/11, leaf-green).
- Menu: 56×56 sky-blue `#3C9DDB` rounded-18 button with a 3-line hamburger, chunky shadow `0 5px 0 #2b7bb0`.
- **Do not** put a version number here.

**4c. Daily Quest card — upper-left, below profile**
- Cream card, radius 22, width ~250. Header: purple 32×32 rounded badge with 📋 + "Daily Quest" (Baloo 2 800/19).
- One line of quest text (Nunito 700/15). Progress bar (track `#E6D9C2`, fill leaf-green gradient, e.g. 60%) + "3/5".
- Primary button "Start ▸": full width, height 52, leaf-green `#66A83E`, chunky shadow `0 5px 0 #4d8a2c`, label Baloo 2 800/19 white.

**4d. Destination tags — docked row near the bottom (the heart of the screen)**
- A horizontal row of destination cards (see §3), centered, `gap 14`, sitting just above the bottom nav (bottom ≈ 104 from screen bottom). On a phone or when there are more subjects than fit, this row **scrolls horizontally**.
- The "today's focus" card is raised ~16dp (`translateY(-16)`) and wears a gold "★ TODAY" ribbon.

**4e. Bottom nav — full-width strip**
- Cream bar, radius 22, height 70, chunky shadow. Four items evenly spaced: My Profile (🐱, teal), Achievements (🏆, gold), Backpack (🎒), Parents (🔒). Labels Baloo 2 700/17. Active item uses the subject/teal color; inactive use ink-muted `#3d5560`.

---

## 3. The Destination Card — exact spec (reuse everywhere)

This is the single most reused component on the home screen. Build it once (`MaxinesSubjectDestination`) and render it in three states.

**Base (unlocked):**
- Fill cream `#FFF7E8` at 97% opacity, radius 22, padding 13.
- **Top border 6dp in the subject color** (this is how the child reads which subject it is at a glance).
- Chunky shadow `0 6px 0 #183B4A@16%` plus a soft ambient `0 14px 28px #183B4A@28%`.
- Row: icon badge 42×42, rounded-12, filled with the subject color, chunky shadow in a darker tint of that color; the glyph/number sits on it in white. Then a column: destination name (Baloo 2 800/18 ink) and subject label (Nunito 800/12 in the subject color).
- Below: a progress row — thin bar (height 9, track = a 12%-tint of the subject color, fill = the subject color) + `7/12` count in the subject color.

**Today's focus (a variant of unlocked):**
- Same card, raised 16dp, with a gold `★ TODAY` ribbon pinned to the top edge (gold `#F5B82E`, ink text, chunky shadow).

**Locked:**
- Fill gray `#ECF0F1` at 95%, top border gray `#9aa7ac`, no ambient glow.
- Badge gray `#7c898e` with a 🔒.
- Title in muted `#5b6b71`, subject label in `#7c898e`.
- **Replace the progress bar with an unlock condition in text**, e.g. "Reach Level 15 to open." Never communicate "locked" by color/dimming alone (accessibility rule, `design.md §7`).

**Subject → color map (memorize; used for border, badge, label, progress):**
| Subject | Destination name | Color | Hex |
|---|---|---|---|
| English | Story Tree | story_purple | `#7653B5` |
| Filipino | Bahay ng Kuwento | coral | `#F47C6B` |
| Math | Number Market | sky_blue | `#3C9DDB` |
| Science | Discovery Lab | leaf_green | `#66A83E` |
| Makabansa (History) | Heritage Harbor | heritage gold | `#B8862B` |
| GMRC | Kindness Corner | village_teal | `#087F83` |

---

## 4. Tactile buttons (applies to every button in the app)

The app's signature "toy-like" feel comes from buttons that look physically pressable:
- Give every primary button a **hard bottom shadow**: `boxShadow y=+5 or +6, blur=0, color = a darker shade of the button color` (e.g. leaf-green button → `#4d8a2c` shadow).
- On press: translate the button down ~4dp and shrink the shadow to `y=+1`, so it "depresses." On release, spring back.
- Minimum touch target 48dp; child primary actions 56–64dp tall.
- Labels in Baloo 2, 700–800 weight.

---

## 5. Placeholders you must replace before shipping

- **Emoji** (📖 🧪 123 🔥 🎒 …) are **placeholders** for produced icons (`icon_subject_*`, counter/coin art). Do not ship emoji.
- **`child-village-home.png`** is a concept placeholder for the final `location_village_day.webp`.
- Guide-character PNGs in `assets/characters/` are flat stand-ins for the final Rive rigs (see `design.md §13`).

---

## 6. Definition of done for this screen

- [ ] Background is a loaded image, not shapes.
- [ ] No character floats; none has a leftover circle behind it.
- [ ] Every subject launcher is a cream card with a colored badge + progress bar; none is a gray ring.
- [ ] Locked stops show 🔒 **and** a text unlock condition.
- [ ] Profile card with avatar + level + gold XP bar is present top-left; no version string in the child UI.
- [ ] Every button has the chunky pressable shadow and depresses on tap.
- [ ] The whole thing matches target panel **2a** in `maxines-world-screens.html`.

---

## 7. UPDATE — v2 home layout (this supersedes the "destination tags in a docked row" idea)

After review, the home screen changed in three important ways. Where §2–§6 above and target **2a** differ from this section, **follow this section** and target panel **3a**.

### 7.1 All subjects are equal-weight buildings on one street
- Show **every** subject as its own building of **equal size and prominence** along a single horizontal "village street": English (Story Tree), Filipino (Bahay ng Kuwento), Math (Number Market), Science (Discovery Lab), Makabansa (Heritage Harbor) — and GMRC (Kindness Corner) when added.
- **Do not** make Filipino or Makabansa smaller, secondary, or locked-by-default. They are peers of the first three.
- The street is a **horizontal scroller** (`LazyRow`): more subjects simply extend the street. On a tablet 5 buildings fit; on a phone the child swipes.

### 7.2 The building itself is the button — there is NO separate row of cards
- Each destination is a single tappable unit (component `MaxinesVillageBuilding`). The **building composite artwork is the control**: the whole building footprint is the hit target (minimum 88×88dp), and it lifts ~4dp and springs back on press.
- **Remove any second, redundant row of clickable subject cards at the bottom of the screen.** The earlier "docked destination tags" row is gone. One building = one button.
- Art comes from `assets/buildings/<key>.png` (building + its guide already grounded inside). Anchor each PNG **bottom-to-path** so the guide's feet sit on the ground. The subject **sign is baked into the art** — do not also render the name as separate native text.

### 7.3 Progress is a small pill at the doorstep, not a card
- Each building shows a small **cream pill pinned at its base**: a short progress bar in the subject color + the count (e.g. `8/12`). That pill is part of the building unit, not a separate card.
- **Locked** subject (if/when used): swap the progress pill for a `🔒` + short text ("Reach Level 15"). Never signal locked by dimming/color alone.
- The **day's focus** building (and only that one) is raised ~18dp and wears a gold `★ TODAY` ribbon with a soft radial glow behind it. All others stay at the base line.

### 7.4 Trim redundant chrome (do this everywhere, not just home)
- **Remove the settings/hamburger button from the top-right of the child home.** There must not be a settings icon there. All parent/settings access goes through the single **Parents** item (gated) in the bottom nav.
- **Daily Quest is one compact, tappable banner** — icon + one line of quest text + a small progress bar/count + a single `▸` chevron. Do **not** add a separate "Start" button (the banner is the button) and do **not** repeat a "Daily Quest" heading above text that already says it. One element, one job.
- General rule: before adding any button, ask "is there already a way to do this on screen?" If yes, don't add a second control.

### 7.5 v2 layer stack (unchanged idea, refined contents)
```
LAYER 4  chrome: profile card (top-left) · streak pill (top-right, NO menu button) ·
                 Daily Quest banner (one tappable strip) · bottom nav
LAYER 4  the street: equal building-buttons, each with a doorstep progress pill;
                 today's building raised + ★ TODAY ribbon
LAYER 3  top + bottom legibility scrims
LAYER 2  building composite PNGs (guide grounded inside) placed along the path
LAYER 1  environment backdrop art (sky, hills, ground, path) — an image asset,
                 NOT rebuilt from shapes.  A ready backdrop ships at
                 assets/scene/village-backdrop.png (+ .svg); replace with the final
                 painted location_village_day.webp when produced.
```

### 7.6 v2 definition of done
- [ ] Five subjects shown as equal-size buildings; Filipino and Makabansa are full peers.
- [ ] Tapping a building opens that subject's lessons; the building is the only control (no bottom card row).
- [ ] Each building has a doorstep progress pill; today's building is raised with a ★ TODAY ribbon.
- [ ] No settings/hamburger icon in the top-right; settings live behind the Parents gate.
- [ ] Daily Quest is a single tappable banner with a chevron — no extra Start button, no duplicate heading.
- [ ] Matches target panel **3a** in `maxines-world-screens.html`.
