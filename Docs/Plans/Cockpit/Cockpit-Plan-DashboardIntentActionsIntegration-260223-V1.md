# Cockpit Dashboard: Unified Hub + IntentActions + Content Module Integration Plan

## Context

The Avanues app has 7 content modules (PDFAvanue, ImageAvanue, VideoAvanue, NoteAvanue, PhotoAvanue, RemoteCast, AnnotationAvanue) integrated as Cockpit frame types but ranging 70-95% complete. The current launcher (SpatialOrbitHub) is a separate radial UI in the app layer. The Actions module duplicates 64% of VoiceOSCore's functionality via IPC stubs and redundant handlers.

### Goals
1. **Unify command dispatch** — Migrate Actions' unique 24 intent handlers into a new `IIntentAction` system in AI/NLU; delete 94 redundant handlers
2. **Make Cockpit the unified Hub** — Dashboard layout mode replaces SpatialOrbitHub
3. **Hybrid Adaptive Display** — PseudoSpatial on LCD, True Spatial on see-through glasses
4. **Standalone launcher icons** — Activity-alias per content module (7 new aliases)
5. **Upgrade content module voice commands** — All modules to Tier 4 ModuleCommandCallbacks
6. **Clean up deprecated modules** — Remove AvaMagic, CommandManager, Translation, VoiceOS, VUID

## Decisions Made

- **Command Architecture**: Two clean systems — VoiceOSCore (screen control: gestures, system, modules) + IntentActions (world interaction: launch apps, communicate, navigate). Macros compose both.
- **Display Mode**: Hybrid Adaptive — PseudoSpatial for phones/tablets, True Spatial for see-through glasses
- **Hub Strategy**: Cockpit IS the Hub — new `LayoutMode.DASHBOARD`
- **Launcher Icons**: Activity-alias per module (7 new)
- **Actions Module**: Deprecated — 24 unique handlers → IIntentAction in AI/NLU; 94 redundant handlers deleted

---

## Phase 0: Actions → IntentActions Migration (Foundation)

### Architectural Overview

```
ANY INPUT (voice, AI chat, macro step)
         │
         ▼
  NLU Classifier (intent + type)
         │
    ┌────┴────┐
    ▼         ▼
VoiceOSCore  IntentActions
(IHandler)   (IIntentAction)
    │              │
    ├─Gestures     ├─Launch App
    ├─System ctrl  ├─Send Email/SMS
    ├─Media ctrl   ├─Make Call
    ├─Module CB    ├─Navigate Map
    ├─Text manip   ├─Set Alarm/Timer
    ├─Screen read  ├─Web Search
    │              ├─Math Calc
    │              ├─Calendar/Reminder
    │              │
    └──── MACROS ──┘ (compose both)
```

### 0A. Rename Actions → IntentActions + Create IIntentAction Framework

**Rename**: `Modules/Actions/` → `Modules/IntentActions/`
**Update**: `settings.gradle.kts` — change `:Modules:Actions` → `:Modules:IntentActions`
**Update**: `Modules/IntentActions/build.gradle.kts` — update namespace to `com.augmentalis.intentactions`

**New files in `Modules/IntentActions/src/commonMain/kotlin/com/augmentalis/intentactions/`**:

| File | Content |
|------|---------|
| `IIntentAction.kt` | Interface: `intentId`, `category: IntentCategory`, `requiredEntities`, `suspend execute(context, entities): IntentResult` |
| `IntentCategory.kt` | Enum: COMMUNICATION, NAVIGATION, PRODUCTIVITY, SEARCH, MEDIA_LAUNCH, SYSTEM_SETTINGS |
| `IntentResult.kt` | Sealed class: Success, NeedsMoreInfo(missingEntity, prompt), Failed(reason) |
| `IntentActionRegistry.kt` | Registry: `register()`, `execute()`, `findByIntent()`, `getAll()` — queryable by macros |
| `ExtractedEntities.kt` | Data class holding extracted entities (query, url, phone, recipient, message, location, time, duration) |

### 0B. Migrate Entity Extractors to IntentActions commonMain

**Move from** `Modules/IntentActions/src/androidMain/.../extractors/` **to** `Modules/IntentActions/src/commonMain/.../extractors/`:

