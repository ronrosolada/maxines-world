# Homepage critique and design decisions

## Why the current build feels unfinished

The existing homepage is dominated by a large empty Daily Challenge panel, followed by a fixed-width quest sidebar and a clipped flat map. The screen repeats emoji characters, draws scenery as basic geometry, places labels in opaque brown rectangles, and exposes five destinations despite six supplied building sets. Its hierarchy says “dashboard prototype,” not “adventure world.”

## New hierarchy

1. Player identity and persistent progress.
2. One actionable Daily Quest.
3. Six equal subject destinations inside one continuous village.
4. Secondary collection/navigation actions.

## Gamification model

The redesign makes progress visible without turning learning into a reward shop. Each destination shows completion, the quest shows finite steps and explicit rewards, and the HUD shows level, streak, stars, and paw coins. Rewards are deterministic and secondary to the learning objective.

## Six-subject parity rule

Every subject receives the same destination footprint, building scale, character treatment, label structure, state model, and touch target. “Recommended” may change ordering emphasis temporarily, but it must not change permanent destination size. Locked content stays equally visible and explains its state.

## Engineering rule

Art is decorative or representational; all text, progress, state, and controls remain native Compose. The background, buildings, characters, labels, and hit targets are independent layers. This allows localization, accessibility, responsive layout, and future animation without regenerating scene art.
