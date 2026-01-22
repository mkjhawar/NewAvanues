# AI Architecture Rework - Phase 2 Handover Document

**Date:** 2026-01-22
**Branch:** `AI-Architecture-Rework`
**Session:** Phase 2 - Module Contracts & Migration
**Status:** Ready for Implementation
**Previous Phase:** Phase 1 Complete (commit a52a9cba)

---

## Phase 1 Summary (Completed)

Phase 1 established the Universal Plugin Architecture foundation:

### Files Created
```
Modules/UniversalRPC/Common/proto/plugin.proto              ✅ gRPC service definition
Modules/UniversalRPC/src/commonMain/.../plugin/
├── PluginProtoStubs.kt                                     ✅ Proto message stubs
├── PluginServiceClient.kt                                  ✅ Wire client interface
└── GrpcPluginServiceClient.kt                              ✅ Client implementation

Modules/UniversalRPC/android/Plugin/
├── PluginServiceGrpcServer.kt                              ✅ gRPC server
└── PluginServiceGrpcClient.kt                              ✅ Android client with retry

Modules/PluginSystem/src/commonMain/.../universal/
├── UniversalPlugin.kt                                      ✅ Core interface
├── PluginCapability.kt                                     ✅ 25+ capabilities
├── PluginState.kt                                          ✅ 9-state lifecycle
├── PluginTypes.kt                                          ✅ Config, Context, HealthStatus
├── UniversalPluginRegistry.kt                              ✅ Registry with ServiceRegistry
├── PluginEventBus.kt                                       ✅ Event bus interface
├── GrpcPluginEventBus.kt                                   ✅ SharedFlow implementation
└── PluginLifecycleManager.kt                               ✅ Lifecycle management

Modules/PluginSystem/src/commonTest/.../universal/
└── UniversalPluginIntegrationTest.kt                       ✅ Integration tests
```

---

## Phase 2 Overview

**Goal:** Migrate existing modules to the Universal Plugin pattern by creating module-specific contracts (interfaces) that extend `UniversalPlugin`.

**Timeline:** Weeks 3-4 of the overall plan

### Deliverables
1. VoiceOSCore plugin contracts (Handler, Synonym, Overlay)
2. AI plugin contracts (LLM, NLU, Embedding, RAG)
3. Speech plugin contracts (SpeechEngine, TTS, WakeWord)
4. Plugin SDK template & utilities
5. First migrated plugin (prototype)

---

## Phase 2 Tasks

### Task 2.1: VoiceOSCore Plugin Contracts

Create interfaces for VoiceOSCore-specific plugins.

**Files to Create:**
```
Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/universal/contracts/
├── voiceoscore/
│   ├── HandlerPlugin.kt              # Execute voice commands
│   ├── SynonymProviderPlugin.kt      # Custom vocabulary/synonyms
│   ├── OverlayPlugin.kt              # Accessibility overlays
│   ├── ThemeProviderPlugin.kt        # Visual themes
│   └── CommandPersistencePlugin.kt   # Command storage
```

**HandlerPlugin.kt Interface:**
```kotlin
package com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement

/**
 * Handler Plugin - Execute voice/gaze commands.
 *
 * Handlers are the workhorses of VoiceOSCore, processing user commands
 * and executing actions on UI elements.
 */
interface HandlerPlugin : UniversalPlugin {

    /** Handler type for categorization */
    val handlerType: HandlerType

    /** Command patterns this handler supports */
    val patterns: List<CommandPattern>

    /**
     * Check if this handler can process a command.
     * Called by VoiceOSCore to find the right handler.
     */
    fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean

    /**
     * Execute the command.
     * @return ActionResult with success/failure and execution details
     */
    suspend fun handle(command: QuantizedCommand, context: HandlerContext): ActionResult

    /**
     * Get confidence score for handling this command.
     * Higher scores win when multiple handlers match.
     */
    fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float
}

enum class HandlerType {
    NAVIGATION,      // App navigation (back, home, open app)
    UI_INTERACTION,  // Click, scroll, swipe
    TEXT_INPUT,      // Type, dictate
    SYSTEM,          // Volume, brightness, settings
    ACCESSIBILITY,   // Screen reader, magnification
    CUSTOM           // Third-party handlers
}

data class CommandPattern(
    val regex: Regex,
    val intent: String,
    val requiredEntities: Set<String> = emptySet(),
    val examples: List<String> = emptyList()
)

data class HandlerContext(
    val currentScreen: ScreenContext,
    val elements: List<QuantizedElement>,
    val previousCommand: QuantizedCommand?,
    val userPreferences: Map<String, Any>
)

data class ScreenContext(
    val packageName: String,
    val activityName: String,
    val screenTitle: String?,
    val elementCount: Int,
    val primaryAction: String?
)
```

