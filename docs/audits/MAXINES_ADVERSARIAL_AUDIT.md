# Maxine's World Adversarial Platform Audit

**Audited baseline:** `eee430a22ef32170be10eb95eb0c9c45e7747c6f` (v0.76.0)
**Scope:** Android platform/logic only: authentication and child data, Room 11→12, media/offline, WebView, APK OTA, Media3 playback, ViewModel recovery, and user-controlled DB/file inputs. Content JSON and `server/` were excluded.

## Summary

| Severity | Count | Fixed here | Deferred |
|---|---:|---:|---:|
| CRIT | 0 | 0 | 0 |
| HIGH | 3 | 1 | 2 |
| MED | 5 | 2 | 3 |
| LOW | 2 | 0 | 2 |
| **Total** | **10** | **3** | **7** |

The Room 11→12 implementation matches schema 12 and preserves profiles. Downloaded media is size/SHA-256 verified before atomic promotion. OTA rejects absent/mismatched checksums and non-newer APKs, but its sidecar trust model remains insufficient over HTTP. The largest residual risk is parent authorization: every installation shares a source-visible default PIN and the reset flow uses an unlimited multiplication challenge.

## HIGH

### A-01 — Universal default PIN makes parent authorization predictable

**Evidence:** `android/feature-auth/src/main/java/com/maxinesworld/featureauth/ParentAuthManager.kt:33-35,53-64,112-123`; `android/feature-auth/src/test/java/com/maxinesworld/featureauth/ParentAuthLockoutTest.kt:88`
**Status:** Deferred

Fresh installs initialize the same six-digit PIN (`421988`), and reset restores it. A child who learns the value on one device can enter parent controls on every device; source/tests also disclose it. Salted hashing protects the stored representation but cannot add entropy to a shared credential.

**Proposed fix:** require caregiver-chosen PIN setup on first launch, store only a slow KDF result (PBKDF2/Argon2 with per-install salt), and remove the universal reset target. This changes onboarding and recovery semantics, so it was not safe for this narrowly scoped patch.

### A-02 — Math challenge bypasses both PIN secrecy and persisted lockout

**Evidence:** `android/feature-parent/src/main/java/com/maxinesworld/featureparent/ParentGateScreen.kt:294-348,352-413`; `android/feature-auth/src/main/java/com/maxinesworld/featureauth/ParentAuthViewModel.kt:367-392`
**Status:** Deferred

During lockout the UI explicitly offers a bypass. The challenge has only 56 factor pairs, unlimited retries, no delay, and its successful path restores the universal default PIN. A numerate child—or simple enumeration—can reset parent access without knowing the caregiver credential.

**Proposed fix:** make recovery caregiver-controlled (platform credential, recovery code, or authenticated companion flow), rate-limit and persist recovery failures, and never reset to a public fixed PIN. Product/recovery design is required.

### A-03 — Bundled JavaScript WebView lacked a platform-level network kill switch

**Evidence:** `android/app/src/main/java/com/maxinesworld/app/MiniGameWebScreen.kt:209-258,261-282`
**Status:** **Fixed**

The client interceptor attempted to block off-host requests, but JavaScript remained enabled and the WebView itself was permitted to initiate network loads. Client callbacks are defense-in-depth, not the strongest platform guarantee, and future callback/API behavior could expose a child-facing game to remote content or tracking.

**Fix:** set `WebSettings.blockNetworkLoads = true` while retaining file/content denial, mixed-content denial, navigation interception, and teardown. Regression contract: `android/tools/test_adversarial_platform_contracts.py`.

## MED

### A-04 — Media parser declared supported versions but accepted every future version

**Evidence:** `android/core-network/src/main/java/com/maxinesworld/corenetwork/MediaCatalogParser.kt:18-27,115-119`
**Status:** **Fixed**

The parser used `catalogVersion >= 1` even though only versions 1–3 are declared supported. A future incompatible catalog could be silently interpreted with old defaults and influence child-visible media, assessments, file paths, or download metadata.

**Fix:** require membership in `SUPPORTED_CATALOG_VERSIONS`. Regression contract: `android/tools/test_adversarial_platform_contracts.py`.

### A-05 — OTA sidecar checksum is not an authenticity proof over HTTP

**Evidence:** `android/core-network/src/main/java/com/maxinesworld/corenetwork/AppUpdateManager.kt:38-44,93-112,205-226`; `android/core-network/src/main/java/com/maxinesworld/corenetwork/MediaCacheManager.kt:51-53`
**Status:** Deferred

When no pinned checksum is supplied, APK and checksum are fetched from the same endpoint. On the permitted cleartext LAN path, an active network attacker can replace both. Android package-signature enforcement normally blocks a differently signed APK, but this code does not independently verify the expected signing certificate and therefore delegates the final trust decision to installer behavior.

