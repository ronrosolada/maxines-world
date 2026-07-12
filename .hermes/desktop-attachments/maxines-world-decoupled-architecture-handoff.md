# Maxine’s World: Decoupled Application and Educational Content Architecture

## 1. Instructions for the Implementing LLM

You are responsible for changing Maxine’s World so that the Android application and educational content can be developed, reviewed, released, updated, and rolled back independently.

Repository:

```text
https://github.com/ronrosolada/maxines-world
```

Do not provide only a proposal. Inspect the repository, implement the architecture, add tests and documentation, and report exact validation results.

Do not claim that a command or test passed unless you ran it successfully.

## 2. Desired Outcome

Maxine’s World must become three independently managed systems:

```text
Android Learning Runtime
        ↓ reads versioned packages
Educational Content Repository
        ↓ publishes approved releases
Read-Only Content Distribution Server
```

After implementation:

```text
Correcting a lesson
    → content release only

Adding an illustration or narration
    → content release only

Changing an assessment
    → content release only

Adding a new activity type
    → Android release followed by content release

Changing Room, rewards, or application behavior
    → Android release
```

The Android application must function as a generic, versioned, offline-first learning player. Curriculum must not be hard-coded into individual Kotlin or Compose screens.

## 3. Nonnegotiable Principles

The implementation must preserve these rules:

* Content and application versions are independent.
* Learner data stays on the Android device.
* The content server contains no learner database.
* Installed lessons remain available when the server is offline.
* Content is downloaded into staging before activation.
* Packages are verified before use.
* Activation is atomic.
* The previous valid package remains available for rollback.
* Unsupported content is rejected rather than rendered incorrectly.
* Content marked unreviewed must not appear approved.
* No package may claim DepEd approval without documented authorization.
* A lesson correction must not erase learner progress.
* Package files are immutable after publication.
* A published version number must never be reused for different bytes.
* Production builds must not disable TLS verification.
* The NAS administration interface must never be exposed as part of content hosting.

## 4. System Boundaries

### 4.1 Android Learning Runtime

Recommended repository:

```text
maxines-world
```

Responsibilities:

* Catalog synchronization
* Package downloading
* Integrity and authenticity verification
* Secure extraction
* Schema validation
* Package activation and rollback
* Generic activity rendering
* Assessment execution
* Local progress persistence
* Daily subject-credit calculation
* Wildlife badge awards
* Parent content-management controls
* Offline operation

The application must not own:

* Editable curriculum source files
* Educator review records
* Content-authoring workflows
* Production catalog generation
* Server-side learner profiles
* A lesson-specific Compose screen for every lesson

### 4.2 Educational Content Repository

Create a separate private repository:

```text
maxines-world-content
```

Responsibilities:

* Editable lesson source files
* Assessments and answer keys
* Graphics and audio sources
* Curriculum mappings
* Claim provenance
* Educator review records
* Accessibility review records
* Content validation
* Package creation
* Release signing
* Catalog generation
* Publishing approved releases

### 4.3 Infrastructure Repository

A third repository is recommended but optional:

```text
maxines-world-infra
```

Responsibilities:

* Docker Compose
* Caddy or Nginx configuration
* NAS deployment scripts
* Backup scripts
* Monitoring configuration
* Release-publishing scripts
* Operational documentation

For a small private project, infrastructure may initially live under:

```text
maxines-world-content/infra/
```

Do not mix infrastructure into Android feature modules.

## 5. Target Repository Layouts

### 5.1 Android Repository

```text
maxines-world/
├── app/
├── core/
│   ├── content-domain/
│   ├── content-data/
│   ├── content-network/
│   ├── content-storage/
│   ├── database/
│   ├── designsystem/
│   └── testing/
├── engine/
│   ├── activity/
│   ├── assessment/
│   ├── lesson/
│   ├── rewards/
│   └── sync/
├── feature/
│   ├── dailytrail/
│   ├── lessonplayer/
│   ├── fieldguide/
│   └── parentcontent/
├── starter-content/
├── docs/
└── build.gradle.kts
```

Adapt this layout to the actual module structure. Do not create unnecessary duplicate modules if equivalent modules already exist.

### 5.2 Content Repository

```text
maxines-world-content/
├── README.md
├── schemas/
│   ├── catalog.schema.json
│   ├── package.schema.json
│   ├── lesson.schema.json
│   ├── assessment.schema.json
│   ├── asset.schema.json
│   ├── provenance.schema.json
│   └── review.schema.json
├── curriculum/
│   └── grade-3/
│       ├── english/
│       ├── filipino/
│       ├── mathematics/
│       ├── science/
│       └── araling-panlipunan/
├── shared-assets/
├── badge-catalog/
├── source-records/
├── review-records/
├── migrations/
├── tools/
│   ├── validate/
│   ├── build/
│   ├── sign/
│   └── publish/
├── tests/
├── releases/
│   ├── preview/
│   └── production/
└── infra/
```

