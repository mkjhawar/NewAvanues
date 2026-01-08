# VoiceOSCoreNG Integration Analysis

**Document ID**: VoiceOSCoreNG-Integration-Analysis-70107-V1
**Date**: 2026-01-07
**Module**: Modules/VoiceOSCoreNG
**Status**: Active Development
**Author**: VOS4 Development Team

---

## Executive Summary

VoiceOSCoreNG is a Kotlin Multiplatform (KMP) module providing voice-first accessibility across Android, iOS, and Desktop platforms. This analysis documents the integration between the core subsystems: Voice Recognition, Command Generation, NLU, Database, and Action Execution.

**Overall Integration Status**: 85% Complete

| Subsystem | Status | Confidence |
|-----------|--------|------------|
| Voice Recognition | ✅ Operational | High |
| Command Generation | ✅ Operational | High |
| Command Matching | ✅ Operational | High |
| Action Execution | ✅ Operational | High |
| Database Persistence | ⚠️ Partial | Medium |
| NLU Integration | ⚠️ Basic | Low |
| Screen Change Detection | ❌ Missing | N/A |

---

## 1. Architecture Overview

### 1.1 System Flowchart

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              VOICE INPUT LAYER                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────┐    ┌───────────────────────┐    ┌────────────────────────┐   │
│  │  🎤 Audio    │───▶│  SpeechEngineManager  │───▶│  CommandWordDetector   │   │
│  │  Stream      │    │                       │    │                        │   │
│  └──────────────┘    │  Engines:             │    │  Match Strategies:     │   │
│                      │  • Vivoka (primary)   │    │  • EXACT (1.0)         │   │
│                      │  • Vosk (offline)     │    │  • WORD_SEQ (0.98)     │   │
│                      │  • Google Cloud       │    │  • FUZZY (~0.95)       │   │
│                      │  • Azure              │    │  • PARTIAL (~0.85)     │   │
│                      │  • Android STT        │    │                        │   │
│                      └───────────┬───────────┘    └───────────┬────────────┘   │
│                                  │                            │                 │
│                       CommandEvent(text, conf)     CommandMatch(phrase, conf)  │
│                                  │                            │                 │
└──────────────────────────────────┼────────────────────────────┼─────────────────┘
                                   │                            │
                                   ▼                            ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            COMMAND MATCHING LAYER                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────┐        ┌─────────────────────────┐                 │
