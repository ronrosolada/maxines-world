# Maxine’s World — Hermes Handoff Package

## Start here

Use `maxines-world-implementation-handoff.md` as the implementation source of truth.
Use `design.md` as the visual design, component, character, animation, accessibility, and asset-production source of truth.
Use `grade-3-learning-app-plan.md` for the broader product rationale and rollout plan.
The `assets/graphics` directory contains the seven original concept images referenced by local paths in the handoff.

## Hermes kickoff prompt

Implement Maxine’s World using `maxines-world-implementation-handoff.md` as the source of truth.

Start with one production-quality vertical slice:

1. Parent login and one child profile
2. Child village home
3. Offline lesson engine
4. One pilot lesson for each of five subjects
5. Progress and mastery storage
6. Parent dashboard
7. Basic learning-linked rewards
8. Automated tests

Use Kotlin, Jetpack Compose, Room, WorkManager, and modular architecture. Keep curriculum and lessons in versioned external JSON rather than hard-coding them.

Before coding, produce architecture decisions, repository structure, implementation milestones, and unresolved risks. Then implement milestone by milestone, running the Android build and tests after every milestone. Do not claim curriculum certification or invent DepEd competency codes.

## Required local tools

* Android Studio
* Android SDK
* JDK 17 or the version required by the selected Android Gradle Plugin
* Git

## Important constraints

* The PNG files are concept references; recreate controls and text natively.
* Validate all curriculum mappings with qualified educators before release.
* Preserve child privacy: no ads, public profiles, public chat, or behavioral tracking.
* The first milestone is a reliable offline ten-minute learning session.
