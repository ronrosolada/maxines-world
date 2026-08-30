### 1. Playroom Home — Too many equal-weight cards compete with Today’s Quest

- **Rule invoked:** impeccable — **Mode = Operate**, **hierarchy & the quieter discipline**, and **restraint as craft**.
- **Current look & feel:** The gold→coral background, TodayQuestCard, LearningStreakCard, ArenaBannerCard, QuickBitsHomeCard, subject cards, sticker board, and sanctuary board all ask for attention. Repeated white/cream cards with 12–14dp radii create a wall of similarly weighted containers. Today’s task does not read as the single obvious starting point.
- **Design fix (concrete):**
  - Make **TodayQuestCard the only hero**: 24dp outer padding, 24dp internal padding, 14dp radius, white surface, and a restrained 2dp `SunshineGold` progress accent rather than additional decorative fills.
  - Use a consistent tablet gutter of **32dp** and phone gutter of **16dp**.
  - Establish chapter spacing:
    - Header → Today’s Quest: **24dp**
    - Today’s Quest → supporting actions: **32dp**
    - Supporting actions → Explore subjects: **40dp**
    - Subjects → Wildlife: **40dp**
    - Wildlife → Sanctuary: **32dp**
  - Demote Learning Streak and Quick Bits from full cards to compact **64–72dp rows** on `Cream`, with 16dp horizontal padding, one icon, one text block, and no independent shadow.
  - Keep Arena as the one secondary promotional card, but remove any decorative backdrop that competes with the quest. Use a pale `StoryPurple` tint, not a saturated gradient.
  - Avoid nested surfaces in keepsake sections: use one board surface with sticker slots inside it, not a card around every slot.
  - In Compose, centralize this rhythm with `Arrangement.spacedBy()` values from the 4dp grid rather than per-component ad hoc padding.
- **Why it improves the feel:** Maxine immediately sees “what to do now,” while exploration remains available without becoming visual noise. Caregivers perceive a deliberate learning flow rather than a dashboard of promotions.

### 2. Subject and sticker grids — Seven subjects and variable sticker states risk dead cells and visual scatter

- **Rule invoked:** gpt-taste — **grid interlock / zero dead cells**; impeccable — **layout discipline** and **restraint as craft**.
- **Current look & feel:** A responsive 1–3-column SubjectGrid containing seven subjects produces an incomplete final row at two or three columns unless spans are intentional. Individually accented cards can also become seven competing designs. Locked sticker slots with dashed borders add another dense visual pattern below.
- **Design fix (concrete):**
  - Treat all subjects as **one repeated card system**, not seven visually distinct card styles. Use `Cream` or each subject’s pale tinted surface, `Ink` text, one crafted vector icon, and a narrow 4dp accent rail in the subject color.
  - Use deterministic spans:
    - **Phone, 1 column:** seven standard items.
    - **Medium, 2 columns:** featured/recent subject spans 2 columns; the remaining six fill three complete rows.
    - **Tablet, 3 columns:** featured/recent subject spans 3 columns; the remaining six fill two complete rows.
  - Compose implementation:
    ```kotlin
    LazyVerticalGrid(columns = GridCells.Fixed(columnCount)) {
        item(
            span = { GridItemSpan(maxLineSpan) },
            key = "featured-subject"
        ) { FeaturedSubjectCard(...) }

        items(remainingSubjects, key = { it.id }) {
            SubjectCard(...)
        }
    }
    ```
  - Use **12dp grid gaps**, 16dp card padding, 88–104dp standard card height, and a 120dp featured card. Do not vary radii or elevation by subject.
  - For wildlife stickers, calculate the column count from minimum slot width and always fill the board footprint. If the final collectible row is incomplete, make the final earned or next-to-earn sticker span the remaining columns instead of rendering blank cells.
  - Quiet locked stickers: `Cream`/beige fill, `Ink` at 12–18% for the glyph, and one 1dp dashed outline. Won stickers may use the gold/cream gradient, but not extra shadows, sparkles, and borders simultaneously.
- **Why it improves the feel:** Dense, aligned grids feel collectible and complete rather than accidental. Maxine can scan subjects and earned wildlife faster, while caregivers see a system that remains orderly at every breakpoint.

### 3. Typography and labels — Display type and small utility copy need stricter hierarchy and child-readable limits

