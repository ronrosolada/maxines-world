# Child surfaces UI design polish — pass 1

Applied the approved Impeccable Operate/quieter discipline and native gpt-taste constraints. This pass preserves behavior, public APIs, semantics, test identifiers, color values, and reduced-motion behavior.

## Per-surface findings

- **QuickBitsScreen:** Removed emoji category chrome, broadened the dark navigation chapter, and normalized the count badge radius. Deferred video playback and pager behavior as explicitly out of scope.
- **LessonCompletionScreen:** Tightened the result hero, normalized the reward surface, increased chapter separation before actions, and retained the existing reduced-motion gates. Deferred reward policy and navigation behavior.
- **LessonFeedbackLayout:** Increased the reserved sticky-feedback breathing room to the spacing system’s 120dp composition without changing the layout API. Deferred feedback behavior.
- **MediaAssessmentQuiz:** Documented the stable state/presentation boundary so the quieter UI remains independent of assessment logic. Deferred all scoring/state transitions because this file intentionally contains no Compose surface.
- **VideoStep:** Increased workspace rhythm and normalized the primary step surface while leaving player and playback logic untouched.
- **WildlifeFieldGuideScreen:** Removed decorative sparkle chrome, strengthened biome chapter spacing, and normalized hero radius/grid gaps. Deferred badge data and collection rules.
- **TreatShopScreen:** Replaced the Unicode back glyph with the Material back icon, widened gutters, normalized card shape, and increased list rhythm. Deferred purchase behavior and catalog content.
- **BadgeComponents:** Increased collectible-card breathing room, normalized detail radius, and restrained detail-title scale. Deferred badge art and discovery rules.
- **BadgeFlipCard:** Increased internal spacing, restrained title hierarchy, and raised tiny helper copy to a child-readable size. Existing reduced-motion-safe flip behavior remains unchanged.
- **DailyTrailScreen:** Widened chapter gutters, normalized the progress hero radius, and increased row separation. Deferred trail calculations and navigation.
- **RewardHubScreen:** Removed the infinite decorative lock-ring pulse, capped the one-shot reveal at 400ms, widened the empty-state chapter, and used the typography hierarchy for the message. Reduced motion still snaps immediately.
- **MiniGameLibraryScreen:** Increased responsive grid gutters and inter-card rhythm while preserving full-span hero/header items and game launch behavior.
- **MiniGameWebScreen:** Removed emoji-as-design chrome, constrained the title to two lines, and improved timer spacing. Deferred WebView security, timer, and result logic.
- **PawprintParkourScreen:** Normalized wide-screen chapter spacing, panel radii, and panel padding while preserving controls, labels, game logic, and reduced-motion state.
- **KittenMatchScreen:** Increased page/grid rhythm, minimum tile footprint, and normalized modal radius while preserving card semantics and all game state.
- **CatCafeDashScreen:** Normalized wide-screen chapter spacing, primary panel radii/padding, and food-grid rhythm while preserving order, audio, timer, and exit behavior.

## Deferred across the pass

- No new design tokens or color values: the later design-system pass owns token expansion.
- No content/string-resource, asset, ViewModel, DAO, database, navigation, build, or playback changes.
- No Gradle build was run in this environment, per task constraint; repository Python verification gates are the local acceptance checks.
