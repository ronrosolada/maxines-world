# Content Packaging — Bundled-Only (2026-08-01)

**Decision: no external content server.** All educational content ships inside
the APK (`app/src/main/assets/content-pack/`). DreamNAS sync is retired.
The content repo (github.com/ronrosolada/maxines-world-content) is the
authoring source of truth; each APK release bundles the current month/quarter
content into the playable pack.

## How content gets into the app

1. Authoring home: `maxines-world-content` repo — 62 weekly packages
   (catalog v2 + `.zip` packages per subject/week).
2. The app repo mirrors the DepEd Matatag SLM source under
   `app/src/main/assets/content/ph-matatag/grade-3/`.
3. `tools/convert_slm_to_pack.py` converts SLM source → playable
   `Month1Lesson` format in `app/src/main/assets/content-pack/month-01/lessons/`.
4. The APK build bundles the pack; the app reads it from assets at runtime
   (`ActiveContentIndex` catalog v2 + `LessonLoader`).

## Runtime layout

```
assets/content-pack/
├── month-01/
│   ├── lessons/          # 329 lessons (100 legacy + 229 converted)
│   └── days/             # Day manifests
└── catalog.json          # ActiveContentIndex catalog
```

No `filesDir/content/` usage, no download, no SHA-256 verification at
runtime — the APK is the immutable, versioned unit.

## Content updates

Any content change = update `maxines-world-content` (or the SLM source),
regenerate the pack (converter), rebuild the APK, ship a new release.
There is no content-only update path — by design: the app is self-contained
and never depends on network availability.

## What was removed

- `nas-deployment/` (Caddy content server, catalog, package ZIPs, deploy/verify scripts)
- Runtime sync: `ContentSyncWorker` enqueue from parent screen, `content/active/` scan paths
- Rollback/staging machinery (`content/staging/`, `content/rollback/`)

## Why

- Child-safe, privacy-first: no network dependency, no server to reach, no
  tracking of what the child loads.
- Simpler correctness story: the APK is the only versioning unit; upgrade
  and content update are the same action.
