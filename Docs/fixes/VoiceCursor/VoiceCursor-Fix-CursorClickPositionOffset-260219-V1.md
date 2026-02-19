# VoiceCursor Fix: Cursor Click Position Offset

**Date:** 2026-02-19
**Module:** VoiceCursor
**Severity:** Critical (clicks land on wrong elements)
**Status:** Fixed

## Symptoms

- Cursor visually appears on one element (e.g., "8" on calculator)
- Click dispatches to a different element above the cursor (e.g., "()" button)
- Offset is constant: approximately `statusBarHeight` pixels upward
- On 3x density devices, ~72px offset; on 2x devices, ~48px

## Root Cause

**Missing `FLAG_LAYOUT_IN_SCREEN` on cursor overlay window.**

The cursor overlay in `CursorOverlayService.kt` used these flags:
```kotlin
FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE or FLAG_LAYOUT_NO_LIMITS
```

Without `FLAG_LAYOUT_IN_SCREEN`, the overlay window's coordinate origin (y=0) is
**below the status bar**. However, `AccessibilityService.dispatchGesture()` uses
**absolute screen coordinates** where y=0 is the physical top of the screen.

This creates a constant vertical offset:
- **Overlay visual position**: y measured from below status bar
- **Click dispatch position**: y measured from screen top
- **Result**: Click lands `statusBarHeight` pixels above where cursor appears

### Evidence

The `CommandOverlayService.kt` (numbers overlay) at line 215 ALREADY uses
`FLAG_LAYOUT_IN_SCREEN` correctly. The flag was missed when the VoiceCursor module
was created as a standalone service.

The legacy `OverlayService.kt` in `android/apps/VoiceOS/` also uses this flag
at lines 222, 326, and 375.

## Fix

Added `FLAG_LAYOUT_IN_SCREEN` to the cursor overlay window flags:

**File:** `Modules/VoiceCursor/src/androidMain/.../CursorOverlayService.kt`

```kotlin
FLAG_NOT_FOCUSABLE or
    FLAG_NOT_TOUCHABLE or
    FLAG_LAYOUT_NO_LIMITS or
    FLAG_LAYOUT_IN_SCREEN,  // <-- ADDED: align overlay coords with dispatchGesture coords
```

This makes the overlay's y=0 match the screen's y=0, eliminating the status bar offset.
Both visual cursor position and click dispatch now use the same coordinate space.

## Impact

- **Files changed:** 1 (`CursorOverlayService.kt`)
- **Risk:** Low — only changes coordinate origin, doesn't affect overlay sizing or behavior
- **Testing:** Verify on device that cursor click position matches visual position
