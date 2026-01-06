# VoiceOSCore vs VoiceOSCoreNG Feature Gap Analysis

**Date:** 2026-01-06
**Version:** V3 (Updated)
**Author:** VOS4 Development Team
**Status:** Analysis Complete + Major Implementation Progress

## Recent Implementation Progress (2026-01-06)

### Completed - Phases 1-5 (Previous Session)
1. **Database Integration** - VoiceOSCoreNG now wired to VoiceOS/core/database SQLDelight
   - `SQLDelightCommandRepositoryAdapter` - bridges QuantizedCommand ↔ GeneratedCommandDTO
   - `SQLDelightVuidRepositoryAdapter` - bridges VuidEntry ↔ ScrapedElementDTO
   - `RepositoryProvider.configureWithSQLDelight()` for production configuration

2. **UniversalRPC Integration** (NOT AIDL - migrated to gRPC)
   - `ExplorationBridge` connects to UniversalRPC's ExplorationServiceClient
   - JIT learning, element capture, exploration session management
   - Auto-saves learned elements to database

3. **Vivoka KMP Interface**
   - `IVivokaEngine` interface with wake word, models, offline support
   - Android implementation bridges to SpeechRecognition library
   - iOS/Desktop stub implementations
   - Integrated into `SpeechEngineFactoryProvider`

### Completed - Phases 6-9 (Current Session)

4. **Handler System Infrastructure** (Phase 6)
   - `ActionCategory.kt` - 11 action categories with priority order
   - `IHandler.kt` - KMP handler interface with suspend execute()
   - `HandlerResult.kt` - Rich result type (Success, Failure, RequiresInput, InProgress)
   - `HandlerRegistry.kt` - Priority-based handler lookup with coroutine safety

5. **Core Handlers** (Phase 7) - 4 handlers ported
   - `NavigationHandler.kt` - scroll, swipe, page navigation (commonMain)
   - `UIHandler.kt` - click, tap, press, toggle, expand/collapse (commonMain)
   - `InputHandler.kt` - type, delete, clipboard, search (commonMain)
   - `SystemHandler.kt` - back, home, recents, notifications (commonMain)
   - Android executors: `AndroidNavigationExecutor`, `AndroidUIExecutor`, `AndroidInputExecutor`, `AndroidSystemExecutor`
   - iOS/Desktop stub executors

6. **Manager System** (Phase 8)
   - `ActionCoordinator.kt` - Command routing, metrics, voice interpretation
   - `MetricsCollector.kt` - Performance tracking with per-command and global metrics
   - `ServiceState.kt` - State machine for service lifecycle

7. **Integration Layer** (Phase 9)
   - `VoiceOSCoreNG.kt` - Main facade with Builder pattern
   - `AndroidHandlerFactory.kt` - Creates handlers with Android executors
   - `IOSHandlerFactory.kt` / `DesktopHandlerFactory.kt` - Platform stubs

---

## Executive Summary

VoiceOSCoreNG (~130 files) is a KMP rewrite of VoiceOSCore (381 files) now covering **~50%** of the original functionality. Remaining gaps:

1. **Overlay System** - Complete overlay management missing
2. **Cursor System** - Spatial cursor and focus management missing
3. **Additional Handlers** - 15 more handlers to port (Media, Bluetooth, etc.)
4. **iOS/Desktop Executors** - Stub implementations need real platform code

---

## Feature Comparison Matrix (Updated 2026-01-06)

| Category | VoiceOSCore | VoiceOSCoreNG | Gap % | Status |
|----------|-------------|---------------|-------|--------|
| Total Files | 381 | ~130 | 66% | 🔄 |
| Speech Engines | 5 (real) | 2 (real) + 5 (stub) | 60% | ✅ Vivoka added |
| Handlers | 19 | 4 (P0 complete) | 79% | ✅ Core handlers |
| Managers | 9 | 3 | 67% | ✅ Coordinator, Metrics, State |
| Overlay Components | 15+ | 0 | 100% | ❌ Missing |
| Exploration Engine | Full | Full | 0% | ✅ ExplorationBridge |
| Database Integration | SQLDelight | SQLDelight | 0% | ✅ Adapters complete |
| Safety/Detection | Full | Partial | 40% | 🔄 |
| Command Generation | Full | Full | 0% | ✅ |
| VUID System | Full | Full | 0% | ✅ Adapters complete |

---

## Detailed Gap Analysis

### 1. Speech Engine Integration

#### VoiceOSCore Speech Engines (5 Real Implementations)

