# Module Readiness Audit for Avanues App Integration

**Date**: 2026-02-23
**Scope**: All 41 modules under `Modules/`, Avanues app integration, content module completeness, command dispatch architecture, deprecated modules, smart glasses UI design
**Branch**: VoiceOS-1M-SpeechEngine

---

## 1. Executive Summary

The NewAvanues repository contains **41 module directories** with **44 entries in settings.gradle.kts**. The consolidated Avanues app (`apps/avanues/`) depends on **23 modules**. Seven content modules (PDFAvanue, ImageAvanue, VideoAvanue, NoteAvanue, PhotoAvanue, RemoteCast, AnnotationAvanue) are integrated into the Cockpit multi-window compositor but range from 70-95% complete. Five modules referenced in settings.gradle.kts no longer exist on disk and should be cleaned up.

**Key Decisions Made**:
- Cockpit will become the unified Hub (replacing SpatialOrbitHub)
- Hybrid Adaptive display: PseudoSpatial on LCD, True Spatial on see-through glasses
- Activity-alias per content module for standalone launcher icons
- **Unified Command Architecture**: Actions module deprecated. Two clean systems — VoiceOSCore (`IHandler`) for screen control + IntentActions (`IIntentAction` in AI/NLU) for world interaction. Macros compose both. See Chapter 110.
- 94 redundant Actions handlers deleted (76 IPC stubs + 18 duplicates). 24 unique intent handlers migrated to `IIntentAction`.
- Developer Manual: Chapter 110 at `Docs/MasterDocs/VoiceOSCore/Developer-Manual-Chapter110-UnifiedCommandArchitecture.md`

---

## 2. Module Inventory by Tier

### Tier 1: Production-Grade (50+ .kt files)

| Module | .kt Files | Source Sets | Completeness | In App |
|--------|-----------|-------------|-------------|--------|
| VoiceOSCore | ~398 | common(206), android(159), ios(15), desktop(7) | 95% | Yes |
| WebAvanue | ~256 | common(165), android(54), ios(21), desktop(3) | 90% | Yes |
| Rpc | ~185 | common(177), android(4), desktop(2), ios(2) | 85% | No |
| PluginSystem | ~173 | common(98), android(36), ios(13), jvm(13) | 80% | No |
| Database | ~97 | common(91), android(1), desktop(1), ios(1) | 95% | Yes |
| HTTPAvanue | ~116 | common(77), android(8), desktop(8), ios(8), js(6) | 90% | No |
| SpeechRecognition | ~96 | common(16), android(46), desktop(9), ios(9) | 85% | Yes |
| DeviceManager | ~82 | common(7), android(66), desktop(2), ios(2) | 80% | Yes |
| AvanueUI | ~71 | common(66), android(3), desktop(1), ios(1) | 95% | Yes |
| AVU | ~63 | common(37), desktop(2), ios(2), android(1) | 90% | Yes |
| Cockpit | ~50 | common(38), android(6), desktop(3) | 85% | Yes |
| NetAvanue | ~52 | common(23), android(5), desktop(5), ios(5), js(5) | 75% | No |

### Tier 1 Sub-Modules (AI/)

| Module | .kt Files | In App |
|--------|-----------|--------|
| AI/LLM | ~89 | Yes |
| AI/NLU | ~89 | Yes |
| AI/RAG | ~75 | No |
| AI/Chat | ~61 | No |

### Tier 2: Medium (15-49 .kt files)

| Module | .kt Files | In App | Notes |
|--------|-----------|--------|-------|
| AvidCreator | ~39 | No | AVID authoring tools |
| Actions | ~41 | No | AVA command engine, Android-only |
| IPC | ~31 | No | AVU IPC protocol |
| Foundation | ~39 | Yes | Platform abstractions |
| NoteAvanue | ~16 | Yes | Rich voice-first editor |
| PhotoAvanue | ~15 | Yes | CameraX pro camera |
| Utilities | ~20 | No | Symmetric KMP utilities |
| VoiceKeyboard | ~22 | No | Legacy, not KMP |
| AVID | ~14 | Yes | Voice identifiers |
| RemoteCast | ~14 | Yes | Smart glasses transport |
| AVACode | ~15 | No | Kotlin DSL builders |
| Logging | ~15 | No | Full 5-platform logging |

### Tier 3: Small (1-14 .kt files)

