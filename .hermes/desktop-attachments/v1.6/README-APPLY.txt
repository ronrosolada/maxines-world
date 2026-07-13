MAXINE’S WORLD V1.6 TARGET-LOCKED UI KIT

APPLY ORDER
1. Copy drawable-nodpi PNGs unchanged.
2. Copy VillageChromeV16.kt. If the repository package differs, change only the package line.
3. Render the existing village background before VillageChromeV16.
4. Pass the current profile, quest, reward, and navigation content slots.
5. Connect every destination ID to its matching route.
6. Remove the v1.5 white rounded cards and plain white bottom bar.
7. Compare the result with reference/EXPECTED_TARGET.png.
8. Do not redesign the supplied surfaces.

BUILD
./gradlew :feature-child-home:compileDebugKotlin
./gradlew :feature-child-home:testDebugUnitTest
./gradlew assembleDebug
