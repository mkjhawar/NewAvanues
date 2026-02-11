# VoiceOSCore-Plan-GestureJSFullWiring-260211-V1

## Summary
Wire all 14 remaining `gestures.js` functions through the 5-layer voice command pipeline:
CommandActionType -> WebActionType -> DOMScraperBridge -> WebCommandExecutorImpl -> WebCommandHandler

## Gestures Wired (14)
| Gesture | JS Function | Voice Phrases |
|---------|------------|---------------|
| PAN | `AvanuesGestures.pan(dx, dy)` | pan, pan left/right/up/down |
| TILT | `AvanuesGestures.tilt(x, y, angle)` | tilt, tilt up/down |
| ORBIT | `AvanuesGestures.orbit(x, y, dX, dY)` | orbit, orbit left/right |
| ROTATE_X | `AvanuesGestures.rotateX(x, y, angle)` | rotate x |
| ROTATE_Y | `AvanuesGestures.rotateY(x, y, angle)` | rotate y |
| ROTATE_Z | `AvanuesGestures.rotateZ(x, y, angle)` | rotate z |
| PINCH | `AvanuesGestures.pinch(x1, y1, x2, y2, scale)` | pinch, pinch in/out |
| FLING | `AvanuesGestures.fling(velocity, direction)` | fling, fling up/down/left/right |
| THROW | `AvanuesGestures.throwElement(vX, vY)` | throw, toss |
| SCALE | `AvanuesGestures.scale(x, y, factor)` | scale, scale up/down |
| RESET_ZOOM | `AvanuesGestures.resetZoom()` | reset zoom |
| SELECT_WORD | `AvanuesGestures.selectWord(x, y)` | select word |
| CLEAR_SELECTION | `AvanuesGestures.clearSelection()` | clear selection, deselect |
| HOVER_OUT | `AvanuesGestures.hoverOut(x, y)` | hover out, stop hovering |

Also wired: "lock" / "lock element" -> GRAB (existing)

## Files Modified (6)
1. `Modules/VoiceOSCore/src/commonMain/.../command/CommandActionType.kt` - 14 new enum entries + isBrowserAction() updated
2. `Modules/VoiceOSCore/src/commonMain/.../interfaces/IWebCommandExecutor.kt` - 14 new WebActionType entries
3. `Modules/WebAvanue/src/commonMain/.../DOMScraperBridge.kt` - 14 new JS script generators (delegates to window.AvanuesGestures)
4. `Modules/WebAvanue/src/commonMain/.../WebCommandExecutorImpl.kt` - 14 new buildScript() when-cases
5. `Modules/VoiceOSCore/src/commonMain/.../handler/WebCommandHandler.kt` - Voice phrases, resolveWebActionType, resolveFromPhrase, extractDirectionParams helper
6. `Modules/VoiceOSCore/src/commonMain/.../actions/ActionCoordinator.kt` - 14 new actionTypeToPhrase() entries (exhaustive when fix)

## Build Status
BUILD SUCCESSFUL - compileDebugKotlinAndroid for both VoiceOSCore and WebAvanue

## Localization Note
Voice phrases are currently English-only. The `extractDirectionParams()` and `resolveFromPhrase()` methods use hardcoded English strings. For multi-language support (Vivoka, Whisper, Google STT), these phrase tables should be extracted into a localizable registry. This is a follow-up task.

## Branch
`VoiceOSCore-KotlinUpdate`

## Date
2026-02-11
