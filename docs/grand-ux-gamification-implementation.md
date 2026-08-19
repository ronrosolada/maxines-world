# Grand UX, UI, and Gamification Review, Implementation Status

**Status: Historical, superseded by `HANDOFF.md` (canonical).** Describes DB v8 and the wildlife expedition as current; its follow-up list overlaps with newer briefs. Kept for the record (spec CH-13).

**Review baseline:** 6 August 2026
**Implementation branch:** `feat/lesson-concept-visuals`
**Release candidate:** `0.22.0`

This note records the implemented child-facing policy and the remaining visual/product follow-up. Passing tests do not replace a human review of the experience, but the core reward loop is now persistent and replay-safe.

## Implemented

### Homepage trust

- Child Home no longer displays XP or streak counters that do not serve the learning loop.
- The disabled/no-op Progress destination was removed from child navigation.
- The misleading Avatars destination was renamed **Collection** and routes to the Wildlife Field Guide.
- GMRC is available from the first session; it is not a Level 4 curriculum gate.
- The collection preview uses real earned Wildlife Sticker records and does not render generic demo slots.

### Wildlife Expedition

- The old daily five-subject reset was replaced by a persistent local-week expedition.
- A child completes **3 distinct lessons across at least 2 learning areas**.
- Progress survives day changes and app restarts; missing a day does not reset it.
- GMRC counts exactly like every other learning area.
- A repeated lesson ID cannot inflate progress.
- The next wildlife sticker is awarded at most once per child/week.
- Week rollover starts a new expedition while retaining previously collected stickers.

### Reward semantics

- First completion awards 1 star.
- Accuracy can add up to 2 mastery stars (`>=80%`, `>=95%`).
- Completion rewards remain idempotent through the deterministic key:
  `lesson-first:{childId}:{lessonId}`.
- Coins are still awarded by the lesson flow (10 per lesson at `>=80%` accuracy,
  idempotent through the reward ledger) and shown as a balance in the rewards
  hub, but there is no spend mechanism yet: no purchase, redeem, or exchange
  flow exists in code. The intended policy is that coins become meaningful only
  when a cosmetic use (e.g., Kindness Garden decorations) ships, until then
  the balance is purely informational.
- The Room database is now version 8 with a migration for `wildlife_expeditions`.

### Reduced motion

- Badge reveal animation is skipped when Android's animator duration scale is zero.
- The CI emulator disables animations and uses a fresh headless AVD to reduce false failures.

### Reward breaks and concept support

- The reward hub now exposes 29 bundled HTML games plus native reward games.
- Mini-games are bounded by the local entitlement policy and are isolated from
  network access through CSP and WebView request filtering.
- Lesson concept visuals are answer-neutral and remain subordinate to the
  activity prompt and response controls.

## Remaining follow-up (post-release enhancements)

1. Add a real cosmetic Kindness Garden reward at the existing level milestone;
   curriculum access remains independent of it.
2. Continue screenshot/accessibility review at compact phone, tablet, and
   approximately 1.3x font scale as new screens are added.
3. Obtain independent human curriculum review; automated metadata and tests are
   release gates, not a substitute for educator sign-off.

## Verification

```bash
cd android
./gradlew testDebugUnitTest assembleDebug lintDebug --stacktrace
./gradlew :core-database:connectedDebugAndroidTest \
  :feature-child-home:connectedDebugAndroidTest \
  :app:connectedDebugAndroidTest --stacktrace
```

Latest local results:

- Full unit suite, debug assembly, and lint: passed.
- Core database instrumentation: **19/19 passed**.
- Child Home instrumentation: **6/6 passed**.
- App instrumentation: **5/5 passed**.
- Remote hotspot migration rerun: still blocked before test execution by GitHub's emulator (`adb exit code 1`, `No compatible devices connected`); the workflow now adds fresh-AVD and headless-emulator hardening.
