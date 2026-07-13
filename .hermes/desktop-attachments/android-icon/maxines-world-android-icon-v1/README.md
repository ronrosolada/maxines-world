# Maxine's World Android Launcher Icon v1

## Approved design

Maxine is the focal character. The launcher icon uses bubblegum-pink glasses, backpack straps, and necktie, with a bamboo-and-rattan frame, teal Filipino village setting, and gold paw-star badge.

## Copy into the app

Copy the contents of `android/app/src/main/res/` into the repository's `android/app/src/main/res/`, preserving folders.

Ensure `AndroidManifest.xml` references:

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round" />
```

## Resource strategy

- Android 8–12 uses the v26 adaptive icon.
- Android 13+ uses the v33 adaptive icon with a monochrome themed-icon layer.
- Pre-Android 8 launchers use density-specific legacy PNGs.
- The adaptive background is a solid Village Teal `#087F83` VectorDrawable.
- The foreground is a transparent 432×432 PNG containing the illustrated bamboo medallion.
- The monochrome layer is a simple cat, glasses, backpack, and tie mark; it does not flatten the full-color illustration.

## Required verification

1. Build with `./gradlew :app:processDebugResources`.
2. Install on API 25, 26, and 33+ test devices or emulators.
3. Check circle, squircle, rounded-square, and teardrop launcher masks.
4. Enable Android 13 themed icons and verify the monochrome mark.
5. Confirm the pink glasses, backpack, and tie remain recognizable at 48px.
6. Confirm no bamboo, ears, glasses, or paw-star are awkwardly clipped.
7. Confirm there is no white halo or checkerboard transparency.

## Notes

The generated source is intentionally text-free. Do not add the app name inside the icon. Do not reintroduce the old generic orange-cat-and-hills launcher art.
