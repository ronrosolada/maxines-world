# Maxine's World 0.26.0

## Learning and sanctuary progression

- Added idempotent activity paw-print rewards for completed learning activities.
- Every first lesson completion grants base stars and sanctuary tokens, with accuracy mastery bonuses that never remove the base reward.
- Daily Quest completion now means 3/3 targets and grants one sanctuary piece plus one five-minute reward break.
- Mini-game paw tokens and collectibles are persisted exactly once.
- Added Milo's Wildlife Sanctuary progress to the child Playroom.
- Replaced reward-inflating Treat Shop perks with cosmetic-only sanctuary decorations.
- Added child-facing Daily Quest reward previews and lesson reward summaries.

## Optional Tagalog media

- Added an optional, resumable, SHA-256-verified video library for 18 Tagalog learning videos.
- Video files remain outside the APK and are downloaded only when requested from the configured LAN media catalog.
- The current catalog is `PREVIEW` / `PERSONAL_USE` content served by the family's private media endpoint.
- Lessons remain usable and completable when the media endpoint is unavailable.
- Video downloads are stored in private app storage and are never required for lesson completion.

## Verification

- 358 educator-reviewed lessons pass the release gate.
- 29 bundled mini-games pass the offline/CSP gate.
- JVM and Android instrumentation suites pass.
- Release APK: version code 27, version name 0.26.0.
