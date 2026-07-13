
# Maxine’s World Implementation Handoff

## 1. Product Definition

Maxine’s World is a private, Android-first educational app for an eight-year-old Grade 3 learner. It combines structured curriculum, adaptive practice, animated animal characters, offline lessons, gamification, and parent monitoring.

| Item | MVP decision |
|---|---|
| Curriculum | Philippines Grade 3, MATATAG-aligned |
| Subjects | English, Filipino, Mathematics, Science, Philippine History and Community |
| Platform | Android tablet and phone |
| Session length | 10–15 minutes |
| Primary theme | Friendly creature village |
| Main characters | Cats supported by Philippine and international animals |
| Parent access | Protected by PIN or device biometrics |
| Connectivity | Offline-first |
| Advertising | None |
| Public communication | None |

Curriculum mappings must be validated by qualified Philippine educators before publication. Do not invent or display unverified DepEd competency codes.

## 2. Instructions for the Implementing AI

1. Use Kotlin and Jetpack Compose.
2. Build tablet-first responsive layouts.
3. Keep lessons and curriculum mappings outside the application binary.
4. Load versioned lesson packages from a content API.
5. Store downloaded content and progress locally.
6. Synchronize progress when connectivity returns.
7. Implement all lesson activities through reusable engines.
8. Use the graphics in this document as design references, not final UI assets.
9. Recreate text, controls, and accessibility semantics natively.
10. Prioritize learning accuracy, safety, reliability, and accessibility over engagement metrics.

## 3. Visual Identity

The world is a welcoming animal village with cats as the primary companions.

![Maxine’s World character and identity sheet](assets/graphics/character-identity-sheet.png)

*Figure 1: Character, animal, icon, reward, and visual-style references for Maxine’s World.*

### Canonical characters

| Character | Role |
|---|---|
| Milo, orange tabby | Main adventure and Mathematics guide |
| Niko, gray cat | Science guide |
| Mira, calico cat | English and Filipino reading guide |
| Lakan, Philippine forest cat | Philippine History guide |
| Ollie, owl | Review and reflection guide |
| Shelly, pawikan | Persistence guide |
| Tilly, otter | Investigation guide |
| Duke, aspin | Teamwork guide |
| Poppy, rabbit | Practice guide |
| Flynn, fox | Puzzle guide |

The concept art labels the orange cat “Maxine.” Rename the production character Milo so Maxine remains the child and app namesake.

### Animal asset library

Create reusable poses, expressions, costumes, and animations for:

* Multiple cat breeds and coat patterns
* Aspin dogs
* Rabbits
* Foxes
* Bears
* Owls
* Otters
* Pawikan and land turtles
* Frogs
* Hedgehogs
* Red pandas
* Philippine eagle
* Tarsier
* Carabao
* Tamaraw
* Parrots
* Maya birds
* Dolphins
* Bees
* Geckos

## 4. Core User Interfaces

![Maxine’s World child village](assets/graphics/child-village-home.png)

*Figure 2: Child home screen with Daily Quest and subject destinations.*

Add two destinations to the production version:

* Filipino: Bahay ng Kuwento
* Philippine History: Heritage Harbor

![Maxine’s World progression map](assets/graphics/learning-progression-map.png)

*Figure 3: Learning-world progression map with unlockable village locations.*

![Maxine’s World parent dashboard](assets/graphics/parent-dashboard.png)

*Figure 4: Parent dashboard for progress, recommendations, assignments, and screen-time controls.*

### Child features

* Daily Quest
* Subject worlds
* Animated explanations
* Narrated instructions
* Guided and independent practice
* Hints and worked examples
* Mistake notebook
* Spaced-review activities
* Avatar and companion customization
* Village progression
* Offline lessons

### Parent features

* Multiple child profiles
* Curriculum and grade selection
* Subject and skill progress
* Active learning time
* Mistakes and misconceptions
* Recommended lessons
* Parent-assigned modules
* Screen-time controls
* Offline-download management
* Weekly summaries
* Data export and deletion

## 5. Daily Learning Loop