### 5.3 Lesson Source Directory

```text
curriculum/grade-3/mathematics/q1/week-01/day-01/
├── lesson.yaml
├── activities.yaml
├── assessment.yaml
├── provenance.yaml
├── review.yaml
└── assets/
    ├── place-value-board.svg
    ├── place-value-board.webp
    └── narration.mp3
```

## 6. Independent Versioning

Never use one version number for the entire system.

Maintain these independent values:

| Version | Meaning |
|---|---|
| App version | Android runtime release |
| Schema version | Package data-contract version |
| Content version | Revision of a content package |
| Catalog version | Revision of a release channel |
| Asset version | Optional shared-asset revision |
| Badge-catalog version | Wildlife collection revision |

Example:

```json
{
  "packageId": "g3-mathematics-q1-week-01",
  "contentVersion": 4,
  "schemaVersion": 1,
  "minimumAppVersionCode": 18,
  "requiredCapabilities": [
    "MULTIPLE_CHOICE_V1",
    "NUMBER_MANIPULATIVE_V1",
    "SEQUENCE_BUILDER_V1"
  ]
}
```

A content-only correction increments `contentVersion`, not `schemaVersion`.

## 7. Runtime Capability Contract

Every activity renderer must advertise a stable capability identifier.

Examples:

```text
ANIMATED_EXPLANATION_V1
MULTIPLE_CHOICE_V1
DRAG_AND_DROP_V1
SORT_AND_CLASSIFY_V1
SENTENCE_BUILDER_V1
MATCHING_PAIRS_V1
HOTSPOT_IMAGE_V1
SEQUENCE_BUILDER_V1
NUMBER_MANIPULATIVE_V1
MAP_ACTIVITY_V1
PREDICTION_OBSERVATION_V1
```

The application exposes:

```kotlin
data class RuntimeCapabilities(
    val appVersionCode: Int,
    val supportedSchemaVersions: Set<Int>,
    val activityCapabilities: Set<String>
)
```

Compatibility check:

```kotlin
fun isCompatible(
    packageInfo: RemoteContentPackage,
    runtime: RuntimeCapabilities
): Boolean {
    return packageInfo.minimumAppVersionCode <= runtime.appVersionCode &&
        packageInfo.schemaVersion in runtime.supportedSchemaVersions &&
        runtime.activityCapabilities.containsAll(
            packageInfo.requiredCapabilities
        )
}
```

Reject an incompatible package before downloading when possible.

Never render an unsupported activity as plain text merely to avoid an error.

## 8. Release Channels

Provide three independent catalogs:

```text
development.json
preview.json
production.json
```

### Development

May contain:

* Incomplete lessons
* Generated assets
* Unverified answers
* Debug packages
* Experimental activity types

Only development builds should use this channel by default.

### Preview

May contain:

* Structurally complete content
* Parent-supervised content
* Lessons awaiting final educator approval
* Release candidates

The application must display a parent-facing warning.

### Production

May contain only:

* Approved lessons
* Verified answer keys
* Complete source provenance
* Reviewed accessibility metadata
* Approved artwork and audio
* Supported runtime capabilities

The child-facing production mode must not activate a package with:

```json
{
  "educatorValidated": false
}
```

unless an explicit local parent-supervised preview setting is active.

## 9. Catalog Contract

Example production catalog:

```json
{
  "catalogSchemaVersion": 1,
  "catalogVersion": 12,
  "channel": "production",
  "generatedAt": "2026-07-13T08:00:00Z",
  "keyId": "maxines-content-production-2026-01",
  "packages": [
    {
      "packageId": "g3-mathematics-q1-week-01",
      "contentVersion": 4,
      "schemaVersion": 1,
      "minimumAppVersionCode": 18,
      "requiredCapabilities": [
        "MULTIPLE_CHOICE_V1",
        "NUMBER_MANIPULATIVE_V1"
      ],
      "educatorValidated": true,
      "releaseStatus": "APPROVED",
      "sizeBytes": 28491632,
      "sha256": "FULL_LOWERCASE_SHA256",
      "url": "https://content.example.net/packages/g3-mathematics-q1-week-01-v4.zip"
    }
  ],
  "signature": "BASE64_SIGNATURE"
}
```

The catalog signature must cover a canonical representation that excludes the signature field itself.

Document the canonicalization rules precisely.

