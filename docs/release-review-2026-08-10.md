# Maxine's World — Release Review Brief (`v0.27.0`)

**Status:** Release candidate; commit `b093370` pushed, pending remote CI and tag.

**Branch:** `feat/optional-offline-video-pack`
**Pull request:** #74
**Version:** `0.27.0` / version code `28`

## Release scope

- Playroom home polish with transparent mascot and subject artwork.
- Sanctuary presented as a next-reward surface with progress and the next piece.
- Wildlife Stickers retained as the collection/Field Guide surface.
- Decorative header avatar/profile control removed.
- Optional media catalog and resumable media downloads from the trusted home LAN.
- Unified mini-game/reward-break presentation and reward-flow fixes.
- Reversible PIN-gated parent god mode for development and QA.

## LAN media policy

LAN media is intentional for the home deployment. The release includes
`android.permission.INTERNET` only to support the optional media catalog and
media downloads. Core lessons and reward-break mini-games remain bundled and
playable without network access.

Cleartext traffic is restricted by `network_security_config.xml` to the
configured home-LAN media host (`10.10.10.33`). The media path is not a
telemetry channel, cloud content-sync path, or lesson-delivery dependency. The
endpoint must use HTTPS before it is exposed outside the trusted home network.

## Local verification

| Gate | Result |
|---|---|
| `:app:verifyPlayableContent` | 358/358 playable lessons approved and released |
| Content quality audit | Passed with `--check` |
| Content schema/asset validation | 358 lessons, 0 errors, 0 warnings |
| Content tooling tests | 9/9 passed |
| Core-database connected tests | 29/29 passed on API 35 emulator |
| App connected tests | 30/30 passed on API 35 emulator |
| Auth connected tests | 4/4 passed on API 35 emulator |
| Child-home connected tests | 19/19 passed on API 35 emulator |
| Rewards connected tests | 5/5 passed on API 35 emulator |
| Parent connected tests | 0 tests defined; task completed successfully |
| JVM unit-test suite | Gradle `testDebugUnitTest` passed |
| Lint | Completed; no fatal lint errors |
| Release assembly | Completed successfully; SHA-256 `3b7db38a747e3a26a33b29796444545b0619dda36f19689594c27c7fed884f04` |
| APK signature | `apksigner verify --verbose` passed using APK Signature Scheme v2 |

## CI failure and fix

PR #74's previous migration-test failure was environmental, not an application
test failure. The API 34 emulator could not create its userdata partition:
GitHub Actions configured `disk-size: 4G`, while the prebuilt test artifacts
required at least `7.37 GB`.

The workflow now uses `disk-size: 8G` with an explanatory comment. The fix must
be verified by the next remote CI run before the release tag is created.

## Final release checklist

- [x] Version and version code are `0.27.0` / `28`.
- [x] Release signing configuration remains user-only and untracked.
- [x] Educator and mini-game gates pass locally.
- [x] LAN permission/policy is documented and intentional.
- [x] `git diff --check` passes.
- [x] Commit and push the complete release candidate (`b093370`).
- [ ] All PR checks pass, including API 34 migration tests.
- [ ] Rebuild and inspect the exact APK from the clean release commit.
- [ ] Create and push the `v0.27.0` tag.
- [ ] Create the GitHub release with the exact APK attached.

The APK is a build artifact and is not committed to Git.