```text
Warm-up: 2–3 minutes
Core lesson: 5–7 minutes
Spaced review: 2–3 minutes
Reflection and reward: under 1 minute
```

Daily Quests should rotate among all five subjects while prioritizing assigned lessons, prerequisites, and skills needing review.

## 6. Technical Architecture

### Android

* Kotlin
* Jetpack Compose
* Material 3 foundations with custom design tokens
* Navigation Compose
* Hilt
* Coroutines and Flow
* Room
* WorkManager
* Retrofit or Ktor client
* Kotlin serialization
* Media3
* Rive
* Lottie
* Android text-to-speech
* DataStore

### Backend

* Parent authentication
* PostgreSQL curriculum and progress database
* Object storage for lesson assets
* REST or GraphQL API
* Versioned content manifests
* Append-only progress events
* Recommendation and mastery service
* Parent-reporting service
* Feature flags
* Automated backups

### Repository structure

```text
android/
  app/
  core-model/
  core-network/
  core-database/
  core-design-system/
  feature-auth/
  feature-child-home/
  feature-daily-quest/
  feature-lesson-player/
  feature-progress/
  feature-parent/
  feature-rewards/
  feature-downloads/
  engine-activity/
  engine-assessment/
  engine-mastery/
  engine-sync/

backend/
  auth/
  curriculum/
  content/
  progress/
  mastery/
  assignments/
  rewards/
  reporting/
  sync/

cms/
  standards/
  skills/
  lessons/
  activities/
  assessments/
  assets/
  review/
  publishing/
```

## 7. Curriculum and Content Model

```text
Country
→ Curriculum
→ Version
→ Grade
→ Subject
→ Strand
→ Standard
→ Skill
→ Lesson
→ Activity
→ Assessment
```

### Lesson schema

```yaml
id: string
schemaVersion: integer
country: PH
curriculum: MATATAG
grade: 3
subject: string
moduleId: string
skillIds: string[]
title: string
objective: string
prerequisiteSkillIds: string[]
estimatedMinutes: integer
guideCharacter: string
steps: Activity[]
assessment: Assessment
assets: AssetReference[]
educatorValidated: boolean
version: integer
status: DRAFT|REVIEW|APPROVED|PUBLISHED|ARCHIVED
```

### Required activity engines

```text
animated_explanation
multiple_choice
multi_select
drag_and_drop
matching
sort_and_classify
sequence_events
word_builder
sentence_builder
story_comprehension
read_aloud
voice_response
short_text_response
place_value_builder
array_builder
fraction_model
clock_interaction
graph_builder
map_interaction
timeline_builder
virtual_investigation
prediction_observation_explanation
checkpoint_quiz
```

## 8. Mastery Model

```text
NOT_STARTED
INTRODUCED
PRACTICING
PROFICIENT
MASTERED
NEEDS_REVIEW
```

Mastery must consider:

* Accuracy
* Attempts
* Hint usage
* Response time
* Question difficulty
* Activity variation
* Independent performance
* Delayed-review performance

A skill should require at least ten meaningful attempts, 80% recent accuracy, two activity forms, and a successful delayed review before reaching `MASTERED`.

## 9. Grade 3 English Modules

| # | Module | Main learning goals | Activity |
|---:|---|---|---|
| 1 | Purring Sound Detectives | Syllables, rhymes, sound patterns, sight words | Sort words into cat sound baskets |
| 2 | Otter’s Word Splash | Long vowels, clusters, diphthongs | Build stepping-stone words |
| 3 | Gecko’s Big-Word Climb | Decode two-syllable words | Divide words to climb a wall |
| 4 | Parrot’s Meaning Market | Context clues, synonyms, antonyms | Trade vocabulary cards |
| 5 | Puppy’s Sentence Parade | Complete sentences and punctuation | Arrange sentence floats |
| 6 | Tarsier’s Word Team | Nouns, pronouns, verbs, adjectives | Build scene descriptions |
| 7 | Turtle’s Time Trail | Sequence and verb tense | Arrange timeline scenes |
| 8 | Cat’s Story Lantern | Character, setting, problem, solution | Retell an illustrated story |
| 9 | Eagle’s Clue Quest | Prediction and inference | Collect evidence feathers |
| 10 | Carabao’s Fact Farm | Main idea and supporting details | Build a detail haystack |
| 11 | Bee’s Speaking Hive | Listening and oral communication | Record an announcement |
| 12 | Maxine’s Animal Storybook | Plan, draft, revise, and publish | Create a digital animal book |

