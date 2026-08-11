# Optional video media

The Android app remains lesson-offline-first. `VIDEO_V1` activities reference a
stable `mediaId`; the corresponding MP4 is downloaded explicitly, verified, and
played from private app storage.

## Current personal-use pilot

- Source playlists:
  - `Kids Tagalog Lessons` — `Tagalog Time with Pat` (media 01–18)
  - `Full-Length Tagalog Lessons` — `Tagalog Time with Pat` (media 19–26)
- Requested quality: best H.264/AAC format up to 480p (the new playlist's last four videos are source-limited to 640×360)
- Media endpoint: `http://10.10.10.33/media/catalog.json`
- Media root on DreamNAS: `/mnt/user/appdata/maxines-world-content/server/content/media/`
- App storage: `filesDir/maxines-media/`
- Catalog status: `PREVIEW`
- License status: `PERSONAL_USE`

The endpoint is LAN-only and currently uses HTTP. The Android network security
configuration permits cleartext traffic only to `10.10.10.33`; do not expose
this endpoint outside the home network. Use an HTTPS Caddy route before any
broader distribution.

After a successful refresh, the app stores the raw catalog atomically at
`filesDir/maxines-media/catalog.json`. A later refresh falls back to that
validated catalog when DreamNAS is unavailable, so already-downloaded videos
remain discoverable after navigation or an app restart. If no catalog has ever
been cached, the screen shows a retry action instead of pretending that the
library is empty.

## Catalog contract

`media/catalog.json` is version 1 and contains individual assets, not a giant
archive:

```json
{
  "catalogVersion": 1,
  "generatedAt": "2026-08-09T00:00:00+08:00",
  "media": [
    {
      "mediaId": "kids-tagalog-01-introductions",
      "title": "Kids Tagalog Lesson Ep.1",
      "file": "media/kids-tagalog/01-O6mA_5-JPaw.mp4",
      "sha256": "<64 lowercase hex characters>",
      "sizeBytes": 123,
      "durationSeconds": 600,
      "width": 854,
      "height": 480,
      "mimeType": "video/mp4",
      "releaseStatus": "PREVIEW",
      "licenseStatus": "PERSONAL_USE"
    }
  ]
}
```

The parser rejects duplicate IDs, unsafe paths, non-MP4 files, invalid hashes,
zero sizes/durations, and unsupported MIME types. The downloader supports
HTTP range resume and promotes a `.part` file only after size and SHA-256
verification.

## Optional comprehension checks

Each media asset may include an `assessment` block. The current personal-use
pack uses ten `MULTIPLE_CHOICE` items per video and an 8/10 formative pass mark.
`claimsMastery` remains `false`: this checks whether Maxine remembers the video,
not whether she has mastered the broader language skill.

Published assessment items contain only child-facing fields:

```json
{
  "questionCount": 10,
  "passingCorrectCount": 8,
  "claimsMastery": false,
  "items": [
    {
      "itemId": "kids-tagalog-01-introductions-q01",
      "sequence": 1,
      "type": "MULTIPLE_CHOICE",
      "prompt": "Which Tagalog word means nose?",
      "options": [
        {"id": "a", "text": "ilong"},
        {"id": "b", "text": "mata"},
        {"id": "c", "text": "tenga"},
        {"id": "d", "text": "baba"}
      ],
      "correctOptionIds": ["a"],
      "explanation": "Ilong means nose."
    }
  ]
}
```

Authoring evidence such as transcript timestamps and source quotes stays in the
local review draft and is stripped by `build_media_catalog.py` before publish.
The assessment is supplemental and never blocks a lesson or video download. Once
a video is downloaded, `VideoLibraryScreen` shows a child-facing `What do you
remember?` check with one-question-at-a-time feedback, explanations, retry, and
a final score. It does not award lesson rewards or claim mastery.

## Deployment rule

Stage and inspect the files locally first. Copy only the MP4 files and the
catalog to the Caddy read-only content root. Do not publish yt-dlp `.info.json`
sidecars; they are source metadata, not app content.

```bash
python3 android/tools/build_media_catalog.py \
  --staging /home/ron/maxines-media-staging \
  --assessments android/app/src/main/assets/content-pack/media-assessments.json \
  --output /home/ron/maxines-media-catalog.json
rsync -av --partial /home/ron/maxines-media-staging/*.mp4 \
  root@10.10.10.5:/mnt/user/appdata/maxines-world-content/server/content/media/kids-tagalog/
rsync -av --partial /home/ron/maxines-media-catalog.json \
  root@10.10.10.5:/mnt/user/appdata/maxines-world-content/server/content/media/catalog.json
```

`app/src/main/assets/content-pack/media-assessments.json` is the tracked
assessment source of truth. The current 26-video pilot has ten memory-check
items per video. Always pass this file to `build_media_catalog.py`; omitting
`--assessments` produces a structurally valid catalog that silently drops all
existing comprehension items. For an intentionally unassessed supplementary
video, use `--allow-unassessed-media` while still passing the assessment source.

Verify after deployment with a bounded header request and a SHA-256 comparison
against the local catalog. The app must be able to skip the video when the
server is unavailable; the video is never a lesson-completion prerequisite.