| Module | .kt Files | In App | Notes |
|--------|-----------|--------|-------|
| LicenseValidation | ~14 | No | KMP license client |
| VoiceDataManager | ~11 | No | DB monitoring UI |
| VoiceCursor | ~10 | Yes | Cursor control |
| AnnotationAvanue | ~9 | Yes | Whiteboard/signature |
| VoiceIsolation | ~7 | No | Audio preprocessing |
| LicenseManager | ~7 | No | Legacy, not KMP |
| LicenseSDK | ~2 | No | commonMain only |
| Localization | ~6 | No | KMP i18n |
| VideoAvanue | ~5 | Yes | ExoPlayer video |
| PDFAvanue | ~5 | Yes | PdfRenderer viewer |
| ImageAvanue | ~4 | Yes | Coil image viewer |
| Gaze | ~4 | Yes | Eye tracking |
| VoiceAvanue | ~3 | Yes | Unified wrapper |

### Tier 4: Container / Non-Standard

| Module | Status |
|--------|--------|
| AI/ | Container — 7 sub-modules with own build files |
| AVA/ | Container — core/ (131 .kt, non-KMP), Overlay/ (legacy) |
| Voice/ | Container — WakeWord sub-module only |
| WebSocket/ | No build file — likely orphaned |
| Whisper/ | whisper.cpp C/C++ clone, CMake-based |

---

## 3. Content Module Completeness Assessment

### PDFAvanue (70%)

| Feature | Status |
|---------|--------|
| Page rendering (PdfRenderer) | Done |
| Page navigation (prev/next) | Done |
| Pinch-zoom + pan | Done |
| Search | **Stub** (returns emptyList) |
| Text extraction | **Stub** (returns empty string) |
| Continuous scroll mode | **Not implemented** (model defined) |
| Two-page spread mode | **Not implemented** (model defined) |
| Voice commands | Tier 2 AVID only |
| Desktop implementation | **None** |

### ImageAvanue (90%)

| Feature | Status |
|---------|--------|
| Image display (Coil) | Done |
| Gallery navigation | Done |
| Pinch-zoom + rotation | Done |
| 7 color filters (ColorMatrix) | Done |
| 2 filters (Blur/Sharpen) | **Not possible via ColorMatrix** |
| Metadata panel | Done |
| Voice commands | Tier 4 ModuleCallbacks (17 commands) |
| Desktop controller | Done (java.nio gallery, javax.imageio) |

### VideoAvanue (85%)

| Feature | Status |
|---------|--------|
| ExoPlayer playback | Done |
| MediaStore gallery | Done |
| Speed control (0.25-3x) | Done |
| Mute/loop/fullscreen | Done |
| Voice commands | Tier 4 ModuleCallbacks (12 commands) |
| Desktop playback engine | **State-only shell** (no rendering) |

### NoteAvanue (95%)

| Feature | Status |
|---------|--------|
| Rich text (compose-rich-editor) | Done |
| Markdown round-trip | Done |
| 12 formatting commands | Done |
| Voice dictation with format detection | Done |
| Voice-origin percentage tracking | Done |
| Audio recording (MediaRecorder) | Done |
| RAG semantic search | Done |
| Smart folders (dynamic SQL) | Done |
| Custom attachment URI scheme | Done |
| Undo/redo | **Stub** ("not available in current editor version") |
| Voice commands | Tier 4 ModuleCallbacks + Voice Router |
| Desktop | commonMain screens cross-platform |

### PhotoAvanue (95%)

| Feature | Status |
|---------|--------|
| CameraX photo capture | Done |
| CameraX video with pause/resume | Done |
| Pro mode (ISO/shutter/focus/WB/RAW) | Done (Camera2 interop) |
| Extensions (Bokeh/HDR/Night/FaceRetouch) | Done |
| GPS EXIF tagging | Done |
| Flash cycling | Done |
| Zoom/exposure controls | Done |
| Voice commands | Tier 2 AVID only |
| Desktop | **None** |

### RemoteCast (85%)

| Feature | Status |
|---------|--------|
| WebSocket transport (LAN) | Done |
| P2P DataChannel transport | Done |
| Android sender (MediaProjection) | Done |
| Android receiver (Flow<ByteArray>) | Done |
| Desktop sender (Robot screen capture) | Done |
| CAST binary protocol (20-byte header) | Done |
| Device discovery (mDNS) | **Deferred** (hardcoded placeholder) |
| Voice commands | **None** |