**SynonymProviderPlugin.kt Interface:**
```kotlin
interface SynonymProviderPlugin : UniversalPlugin {

    /** Locale(s) this provider supports */
    val supportedLocales: Set<String>

    /**
     * Get synonyms for a canonical phrase.
     * Used to expand voice command vocabulary.
     */
    fun getSynonyms(phrase: String, locale: String = "en-US"): List<String>

    /**
     * Normalize a phrase to its canonical form.
     * "tap the button" -> "click button"
     */
    fun normalize(phrase: String, locale: String = "en-US"): String

    /**
     * Check if two phrases are equivalent.
     */
    fun areEquivalent(phrase1: String, phrase2: String, locale: String = "en-US"): Boolean

    /**
     * Get action verb synonyms.
     * "click" -> ["tap", "press", "select", "activate"]
     */
    fun getActionSynonyms(action: String, locale: String = "en-US"): List<String>
}
```

**OverlayPlugin.kt Interface:**
```kotlin
interface OverlayPlugin : UniversalPlugin {

    /** Overlay priority (higher = rendered on top) */
    val priority: Int

    /** Overlay visibility mode */
    val visibilityMode: OverlayVisibility

    /**
     * Render overlay content for current screen.
     */
    fun render(context: OverlayContext): OverlayContent

    /**
     * Handle user interaction with overlay.
     * @return true if interaction was consumed
     */
    suspend fun onInteraction(event: OverlayInteractionEvent): Boolean

    /**
     * Called when overlay should update (screen change, etc.)
     */
    fun onScreenChanged(context: OverlayContext)
}

enum class OverlayVisibility {
    ALWAYS,          // Always visible
    ON_COMMAND,      // Show when voice command active
    ON_GAZE,         // Show when gaze detected
    MANUAL           // Explicit show/hide
}

data class OverlayContent(
    val elements: List<OverlayElement>,
    val bounds: Rect?,
    val alpha: Float = 1.0f
)

sealed class OverlayElement {
    data class Label(val text: String, val position: Point, val style: TextStyle) : OverlayElement()
    data class Highlight(val bounds: Rect, val color: Int, val strokeWidth: Float) : OverlayElement()
    data class Badge(val number: Int, val position: Point) : OverlayElement()
}
```

---

### Task 2.2: AI Plugin Contracts

Create interfaces for AI/ML plugins.

**Files to Create:**
```
Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/universal/contracts/
├── ai/
│   ├── LLMPlugin.kt                 # Text generation
│   ├── NLUPlugin.kt                 # Intent & entity extraction
│   ├── EmbeddingPlugin.kt           # Vector embeddings
│   ├── RAGPlugin.kt                 # Retrieval-augmented generation
│   └── AIModelInfo.kt               # Shared model metadata
```

