# Release Review — v0.48.1 (2026-08-17)

**Branch:** `feat/deped-video-hub-pivot`  
**Release:** `0.48.1` / version code `61`  
**Purpose:** version synchronization and signed OTA distribution over the
installed `0.48.0` / version-code `60` baseline.

## Scope

This release carries the current DepEd Video Hub and Grade 3 Assessment Arena
implementation together with the adversarial UX hardening documented in the
previous review. The version metadata is intentionally advanced to code `61`
so Android package installation is monotonic on the target device.

## Included behavior

- DepEd Video Hub subject routing, deterministic grade/quarter/episode ordering,
  bulk downloads, watch-to-earn progress, and dual-homed trusted-LAN media.
- Grade 3 Assessment Arena quizzes for six subjects across Philippine,
  Singapore, and United States tracks, with item-count-aware 80% thresholds,
  responsive phone layouts, reward integration, and curriculum-linked badges.
- Parent-dashboard local OTA discovery, installed-version display, APK download,
  and Android package-install handoff.
- Adversarial UX fixes covering 56dp controls, narrow-phone spacing and wrapping,
  TalkBack tab/radio/progress semantics, WCAG AA contrast tokens, descriptive
  artwork text, deterministic video ordering, and idempotent reward insertion.

## Release verification

- [x] `versionName` is `0.48.1` and `versionCode` is `61`.
- [x] Focused unit-test modules completed successfully.
- [x] `assembleRelease` completed with the configured user-level signing key.
- [x] APK metadata and v2 signature verified with Android SDK tools.
- [x] APK copied to both DreamNAS trusted-LAN content paths.
- [x] Branch pushed and local status verified clean and synchronized.

## Distribution boundaries

The bundled lesson and reward-break experience remains offline-first. Media
catalogs and APK updates are trusted-LAN features only; they are not cloud
content synchronization or telemetry.