| File | Notes |
|------|-------|
| `EntityExtractor.kt` | Interface — already pure Kotlin |
| `QueryEntityExtractor.kt` | Regex-based — pure Kotlin |
| `URLEntityExtractor.kt` | Regex-based — pure Kotlin |
| `PhoneNumberEntityExtractor.kt` | Regex-based — pure Kotlin |
| `RecipientEntityExtractor.kt` | Regex-based — pure Kotlin |
| `MessageEntityExtractor.kt` | Regex-based — pure Kotlin |

### 0C. Migrate MathCalculator to IntentActions commonMain

**Move from** `Modules/IntentActions/src/androidMain/.../math/` **to** `Modules/IntentActions/src/commonMain/.../math/`:

| File | Notes |
|------|-------|
| `MathCalculator.kt` | Pure Kotlin math parser (arithmetic, trig, logs, percentages) |
| `CalculationResult.kt` | Data class — pure Kotlin |

### 0D. Refactor 26 Intent Handlers to IIntentAction Interface

**Refactor in-place in `Modules/IntentActions/src/androidMain/kotlin/com/augmentalis/intentactions/actions/`**:

| Category | IntentActions (refactored from Actions handlers) | Count |
|----------|--------------------------------------------------|-------|
| COMMUNICATION | SendEmailAction, SendTextAction, MakeCallAction | 3 |
| NAVIGATION | GetDirectionsAction, FindNearbyAction, ShowTrafficAction, ShareLocationAction, SaveLocationAction | 5 |
| PRODUCTIVITY | SetAlarmAction, SetTimerAction, CreateReminderAction, CreateCalendarEventAction, AddTodoAction, CreateNoteAction, CheckCalendarAction, GetTimeAction | 8 |
| SEARCH | WebSearchAction (DuckDuckGo), NavigateURLAction, CalculateAction, GetWeatherAction | 4 |
| MEDIA_LAUNCH | PlayVideoAction (YouTube), ResumeMusicAction, OpenBrowserAction, OpenAppAction | 4 |
| SYSTEM_SETTINGS | OpenSettingsAction (+ subsection variants: security, connection, sound, display, about) | 2 |
| **Total** | | **26** |

Each `IIntentAction` impl uses `PlatformContext` (expect/actual for Activity) to launch Android Intents.

### 0E. Wire NLU → VoiceOSCore Unified Dispatch

**Modified file**: `Modules/AI/NLU/src/commonMain/.../IntentClassifier.kt`
- Add `classifyAndRoute(utterance)` that returns either `VoiceCommand(phrase)` or `IntentAction(intentId, entities)`
- VoiceCommand → feed to `VoiceOSCore.processCommand()`
- IntentAction → feed to `IntentActionRegistry.execute()`

**Modified file**: `Modules/AI/NLU/src/androidMain/.../AndroidIntentClassifier.kt`
- Wire `IntentActionRegistry` initialization with all 26 actions
- Expose `IntentActionRegistry` for macro executor injection

### 0F. MacroStep Model (VoiceOSCore commonMain)

**New file**: `Modules/VoiceOSCore/src/commonMain/.../macro/MacroStep.kt`

```kotlin
sealed class MacroStep {
    data class VoiceAction(val command: String) : MacroStep()
    data class Intent(val intentId: String, val entities: Map<String, String>) : MacroStep()
    data class Delay(val ms: Long) : MacroStep()
    data class Conditional(val check: String, val thenSteps: List<MacroStep>) : MacroStep()
}
```

### 0G. Deprecate Actions Module

**Modified file**: `Modules/Actions/build.gradle.kts`
- Add `@Deprecated` notice in README or top-level file
- Do NOT delete yet — keep for reference during migration verification

**Modified file**: `settings.gradle.kts`
- Comment out `include(":Modules:Actions")` with deprecation note

### What Gets Deleted (94 handlers)

| Category | Count | Reason |
|----------|-------|--------|
| VoiceOS IPC routing stubs | 76 | VoiceOSCore handles directly |
| Duplicate system controls (WiFi, BT, brightness, flashlight, lock, screenshot) | 8 | VoiceOSCore ScreenHandler |
| Duplicate media controls (play, pause, next, previous, volume) | 6 | VoiceOSCore MediaHandler |
| Duplicate navigation (home, back, recent apps, notifications) | 4 | VoiceOSCore SystemHandler |
| **Total redundant** | **94** | |

---

