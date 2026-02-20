# AnnotationAvanue Full Implementation Plan

**Document:** AnnotationAvanue-Plan-FullImplementation-260219-V1.md
**Date:** 2026-02-19
**Branch:** Cockpit-Development
**Mode:** .yolo .cot

---

## Overview
- Fix P0 stroke persistence bug
- Add shape tools, color picker, Bezier smoothing
- Move drawing primitives to commonMain for KMP
- 15 voice commands in 5 locales
- Cockpit Whiteboard frame integration
- Target KMP Score: ~65%

## Phase 1: commonMain Models (Foundation)

### 1.1 Stroke.kt
- `Modules/AnnotationAvanue/src/commonMain/kotlin/com/augmentalis/annotationavanue/model/Stroke.kt`
- Data class: id (UUID string), points (List of Point), toolType (ShapeTool), color (Long), strokeWidth (Float), alpha (Float)
- Point data class: x (Float), y (Float), pressure (Float = 1f)
- Must be @Serializable (kotlinx.serialization) — this FIXES the persistence bug
- The old code lost data because it didn't serialize Bezier control points — we store RAW points and compute Bezier at render time

### 1.2 ShapeTool.kt
- `Modules/AnnotationAvanue/src/commonMain/kotlin/com/augmentalis/annotationavanue/model/ShapeTool.kt`
- Sealed class with objects: Pen, Highlighter, Eraser, Rectangle, Circle, Arrow, Line
- Highlighter has alpha = 0.4f default
- @Serializable with @SerialName for each

### 1.3 AnnotationColor.kt
- `Modules/AnnotationAvanue/src/commonMain/kotlin/com/augmentalis/annotationavanue/model/AnnotationColor.kt`
- 12 preset colors (black, white, red, blue, green, yellow, orange, purple, pink, brown, gray, cyan)
- Custom color: HSV wheel state (hue, saturation, value)
- @Serializable

### 1.4 AnnotationState.kt (REWRITE)
- `Modules/AnnotationAvanue/src/commonMain/kotlin/com/augmentalis/annotationavanue/model/AnnotationState.kt`
- Replace existing with: strokes (List of Stroke), currentTool (ShapeTool), currentColor (Long), strokeWidth (Float), undoStack (List of Stroke), redoStack (List of Stroke), isDrawing (Boolean)
- @Serializable

### 1.5 IAnnotationController.kt
- `Modules/AnnotationAvanue/src/commonMain/kotlin/com/augmentalis/annotationavanue/controller/IAnnotationController.kt`
- Interface: selectTool(tool: ShapeTool), setColor(color: Long), setStrokeWidth(width: Float), undo(), redo(), clear(), addStroke(stroke: Stroke), removeStroke(id: String), save(): ByteArray (PNG), getState(): AnnotationState

### 1.6 AnnotationSerializer.kt
- `Modules/AnnotationAvanue/src/commonMain/kotlin/com/augmentalis/annotationavanue/controller/AnnotationSerializer.kt`
- toJson(state: AnnotationState): String — using kotlinx.serialization
- fromJson(json: String): AnnotationState — THIS fixes the persistence bug
- Handles backwards compat with old format (try new first, fall back to old)

### 1.7 BezierSmoother.kt
- `Modules/AnnotationAvanue/src/commonMain/kotlin/com/augmentalis/annotationavanue/controller/BezierSmoother.kt`
- Takes raw List of Point → outputs smoothed List of Point with cubic Bezier control points
- Catmull-Rom to Bezier conversion algorithm
- Configurable tension parameter (0.3 default)
- Freehand strokes get smoothed; shape tools don't

## Phase 2: androidMain UI (REWRITE existing)

### 2.1 AnnotationCanvas.kt (REWRITE)
- `Modules/AnnotationAvanue/src/androidMain/kotlin/com/augmentalis/annotationavanue/ui/AnnotationCanvas.kt`
- Compose Canvas with:
  - pointerInput for drag gesture detection
  - Renders all strokes from state using drawPath
  - Applies BezierSmoother for freehand strokes
  - Shape tool rendering: drawRect, drawOval, drawLine for shape types
  - Eraser: removes strokes that intersect eraser path
  - Current drawing stroke rendered with animation
- Takes AnnotationState as parameter
- Reports stroke completion via onStrokeCompleted callback
- Semantics: contentDescription = "Voice: click canvas" (AVID)

### 2.2 AnnotationToolbar.kt (NEW)
- `Modules/AnnotationAvanue/src/androidMain/kotlin/com/augmentalis/annotationavanue/ui/AnnotationToolbar.kt`
- Horizontal scrollable row of tool icons
- Icons: Pen, Highlighter, Rectangle, Circle, Arrow, Line, Eraser
- Selected tool highlighted with AvanueTheme.colors.primary
- Stroke width slider (1-20dp)
- Color dots row (12 presets + custom color button)
- Undo/Redo buttons
- Clear button (with confirmation)
- AvanueUI v5.1 themed (AvanueTheme.colors.*, AvanueTheme.glass.*)
- ALL buttons have AVID semantics

### 2.3 ColorPickerSheet.kt (NEW)
- `Modules/AnnotationAvanue/src/androidMain/kotlin/com/augmentalis/annotationavanue/ui/ColorPickerSheet.kt`
- ModalBottomSheet (Material3)
- HSV color wheel + brightness slider
- 12 preset color chips
- Preview of selected color
- AvanueUI themed

### 2.4 SignatureCapture.kt (REWRITE)
- `Modules/AnnotationAvanue/src/androidMain/kotlin/com/augmentalis/annotationavanue/ui/SignatureCapture.kt`
- Now extends AnnotationCanvas with restricted tool set (pen only, black)
- Signature pad with clear and done buttons
- Used by Cockpit FrameContent.Signature

### 2.5 AndroidAnnotationController.kt (NEW)
- `Modules/AnnotationAvanue/src/androidMain/kotlin/com/augmentalis/annotationavanue/controller/AndroidAnnotationController.kt`
- Implements IAnnotationController
- save() renders Canvas to Bitmap → PNG ByteArray
- Manages state with MutableStateFlow

## Phase 3: Voice Commands & Handler

### 3.1 AnnotationCommandHandler.kt (NEW)
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/handlers/AnnotationCommandHandler.kt`
- Extends BaseHandler
- category = ActionCategory.ANNOTATION
- Handles all 15 ANNOTATION_* CommandActionTypes
- Routes to AnnotationController via service binding or event bus
- Pattern: same as NoteCommandHandler/CockpitCommandHandler

### 3.2 Register in AndroidHandlerFactory
- Add `AnnotationCommandHandler(service)` to createHandlers() list

## Phase 4: Cockpit Integration

### 4.1 ContentRenderer.kt
- Wire FrameContent.Whiteboard → AnnotationCanvas with state from strokesJson
- Wire FrameContent.Signature → SignatureCapture
- State changes serialize back to CockpitRepository

### 4.2 ContentAccent.kt
- Whiteboard accent: AvanueTheme.colors.tertiary (creative/drawing = tertiary)

## Phase 5: VOS Commands
- Add 15 annotation commands to en-US.app.vos
- Translate to es-ES, fr-FR, de-DE, hi-IN

## Dependencies
- kotlinx-serialization-json (existing)
- compose-multiplatform (existing)
- No new external libraries needed

## Estimated Files
- 7 new commonMain files
- 5 androidMain files (3 rewrites + 2 new)
- 1 handler in VoiceOSCore
- VOS files updates (5 locales)
- Total: ~13 new/modified Kotlin files + 5 VOS files
