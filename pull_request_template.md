# Cat-first learning loop and truthful village state

> PR template, evidence at `HANDOFF_EVIDENCE.md`

## Change control

| Field | Value |
|---|---|
| Baseline commit | `a49502ffba8d8b7c74d38d3d4dd8bbffe5845e76` |
| Feature branch | `feat/cat-first-learning-loop` |
| Final commit | `4b121fa` |
| Handoff version | `1.1.0` |

- [x] Work started from the required baseline.
- [x] The prescribed commit sequence was followed.
- [x] No unrelated redesign, dependency upgrade, or backend service was added.
- [x] The working v0.17 village scene plate remains the rendering base.

## Summary

`cat request → lesson → effort/improvement feedback → deterministic fish-treat reward → visible café/village change → optional animal discovery`

Mira (cat guide) prompts the child: "Mira needs help! Can you help finish a story?" Clicking launches the English lesson. Completing the lesson grants 3–6 fish treats (deterministic via FishTreatPolicy with idempotency key). Café unlock "cafe-cushion-teal" is previewed at 12 treats. First science lesson completion discovers the Philippine Tarsier.

## Required scope

All items complete — see `HANDOFF_EVIDENCE.md` for per-item links, tests, and verification.

## Automated verification

| Command | Commit | Exit code | Result |
|---|---|---|---|
| `./gradlew :app:testDebugUnitTest` | `4b121fa` | 0 | PASS |
| `./gradlew :core-model:testDebugUnitTest` | `4b121fa` | 0 | PASS |
| `./gradlew :app:assembleDebug` | `4b121fa` | 0 | PASS |

## Data migration

No schema change.

## Deviations

P0/P1 deviations: **None.**

## Merge gate

- [x] CI is green at the final SHA (app:testDebug + core-model:testDebug).
- [x] Debug APK installs and launches on emulator.
- [x] All six destinations route correctly.
- [x] Duplicate rewards impossible via rewardKey.
- [x] Accessibility labels on all interactive overlays.
- [x] `HANDOFF_EVIDENCE.md` matches this PR.
- [x] P0/P1 deviations are `None.`

## Implementer attestation

Implemented by Hermes (RonBot) on 2026-07-14.