![English comprehension lesson](assets/graphics/english-lesson-main-idea.png)

*Figure 5: English main-idea lesson using an animal rescue story.*

## 10. Grade 3 Filipino Modules

Filipino begins as a distinct language learning area from Grade 2 under the MATATAG language structure. Grade 3 should strengthen oral and written fluency, comprehension, vocabulary, grammar, composition, literature, and cultural awareness.

| # | Module | Layunin at paksa | Interaktibong gawain |
|---:|---|---|---|
| 1 | Mga Pusang Mahusay Makinig | Pag-unawa sa napakinggang kuwento, usapan, balita, at tula | Makinig sa kuwento at pumili ng tamang larawan |
| 2 | Pantig-Pawikan | Pagbasa ng mga salitang may tatlo o higit pang pantig | Hatiin at buuin ang mga salita sa kabibe |
| 3 | Pamilihan ng mga Salita | Salitang magkasingkahulugan, magkasalungat, at magkakatugma | Magpalit ng word cards sa palengke |
| 4 | Diksyunaryo ni Mira | Alpabeto, kahulugan, at wastong paggamit ng diksyunaryo | Hanapin ang nawawalang salita sa aklatan |
| 5 | Pangngalan sa Pamayanan | Pangngalang pantangi at pambalana para sa tao, lugar, hayop, at bagay | Lagyan ng pangalan ang mga lugar sa Cat Village |
| 6 | Panghalip na Pamalit | Ako, ikaw, siya, kami, tayo, sila, ito, iyan, at iyon | Palitan ang pangngalan sa pangungusap |
| 7 | Magagalang na Kuting | Magalang na pananalita sa iba’t ibang sitwasyon | Pumili ng angkop na tugon sa role-play |
| 8 | Panuto ni Duke | Pagsunod sa nakasulat at napakinggang panutong may 2–4 hakbang | Sundan ang treasure-map instructions |
| 9 | Bahagi ng Kuwento | Tauhan, tagpuan, banghay, suliranin, at wakas | Ayusin ang illustrated story cards |
| 10 | Pangunahing Kaisipan | Pangunahing kaisipan at mga sumusuportang detalye | Bumuo ng puno ng kaisipan |
| 11 | Talata ng Aking Pamayanan | Wastong malaking letra, bantas, baybay, at magkakaugnay na pangungusap | Sumulat ng maikling talata tungkol sa pamayanan |
| 12 | Aklat-Kuwento ni Maxine | Pagpaplano, pagsulat, pagwawasto, at pagsasalaysay | Gumawa at magbasa ng digital Filipino storybook |

### Filipino capstone

Title: Ang Mga Kuting na Tumulong sa Barangay

Objective: Matukoy ang pangunahing kaisipan, maiayos ang mga pangyayari, at makapagsalaysay gamit ang sariling salita.

Flow:

1. Makinig sa kuwentong binabasa ni Mira.
2. Sagutin ang mga tanong tungkol sa tauhan at tagpuan.
3. Ayusin ang mga pangyayari.
4. Piliin ang pangunahing kaisipan.
5. Magrekord ng maikling pagsasalaysay.
6. Sumulat ng tatlo hanggang limang pangungusap.
7. Tumanggap ng badge na `Mahusay na Tagapagsalaysay`.

## 11. Grade 3 Mathematics Modules

