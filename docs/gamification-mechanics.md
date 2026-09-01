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
| Reward-break entitlement | Daily Quest completion at 3/3 | One 5-min window | One bounded 5-minute play session; remaining time can resume until it is consumed or expires |
| Playground unlock receipt | Daily Quest completion at 3/3 | One per child/day | Ledger that today's quest completed; it does not re-arm extra sessions |

The database retains `COIN` as the compatibility name for the existing balance. Child-facing copy calls it `Tokens` or `sanctuary tokens`. The unlock receipt lives in `playground_unlock_receipts {id="$childId:$dayKey", childId, dayKey, sourceQuestSetHash, unlockedAt}` with `insertIgnoring` (first-write-wins). The playable window is the `reward_break_entitlements` row for that child/day.

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
- one `CREATED` reward-break entitlement (one 5-minute window);
- one `playground_unlock_receipts` row recording that today's quest completed;
- the corresponding idempotent Daily Quest completion state.

The source keys are deterministic:

```text
daily-quest:{childId}:{dayKey}:piece
reward-break:{childId}:{dayKey}
playground-unlock:{childId}:{dayKey}   // receipt id "$childId:$dayKey"
```

Completing only 1/3 or 2/3 must not create the Daily Quest bonus or reward break. Reconciliation from the child home can safely repair an interrupted completion.

## One 5-minute play break

Once 3/3 is reached, the child may open the playground for **one 5-minute session**. The entitlement starts when a game is chosen, can resume with remaining time if the child leaves early, and becomes unusable after `consume()` or expiry. `RewardBreakViewModel` does not re-arm a fresh 5 minutes. `PlayroomHomeViewModel` observes the entitlement and shows `Open Playground` only while `RewardBreakPolicy.canUse` is true; after the break is consumed the card returns to `Open Sanctuary`. God mode still opens the playground without an entitlement.

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

A duplicate result cannot create duplicate tokens or collectibles. Mini-game results must fall inside the active 5-minute window (`RewardBreakPolicy.isValidResultWindow`). After the entitlement is consumed, the playground cannot be re-entered until the next day's Daily Quest.

The child shelf is an 8-year-old allowlist (`MiniGameShelf`). Wordle, Sudoku, Solitaire, FreeCell, Checkers, Reversi, Mancala, Yahtzee, and Domino stay bundled but are hidden from the child route.

## Verification invariants

Tests should continue to cover:

- base reward at low accuracy;
- mastery bonus thresholds;
- activity retry idempotency;
- lesson replay idempotency;
- Daily Quest behavior at 1/3, 2/3, and 3/3;
- reward-break creation only at 3/3;
- one 5-minute playground session with no re-arm after consume or expiry;
- mini-game result idempotency inside the active window;
- child mini-game allowlist (Wordle/Sudoku/Solitaire/Yahtzee hidden);
- sanctuary progress and reward preview visibility;
- offline/restart safety;
- TalkBack labels, large text, and reduced-motion behavior.
