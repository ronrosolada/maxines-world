# Maxine’s World Village Home v1.7 Fail-safe Handoff

## What failed in the latest build

The screenshot renders the overlay layer on a blank cream canvas. The village background and all landmarks are absent, and the obsolete ornate bamboo frames returned. This is a layer/resource integration regression, not a new art-direction issue.

## What this package changes

This package uses a single 1536×1024 scene plate containing:

* The complete illustrated Filipino village.
* Six subject buildings.
* Endemic animals.
* Empty woven-paper signboards in final positions.
* Empty profile, quest, reward, and navigation surfaces.

Compose adds only native text, progress, semantics, and click handlers. DeepSeek must not position or assemble any frame artwork.

## Install

From the extracted package:

```bash
python3 tools/install_into_repo.py /absolute/path/to/maxines-world
```

The installer:

* Finds the current `VillageHomeScreen.kt`.
* Copies `VillageHomeV17.kt` beside it.
* Rewrites the placeholder package declaration automatically.
* Copies the required background, vectors, and strings.
* Fails loudly if the repository layout is unexpected.

## Only manual integration step

In the existing child-home route, call:

```kotlin
VillageHomeV17Screen(
    state = mappedV17State,
    onDestinationClick = ::openSubject,
    onQuestClick = ::openDailyQuest,
    onHomeClick = ::openHome,
    onProgressClick = ::openProgress,
    onAvatarsClick = ::openAvatars,
    onParentsClick = ::openParentGate,
)
```

Do not copy any old `BambooPlaqueSurface`, rail, corner, NinePatch, or white-card code into the new screen.

## Mandatory compile sequence

```bash
cd android
./gradlew :feature-child-home:compileDebugKotlin
./gradlew :feature-child-home:testDebugUnitTest
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Automatic rejection

Reject the implementation if:

* The village is blank or cream.
* `mw_village_scene_v17` is not visible.
* Any old ornate bamboo frame is rendered over the scene plate.
* Duplicate frame assets surround a sign already present in the scene.
* A destination click area is not aligned to its sign.
* Any enabled callback is empty.
* DeepSeek returns without a 1353×949 screenshot.
