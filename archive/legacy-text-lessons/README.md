# Legacy Text-Based Lessons Archive

## Overview
This archive preserves the legacy text-based interactive lessons and companion SVG visual assets from the early development phases of Maxine's World.

## Deprecation & Transition
- **Status:** Archived & Deprecated for new development.
- **Active Paradigm:** Maxine's World uses the **DepEd Matatag Video-First Lessons** (streamed/cached MP4 video hub with Whisper subtitle sync) and the **Assessment Arena** (`media-assessments.json` & `assessment-packs/`).
- **Compatibility:** A bundled copy remains in `android/app/src/main/assets/content-pack/month-01` to maintain binary/offline backwards compatibility with legacy `ContentLessonLoader` and fallback flows without crashing existing test suites.

## Contents
- `month-01/lessons/`: 358 legacy JSON lesson activity bundles (covering Grade 3 Filipino, English, Math, Science, GMRC, Makabansa).
- `month-01/assets/vectors/`: 358 companion SVG vector illustrations.
- `month-01/days/`: Legacy daily sequence mapping files.

## Preserved Metrics (Pre-Archive Quality Gate State)
- **Tooling Tests:** 120/120 passing across `android/tools/`.
- **Validation Gates:** 0 validation errors, 0 duplicate titles, clean distractor formatting.
