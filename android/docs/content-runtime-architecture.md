# Content Runtime Architecture

Maxine's World uses a decoupled architecture where the Android application is a stable learning runtime and educational content is authored in a dedicated repo, converted, and **bundled inside the APK** (no external content server).

## Two Systems

```
Content repo (ronrosolada/maxines-world-content)     Android Learning Runtime
        ↓ converted into the pack                            ↓ reads bundled assets
  Weekly packages, catalog v2                        Generic player + progress
  DepEd SLM source, assets                           Room persistence + rewards
```

Content pipeline (offline, deterministic):

```
maxines-world-content (authoring: 62 weekly packages)
        ↓ SLM source mirrored in app assets
ph-matatag/grade-3 (assets)
        ↓ tools/convert_slm_to_pack.py
content-pack/month-01/lessons/ (playable Month1Lesson format)
        ↓ APK build
Bundled assets → ActiveContentIndex (catalog v2) → LessonLoader
```

## Schema Version 1 (Frozen 2026-07-13)

| Property | Value |
|---|---|
| schemaVersion | 1 |
| Minimum app version | v0.9.x |
| Capabilities | ANIMATED_EXPLANATION_V1, MULTIPLE_CHOICE_V1, SORT_AND_CLASSIFY_V1, HOTSPOT_IMAGE_V1, MATCHING_PAIRS_V1, SEQUENCE_BUILDER_V1, INTERACTIVE_SPEC_V1 |
| Assessment model | 5 items, 4/5 pass threshold |
| Activity model | 6 activities per lesson, stable IDs |
| Badge model | 5 subjects/day → 1 badge, 50 badges total |

## Package Lifecycle (bundled)

```
Author/review SLM source → convert to Month1Lesson → commit to assets →
APK build bundles the pack → release signed APK → install/upgrade replaces content atomically
```

## Content Updates

| Change type | What's needed |
|---|---|
| Fix a typo in a lesson | Regenerate pack + APK release |
| Add an illustration | APK release (asset bundled) |
| Change an assessment | APK release |
| New activity type | Android release first (add capability), then content in the pack |
| Database schema change | Android release with migration |
| Reward rule change | Android release only |

## Server — RETIRED

There is no content server. DreamNAS (10.10.10.33) is no longer a content
source; the app never contacts it. Authoring lives in
`ronrosolada/maxines-world-content`; the APK bundles the playable pack.
See `content-sync-and-rollback.md` for the full bundled-only decision.
