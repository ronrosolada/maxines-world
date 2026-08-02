# Grand UX, UI, and Gamification Review — Implementation Status

**Review baseline:** 3 August 2026
**Implementation branch:** `fix/grand-ux-gamification-review`

This note records which findings were addressed in code and which remain product decisions. It is intentionally explicit: passing tests do not make an unfinished engagement policy production-ready.

## Addressed in this branch

### Homepage trust

- Homepage streak and XP remain visible only because the ViewModel derives them from persisted completion data; there are no demo defaults in the production ViewModel.
- The disabled/no-op Progress destination was removed from child navigation.
- The misleading Avatars destination was renamed **Collection** and routes to the Wildlife Field Guide.
- The quest action routes to a subject or the Field Guide; the selected Home destination does not push a duplicate route.

### Collection and reward reveal

- The home preview is now **Wildlife Stickers**, backed by the same collected wildlife badge IDs and count used by the Field Guide.
- The preview shows at most three real earned animal stickers, ordered by earned timestamp, plus **Open Field Guide**. It no longer renders generic locked/demo slots on the homepage.
- Lesson reward reveal navigation carries the exact newly awarded badge ID into the Field Guide. The Field Guide selects that collected item when opened.

### Replay-safe rewards

- First-completion star and coin rewards use a deterministic key:
  `lesson-first:{childId}:{lessonId}`.
- Reward inserts use `IGNORE`, so replaying or concurrently completing the same lesson cannot insert another first-completion reward record.
- A DAO instrumentation test verifies the first insert succeeds and the second insert is ignored.

## Remaining release blockers from the review

These were not silently changed because they alter product policy or require new UX/data design:

1. **GMRC/Kindness gate:** still unlocks at Level 4. Decide whether GMRC is available from the first session and move progression rewards to cosmetic content.
2. **Daily five-subject challenge:** still uses the existing daily subject set. Replace it with a forgiving multi-day Wildlife Expedition before child-facing release.
3. **Star semantics:** first completion is now idempotent, but the existing accuracy-based star formula still needs a deliberate completion/mastery policy.
4. **Coins:** still have no child-facing use in this slice. Remove them or define one fixed, non-consumable cosmetic use before expanding the economy.
5. **Reduced motion:** completion/reveal animation accessibility still needs implementation and device verification.
6. **Responsive visual review:** compact phones, tablets, and large font scales still need representative emulator/screenshot review.

## Verification

```bash
cd android
./gradlew :feature-child-home:testDebugUnitTest \
  :feature-lesson-player:testDebugUnitTest \
  :feature-rewards:testDebugUnitTest
./gradlew :core-database:connectedDebugAndroidTest \
  :feature-child-home:connectedDebugAndroidTest
```

The database suite covers the stable reward-key insert behavior. The child-home suite covers the Collection navigation label and removal of the disabled Progress destination.
