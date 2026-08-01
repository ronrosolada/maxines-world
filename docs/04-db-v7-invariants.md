# DB v7 Uniqueness Invariants (audit 2026-08-01)

**Status:** Audited ✅ — no schema correction needed. No v8 required.
**Scope:** `core-database` Room v7 schema (`MaxinesDatabase.kt`, schemas/7.json)
**Audit trigger:** Implementation handoff Phase 4 — verify intended product cardinality,
especially badge ownership.

## Verdict

The v7 schema correctly represents product cardinality. The `collected_badges`
composite index fix shipped in `b0831fb` is the right design:

- **Badge ownership is per-child**: `UNIQUE (childId, badgeId)` — two children CAN
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
| `lesson_completions` | `(childId, lessonId, attemptId)` | Attempt idempotency — replay-safe progress |
| `reward_ledger` | `(sourceKey)` | One ledger entry per source event |
| `mini_game_results` | `(idempotencyKey)` | No duplicate minigame submissions |
| `daily_challenges` | `(childId, challengeDate)` | One challenge per child per day |
| `reward_break_entitlements` | `(dailyQuestCompletionId)` | One break per quest completion |
| `content_packages` | `(packageId, version)` | One row per package version |

## Enforcement semantics

- All inserts use `OnConflictStrategy.IGNORE` (badges, inventory, quest sets,
  completions, unlock receipts, ledger) — duplicates are silently dropped,
  first-write-wins.
- `REPLACE` used only for idempotent upserts (parent accounts, screen-time
  limits, content package registry) where last-write-wins is the contract.

## Test coverage

`UniquenessInvariantTest` (instrumented, `core-database` androidTest):

1. `twoChildrenCanEarnTheSameBadge` — cross-child badge ownership allowed
2. `sameChildCannotEarnSameBadgeTwice` — same-child duplicate dropped (IGNORE)
3. `twoChildrenCanOwnSameItemButOneChildOnce` — inventory per-child + first-wins
4. `oneQuestSetPerChildPerDay` — quest sets per child/day + first-wins
5. `lessonCompletionUniquePerChildLessonAttempt` — attempt idempotency, original
   row preserved, distinct count unaffected

## Rules going forward

- **Never edit the released v7 schema in place.** Any future schema change ships
  as v8+ with an additive migration tested from v7 (see `MigrationTest`).
- Any new table's uniqueness must map to a documented product invariant before
  merge.