**LLMPlugin.kt Interface:**
```kotlin
interface LLMPlugin : UniversalPlugin {

    /** Model information */
    val modelInfo: AIModelInfo

    /** Maximum context window in tokens */
    val contextWindow: Int

    /** Whether model supports streaming */
    val supportsStreaming: Boolean

    /**
     * Generate text from prompt.
     */
    suspend fun generate(request: GenerationRequest): GenerationResponse

    /**
     * Stream generated tokens.
     */
    fun generateStream(request: GenerationRequest): Flow<GenerationToken>

    /**
     * Count tokens in text.
     */
    fun countTokens(text: String): Int

    /**
     * Check if model is loaded and ready.
     */
    fun isReady(): Boolean
}

data class AIModelInfo(
    val name: String,
    val version: String,
    val provider: String,           // "local", "openai", "anthropic", etc.
    val quantization: String?,      // "q4_0", "q8_0", "fp16", null
    val parameterCount: Long?,      // 7B, 13B, etc.
    val fileSizeBytes: Long?
)

data class GenerationRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val maxTokens: Int = 256,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val stopSequences: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

data class GenerationResponse(
    val text: String,
    val finishReason: FinishReason,
    val tokenCount: Int,
    val latencyMs: Long,
    val metadata: Map<String, Any> = emptyMap()
)

enum class FinishReason {
    COMPLETE,       // Model finished naturally
    MAX_TOKENS,     // Hit token limit
    STOP_SEQUENCE,  // Hit stop sequence
    ERROR           // Error occurred
}
```

**NLUPlugin.kt Interface:**
```kotlin
interface NLUPlugin : UniversalPlugin {

    /** Supported intents this NLU can classify */
    val supportedIntents: Set<String>

    /** Supported entity types */
    val supportedEntities: Set<String>

    /**
     * Classify intent from user utterance.
     */
    suspend fun classifyIntent(
        utterance: String,
        context: NLUContext
    ): IntentResult

    /**
     * Extract entities from utterance.
     */
    suspend fun extractEntities(utterance: String): List<Entity>

    /**
     * Generate voice command suggestions from UI context.
     */
    suspend fun suggestCommands(
        elements: List<QuantizedElement>,
        screenContext: ScreenContext
    ): List<CommandSuggestion>
}

data class NLUContext(
    val screenContext: ScreenContext?,
    val previousUtterances: List<String> = emptyList(),
    val availableCommands: List<QuantizedCommand> = emptyList(),
    val userPreferences: Map<String, Any> = emptyMap()
)

data class IntentResult(
    val intent: String,
    val confidence: Float,
    val entities: List<Entity> = emptyList(),
    val alternatives: List<IntentAlternative> = emptyList()
)

data class Entity(
    val type: String,       // "NUMBER", "DIRECTION", "APP_NAME", etc.
    val value: String,
    val normalizedValue: Any? = null,
    val start: Int,
    val end: Int,
    val confidence: Float = 1.0f
)

data class CommandSuggestion(
    val phrase: String,
    val targetAvid: String?,
    val confidence: Float,
    val synonyms: List<String> = emptyList()
)
```

**EmbeddingPlugin.kt Interface:**
```kotlin
interface EmbeddingPlugin : UniversalPlugin {

    /** Embedding dimension */
    val dimension: Int

    /** Maximum input length in tokens */
    val maxInputLength: Int

    /**
     * Generate embedding vector for text.
     */
    suspend fun embed(text: String): FloatArray

    /**
     * Batch embed multiple texts efficiently.
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>

    /**
     * Calculate similarity between two embeddings.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float
}
```

---

### Task 2.3: Speech Plugin Contracts

Create interfaces for speech recognition and TTS plugins.

**Files to Create:**
```
Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/universal/contracts/
├── speech/
│   ├── SpeechEnginePlugin.kt        # Speech-to-text
│   ├── TTSPlugin.kt                 # Text-to-speech
│   ├── WakeWordPlugin.kt            # Wake word detection
│   └── SpeechTypes.kt               # Shared types
```

