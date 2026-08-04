# Filipino Q1 SLM conversion report

## Scope

- Source: `ph-matatag-g3-filipino-q1-slm-v2.zip`
- Source lessons converted and validated: 15
- Coverage: Grade 3 Filipino, Q1 weeks 1–8
- Bundled lesson count: 349 → 358
- Published new lessons: 9 (Q1 W04–W08)
- Six overlapping Q1 W01–W03 IDs were retained from the existing reviewed bundle; they were not overwritten by this import.
- Vector assets copied for the new lessons: 9

The source lessons remain review-gated. Every converted lesson has
`educatorValidated=false` and `releaseStatus=REQUIRES_EDUCATOR_REVIEW`.

## Conversion policy

1. Preserve authored source activities and assessment copy where present.
2. Reorder activities into the Android canonical order:
   `ANIMATED_EXPLANATION`, `HOTSPOT_IMAGE`, `SORT_AND_CLASSIFY`,
   `MULTIPLE_CHOICE`, `MATCHING_PAIRS`, `SEQUENCE_BUILDER`.
3. Derive only missing activity types from authored lesson data:
   - hotspot examples from authored sort/matching/assessment examples;
   - sort groups from authored correct/incorrect assessment choices;
   - matching pairs from authored assessment questions and correct answers;
   - missing sequence steps from an explicit objective-specific Filipino policy.
4. Mark all derived activity types in `contentReview.derivedActivityTypes`.
5. Replace source accessibility filler with the authored Filipino instruction and
   remove the source `✓` next-label marker.

## Derived activity inventory

| Lesson | Derived types |
|---|---|
| W01 D01 | `SEQUENCE_BUILDER` |
| W01 D02 | `HOTSPOT_IMAGE`, `MATCHING_PAIRS` |
| W02 D01 | `HOTSPOT_IMAGE` |
| W02 D02 | `SORT_AND_CLASSIFY` |
| W03 D01 | `HOTSPOT_IMAGE` |
| W03 D02 | `HOTSPOT_IMAGE`, `MATCHING_PAIRS` |
| W04 D01 | `HOTSPOT_IMAGE` |
| W04 D02 | `SORT_AND_CLASSIFY` |
| W05 D01 | `HOTSPOT_IMAGE` |
| W05 D02 | `HOTSPOT_IMAGE` |
| W06 D01 | `HOTSPOT_IMAGE` |
| W06 D02 | `HOTSPOT_IMAGE`, `SORT_AND_CLASSIFY` |
| W07 D01 | `HOTSPOT_IMAGE` |
| W07 D02 | `HOTSPOT_IMAGE` |
| W08 D01 | `HOTSPOT_IMAGE` |

## Validation evidence

- Converter unit test: passed
- Android structural validator (15-lesson source stage): 0 errors, 0 warnings
- Published nine-lesson quality audit: 0 errors, 7 conservative alignment warnings
- Full bundled validator: 358 lessons, 0 errors, 315 existing soft warnings
- Strict curation flags on published lessons: 0
- Filipino English/placeholder scan on published lessons: 0 hits
- Similarity gate at 0.85 on the source stage: 0 near-duplicate pairs, 0 clusters

The seven published-lesson alignment warnings are report-only keyword-overlap
warnings and remain visible for owner/educator review; they were not suppressed
or converted into release approval. The full-pack soft warnings are historical
assessment-type warnings in retained content.
