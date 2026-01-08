# VoiceOSCoreNG Migration Gaps - Comprehensive Analysis

**Issue ID:** VOS-CORENG-001
**Created:** 2026-01-05
**Priority:** P0 - Critical
**Status:** Open
**Author:** VOS4 Development Team

---

## Executive Summary

VoiceOSCoreNG was created as a Kotlin Multiplatform (KMP) migration of VoiceOSCore but is **missing critical subsystems**. The current implementation has UI extraction and command generation but lacks:

1. **Voice Recognition System** - No speech engine integration
2. **Speech Mode Handling** - No SpeechMode enum or mode-based processing
3. **Command Execution Logic** - No action mapping or command dispatch
4. **Database Integration** - No persistence layer
5. **Static Commands** - No predefined command set
6. **Engine Adapters** - No KMP-compatible speech engine interfaces

---

## Current State Analysis

### VoiceOSCoreNG (What Exists)

| Component | Location | Status |
|-----------|----------|--------|
| CommandGenerator | `commonMain/command/` | ✅ Implemented |
| CommandRegistry | `commonMain/command/` | ✅ Implemented |
| CommandMatcher | `commonMain/command/` | ✅ Implemented |
| ElementExtraction | `commonMain/extraction/` | ✅ Implemented |
| AVU Serialization | `commonMain/avu/` | ✅ Implemented |
| FrameworkHandlers | `commonMain/handlers/` | ✅ Implemented |
| VUIDGenerator | `commonMain/common/` | ✅ Implemented |
| ExplorationEngine | `androidMain/exploration/` | ✅ Android only |

### VoiceOSCore (What's Missing)

| Component | Location | Migration Status |
|-----------|----------|------------------|
| ISpeechEngine | `speech/` | ❌ NOT MIGRATED |
| ISpeechEngineFactory | `speech/` | ❌ NOT MIGRATED |
| SpeechEngineManager | `speech/` | ❌ NOT MIGRATED |
| SpeechEngine enum | `SpeechRecognition/` | ❌ NOT MIGRATED |
| SpeechMode enum | `SpeechRecognition/` | ❌ NOT MIGRATED |
| SpeechConfig | `SpeechRecognition/` | ❌ NOT MIGRATED |
| SpeechState | `speech/` | ❌ NOT MIGRATED |
| VoskEngineAdapter | `speech/` | ❌ NOT MIGRATED |
| GoogleEngineAdapter | `speech/` | ❌ NOT MIGRATED |
| AzureEngineAdapter | `speech/` | ❌ NOT MIGRATED |
| CommandMapper | `handlers/` | ❌ NOT MIGRATED |
| Static Commands | `CommandRegistry` | ❌ NOT MIGRATED |
| Database Repos | `core/database/` | ❌ NOT MIGRATED |

---

## Gap 1: Voice Recognition System

### Problem
VoiceOSCoreNG has no voice recognition capability. It can:
- ✅ Scrape UI elements
- ✅ Generate voice commands from elements
- ✅ Match voice input to commands (if provided externally)
- ❌ Actually listen for voice input
- ❌ Process speech-to-text
- ❌ Register commands with speech engines

### VoiceOSCore Architecture (Current)

```
┌─────────────────────────────────────────────────────────┐
│                  SpeechEngineManager                     │
│  - Manages engine lifecycle                             │
│  - Handles state via StateFlow                          │
│  - Emits command events via SharedFlow                  │
└─────────────────────┬───────────────────────────────────┘
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│VivokaEngine│ │VoskAdapter  │ │GoogleAdapter│ ...
│  (Direct)  │ │(ISpeechEng) │ │(ISpeechEng) │
└─────────────┘ └─────────────┘ └─────────────┘
```

### KMP Migration Architecture (Proposed)

