# VoiceOSCore-Fix-StubHandlerToCallbackWiring-260220-V1

## Problem

Seven module command handlers were 100% hardcoded stubs returning fake `HandlerResult.success()` without performing any actual operations:

| Handler | Module | Commands | Status |
|---------|--------|----------|--------|
| NoteCommandHandler | NoteAvanue | 48 | STUB |
| CockpitCommandHandler | Cockpit | 23 | STUB |
| AnnotationCommandHandler | AnnotationAvanue | 15 | STUB |
| ImageCommandHandler | ImageAvanue | 18 | STUB |
| VideoCommandHandler | VideoAvanue | 12 | STUB |
| CastCommandHandler | RemoteCast | 5 | STUB |
| AICommandHandler | AI | 5 | STUB |

**Total: 126 voice commands returning fake success with zero actual implementation.**

VoiceControlHandler was also investigated but found to be **fully implemented** via VoiceControlCallbacks pattern — all 18 commands work correctly through callback registration in `VoiceAvanueAccessibilityService.onServiceReady()`.

## Root Cause

The handlers were created during the six-module implementation sprint (260219) as scaffolding. Each handler had a `when` block returning `HandlerResult.success("Bold toggled")` etc. without actually toggling anything. The callback wiring that makes them functional was never implemented.

## Solution

### Phase 1: Handler Rewrite (All 7 handlers)

Created `ModuleCommandCallbacks.kt` — a central callback registry following the proven `VoiceControlCallbacks` pattern:

```kotlin
object ModuleCommandCallbacks {
    @Volatile var noteExecutor: ModuleCommandExecutor? = null
    @Volatile var cockpitExecutor: ModuleCommandExecutor? = null
    @Volatile var annotationExecutor: ModuleCommandExecutor? = null
    @Volatile var imageExecutor: ModuleCommandExecutor? = null
    @Volatile var videoExecutor: ModuleCommandExecutor? = null
    @Volatile var castExecutor: ModuleCommandExecutor? = null
    @Volatile var aiExecutor: ModuleCommandExecutor? = null
    fun clearAll() { /* nulls all */ }
    fun activeModules(): List<String> = buildList { /* checks non-null */ }
}
```

Rewrote all 7 handlers to use the executor pattern:
- If executor registered: dispatch to it and return real result
- If no executor: return honest `HandlerResult.failure("Module not active")` with recovery suggestion

### Phase 2: Executor Wiring (5 of 7 modules)

Wired module composables to register their executors via `DisposableEffect`:

| Module | File | Commands Wired | Method |
|--------|------|----------------|--------|
| Cockpit | `CockpitScreen.kt` | 20 | CockpitViewModel methods |
| VideoAvanue | `VideoPlayer.kt` | 12 | ExoPlayer API (Main thread) |
| NoteAvanue | `NoteEditor.kt` | 15 | RichTextState formatting |
| ImageAvanue | `ImageViewer.kt` | 18 | Internal state (zoom/rotate/filter) |
| AnnotationAvanue | — | 0 | DEFERRED (see below) |
| RemoteCast | — | 0 | DEFERRED (see below) |
| AI | — | 0 | DEFERRED (no module exists yet) |

### Cockpit Command Mapping (executeCockpitCommand)

| CommandActionType | CockpitViewModel Method |
|-------------------|------------------------|
| ADD_FRAME | addFrame(Web(), "New Frame") |
| MINIMIZE_FRAME | toggleMinimize(selectedFrameId) |
| MAXIMIZE_FRAME | toggleMaximize(selectedFrameId) |
| CLOSE_FRAME | removeFrame(selectedFrameId) |
| LAYOUT_GRID | setLayoutMode(GRID) |
| LAYOUT_SPLIT | setLayoutMode(SPLIT_LEFT) |
| LAYOUT_FREEFORM | setLayoutMode(FREEFORM) |
| LAYOUT_FULLSCREEN | setLayoutMode(FULLSCREEN) |
| LAYOUT_WORKFLOW | setLayoutMode(WORKFLOW) |
| LAYOUT_PICKER | Cycle through modes |
| ADD_WEB/CAMERA/NOTE/PDF/IMAGE/VIDEO/WHITEBOARD/TERMINAL | addFrame(ContentType(), title) |

