# Gamification mechanics

This document is the source-of-truth summary for the child-facing learning reward loop.

## Product loop

`Learn → receive immediate appreciation → grow Milo's sanctuary → choose cosmetic rewards → take a bounded break`

Learning remains the purpose of the app. Rewards never gate curriculum access, punish mistakes, or provide an academic advantage.

## Reward types

| Type | Source | Spendable? | Child-facing meaning |
| --- | --- | --- | --- |
| `ACTIVITY_PAW` | Every completed activity step | No | A persisted paw-print receipt for finishing a task |
| `STAR` | First distinct lesson completion | No | Learning progress; mastery adds stars |
| `COIN` | First distinct lesson completion and valid mini-game result | Yes | Sanctuary token used only for cosmetic workshop items |
| `SANCTUARY_PIECE` | Daily Quest completion at 3/3 | No | A permanent piece that grows Milo's Wildlife Sanctuary |
| Wildlife sticker | Weekly expedition | No | Collection item plus a factual animal card |
| Reward-break entitlement | Daily Quest completion at 3/3 | One 5-min window | A bounded 5-minute play session, re-armed while the day-pass holds |
| Playground day-pass | Daily Quest completion at 3/3 | One per child/day | Re-enterable playground unlock for the rest of the local day |

The database retains `COIN` as the compatibility name for the existing balance. Child-facing copy calls it `Tokens` or `sanctuary tokens`. The playground day-pass lives in `playground_unlock_receipts {id="$childId:$dayKey", childId, dayKey, sourceQuestSetHash, unlockedAt}` with `insertIgnoring` (first-write-wins) and `dayKey = LocalDate.now(ZoneId.systemDefault()).toString()` midnight expiry.

## Lesson policy

Every first distinct lesson completion grants a base reward, regardless of accuracy:

- 1 Learning Star
- 1 Sanctuary Token
- +1 Learning Star at 80% accuracy
- +1 Learning Star at 95% accuracy
- +1 Sanctuary Token at 80% accuracy

Retries do not remove the base reward. Replays cannot farm the reward because grants use deterministic source keys.

## Activity policy

Each activity result writes one `ACTIVITY_PAW` grant with the source key:

```text
activity:{childId}:{lessonId}:{activityId}
```

The reward is deliberately not a spendable balance. It gives the child immediate feedback and a visible completion message without encouraging rapid tapping or answer guessing.

## Daily Quest policy

The current Daily Quest has three assigned lesson targets. A target is recorded when its lesson completion is committed. At 3/3, one transaction creates:

- one `SANCTUARY_PIECE` grant;
- one `CREATED` reward-break entitlement (5-minute window);
- one `playground_unlock_receipts` day-pass (`"$childId:$dayKey"` → rest of local day, re-enterable);
- the corresponding idempotent Daily Quest completion state.

The source keys are deterministic:

```text
daily-quest:{childId}:{dayKey}:piece
reward-break:{childId}:{dayKey}
playground-unlock:{childId}:{dayKey}   // receipt id "$childId:$dayKey"
```

Completing only 1/3 or 2/3 must not create the Daily Quest bonus, reward break, or day-pass. Reconciliation from the child home can safely repair an interrupted completion.

## Playground day-pass

Once 3/3 is reached, the playground is **re-enterable for the rest of the local calendar day**. Every hub entry re-arms the 5-minute session via `RewardBreakDao.reactivateForDayPass(id, childId, now, 5 min)` (atomic `ACTIVE` + `startedAt=now` + `remaining=5 min` + clear `consumedAt`). `RewardBreakViewModel.consume()` keeps the entitlement `ACTIVE` while the day-pass holds; `saveResult()` bypasses the `ACTIVE`-window check while the day-pass holds and relies on `idempotencyKey` duplicate suppression to prevent farming. `PlayroomHomeViewModel` observes `playground_unlock_receipts` via Flow and flips the quest card to `Open Playground` when `playgroundUnlocked` is true. Midnight (`LocalDate.now(ZoneId.systemDefault())`) expires the pass.

## Sanctuary

Milo's Wildlife Sanctuary contains 12 deterministic pieces. The Playroom shows:

- earned piece count;
- the next piece name;
- collected pieces when available;
- the Daily Quest reward preview before completion.

The Sanctuary Workshop contains cosmetic decorations only. Former reward-multiplier perks are compatibility no-ops; owning a decoration cannot alter future lesson rewards.

## Mini-game policy

A valid, in-window mini-game result is persisted once by idempotency key. In the same transaction:

- `pawTokensEarned` becomes a `COIN` grant with a stable source key;
- `collectibleId` becomes an idempotent inventory item.

A duplicate result cannot create duplicate tokens or collectibles. While the playground day-pass holds (until midnight local), `RewardBreakViewModel.saveResult()` accepts results via the same idempotency gate without requiring a still-`ACTIVE` 5-minute window, and `consume()` does not set `CONSUMED` — keeping the playground re-enterable.

## Verification invariants

Tests should continue to cover:

- base reward at low accuracy;
- mastery bonus thresholds;
- activity retry idempotency;
- lesson replay idempotency;
- Daily Quest behavior at 1/3, 2/3, and 3/3;
- reward-break creation only at 3/3;
- playground day-pass creation only at 3/3 and re-enterability until midnight;
- mini-game result idempotency (including day-pass path);
- sanctuary progress and reward preview visibility;
- offline/restart safety;
- TalkBack labels, large text, and reduced-motion behavior.