```
┌─────────────────────────────────────────────────────────┐
│                 commonMain                               │
├─────────────────────────────────────────────────────────┤
│ ISpeechEngine         - Interface                       │
│ ISpeechEngineFactory  - Factory interface               │
│ SpeechEngine          - Enum (VOSK, ANDROID_STT, etc)  │
│ SpeechMode            - Enum (STATIC, DYNAMIC, etc)    │
│ SpeechConfig          - Data class                      │
│ SpeechState           - State holder                    │
│ SpeechEngineManager   - Core logic (expect/actual)     │
└─────────────────────────────────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   androidMain   │ │     iosMain     │ │   desktopMain   │
├─────────────────┤ ├─────────────────┤ ├─────────────────┤
│VoskAdapter      │ │SpeechFramework  │ │WhisperAdapter   │
│GoogleAdapter    │ │Adapter          │ │VoskAdapter      │
│AzureAdapter     │ │                 │ │                 │
│VivokaAdapter    │ │                 │ │                 │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## Gap 2: SpeechMode Handling

### Problem
VoiceOSCoreNG has no concept of speech modes. VoiceOSCore uses `SpeechMode` to determine:
- Whether to use command matching or free-form dictation
- Grammar constraints for speech recognition
- Confidence thresholds
- Recognition behavior

### SpeechMode Enum (from VoiceOSCore)

```kotlin
enum class SpeechMode {
    STATIC_COMMAND,    // Predefined commands only (highest accuracy)
    DYNAMIC_COMMAND,   // UI-scraped commands (adapts to screen)
    DICTATION,         // Continuous speech input
    FREE_SPEECH,       // Unrestricted speech
    HYBRID             // Auto online/offline switching (Vivoka)

    fun usesCommandMatching(): Boolean =
        this in listOf(STATIC_COMMAND, DYNAMIC_COMMAND)

    fun supportsContinuous(): Boolean =
        this in listOf(DICTATION, FREE_SPEECH)

    fun getRecommendedConfidenceThreshold(): Float = when (this) {
        STATIC_COMMAND -> 0.8f
        DYNAMIC_COMMAND -> 0.7f
        DICTATION -> 0.6f
        FREE_SPEECH -> 0.5f
        HYBRID -> 0.7f
    }
}
```

### Required Migration
Move to `commonMain` with no changes - this is pure Kotlin, fully KMP compatible.

---

## Gap 3: Command Execution Logic

### Problem
VoiceOSCoreNG can generate commands like "tap settings" but has no logic to:
- Execute the tap action
- Map command phrases to accessibility actions
- Handle cursor/focus operations
- Dispatch actions to the accessibility service

### VoiceOSCore Command Flow

```
Voice Input → CommandMatcher → CommandMapper → AccessibilityService
                              │
                              ▼
                         ┌─────────────────────┐
                         │ Action Execution    │
                         │ - performClick()    │
                         │ - performScroll()   │
                         │ - setText()         │
                         │ - moveFocus()       │
                         └─────────────────────┘
```

### KMP Migration Approach

**commonMain (Interfaces + Logic):**
```kotlin
interface IActionExecutor {
    suspend fun tap(vuid: String): Result<Unit>
    suspend fun longPress(vuid: String): Result<Unit>
    suspend fun scroll(direction: ScrollDirection, amount: Float): Result<Unit>
    suspend fun setText(vuid: String, text: String): Result<Unit>
    suspend fun focus(vuid: String): Result<Unit>
}

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failure(val error: String) : ActionResult()
    data class ElementNotFound(val vuid: String) : ActionResult()
}
```

**androidMain (Implementation):**
```kotlin
actual class ActionExecutor(
    private val accessibilityService: VoiceOSService
) : IActionExecutor {
    actual override suspend fun tap(vuid: String): Result<Unit> {
        // Use AccessibilityNodeInfo.performAction(ACTION_CLICK)
    }
}
```

---

## Gap 4: Static Commands

### Problem
VoiceOSCoreNG has `CommandRegistry` for dynamic commands but no static/predefined commands.

### VoiceOSCore Static Commands (from SpeechEngineManager)

```kotlin
private val STATIC_COMMANDS = listOf(
    // Navigation
    "Go back", "Navigate back", "Back", "Previous screen",
    "Go home", "Home", "Navigate home", "Open Home",

    // Settings
    "Open Settings", "Settings", "Show Settings",

    // App Control
    "Open App Drawer", "Show Recent Apps", "Close App",

    // Media
    "Play Music", "Pause Music", "Stop Music",
    "Next Song", "Previous Song",
    "Increase Volume", "Lower Volume",

    // VoiceOS Special
    "mute voice", "wake up voice",
    "dictation", "end dictation"
)
```

### KMP Migration

Add to `commonMain/command/StaticCommandRegistry.kt`:
```kotlin
object StaticCommandRegistry {
    val navigationCommands = listOf(
        StaticCommand("go back", CommandActionType.BACK),
        StaticCommand("home", CommandActionType.HOME),
        // ...
    )