### AnnotationAvanue (90%)

| Feature | Status |
|---------|--------|
| 7 drawing tools | Done |
| Bezier smoothing (Catmull-Rom) | Done |
| Proximity-based eraser | Done |
| Undo/redo stack | Done |
| JSON serialization | Done |
| Signature capture pad | Done |
| Desktop PNG export (Graphics2D) | Done |
| Voice commands | Tier 2 AVID only |

---

## 4. Voice Command Tier Summary

| Tier | Description | Modules |
|------|-------------|---------|
| Tier 2 (AVID) | `contentDescription = "Voice: click X"` on buttons | PDFAvanue, PhotoAvanue, AnnotationAvanue |
| Tier 4 (ModuleCallbacks) | Full `ModuleCommandCallbacks.xxxExecutor` + HandlerResult | ImageAvanue, VideoAvanue, NoteAvanue |
| None | No voice support at all | RemoteCast |

**Gap**: PDFAvanue, PhotoAvanue, AnnotationAvanue, and RemoteCast need Tier 4 ModuleCommandCallbacks upgrade.

---

## 5. Command Dispatch Architecture: Actions vs VoiceOSCore

### Two Separate Pipelines

```
                    USER VOICE INPUT
                          |
           +--------------+--------------+
           |                             |
     [AVA Chat App]              [VoiceOS Service]
           |                             |
    NLU IntentClassifier           Speech Engine
           |                             |
     IntentRouter                  ActionCoordinator
      /         \                  /      |       \
AVA_LOCAL    VOICEOS-->IPC-->  Dynamic  Static   Module
  (Intent     (AIDL)         Commands  Commands  Callbacks
  Handlers)                  (Scraping) (VOS DB)  (Note,
  ~50 native                                      Cockpit,
  ~80 IPC stubs                                   Camera...)
```

### Key Differences

| Aspect | Actions Module | VoiceOSCore |
|--------|---------------|-------------|
| Entry point | AVA chat app | Accessibility service |
| Handler count | ~130 IntentActionHandlers | 17 IHandlers |
| Content modules | Not supported | Full ModuleCommandCallbacks |
| Dynamic commands | None | Screen scraping + CommandOrchestrator |
| Fuzzy matching | None (exact intent) | Jaccard + synonym + cache |
| NLU/LLM hooks | Consumer (classifies then routes) | Built-in (getNluSchema, getCommandsAsAvu) |
| KMP potential | Low (Android intents/AIDL) | Partial (commonMain framework) |

### Overlap

- ~80 Actions handlers are thin IPC stubs forwarding to VoiceOS (gesture, cursor, scroll, keyboard, drag)
- Media/system control duplicated in both (Actions via AudioManager, VoiceOSCore via MediaHandler)

### Recommendation

- **In-app/module commands**: Route through VoiceOSCore's ActionCoordinator
- **System intents**: Route through Actions (app launch, email, SMS, calendar, reminders)
- **AI-generated commands**: Feed NLU classification into VoiceOSCore's processCommand() using existing integration points

---

## 6. VoiceDataManager Assessment

**Purpose**: Database monitoring UI + recognition learning repositories

| Aspect | Assessment |
|--------|-----------|
| Size | 11 .kt files, ~2,500 lines |
| commonMain | Data models only (useful) |
| Android | Heavy (Activity, ViewModel, DatabaseManager facade) |
| Stubs | Import/export/cleanup/retention all stubbed |
| Theme compliance | **VIOLATES Rule #3** (uses MaterialTheme(darkColorScheme()) + hardcoded DataColors) |
| Overlap | Wraps Database module; DatabaseManager is redundant facade |
| KMP needed? | **No** — learning repos should migrate to Database commonMain; Activity UI absorbed into Developer Console |

---

## 7. Actions Module Assessment

**Purpose**: Voice command execution engine for AVA AI assistant (120+ handlers)

| Aspect | Assessment |
|--------|-----------|
| Size | 40+ files, ~6,000+ lines |
| commonMain | Empty (smoke test only) |
| Android | Extremely heavy (Intents, AIDL, AudioManager, BluetoothAdapter, system services) |
| Pure Kotlin | EntityExtractors, MathCalculator, ActionResult — could be commonMain |
| KMP needed? | **Partially** — extract pure-Kotlin framework to commonMain; handlers are permanently Android |
| Overlap | ~80 handlers are VoiceOS IPC stubs; media/system control duplicated with VoiceOSCore |

