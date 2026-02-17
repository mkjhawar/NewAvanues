# VoiceCursor-Fix-CursorOverlayTouchBlocking-260213-V1

## Summary
Fixed cursor overlay blocking all screen touches, cursor appearing at (0,0) top-left corner,
and voice movement commands having no effect on the visual cursor.

## Root Causes (3)

### RC1: Full-Screen Overlay Blocks Touches (CRITICAL)
- **Location**: `VoiceCursor/overlay/CursorOverlayService.kt:131-139`
- **Problem**: Overlay used `MATCH_PARENT x MATCH_PARENT` covering entire screen.
  Android 12+ "untrusted touch" security blocks touches passing through
  `TYPE_APPLICATION_OVERLAY` windows, making the entire screen unresponsive.
- **Fix**: Changed to small, positioned overlay window that covers only the cursor
  dot + dwell ring area. Uses `updateViewLayout()` to track cursor position.
  Overlay sizing computed by KMP `CursorOverlaySpec` (shared with iOS/Desktop).

### RC2: Cursor Renders at (0,0) — Top-Left Corner
- **Location**: `VoiceCursor/core/CursorController.kt:58-64`
- **Problem**: `CursorState` defaults to `CursorPosition.Zero` (0f, 0f).
  `initialize()` set screen bounds but never centered the cursor.
- **Fix**: `initialize()` now auto-centers cursor at `(screenWidth/2, screenHeight/2)`
  with `isVisible = true`. KMP change — benefits all platforms.

### RC3: Voice Movement Commands Have No Effect
- **Location**: `VoiceOSCore/actions/CursorActions.kt`
- **Problem**: `CursorActions.initialize()` was **never called** anywhere.
  `controller` field was always null. Voice commands ("cursor up/down/left/right")
  silently failed. Additionally, `ClickDispatcher` was never wired.
- **Fix**: `AndroidCursorHandler.showCursor()` now wires `CursorActions` to
  the overlay service's `CursorController` + `AndroidGestureDispatcher` after
  service start. Also wires `ClickDispatcher` for dwell-click dispatch.

## Files Modified

### KMP (commonMain — shared across Android/iOS/Desktop)
| File | Change |
|------|--------|
| `VoiceCursor/core/CursorController.kt` | `initialize()` auto-centers cursor |
| `VoiceCursor/core/CursorTypes.kt` | Added `CursorOverlaySpec` — shared overlay sizing/positioning |

### Android (androidMain)
| File | Change |
|------|--------|
| `VoiceCursor/overlay/CursorOverlayService.kt` | Small moveable overlay, density scaling, getCursorController() |
| `VoiceOSCore/AndroidCursorHandler.kt` | Wires CursorActions + ClickDispatcher after service start |

### iOS (iosMain — new)
| File | Change |
|------|--------|
| `VoiceCursor/filter/CursorFilterPlatform.kt` | `currentTimeMillis()` actual via kotlinx.datetime |
| `VoiceCursor/build.gradle.kts` | Added iOS targets (iosX64, iosArm64, iosSimulatorArm64) |

## Architecture Changes

### Before (Broken)
```
CursorOverlayService creates MATCH_PARENT overlay
  -> Covers entire screen -> blocks all touches (Android 12+)
  -> Cursor at (0,0) -> barely visible in corner
  -> CursorActions.controller = null -> voice commands dead
```

### After (Fixed)
```
CursorOverlayService creates small overlay (cursor_radius * ~4 px)
  -> Only covers cursor area -> no touch blocking
  -> updateViewLayout() repositions on every state change
  -> CursorController.initialize() auto-centers at screen center
  -> AndroidCursorHandler wires CursorActions to service controller
  -> Voice movement/click commands work through the same controller
```

### KMP Overlay Spec (Shared Logic)
```
CursorOverlaySpec.fromConfig(config, density) -> sizePx, centerOffsetPx
CursorOverlaySpec.overlayOrigin(cursorX, cursorY) -> top-left position
```
Both Android and future iOS implementations use this for consistent sizing.

## Visual Changes
- Cursor now density-scaled (`cursorRadius * displayDensity`)
  - Default 12dp x 3.0 density = 36px (was 12px raw)
- Added outer glow ring (semi-transparent black) for visibility on any background
- Dwell ring gap and stroke also density-scaled

## Testing Notes
1. "Show cursor" -> cursor appears at screen center, clearly visible
2. Touch anywhere on screen -> touches pass through normally
3. "Cursor up/down/left/right" -> cursor moves in direction
4. "Cursor click" / "click here" -> click dispatched at cursor position
5. "Hide cursor" -> overlay removed, service stopped

## Branch
`IosVoiceOS-Development`

## Related
- Chapter 94 (4-Tier Voice Enablement)
- `memory/voiceoscore-features.md` (Cursor Voice section)