    val mediaCommands = listOf(...)
    val voiceOSCommands = listOf(...)

    fun all(): List<StaticCommand> =
        navigationCommands + mediaCommands + voiceOSCommands
}
```

---

## Gap 5: Database Integration

### Problem
VoiceOSCoreNG has no database integration. VoiceOSCore uses SQLDelight repositories for:
- Generated commands persistence
- Screen context caching
- VUID storage and lookup
- Analytics

### Required Repositories

| Repository | Purpose | KMP Location |
|------------|---------|--------------|
| IGeneratedCommandRepository | Store/retrieve generated commands | core/database |
| IScreenContextRepository | Cache screen states | core/database |
| IVUIDRepository | VUID lifecycle management | core/database |

### Migration Path
These repositories already exist in `Modules/VoiceOS/core/database/` and are KMP-compatible. VoiceOSCoreNG needs to add dependency and use them.

---

## Gap 6: Speech Engine Adapters

### Problem
VoiceOSCoreNG has no speech engine implementations.

### Current Engine Adapters (VoiceOSCore)

| Engine | File | Platform |
|--------|------|----------|
| Vosk | VoskEngineAdapter.kt | Android (offline) |
| Google STT | GoogleEngineAdapter.kt | Android (online) |
| Azure | AzureEngineAdapter.kt | Android (online) |
| Vivoka | VivokaEngine.kt | Android (hybrid) |

### KMP Engine Strategy

| Engine | androidMain | iosMain | desktopMain |
|--------|-------------|---------|-------------|
| Vosk | ✅ | ❌ | ✅ |
| Native STT | ✅ (SpeechRecognizer) | ✅ (Speech.framework) | ❌ |
| Azure | ✅ | ✅ | ✅ |
| Whisper | ✅ (NDK) | ✅ (Metal) | ✅ |
| Vivoka | ✅ | ❌ | ❌ |

---

## Stashed Files Analysis

### Stash #16 (VoiceOS-Development)
**Content:** Context save files only - no code relevance

### Stash #17 (VoiceOS-Development)
**Files:**
- `DangerousElementDetector.kt` - Detects elements that could cause damage
- `ExplorationEngine.kt` - LearnApp exploration logic
- `ScreenFingerprinter.kt` - Screen state fingerprinting
- `ScreenStateManager.kt` - Screen state management

**KMP Equivalent:** Partial
- VoiceOSCoreNG has `ExplorationEngine.kt` in `androidMain/exploration/`
- Missing: DangerousElementDetector, ScreenFingerprinter, ScreenStateManager

**Action:** Review and migrate to KMP structure

### Stash #18 (VoiceOS-Development)
**Files:**
- `LearnAppIntegration.kt` - Integration layer
- `FloatingProgressWidget.kt` - UI widget

**KMP Equivalent:** No
- These are Android-specific UI components
- FloatingProgressWidget uses Android Views/WindowManager

**Action:** Keep in `androidMain` as platform-specific

---

## Migration Plan

### Phase 1: Core Interfaces (commonMain)
1. Create `speech/ISpeechEngine.kt`
2. Create `speech/ISpeechEngineFactory.kt`
3. Migrate `SpeechEngine` enum
4. Migrate `SpeechMode` enum
5. Migrate `SpeechConfig` data class
6. Migrate `SpeechState` data class
7. Create `speech/SpeechResult.kt`

### Phase 2: Command Execution (commonMain)
1. Create `execution/IActionExecutor.kt`
2. Create `execution/ActionResult.kt`
3. Create `command/StaticCommandRegistry.kt`
4. Create `command/CommandDispatcher.kt`

### Phase 3: Android Implementation (androidMain)
1. Create `speech/VoskEngineAdapter.kt`
2. Create `speech/GoogleEngineAdapter.kt`
3. Create `speech/AzureEngineAdapter.kt`
4. Create `speech/SpeechEngineFactory.kt`
5. Create `execution/AndroidActionExecutor.kt`
6. Create `speech/SpeechEngineManager.kt` (actual)

### Phase 4: Database Integration
1. Add dependency on `core/database` module
2. Create repository adapters for KMP usage
3. Integrate command persistence

### Phase 5: Stash Resolution
1. Review stash #17 for DangerousElementDetector migration
2. Decide on ScreenFingerprinter KMP approach
3. Keep FloatingProgressWidget in androidMain

---

## File Structure (Target)

```
Modules/VoiceOSCoreNG/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/voiceoscoreng/
│   │   ├── speech/
│   │   │   ├── ISpeechEngine.kt         # NEW
│   │   │   ├── ISpeechEngineFactory.kt  # NEW
│   │   │   ├── SpeechEngine.kt          # NEW (enum)
│   │   │   ├── SpeechMode.kt            # NEW (enum)
│   │   │   ├── SpeechConfig.kt          # NEW
│   │   │   ├── SpeechState.kt           # NEW
│   │   │   └── SpeechResult.kt          # NEW
│   │   ├── execution/
│   │   │   ├── IActionExecutor.kt       # NEW
│   │   │   ├── ActionResult.kt          # NEW
│   │   │   └── CommandDispatcher.kt     # NEW
│   │   ├── command/
│   │   │   ├── CommandGenerator.kt      # EXISTS
│   │   │   ├── CommandRegistry.kt       # EXISTS
│   │   │   ├── CommandMatcher.kt        # EXISTS
│   │   │   └── StaticCommandRegistry.kt # NEW
│   │   └── ...existing...
│   │
│   ├── androidMain/kotlin/com/augmentalis/voiceoscoreng/
│   │   ├── speech/
│   │   │   ├── VoskEngineAdapter.kt     # NEW
│   │   │   ├── GoogleEngineAdapter.kt   # NEW
│   │   │   ├── AzureEngineAdapter.kt    # NEW
│   │   │   ├── VivokaEngineAdapter.kt   # NEW
│   │   │   ├── SpeechEngineFactory.kt   # NEW (actual)
│   │   │   └── SpeechEngineManager.kt   # NEW (actual)
│   │   ├── execution/
│   │   │   └── AndroidActionExecutor.kt # NEW
│   │   └── ...existing...
│   │
│   ├── iosMain/kotlin/com/augmentalis/voiceoscoreng/
│   │   ├── speech/
│   │   │   ├── AppleSpeechAdapter.kt    # NEW
│   │   │   └── SpeechEngineFactory.kt   # NEW (actual)
│   │   └── execution/
│   │       └── IOSActionExecutor.kt     # NEW
│   │
│   └── desktopMain/kotlin/com/augmentalis/voiceoscoreng/
│       ├── speech/
│       │   ├── WhisperEngineAdapter.kt  # NEW
│       │   └── SpeechEngineFactory.kt   # NEW (actual)
│       └── execution/
│           └── DesktopActionExecutor.kt # NEW
```

---

## Acceptance Criteria

- [ ] SpeechMode enum in commonMain with all modes
- [ ] SpeechEngine enum in commonMain with all engines
- [ ] ISpeechEngine interface in commonMain
- [ ] At least one working engine adapter per platform
- [ ] Static commands registered and matchable
- [ ] Command execution working on Android
- [ ] Database repositories integrated
- [ ] All stashed files reviewed and actioned

---

## References

- VoiceOSCore Speech: `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/speech/`
- SpeechRecognition Library: `Modules/VoiceOS/libraries/SpeechRecognition/`
- Database Repositories: `Modules/VoiceOS/core/database/src/commonMain/`
- VoiceOSCoreNG Current: `Modules/VoiceOSCoreNG/src/`