## Phase 1: Review Report + Deprecated Cleanup + Developer Manual

### 1A. Review Report (DONE)
- **Output**: `docs/reviews/Cockpit-Review-ModuleReadinessAudit-260223-V1.md` ✓

### 1B. Developer Manual Chapter 110
- **Output**: `Docs/MasterDocs/VoiceOSCore/Developer-Manual-Chapter110-UnifiedCommandArchitecture.md`
- Content: VoiceOSCore IHandler vs IntentAction IIntentAction, dispatch flow, macro composition, migration from Actions module, entity extraction, interface contracts

### 1C. Clean Up settings.gradle.kts
- **File**: `settings.gradle.kts`
- Remove 5 deprecated entries: AvaMagic, CommandManager, Translation, VoiceOS, VUID
- Comment out Actions module with migration note

### 1D. Update Review Report
- Add Actions → IntentActions architectural decision to existing review

---

## Phase 2: Cockpit Dashboard Foundation (commonMain)

### 2A. Add LayoutMode.DASHBOARD
- **File**: `Modules/Cockpit/src/commonMain/.../model/LayoutMode.kt`
- Add `DASHBOARD` to the enum
- Update `LayoutModeResolver`: DASHBOARD default when no session is active
- DASHBOARD available on ALL DisplayProfiles

### 2B. GlassDisplayMode Enum (New)
- **File**: `Modules/AvanueUI/src/commonMain/.../display/GlassDisplayMode.kt`
- `enum class GlassDisplayMode { FLAT_SCREEN, SEE_THROUGH, OPAQUE_GLASS }`
- SEE_THROUGH → `colorsXR`, OPAQUE_GLASS → dark theme, FLAT_SCREEN → appearance-aware
- Integrate with `DisplayProfile` and `SmartGlassDetection`

### 2C. Dashboard Data Model
- **File**: `Modules/Cockpit/src/commonMain/.../model/DashboardState.kt`
- `DashboardState`: recentSessions, availableModules, activeSession, quickActions, displayMode
- `DashboardModule`: id, displayName, subtitle, icon, contentType, accentColor
- `DashboardModuleRegistry`: static registry (migrated from app-level HubModuleRegistry)

### 2D. CockpitViewModel Dashboard Extensions
- **File**: `Modules/Cockpit/src/commonMain/.../CockpitViewModel.kt`
- Add `dashboardState: StateFlow<DashboardState>`
- Add `launchModule(moduleId)`, `resumeSession(sessionId)`, `returnToDashboard()`

### 2E. DashboardLayout Composable (commonMain)
- **File**: `Modules/Cockpit/src/commonMain/.../ui/layouts/DashboardLayout.kt`
- Dispatched by `LayoutEngine` for `LayoutMode.DASHBOARD`
- Adaptive: Phone 3-col grid, Tablet list-detail, Glass paginated voice-list

---

## Phase 3: PseudoSpatial Rendering (commonMain + androidMain)

### 3A. PseudoSpatialController (commonMain)
- **File**: `Modules/Cockpit/src/commonMain/.../spatial/PseudoSpatialController.kt`
- 4-layer parallax: Background(0.3x/12dp), Mid(0.6x/8dp), Foreground(1.0x/4dp), HUD(0x/0dp)
- Gyroscope input via existing `ISpatialOrientationSource`
- 3D card transforms: active 1.0 scale, adjacent 0.85 + 12deg rotationY

### 3B. PseudoSpatialCanvas (commonMain)
- **File**: `Modules/Cockpit/src/commonMain/.../ui/PseudoSpatialCanvas.kt`
- Parallax layers, `graphicsLayer { rotationY, scaleX, scaleY }`
- Scanline grid overlay (48dp), corner accent borders (HUD aesthetic)

### 3C. Glass-Specific Adaptations
- **File**: `Modules/Cockpit/src/commonMain/.../ui/GlassFrameChrome.kt`
- Minimal chrome, text halo (3dp blur, 60% black), peek panels (24dp edge strips)

### 3D. Display Mode Integration in LayoutEngine
- **File**: `Modules/Cockpit/src/commonMain/.../ui/LayoutEngine.kt`
- FLAT_SCREEN → PseudoSpatialCanvas, SEE_THROUGH → SpatialCanvas + colorsXR, OPAQUE_GLASS → SpatialCanvas + dark

---