│  │  StaticCommandRegistry  │        │  CommandRegistry        │                 │
│  │                         │        │  (Dynamic)              │                 │
│  │  System commands:       │        │                         │                 │
│  │  • "go back"            │        │  Screen-specific:       │                 │
│  │  • "scroll down/up"     │        │  • "click submit"       │                 │
│  │  • "show notifications" │        │  • "tap settings"       │                 │
│  │  • "go home"            │        │  • "open menu"          │                 │
│  └───────────┬─────────────┘        └───────────┬─────────────┘                 │
│              │                                  │                               │
│              └──────────────┬───────────────────┘                               │
│                             ▼                                                   │
│              ┌──────────────────────────────┐                                   │
│              │       CommandMatcher         │                                   │
│              │                              │                                   │
│              │  • Jaccard similarity        │                                   │
│              │  • Ambiguity detection       │                                   │
│              │  • Confidence threshold      │                                   │
│              └──────────────┬───────────────┘                                   │
│                             │                                                   │
│                  MatchResult (Exact|Fuzzy|Ambiguous|NoMatch)                    │
│                             │                                                   │
└─────────────────────────────┼───────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          COMMAND EXECUTION LAYER                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────┐   ┌─────────────────┐   ┌─────────────────────────────┐    │
│  │ CommandDispatcher│─▶│ActionCoordinator│─▶│  Handlers (by category)     │    │
│  │                │   │                 │   │                             │    │
│  │ • Static first │   │ • Find handler  │   │  • NavigationHandler        │    │
│  │ • Then dynamic │   │ • 5s timeout    │   │  • UIHandler                │    │
│  │ • Emit events  │   │ • NLU interpret │   │  • InputHandler             │    │
│  └────────────────┘   │ • Metrics       │   │  • SystemHandler            │    │
│                       └────────┬────────┘   │  • MediaHandler             │    │
│                                │            │  • DeviceHandler            │    │
│                                │            │  • AppHandler               │    │
│                                │            │  • WebViewHandler           │    │
│                                │            └──────────────┬──────────────┘    │
│                                │                           │                    │
│                                ▼                           ▼                    │
│                       ┌─────────────────┐      ┌────────────────────────┐      │
│                       │ IActionExecutor │─────▶│  Platform Actions      │      │
│                       │ (interface)     │      │                        │      │
│                       │                 │      │  Android:              │      │
│                       │ • tap(vuid)     │      │    AccessibilityService│      │
│                       │ • scroll(dir)   │      │  iOS:                  │      │
│                       │ • enterText()   │      │    UIAccessibility     │      │
│                       │ • back/home()   │      │  Desktop:              │      │
│                       └─────────────────┘      │    System automation   │      │
│                                                └────────────────────────┘      │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                       UI EXTRACTION & COMMAND GENERATION                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────┐   ┌─────────────────┐   ┌──────────────────────────┐     │
│  │  UI Screen       │──▶│  ElementParser  │──▶│  CommandGenerator        │     │
│  │                  │   │                 │   │                          │     │
│  │  Sources:        │   │  • Extract      │   │  • QuantizedCommand      │     │
│  │  • HTML DOM      │   │    ElementInfo  │   │  • Generate VUID         │     │
│  │  • Accessibility │   │  • Voice content│   │  • Derive phrase         │     │
│  │    Tree          │   │    priority     │   │  • Set ActionType        │     │
│  │  • WebView JS    │   │                 │   │  • Calculate confidence  │     │
│  │  • CDP           │   └─────────────────┘   └────────────┬─────────────┘     │
│  └──────────────────┘                                      │                    │
│                                                 QuantizedCommand[]              │
│                                                            │                    │
│                         ┌──────────────────────────────────┼────────────────┐  │
│                         ▼                                  ▼                │  │
│          ┌──────────────────────────┐      ┌─────────────────────────────┐ │  │
│          │  CommandRegistry         │      │  Database (SQLDelight)      │ │  │
│          │  (in-memory)             │      │                             │ │  │
│          │                          │      │  ┌─────────────────────┐    │ │  │
│          │  • Fast VUID lookup      │      │  │ scraped_app         │    │ │  │
│          │  • Phrase matching       │      │  │   ↓ FK              │    │ │  │
│          │  • Current screen only   │      │  │ scraped_element     │    │ │  │
│          └──────────────────────────┘      │  │   ↓ FK              │    │ │  │
│                                            │  │ commands_generated  │    │ │  │
│                                            │  └─────────────────────┘    │ │  │
│                                            └─────────────────────────────┘ │  │
│                                                                             │  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Data Flow Summary

```
Audio → CommandEvent → CommandMatch → MatchResult → QuantizedCommand → HandlerResult → ActionResult
         (text,conf)   (phrase,conf)  (Exact/Fuzzy)  (phrase,action,    (success/fail)  (executed)
                                                       targetVuid)
```

---

## 2. Component Analysis

### 2.1 Voice Recognition System

#### SpeechEngineManager
**Location**: `src/commonMain/kotlin/.../speech/SpeechEngineManager.kt`

| Aspect | Details |
|--------|---------|
| **Role** | Central coordinator for speech recognition engines |
| **State** | `StateFlow<SpeechManagerState>` for observable status |
| **Output** | `SharedFlow<CommandEvent>` for recognized commands |
| **Fallback** | Automatic engine switching on failure |

**Supported Engines**:
| Engine | Type | Use Case |
|--------|------|----------|
| Vivoka | Command-word | Primary (low latency, high accuracy) |
| Vosk | Continuous/Offline | Offline mode, privacy-focused |
| Google Cloud | Continuous/Online | High accuracy, cloud-based |
| Azure | Continuous/Online | Enterprise, translation support |
| Android STT | Continuous/Online | Fallback, no API key needed |

