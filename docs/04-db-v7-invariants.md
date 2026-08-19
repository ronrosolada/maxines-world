# DB v7 Uniqueness Invariants (audit 2026-08-01) + v8 delta (2026-08-06)

**Status:** Audited ✅, no schema correction needed.
**Scope:** `core-database` Room schema. v7 audited 2026-08-01; **v8 added on
`feat/lesson-visuals-and-player`** (`wildlife_expeditions`, `MIGRATION_7_8`)
for the Wildlife Expedition feature, additive, one new table, no v7 table
modified. Main still ships v7 until PR #58 merges.
**Audit trigger:** Implementation handoff Phase 4, verify intended product
cardinality, especially badge ownership.

## Verdict

The v7 schema correctly represents product cardinality. The `collected_badges`
composite index fix shipped in `b0831fb` is the right design:

- **Badge ownership is per-child**: `UNIQUE (childId, badgeId)`, two children CAN
  earn the same badge; one child CANNOT earn it twice.
- A global `badgeId` uniqueness would have been a bug (only one child in the app
  could ever own a given badge).

## Constraint inventory

| Table | Unique constraint | Product invariant |
|---|---|---|
| `collected_badges` | `(childId, badgeId)` | Badge ownership per-child; same badge once per child |
| `inventory` | `(childId, itemId)` | Item ownership per-child; same item once per child |
| `daily_quest_sets` | `(childId, dayKey)` | One quest set per child per day |
| `daily_quest_completions` | `(childId, dayKey, questId)` | One completion per quest per child/day |
| `playground_unlock_receipts` | `(childId, dayKey)` | One unlock receipt per child/day (prevents re-lock) |
| `lesson_completions` | `(childId, lessonId, attemptId)` | Attempt idempotency, replay-safe progress |
| `reward_ledger` | `(sourceKey)` | One ledger entry per source event, **dead code as of 2026-08-06: no production writer; `rewards` is the real currency ledger** (audit F4) |
| `mini_game_results` | `(idempotencyKey)` | No duplicate minigame submissions |
| `daily_challenges` | `(childId, challengeDate)` | One challenge per child per day |
| `reward_break_entitlements` | `(dailyQuestCompletionId)` | One break per quest completion |
| `content_packages` | `(packageId, version)` | One row per package version |
| `wildlife_expeditions` | `(childId, weekKey)` | One expedition row per child per week (v8) |

## Enforcement semantics

- All inserts use `OnConflictStrategy.IGNORE` (badges, inventory, quest sets,
  completions, unlock receipts, ledger), duplicates are silently dropped,
  first-write-wins.
- `REPLACE` used only for idempotent upserts (parent accounts, screen-time
  limits, content package registry) where last-write-wins is the contract.

## 2026-08-06 reliability additions (audit F1/F2)

- **Single parent row:** `ParentAuthViewModel` now reuses the existing parent
  row's id (or the constant `"parent"` on fresh installs) instead of a fresh
  UUID, and `ParentAccountDao.getParent()` orders by `createdAt ASC`, a second
  PIN setup can no longer strand child data under an orphaned parent id.
- **Corruption guard:** `DatabaseModule` runs `PRAGMA quick_check(1)` on the
  raw DB file before Room opens; a corrupt database is quarantined
  (`*.corrupt-<ts>` rename) so Room recreates fresh instead of crash-looping.
  `fallbackToDestructiveMigration()` is enabled as a last-resort crash
  prevention for unknown schema versions.

## Test coverage

`UniquenessInvariantTest` (instrumented, `core-database` androidTest):

1. `twoChildrenCanEarnTheSameBadge`, cross-child badge ownership allowed
2. `sameChildCannotEarnSameBadgeTwice`, same-child duplicate dropped (IGNORE)
3. `twoChildrenCanOwnSameItemButOneChildOnce`, inventory per-child + first-wins
4. `oneQuestSetPerChildPerDay`, quest sets per child/day + first-wins
5. `lessonCompletionUniquePerChildLessonAttempt`, attempt idempotency, original
   row preserved, distinct count unaffected

Plus `MigrationTest`: 1→2→3, 3→7, 4→7, 6→7, 7→8 (v5 never shipped to main  - 
no v5 migration needed; see audit F6).

## Rules going forward

- **Never edit a released schema in place.** Any future schema change ships
  as a new version with an additive migration tested from every shipped
  predecessor (see `MigrationTest`).
- Any new table's uniqueness must map to a documented product invariant before
  merge.