**SpeechEnginePlugin.kt Interface:**
```kotlin
interface SpeechEnginePlugin : UniversalPlugin {

    /** Engine capabilities */
    val engineCapabilities: Set<SpeechCapability>

    /** Supported languages (BCP-47 codes) */
    val supportedLanguages: Set<String>

    /** Current recognition state */
    val recognitionState: StateFlow<RecognitionState>

    /**
     * Start listening for voice input.
     * @return Flow of recognition results
     */
    fun startRecognition(config: RecognitionConfig): Flow<RecognitionResult>

    /**
     * Stop listening.
     */
    fun stopRecognition()

    /**
     * Load custom vocabulary for better recognition.
     */
    suspend fun loadVocabulary(vocabulary: CustomVocabulary): Result<Unit>

    /**
     * Calibrate to user's voice (for accessibility).
     */
    suspend fun calibrate(samples: List<AudioSample>): CalibrationResult
}

enum class SpeechCapability {
    OFFLINE,            // Works without internet
    CONTINUOUS,         // Continuous listening mode
    WAKE_WORD,          // Wake word detection
    CUSTOM_VOCABULARY,  // Custom vocabulary support
    SPEAKER_ID,         // Speaker identification
    STREAMING,          // Streaming recognition
    NOISE_REDUCTION,    // Background noise handling
    PUNCTUATION,        // Auto punctuation
    PROFANITY_FILTER    // Profanity filtering
}

enum class RecognitionState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

data class RecognitionConfig(
    val language: String = "en-US",
    val continuous: Boolean = true,
    val partialResults: Boolean = true,
    val maxAlternatives: Int = 3,
    val profanityFilter: Boolean = false
)

data class RecognitionResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val alternatives: List<String> = emptyList(),
    val timestamp: Long = currentTimeMillis()
)
```

---

### Task 2.4: AccessibilityDataProvider Implementation

Create the implementation that provides plugins with access to VoiceOSCore data.

**Files to Create:**
```
Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/universal/data/
├── AccessibilityDataProvider.kt           # Interface (from architecture plan)
├── AccessibilityDataProviderImpl.kt       # Implementation
└── CachedAccessibilityData.kt             # Caching layer
```

**AccessibilityDataProvider.kt:**
```kotlin
interface AccessibilityDataProvider {

    // UI Element Data
    suspend fun getCurrentScreenElements(): List<QuantizedElement>
    suspend fun getElement(avid: String): QuantizedElement?

    // Command Data
    suspend fun getScreenCommands(): List<QuantizedCommand>
    suspend fun getCommandHistory(limit: Int = 100, successOnly: Boolean = false): List<CommandHistoryEntry>
    suspend fun getTopCommands(limit: Int = 20, context: String? = null): List<RankedCommand>

    // Screen Context
    suspend fun getScreenContext(): ScreenContext
    suspend fun getNavigationGraph(packageName: String): NavigationGraph

    // User Preferences
    suspend fun getContextPreferences(): List<ContextPreference>

    // Flow-based reactive updates
    val screenElementsFlow: StateFlow<List<QuantizedElement>>
    val screenContextFlow: StateFlow<ScreenContext?>
}
```

**Implementation approach:**
- Wrap existing VoiceOSCore repositories
- Add caching for hot data (current screen)
- Subscribe to accessibility events for real-time updates
- Provide Flow-based APIs for reactive UI

---

### Task 2.5: Plugin SDK Template

Create a plugin development template/SDK.

**Files to Create:**
```
Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/sdk/
├── BasePlugin.kt                  # Abstract base with common functionality
├── PluginContextBuilder.kt        # Context creation helper
├── PluginConfigLoader.kt          # AVU config loading
└── PluginTestHarness.kt           # Testing utilities
```

