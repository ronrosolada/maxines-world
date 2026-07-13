
# Grade 3 Learning App Product Plan

## 1. Product Vision

Create a private Android learning app that helps your daughter enjoy Grade 3 English, Mathematics, and Science through short lessons, stories, games, animations, challenges, and rewards.

The first complete curriculum pack will follow the Philippines MATATAG curriculum. The platform will nevertheless support curriculum packs for Singapore and the United States without requiring changes to the Android application.

## 2. Recommended Product Strategy

### Initial release

* Audience: Your family
* Primary learner: Your 8-year-old daughter
* Starting level: Grade 3
* Subjects: English, Mathematics, and Science
* First complete curriculum: Philippines MATATAG
* Devices: Android phones and tablets
* Accounts: One parent account with one or more child profiles
* Session length: Approximately 10–15 minutes
* Initial business model: Private and free
* Future option: Family subscription followed by school accounts

### Grade expansion

Build the curriculum structure for every grade from the beginning, but fully author Grade 3 first.

| Stage | Curriculum availability |
|---|---|
| MVP | Complete Philippines Grade 3 pack |
| Expansion 1 | Philippines Grades 1–6 |
| Expansion 2 | Singapore Primary 1–6 |
| Expansion 3 | US Grades K–5 |
| Expansion 4 | Philippines, Singapore, and US secondary grades |

This avoids the cost and quality risk of producing thousands of lessons before testing the experience with a real learner.

## 3. Product Principles

* Learning comes before rewards.
* Every activity maps to a documented curriculum competency.
* Lessons should be short enough to complete without fatigue.
* Incorrect answers should produce explanations rather than punishment.
* Difficulty should adapt separately for each subject and skill.
* The parent should understand progress without needing to interpret educational data.
* Animations should explain concepts, celebrate effort, and support storytelling.
* Content updates should not require a new Android release.
* No advertising, public chat, or manipulative engagement mechanics.
* Child privacy and safety should be built into the product.

## 4. Core User Experiences

### Child experience

The child application should include:

* A personalized home world or adventure map
* A daily learning quest
* Separate English, Mathematics, and Science zones
* Animated lessons and interactive stories
* Practice games and mini-challenges
* Hints and visual worked examples
* Read-aloud instructions
* Optional speech and pronunciation activities
* A collection of earned characters, badges, and cosmetic items
* A mistake-review area presented as a positive retry activity
* Downloaded lessons for offline use
* A child-friendly profile with avatar selection

### Parent experience

The protected parent area should include:

* Child profile and curriculum selection
* Weekly learning summary
* Time spent by subject
* Lessons attempted and completed
* Skills mastered, developing, or needing help
* Frequently repeated mistakes
* Recommended revision activities
* Daily and weekly screen-time limits
* Ability to assign a module
* Ability to lock or unlock optional games
* Content-download and storage controls
* Notification preferences
* Ability to switch between children in the future

Parent access should require a PIN, password, or device biometric authentication.

## 5. Grade 3 MVP Curriculum