## 10. Package Contract

Each immutable ZIP package should contain:

```text
g3-mathematics-q1-week-01-v4.zip
├── package.json
├── lessons/
│   ├── mathematics-g3-q1-w01-d01.json
│   ├── mathematics-g3-q1-w01-d02.json
│   └── ...
├── assessments/
├── assets/
│   ├── illustrations/
│   ├── icons/
│   ├── audio/
│   └── animation/
├── provenance/
├── reviews/
└── checksums.json
```

Example `package.json`:

```json
{
  "packageId": "g3-mathematics-q1-week-01",
  "contentVersion": 4,
  "schemaVersion": 1,
  "grade": 3,
  "subject": "MATHEMATICS",
  "quarter": 1,
  "week": 1,
  "minimumAppVersionCode": 18,
  "requiredCapabilities": [
    "ANIMATED_EXPLANATION_V1",
    "NUMBER_MANIPULATIVE_V1",
    "MULTIPLE_CHOICE_V1"
  ],
  "educatorValidated": true,
  "releaseStatus": "APPROVED",
  "lessonIds": [
    "mathematics-g3-q1-w01-d01",
    "mathematics-g3-q1-w01-d02"
  ]
}
```

## 11. Lesson Contract

Example editable source:

```yaml
lessonId: mathematics-g3-q1-w01-d01
contentVersion: 4
schemaVersion: 1
grade: 3
subject: MATHEMATICS
quarter: 1
week: 1
day: 1
title: Building Numbers to 10,000
objective: Build and represent four-digit numbers using place value.
estimatedMinutes: 15
educatorValidated: true
releaseStatus: APPROVED

activities:
  - activityId: mathematics-g3-q1-w01-d01-a01
    sequence: 1
    type: ANIMATED_EXPLANATION
    capability: ANIMATED_EXPLANATION_V1
    required: true
    assetId: place-value-board
    instruction: Observe how thousands, hundreds, tens, and ones form a number.

  - activityId: mathematics-g3-q1-w01-d01-a02
    sequence: 2
    type: NUMBER_MANIPULATIVE
    capability: NUMBER_MANIPULATIVE_V1
    required: true
    interactionId: build-four-digit-number

assessment:
  itemCount: 5
  passingCorrectCount: 4
  purpose: FORMATIVE_MODULE_CHECK
  claimsMastery: false

accessibility:
  narrationAvailable: true
  captionsAvailable: true
  reducedMotionSupported: true
  dragAlternativeAvailable: true
  colorIndependent: true
```

Stable IDs must not depend on display text.

## 12. Educational Review Metadata

Every lesson requires a review record:

```yaml
lessonId: mathematics-g3-q1-w01-d01

curriculum:
  framework: MATATAG
  mappingStatus: REVIEWED
  learningArea: Mathematics
  grade: 3
  quarter: 1
  competencyText: VERIFIED_TEXT
  reviewerId: reviewer-001
  reviewedAt: 2026-07-01

factualReview:
  status: APPROVED
  reviewerId: reviewer-002
  reviewedAt: 2026-07-02

assessmentReview:
  status: APPROVED
  reviewerId: reviewer-002
  reviewedAt: 2026-07-02

accessibilityReview:
  status: APPROVED
  reviewerId: reviewer-003
  reviewedAt: 2026-07-03

culturalReview:
  status: NOT_REQUIRED

educatorValidated: true
```

The production build pipeline must reject missing or incomplete required reviews.

## 13. Claim Provenance

Child-facing factual claims need traceable source records.

Example:

```yaml
lessonId: science-g3-q3-w02-d01

claims:
  - claimId: science-magnets-001
    exactClaim: Magnets attract some materials, but not all metals.
    sourceTitle: VERIFIED_SOURCE_TITLE
    sourceUrl: https://example.gov/source
    sourceType: SCIENTIFIC_INSTITUTION
    retrievedOn: 2026-06-20
    factualReviewStatus: APPROVED
    reviewerId: reviewer-002
```

Do not let a generative model invent:

* Competency codes
* Population statistics
* Local histories
* Animal facts
* Scientific rules
* Pronunciations
* Assessment answers
* Cultural practices
* Official-looking symbols

If a claim cannot be verified, omit it or mark the lesson blocked.

## 14. Content Build Pipeline

Implement a deterministic pipeline:

```text
Editable YAML and source assets
→ schema validation
→ semantic validation
→ provenance validation
→ review-gate validation
→ asset processing
→ canonical JSON generation
→ package assembly
→ package checksum generation
→ package signing
→ catalog generation
→ catalog signing
→ release publication
```

Given identical inputs and tool versions, package output should be reproducible where practical.