**BasePlugin.kt:**
```kotlin
abstract class BasePlugin : UniversalPlugin {

    protected lateinit var context: PluginContext
    protected lateinit var config: PluginConfig

    private val _stateFlow = MutableStateFlow(PluginState.UNINITIALIZED)
    override val stateFlow: StateFlow<PluginState> = _stateFlow.asStateFlow()
    override val state: PluginState get() = _stateFlow.value

    override suspend fun initialize(config: PluginConfig, context: PluginContext): InitResult {
        this.config = config
        this.context = context

        _stateFlow.value = PluginState.INITIALIZING

        return try {
            val result = onInitialize()
            if (result.isSuccess) {
                _stateFlow.value = PluginState.ACTIVE
            } else {
                _stateFlow.value = PluginState.FAILED
            }
            result
        } catch (e: Exception) {
            _stateFlow.value = PluginState.FAILED
            InitResult.Failure(e.message ?: "Unknown error")
        }
    }

    /** Override this for plugin-specific initialization */
    protected abstract suspend fun onInitialize(): InitResult

    override suspend fun activate(): Result<Unit> {
        _stateFlow.value = PluginState.ACTIVE
        return Result.success(Unit)
    }

    override suspend fun pause(): Result<Unit> {
        _stateFlow.value = PluginState.PAUSED
        return Result.success(Unit)
    }

    override suspend fun resume(): Result<Unit> {
        _stateFlow.value = PluginState.ACTIVE
        return Result.success(Unit)
    }

    override suspend fun shutdown(): Result<Unit> {
        _stateFlow.value = PluginState.STOPPING
        onShutdown()
        _stateFlow.value = PluginState.STOPPED
        return Result.success(Unit)
    }

    protected open suspend fun onShutdown() {}

    override fun healthCheck(): HealthStatus {
        return HealthStatus(
            healthy = state.isOperational(),
            message = "State: $state",
            diagnostics = getHealthDiagnostics(),
            lastCheckTime = currentTimeMillis(),
            checkDurationMs = 1
        )
    }

    protected open fun getHealthDiagnostics(): Map<String, Any> = emptyMap()

    override suspend fun onConfigurationChanged(config: Map<String, Any>) {}
    override suspend fun onEvent(event: PluginEvent) {}
}
```

---

### Task 2.6: First Plugin Migration (Prototype)

Migrate one existing handler as a proof of concept.

**Candidate:** NavigationHandler from VoiceOSCore

**Current location:** `Modules/VoiceOSCore/src/commonMain/.../handlers/NavigationHandler.kt`

**New location:**
```
Modules/PluginSystem/src/commonMain/.../plugins/builtin/
└── NavigationHandlerPlugin.kt
```

**Migration steps:**
1. Create `NavigationHandlerPlugin` extending `HandlerPlugin`
2. Wrap existing NavigationHandler logic
3. Register with UniversalPluginRegistry
4. Test via integration tests

---

## Key Architecture Decisions

1. **Contract Segregation:** Separate interfaces per module (voiceoscore/, ai/, speech/)
2. **Extension Pattern:** Module contracts extend UniversalPlugin, not replace it
3. **Data Provider:** Read-only access to VoiceOSCore data via AccessibilityDataProvider
4. **SDK Base Class:** BasePlugin provides lifecycle boilerplate
5. **Gradual Migration:** Existing handlers continue to work, plugins are additive

---

## File Structure After Phase 2

