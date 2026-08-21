# Video playlist replacement — 2026-08-20

## Status

This document records the personal-use video catalog replacement shipped with the
`feat/playlist-video-replacement` branch. The app source contains the assessment
manifest; the MP4 files and generated catalog are deployed separately to the
trusted DreamNAS content server.

- **Source workbook:** `Video Lesson Sorting - Updated.xlsx`
- **Selected scope:** 237 titled rows from the Grade 1–4 workbook playlists
- **Playlist total in source workbook:** 464 rows
- **Catalog version:** 1
- **Release status:** `PREVIEW`
- **License status:** `PERSONAL_USE`
- **Catalog endpoint:** `http://10.10.10.33/media/catalog.json`
- **Media root:** `http://10.10.10.33/media/`
- **App package:** `com.maxinesworld.app`
- **Release APK version:** `0.56.0-6-g8647a8ae` (`versionCode 292`)

The endpoint is intended for the home LAN only. Do not expose the HTTP endpoint
outside the trusted network. Use an HTTPS Caddy route and review the source
licenses before any broader distribution.

## Catalog contents

The workbook's `Subject` column is the canonical source for subject placement.
Grade 1 and Grade 2 contain Filipino, Makabansa, and GMRC. Grade 3 and Grade 4
also contain Mathematics, Science, and English.

| Subject | Videos |
|---|---:|
| Filipino | 100 |
| Makabansa | 51 |
| Mathematics | 24 |
| English | 22 |
| GMRC | 20 |
| Science | 20 |
| **Total** | **237** |

| Grade | Videos |
|---|---:|
| Grade 1 | 29 |
| Grade 2 | 53 |
| Grade 3 | 95 |
| Grade 4 | 60 |
| **Total** | **237** |

Every entry has a stable `mediaId`, real workbook-derived title, subject ID,
grade level, quarter, episode number, relative MP4 path, SHA-256, size, and
stream metadata. Files are stored below subject/grade directories, for example:

```text
media/playlists/english/g3/yt-kr4unsat2yk.mp4
media/playlists/mathematics/g4/yt-<video-id>.mp4
```

The app sorts lessons by subject and then episode number. Duplicate episode
numbers or repeated topic names are not treated as interchangeable: the stable
YouTube-derived media ID remains the identity key.

## Assessment policy

The tracked source of truth is:

```text
android/app/src/main/assets/content-pack/media-assessments.json
```

Each of the 237 videos has five `MULTIPLE_CHOICE` memory-check items:

- **Items per video:** 5
- **Total items:** 1,185
- **Passing score:** 4/5 (80%)
- **Mastery claim:** false
- **Answer choices:** four per item, IDs `a` through `d`
- **Scoring:** the existing watch-completion and 80% pass/reward flow is retained

Assessment language follows the subject:

- **English:** English, Mathematics, Science
- **Filipino:** Filipino, Makabansa, GMRC

These questions check recall of the video and do not claim mastery of the broader
curriculum. The video remains optional media: a child can continue using the
bundled lesson experience when the LAN server is unavailable.

## App behavior

1. The video library requests and validates the catalog.
2. Subject filters expose only entries carrying that subject ID.
3. Lessons are ordered by subject and episode number.
4. The first eligible lesson is unlocked according to the existing progression
   rules; later lessons remain locked until the normal prerequisite is met.
5. A child explicitly downloads a video before offline playback.
6. The app verifies the response, size, and SHA-256 before promoting the file from
   its temporary download path into private app storage.
7. Playback completion enables the five-question memory check.
8. Four correct answers pass the check and preserve the established reward flow;
   retry behavior remains available without a mastery claim.

## Deployment layout

DreamNAS stores the read-only Caddy content at:

```text
/mnt/user/appdata/maxines-world-content/server/content/
├── app-release.apk
├── catalog.json
└── media/
    ├── catalog.json
    └── playlists/
        ├── english/g3/*.mp4
        ├── filipino/g1/*.mp4
        ├── gmrc/g*.mp4
        ├── makabansa/g*.mp4
        ├── mathematics/g*.mp4
        └── science/g*.mp4
```

`media/catalog.json` is the media endpoint consumed by the Android app. The
root `catalog.json` is retained for the content server's existing clients. Both
catalog copies must describe the same 237 assets.

The release APK is served at:

```text
http://10.10.10.33/app-release.apk
```

Before replacing a deployed artifact, preserve a timestamped rollback copy. The
2026-08-20 deployment preserved:

```text
app-release.apk.bak-playlist-20260820-121500
```

Do not publish downloader sidecars such as `.info.json` files. Only MP4 media,
validated catalogs, and the intended APK belong in the read-only content root.

## Build and validation

Run the content and Android gates from the repository before publishing:

```bash
cd android
./gradlew check assembleRelease
./gradlew :core-network:testDebugUnitTest \
  :feature-lesson-player:testDebugUnitTest
./gradlew :core-database:connectedDebugAndroidTest \
  :feature-child-home:connectedDebugAndroidTest \
  :app:connectedDebugAndroidTest
```

Inspect the release artifact before deployment:

```bash
apkanalyzer apk summary android/app/build/outputs/apk/release/app-release.apk
autosign="android/app/build/outputs/apk/release/app-release.apk"
sha256sum "$autosign"
```

Release signing is workstation-only through
`~/.gradle/maxines-world-signing.properties`; credentials and keystores must not
be committed.

For a deployed catalog, verify all of the following:

- HTTP status 200 for `/media/catalog.json`
- Exactly 237 media entries
- Subject totals match the table above
- Every referenced MP4 returns `video/mp4`
- Local and deployed catalog SHA-256 values match
- A sample MP4 downloads successfully
- The app can open the library and download the first unlocked lesson

The verified release APK hash for this deployment is:

```text
549e93357753c0570108984ec4ae5dca552b4d0982715f2db8a477203dc23795
```

## Rollback

If the APK or catalog fails verification:

1. Stop publication of the affected artifact.
2. Restore the matching timestamped `.bak-playlist-*` file.
3. Recheck its SHA-256 and HTTP response.
4. Leave the media catalog and APK versions paired; do not mix a new APK with an
   incompatible catalog.
5. Record the failure and new hash in the release handoff before retrying.

The MP4 deployment also keeps the prior catalog backups beside the active files.
Never delete the rollback copy until the replacement has passed an emulator smoke
test and a LAN download check.

## Known boundaries

- This is a personal-use preview catalog, not a public content release.
- The source workbook intentionally selects 237 of 464 playlist rows.
- Network media is optional and LAN-only; core lessons remain bundled.
- The HTTP endpoint must be replaced with HTTPS before use outside the home LAN.
- Source licensing and educator review remain separate from structural catalog
  validation.
- The generated catalog is deployment output; the tracked assessment manifest is
  the repository source of truth for the five-question policy.

## Change record

The implementation branch contains:

- the 237-entry assessment manifest replacement;
- the optional-media documentation and deployment contract;
- regression-test updates for current child-home, database, and reward APIs;
- the release APK built, installed, smoke-tested, and published to DreamNAS.

The branch should be reviewed and merged through the normal GitHub pull-request
workflow rather than committing directly to `main`.

---

*Last updated: 2026-08-20.*

[Back to the repository README](../README.md)
 | [Optional media contract](../android/docs/optional-video-media.md)
 | [Release handoff](../HANDOFF.md)