Required validation includes:

* Unique lesson IDs
* Unique activity IDs
* Unique assessment IDs
* Supported schema version
* Supported capability identifiers
* Valid answer references
* Existing assets
* Valid asset paths
* No path traversal
* Required content descriptions
* Audio transcript availability
* Drag alternative availability
* No color-only meaning
* Exact assessment-item count where required
* Correct passing threshold
* Complete review metadata
* Complete provenance
* No remote runtime asset dependencies
* No unapproved content in production

## 15. Content CI Workflow

The content repository should run these jobs on every pull request:

```text
lint-source
validate-schema
validate-semantics
validate-assets
validate-provenance
validate-reviews
verify-assessment-answers
build-preview-packages
run-package-tests
publish-preview-artifacts
```

A production release should require:

```text
All validation jobs pass
+ educator approval
+ factual approval
+ accessibility approval
+ protected-branch approval
+ signed release tag
```

Example GitHub Actions outline:

```yaml
name: Content Validation

on:
  pull_request:
  push:
    branches:
      - main

jobs:
  validate:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: "3.12"

      - name: Install tooling
        run: pip install -r tools/requirements.txt

      - name: Validate schemas
        run: python tools/validate/schemas.py

      - name: Validate lessons
        run: python tools/validate/lessons.py

      - name: Validate assets
        run: python tools/validate/assets.py

      - name: Validate provenance
        run: python tools/validate/provenance.py

      - name: Validate reviews
        run: python tools/validate/reviews.py --channel preview

      - name: Build preview packages
        run: python tools/build/build_packages.py --channel preview

      - name: Verify packages
        run: python tools/validate/packages.py releases/preview
```

Adapt the language and tooling to the actual project.

## 16. Publishing Rules

Use immutable filenames:

```text
g3-mathematics-q1-week-01-v1.zip
g3-mathematics-q1-week-01-v2.zip
g3-mathematics-q1-week-01-v3.zip
```

Never replace `v2.zip` with different bytes.

Publishing order:

```text
Build package
→ calculate SHA-256
→ sign metadata
→ upload package
→ verify remote package
→ update catalog
→ sign catalog
→ publish catalog last
```

Publishing the catalog last prevents clients from discovering unavailable packages.

## 17. NAS Distribution Layout

```text
/srv/maxines-world/
├── catalogs/
│   ├── development.json
│   ├── preview.json
│   └── production.json
├── packages/
│   ├── g3-mathematics-q1-week-01-v4.zip
│   └── ...
├── signatures/
├── archive/
└── health/
    └── status.json
```

The NAS service must be read-only.

Recommended Docker Compose:

```yaml
services:
  content-server:
    image: caddy:2-alpine
    container_name: maxines-world-content
    restart: unless-stopped

    ports:
      - "8088:80"

    volumes:
      - ./content:/srv:ro
      - ./config/Caddyfile:/etc/caddy/Caddyfile:ro

    read_only: true

    security_opt:
      - no-new-privileges:true

    cap_drop:
      - ALL

    tmpfs:
      - /config
      - /data
```

Recommended Caddy behavior:

* Serve static files only.
* Disable directory browsing.
* Cache immutable packages for a long duration.
* Cache catalogs for a short duration.
* Add security headers.
* Do not provide an upload endpoint.
* Do not provide a learner API.
* Do not expose NAS administration.

## 18. Private Remote Access

For family-only access, prefer:

```text
Android device
→ Tailscale or WireGuard
→ NAS content server
```

Advantages:

* No public NAS exposure
* No port forwarding
* Private DNS
* Encrypted connection
* Simpler family-only deployment

Production Android builds must still use proper certificate validation.

Do not embed NAS usernames or passwords in the APK.

## 19. Android Catalog Synchronization

Use WorkManager.

Required workflow:

```text
Fetch selected channel catalog
→ verify catalog signature
→ validate catalog schema
→ compare local package versions
→ check runtime compatibility
→ download package into staging
→ verify size
→ verify SHA-256
→ verify package signature if used
→ securely extract
→ validate package
→ atomically activate
→ retain previous package
→ clean staging
```

Required behavior:

* Use unique work.
* Prevent duplicate concurrent synchronization.
* Use network constraints.
* Support manual parent-triggered synchronization.
* Optionally support Wi-Fi-only downloads.
* Use exponential backoff.
* Treat server downtime as recoverable.
* Never block access to installed content.
* Never download an already installed valid version unnecessarily.
* Stream downloads instead of loading entire files into memory.
* Surface synchronization state to parent UI.
* Never log learner information.

## 20. Secure ZIP Extraction

Protect against ZIP traversal.

