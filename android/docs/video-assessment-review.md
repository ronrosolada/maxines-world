# Video-Lesson Post-Watch Assessment Quality Review

Scope: the 237 video assessments that a child sees after watching a lesson. The
authoritative runtime source is `server/content/catalog.json`
(`media[].assessment.items[]`); it is mirrored to
`android/app/src/main/assets/content-pack/media-assessments.json` and the
checkpoint manifest `video-checkpoints.json`.

Each assessment has exactly 5 `MULTIPLE_CHOICE` items (`{mediaId}-q01..q05`), four
options (`a`–`d`), one `correctOptionIds`, an `explanation`, `questionCount=5`,
`passingCorrectCount=4`, `claimsMastery=false`. Corpus totals: 237 videos /
1,185 items. Subjects: filipino 100, makabansa 51, mathematics 24, english 22,
gmrc 20, science 20. Grades 1–4 (G3 = 95). Release: 95 RELEASED (all G3), 142
PREVIEW.

## 1. Methodology

Assessed programmatically (scripts run against the catalog) and by reading every
flagged video end-to-end:

- Global/within-video **duplicate normalized prompts** (`review_assessment_quality.py`
  normalization: lowercase, strip non-alphanumerics).
- **Templated / generic prompts**: `"{topic}"` fill-ins, `"Which choice shows the
  skill…"`, the Filipino template `"Ano ang payak na paliwanag sa …"`, and
  cross-video prompt *skeletons* (quotes/numbers masked) shared by ≥4 videos.
- **Weak explanations**: explanations that merely restate the correct option
  (after removing the answer text, ≤3 words of teaching remain) or are too short
  to teach (<25 chars), plus the gate's banned template phrases (`"the correct
  answer is"`, `"matches the concept/skill"`, `"apply the rule or calculation
  shown"`, `"tumutugon sa konseptong sinusukat"`).
- **Weak/duplicate distractors**: duplicate option texts, and single-defensible
  answers for parity stems.

## 2. Findings

### 2.1 The mirror was stale; the catalog was already clean of duplicates

The child-facing catalog had **0 duplicate normalized prompts**, but the tracked
mirror `media-assessments.json` was regenerated from an older catalog and carried
the legacy problems the educator gate rejects:

- **44 duplicate normalized prompts** across **26 mediaIds** (the failing gate:
  `video: 44 duplicate normalized prompts`).
- **100** Filipino prompts still using the `"…payak na paliwanag sa {topic}…"`
  template (0 remain in the catalog).
- **8** G3 mathematics videos whose mirror `q01` and `q03` prompts were identical
  (0 remain in the catalog).

Every one of the 237 videos differed between catalog and mirror, confirming the
mirror had never been re-synced after the catalog was rewritten to be
video-grounded. Example (`yt-kr4unsat2yk`, "Picture Talk"): the catalog `q01` is
`"Picture Talk: A picture shows a boy beside a yellow kite. Which sentence
describes a visible detail?"` while the stale mirror `q01` was the generic
`"[English Q1 Ep1: Picture Talk] Which sentence is complete?"`.

### 2.2 Remaining catalog weakness: explanations that restate the answer

The prompts, options, and answers in the catalog are distinct and grounded, but
**110 explanations across 59 videos** taught little beyond repeating the answer.
Two patterns dominated:

- **Restated fact** (mostly Filipino/makabansa/gmrc): e.g. `yt-2j45-2yk3f4-q01`
  answer "Upang malaman ang lokasyon at direksiyon" → explanation "Ginagamit ang
  mapa upang malaman ang lokasyon at direksiyon." — the answer echoed as the
  reason.
- **Bare computation / one-liner** (mathematics/science): e.g.
  `yt--iuhay5qbnk-q01` "What is 72 − 38?" → "72 - 38 = 34."; `yt-ikuyv1-pfnq-q03`
  "Which example is a gas?" → "Air is a gas." Neither explains the method or the
  *why*.

Breakdown of the 110 affected items (59 videos):

