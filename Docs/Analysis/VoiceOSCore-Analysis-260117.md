# VoiceOSCore Module Structural Analysis

**Date:** 2026-01-17 | **Version:** V1 | **Author:** Claude

## Summary

This analysis examines the VoiceOSCore module located at `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore`, created by consolidating `Voice:Core` (voiceoscoreng) and `VoiceOS` modules into a unified KMP module. The module has compilation errors due to duplicate type definitions and missing `actual` implementations for several `expect` declarations.

---

## 1. Source Set Structure

### 1.1 File Distribution

| Source Set | File Count | Purpose |
|------------|------------|---------|
| `commonMain` | ~175 files | Shared KMP business logic |
| `androidMain` | 5 files | Android-specific actuals |
| `iosMain` | 5 files | iOS-specific actuals |
| `desktopMain` | 5 files | Desktop/JVM-specific actuals |
| `commonTest` | TBD | Shared tests |

### 1.2 commonMain Files (175 files)

The commonMain directory contains the bulk of the module:

**Core Entry Points:**
- `VoiceOSCoreNG.kt` - Main facade and Builder pattern entry
- `VoiceOSCore.kt` - Legacy entry point (object)
- `SpeechEngineManager.kt` - Speech engine coordinator

**Handler Classes:**
- `NavigationHandler.kt`, `InputHandler.kt`, `UIHandler.kt`
- `GestureHandler.kt`, `DragHandler.kt`, `SystemHandler.kt`
- `DeviceHandler.kt`, `NumberHandler.kt`, `SelectHandler.kt`
- `HelpMenuHandler.kt`, `AppHandler.kt`

**Framework Handlers:**
- `FrameworkHandler.kt` (interface + registry)
- `NativeHandler.kt`, `ComposeHandler.kt`
- `FlutterHandler.kt`, `ReactNativeHandler.kt`
- `UnityHandler.kt`, `WebViewHandler.kt`

**Speech/Voice:**
- `ISpeechEngine.kt`, `ISpeechEngineFactory.kt`
- `SpeechEngine.kt` (enum + registry)
- `SpeechConfig.kt`, `SpeechMode.kt`
- `IVivokaEngine.kt`, `VivokaEngineFactory.kt`
- `ContinuousSpeechAdapter.kt`

**NLU/LLM:**
- `INluProcessor.kt`, `NluProcessorFactory.kt`
- `ILlmProcessor.kt`, `LlmProcessorFactory.kt`
- `LlmFallbackHandler.kt`

**Command System:**
- `CommandModels.kt` - Core command data classes
- `QuantizedCommand.kt`, `QuantizedElement.kt`
- `CommandRegistry.kt`, `StaticCommandRegistry.kt`
- `CommandMatcher.kt`, `CommandGenerator.kt`
- `CommandLearner.kt`, `CommandWordDetector.kt`
- `ICommandPersistence.kt`, `IStaticCommandPersistence.kt`

**Exploration:**
- `IExplorationEngine.kt`, `ExplorationState.kt`
- `ExplorationFrame.kt`, `ExplorationStats.kt`
- `ExplorationCommand.kt`, `ExplorationConfig.kt`

**Framework Detection:**
- `FrameworkDetector.kt` - Advanced detector with NodeInfo
- `FrameworkInfo.kt` - Simple detector (DUPLICATE)
- `FrameworkDetectionResult.kt`, `AppFramework.kt`

**Overlay System:**
- `IOverlay.kt`, `BaseOverlay.kt`
- `OverlayManager.kt`, `OverlayCoordinator.kt`
- `OverlayTheme.kt`, `OverlayThemes.kt`
- `NumberedSelectionOverlay.kt`, `ConfidenceOverlay.kt`
- `CommandStatusOverlay.kt`, `ContextMenuOverlay.kt`

**Synonym System:**
- `SynonymParser.kt`, `SynonymLoader.kt`
- `SynonymPaths.kt`, `SynonymMap.kt`
- `SynonymEntry.kt`, `SynonymBinaryFormat.kt`
- `ISynonymProvider.kt`