- **Rule invoked:** gpt-taste — **2–3 line iron rule** and **meta-label ban**; impeccable — **typography discipline**.
- **Current look & feel:** Baloo 2 can become decorative noise if used across card titles, counters, rewards, and section labels. “X of Y” and similar small labels can become visually orphaned beside the paw track. At phone widths, quest or result headings may stack into four lines even though the underlying task is simple.
- **Design fix (concrete):**
  - Reserve **Baloo 2 Bold** for the screen title, hero title, major result, and chapter headings only. Use **Nunito** for task text, chips, progress labels, card titles, metadata, and buttons.
  - Recommended hierarchy:
    - Screen/hero: Baloo 2 Bold **36/44 tablet**, **30/36 phone**, `maxLines = 2`
    - Section heading: Baloo 2 Bold **22/28**, `maxLines = 2`
    - Card title: Nunito Bold **18/24**, `maxLines = 2`
    - Body/task: Nunito SemiBold or Regular **16/24**, `maxLines = 3`
    - Supporting label: Nunito Bold **13/18**, never isolated without an icon or nearby parent text
  - Replace generic labels with meaningful language. Use “**2 of 5 paws found**,” “**3 lessons ready**,” or “**Next reward: Tamaraw sticker**,” not “QUEST 05,” “SECTION,” or detached numeric counters.
  - Keep the progress phrase adjacent to the paw track in the same semantic group. Do not put it in a separate badge unless it is actionable.
  - Enforce `maxLines` in Compose, but do not rely on ellipsis for primary child instructions. Use `BoxWithConstraints` to select the phone display style, and rewrite localized copy that still exceeds three lines.
  - Use Material vector or custom illustrated glyphs for paw, gift, trophy, stars, and wildlife. Do not substitute emoji in strings, buttons, empty states, or reward chrome.
- **Why it improves the feel:** The interface sounds warm without looking juvenile or improvised. Maxine gets short, readable instructions; caregivers can distinguish tasks, progress, and supporting information at a glance.

### 4. Arena actions and reward motion — CTA hierarchy and celebration effects need verified, disciplined behavior

- **Rule invoked:** gpt-taste — **button contrast perfection** and **no emoji as design**; impeccable — **delight with discipline**.
- **Current look & feel:** Arena pack cards, gradient results, trophy celebration, paw progress, and reward feedback can create competing pops. Alpha-tinted reward strips are appropriate for information but unsafe as button treatments. Multiple saturated subject colors also make primary versus secondary actions ambiguous.
- **Design fix (concrete):**
  - Allow one primary action per state:
    - Default learning CTA: `VillageTeal` with `Cream` or white text.
    - Arena-specific primary CTA: subject accent only when paired with a verified on-color.
    - Secondary action: `Cream` surface, `Ink` text, 1dp `Ink` at 20–24% outline.
  - Use the supplied verified token pairs for saturated buttons:
    - `SunshineGold` + `OnGold` — approximately **8.8:1**
    - `Coral` + `OnCoral` — approximately **6.2:1**
    - `LeafGreen` + `OnLeafGreen` — approximately **5.6:1**
    - `SkyBlue` + `OnSkyBlue` — approximately **5.3:1**
  - Never place white text on `SunshineGold`, and never use an alpha-blended fill for a primary CTA. Add automated WCAG contrast tests for every subject accent and its chosen on-color; require at least **4.5:1** for normal button text.
  - Motion specification:
    - Paw fill: **240ms**, `FastOutSlowInEasing`, only for the newly earned paw.
    - Card completion check: **180ms** scale from 0.92→1 plus fade.
    - Result trophy entrance: **360ms**, one translation/fade sequence.
    - Reward reveal: **420ms maximum**, one coordinated sequence.
    - No infinite pulse, shimmer, bounce, or simultaneous card-by-card popping.
  - Under reduced motion, replace translation and scale with a **120ms crossfade**, or render the completed state immediately.
  - Use a crafted trophy/paw vector or Milo illustration—never trophy, star, or animal emoji as celebration UI.
- **Why it improves the feel:** Maxine receives clear, satisfying confirmation without losing focus or being overstimulated. Caregivers see trustworthy controls, readable actions, and accessibility treated as part of the visual system.

**Highest-impact single change:** Make Today’s Quest the sole home hero, flatten Learning Streak and Quick Bits into quiet supporting rows, and introduce 32–40dp chapter spacing between major sections.

**New items vs a generic review:** The mathematically complete seven-subject span strategy, literal two-to-three-line enforcement, removal of meta-label/emoji shortcuts, one-hero restraint, and verified token-level CTA contrast emerge specifically from the impeccable and gpt-taste lenses.