Equivalent logic:

```kotlin
val root = stagingDirectory.canonicalFile
val destination = File(root, entry.name).canonicalFile

require(
    destination.path == root.path ||
        destination.path.startsWith(root.path + File.separator)
) {
    "Unsafe ZIP entry: ${entry.name}"
}
```

Also:

* Reject absolute paths.
* Reject `../` traversal.
* Reject unsupported symbolic links.
* Limit total extracted bytes.
* Limit entry count.
* Reject duplicate conflicting entries.
* Delete failed staging directories.
* Never activate partially extracted content.

## 21. Local Storage Boundary

### Filesystem

Store:

* Package manifests
* Lesson JSON
* Images
* SVG files
* Audio
* Animations
* Provenance metadata
* Review metadata

Recommended structure:

```text
filesDir/content/
├── staging/
├── active/
│   └── package-id/
│       └── version/
├── rollback/
└── quarantine/
```

### Room

Store:

* Installed-package metadata
* Active-package pointers
* Lesson progress
* Assessment attempts
* Daily subject credits
* Earned badges
* Pending badge reveals
* Parent content settings

Do not store large lesson files or images as database blobs.

## 22. Installed Package Model

Example:

```kotlin
@Entity(
    tableName = "installed_content_packages",
    primaryKeys = ["packageId", "contentVersion"]
)
data class InstalledContentPackageEntity(
    val packageId: String,
    val contentVersion: Int,
    val schemaVersion: Int,
    val localPath: String,
    val sha256: String,
    val state: ContentPackageState,
    val educatorValidated: Boolean,
    val releaseStatus: String,
    val installedAt: Instant
)

enum class ContentPackageState {
    STAGING,
    VALIDATING,
    ACTIVE,
    SUPERSEDED,
    QUARANTINED,
    FAILED
}
```

Use explicit Room migrations. Never enable destructive migration in production.

## 23. Atomic Activation

Activation must be crash-safe.

Required process:

1. Download to staging.
2. Verify the package.
3. Extract into a temporary versioned directory.
4. Validate every required file.
5. Persist the validated package record.
6. Move or rename the directory into final storage.
7. Update the active-package pointer transactionally.
8. Mark the former package superseded.
9. Retain at least one rollback version.
10. Remove obsolete staging files.

A crash must leave either the old package or the new package active.

## 24. Lesson Resolution

The lesson player should depend on a domain repository, not package files directly.

```kotlin
interface LessonRepository {
    suspend fun getLesson(lessonId: String): Lesson

    fun observeLearningDay(
        learnerId: String,
        localDate: LocalDate
    ): Flow<LearningDay>

    fun observeProgress(
        learnerId: String,
        lessonId: String
    ): Flow<LessonProgress>

    suspend fun recordActivityResult(
        learnerId: String,
        lessonId: String,
        result: ActivityResult
    )

    suspend fun submitAssessment(
        learnerId: String,
        lessonId: String,
        responses: List<AssessmentResponse>
    ): AssessmentAttemptResult
}
```

Parse lesson files lazily or build a small index. Avoid repeatedly parsing all installed lessons.

## 25. Stable Progress Identity

Progress must use stable identifiers:

```text
learnerId
lessonId
activityId
assessmentItemId
contentVersion
```

A content update must declare how progress is handled.

Default policy:

* Same lesson ID: preserve completion.
* Same activity ID: preserve activity progress.
* Removed assessment item: preserve prior lesson completion.
* Reworded prompt with same educational meaning: preserve completion.
* Material objective change: publish a new lesson ID.
* Split lesson: provide an explicit migration map.

## 26. Content Migration Map

Example:

```json
{
  "migrationVersion": 1,
  "mappings": [
    {
      "oldLessonId": "math-g3-old-001",
      "newLessonId": "mathematics-g3-q1-w01-d01",
      "progressPolicy": "PRESERVE_COMPLETION"
    },
    {
      "oldLessonId": "science-g3-old-017",
      "newLessonId": "science-g3-q3-w02-d01",
      "progressPolicy": "REQUIRE_REASSESSMENT"
    }
  ]
}
```

Do not infer migration behavior from similar titles.

## 27. Generic Activity Engine

The application must use reusable renderers.

Example:

```kotlin
@Composable
fun ActivityRenderer(
    activity: LearningActivity,
    state: ActivityUiState,
    onAction: (ActivityAction) -> Unit
) {
    when (activity.capability) {
        "ANIMATED_EXPLANATION_V1" ->
            AnimatedExplanationActivity(activity, state, onAction)

        "MULTIPLE_CHOICE_V1" ->
            MultipleChoiceActivity(activity, state, onAction)

        "DRAG_AND_DROP_V1" ->
            DragAndDropActivity(activity, state, onAction)

        "SORT_AND_CLASSIFY_V1" ->
            SortAndClassifyActivity(activity, state, onAction)

        "SENTENCE_BUILDER_V1" ->
            SentenceBuilderActivity(activity, state, onAction)

        "MATCHING_PAIRS_V1" ->
            MatchingPairsActivity(activity, state, onAction)

        "HOTSPOT_IMAGE_V1" ->
            HotspotImageActivity(activity, state, onAction)

        "SEQUENCE_BUILDER_V1" ->
            SequenceBuilderActivity(activity, state, onAction)

        "NUMBER_MANIPULATIVE_V1" ->
            NumberManipulativeActivity(activity, state, onAction)

        "MAP_ACTIVITY_V1" ->
            MapActivity(activity, state, onAction)

        "PREDICTION_OBSERVATION_V1" ->
            PredictionObservationActivity(activity, state, onAction)

        else ->
            UnsupportedActivityError(activity.capability)
    }
}
```

Do not create one Compose screen per lesson.

## 28. Starter Content

The APK should contain only a small fallback package.

Options:

* One introductory lesson per subject
* One complete learning week
* A parent-selected imported package

The starter package should demonstrate supported activity types and permit first launch without a NAS connection.

The complete curriculum should remain external.

## 29. Parent Content Management

Create a parent-only content-management screen.

Display:

* Selected release channel
* Content server URL
* Catalog version
* Last successful synchronization
* Installed packages
* Active versions
* Package sizes
* Review status
* Compatibility status
* Download progress
* Errors
* Rollback controls
* Storage usage

Actions:

* Test server connection
* Synchronize now
* Enable Wi-Fi-only downloads
* Select preview or production channel
* Import a local ZIP
* Remove an inactive package
* Roll back to the previous valid package

Preview-channel activation must require a clear parent confirmation.

## 30. Educational Status Enforcement

Content metadata must remain authoritative.

The application must not modify:

```json
{
  "educatorValidated": false,
  "releaseStatus": "REQUIRES_EDUCATOR_REVIEW"
}
```

Production child mode must refuse unvalidated content.

Parent-supervised preview may allow it only with a local setting and warning.

Required warning:

```text
This lesson package has not completed educator review. It is available
for supervised preview only and must not be represented as DepEd-approved.
```

## 31. Reward Mechanics Remain Application-Owned

Content may declare which lessons qualify, but the Android runtime owns reward enforcement.

The learner earns one badge only after:

```text
One qualifying English lesson passed
+ one qualifying Filipino lesson passed
+ one qualifying Mathematics lesson passed
+ one qualifying Science lesson passed
+ one qualifying Araling Panlipunan lesson passed
+ same learner
+ same local date
= one badge
```

Rules:

* Passing score is at least four of five.
* All required activities must be completed.
* Maximum one credit per subject per date.
* Maximum one badge per learner per date.
* Replays do not add credits.
* Several lessons from one subject do not replace another subject.
* Partial progress does not cross local midnight.
* Awarding is transactional and idempotent.
* Earned badges are not revoked after content corrections.

Do not let content JSON directly insert a badge award.

## 32. Wildlife Field Guide

The Android application owns the Field Guide interface and earned state.

The badge catalog may be separately versioned content containing:

* Badge ID
* Collection
* Position
* Animal name
* Badge title
* Artwork
* Locked silhouette
* Child-friendly fact
* Narration
* Provenance

All 50 positions must remain visible.

Unearned positions show locked silhouettes. Earned positions reveal full artwork and facts.

Badge order must be deterministic.

## 33. App CI

The Android repository should validate:

```text
Compilation
Unit tests
Room migration tests
Content parser tests
Package validator tests
ZIP-security tests
Checksum tests
Signature tests
Activity-renderer tests
Reward transaction tests
Compose accessibility tests
Lint and static analysis
```

Include small fixture packages in test resources:

```text
valid-package.zip
bad-checksum.zip
zip-traversal.zip
unsupported-schema.zip
missing-asset.zip
unsupported-capability.zip
unreviewed-package.zip
migration-package.zip
```

## 34. Required Tests

### Catalog tests

* Valid signed catalog
* Invalid signature
* Unknown signing key
* Unsupported catalog schema
* Missing package field
* Incompatible minimum app version
* Missing runtime capability
* Preview content in production mode

### Package tests

* Correct checksum
* Incorrect checksum
* Wrong size
* Interrupted download
* Malformed ZIP
* Path traversal
* Excessive extracted size
* Missing package manifest
* Missing lesson
* Duplicate lesson ID
* Missing asset
* Invalid answer key
* Unsupported capability
* Atomic activation
* Rollback after failure

