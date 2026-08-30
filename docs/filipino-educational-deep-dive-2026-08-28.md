# Maxine’s World — Educational Deep Dive and Filipino-Learning Roadmap

**Audit date:** 2026-08-28  
**Learner:** Maxine, age 8, Grade 3, complete beginner in Filipino  
**Latest published release:** `v0.70.0` at `6f95a04c`  
**Latest repository build audited:** `origin/main` at `c5b031804bf3b272258e286c909ac9429e7b4912` (`0.70.0-2-gc5b03180`, version code 345)

## Executive decision

Maxine’s World has a strong technical shell: offline caching, orderly video progression, post-video checks, rewards, Filipino TTS in Assessment Arena, local progress storage, and a large curriculum library. However, it does **not yet provide a coherent Filipino acquisition path for a child starting from zero**.

The present Filipino experience is best described as a **large native-school video library with quizzes**, not a beginner second-language course. The 16 Grade 1 videos are useful raw material, but Grade 1 school content assumes more linguistic knowledge than Maxine has. They should remain, but be preceded and wrapped by a purpose-built **Filipino Foundations / Pre-A1 bridge**.

The highest-value direction is:

> Build a short, audio-first Filipino Foundations track; segment and scaffold the existing Grade 1 videos; re-author every video assessment from verified video evidence; then wire mastery, spaced review, and the new prerequisite graph into the learner path.

Do **not** add more videos yet. The bottleneck is not quantity. It is sequencing, comprehensibility, active language use, and retention.

## 1. What the latest build actually contains

### Release and verification status

- GitHub release `v0.70.0` is two commits behind current `origin/main`.
- The two newer commits add `video-checkpoints.json` and `skill-graph.json`.
- Local `assembleDebug` succeeds at `c5b03180`.
- Local `testDebugUnitTest` does not compile because `MiloReviewQueueResolverTest.kt:19,27,49` passes a removed `lastPracticed` parameter; `MasteryRecord` now defines `lastActivityAt` (`core-model/.../Models.kt:193-200`).
- GitHub CI for `c5b03180` is red. The semantic job finds 10 exact duplicate assessment payloads, including five Filipino duplicates. Connected tests also fail.
- Structural validators pass:
  - 358 lesson files, 0 strict schema errors/warnings.
  - 237 media records and 711 checkpoint records structurally valid.
  - 168 skill nodes; acyclic; no dangling references.
- These checks prove shape and referential consistency, not educational alignment.

Current `main` therefore builds an APK, but it is **not a green release candidate**.

### Live media inventory

The deployed LAN catalog at `http://10.10.10.33/media/catalog.json` contains 237 videos:

| Subject | Videos |
|---|---:|
| Filipino | 100 |
| Makabansa | 51 |
| Mathematics | 24 |
| English | 22 |
| GMRC | 20 |
| Science | 20 |

Filipino distribution:

| Grade | Videos | Status |
|---|---:|---|
| Grade 1 | 16 | `PREVIEW` |
| Grade 2 | 35 | `PREVIEW` |
| Grade 3 | 33 | `RELEASED` |
| Grade 4 | 16 | `PREVIEW` |

All are `PERSONAL_USE`. The Grade 1 sequence is about **370 minutes**, averages **23.1 minutes per video**, and totals approximately **1.09 GB**. The entire Filipino library is approximately **5.24 GB**.

### Catalog provenance mismatch — release blocker

The repository contains two entirely different media generations:

| Source | Total media | Filipino | Filipino grades | Questions per Filipino video |
|---|---:|---:|---|---:|
| Live LAN catalog (`10.10.10.33`) | 237 | 100 | G1 16 · G2 35 · G3 33 · G4 16 | 5 |
| Checked-in `server/content/catalog.json` | 337 | 81 | G1 33 · G2 23 · G3 25 | 3 |

The catalogs have **zero overlapping `mediaId` values**. The tracked `media-assessments.json` matches all 237 live IDs and none of the 337 checked-in IDs. Therefore, the counts above describe separate catalog generations and must never be combined. The live LAN catalog is authoritative for the emulator behavior observed in this audit; the checked-in server catalog is evidence of unreconciled deployment state, not the live inventory.

Before any new content release, reconcile the checked-in server catalog, `media-assessments.json`, generated deployment catalog, and live LAN catalog by stable `mediaId`, then fail CI on divergence.

### Runtime learner journey

Verified on the API 35 tablet emulator at 3048×2032:

1. Home presents a Filipino card labeled `Not started · 0 of 100 videos`.
2. Tapping Filipino opens `Filipino · Kwentuhan`.
3. The screen offers `Download All (100)`.
4. Episode 1 is `Grade 1 MTB Q1 Ep1: Pagsasabi ng Tungkol sa Sarili at Sariling Karanasan`, duration 22:30.
5. Episodes 2 onward say `Complete the previous lesson first`.
6. There is no beginner orientation, English support choice, word/phrase preview, goal card, caption track, or explanation of Filipino versus Tagalog.

The navigation is technically clear, but the learner is placed into a long native-school lesson with no linguistic bridge.

## 2. What currently works well

### A. Age-appropriate product shell

- The subject card is easy to find.
- Lessons are presented in chronological order.
- Completed lessons move to a separate section.
- Touch targets and tablet layout are generally child-friendly.
- Rewards are idempotent; replay cannot farm stars.
- The app stores learning data locally and works with downloaded media.

### B. Honest assessment metadata

Every media assessment has five MCQs and requires 4/5. Importantly, `claimsMastery=false` (`core-model/.../MediaModels.kt:35-42`). This correctly recognizes that a short memory check does not prove language mastery.

### C. Assessment Arena read-aloud

Assessment Arena can read prompts, options, and explanations using Filipino `fil-PH` TTS (`AssessmentArenaScreen.kt:623-643`, `938-955`; `LessonTtsPlayer.kt:77-95`). This helps an emerging reader.

### D. Good building blocks exist

The repository already contains:

- a mastery algorithm with delayed-review requirements (`engine-mastery/.../MasteryEngine.kt:12-70`);
- a due-review resolver (`core-model/.../MiloReviewQueue.kt:25-71`);
- a Room mastery table/DAO;
- a 168-node prerequisite graph;
- a 711-item video-checkpoint manifest;
- a writing-production renderer;
- daily mission, parent dashboard, and reward systems.

The problem is integration, not absence of ideas.

## 3. Critical educational gaps

### P0 — Catalog sources disagree completely

Release tooling cannot currently make one authoritative claim about the Filipino corpus: the live app exposes 100 Filipino videos, while the checked-in server catalog describes 81 different Filipino records. With zero shared IDs, assessment, checkpoint, license, and progression validation can accidentally certify one generation while the child uses another.

**Consequence:** catalog reconciliation is prerequisite work. Do not author, approve, package, or promote Filipino content until every source agrees on identity and assessment namespace.

### P0 — Filipino video assessments are not video-specific

The 100 Filipino videos have 500 questions, but only:

- **33 unique normalized prompts**;
- **9 distinct five-question banks**;
- one bank reused across **29 videos spanning Grades 1–4**.

The first video is about saying things about oneself and one’s experiences. Its check instead asks about complete sentences, verbs, listening behavior, story organization, and test-taking behavior (`media-assessments.json:3266-3410`). These are not a focused check of self-introduction or personal expression.

All 500 Filipino explanations use the template:

> `Tama. Ang sagot ay ... dahil ito ang tumutugon sa konseptong sinusukat ng tanong.`

This restates the key and uses assessment jargon instead of explaining the language clue. Passing can reflect recognition of a recycled bank rather than learning from the current video.

**Consequence:** the hard sequence lock is pedagogically unsafe. A mismatched 4/5 quiz becomes the gate to the next lesson.

### P0 — School grade is being treated as language proficiency

Grade 1 material is an appropriate source of themes and literacy practice, but it is not automatically a zero-beginner course. The DepEd Grade 1 portal treats Filipino as a broad subject with speaking, listening, phonological awareness, vocabulary, reading, composition, and writing strands, and lists Mother Tongue separately.[4]

Maxine needs **Grade 1 linguistic content presented at an eight-year-old cognitive and social level**. She should not receive infantile themes, but she does need beginner-language scaffolds that a Filipino-speaking first grader would not.

### P0 — Daily guidance bypasses the Grade 1 bridge

`DailyQuestManager.kt:142-168` deliberately filters videos to Grade 3 and `RELEASED`. Therefore the 16 Grade 1 Filipino videos cannot become Maxine’s guided next step; she must discover them manually through the 100-video library.

The runtime Daily Mission selected a Grade 3 Filipino video plus two Grade 3 Math Arena packs. That is correct for a normal Grade 3 learner, but wrong for Maxine’s Filipino proficiency.

### P0 — New checkpoint and skill-graph systems are not wired

