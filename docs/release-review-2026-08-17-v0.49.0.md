# Release Review — v0.49.0 (2026-08-17)

**Branch:** `feat/deped-video-hub-pivot`
**Release:** `0.49.0` / version code `62`
**Purpose:** integrate Quick Bits into the signed DepEd learning release while
preserving the Video Hub, Assessment Arena, progress sync, and offline-first
lesson experience.

## Scope

This release adds a child-facing Quick Bits vertical video feed backed by a
bundled catalog of 60 educational 480p videos. It is designed for quick
exploration, with local caching so downloaded videos remain available offline.
The existing Assessment Arena and DepEd Video Hub remain part of the same
release and retain their trusted-LAN/offline boundaries.

## Included behavior

- Quick Bits route from the Playroom home card.
- TikTok-style vertical paging through four categories: animals, space,
  science, and math (15 videos each).
- Single-video and bulk download actions, progress feedback, local cache
  detection, and clear-all cache management.
- Tactile spring press feedback on the Playroom Quick Bits entry card.
- Existing DepEd Video Hub subject routing, downloads, watch-to-earn progress,
  Assessment Arena quizzes, progress-sync worker, and parent-dashboard OTA flow.

## Release verification

- [x] `versionName` is `0.49.0` and `versionCode` is `62`.
- [x] Quick Bits catalog parsing unit coverage is present.
- [x] `testDebugUnitTest` completed successfully.
- [x] `assembleRelease --no-daemon` completed with the configured user-level
  signing key.
- [x] APK metadata reports version code `62` and version name `0.49.0`.
- [x] APK v2 signature verification completed successfully.
- [x] Quick Bits launch, vertical paging, offline cache behavior, Assessment
  Arena, and Video Hub were checked on the available emulator/device path.
- [x] APK copied to DreamNAS root and media paths and to local Downloads.
- [x] Branch pushed and local status verified clean and synchronized.

## Distribution boundaries

Quick Bits assets and cached downloads work offline. Video catalogs and APK
updates remain trusted-LAN features; the app does not add cloud telemetry or
cloud content synchronization.