The detailed lesson list should be validated against the current [DepEd Revised K–10 Curriculum](https://www.deped.gov.ph/revised-k-to-10-curriculum/) and its official curriculum guides before publication.

### English

Initial module groups should cover:

* Listening comprehension
* Oral language and vocabulary
* Phonics and word recognition
* Reading fluency
* Literal and inferential comprehension
* Grammar and sentence construction
* Spelling and word formation
* Paragraph writing
* Sequencing and summarizing
* Visual and media literacy

Recommended activity formats include interactive stories, sentence construction, read-and-answer adventures, vocabulary games, listening missions, and short guided writing.

### Mathematics

Initial module groups should cover:

* Place value and whole numbers
* Addition and subtraction
* Multiplication and division
* Fractions
* Patterns and algebraic thinking
* Measurement
* Time and money
* Geometry
* Data and graphs
* Multi-step word problems

Each lesson should move through a concrete, visual, and symbolic sequence. For example, multiplication can begin with animated groups of objects, progress to arrays, and then introduce equations.

### Science

Initial module groups should cover:

* Living and non-living things
* Parts and needs of plants
* Animals and habitats
* Human body and health
* Matter and observable properties
* Force, motion, light, heat, and sound
* Weather and the environment
* Earth and natural resources
* Observation, classification, and measurement
* Safe virtual investigations

Science activities should emphasize observation, prediction, experimentation, evidence, and explanation rather than memorization.

## 6. Curriculum Architecture

Every learning activity should be linked to structured curriculum metadata.

```text
Country
  → Curriculum and version
    → Grade or level
      → Subject
        → Strand
          → Standard or competency
            → Skill
              → Lesson
                → Activity
                  → Assessment item
```

Each content record should include:

* Country and curriculum version
* Grade and subject
* Quarter, term, or sequence
* Official standard or competency identifier
* Learning objective
* Prerequisite skills
* Difficulty level
* Estimated duration
* Activity type
* Mastery evidence
* Language and localization
* Accessibility metadata
* Publishing status and revision history

### Country packs

Philippines content should map to DepEd MATATAG competencies and school-year versions. English begins as a distinct subject from Grade 2, while formal Science begins in Grade 3.

Singapore content should map to [MOE syllabuses](https://www.moe.gov.sg/primary/curriculum/syllabus), including Primary levels, Standard and Foundation pathways, and later subject-based levels. Formal Primary Science begins at Primary 3.

US content should map English and Mathematics primarily to [Common Core](https://www.thecorestandards.org/read-the-standards/) and Science to [NGSS](https://www.nextgenscience.org/standards). Because the US does not have one federal curriculum, the system must allow additional state-specific mappings.

One lesson may map to multiple standards. This will make it possible to reuse suitable activities across countries while preserving separate sequences and terminology.

## 7. Content Management and Updating

Build a web-based content management system so authorized adults can update lessons without modifying the Android application.

### Content workflow

1. Create or import a curriculum standard.
2. Add learning objectives and prerequisites.
3. Build a lesson from reusable activity templates.
4. Attach images, narration, animations, questions, and hints.
5. Map the lesson to one or more curriculum standards.
6. Preview it in child mode.
7. Submit it for educational review.
8. Submit it for quality and safety review.
9. Publish it to a test group.
10. Release it by country, grade, subject, or child profile.
11. Roll back to an earlier version when necessary.

### Reusable lesson templates

* Animated explanation
* Interactive story
* Multiple-choice challenge
* Drag-and-drop classification
* Number-line activity
* Word builder
* Sentence builder
* Matching game
* Virtual science investigation
* Read-aloud comprehension
* Drawing or annotation task
* Short quiz
* Mastery checkpoint

Curriculum definitions, lesson metadata, questions, and asset references should be remotely delivered. Core game mechanics should remain inside the app for reliability and performance.

## 8. Learning and Mastery Model

Each skill should have a mastery state:

* Not started
* Introduced
* Practicing
* Proficient
* Mastered
* Needs review

Mastery should consider:

* Accuracy
* Number of attempts
* Hint usage
* Response time
* Performance on different question forms
* Retention after several days
* Performance on cumulative reviews

The app should not mark a skill as mastered after one lucky answer. It should require evidence across several questions and at least one delayed review.

### Adaptive learning loop

1. Run a short diagnostic.
2. Select an appropriate skill.
3. Show an animated explanation.
4. Provide guided practice.
5. Give immediate feedback.
6. Adjust difficulty.
7. Schedule review using spaced repetition.
8. Recommend the next skill based on prerequisites.

## 9. Gamification System

The central game concept can be an explorable world restored through learning.

### Core loop

Learn → practice → earn energy → restore or unlock part of the world → collect a reward → continue the story.

### Recommended mechanics

* Daily quests containing activities from all three subjects
* Experience points for effort and completion
* Stars for demonstrating mastery
* Coins for cosmetic items
* Subject-specific badges
* Avatar and companion customization
* Story chapters unlocked through learning
* Weekly family challenges
* Surprise rewards with clearly bounded frequency
* Gentle streaks with recovery days
* Boss challenges presented as mixed-skill reviews

### Engagement safeguards

* Never remove earned items because a child missed a day.
* Do not use public leaderboards.
* Avoid unlimited reward grinding.
* Reward retries, persistence, and hint use when appropriate.
* Keep optional mini-games short and connected to learning.
* Allow the parent to control total time and reward-game time.

## 10. Animation and Visual Direction

Use a coherent animated world rather than unrelated graphics.

The initial visual system should include:

* One friendly guide character
* Several collectible companions
* Three visually distinct subject worlds
* Animated lesson introductions
* Concept animations for difficult ideas
* Touch feedback and small celebration effects
* Character reactions to effort and progress
* Illustrated story scenes
* Accessible icons and large touch targets
* Reduced-motion mode

Prefer lightweight vector animations using Rive or Lottie. Use sprite sheets or short video only where vector animation is unsuitable. Assets should be downloadable and versioned through the content platform.

## 11. Technical Architecture

### Android application

Recommended technologies:

* Kotlin
* Jetpack Compose
* Clean Architecture or modular MVVM
* Room for offline storage
* WorkManager for content synchronization
* Media3 for audio and video
* Rive or Lottie for animation
* Text-to-speech for supported instructions
* Firebase Crashlytics or an equivalent private monitoring service

### Backend

The backend should provide:

* Parent and child accounts
* Curriculum and content APIs
* Progress and mastery records
* Assignment management
* Asset delivery
* Feature flags
* Content versioning
* Notifications
* Analytics
* Backup and synchronization

A practical private-MVP stack is Firebase Authentication, Firestore or PostgreSQL, Cloud Storage, serverless functions, and a small web CMS. PostgreSQL becomes preferable as standards mappings, reporting, and content relationships grow more complex.

### Offline behavior

The application should:

* Download selected grade and subject packs
* Save progress locally
* Allow lessons to continue without internet access
* Synchronize when connectivity returns
* Resolve conflicts without losing child progress
* Show the parent which content is available offline

## 12. Privacy, Safety, and Accessibility

* Collect only the minimum child data required.
* Use parent-controlled account creation.
* Do not include advertising or behavioral ad tracking.
* Do not expose child profiles publicly.
* Encrypt data in transit and at rest.
* Provide data export and deletion controls.
* Obtain specialist review before commercial release for COPPA and other applicable child-privacy requirements.
* Include captions, narration, readable fonts, strong contrast, and color-independent instructions.
* Support reduced motion and configurable audio.
* Avoid requiring reading ability for navigation.

## 13. MVP Delivery Roadmap

| Phase | Duration | Main outcome |
|---|---:|---|
| Discovery and curriculum mapping | 3–4 weeks | Grade 3 competency map, learning model, child testing plan |
| Prototype | 4–6 weeks | Clickable child journey, one mini-game, one animated lesson, parent dashboard |
| Foundation build | 8–10 weeks | Accounts, profiles, offline framework, CMS, progress tracking |
| Grade 3 content pilot | 8–12 weeks | One complete unit per subject with assessments and animations |
| Private beta | 4–6 weeks | Family testing, usability fixes, learning-data validation |
| Grade 3 completion | 12–20 weeks | Full Philippines Grade 3 content pack |
| Curriculum expansion | Ongoing | Additional grades and country mappings |

A small team should expect approximately six to nine months for a polished private Grade 3 product. A simple usable pilot can be ready sooner.

## 14. Suggested Team

* Product owner
* Android developer
* Backend or full-stack developer
* UI and child-experience designer
* Animator or illustrator
* Grade 3 curriculum specialist
* English, Mathematics, and Science reviewers
* QA tester
* Child-safety or privacy adviser before wider release

For the private MVP, some roles can be combined and content production can begin with a limited set of reusable templates.

## 15. Success Measures

### Learning

* Skill mastery rate
* Improvement between diagnostic and review
* Retention after 7 and 30 days
* Reduction in repeated mistakes
* Progress across prerequisite skill paths

### Engagement

* Completed learning sessions per week
* Percentage of sessions completed
* Voluntary return rate
* Average lessons completed without parent prompting
* Ratio of learning time to reward-game time

### Parent value

* Weekly report views
* Assigned-module completion
* Parent understanding of strengths and gaps
* Screen-time control usage
* Parent satisfaction

The goal should be consistent voluntary learning and measurable mastery, not maximum screen time.

## 16. First Build Backlog

### Must have

* Parent account and secure parent area
* Child profile and avatar
* Philippines Grade 3 curriculum map
* One complete unit for each subject
* Daily quest
* Animated explanations
* Guided practice and quizzes
* Hints and worked solutions
* Progress and mastery tracking
* Weekly parent report
* Parent-assigned activities
* Offline lesson support
* Content CMS with review and publishing
* Basic rewards, badges, and customization

### Should have

* Spaced-repetition review
* Voice narration
* Story progression
* Collectible companions
* Mistake notebook
* Screen-time schedules
* Content experimentation and feature flags
* Singapore and US standards mapping prototypes

### Later

* Multiple children
* Teacher and classroom accounts
* School reporting
* Collaborative family challenges
* Advanced speech recognition
* Creative projects
* Full K–12 content library
* Family subscription and school licensing

## 17. Immediate Next Steps

1. Obtain and organize the official Philippines Grade 3 MATATAG competency documents.
2. Convert English, Mathematics, and Science competencies into a structured skill map.
3. Select one unit from each subject for the pilot.
4. Interview and observe your daughter using two or three comparable learning apps.
5. Create a child-experience prototype and parent-dashboard prototype.
6. Test session length, reading level, reward preference, and navigation.
7. Define the first reusable activity templates.
8. Design the curriculum database and content workflow.
9. Build one production-quality animated lesson and one mini-game.
10. Use the pilot results to finalize the Grade 3 production schedule.

The most important first milestone is not the complete application. It is a working 10-minute learning loop that your daughter voluntarily completes, understands, and wants to repeat.
