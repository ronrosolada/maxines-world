## Summary

This PR implements all seven phases of the cat-first remediation plan for Maxine's World v0.6.x:

1. **Safe typed subject routing** — `Subject` enum with explicit mapping; unknown IDs never open English
2. **Atomic idempotent lesson completion** — `LessonCompletionEntity` with (childId, lessonId, attemptId) unique index; deterministic reward/progress IDs
3. **Fish treat economy** — `FishTreatPolicy` replaces STAR/COIN; 3 base +1 retry +2 mastery, max 6 per attempt
4. **Live homepage state** — `VillageHomeViewModel` drives all displayed values from Room; zero fabricated defaults
5. **Redesigned village UX** — compact header (avatar+name+fish treats), Mira bubble, safe hotspot lookups, bottom nav (Home/Discoveries/Cat Café/Parents)
6. **Mira English loop** — Mira is the English lesson guide; full vertical slice: request→lesson→reward→café progress
7. **Accessibility** — contentDescription, Role.Button, merged semantics on all interactive elements

## Checklist

- [x] All six subject routes verified
- [x] Unknown destinations never open English
- [x] History/philippine-history/makabansa → History/Makabansa
- [x] Lesson completion is idempotent (duplicate submissions rewarded once)
- [x] Deterministic completion/reward/event IDs
- [x] Fish treats are the only spendable child currency
- [x] Homepage values come from persistent Room state
- [x] No fabricated defaults (Level 12, 660 XP, 7-day streak removed)
- [x] Mira drives English learning loop
- [x] Badge uniqueness scoped by (childId, badgeId)
- [x] Compact and expanded layouts have equivalent navigation
- [x] 48dp touch targets on all interactive elements
- [x] TalkBack labels on destinations, Mira bubble, and bottom nav
- [x] Migration 3→4 included (lesson_completions + badge index fix)

## Verifiable State

| Item | Value |
|------|-------|
| Final commit | `e7cdd18` |
| Baseline commit | `a49502f` |
| Changed files | 9 files + 3 new |
| Tests added | 27 (17 Subject + 10 FishTreatPolicy) |
| `:app:assembleDebug` | ✅ |
| `:core-model:testDebugUnitTest` | ✅ PASS (27/27) |
| `:app:lintDebug` | ✅ |

## Migration

Database version bumped from 3 → 4. Migration creates:
- `lesson_completions` table
- Unique index on (childId, lessonId, attemptId)
- Drops old global badgeId unique index
- Creates (childId, badgeId) unique index

## Rollback

```bash
git checkout origin/main
# Database: manually drop lesson_completions + recreate badge index
```

## Known Limitations

- `engine-mastery:testDebugUnitTest` has 1 pre-existing failure (unchanged from main)
- Instrumentation tests blocked (no physical device)
- Filipino TTS accent issue not addressed (device lacks Filipino voice)
- Reduced-motion formal support deferred (app uses minimal animations)
- Screen-time enforcement is display-only
- Sentence-builder activity engine is no-op

## Screenshots

Required per spec: compact homepage, tablet homepage, Mira request, English lesson with Mira, lesson result, fish-treat reward, Cat Café preview, café before unlock, café after unlock, wildlife discovery, parent gate, increased font scale.
