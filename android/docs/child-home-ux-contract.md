# Child-home UX contract

This contract defines the child-facing information hierarchy and the stable
semantics hooks for the Playroom home. It is intentionally independent of
visual styling so layout and illustration work can evolve without breaking
child navigation, accessibility, or UI regression coverage.

## Information hierarchy

The home presents information in this order:

1. **Today action** — Today’s Quest is the single primary action. It tells the
   child what to do now and exposes one Start/Continue action.
2. **Concrete progress** — Quest progress is shown as a count (for example,
   `2 of 3`) and the reward preview remains visible before completion. The
   child-visible learning-day streak card explains `N days learning` (or
   its zero state) separately from the Quest paw-row count.
3. **Subject destinations** — The six canonical subject cards are stable
   destinations. Each card has a formal label, a playful label, a progress
   state, and an availability/lock state.
4. **Collection** — Wildlife Stickers / the Field Guide is a child-facing
   destination for earned collection progress.
5. **Parent gate** — Parents is a separate protected destination and must remain
   clearly announced as the parent entry point; the destination itself owns the
   PIN or other gate.

The bottom navigation remains intentionally compact: Home (selected),
Collection, and Parents. No additional reserved destinations are exposed as
child-facing no-op controls.

## Stable semantics tags

These tags are an interaction contract. Keep the values unchanged when
refactoring the home layout:

| Tag | Required target |
| --- | --- |
| `home_today_quest` | Today’s Quest card and its primary action region |
| `home_streak` | Learning-day streak card (`N days learning`, or its zero-state copy) |
| `home_subject_<id>` | Subject card, where `<id>` is the stable subject ID (for example `home_subject_mathematics`) |
| `home_collection` | Collection item in home navigation |
| `home_parents` | Parent-gate item in home navigation |
| `home_nav_selected` | The currently selected home-navigation destination |

Subject IDs are the canonical IDs: `mathematics`, `english`, `science`,
`filipino`, `makabansa`, and `gmrc`.

## Semantics requirements

- Today’s Quest exposes exactly one primary Start/Continue action for the
  current quest state.
- Every subject card announces its stable formal/playful label and exposes a
  `stateDescription` for `Not started`, percentage progress, or `Complete`.
- The selected navigation destination exposes `selected = true` and announces
  its label (currently `Home`).
- Collection and Parents retain click actions and stable tags at compact,
  medium, and wide width classes.
- The contract is exercised at font scales `1.0` and `1.3`; increased text must
  not remove or hide the essential navigation targets.

## Responsive coverage

The connected contract test exercises the home at representative widths for
all three required classes:

- Compact: `360dp`
- Medium: `840dp`
- Wide: `1100dp`

The test uses a fixed-width composition so the width-class logic is exercised
without changing production behavior or device configuration.