## Phase 4: Activity-Alias Launcher Icons

### 4A-4B. New AvanueMode Variants + determineLaunchMode()
- **File**: `apps/avanues/.../MainActivity.kt`
- Add: PDF, IMAGE, VIDEO, NOTE, PHOTO, CAST, DRAW to enum
- Map 7 alias class names → new modes → Cockpit with pre-loaded frame

### 4C. AndroidManifest.xml — 7 New Aliases
- **File**: `apps/avanues/src/main/AndroidManifest.xml`
- `.PDFAvanueAlias`, `.ImageAvanueAlias`, `.VideoAvanueAlias`, `.NoteAvanueAlias`, `.PhotoAvanueAlias`, `.CastAvanueAlias`, `.DrawAvanueAlias`

### 4D. NavHost Route Handling
- **File**: `apps/avanues/.../MainActivity.kt`
- Routes `cockpit/pdf`, `cockpit/image`, etc. → CockpitScreen with `launchModule()`

### 4E. Deprecate SpatialOrbitHub
- Mark `@Deprecated`, update HUB route → Cockpit DASHBOARD mode

---

## Phase 5: Content Module Voice Command Upgrades

| Module | File | New Executor | Commands |
|--------|------|-------------|----------|
| PDFAvanue | `PdfViewer.kt` | `pdfExecutor` | PDF_NEXT_PAGE, PDF_PREVIOUS_PAGE, PDF_ZOOM_IN/OUT, PDF_GO_TO_PAGE, PDF_SEARCH |
| PhotoAvanue | `CameraPreview.kt` | `cameraExecutor` | CAMERA_CAPTURE, CAMERA_RECORD, CAMERA_STOP, CAMERA_SWITCH_LENS, CAMERA_FLASH, CAMERA_ZOOM |
| AnnotationAvanue | `AnnotationCanvas.kt` | `annotationExecutor` | ANNOTATION_PEN, ANNOTATION_HIGHLIGHTER, ANNOTATION_ERASER, ANNOTATION_UNDO/REDO, ANNOTATION_CLEAR |
| RemoteCast | `CastOverlay.kt` | `castExecutor` | CAST_START, CAST_STOP, CAST_QUALITY, CAST_CONNECT, CAST_DISCONNECT |

---

## Phase 6: PDFAvanue Gap Fill

### 6A. PDF Search — `AndroidPdfEngine.kt`
### 6B. Continuous Scroll — `PdfViewer.kt` (LazyColumn mode)

---

## Phase 7: Smart Glasses UI Design Spec

- **Output**: `docs/plans/Cockpit/Cockpit-Spec-SmartGlassesUIDesign-260223-V1.md`

---

## Files Summary

### New Files (Phase 0)
| File | Phase |
|------|-------|
| `Modules/AI/NLU/src/commonMain/.../intent/IIntentAction.kt` | 0A |
| `Modules/AI/NLU/src/commonMain/.../intent/IntentCategory.kt` | 0A |
| `Modules/AI/NLU/src/commonMain/.../intent/IntentResult.kt` | 0A |
| `Modules/AI/NLU/src/commonMain/.../intent/IntentActionRegistry.kt` | 0A |
| `Modules/AI/NLU/src/commonMain/.../intent/ExtractedEntities.kt` | 0A |
| `Modules/AI/NLU/src/commonMain/.../extractors/*.kt` (6 files) | 0B |
| `Modules/AI/NLU/src/commonMain/.../math/MathCalculator.kt` | 0C |
| `Modules/AI/NLU/src/commonMain/.../math/CalculationResult.kt` | 0C |
| `Modules/AI/NLU/src/androidMain/.../intent/actions/*.kt` (26 files) | 0D |
| `Modules/VoiceOSCore/src/commonMain/.../macro/MacroStep.kt` | 0F |

### New Files (Phases 1-7)
| File | Phase |
|------|-------|
| `Docs/MasterDocs/VoiceOSCore/Developer-Manual-Chapter110-UnifiedCommandArchitecture.md` | 1B |
| `Modules/AvanueUI/src/commonMain/.../display/GlassDisplayMode.kt` | 2B |
| `Modules/Cockpit/src/commonMain/.../model/DashboardState.kt` | 2C |
| `Modules/Cockpit/src/commonMain/.../ui/layouts/DashboardLayout.kt` | 2E |
| `Modules/Cockpit/src/commonMain/.../spatial/PseudoSpatialController.kt` | 3A |
| `Modules/Cockpit/src/commonMain/.../ui/PseudoSpatialCanvas.kt` | 3B |
| `Modules/Cockpit/src/commonMain/.../ui/GlassFrameChrome.kt` | 3C |
| `docs/plans/Cockpit/Cockpit-Spec-SmartGlassesUIDesign-260223-V1.md` | 7 |

