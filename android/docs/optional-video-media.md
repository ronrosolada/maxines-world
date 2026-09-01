# Optional video media

The Android app remains lesson-offline-first. `VIDEO_V1` activities reference a
stable `mediaId`; the corresponding MP4 is downloaded explicitly, verified, and
played from private app storage. Video lessons are optional media and are never
a prerequisite for the bundled curriculum.

## Current playlist replacement

The current personal-use preview catalog is documented in full at
[`docs/video-playlist-replacement-2026-08-20.md`](../../docs/video-playlist-replacement-2026-08-20.md).

- Source: `Video Lesson Sorting - Updated.xlsx`, Grade 1–4 workbook
- Scope: 237 selected videos from 464 source playlist rows
- Subjects: Filipino 100, Makabansa 51, Mathematics 24, English 22, GMRC 20, Science 20
- Grades: Grade 1 29, Grade 2 53, Grade 3 95, Grade 4 60
- Media: H.264/AAC MP4, downloaded on demand from the trusted home LAN
- Media endpoint: `https://10.10.10.33/media/catalog.json`
- Media root on DreamNAS: `/mnt/user/appdata/maxines-world-content/server/content/media/`
- App storage: `filesDir/maxines-media/`
- Catalog version: `1`
- Child-facing catalog filter: Grade 3 and `releaseStatus=RELEASED` only
  (`ChildFacingMediaPolicy`). Grade 1/2/4 and `PREVIEW` rows remain in the
  LAN catalog for later review and are not shown as core curriculum.
- Catalog status: Grade 3 rows are `RELEASED`; other grades remain `PREVIEW`
- License status: `PERSONAL_USE` (household LAN-only; not a public license)
- Assessment policy: five subject-specific multiple-choice items per video; 4/5 required; `claimsMastery=false`

The endpoint is LAN-only. Videos remain `PERSONAL_USE` household media; do
not expose this endpoint outside the home network or rewrite license metadata
to imply public-distribution rights. Use an HTTPS Caddy route (see PR #107)
before any broader distribution.

## Catalog contract

`media/catalog.json` contains individual assets rather than a giant archive:

```json
{
  "catalogVersion": 1,
  "generatedAt": "2026-08-20T00:00:00+08:00",
  "media": [
    {
      "mediaId": "yt-kr4unsat2yk",
      "title": "Grade 3 English Q1 Ep1: Picture Talk",
      "file": "media/playlists/english/g3/yt-kr4unsat2yk.mp4",
      "sha256": "<64 lowercase hex characters>",
      "sizeBytes": 123,
      "durationSeconds": 1266,
      "width": 490,
      "height": 360,
      "subjectId": "english",
      "gradeLevel": 3,
      "quarter": 1,
      "episodeNumber": 1,
      "mimeType": "video/mp4",
      "releaseStatus": "PREVIEW",
      "licenseStatus": "PERSONAL_USE"
    }
  ]
}
```

The parser rejects duplicate IDs, unsafe paths, non-MP4 files, invalid hashes,
zero sizes/durations, and unsupported MIME types. The downloader supports HTTP
range resume and promotes a `.part` file only after size and SHA-256 verification.
The catalog is cached atomically at `filesDir/maxines-media/catalog.json` and a
validated cached catalog remains usable when DreamNAS is unavailable.

The app sorts entries by subject and episode number. The workbook `Subject`
column is the canonical tag source; titles are real source titles, not generated
placeholders. Grade 1 and Grade 2 are limited to Filipino, Makabansa, and GMRC.
Grade 3 and Grade 4 also include Mathematics, Science, and English.

## Assessment contract

The tracked assessment source is:

```text
app/src/main/assets/content-pack/media-assessments.json
```

The replacement contains five items for every one of the 237 videos (1,185
items total):

```json
{
  "schemaVersion": 1,
  "assessmentPolicy": {
    "itemsPerVideo": 5,
    "passingCorrectCount": 4,
    "claimsMastery": false
  },
  "media": [
    {
      "mediaId": "yt-kr4unsat2yk",
      "questionCount": 5,
      "passingCorrectCount": 4,
      "claimsMastery": false,
      "items": [
        {
          "itemId": "yt-kr4unsat2yk-q01",
          "sequence": 1,
          "type": "MULTIPLE_CHOICE",
          "prompt": "Which sentence is complete?",
          "options": [
            {"id": "a", "text": "The bird sings."},
            {"id": "b", "text": "Because the bird"},
            {"id": "c", "text": "Singing in the"},
            {"id": "d", "text": "The blue"}
          ],
          "correctOptionIds": ["a"],
          "explanation": "The correct answer is The bird sings."
        }
      ]
    }
  ]
}
```

Assessment language follows subject:

- English for English, Mathematics, and Science
- Filipino for Filipino, Makabansa, and GMRC

The check is supplemental memory feedback. It preserves the existing watch
completion, 80% pass, reward, retry, and progress behavior without claiming
mastery of the broader subject.

## Deployment

Stage and inspect locally first. Copy only the MP4 files and catalog into the
Caddy read-only content root. Do not publish yt-dlp `.info.json` sidecars.

```bash
python3 android/tools/build_media_catalog.py \
  --staging /home/ron/maxines-media-staging \
  --assessments android/app/src/main/assets/content-pack/media-assessments.json \
  --output /home/ron/maxines-video-catalog.json

rsync -av --partial /home/ron/maxines-media-staging/ \
  root@10.10.10.5:/mnt/user/appdata/maxines-world-content/server/content/media/
rsync -av --partial /home/ron/maxines-video-catalog.json \
  root@10.10.10.5:/mnt/user/appdata/maxines-world-content/server/content/media/catalog.json
```

Always pass `--assessments`. Omitting it produces a structurally valid catalog
that silently drops the comprehension items. For a deliberately unassessed
supplementary video, use `--allow-unassessed-media` while still passing the
assessment source.

The deployed root catalog and `/media/catalog.json` must describe the same 237
assets. After deployment, verify HTTP 200, subject counts, catalog SHA-256, and
at least one `video/mp4` response. The full release procedure, APK deployment,
rollback, and current verified hashes are in
[`docs/video-playlist-replacement-2026-08-20.md`](../../docs/video-playlist-replacement-2026-08-20.md).