| Subject | Grade | Release | Videos | Items |
|---|---|---|---|---|
| filipino | G1 | PREVIEW | 3 | 4 |
| filipino | G2 | PREVIEW | 6 | 7 |
| filipino | G3 | RELEASED | 5 | 5 |
| filipino | G4 | PREVIEW | 5 | 5 |
| gmrc | G4 | PREVIEW | 1 | 1 |
| makabansa | G1 | PREVIEW | 1 | 1 |
| makabansa | G2 | PREVIEW | 2 | 2 |
| makabansa | G3 | RELEASED | 3 | 5 |
| makabansa | G4 | PREVIEW | 1 | 1 |
| mathematics | G3 | RELEASED | 15 | 51 |
| mathematics | G4 | PREVIEW | 4 | 11 |
| science | G3 | RELEASED | 8 | 9 |
| science | G4 | PREVIEW | 5 | 8 |

By release status: **RELEASED — 31 videos / 70 items**; **PREVIEW — 28 videos /
40 items**. The heaviest concentration is the G3 mathematics quarter
(computation and estimation episodes), which is also RELEASED and therefore
drives daily quests.

### 2.3 Checked and found acceptable (no change made)

- **Templated prompts in the catalog**: none. No `"{topic}"` fill-ins, no `"Which
  choice shows the skill…"`, no `"payak na paliwanag"`, and no prompt skeleton
  shared by ≥4 videos.
- **Within-video duplicate prompts**: none.
- **Duplicate option texts**: the 27 items flagged by a naive
  case/punctuation-stripping check are legitimate capitalization/punctuation and
  end-mark lessons (e.g. options `?`/`.`/`,`/`!`, or `Ana`/`ana`/`aNa`/`ANa`)
  where the distinction *is* the skill; left unchanged.
- **Math prompt prefixes** (e.g. "In the Grade 3 Q1 Episode 13 … video, …") are
  verbose but correctly ground each item in its episode and stay distinct across
  the five items; left unchanged to preserve the 0-duplicate property.

## 3. Authoring: what was re-authored

For each of the 110 weak items the `explanation` field was rewritten to teach the
**method or reason**, while `itemId`, `sequence`, `type`, `prompt`, `options`
(ids `a`–`d`), and `correctOptionIds` were left byte-for-byte unchanged
(`git diff` shows only 110 changed lines, all `"explanation"`). Language policy
was honored: English for english/mathematics/science, Filipino for
filipino/makabansa/gmrc. No banned template phrases were introduced.

Representative before → after:

- `yt--iuhay5qbnk-q01` (G3 math, regrouping): "72 - 38 = 34." →
  "Regroup because 2 ones minus 8 isn't possible: rename 72 as 6 tens and 12
  ones, then 12−8=4 and 6−3=3, so 34."
- `yt-wjzhykioolk-q01` (G3 math, estimation): "700 - 300 = 400." →
  "Round each number to the nearest hundred: 684→700 and 251→300, then
  700−300=400."
- `yt-jmw-rfq3ubi-q01` (G3 math, mental addition): "49 + 8 = 57." →
  "Make a ten to add mentally: 49+1=50, then add the leftover 7 to get 57."
- `yt-ikuyv1-pfnq-q03` (G3 science): "Air is a gas." →
  "Air is a gas because it has no shape of its own and spreads out to fill any
  container."
- `yt-2j45-2yk3f4-q01` (G3 makabansa): "Ginagamit ang mapa upang malaman ang
  lokasyon at direksiyon." → "Ginagamit ang mapa upang matukoy ang lokasyon at
  direksiyon ng mga lugar, hindi para magluto o sumukat ng temperatura."
- `yt-bti10zl-hj0-q02` (G3 filipino, idioms): "Tumutukoy ito sa madaling maubusan
  ng pera." → "Ang “butas ang bulsa” ay idyoma—hindi literal—na nangangahulugang
  madaling maubusan ng pera dahil hindi ito naiipon."