| Engine | File | Lines | Status |
|--------|------|-------|--------|
| **Vivoka** | `speech/VivokaEngine.kt` | 1,057 | ✅ Production |
| Google Cloud | `speech/GoogleEngineAdapter.kt` | ~300 | ✅ Production |
| Vosk (Offline) | `speech/VoskEngineAdapter.kt` | ~400 | ✅ Production |
| Azure | `speech/AzureEngineAdapter.kt` | ~350 | ✅ Production |
| Android STT | `speech/SpeechEngineManager.kt` | ~800 | ✅ Production |
| Whisper | `speech/WhisperEngineAdapter.kt.todo` | - | ❌ Disabled |

#### VoiceOSCoreNG Speech Engines

| Engine | File | Status |
|--------|------|--------|
| Android STT | `androidMain/speech/AndroidSTTEngineImpl.kt` | ✅ Real Implementation |
| Vivoka | enum only | ❌ **CRITICAL: No Implementation** |
| Google Cloud | enum only | ❌ Stub |
| Vosk | enum only | ❌ Stub |
| Azure | enum only | ❌ Stub |
| Apple Speech | enum only | ❌ Stub |
| Whisper | enum only | ❌ Stub |

#### Vivoka Integration Gap

**VoiceOSCore Vivoka Components (16 files in SpeechRecognition library):**

```
Modules/VoiceOS/libraries/SpeechRecognition/
├── VivokaEngine.kt (1,057 lines) - Main engine
├── VivokaConfig.kt - Configuration
├── VivokaModelManager.kt - Model management
├── VivokaAudioProcessor.kt - Audio pipeline
├── VivokaNLUBridge.kt - NLU integration
├── VivokaCommandMatcher.kt - Command matching
├── VivokaWakeWordDetector.kt - Wake word
├── VivokaStreamHandler.kt - Streaming
├── VivokaErrorHandler.kt - Error handling
├── VivokaMetrics.kt - Analytics
├── VivokaPermissionHelper.kt - Permissions
├── VivokaStateManager.kt - State machine
├── VivokaRecoveryManager.kt - Recovery
├── VivokaCacheManager.kt - Caching
├── VivokaDebugHelper.kt - Debugging
└── VivokaTestHelper.kt - Testing
```

**VoiceOSCoreNG Vivoka: NONE**

**Recommendation:** Create KMP wrapper for Vivoka:
```
VoiceOSCoreNG/
├── commonMain/speech/vivoka/
│   ├── IVivokaEngine.kt (interface)
│   ├── VivokaConfig.kt (shared config)
│   ├── VivokaState.kt (shared state model)
│   └── VivokaResult.kt (shared result model)
└── androidMain/speech/vivoka/
    └── VivokaEngineImpl.kt (actual implementation)
```

---

### 2. Handler System

#### VoiceOSCore Handlers (19)

| Handler | Purpose | Priority |
|---------|---------|----------|
| `NavigationHandler.kt` | App navigation | P0 |
| `ClickHandler.kt` | Tap/click actions | P0 |
| `ScrollHandler.kt` | Scroll management | P0 |
| `TextHandler.kt` | Text input/editing | P0 |
| `SystemHandler.kt` | System actions | P1 |
| `MediaHandler.kt` | Media controls | P1 |
| `GestureHandler.kt` | Complex gestures | P1 |
| `SelectionHandler.kt` | Element selection | P1 |
| `FocusHandler.kt` | Focus management | P1 |
| `CursorHandler.kt` | Spatial cursor | P2 |
| `NumberHandler.kt` | Number overlay | P2 |
| `ContextMenuHandler.kt` | Context menus | P2 |
| `NotificationHandler.kt` | Notifications | P2 |
| `ClipboardHandler.kt` | Clipboard | P2 |
| `PhoneHandler.kt` | Phone calls | P3 |
| `CameraHandler.kt` | Camera controls | P3 |
| `AccessibilityHandler.kt` | A11y features | P3 |
| `SettingsHandler.kt` | Settings access | P3 |
| `DebugHandler.kt` | Debug tools | P3 |

#### VoiceOSCoreNG Handlers: **NONE**

**Recommendation:** Port handlers incrementally by priority:

```kotlin
// commonMain/handlers/IHandler.kt
interface IHandler {
    val name: String
    val supportedCommands: Set<String>
    suspend fun handle(command: QuantizedCommand): ActionResult
    fun canHandle(command: QuantizedCommand): Boolean
}

// commonMain/handlers/HandlerRegistry.kt
object HandlerRegistry {
    private val handlers = mutableMapOf<String, IHandler>()
    fun register(handler: IHandler)
    fun findHandler(command: QuantizedCommand): IHandler?
}
```

---

### 3. Overlay System