#### CommandWordDetector
**Location**: `src/commonMain/kotlin/.../speech/CommandWordDetector.kt`

Bridges continuous speech engines to command-word operation.

**Matching Strategies** (in priority order):
| Strategy | Confidence | Description |
|----------|------------|-------------|
| EXACT | 1.0 | Exact substring match |
| WORD_SEQUENCE | 0.98 | All words in correct order |
| FUZZY | ~0.95 | Levenshtein similarity ≥85% |
| PARTIAL | ~0.85 | ≥80% of command words present |

**Configuration**:
```kotlin
CommandWordDetector(
    confidenceThreshold = 0.9f,  // Minimum to accept
    maxMatches = 5,              // Top N results
    enableFuzzyMatching = true,
    fuzzyTolerance = 0.15f       // Stricter matching
)
```

### 2.2 Command Generation System

#### ElementParser
**Location**: `src/commonMain/kotlin/.../extraction/ElementParser.kt`

| Input Source | Platform | Method |
|--------------|----------|--------|
| HTML DOM | Web/Desktop | Parse HTML structure |
| Accessibility Tree | Android/iOS | JSON from accessibility service |
| WebView JS | Hybrid apps | JavaScript injection |
| CDP | Chrome-based | DevTools Protocol |

**Voice Content Priority**:
1. Display text (most user-visible)
2. Content description (accessibility)
3. Resource ID (developer-defined)
4. Class name (fallback)

#### CommandGenerator
**Location**: `src/commonMain/kotlin/.../common/CommandGenerator.kt`

Generates `QuantizedCommand` objects from `ElementInfo`:

```kotlin
data class QuantizedCommand(
    val uuid: String,                    // Command identifier
    val phrase: String,                  // Voice trigger ("click submit")
    val actionType: CommandActionType,   // CLICK, SCROLL, TYPE, etc.
    val targetVuid: String?,             // Target element VUID
    val confidence: Float,               // 0.0-1.0
    val metadata: Map<String, String>    // packageName, screenId, etc.
)
```

### 2.3 Command Matching System

#### CommandRegistry
**Location**: `src/commonMain/kotlin/.../common/CommandRegistry.kt`

| Feature | Description |
|---------|-------------|
| **Storage** | In-memory, per-screen |
| **Indexing** | By VUID and phrase |
| **Update** | Clear-and-replace on screen change |

#### StaticCommandRegistry
**Location**: `src/commonMain/kotlin/.../common/StaticCommandRegistry.kt`

Pre-defined system commands:
- Navigation: "go back", "go home", "recent apps"
- Scrolling: "scroll up/down/left/right"
- System: "show notifications", "quick settings"
- Control: "stop listening", "start listening"

#### CommandMatcher
**Location**: `src/commonMain/kotlin/.../handlers/CommandMatcher.kt`

**Algorithm**: Jaccard index on word sets with partial matching

```kotlin
sealed class MatchResult {
    data class Exact(val command: QuantizedCommand) : MatchResult()
    data class Fuzzy(val command: QuantizedCommand, val confidence: Float) : MatchResult()
    data class Ambiguous(val candidates: List<QuantizedCommand>) : MatchResult()
    object NoMatch : MatchResult()
}
```

### 2.4 Command Execution System

#### CommandDispatcher
**Location**: `src/commonMain/kotlin/.../handlers/CommandDispatcher.kt`

| Mode | Behavior |
|------|----------|
| STATIC_ONLY | Only system commands |
| DYNAMIC_ONLY | Only screen-specific |
| COMBINED | Static first, then dynamic |

#### ActionCoordinator
**Location**: `src/commonMain/kotlin/.../handlers/ActionCoordinator.kt`

| Feature | Value |
|---------|-------|
| Timeout | 5000ms per command |
| Metrics | Success rate, latency tracking |
| NLU | Pattern-based interpretation |

#### Handler Categories