---

## 8. Deprecated Modules (settings.gradle.kts cleanup)

These 5 entries reference directories that no longer exist on disk:

| Module | Likely Status |
|--------|--------------|
| AvaMagic | Removed — functionality absorbed elsewhere |
| CommandManager | Merged into VoiceOSCore's command pipeline |
| Translation | Merged into Localization module |
| VoiceOS | Renamed to VoiceOS-Legacy |
| VUID | Functionality moved to AVID module |

**Action**: Remove from settings.gradle.kts with deprecation comment.

---

## 9. Platform Coverage Matrix

| Module | common | android | ios | desktop | js |
|--------|--------|---------|-----|---------|-----|
| VoiceOSCore | 206 | 159 | 15 | 7 | - |
| WebAvanue | 165 | 54 | 21 | 3 | - |
| Rpc | 177 | 4 | 2 | 2 | - |
| PluginSystem | 98 | 36 | 13 | - | - |
| Database | 91 | 1 | 1 | 1 | - |
| HTTPAvanue | 77 | 8 | 8 | 8 | 6 |
| AvanueUI | 66 | 3 | 1 | 1 | - |
| NetAvanue | 23 | 5 | 5 | 5 | 5 |
| Cockpit | 38 | 6 | - | 3 | - |
| Foundation | 20 | 1 | 5 | 5 | 1 |

---

## 10. Integration Priority Recommendations

### Immediate (this plan)
1. Cockpit Dashboard as unified Hub
2. Content module voice command upgrades (Tier 2 → Tier 4)
3. PDFAvanue search + continuous scroll
4. Activity-alias standalone launcher icons
5. PseudoSpatial + True Spatial display adaptation

### Next Priority
1. **PluginSystem** (173 kt) — user-extensibility via .avp plugins
2. **AI:Chat + AI:Memory + AI:ALC** — "Ask Avanue" AI assistant in hub
3. **Voice:WakeWord + VoiceIsolation** — hands-free activation + noise cancellation
4. **NetAvanue** — P2P pairing prerequisite for RemoteCast glasses workflow
5. **AvidCreator** — user AVID authoring for custom apps

### Defer
- Actions KMP migration (low ROI, handlers are Android-specific)
- VoiceDataManager (absorb into Developer Console)
- Rpc (separate transport, VoiceOSCore uses own RPC)
- LicenseManager/LicenseSDK (needs KMP migration first)
- VoiceKeyboard (legacy, needs full rewrite)

---

## 11. Smart Glasses UI Design Summary

Full specification to be written at: `docs/plans/Cockpit/Cockpit-Spec-SmartGlassesUIDesign-260223-V1.md`

### Key Design Decisions

| Aspect | LCD (Phone/Tablet) | See-Through (Vuzix M4000) | Opaque Glass |
|--------|-------------------|--------------------------|--------------|
| Rendering | PseudoSpatial (parallax, 3D cards) | True Spatial (head-tracked) | Spatial (dark theme) |
| Color scheme | Appearance-aware (light/dark) | colorsXR (additive) | Standard dark |
| Input | Touch + voice | Voice-only | Voice + touchpad |
| Layout | 3-col grid, carousel | Paginated voice-list (5 items max) | Paginated list |
| Animation | Scale + fade (400ms) | Fade only (150-250ms) | Fade only |
| Frame chrome | Full (traffic lights, resize) | Minimal (thin title strip) | Minimal |
| Text | Standard sizing | 18px min (30.5 PPD formula) | 16px min |

### New Enum: GlassDisplayMode
```
FLAT_SCREEN → appearance-aware colors
SEE_THROUGH → colorsXR (additive-optimized)
OPAQUE_GLASS → standard dark theme
```

### PseudoSpatial 4-Layer Parallax
| Layer | Gyro Multiplier | Max Offset |
|-------|----------------|------------|
| Background | 0.3x | 12dp |
| Mid-ground | 0.6x | 8dp |
| Foreground | 1.0x | 4dp |
| HUD overlay | 0x (locked) | 0dp |

---

*Review by: Manoj Jhawar | 2026-02-23*