#### VoiceOSCore Overlays (15+ components)

| Component | Purpose | Status in NG |
|-----------|---------|--------------|
| `BaseOverlay.kt` | Base class | ❌ Missing |
| `NumberOverlayRenderer.kt` | Number badges | ❌ Missing |
| `NumberOverlayManager.kt` | Overlay management | ❌ Missing |
| `ConfidenceOverlay.kt` | Confidence display | ❌ Missing |
| `CommandStatusOverlay.kt` | Command feedback | ❌ Missing |
| `ContextMenuOverlay.kt` | Context menus | ❌ Missing |
| `FocusIndicator.kt` | Focus visualization | ❌ Missing |
| `RenameHintOverlay.kt` | Rename suggestions | ❌ Missing |
| `NumberedSelectionOverlay.kt` | Number selection | ❌ Missing |

#### VoiceOSCoreNG Overlays: **NONE**

**Recommendation:** Create KMP overlay abstraction:

```kotlin
// commonMain/overlay/IOverlay.kt
interface IOverlay {
    val isVisible: Boolean
    fun show()
    fun hide()
    fun update(data: OverlayData)
}

// commonMain/overlay/OverlayManager.kt
expect class OverlayManager {
    fun showNumberOverlay(elements: List<ElementInfo>)
    fun hideAll()
}

// androidMain/overlay/AndroidOverlayManager.kt
actual class OverlayManager(context: Context) {
    // Uses WindowManager for Android overlays
}
```

---

### 4. Manager System

#### VoiceOSCore Managers (9)

| Manager | Purpose | Status in NG |
|---------|---------|--------------|
| `SpeechEngineManager.kt` | Engine lifecycle | Partial |
| `CommandManager.kt` | Command orchestration | ❌ Missing |
| `StateManager.kt` | App state | ❌ Missing |
| `OverlayManager.kt` | Overlay lifecycle | ❌ Missing |
| `CursorManager.kt` | Spatial cursor | ❌ Missing |
| `FocusManager.kt` | Focus tracking | ❌ Missing |
| `NavigationManager.kt` | Navigation state | ❌ Missing |
| `MetricsManager.kt` | Analytics | ❌ Missing |
| `RecoveryManager.kt` | Error recovery | ❌ Missing |

#### VoiceOSCoreNG Managers: **NONE**

---

### 5. LearnApp System

#### VoiceOSCore LearnApp (33 directories)

Major components:
- `exploration/` - ExplorationEngine, ExplorationState
- `learning/` - JIT learning, command generation
- `database/` - Persistence layer
- `consent/` - User consent management
- `safety/` - SafetyManager, DangerousElementDetector
- `detection/` - CrossPlatformDetector
- `config/` - LearnAppConfig
- `metrics/` - Analytics
- `integration/` - Service integration

#### VoiceOSCoreNG LearnApp Status

| Component | Status |
|-----------|--------|
| `features/LearnAppConfig.kt` | ✅ Partial |
| `features/LearnAppDevToggle.kt` | ✅ Complete |
| `features/FeatureGate.kt` | ✅ Complete |
| `safety/DangerousElementDetector.kt` | ✅ Complete |
| `fingerprinting/ScreenFingerprinter.kt` | ✅ Complete |
| `command/CommandGenerator.kt` | ✅ Partial |
| `command/CommandRegistry.kt` | ✅ Partial |
| `extraction/ElementExtractor.kt` | ✅ Partial |
| Exploration Engine | ❌ Missing |
| JIT Learning | ❌ Missing |
| Consent Management | ❌ Missing |

---

### 6. Database Integration

#### VoiceOSCore Database

- **Location:** `Modules/VoiceOS/core/database/`
- **Technology:** SQLDelight
- **Repositories:**
  - `IScreenContextRepository` → `SQLDelightScreenContextRepository`
  - `IGeneratedCommandRepository` → `SQLDelightGeneratedCommandRepository`
  - `IVUIDRepository` → `SQLDelightVUIDRepository`

#### VoiceOSCoreNG Database

- **Current:** In-memory repositories only
- **Missing:** SQLDelight integration

**Recommendation:** Create repository abstractions that can be injected:

```kotlin
// commonMain/repository/RepositoryProvider.kt (exists)
// Need to create SQLDelight implementations for production
```

---

## Priority Migration Plan

### Phase 1: Critical Path (P0) - 2 weeks

1. **Vivoka Engine Integration**
   - Create `IVivokaEngine` interface in commonMain
   - Port `VivokaEngineImpl` to androidMain
   - Add wake word detection