`video-checkpoints.json` contains 711 records, but no Kotlin model, loader, player overlay, or navigation path reads it. The player only marks completion when ExoPlayer reaches `STATE_ENDED` (`VideoStep.kt:176-180`).

`skill-graph.json` contains 168 nodes and 126 prerequisite links, but production Kotlin never loads it. The validator checks JSON shape, references, and cycles only (`tools/validate_skill_graph.py:9-56`).

The graph also uses broad generic nodes, such as seven Grade 1 Filipino strands (`skill-graph.json:1392-1494`), with no mapping from a media ID or assessment item to a skill node.

**Conclusion:** both are promising content artifacts, not live educational features.

### P1 — Recognition dominates; production is absent

The active Filipino learning loop is:

> watch long video → choose four-option answers → receive stars

There is no active speech recognizer, recording flow, phrase imitation, conversational response, or typed production in the active video path. A writing renderer exists, but no Filipino lesson JSON uses `WRITING_PRODUCTION`, and legacy text lessons are not reachable from the video-first child home.

ACTFL frames proficiency as functional, real-world language ability and notes that outcomes depend on the amount/type of exposure, instruction, and assessment; it also recommends supplementing proficiency tests with information-rich assessments.[1] Current completion and MCQ scores do not show whether Maxine can understand or use Filipino.

### P1 — Listening support is inconsistent

- Assessment Arena has Filipino TTS.
- Video memory checks do not have a Listen button.
- The Arena TTS says English chrome such as `Option A` even when reading Filipino content (`AssessmentArenaScreen.kt:628-631`).
- Generic hints fall back to English (`AssessmentArenaScreen.kt:902-904`).
- If Filipino TTS data is missing, Arena calls do not supply an `onUnavailable` handler, so failure is silent.
- The video player provides 1.0×, 1.25×, 1.5×, and 2.0×, but no 0.75× learner mode (`VideoStep.kt:212-255`).
- No SRT/VTT captions, transcripts, sentence highlighting, or phrase-level loop is connected.

### P1 — Long videos create unnecessary cognitive load

The Grade 1 videos average 23.1 minutes. The video-learning review recommends brief, targeted segments, signaling, and interactive questions; it specifically lists short videos of six minutes or less and packaging video with interactive questions as useful design patterns.[3]

This evidence comes mainly from broader educational-video research rather than a Filipino child trial, but it matches the observed usability problem: one 22–25 minute uninterrupted lesson is a heavy first encounter for an eight-year-old who understands almost none of the spoken language.

### P1 — Mastery and spaced review are mostly dormant

The code has sensible mastery/review algorithms, but:

- `ProgressTracker.getMasterySummary()` returns `emptyList()` (`feature-progress/.../ProgressTracker.kt:8-13`).
- No production caller invokes `MasteryEngine.computeMastery()` or `nextReviewDays()`.
- No production code calls `MasteryRecordDao.upsert()`.
- `MiloReviewQueueResolver` is used only by tests.
- The parent dashboard reads mastery rows but has no active pipeline that creates them.
- The dashboard labels passed videos as `videos mastered` (`ParentDashboardScreen.kt:696-700`) even though media assessments explicitly set `claimsMastery=false`.

Spacing and active retrieval improve long-term retention; a language-learning example in the AERO guide explicitly recommends spreading vocabulary across lessons and later applying it in conversation.[2]

### P2 — Assessment Arena is a challenge surface, not beginner instruction

The Filipino Philippine pack starts with terms such as *imperpektibo*, *pang-uri*, *panghalip panao*, *pangngalang pambalana*, and *salitang-ugat* (`assessment-packs/filipino-g3-ph.json:11-191`). This is suitable as a Grade 3 diagnostic or challenge after instruction, not as a zero-beginner path.

## 4. Recommended target learning architecture

### Principle: separate age, school grade, and language proficiency

Keep Maxine’s profile at Grade 3, but add a Filipino proficiency state:

- `NEW_TO_FILIPINO`
- `FOUNDATIONS_1`
- `FOUNDATIONS_2`
- `EARLY_READER`
- `GRADE_LEVEL_SUPPORT`

This prevents all other subjects from being lowered while allowing Filipino to begin at the right linguistic level.

### Add a Filipino Foundations bridge

#### Stage 0 — Orientation and help language

Teach what Filipino is and give Maxine control:

- `Pakiulit.` — Please repeat.
- `Hindi ko naiintindihan.` — I don’t understand.
- `Ano ang ibig sabihin nito?` — What does this mean?
- `Opo / Hindi po.`
- `Salamat.`

Explain, briefly, that Filipino is the national language, is closely based on Tagalog, and exists alongside many Philippine languages.

#### Stage 1 — Me and greetings

- `Kumusta?`
- `Mabuti ako.`
- `Ang pangalan ko ay Maxine.`
- age, feelings, yes/no, please/thank you.

#### Stage 2 — Home, family, and needs

- family members;
- food, drink, water;
- `Gusto ko...`, `Ayoko...`, `Mayroon akong...`;
- common household objects and requests.

#### Stage 3 — School and play

- classroom objects;
- `Makinig`, `Tingnan`, `Basahin`, `Isulat`, `Buksan`, `Isara`;
- colors, numbers, position words;
- playground and animal vocabulary.

#### Stage 4 — Routines and short stories

- waking, eating, school, play, bedtime;
- simple present/progressive chunks;
- picture-supported stories;
- sequencing and retelling.

#### Stage 5 — Grade 1 literacy bridge

Only now introduce the existing Grade 1 phonics, rhyme, letters, book vocabulary, and listening-comprehension videos. Pre-teach recurring words and segment every video.

### Use one repeatable micro-lesson loop

Each 8–12 minute session should follow:

1. **Listen and notice** — 4–6 words/phrases with picture or action.
2. **Understand** — select the matching image/action; minimal English reveal is optional.
3. **Say it with Milo** — record or repeat a short phrase; effort is rewarded.
4. **Build it** — arrange phrase tiles or choose a meaningful reply.
5. **Use it** — one mini-dialogue or real-world scenario.
6. **Retrieve it later** — schedule a different-context review tomorrow and next week.

Do not introduce more than one or two productive sentence frames per session.

### Transform existing videos instead of discarding them

For each Grade 1 video:

- create a verified transcript;
- select one learning objective;
- cut or chapter the lesson into 3–6 minute segments;
- add Filipino captions and word-level replay;
- add a 0.75× mode;
- pre-teach 4–6 target words;
- insert one genuinely topic-specific checkpoint per segment;
- write a new five-item check grounded in the transcript;
- add one listening-only item and one guided production task;
- provide an English gloss only when requested;
- download the next segment/module, not all 100 videos.

### Make Milo a language partner, not decoration

Use a deterministic, offline dialogue state machine before considering an LLM:

1. Milo speaks.
2. Maxine chooses a response.
3. Milo models the phrase slowly.
4. Maxine repeats or records it.
5. Milo accepts reasonable variants.
6. The same phrase returns in a new setting.

Example:

> Milo: `Kumusta ka?`  
> Maxine: chooses or says `Mabuti ako.`  
> Milo: `Mabuti rin ako! Ano ang pangalan mo?`  
> Maxine: `Ang pangalan ko ay Maxine.`

### Track mastery by modality

Do not store one undifferentiated Filipino score. For each word/phrase, track:

- heard and understood;
- read and understood;
- recalled from picture/context;
- produced with tiles/text;
- spoken attempt completed;
- used in a new context;
- retained after 1, 7, and 30 days.

A progression gate should require evidence across at least two contexts and a delayed review. A single 4/5 immediate MCQ should never equal mastery.

## 5. Recommended daily and weekly rhythm

### Daily Filipino session: 10–15 minutes

- 1 short new micro-lesson;
- 1 due review;
- 1 meaningful use task;
- optional story/song extension.

Replace the current 30–50 minute multi-subject video quest for the beginner Filipino track. Keep the broader Daily Mission for other subjects.

### Weekly spiral

| Day | Focus |
|---|---|
| 1 | Introduce words and one phrase frame |
| 2 | Listening discrimination and picture recognition |
| 3 | Guided speaking and phrase building |
| 4 | Story/game using the same language |
| 5 | Delayed review and real-life mission |
| Weekend | Optional caregiver activity |

## 6. Caregiver and non-app support

The app should make a non-Filipino-speaking caregiver effective without turning them into the teacher.

Add a caregiver card containing:

- this week’s five phrases;
- tap-to-hear pronunciation;
- one five-minute family activity;
- what Maxine can understand versus say;
- due review items;
- encouragement guidance: model and repeat, do not over-correct accent.

Useful activities:

- greet each other in Filipino at breakfast;
- label five objects at home;
- count steps or food pieces;
- ask for water using `Tubig po` / `Pahingi po ng tubig`;
- play Filipino “I spy” with colors and animals;
- have a Filipino-speaking relative/tutor conduct a relaxed 10-minute conversation once or twice a week;
- use songs and read-aloud stories, but follow them with a comprehension or phrase-use task.

## 7. Implementation roadmap

### Release repair — immediate

1. Reconcile all catalog/assessment/checkpoint sources by stable `mediaId`; add a CI parity gate.
2. Fix the stale `lastPracticed` unit tests.
3. Resolve the 10 duplicate assessment payloads blocking CI.
4. Add CI jobs for `validate_video_checkpoints.py` and `validate_skill_graph.py`.
5. Do not advertise checkpoints or skill adaptation as live until Kotlin consumers and emulator tests exist.
6. Change parent copy from `videos mastered` to `video checks passed`.

### Phase 1 — Filipino Foundations pilot

1. Add language-proficiency state independent of Grade 3, with onboarding choices equivalent to `Wala pa akong alam na Filipino`, `May kaunti akong alam`, and `Handa na ako sa baitang`.
2. Build 20–24 Foundations micro-lessons covering orientation, self, family, needs, school instructions, colors/numbers, and daily routines.
3. Add native-speaker or human-reviewed audio, captions, phrase replay, and 0.75× speed.
4. Replace hard locking with a recommended path plus gentle prerequisite help.
5. Surface Foundations in Daily Quest when proficiency is `NEW_TO_FILIPINO`.
6. Keep **Filipino** as the learner-facing subject name; use **Tagalog** only as a search synonym or brief explanatory term unless content specifically teaches heritage Tagalog.

### Phase 2 — trustworthy video learning

1. Transcribe and educator-review all 100 Filipino videos.
2. Re-author 500 questions so every video has a unique, evidence-grounded bank.
3. Replace all 500 generic explanations with clue/rule/context explanations.
4. Wire `video-checkpoints.json` into ExoPlayer.
5. Split/segment long Grade 1 videos.
6. Add per-module download; remove `Download All (100)` as the primary action.

### Phase 3 — mastery and spaced review

1. Map every assessment/checkpoint to a skill-graph node.
2. Write progress events and mastery records after valid evidence.
3. Wire `MasteryEngine` and `MiloReviewQueueResolver` into Daily Quest.
4. Schedule reviews after 1, 3, 7, and 30 days, adjusted by performance.
5. Report listening, reading, production, and retention separately.

### Phase 4 — speaking and caregiver loop

1. Add record-and-playback first.
2. Add local speech recognition as supportive feedback, never a strict pass gate.
3. Add scripted Milo dialogues.
4. Add caregiver phrase cards and real-world missions.
5. Run true-beginner usability sessions with Maxine and at least two other children.

## 8. Measurable success criteria

After the first Foundations cycle, Maxine should be able to demonstrate—not merely select—that she can:

- understand 5–10 common one-step instructions;
- answer her name, age, and feeling with a short phrase;
- recognize 50–75 high-frequency words from audio/pictures;
- produce 10–15 useful phrase frames with support;
- ask for repetition/help;
- complete a 3–4 turn scripted dialogue;
- retain at least 80% of selected core phrases after one week;
- approach Filipino willingly and recover calmly from mistakes.

The parent dashboard should show these “Can Do” outcomes, not only minutes watched, quizzes passed, stars, or streaks.

## Final recommendation

Keep the Grade 1 Filipino videos, but stop treating their grade label as proof that they form a zero-beginner pathway. Use them as **source material inside a spiral, audio-first, communicative curriculum**.

The best next product milestone is not “more Filipino content.” It is:

> **Filipino Foundations v1: 20–24 short, human-reviewed, audio-first lessons; unique topic-grounded checks; one guided dialogue per unit; and real spaced review wired to the learner model.**

That would turn Maxine’s World from a good educational media library into an app that can genuinely help Maxine begin understanding and using Filipino.

## Sources

[1] https://www.actfl.org/research/research-briefs/proficiency-levels-for-k16 — ACTFL: K-16 language proficiency research brief
[2] https://www.edresearch.edu.au/guides-resources/practice-guides/spacing-and-retrieval-practice-guide-full-publication — AERO: Spacing and retrieval practice guide
[3] https://pmc.ncbi.nlm.nih.gov/articles/PMC5132380 — Effective Educational Videos
[4] https://lrmds.deped.gov.ph/grade/1 — DepEd Learning Portal: Grade 1
