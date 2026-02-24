# Cockpit-Plan-CommandBarModuleWiring-260224-V1

**Module:** Cockpit + NoteAvanue
**Branch:** VoiceOS-1M-SpeechEngine
**Status:** Implemented

---

## Problem

After the deferred review fixes (commits `e5ca62e4a`..`c0efa242f`), Fix 3.5 bypassed NOTE_ACTIONS and CAMERA_ACTIONS in `forContentType()` because the modules lacked exposed APIs. The CommandBar had 20 ContentAction values but only Web/PDF/Image/Video were wired. Note, Camera, and Whiteboard content types fell through to generic FRAME_ACTIONS (minimize/maximize/close).

## Solution

Wire NOTE_ACTIONS, CAMERA_ACTIONS, and new WHITEBOARD_ACTIONS so the CommandBar dispatches real operations to NoteAvanue, PhotoAvanue, and AnnotationAvanue via the existing `ModuleCommandCallbacks` bridge pattern.

### Architecture

```
CommandBar chip tap → ContentAction enum
  → CockpitViewModel._contentAction.tryEmit()
  → ContentRenderer LaunchedEffect collects
  → Maps ContentAction → CommandActionType
  → Calls ModuleCommandCallbacks.{module}Executor?.invoke(actionType, emptyMap())
```

Zero changes to module composable signatures.

## Changes

### Phase 1: NoteAvanue Undo Manager

| File | Change |
|------|--------|
| `RichTextUndoManager.kt` (NEW) | Snapshot-based undo/redo for compose-rich-editor RichTextState. Captures markdown before each formatting op, restores on undo/redo. Max 50 snapshots. |
| `NoteEditor.kt` | Wire undoManager into DisposableEffect, pass to executeNoteCommand, add captureSnapshot() before all formatting handlers, implement NOTE_UNDO/NOTE_REDO via manager. |

### Phase 2: ContentAction & CommandBarState

| File | Change |
|------|--------|
| `CommandBar.kt` | Expand ContentAction enum with NOTE_BOLD/ITALIC/UNDERLINE/STRIKETHROUGH/SAVE + WB_PEN/HIGHLIGHTER/ERASER/UNDO/REDO/CLEAR. Update NOTE_ACTIONS chips (7 chips). Add WHITEBOARD_ACTIONS chips (6 chips). |
| `CommandBarState.kt` | Add WHITEBOARD_ACTIONS enum value. Update parent/CONTENT_SPECIFIC_STATES. Restore forContentType() routing for note/camera/whiteboard. |

### Phase 3: ContentRenderer Wiring

| File | Change |
|------|--------|
| `ContentRenderer.kt` | Add LaunchedEffect blocks for Note/Camera/Whiteboard content types. Each translates ContentAction → CommandActionType → ModuleCommandCallbacks executor. |

### Phase 4: Documentation

| File | Change |
|------|--------|
| Chapter 97 | Updated CommandBarState tree, forContentType routing table, content type checklist, updates section. |
| This plan doc | Created. |

## Key Decisions

1. **Snapshot-based undo** — compose-rich-editor RC13 doesn't expose undo/redo API, so RichTextUndoManager captures markdown snapshots. Tradeoff: loses cursor position on undo, but preserves content correctly.
2. **ModuleCommandCallbacks bridge** — reuses existing voice command infrastructure rather than creating new callback interfaces. ContentRenderer translates ContentAction → CommandActionType in a `when` expression.
3. **No composable signature changes** — all wiring happens through the already-registered DisposableEffect executors, so NoteEditor/CameraPreview/AnnotationCanvas signatures are untouched.
