# VoiceOSCore-Fix-OverlayNumberReset-260212-V1

## Summary
Fixed overlay badge numbers never resetting when switching apps or navigating within non-list apps. Numbers now restart from 1 on each app change and on screen changes in non-target apps.

## Bug Description
The number overlay system assigns incrementing badge numbers to interactive UI elements. Numbers are meant to reset to 1 when switching apps, but they never did. Observed behavior:
- Calculator: badges 133-158
- Home screen: 174-187
- Google app: 220-237
- Gmail: 325-329

All should start from 1.

## Root Cause
`DynamicCommandGenerator.processScreen()` never tracked package name changes. The `avidToNumber` map and `maxAssignedNumber` counter in `OverlayStateManager` were never reset because:
1. `DynamicCommandGenerator` had no `lastPackageName` field
2. `clearCache()` existed but was never called on app change
3. Screen hash changes within the same non-target app also didn't trigger numbering reset

## Fix Applied

### File: `apps/avanues/.../service/DynamicCommandGenerator.kt`

Added `lastPackageName` tracking with smart reset logic:

1. **App change detection**: Compare `packageName` against `lastPackageName`. On mismatch, reset `lastScreenHash` and call `OverlayStateManager.clearOverlayItems()` which resets both `avidToNumber` map and `maxAssignedNumber` counter.

2. **Same-app screen change**: If the screen hash changed but app didn't change, reset numbering only for non-target apps. Target apps (Gmail, WhatsApp) preserve numbers for scroll stability.

3. **`clearCache()` updated**: Also resets `lastPackageName` for consistency.

### Logic Matrix

| Scenario | isAppChange | isTargetApp | Reset? | Why |
|----------|-------------|-------------|--------|-----|
| Calculator -> Home | true | false | YES | Different app |
| Home -> Gmail | true | true | YES | Different app |
| Gmail inbox -> email detail | false | true | NO | List app, scroll preserved |
| Gmail inbox scroll | false (same hash) | true | SKIP | Same screen |
| Google home -> search results | false | false | YES | Non-list app, new screen |

## Lines Changed
- `DynamicCommandGenerator.kt`: +1 field, +12 lines logic, +1 line in clearCache()

## Verification
1. App switching: Numbers restart from 1 on each new app
2. Gmail scroll: Numbers persist when scrolling up/down
3. Same-app navigation (non-target): Numbers restart from 1 on new screen
4. Numbers ON mode survives app changes (mode persists, only numbers reset)

## Branch
`IosVoiceOS-Development`