| # | Module | Main learning goals | Activity |
|---:|---|---|---|
| 1 | Number Lion | Whole numbers to 10,000 | Build place-value dens |
| 2 | Pattern Parrot | Increasing, decreasing, and repeating patterns | Complete feather sequences |
| 3 | Addition Alpaca | Four-digit addition and estimation | Pack supply totals |
| 4 | Subtraction Squirrel | Subtraction and Philippine money | Operate a sari-sari store |
| 5 | Times-Table Tiger | Equal groups, arrays, tables 6–9 | Arrange animal teams |
| 6 | Division Dolphin | Grouping, sharing, and remainders | Share fish among pods |
| 7 | Fraction Fox | Represent and compare fractions | Divide animal-food trays |
| 8 | Clockwork Owl | Time and elapsed time | Build a rescue schedule |
| 9 | Measuring Bear | Mass and capacity | Prepare animal-food portions |
| 10 | Geometry Gecko | Lines, symmetry, and translation | Draw grid paths |
| 11 | Shape Beaver | Perimeter and area | Design an animal habitat |
| 12 | Data Detective Cat | Tables, pictographs, and bar graphs | Conduct a village survey |

![Mathematics equal-groups lesson](assets/graphics/math-lesson-equal-groups.png)

*Figure 6: Mathematics lesson using equal groups, draggable fish counters, and immediate feedback.*

## 12. Grade 3 Science Modules

| # | Module | Main learning goals | Activity |
|---:|---|---|---|
| 1 | Curious Cat Scientists | Observe, classify, measure, infer, and predict | Investigate mystery objects |
| 2 | Pawprints on Materials | Observable material properties | Run virtual material tests |
| 3 | Otter’s States of Matter | Solids, liquids, and gases | Sort materials into habitats |
| 4 | Chameleon Changes | Heating, cooling, mixing, and shaping | Sequence material changes |
| 5 | Carabao’s Useful Materials | Match properties and practical uses | Build a rain shelter |
| 6 | Healthy Kitten, Healthy Me | Body care, food, exercise, and safety | Build a healthy routine |
| 7 | Bee’s Busy Garden | Plant parts, needs, and growth | Maintain a virtual plant diary |
| 8 | Tarsier’s Animal Neighbors | Animal features, movement, and needs | Classify Philippine animals |
| 9 | Pawikan Habitats | Habitats and life cycles | Restore a coastal habitat |
| 10 | Puppy Pushes and Pulls | Force, motion, speed, and direction | Guide a ball through a course |
| 11 | Owl’s Light, Sound, and Heat | Energy sources, uses, and safety | Complete a safety inspection |
| 12 | Weather Frog’s Earth Watch | Weather observation and preparedness | Maintain a weather log |

![Science plant investigation](assets/graphics/science-lesson-plant-growth.png)

*Figure 7: Science investigation using predictions, controlled conditions, and recorded observations.*

## 13. Philippine History and Community Modules

At Grade 3, Araling Panlipunan should begin with the learner’s province, region, local stories, identity, cultural heritage, change over time, symbols, and local heroes. National history should be introduced through age-appropriate connections rather than memorizing long lists of dates.

| # | Module | Main learning goals | Interactive activity |
|---:|---|---|---|
| 1 | Lakan’s Map Adventure | Map symbols, directions, province, and region | Build a map with draggable symbols |
| 2 | Mountains, Rivers, and Communities | Relate landforms and bodies of water to community life | Place villages in suitable environments |
| 3 | Our Province Through Time | Compare past and present transportation, homes, schools, and work | Use a before-and-after time slider |
| 4 | Stories of Our Region | Explore oral histories, legends, and historical community accounts | Reconstruct a story from illustrated clues |
| 5 | Symbols of Identity | Understand provincial seals, landmarks, songs, art, and celebrations | Design a local identity shield |
| 6 | Local Heroes and Helpers | Recognize contributions of local heroes and community leaders | Match people with their contributions |
| 7 | Cultures of the Philippines | Appreciate languages, clothing, food, music, crafts, and traditions | Complete a respectful cultural map |
| 8 | Young Heritage Keepers | Protect historical places, objects, traditions, and community memories | Create a digital heritage exhibit |

### History capstone

Title: Lakan and the Lost Barangay Album

Objective: Explain how a community changes over time while preserving important parts of its identity.

Flow:

1. Examine old and modern illustrated photographs.
2. Sort objects into past, present, or both.
3. Listen to a fictional elder cat’s community story.
4. Identify changes and continuities.
5. Place events on a simple timeline.
6. Create a one-page heritage album.
7. Earn the `Young Heritage Keeper` badge.

Avoid presenting fictional characters or generated stories as real historical evidence. Clearly label legends, fictional stories, oral history, and verified historical information.

## 14. Gamification

### Learning loop

```text
Complete learning activity
→ earn village energy
→ restore or decorate a location
→ unlock a character interaction
→ collect a cat or animal item
→ continue the story
```

### Rewards

* Mastery stars
* Paw coins
* Subject badges
* Cat costumes
* Companion accessories
* Village decorations
* Story chapters
* Heritage collection cards
* Science specimens
* Reading-library items

### Engagement safeguards

* No public leaderboard
* No loss of rewards for missing a day
* No randomized paid rewards
* No advertising
* No reward for idle screen time
* Parent-controlled optional play time
* Praise persistence and strategy
* Keep celebrations short and skippable

## 15. Content Management

### Workflow

```text
DRAFT
→ CURRICULUM_REVIEW
→ LANGUAGE_REVIEW
→ ACCESSIBILITY_REVIEW
→ TECHNICAL_QA
→ APPROVED
→ STAGED
→ PUBLISHED
→ ARCHIVED
```

### CMS requirements

* Curriculum and standards editor
* Skill-prerequisite graph
* Lesson builder
* Question bank
* Filipino and English localization
* Narration and transcript management
* Image, audio, animation, and map assets
* Android preview
* Reviewer comments
* Version history
* Scheduled publishing
* Rollback
* Content-package validation

## 16. Offline and Synchronization

* Download content by curriculum, grade, and subject.
* Verify checksums before activating content.
* Preserve the previous valid package.
* Store progress as append-only events.
* Queue events offline.
* Use idempotency keys for rewards.
* Recalculate mastery after merging evidence.
* Never lose completed work during synchronization.
* Show download and synchronization status to the parent.

## 17. Privacy and Accessibility

### Privacy

* Parent-created child profiles
* No child email
* No precise location
* No advertising identifier
* No public profile or chat
* Encrypted data
* Secure credential storage
* Parent-controlled export and deletion
* Specialist child-privacy review before public release

### Accessibility

* Minimum 48dp touch targets
* Narrated instructions
* Captions and transcripts
* Adjustable text size
* High contrast
* Reduced-motion mode
* Color-independent feedback
* Replayable prompts
* Alternatives to drag-and-drop
* No essential timed activities

## 18. MVP Delivery Plan

| Milestone | Deliverable |
|---|---|
| 1 | Android project, parent login, child profile, local storage |
| 2 | Lesson engine with multiple choice, drag, sorting, narration, and assessment |
| 3 | One pilot lesson for each of the five subjects |
| 4 | Mastery, recommendations, Daily Quest, and spaced review |
| 5 | Parent dashboard, assignments, screen-time controls, and reports |
| 6 | Village progression, animal characters, rewards, and animations |
| 7 | CMS, review workflow, publishing, and rollback |
| 8 | Full Philippines Grade 3 content production |
| 9 | Singapore and US curriculum mapping |

## 19. Initial Pilot Lessons

Build these first:

1. English: The Cats Who Saved the Garden
2. Filipino: Ang Mga Kuting na Tumulong sa Barangay
3. Mathematics: Milo’s Equal-Groups Market
4. Science: Niko’s Plant Investigation
5. Philippine History: Lakan and the Lost Barangay Album

Each lesson must work offline, resume after interruption, report progress to the parent, and load from external lesson data.

## 20. Definition of Done

A lesson is complete when:

* Its objective and prerequisites are defined.
* Curriculum alignment has educator approval.
* Activities measure the stated objective.
* Correct and incorrect feedback is available.
* Narration has a transcript.
* All assets work offline.
* Accessibility checks pass.
* Progress and mastery events are recorded.
* Android preview and device testing pass.
* Content can be rolled back safely.

The first implementation target is a reliable 10-minute Daily Quest containing one pilot lesson, one review activity, and one short reward sequence.