### Progress tests

* Progress survives restart.
* Content update preserves stable lesson progress.
* A new lesson ID does not inherit progress accidentally.
* Explicit migration preserves completion.
* Explicit reassessment policy requires reassessment.

### Reward tests

* One passed subject produces `1/5`.
* Five distinct subjects award one badge.
* Five lessons in one subject award no badge.
* A replay creates no duplicate credit.
* Concurrent callbacks create one award.
* Midnight separates daily progress.
* Timezone changes do not duplicate an existing award.
* Badge reveal survives a restart.

### Offline tests

* First launch works with starter content.
* Installed lessons work without the NAS.
* Server failure does not deactivate packages.
* Failed update retains the active version.
* Rollback works without a network connection.

## 35. Security Requirements

* Verify catalog signatures.
* Verify package checksums.
* Use HTTPS in production.
* Never disable certificate validation.
* Prevent ZIP traversal.
* Treat package content as untrusted input.
* Validate local asset paths.
* Do not execute package code.
* Do not load arbitrary remote web content.
* Do not include NAS credentials in the APK.
* Keep content hosting read-only.
* Keep learner data device-local.
* Avoid logging learner answers or personal information.
* Rotate signing keys through an explicit key-transition process.

Store the production signing private key outside the NAS web root and outside Git.

The application should contain only trusted public keys.

## 36. Backup Strategy

Use a 3-2-1-style approach for:

* Editable lesson sources
* Original artwork and narration
* Review records
* Generated packages
* Catalogs
* Deployment configuration
* Signing keys
* Release logs

Recommended:

```text
Primary: Git and NAS working storage
Secondary: NAS snapshots
Offsite: encrypted external or cloud backup
```

Signing keys need a separate encrypted offline backup.

Learner data remains device-local unless a future privacy-reviewed backup design is approved.

## 37. Monitoring

The static server needs only lightweight monitoring:

* HTTPS availability
* Catalog reachability
* Package reachability
* Disk capacity
* Certificate expiration
* NAS health
* Backup success

Do not add invasive learner analytics.

The parent screen may retain local operational information:

* Last synchronization
* Last successful catalog version
* Last error category
* Active package versions

## 38. Migration Plan from Current Architecture

Implement in stages.

### Phase 1: Freeze Contracts

1. Inventory existing lesson models.
2. Inventory all activity types.
3. Freeze `schemaVersion: 1`.
4. Assign stable capability identifiers.
5. Document compatibility rules.
6. Add parser and validator fixtures.

### Phase 2: Extract Content

1. Move full curriculum sources out of the Android repository.
2. Preserve only a starter package in APK assets.
3. Create the content repository.
4. Add schemas and validation scripts.
5. Add review and provenance records.
6. Build the first external package.

### Phase 3: Add Android Package Management

1. Add catalog models.
2. Add package metadata persistence.
3. Add secure downloader.
4. Add checksum and signature verification.
5. Add safe extraction.
6. Add package validation.
7. Add atomic activation.
8. Add rollback.
9. Add WorkManager synchronization.

### Phase 4: Decouple the Lesson Player

1. Introduce `LessonRepository`.
2. Resolve lessons from the active package.
3. Remove lesson-specific screens.
4. Route activities through generic renderers.
5. Preserve progress using stable IDs.
6. Add controlled unsupported-content errors.

### Phase 5: Parent Controls

1. Add server URL configuration.
2. Add channel selection.
3. Add connection testing.
4. Add package-management UI.
5. Add preview warnings.
6. Add rollback controls.
7. Add storage reporting.

### Phase 6: Content CI and NAS

1. Add content validation.
2. Add deterministic package builds.
3. Add signing.
4. Add catalog generation.
5. Deploy static hosting.
6. Add publishing scripts.
7. Add backups and monitoring.

### Phase 7: Remove Legacy Coupling

1. Remove duplicated curriculum Kotlin code.
2. Remove obsolete asset copies.
3. Remove fake production content.
4. Verify starter-content fallback.
5. Test content releases without rebuilding the APK.
6. Document the complete release process.

## 39. Implementation Sequence for the Other LLM

Follow this order:

1. Inspect the current repository.
2. Record baseline build and test results.
3. Produce an inventory of content-related modules and models.
4. Identify current coupling between lessons and UI.
5. Propose only necessary module changes.
6. Freeze schema version 1.
7. Add runtime capability declarations.
8. Add catalog and package models.
9. Add installed-package Room entities and migrations.
10. Implement catalog signature verification.
11. Implement package download and checksum verification.
12. Implement secure extraction.
13. Implement package validation.
14. Implement atomic activation and rollback.
15. Add `LessonRepository`.
16. Refactor lesson rendering to use generic activity renderers.
17. Add parent content-management UI.
18. Add preview and production channel enforcement.
19. Extract full curriculum sources to the content repository.
20. Add content schemas and validation.
21. Add package and catalog build scripts.
22. Add NAS configuration.
23. Add tests.
24. Run builds and tests.
25. Perform manual offline and rollback testing.
26. Update documentation.
27. Produce a final implementation report.

Keep the repository buildable after every phase.

## 40. Documentation to Create

In the Android repository:

```text
docs/content-runtime-architecture.md
docs/content-package-schema.md
docs/content-sync-and-rollback.md
docs/runtime-capabilities.md
docs/reward-badge-mechanics.md
docs/parent-content-management.md
```

In the content repository:

```text
docs/authoring-guide.md
docs/curriculum-mapping.md
docs/provenance-and-review.md
docs/content-validation.md
docs/release-process.md
docs/package-versioning.md
```

In the infrastructure repository:

```text
docs/nas-deployment.md
docs/security.md
docs/backup-and-recovery.md
docs/key-management.md
docs/monitoring.md
```

## 41. Definition of Done

The architecture change is complete only when:

* The Android application builds.
* Existing functionality remains operational.
* Full curriculum content is no longer hard-coded into the APK.
* A starter package supports first launch.
* A content-only correction can be published without rebuilding the APK.
* The Android app can fetch a catalog.
* Catalog authenticity is verified.
* Package checksums are verified.
* ZIP extraction is secure.
* Package schemas and assets are validated.
* Activation is atomic.
* The previous valid package can be restored.
* Installed lessons remain usable offline.
* Unsupported capabilities are rejected.
* Stable IDs preserve progress appropriately.
* Preview content is clearly restricted.
* Production mode rejects unapproved content.
* Learner data is never sent to the NAS.
* The NAS server is read-only.
* Content CI produces immutable packages.
* Catalogs are published last.
* App and content versions are independent.
* Automated tests pass.
* Manual offline and rollback tests pass.
* Documentation is complete.

## 42. Required Final Report from the Implementing LLM

Report these sections:

### Summary

Describe completed work in no more than ten bullets.

### Repository Changes

Group changed files by:

* Android domain
* Android data
* Networking
* Database
* Content engine
* UI
* Rewards
* Content repository
* Infrastructure
* Tests
* Documentation

### Architecture Decisions

Explain deviations required by the actual repository.

### Version Contracts

List:

* App version
* Supported schema versions
* Runtime capabilities
* Content package versions
* Catalog versions
* Trusted signing-key IDs

### Validation Results

Use only:

```text
PASS
FAIL
NOT RUN
```

Report:

* Android build
* Unit tests
* Lint
* Room migration tests
* Instrumentation tests
* Content schema validation
* Content semantic validation
* Package verification
* Signature verification
* Docker Compose validation
* Offline smoke test
* Rollback smoke test

Never imply `NOT RUN` means success.

### Known Limitations

List:

* Missing renderers
* Unreviewed lessons
* Placeholder artwork
* Migration risks
* Device-specific issues
* Test gaps
* NAS limitations

### Educational Status

State exactly:

```text
Structural implementation does not establish curriculum approval,
factual certification, educator validation, or DepEd endorsement.
Only packages that complete the documented review gates may be
published through the production catalog.
```

### Deployment Instructions

Provide exact commands for:

* Building content
* Validating content
* Signing packages
* Publishing to the NAS
* Starting the static server
* Configuring the Android app
* Testing synchronization
* Performing rollback

### Git Status

Provide:

* Repository
* Branch
* Commit hash
* Commit message
* Working-tree status

If commits cannot be created, provide complete patches and explain why.

## 43. Failure Handling

If a requirement cannot be completed:

1. Do not omit it silently.
2. Do not leave a mock active in production.
3. Implement everything that can be completed safely.
4. Mark the blocked requirement clearly.
5. Explain the reason.
6. State the smallest next action required.
7. Keep all repositories buildable.
8. Do not weaken integrity, privacy, educational-review, or reward safeguards to bypass the problem.

## 44. Final Architectural Rule

The Android application is a stable learning runtime.

Educational material is independently authored, reviewed, versioned, signed, published, downloaded, validated, and activated as immutable content packages.

The NAS is a read-only distribution endpoint.

Learner progress and rewards remain local application data.

No content update should require an APK release unless it introduces a schema or runtime capability the installed application does not support.