| Handler | Actions |
|---------|---------|
| NavigationHandler | back, home, recents, forward |
| UIHandler | click, tap, long-press, focus |
| InputHandler | type, paste, clear, select |
| ScrollHandler | scroll, swipe, fling |
| SystemHandler | notifications, settings, screenshot |
| MediaHandler | play, pause, volume, mute |
| DeviceHandler | brightness, rotation, wifi |
| AppHandler | launch, close, switch |
| WebViewHandler | navigate, refresh, zoom |

### 2.5 Database Layer

#### Schema (SQLDelight)

```sql
-- App tracking
CREATE TABLE scraped_app (
    appId TEXT PRIMARY KEY,
    packageName TEXT NOT NULL,
    appVersion TEXT,
    lastScanned INTEGER
);

-- Element storage
CREATE TABLE scraped_element (
    vuid TEXT PRIMARY KEY,
    appId TEXT NOT NULL,
    className TEXT,
    resourceId TEXT,
    text TEXT,
    contentDescription TEXT,
    bounds TEXT,
    FOREIGN KEY (appId) REFERENCES scraped_app(appId)
);

-- Generated commands
CREATE TABLE commands_generated (
    uuid TEXT PRIMARY KEY,
    commandText TEXT NOT NULL,
    actionType TEXT NOT NULL,
    targetVuid TEXT,
    confidence REAL,
    appId TEXT NOT NULL,
    FOREIGN KEY (targetVuid) REFERENCES scraped_element(vuid),
    FOREIGN KEY (appId) REFERENCES scraped_app(appId)
);
```

#### DatabaseConverters
**Location**: `src/commonMain/kotlin/.../functions/DatabaseConverters.kt`

```kotlin
// Domain → Database
fun QuantizedCommand.toDTO(): CommandDTO

// Database → Domain
fun CommandDTO.toQuantizedCommand(): QuantizedCommand
```

---

## 3. Integration Points

### 3.1 Speech → Command Detection

```
SpeechEngineManager
        │
        ├─ Engine produces raw text: "I want to scroll down please"
        │
        ▼
CommandWordDetector
        │
        ├─ Registry: ["scroll down", "go back", "click submit"]
        ├─ Normalize: "i want to scroll down please"
        ├─ Match: findWordSequence → ["scroll", "down"] found in order
        │
        ▼
CommandMatch("scroll down", 0.98, WORD_SEQUENCE)
```

### 3.2 UI Extraction → Command Generation

```
Screen Change Detected
        │
        ▼
ElementParser.parse(accessibilityTree)
        │
        ├─ ElementInfo("Button", text="Submit", clickable=true)
        ├─ ElementInfo("EditText", hint="Email", focusable=true)
        │
        ▼
CommandGenerator.generate(elements)
        │
        ├─ QuantizedCommand("click submit", CLICK, "vuid-btn-123")
        ├─ QuantizedCommand("tap email", FOCUS, "vuid-edit-456")
        │
        ▼
CommandRegistry.update(commands)
```

### 3.3 Command → Execution

```
CommandMatcher.match("click submit", registry)
        │
        ▼
MatchResult.Exact(QuantizedCommand)
        │
        ▼
CommandDispatcher.dispatch(command)
        │
        ├─ Emit: DispatchEvent.Processing
        │
        ▼
ActionCoordinator.processCommand(command)
        │
        ├─ Find handler: UIHandler (category: UI)
        ├─ Execute: handler.execute(command)
        │
        ▼
IActionExecutor.tap("vuid-btn-123")
        │
        ├─ Platform: AccessibilityService.performAction(ACTION_CLICK)
        │
        ▼
ActionResult.success("Clicked Submit button")
```

---

## 4. Gap Analysis

### 4.1 Critical Gaps

| Gap | Impact | Priority |
|-----|--------|----------|
| **Screen Change Trigger** | Commands not updated on navigation | P0 |
| **Repository Implementation** | Commands not persisted to database | P1 |
| **NLU Service Integration** | Only pattern matching, no semantics | P1 |

#### Gap 1: Screen Change Trigger
**Current State**: No automatic mechanism to detect screen changes and trigger command regeneration.

**Impact**: Users must manually trigger command updates or commands become stale.

