# VoiceOSCore Fix: Overlay Mode Bypass and Missing Re-scan

**Date:** 2026-02-19
**Module:** VoiceOSCore + Avanues App
**Severity:** Medium (voice commands for show/hide numbers unreliable)
**Status:** Fixed

## Symptoms

1. "Hide numbers" / "Show numbers" voice commands execute inconsistently
2. After "show numbers", badges may not appear until user touches the screen
3. For list-based apps (Gmail, WhatsApp), "hide numbers" may not take effect

## Root Causes

### Issue A: Target App Mode Bypass

**File:** `apps/avanues/.../DynamicCommandGenerator.kt` (line 119-130)

`processScreen()` had two code paths:
- **Target apps** (`isTargetApp=true`): Always generated overlay items, ignoring `NumbersOverlayMode`
- **Non-target apps**: Correctly checked mode before generating items

When mode was OFF, target apps continued generating and storing items via
`updateNumberedOverlayItems()`. While `showNumbersOverlayComputed` was correctly
set to `false` (hiding the overlay), the unnecessary item generation was wasteful
and the mode bypass was architecturally wrong.

### Issue B: Empty Items After Mode Change

When switching OFF→ON:
1. `setNumbersOverlayMode(ON)` fires
2. `showNumbersOverlayComputed` becomes `true` (overlay should show)
3. But `numberedOverlayItems` is EMPTY (cleared when mode was OFF)
4. Overlay renders but shows nothing — user perceives "command didn't work"
5. Only after the next accessibility event does `processScreen()` generate items

### Issue C: No Re-scan Trigger

The `onSetNumbersMode` callback changed mode but didn't invalidate the screen hash
or trigger a re-scan. The next `processScreen()` could skip entirely if the screen
hash matched (content unchanged since last scan).

## Fixes

### Fix A: Unified Mode Check (DynamicCommandGenerator.kt)

Moved mode check to run BEFORE the target/non-target split:

```kotlin
val mode = OverlayStateManager.numbersOverlayMode.value
val overlayItems = if (mode == NumbersOverlayMode.OFF) {
    emptyList()  // Mode OFF: never generate, regardless of app type
} else if (isTargetApp) {
    OverlayItemGenerator.generateForListApp(...)
} else {
    OverlayItemGenerator.generateForAllClickable(...)
}
```

### Fix B + C: Invalidate Hash + Trigger Re-scan (VoiceAvanueAccessibilityService.kt)

After setting mode, the callback now:
1. Invalidates the screen hash via `dynamicCommandGenerator?.invalidateScreenHash()`
2. Launches immediate `refreshOverlayBadges()` on the service scope

This ensures badges appear/disappear immediately after the voice command,
without waiting for the next accessibility event.

## Impact

- **Files changed:** 2 (`DynamicCommandGenerator.kt`, `VoiceAvanueAccessibilityService.kt`)
- **Risk:** Low — mode check is additive; re-scan is the same path used by scroll handling
- **Testing:** Test show/hide/auto numbers on both target apps (Gmail) and non-target apps (Calculator)
