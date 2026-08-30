# UI design polish v3 — pass 2: caregiver, auth, and design system

## ParentDashboardScreen
- Applied the impeccable **Operate** lens: a calm Cream canvas, 24dp adult dashboard gutters, and 24dp chapter rhythm make dense controls and reports easier to scan without changing their order or behavior.
- Retained the teal app bar and moved its foreground to the named `White` token. The teal/white pair was already established by the design system.
- Removed the decorative sparkle from the sticker-award toast; the confirmation remains explicit in words.

## CaregiverPhrasesScreen
- Strengthened title/body hierarchy using theme typography, a restrained 16dp parent-card radius, 24dp internal padding, and 16dp group spacing.
- Preserved search, category filters, phrase ordering, and the practiced-today switch semantics.

## ParentGateScreen
- Kept the calm lockout pattern, 48dp bypass action, PIN keypad behavior, mental-math recovery, and all accessibility descriptions.
- Added a warm Cream canvas and chapter-level vertical breathing room while retaining the existing soft gold pause treatment.

## AuthScreen
- Preserved the IME-safe shared viewport, bring-into-view behavior, PIN dots, digit roles/descriptions, and all public signatures.
- Replaced the harsh red lockout card and lock emoji with the same warm gold pause treatment used by ParentGate. The heading remains plain-language and two-line safe.

## MaxinesComponents
- Added shared 4dp-grid spacing tiers: 8, 12, 16, 24, 32, and 40dp.
- Added a restrained radius family: 12, 14, 16, and 20dp.
- Existing component signatures and reduced-motion branches remain unchanged.

## MaxinesVillageBuilding
- Replaced the Unicode star and all-caps meta-label with the Material star icon plus “Today's focus.”
- Changed gold-ribbon foreground to `OnGold` for the established accessible token pair.
- Preserved the public composable/data signatures, content description, progress states, and `LocalAnimationsDisabled` snap behavior.

## DelightMotion
- Added bounded one-shot timing constants and specs: PawPop 240ms, CheckPop 180ms, TrophyEntrance 360ms, RewardReveal 420ms, and ReducedMotionCrossfade 120ms.
- Kept existing names and behavior available; `QuickPopMs` remains 420ms through an alias.

## Deferrals
- No color aliases were needed; `Color.kt` remains untouched and every existing hex value is unchanged.
- Full Android compilation/emulator visual evidence is outside the local verification contract for this bounded NAS pass; repository Python gates and diff checks are recorded with the commit handoff.