**Recommendation**:
```kotlin
// Implement AccessibilityEventListener
class ScreenChangeDetector(
    private val extractor: UnifiedExtractor,
    private val generator: CommandGenerator,
    private val registry: CommandRegistry
) {
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == TYPE_WINDOW_STATE_CHANGED) {
            val elements = extractor.extract(event.source)
            val commands = generator.generate(elements)
            registry.update(commands)
        }
    }
}
```

#### Gap 2: Repository Implementation
**Current State**: `DatabaseConverters` exist but no repository classes wire them to actual database operations.

**Impact**: Generated commands exist only in memory; lost on process death.

**Recommendation**:
```kotlin
class SQLDelightCommandRepository(
    private val database: VoiceOSDatabase
) : ICommandRepository {

    override suspend fun saveCommands(commands: List<QuantizedCommand>) {
        database.transaction {
            commands.forEach { cmd ->
                database.commandQueries.insert(cmd.toDTO())
            }
        }
    }

    override suspend fun getCommandsForApp(appId: String): List<QuantizedCommand> {
        return database.commandQueries
            .selectByApp(appId)
            .executeAsList()
            .map { it.toQuantizedCommand() }
    }
}
```

#### Gap 3: NLU Service Integration
**Current State**: `ActionCoordinator.interpretVoiceCommand()` uses ~20 hardcoded pattern rules.

**Impact**: Cannot understand semantic variations ("take me back" vs "go back").

**Current Implementation**:
```kotlin
fun interpretVoiceCommand(text: String): String? {
    return when {
        text.contains("go back") || text.contains("back") -> "back"
        text.contains("scroll down") -> "scroll down"
        // ... ~20 more patterns
        else -> null
    }
}
```

**Recommendation**: Integrate NLU service interface:
```kotlin
interface INLUService {
    suspend fun interpret(text: String): NLUResult
}

data class NLUResult(
    val intent: String,           // "navigation.back"
    val confidence: Float,        // 0.92
    val entities: Map<String, String>, // {"direction": "down"}
    val alternates: List<String>  // ["go back", "return"]
)
```

### 4.2 Design Improvements

| Issue | Current | Recommended |
|-------|---------|-------------|
| **Confidence Passthrough** | Speech confidence ignored by handlers | Check confidence, confirm if < 0.9 |
| **Registry Update Pattern** | Clear-and-replace all commands | Differential update (add/remove/keep) |
| **Error Typing** | Generic string messages | Typed error results with recovery hints |
| **State Synchronization** | Speech & handlers independent | Unified ServiceState across all |

---

## 5. Test Coverage

### 5.1 Existing Tests

| Test File | Coverage |
|-----------|----------|
| `CommandWordDetectorTest.kt` | Match strategies, confidence scoring |
| `CommandMatcherTest.kt` | Fuzzy matching, ambiguity detection |
| `DatabaseFKChainIntegrationTest.kt` | FK constraints, cascade behavior |
| `HandlerExecutionTest.kt` | Handler routing, timeout handling |
| `SpeechEngineManagerTest.kt` | Engine lifecycle, fallback chain |

### 5.2 Test Gaps

| Missing Test | Priority |
|--------------|----------|
| End-to-end speech → execution flow | P0 |
| Screen change → command update cycle | P0 |
| Database persistence round-trip | P1 |
| Multi-engine concurrent recognition | P2 |

---

## 6. Performance Considerations

### 6.1 Latency Budget

| Stage | Target | Current |
|-------|--------|---------|
| Speech recognition | <500ms | ~300-800ms (engine-dependent) |
| Command detection | <50ms | ~10-30ms |
| Command matching | <20ms | ~5-15ms |
| Handler execution | <100ms | ~50-200ms |
| **Total** | **<700ms** | **~400-1100ms** |

### 6.2 Memory Usage

| Component | Estimate |
|-----------|----------|
| Speech engine buffers | 2-5 MB |
| Command registry (100 commands) | ~50 KB |
| Element cache (500 elements) | ~200 KB |
| **Total Runtime** | **~5-10 MB** |

---

## 7. Recommendations

### 7.1 Immediate Actions (P0)