**Utilities:**
- `HashUtils.kt`, `TextUtils.kt`, `JsonUtils.kt`
- `ValidationUtils.kt`, `InputValidator.kt`
- `PIIRedactionHelper.kt`, `PIILoggingWrapper.kt`
- `LoggerFactory.kt`, `Logger.kt`, `LogLevel.kt`

### 1.3 Platform Source Sets

**androidMain (5 files):**
```
androidMain/kotlin/com/augmentalis/voiceoscore/
  AndroidLogger.kt       - Logger implementation using Android Log
  LoggerFactory.kt       - actual object LoggerFactory
  PlatformActuals.kt     - actual funs: sha256, currentTimeMillis, etc.
  Sha256Android.kt       - sha256Impl using MessageDigest
  VoiceOSCoreAndroid.kt  - Android-specific initialization
```

**iosMain (5 files):**
```
iosMain/kotlin/com/augmentalis/voiceoscore/
  IosLogger.kt           - Logger implementation using NSLog
  LoggerFactory.kt       - actual object LoggerFactory
  PlatformActuals.kt     - actual funs using Foundation
  Sha256Ios.kt           - sha256Impl using CommonCrypto
  VoiceOSCoreIOS.kt      - iOS-specific initialization
```

**desktopMain (5 files):**
```
desktopMain/kotlin/com/augmentalis/voiceoscore/
  JvmLogger.kt           - Logger implementation using SLF4J/println
  LoggerFactory.kt       - actual object LoggerFactory
  PlatformActuals.kt     - actual funs using java.* APIs
  Sha256Jvm.kt           - sha256Impl using MessageDigest
  VoiceOSCoreDesktop.kt  - Desktop-specific initialization
```

---

## 2. Duplicate Type Definitions

### 2.1 Critical Duplicates (MUST FIX)

| Type | File 1 | File 2 | Recommendation |
|------|--------|--------|----------------|
| `object FrameworkDetector` | `FrameworkDetector.kt:73` | `FrameworkInfo.kt:61` | Keep `FrameworkDetector.kt` (more complete with NodeInfo) |
| `data class ExplorationStats` | `ExplorationStats.kt:16` | `IExplorationEngine.kt:74` | Keep `ExplorationStats.kt` (has AVU format), refactor `IExplorationEngine.kt` |

### 2.2 Duplicate Details

**FrameworkDetector Conflict:**

1. **FrameworkDetector.kt** (lines 73-190+)
   - Uses `NodeInfo` interface for platform-agnostic detection
   - Detects: Unity, Unreal, Flutter, React Native, Compose, WebView
   - Returns `FrameworkDetectionResult`
   - More sophisticated with depth limiting and signal collection

2. **FrameworkInfo.kt** (lines 61-190)
   - Uses simple `List<String>` class name matching
   - Detects same frameworks but simpler approach
   - Returns `FrameworkInfo`
   - Has `FrameworkType` enum (distinct from `AppFramework` enum)

**ExplorationStats Conflict:**

1. **ExplorationStats.kt** (lines 16-81)
   - Properties: `screenCount`, `elementCount`, `commandCount`, `avgDepth`, `maxDepth`, `coverage`, `durationMs`
   - Has `toStaLine()` and `fromStaLine()` for AVU format
   - No package/app name

2. **IExplorationEngine.kt** (lines 74-92)
   - Properties: `packageName`, `appName`, `totalScreens`, `totalElements`, `totalEdges`, `durationMs`, `maxDepth`, `dangerousElementsSkipped`, `loginScreensDetected`, `scrollableContainersFound`, `completeness`
   - Has `toString()` override
   - More detailed for exploration progress

---

## 3. Expect/Actual Analysis

### 3.1 Expect Declarations Summary