### Modified Files
| File | Phase | Change |
|------|-------|--------|
| `Modules/AI/NLU/src/commonMain/.../IntentClassifier.kt` | 0E | Add classifyAndRoute() |
| `Modules/AI/NLU/src/androidMain/.../AndroidIntentClassifier.kt` | 0E | Wire IntentActionRegistry |
| `Modules/Actions/build.gradle.kts` | 0G | Deprecation notice |
| `settings.gradle.kts` | 0G, 1C | Comment out Actions + 5 deprecated |
| `Modules/Cockpit/.../model/LayoutMode.kt` | 2A | Add DASHBOARD |
| `Modules/Cockpit/.../ui/LayoutModeResolver.kt` | 2A | DASHBOARD default |
| `Modules/Cockpit/.../CockpitViewModel.kt` | 2D | Dashboard state + launch/resume |
| `Modules/Cockpit/.../ui/LayoutEngine.kt` | 2E, 3D | DASHBOARD dispatch + display wrapping |
| `apps/avanues/.../MainActivity.kt` | 4A-4E | AvanueMode variants, routes, hub deprecation |
| `apps/avanues/.../AndroidManifest.xml` | 4C | 7 activity-alias entries |
| `Modules/PDFAvanue/.../PdfViewer.kt` | 5A, 6B | Voice commands + continuous scroll |
| `Modules/PDFAvanue/.../AndroidPdfEngine.kt` | 6A | Search implementation |
| `Modules/PhotoAvanue/.../CameraPreview.kt` | 5B | Voice commands |
| `Modules/AnnotationAvanue/.../AnnotationCanvas.kt` | 5C | Voice commands |
| `Modules/RemoteCast/.../CastOverlay.kt` | 5D | AVID + voice commands |
| `apps/avanues/.../ui/hub/SpatialOrbitHub.kt` | 4E | @Deprecated |
| `apps/avanues/.../ui/hub/HubDashboardScreen.kt` | 4E | @Deprecated |
| `apps/avanues/.../ui/hub/HubModule.kt` | 4E | @Deprecated, migrated |

---

## Verification

1. **Build**: `./gradlew :apps:avanues:assembleDebug` — must compile clean
2. **IntentAction test**: NLU classifyAndRoute("send email to john") → IntentAction.SendEmail with extracted recipient
3. **VoiceOSCore test**: Say "scroll down" → ActionCoordinator → AndroidGestureHandler (no IPC detour)
4. **Macro test**: Create macro with VoiceAction + IntentAction steps → both execute in sequence
5. **Dashboard test**: Launch Avanues → Cockpit Dashboard (not SpatialOrbitHub)
6. **Module launch**: Tap tile → Cockpit session with correct frame
7. **Alias launch**: Tap standalone icon → Cockpit with pre-loaded frame
8. **Voice tier test**: Say "next page" in PDF → ModuleCommandCallbacks
9. **Glass test**: Verify GlassDisplayMode detection and XR palette
10. **PseudoSpatial**: Verify parallax responds to gyroscope

---

## Commit Strategy

| Commit | Scope |
|--------|-------|
| Phase 0A-0C | IntentAction framework + entity extractors + math (commonMain) |
| Phase 0D-0E | 26 intent handlers + NLU wiring (androidMain) |
| Phase 0F-0G | MacroStep model + Actions deprecation |
| Phase 1 | Review report + Chapter 110 + settings cleanup |
| Phase 2 | Cockpit Dashboard foundation (model + viewmodel + layout) |
| Phase 3 | PseudoSpatial + Glass rendering |
| Phase 4 | Activity-alias + hub deprecation |
| Phase 5 | Content module voice upgrades |
| Phase 6 | PDFAvanue gaps |
| Phase 7 | Smart glasses design spec |

**This task would benefit from 1M context** — touches 15+ files across 8 modules. Recommend `--model claude-opus-4-6[1m]`.