Priority order followed: **RELEASED Grade-3 first** (31 videos / 70 items,
including the entire G3 mathematics computation/estimation cluster), then the
PREVIEW items (28 videos / 40 items). The legacy Filipino `"payak na paliwanag"`
template cluster and the 8 G3-math `q01==q03` pairs were resolved for the child by
re-syncing the mirror from the already-clean catalog.

### 3.1 Re-authored RELEASED Grade-3 videos (31)

`yt--iuhay5qbnk`, `yt--nnwfx1ka0a`, `yt-0g-y7cn9ypo`, `yt-2hvaoqfc5f0`,
`yt-2j45-2yk3f4`, `yt-4wnbgj6rjni`, `yt-6-3vwcn0-a8`, `yt-7uisqzk2j-i`,
`yt-bk7sb-ynab4`, `yt-bmz7fbkvsro`, `yt-bti10zl-hj0`, `yt-cq9oul3gulu`,
`yt-dtvumnzks6q`, `yt-e9hrbk-edlw`, `yt-gnxhmm2bzsw`, `yt-gpt9rgikq2q`,
`yt-ikuyv1-pfnq`, `yt-jmw-rfq3ubi`, `yt-jxsaexdrbfc`, `yt-lfamu9bx400`,
`yt-lu9kmwqfltq`, `yt-mhjqyako1aq`, `yt-mlrs9pxo-9q`, `yt-nej8m3vizbq`,
`yt-njrpbheus4g`, `yt-nzkcfhvdqpo`, `yt-ppbvvwfyrrm`, `yt-qxb4vcsvitw`,
`yt-t0-tem8vbxo`, `yt-wjzhykioolk`, `yt-xzd4pw2-is4`.

### 3.2 Re-authored PREVIEW videos (28)

`yt-2fowosdx1bc`, `yt-2qy3dtj994q`, `yt-7tdgxbctauk`, `yt-aqodbyx4bhq`,
`yt-cta80augzb4`, `yt-czz83tu9q5c`, `yt-dbqyy-uviti`, `yt-esfg5oyp4aq`,
`yt-ftqcnuyf3c8`, `yt-gynynk-g27q`, `yt-h-4iphr5ihy`, `yt-h8kxujwqxek`,
`yt-hzex9zlk3u4`, `yt-i14trdvtbro`, `yt-iokeoylwto0`, `yt-ksvpbvkolma`,
`yt-pmzskargp1y`, `yt-qvjtu6wwixq`, `yt-ssld0tckot4`, `yt-syxeghdzm5c`,
`yt-t4ilm219oru`, `yt-uhuizq5lmkc`, `yt-v5dk94psyt0`, `yt-vntljsv2buy`,
`yt-wu2jgwdmd9g`, `yt-xnvffeof5eu`, `yt-yvdutve3ofy`, `yt-yvf6cyuporo`.

## 4. Sync + validation

After editing the catalog the mirror was regenerated with
`python3 android/tools/enrich_media_explanations.py` (catalog →
`media-assessments.json`). No `mediaId` was added or removed, so the three
manifests keep identical `mediaId` sets and `video-checkpoints.json` needed no
change. Counts are unchanged: 237 videos, 5 items each, 1,185 items;
`passingCorrectCount=4`, `questionCount=5`.

Final gate results (all passing):

```
=== 1 parity ===
Catalog parity validated successfully across 3 manifests (237 media entries).
=== 2 uniqueness ===
Media assessment uniqueness audit passed: 1185 items, 0 duplicate prompt groups
=== 3 checkpoints ===
Video checkpoints valid: 237 media, 711 checkpoints
=== 4 quality ===
Assessment educator quality gate passed: 18 arena packs/180 items and 237 videos/1,185 items
=== enrich --check ===
Verified .../content-pack/media-assessments.json: 237 videos / 1,185 reviewed items
```

Supporting unit tests also pass:
`test_audit_media_assessment_uniqueness` (5) and `test_validate_catalog_parity`
(2) — 7 tests OK.