| Expect Declaration | File | Has Android Actual | Has iOS Actual | Has Desktop Actual |
|-------------------|------|-------------------|----------------|-------------------|
| `expect fun currentTimeMillis()` | `ISpeechEngine.kt:234` | YES | YES | YES |
| `expect fun getCurrentTimeMillis()` | `LoggingUtils.kt:240` | YES | YES | YES |
| `expect fun sha256(input: String)` | `ScreenFingerprinter.kt:167` | YES | YES | YES |
| `expect internal fun sha256Impl(input: String)` | `HashUtils.kt:114` | YES | YES | YES |
| `expect fun extractAccessibilityElements()` | `PlatformExtractor.kt:25` | YES | YES | YES |
| `expect fun executeWebScript(script: String)` | `PlatformExtractor.kt:34` | YES | YES | YES |
| `expect fun isAccessibilityAvailable()` | `PlatformExtractor.kt:41` | YES | YES | YES |
| `expect fun isWebExtractionAvailable()` | `PlatformExtractor.kt:48` | YES | YES | YES |
| `expect fun getPlatformName()` | `PlatformExtractor.kt:55` | YES | YES | YES |
| `expect object LoggerFactory` | `LoggerFactory.kt:20` | YES | YES | YES |
| `expect object NluProcessorFactory` | `NluProcessorFactory.kt:20` | **NO** | **NO** | **NO** |
| `expect object LlmProcessorFactory` | `LlmProcessorFactory.kt:20` | **NO** | **NO** | **NO** |
| `expect object VivokaEngineFactory` | `VivokaEngineFactory.kt:19` | **NO** | **NO** | **NO** |
| `expect object SpeechEngineFactoryProvider` | `ISpeechEngineFactory.kt:124` | **NO** | **NO** | **NO** |
| `expect object SynonymPathsProvider` | `SynonymPaths.kt:189` | **NO** | **NO** | **NO** |
| `expect object LlmFallbackHandlerFactory` | `LlmFallbackHandler.kt:236` | **NO** | **NO** | **NO** |

### 3.2 Missing Actuals (MUST IMPLEMENT)

**6 expect objects missing ALL actuals:**

1. **NluProcessorFactory** - Creates platform-specific NLU processors
2. **LlmProcessorFactory** - Creates platform-specific LLM processors
3. **VivokaEngineFactory** - Creates Vivoka engine instances (Android-only real, others stub)
4. **SpeechEngineFactoryProvider** - Creates platform-specific speech engine factory
5. **SynonymPathsProvider** - Provides platform-specific synonym file paths
6. **LlmFallbackHandlerFactory** - Creates LLM fallback handler instances

---

## 4. Dependency Graph

### 4.1 Core Component Hierarchy

```
VoiceOSCoreNG (Main Facade)
    |
    +-- Builder
    |     +-- HandlerFactory (user-provided)
    |     +-- ISpeechEngineFactory <- SpeechEngineFactoryProvider.create()
    |     +-- ServiceConfiguration
    |     +-- CommandRegistry
    |     +-- ISynonymProvider <- SynonymLoader <- SynonymPathsProvider
    |     +-- INluProcessor <- NluProcessorFactory.create()
    |     +-- ILlmProcessor <- LlmProcessorFactory.create()
    |     +-- IStaticCommandPersistence
    |
    +-- ActionCoordinator
    |     +-- CommandRegistry (shared reference)
    |     +-- INluProcessor
    |     +-- ILlmProcessor
    |     +-- MetricsCollector
    |     +-- Handlers[]
    |
    +-- ServiceStateManager
    +-- ISpeechEngine (created from factory)
```

### 4.2 Dependency Flow (ASCII Diagram)

```
                    +------------------+
                    |  VoiceOSCoreNG   |
                    |    (Facade)      |
                    +--------+---------+
                             |
         +-------------------+-------------------+
         |                   |                   |
         v                   v                   v
+----------------+  +----------------+  +-----------------+
| ActionCoord-   |  | ServiceState-  |  | ISpeechEngine   |
| inator         |  | Manager        |  | (created via    |
+-------+--------+  +----------------+  | factory)        |
        |                               +-----------------+
        +--------------------+
        |          |         |
        v          v         v
+----------+ +----------+ +----------+
| Command- | | INlu-    | | ILlm-    |
| Registry | | Processor| | Processor|
+----------+ +----------+ +----------+
                 ^              ^
                 |              |
    +------------+    +---------+
    |                 |
+---+------------+  +-+---------------+
| NluProcessor-  |  | LlmProcessor-   |
| Factory        |  | Factory         |
| (MISSING)      |  | (MISSING)       |
+----------------+  +-----------------+
```

### 4.3 Initialization Order