1. **Implement ScreenChangeDetector**
   - Hook into platform accessibility events
   - Trigger extraction → generation → registry update
   - Debounce rapid changes (500ms)

2. **Wire Repository to Pipeline**
   - Implement `SQLDelightCommandRepository`
   - Save commands during generation
   - Load cached commands on app launch

### 7.2 Short-Term (P1)

3. **Add NLU Integration Point**
   - Define `INLUService` interface
   - Create adapter for external NLU (GCP, Azure, local)
   - Fallback to pattern matching if NLU unavailable

4. **Implement Confidence-Aware Execution**
   - Add confirmation dialog for confidence < 0.85
   - Log low-confidence executions for analysis
   - Allow user to adjust threshold

### 7.3 Medium-Term (P2)

5. **Differential Registry Updates**
   - Track element VUIDs across scans
   - Only add new, remove missing, keep stable
   - Reduce command churn during scrolling

6. **Unified ServiceState**
   - Combine speech + handler + database states
   - Single observable for UI status display
   - Clear error states with recovery actions

---

## 8. Appendix

### A. Key File Locations

```
Modules/VoiceOSCoreNG/src/
├── commonMain/kotlin/com/augmentalis/voiceoscoreng/
│   ├── speech/
│   │   ├── SpeechEngineManager.kt
│   │   ├── CommandWordDetector.kt
│   │   └── ContinuousSpeechAdapter.kt
│   ├── handlers/
│   │   ├── CommandDispatcher.kt
│   │   ├── ActionCoordinator.kt
│   │   ├── CommandMatcher.kt
│   │   └── IHandler.kt
│   ├── common/
│   │   ├── CommandRegistry.kt
│   │   ├── CommandGenerator.kt
│   │   ├── QuantizedCommand.kt
│   │   └── StaticCommandRegistry.kt
│   ├── extraction/
│   │   ├── ElementParser.kt
│   │   ├── ElementInfo.kt
│   │   └── UnifiedExtractor.kt
│   ├── functions/
│   │   └── DatabaseConverters.kt
│   └── features/
│       ├── ISpeechEngine.kt
│       ├── ISpeechEngineFactory.kt
│       └── IActionExecutor.kt
├── androidMain/kotlin/.../features/
│   ├── VoskEngineImpl.kt
│   ├── GoogleCloudEngineImpl.kt
│   ├── AzureEngineImpl.kt
│   └── SpeechEngineFactoryProvider.kt
├── iosMain/kotlin/.../
│   ├── speech/AppleSpeechEngine.kt
│   └── features/SpeechEngineFactoryProvider.kt
└── desktopMain/kotlin/.../features/
    └── SpeechEngineFactoryProvider.kt
```

### B. Dependency Graph

```
VoiceOSCoreNG (facade)
    │
    ├── SpeechEngineManager
    │       ├── ISpeechEngine (platform-specific)
    │       └── CommandWordDetector
    │
    ├── ActionCoordinator
    │       ├── CommandDispatcher
    │       │       ├── CommandMatcher
    │       │       ├── StaticCommandRegistry
    │       │       └── CommandRegistry
    │       ├── IHandler (8+ implementations)
    │       └── IActionExecutor (platform-specific)
    │
    └── ServiceStateManager
            └── StateFlow<ServiceState>
```

### C. Configuration Reference

```kotlin
// Speech configuration
SpeechConfig(
    language = "en-US",
    sampleRate = 16000,
    enablePartialResults = true,
    enableProfanityFilter = false,
    enableWordTimestamps = false,
    apiKey = null,  // For cloud engines
    apiRegion = null,
    modelPath = null  // For offline engines
)

// Command detection configuration
CommandWordDetector(
    confidenceThreshold = 0.9f,
    maxMatches = 5,
    enableFuzzyMatching = true,
    fuzzyTolerance = 0.15f
)

// Handler configuration
ActionCoordinator.Config(
    executionTimeout = 5000L,
    enableMetrics = true,
    defaultConfirmationThreshold = 0.85f
)
```

---

**Document Version History**:
| Version | Date | Changes |
|---------|------|---------|
| V1 | 2026-01-07 | Initial analysis |