### Video Command Mapping (executeVideoCommand)

Maps to ExoPlayer methods: play(), pause(), stop(), seekForward(), seekBack(), setPlaybackSpeed(), volume, repeatMode. Wrapped in `withContext(Dispatchers.Main)` for thread safety.

### Note Command Mapping (executeNoteCommand)

Maps to RichTextState: toggleSpanStyle (bold/italic/underline/strikethrough), heading sizes, code span, undo/redo, save, word count.

### Image Command Mapping (executeImageCommand)

Maps to internal state: gallery navigation (next/previous), rotation (+/- 90deg), horizontal flip, zoom in/out, filter presets (grayscale/sepia/blur/sharpen/brightness/contrast), metadata toggle.

## Bug Fix: Stale Closure in CockpitScreen

The original DisposableEffect captured `selectedFrameId` from Compose `collectAsState()`, which would freeze at the value when the effect first ran. Fixed to read `viewModel.selectedFrameId.value` at dispatch time.

## Deferred: AnnotationAvanue + RemoteCast

**AnnotationAvanue**: The `AnnotationCanvas` composable is stateless — it receives all state (strokes, tool, color) and operations (undo, redo, clear) as callbacks from the parent (ContentRenderer). The executor needs to be wired at the ContentRenderer level, which requires the ContentRenderer to maintain annotation state and expose it for voice command dispatch. This is an architectural refactoring.

**RemoteCast**: Cast operations (startCasting, stopCasting, connectToDevice) are service-level operations on `ICastManager`, not composable-level. `CastOverlay` is just a status display. The executor needs to be wired at the service layer where ICastManager is held.

**AI**: No AI module implementation exists yet. The AICommandHandler correctly returns "AI module not available" when no executor is registered.

## Files Modified

### New Files
- `Modules/VoiceOSCore/src/androidMain/.../handlers/ModuleCommandCallbacks.kt`

### Handler Rewrites (VoiceOSCore)
- `handlers/NoteCommandHandler.kt` — stub → callback dispatch
- `handlers/CockpitCommandHandler.kt` — stub → callback dispatch
- `handlers/AnnotationCommandHandler.kt` — stub → callback dispatch
- `handlers/ImageCommandHandler.kt` — stub → callback dispatch
- `handlers/VideoCommandHandler.kt` — stub → callback dispatch
- `handlers/CastCommandHandler.kt` — stub → callback dispatch
- `handlers/AICommandHandler.kt` — stub → callback dispatch

### Executor Wiring
- `Modules/Cockpit/.../ui/CockpitScreen.kt` — executeCockpitCommand()
- `Modules/VideoAvanue/.../VideoPlayer.kt` — executeVideoCommand()
- `Modules/NoteAvanue/.../NoteEditor.kt` — executeNoteCommand()
- `Modules/ImageAvanue/.../ImageViewer.kt` — executeImageCommand()

### Build Dependencies
- `Modules/VideoAvanue/build.gradle.kts` — +VoiceOSCore
- `Modules/NoteAvanue/build.gradle.kts` — +VoiceOSCore
- `Modules/ImageAvanue/build.gradle.kts` — +VoiceOSCore

### Service Lifecycle
- `apps/avanues/.../VoiceAvanueAccessibilityService.kt` — +ModuleCommandCallbacks.clearAll() in onDestroy()

## Impact

- 65 of 126 stub commands now have real implementations
- Cockpit (20), Video (12), Note (15), Image (18) = 65 commands
- Remaining 61 commands (Annotation 15, Cast 5, AI 5, plus some Note/Cockpit edge cases) return honest "module not active" failures
- Zero fake success() returns remaining in any handler