1. **Platform factories must exist first:**
   - `SpeechEngineFactoryProvider.create()` - needed by Builder
   - `NluProcessorFactory.create()` - optional, for NLU
   - `LlmProcessorFactory.create()` - optional, for LLM fallback
   - `SynonymPathsProvider.getPaths()` - for synonym loading
   - `VivokaEngineFactory.create()` - for Vivoka speech engine

2. **Builder configuration:**
   - `HandlerFactory` (required, user-provided)
   - `ServiceConfiguration` (optional, defaults available)
   - Optional: NLU/LLM processors, synonyms, persistence

3. **VoiceOSCoreNG.initialize():**
   - Creates handlers via `HandlerFactory.createHandlers()`
   - Initializes `ActionCoordinator` with handlers
   - Populates static commands (if persistence provided)
   - Initializes synonym provider
   - Initializes NLU processor (if configured)
   - Initializes LLM processor (if configured)
   - Creates and initializes speech engine
   - Registers static commands with speech engine
   - Transitions to `Ready` state

---

## 5. Key Components and Responsibilities

### 5.1 Entry Points

| Component | File | Purpose |
|-----------|------|---------|
| `VoiceOSCoreNG` | `VoiceOSCoreNG.kt` | Main facade with Builder pattern |
| `VoiceOSCore` | `VoiceOSCore.kt` | Legacy object-based entry (thin wrapper) |
| `SpeechEngineManager` | `SpeechEngineManager.kt` | Speech engine lifecycle coordinator |

### 5.2 Core Interfaces

| Interface | File | Purpose |
|-----------|------|---------|
| `IHandler` | `IHandler.kt` | Base handler interface |
| `ISpeechEngine` | `ISpeechEngine.kt` | Speech recognition contract |
| `ISpeechEngineFactory` | `ISpeechEngineFactory.kt` | Factory for speech engines |
| `INluProcessor` | `INluProcessor.kt` | NLU processing contract |
| `ILlmProcessor` | `ILlmProcessor.kt` | LLM processing contract |
| `IVivokaEngine` | `IVivokaEngine.kt` | Vivoka-specific speech engine |
| `IExplorationEngine` | `IExplorationEngine.kt` | App exploration contract |
| `ICommandPersistence` | `ICommandPersistence.kt` | Command storage contract |
| `ISynonymProvider` | `ISynonymProvider.kt` | Synonym lookup contract |
| `IOverlay` | `IOverlay.kt` | Visual overlay contract |

### 5.3 Data Models

| Model | File | Purpose |
|-------|------|---------|
| `QuantizedCommand` | `QuantizedCommand.kt` | Voice command with VUID |
| `QuantizedElement` | `QuantizedElement.kt` | UI element with VUID |
| `ElementInfo` | `ElementInfo.kt` | Raw element data |
| `CommandResult` | `CommandModels.kt` | Command execution result |
| `SpeechResult` | `ISpeechEngine.kt` | Speech recognition result |
| `OverlayTheme` | `OverlayTheme.kt` | Overlay styling |

---

## 6. Recommended Fix Order

### Phase 1: Critical Fixes (Unblock Compilation)

**1.1 Remove Duplicate `FrameworkDetector`:**
- Delete `FrameworkInfo.kt` lines 61-190 (simple detector)
- Keep `FrameworkDetector.kt` (NodeInfo-based)
- Update any references from `FrameworkInfo.detect()` to `FrameworkDetector.detectFramework()`
- Note: `FrameworkType` enum in `FrameworkInfo.kt` vs `AppFramework` enum need reconciliation

**1.2 Resolve Duplicate `ExplorationStats`:**
- Rename `IExplorationEngine.kt`'s `ExplorationStats` to `ExplorationSummary`
- Or merge both into single definition with all fields
- Keep `ExplorationStats.kt` version for AVU format support

**1.3 Create Missing Actuals (Minimal Stubs):**

Each platform needs these 6 actual implementations:

**Android (androidMain):**
```kotlin
// NluProcessorFactory.android.kt
actual object NluProcessorFactory {
    actual fun create(config: NluConfig): INluProcessor = StubNluProcessor()
}

// LlmProcessorFactory.android.kt
actual object LlmProcessorFactory {
    actual fun create(config: LlmConfig): ILlmProcessor = StubLlmProcessor()
}

// VivokaEngineFactory.android.kt
actual object VivokaEngineFactory {
    actual fun isAvailable(): Boolean = false // TODO: check Vivoka SDK
    actual fun create(config: VivokaConfig): IVivokaEngine = StubVivokaEngine(config)
}

// SpeechEngineFactoryProvider.android.kt
actual object SpeechEngineFactoryProvider {
    actual fun create(): ISpeechEngineFactory = AndroidSpeechEngineFactory()
}

// SynonymPathsProvider.android.kt
actual object SynonymPathsProvider {
    actual fun getPaths(): ISynonymPaths = DefaultSynonymPaths.forAndroid(...)
}

// LlmFallbackHandlerFactory.android.kt
actual object LlmFallbackHandlerFactory {
    actual fun create(config: FallbackConfig): ILlmFallbackHandler = StubLlmFallbackHandler()
}
```

**iOS (iosMain) - Similar stubs**
**Desktop (desktopMain) - Similar stubs**

### Phase 2: Type Reconciliation

**2.1 Framework Detection Types:**
- Decide: Use `FrameworkType` or `AppFramework` enum
- Migrate all usages to chosen enum
- Delete the redundant one

**2.2 ExplorationStats Merge:**
- Create unified `ExplorationStats` with all fields
- Add optional fields for backward compatibility
- Keep AVU format methods

### Phase 3: Interface Implementation

**3.1 Implement Real Factories (Android):**
- `AndroidSpeechEngineFactory` - wraps Android STT
- `AndroidNluProcessor` - wraps IntentClassifier
- `AndroidLlmProcessor` - wraps LocalLLMProvider
- `AndroidVivokaEngine` - wraps Vivoka SDK

### Phase 4: Validation

- Run `./gradlew :Modules:VoiceOSCore:compileKotlinAndroid`
- Run `./gradlew :Modules:VoiceOSCore:compileKotlinDesktop`
- Fix any remaining issues

---

## 7. Files That Should Be Platform-Specific

Currently in `commonMain` but may need platform handling:

| File | Issue | Recommendation |
|------|-------|----------------|
| `DatabaseConverters.kt` | Uses SQL-specific types | Move to androidMain or add Room-specific expect/actual |
| `OverlayManager.kt` | References UI components | May need platform-specific rendering |
| `NumberOverlayRenderer.kt` | Graphics operations | Consider platform-specific renderers |

---

## 8. References

### 8.1 Key File Paths

- Module root: `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/`
- Build file: `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/build.gradle.kts`
- Common source: `src/commonMain/kotlin/com/augmentalis/voiceoscore/`
- Android source: `src/androidMain/kotlin/com/augmentalis/voiceoscore/`
- iOS source: `src/iosMain/kotlin/com/augmentalis/voiceoscore/`
- Desktop source: `src/desktopMain/kotlin/com/augmentalis/voiceoscore/`

### 8.2 Related Documentation

- `Docs/MasterDocs/AI/PLATFORM-INDEX.ai.md` - Module overview
- `Docs/analysis/Analysis-VoiceOSCore-Migration-260115-V1.md` - Migration analysis
- `Docs/analysis/Analysis-VoiceOS-Comparison-260115-V1.md` - VoiceOS comparison

---

## Summary of Required Actions

| Priority | Action | Files Affected | Effort |
|----------|--------|----------------|--------|
| P0 | Delete duplicate FrameworkDetector | `FrameworkInfo.kt` | Low |
| P0 | Rename/merge duplicate ExplorationStats | `IExplorationEngine.kt`, `ExplorationStats.kt` | Medium |
| P0 | Create 6 actual stubs (Android) | New files in androidMain | Medium |
| P0 | Create 6 actual stubs (iOS) | New files in iosMain | Medium |
| P0 | Create 6 actual stubs (Desktop) | New files in desktopMain | Medium |
| P1 | Reconcile FrameworkType vs AppFramework | Multiple files | Medium |
| P2 | Implement real Android factories | androidMain factories | High |
| P3 | Implement real iOS/Desktop factories | Platform-specific | High |

---

*Analysis generated by Claude on 2026-01-17*
