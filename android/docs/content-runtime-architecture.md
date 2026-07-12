# Content Runtime Architecture

Maxine's World uses a decoupled architecture where the Android application is a stable learning runtime and educational content is independently authored, reviewed, versioned, and published as immutable packages.

## Three Systems

```
Android Learning Runtime     Educational Content Repository     Content Server (NAS)
        ↓ reads packages              ↓ publishes releases              ↓ serves files
  Generic player + progress      YAML lessons + assessment +      Read-only Caddy container
  Room persistence + rewards     review + provenance + assets     No learner data, no write API
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

## Package Lifecycle

```
Fetch catalog → verify compatibility → download to staging → 
verify SHA-256 → extract safely → validate schema → 
atomic activate → retain rollback → clean staging
```

## Content Updates

| Change type | What's needed |
|---|---|
| Fix a typo in a lesson | Content release only (increment contentVersion) |
| Add an illustration | Content release only |
| Change an assessment | Content release only |
| New activity type | Android release first (add capability), then content release |
| Database schema change | Android release with migration |
| Reward rule change | Android release only |

## Server

DreamNAS @ 10.10.10.33:80 — read-only Caddy container. No auth, no learner data, no write endpoints.
