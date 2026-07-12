# Content Package Schema v1

Frozen 2026-07-13. All content packages must conform to this schema.

## Package Structure

```
{packageId}-v{contentVersion}.zip
├── package.json          # Package metadata
├── lessons/              # One JSON per lesson
├── days/                 # Daily manifest JSON files
├── assets/vectors/       # SVG learning assets
└── validation-report.json
```

## package.json

```json
{
  "packageId": "maxines-world-g3-month-01",
  "contentVersion": 1,
  "schemaVersion": 1,
  "grade": 3,
  "month": 1,
  "minimumAppVersionCode": 9,
  "requiredCapabilities": ["ANIMATED_EXPLANATION_V1", "MULTIPLE_CHOICE_V1"],
  "educatorValidated": false,
  "releaseStatus": "REQUIRES_EDUCATOR_REVIEW",
  "lessonIds": ["english-g3-m01-d01", ...]
}
```

## Lesson JSON

```json
{
  "lessonId": "english-g3-m01-d01",
  "schemaVersion": 1,
  "grade": 3,
  "month": 1,
  "day": 1,
  "subject": "ENGLISH",
  "title": "Picture Detective",
  "objective": "...",
  "estimatedMinutes": 12,
  "educatorValidated": false,
  "releaseStatus": "REQUIRES_EDUCATOR_REVIEW",
  "qualifiesForDailyBadge": true,
  "language": "en-PH",
  "introduction": "...",
  "vocabulary": [{"term": "...", "definition": "..."}],
  "activities": [
    {
      "activityId": "english-g3-m01-d01-a01",
      "sequence": 1,
      "type": "ANIMATED_EXPLANATION",
      "instruction": "...",
      "required": true,
      "completionRule": {"type": "VIEW_AND_ACKNOWLEDGE"},
      "assetId": "english-g3-m01-d01-visual"
    }
  ],
  "assessment": {
    "purpose": "FORMATIVE_MODULE_CHECK",
    "itemCount": 5,
    "passingCorrectCount": 4,
    "claimsMastery": false,
    "items": [...]
  }
}
```

## Day Manifest

```json
{
  "dayId": "g3-m01-d01",
  "sequence": 1,
  "badgePosition": 1,
  "qualifyingLessonIds": [
    "english-g3-m01-d01",
    "filipino-g3-m01-d01",
    "mathematics-g3-m01-d01",
    "science-g3-m01-d01",
    "araling-panlipunan-g3-m01-d01"
  ],
  "requiredPassedSubjects": 5,
  "passingCorrectCountPerLesson": 4,
  "sameLocalDateRequired": true
}
```

## Activity Types

| Type | Capability | Completion Rule |
|---|---|---|
| ANIMATED_EXPLANATION | ANIMATED_EXPLANATION_V1 | VIEW_AND_ACKNOWLEDGE |
| MULTIPLE_CHOICE | MULTIPLE_CHOICE_V1 | CORRECT_RESPONSE |
| HOTSPOT_IMAGE | HOTSPOT_IMAGE_V1 | ALL_TARGETS_VISITED |
| MATCHING_PAIRS | MATCHING_PAIRS_V1 | ALL_PAIRS_MATCHED |
| SORT_AND_CLASSIFY | SORT_AND_CLASSIFY_V1 | ALL_ITEMS_SORTED |
| SEQUENCE_BUILDER | SEQUENCE_BUILDER_V1 | ALL_STEPS_COMPLETED |
| INTERACTIVE_SPEC | INTERACTIVE_SPEC_V1 | VIEW_AND_ACKNOWLEDGE |

## Validation Rules

- All lesson IDs must be unique and stable
- All activity IDs must be unique within a lesson
- All assessment items must reference valid correct answers
- All assetId references must resolve to existing files
- No remote URLs in asset references
- `educatorValidated` must not be changed by the app
- `releaseStatus` must not be upgraded by the app