2. **Core Handlers**
   - `NavigationHandler`
   - `ClickHandler`
   - `ScrollHandler`
   - `TextHandler`

3. **Handler Registry**
   - `IHandler` interface
   - `HandlerRegistry` with priority dispatch

### Phase 2: Overlay System (P1) - 2 weeks

1. **Overlay Infrastructure**
   - `IOverlay` interface
   - `OverlayManager` expect/actual
   - `BaseOverlay` implementation

2. **Key Overlays**
   - `NumberOverlayRenderer`
   - `CommandStatusOverlay`
   - `ConfidenceOverlay`

### Phase 3: Manager System (P1) - 2 weeks

1. **Core Managers**
   - `CommandManager`
   - `StateManager`
   - `FocusManager`

2. **Integration Managers**
   - `NavigationManager`
   - `RecoveryManager`

### Phase 4: LearnApp Complete (P2) - 3 weeks

1. **Exploration Engine**
   - Port ExplorationEngine to KMP
   - Add platform-specific accessibility bindings

2. **JIT Learning**
   - Port learning algorithms
   - Add command pattern matching

3. **Consent Management**
   - Port ConsentDialogManager
   - Add platform-specific UI

### Phase 5: Polish & Optimization (P3) - 2 weeks

1. **Remaining Handlers**
   - Media, Phone, Camera handlers

2. **Database Migration**
   - SQLDelight repository implementations

3. **Metrics & Analytics**
   - MetricsManager port

---

## Vivoka Integration Recommendation

### Option A: Port Existing Implementation (Recommended)

Port the 1,057-line VivokaEngine.kt to KMP with platform abstraction:

```kotlin
// commonMain/speech/vivoka/IVivokaEngine.kt
interface IVivokaEngine : ISpeechEngine {
    suspend fun loadModel(modelPath: String): Result<Unit>
    suspend fun enableWakeWord(word: String): Result<Unit>
    suspend fun disableWakeWord(): Result<Unit>
    val wakeWordDetected: SharedFlow<String>
}

// androidMain/speech/vivoka/VivokaEngineImpl.kt
actual class VivokaEngineImpl(context: Context) : IVivokaEngine {
    // Port existing VivokaEngine.kt
    // Use Vivoka AAR via VivokaSDK wrapper
}
```

### Option B: Create Wrapper Library

Create separate `VoiceOSCoreNG-Vivoka` module that depends on:
- VoiceOSCoreNG (for interfaces)
- SpeechRecognition library (for Vivoka AAR)

### Integration Path

1. Add Vivoka SDK dependency via VivokaSDK wrapper
2. Create `VivokaEngineImpl` in androidMain
3. Wire into `SpeechEngineFactoryProvider`
4. Add wake word support
5. Add NLU bridge integration

---

## Summary

| Category | VoiceOSCore | VoiceOSCoreNG | Action |
|----------|-------------|---------------|--------|
| **Speech Engines** | 5 real | 1 real | Port Vivoka, Google, Vosk |
| **Handlers** | 19 | 0 | Create handler system |
| **Overlays** | 15+ | 0 | Port overlay system |
| **Managers** | 9 | 0 | Port core managers |
| **Database** | SQLDelight | In-memory | Integrate SQLDelight |
| **LearnApp** | Complete | 30% | Complete exploration/JIT |

**Estimated Total Effort:** 11 weeks for full parity

**Critical Blockers:**
1. Vivoka AAR integration (requires VivokaSDK wrapper)
2. Platform-specific accessibility APIs (Android AccessibilityService, iOS UIAccessibility)
3. Overlay system (Android WindowManager, iOS overlay approach TBD)

---

## Appendix: File Counts

### VoiceOSCore Structure (381 files)

```
accessibility/       98 files
├── handlers/        19 files
├── managers/         9 files
├── speech/          12 files
├── overlays/        15 files
├── cursor/           8 files
├── extractors/       6 files
└── recognition/      5 files

learnapp/            87 files
├── exploration/     12 files
├── learning/        15 files
├── database/        18 files
├── consent/          8 files
├── safety/           6 files
└── integration/     11 files

scraping/            23 files
security/            14 files
testing/              8 files
ui/                  42 files
```

### VoiceOSCoreNG Structure (94 files)

```
commonMain/          38 files
├── command/          6 files
├── extraction/       4 files
├── features/         3 files
├── fingerprinting/   1 file
├── safety/           1 file
├── speech/           8 files
├── repository/       3 files
└── common/           4 files

androidMain/         24 files
├── speech/           4 files
├── extraction/       3 files
├── execution/        1 file
└── ui/               8 files

iosMain/              8 files
desktopMain/          8 files
commonTest/          16 files
```