```
Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/
├── universal/
│   ├── UniversalPlugin.kt                    # (Phase 1)
│   ├── PluginCapability.kt                   # (Phase 1)
│   ├── PluginState.kt                        # (Phase 1)
│   ├── PluginTypes.kt                        # (Phase 1)
│   ├── UniversalPluginRegistry.kt            # (Phase 1)
│   ├── PluginEventBus.kt                     # (Phase 1)
│   ├── GrpcPluginEventBus.kt                 # (Phase 1)
│   ├── PluginLifecycleManager.kt             # (Phase 1)
│   ├── contracts/
│   │   ├── voiceoscore/
│   │   │   ├── HandlerPlugin.kt              # Task 2.1
│   │   │   ├── SynonymProviderPlugin.kt      # Task 2.1
│   │   │   ├── OverlayPlugin.kt              # Task 2.1
│   │   │   └── CommandPersistencePlugin.kt   # Task 2.1
│   │   ├── ai/
│   │   │   ├── LLMPlugin.kt                  # Task 2.2
│   │   │   ├── NLUPlugin.kt                  # Task 2.2
│   │   │   ├── EmbeddingPlugin.kt            # Task 2.2
│   │   │   └── RAGPlugin.kt                  # Task 2.2
│   │   └── speech/
│   │       ├── SpeechEnginePlugin.kt         # Task 2.3
│   │       ├── TTSPlugin.kt                  # Task 2.3
│   │       └── WakeWordPlugin.kt             # Task 2.3
│   └── data/
│       ├── AccessibilityDataProvider.kt      # Task 2.4
│       ├── AccessibilityDataProviderImpl.kt  # Task 2.4
│       └── CachedAccessibilityData.kt        # Task 2.4
├── sdk/
│   ├── BasePlugin.kt                         # Task 2.5
│   ├── PluginContextBuilder.kt               # Task 2.5
│   └── PluginTestHarness.kt                  # Task 2.5
└── builtin/
    └── NavigationHandlerPlugin.kt            # Task 2.6
```

---

## Dependencies & Imports

Phase 2 contracts will need imports from:

```kotlin
// VoiceOSCore types (existing)
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import com.augmentalis.voiceoscore.QuantizedNavigation

// Phase 1 universal types
import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.magiccode.plugins.universal.PluginCapability
import com.augmentalis.magiccode.plugins.universal.PluginState
import com.augmentalis.magiccode.plugins.universal.PluginContext
import com.augmentalis.magiccode.plugins.universal.PluginEvent

// Kotlin coroutines
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
```

---

## How to Continue

### Start Command
```bash
cd /Volumes/M-Drive/Coding/NewAvanues
git checkout AI-Architecture-Rework
```

### Prompt for New Session
```
Continue the Universal Plugin Architecture implementation. Read the handover:
Docs/AI/Handover/AI-Architecture-Handover-Phase2-260122.md

Phase 2 tasks:
1. Task 2.1: Create VoiceOSCore plugin contracts (HandlerPlugin, SynonymProviderPlugin, OverlayPlugin)
2. Task 2.2: Create AI plugin contracts (LLMPlugin, NLUPlugin, EmbeddingPlugin)
3. Task 2.3: Create Speech plugin contracts (SpeechEnginePlugin, TTSPlugin, WakeWordPlugin)
4. Task 2.4: Implement AccessibilityDataProvider
5. Task 2.5: Create Plugin SDK (BasePlugin, utilities)
6. Task 2.6: Migrate NavigationHandler as first plugin (prototype)

Use .swarm mode for parallel implementation of contracts.
```

---

## Swarm Recommendations

For efficient parallel execution:

| Agent | Tasks |
|-------|-------|
| Agent 1 | Task 2.1 - VoiceOSCore contracts |
| Agent 2 | Task 2.2 - AI contracts |
| Agent 3 | Task 2.3 - Speech contracts |
| Agent 4 | Task 2.4 - AccessibilityDataProvider |
| Agent 5 | Task 2.5 - SDK utilities |

Task 2.6 (migration) should wait until contracts are done.

---

## Estimated Scope

| Task | Files | LOC (est.) |
|------|-------|------------|
| 2.1 VoiceOSCore | 5 | ~400 |
| 2.2 AI | 5 | ~350 |
| 2.3 Speech | 4 | ~300 |
| 2.4 DataProvider | 3 | ~250 |
| 2.5 SDK | 4 | ~300 |
| 2.6 Migration | 1 | ~150 |
| **Total** | **22** | **~1,750** |

---

## Success Criteria

1. All contracts compile without errors
2. Contracts align with existing VoiceOSCore types
3. AccessibilityDataProvider wraps repositories correctly
4. BasePlugin reduces boilerplate significantly
5. NavigationHandlerPlugin works identically to original
6. Integration tests pass

---

**End of Phase 2 Handover Document**