**Proposed fix:** require a release-pinned SHA-256 delivered with the app, or verify the candidate APK signer against a pinned release certificate digest before returning `ReadyToInstall`; prefer HTTPS with certificate pinning. Preserve LAN fallback only as an explicitly managed mode.

### A-06 — Malformed OTA checksum tokens were not distinguished from valid hashes

**Evidence:** `android/core-network/src/main/java/com/maxinesworld/corenetwork/AppUpdateManager.kt:93-118,238-246`
**Status:** **Fixed**

Arbitrary sidecar/pinned text reached the comparison and was reported only as a mismatch. Strict format validation reduces ambiguity, prevents accidental acceptance logic from growing around malformed data, and makes the fail-closed boundary explicit.

**Fix:** require exactly 64 lowercase hexadecimal characters before comparison and delete the downloaded APK on failure. Regression contract: `android/tools/test_adversarial_platform_contracts.py`.

### A-07 — Child identity silently falls back to a synthetic shared profile

**Evidence:** `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/VideoLibraryViewModel.kt:67-68,362-366`; `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/AssessmentArenaViewModel.kt:56-57,83-85,209-211`
**Status:** Deferred

Missing/blank navigation state writes rewards and video progress under `default_child` instead of failing closed. A malformed internal route or future deep link can create orphan data and cross-session state that is not tied to a real profile.

**Proposed fix:** model absent child ID as an initialization error, verify the ID exists before writes, and expose a recoverable route error. This touches multiple feature state machines and needs Kotlin regression coverage.

### A-08 — Parent dashboard destination relies on navigation history, not authorization state

**Evidence:** `android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt:300-323`; compare gate transition at `305-310` with dashboard destination at `315-323`
**Status:** Deferred

The gate authenticates before normal navigation, but the dashboard destination itself has no session token or auth guard. Any future internal caller or deep-link exposure that navigates directly to `parent_dashboard/{childId}` bypasses the gate.

**Proposed fix:** maintain an expiring in-memory parent session and guard the dashboard route itself, redirecting to the gate when absent. Avoid persisting the session across process death.

## LOW

### A-09 — PIN hashing is fast rather than password-hard

**Evidence:** `android/feature-auth/src/main/java/com/maxinesworld/featureauth/ParentAuthManager.kt:126-163`
**Status:** Deferred

The per-install salt and constant-time comparison are sound, but two SHA-256 rounds are inexpensive to brute-force if app-private preferences are extracted. Six-digit PINs need a deliberately slow KDF.

**Proposed fix:** version the stored hash and migrate on successful verification to PBKDF2-HMAC-SHA256 (high iteration count) or Argon2id. Keep lockout as an online control.

### A-10 — Media eviction ignores deletion failure and cannot report an unsatisfied reservation

**Evidence:** `android/core-network/src/main/java/com/maxinesworld/corenetwork/MediaCacheManager.kt:37-49`; `android/core-network/src/main/java/com/maxinesworld/corenetwork/MediaStorage.kt:107-123`
**Status:** Deferred

`reserve()` discards the result of `File.delete()` and returns even when the requested space still cannot fit (including requests larger than the entire budget). The subsequent download will fail later with less actionable recovery behavior.

**Proposed fix:** reject `requiredBytes > maxStorageBytes`, remeasure after each deletion, and return/throw a typed insufficient-storage result if the budget cannot be met.

## Room 11→12 verification

`MaxinesMigrations.MIGRATION_11_12` checks `PRAGMA table_info(child_profiles)`, closes its cursor, and adds the non-null `filipinoProficiency` column with `BEGINNER` default only when absent (`android/core-database/src/main/java/com/maxinesworld/coredatabase/Migrations.kt:99-118`). The entity default and database version agree (`Entities.kt:16-25`, `MaxinesDatabase.kt:6-35`), and checked-in `schemas/com.maxinesworld.coredatabase.MaxinesDatabase/12.json` exists. Instrumented coverage preserves an existing profile and validates the default (`MigrationTest.kt:460-490`). No migration change was warranted.

## Other traced controls

- Media IDs are constrained before filesystem construction; downloads enforce catalog size and SHA-256 before atomic promotion (`MediaStorage.kt:24-40,60-90,126-131`; `MediaDownloader.kt:17-70`).
- Offline playback accepts only an existing non-empty local file and releases ExoPlayer on disposal (`VideoStep.kt:80-103,182-207`).
- APK candidates are deleted on checksum/version/package rejection and must have a newer version code (`AppUpdateManager.kt:98-143`).
- Web game slugs are resolved through the static catalog rather than accepted as asset paths (`MiniGameWebScreen.kt:59-67,97-111`).

## Verification

Required repository gates:

```text
python3 -m unittest discover -s android/tools
git diff --check
```

Kotlin/Gradle compilation was unavailable by task constraint; changes are deliberately small API-property/validation edits and no schema, route, content, test-tag, or happy-path behavior was changed.
