# VoiceOSCoreNG API Reference

**Version:** 2.0.0
**Date:** 2026-01-06
**Module:** `Modules/VoiceOSCoreNG`

---

## Table of Contents

1. [Core Entry Points](#1-core-entry-points)
2. [Common Data Types](#2-common-data-types)
3. [VUID Generation](#3-vuid-generation)
4. [Command System](#4-command-system)
5. [Element Processing](#5-element-processing)
6. [Framework Detection](#6-framework-detection)
7. [Feature Configuration](#7-feature-configuration)
8. [Speech Engine](#8-speech-engine)
9. [Action Execution](#9-action-execution)
10. [Repository Interfaces](#10-repository-interfaces)
11. [AVU Serialization](#11-avu-serialization)

---

## 1. Core Entry Points

### VoiceOSCoreNG (Android)

**Location:** `androidMain/kotlin/.../core/VoiceOSCoreNG.kt`

Main entry point for Android platform. Singleton object providing initialization and configuration.

```kotlin
object VoiceOSCoreNG {
    // Version info
    fun getVersion(): String           // Returns "2.0.0"
    fun getVersionCode(): Int          // Returns 2

    // Initialization
    fun initialize(
        tier: LearnAppDevToggle.Tier,
        isDebug: Boolean,
        enableTestMode: Boolean = false
    )
    fun isInitialized(): Boolean

    // Mode switching
    fun isDevMode(): Boolean
    fun isLiteMode(): Boolean
    fun isTestModeEnabled(): Boolean
    fun getCurrentTier(): LearnAppDevToggle.Tier
    fun setTier(tier: LearnAppDevToggle.Tier)
    fun toggle()

    // Test mode
    fun enableTestMode()
    fun disableTestMode()

    // Configuration
    fun configureLimits(
        maxElementsPerScan: Int? = null,
        maxAppsLearned: Int? = null,
        batchTimeoutMs: Long? = null,
        explorationDepth: Int? = null
    )

    fun configureFeatures(
        enableAI: Boolean? = null,
        enableNLU: Boolean? = null,
        enableExploration: Boolean? = null,
        enableFrameworkDetection: Boolean? = null,
        enableCaching: Boolean? = null,
        enableAnalytics: Boolean? = null,
        enableDebugOverlay: Boolean? = null
    )

    fun setProcessingMode(mode: LearnAppConfig.ProcessingMode?)

    // Feature checking
    fun isFeatureEnabled(feature: LearnAppDevToggle.Feature): Boolean
    fun isAIEnabled(): Boolean
    fun isNLUEnabled(): Boolean
    fun isExplorationEnabled(): Boolean

    // State
    fun getConfigSummary(): String
    fun getConfig(): LearnAppConfig.VariantConfig
    fun reset()  // For testing
}
```

**Example Usage:**

```kotlin
// Initialize in Application.onCreate()
VoiceOSCoreNG.initialize(
    tier = LearnAppDevToggle.Tier.LITE,
    isDebug = BuildConfig.DEBUG
)

// Configure for specific use case
VoiceOSCoreNG.configureLimits(
    maxElementsPerScan = 200,
    maxAppsLearned = 50
)

// Check current state
if (VoiceOSCoreNG.isAIEnabled()) {
    // Use AI features
}

// Print configuration
println(VoiceOSCoreNG.getConfigSummary())
```

---

## 2. Common Data Types

### ElementInfo

**Location:** `commonMain/kotlin/.../common/ElementInfo.kt`

Core data class representing a UI element for voice accessibility processing.

```kotlin
data class ElementInfo(
    val className: String,
    val resourceId: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val bounds: Bounds = Bounds.EMPTY,
    val isClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEnabled: Boolean = true,
    val packageName: String = ""
) {
    // Computed properties
    val voiceLabel: String          // Best label for voice recognition
    val hasVoiceContent: Boolean    // Has meaningful content for targeting
    val isActionable: Boolean       // Is clickable or scrollable

    companion object {
        val EMPTY: ElementInfo
        fun button(text: String, resourceId: String = "", bounds: Bounds = Bounds.EMPTY, packageName: String = ""): ElementInfo
        fun input(hint: String = "", resourceId: String = "", bounds: Bounds = Bounds.EMPTY, packageName: String = ""): ElementInfo
    }
}
```

### Bounds

**Location:** `commonMain/kotlin/.../common/ElementInfo.kt`

Represents screen coordinates of a UI element.

```kotlin
data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int      // right - left
    val height: Int     // bottom - top
    val centerX: Int    // left + width / 2
    val centerY: Int    // top + height / 2

    companion object {
        val EMPTY: Bounds
        fun fromString(boundsStr: String): Bounds?
    }

    override fun toString(): String  // "left,top,right,bottom"
}
```

### FrameworkType

**Location:** `commonMain/kotlin/.../common/FrameworkInfo.kt`

Enumeration of supported application frameworks.

```kotlin
enum class FrameworkType {
    NATIVE,          // Android/iOS native
    FLUTTER,         // Flutter (Dart)
    UNITY,           // Unity game engine
    UNREAL_ENGINE,   // Unreal Engine
    REACT_NATIVE,    // React Native
    WEBVIEW,         // WebView-based hybrid
    UNKNOWN          // Could not determine
}
```

### FrameworkInfo

**Location:** `commonMain/kotlin/.../common/FrameworkInfo.kt`

Contains detected framework information.

```kotlin
data class FrameworkInfo(
    val type: FrameworkType,
    val version: String? = null,
    val packageIndicators: List<String> = emptyList()
)
```

---

## 3. VUID Generation

### VUIDGenerator

**Location:** `commonMain/kotlin/.../common/VUIDGenerator.kt`

Generates and manages Voice Unique Identifiers (VUIDs).

```kotlin
object VUIDGenerator {
    // Compact VUID generation (16 chars)
    fun generate(
        packageName: String,
        typeCode: VUIDTypeCode,
        elementHash: String
    ): String

    fun generatePackageHash(packageName: String): String  // 6-char hash

    // Validation
    fun isValidVUID(vuid: String): Boolean
    fun parseVUID(vuid: String): VUIDComponents?

    // Type detection
    fun getTypeCode(className: String): VUIDTypeCode

    // Simple format (module:type:hash8)
    fun generateSimple(module: String, typeCode: VUIDTypeCode): String
    fun generateRandomHash8(): String
    fun isSimpleFormat(vuid: String): Boolean

    // Legacy format detection
    fun isLegacyUuid(vuid: String): Boolean
    fun isLegacyVoiceOS(vuid: String): Boolean
    fun isValid(vuid: String): Boolean  // Any valid format

    // Format detection
    fun detectFormat(vuid: String): VuidFormat

    // Migration
    fun migrateToCompact(legacyVuid: String): String?
    fun extractHash(vuid: String): String?

    // Convenience generators
    fun generateMessageVuid(): String
    fun generateConversationVuid(): String
    fun generateDocumentVuid(): String
    fun generateMemoryVuid(): String
    fun generateTabVuid(): String
    fun generateFavoriteVuid(): String
    fun generateDownloadVuid(): String
    fun generateHistoryVuid(): String
    fun generateSessionVuid(): String
    fun generateGroupVuid(): String
    fun generateRequestVuid(): String
    fun generateWindowVuid(): String
    fun generateStreamVuid(): String
    fun generatePresetVuid(): String
    fun generateDeviceVuid(): String
    fun generateSyncVuid(): String
}
```

### VUIDTypeCode

**Location:** `commonMain/kotlin/.../common/VUIDGenerator.kt`

Type codes for VUID generation.

```kotlin
enum class VUIDTypeCode(val code: Char, val abbrev: String) {
    BUTTON('b', "btn"),
    INPUT('i', "inp"),
    SCROLL('s', "scr"),
    TEXT('t', "txt"),
    ELEMENT('e', "elm"),
    CARD('c', "crd"),
    LAYOUT('l', "lay"),
    MENU('m', "mnu"),
    DIALOG('d', "dlg"),
    IMAGE('g', "img"),
    CHECKBOX('k', "chk"),
    SWITCH('w', "swt"),
    LIST('z', "lst"),
    SLIDER('r', "sld"),
    TAB('a', "tab");

    companion object {
        fun fromCode(code: Char): VUIDTypeCode?
        fun fromAbbrev(abbrev: String): VUIDTypeCode?
        fun validCodes(): Set<Char>
    }
}
```

### VUIDComponents

**Location:** `commonMain/kotlin/.../common/VUIDGenerator.kt`

Parsed components of a VUID.

```kotlin
data class VUIDComponents(
    val packageHash: String,
    val typeCode: VUIDTypeCode,
    val elementHash: String
) {
    fun toVUID(): String
}
```

### VuidFormat

**Location:** `commonMain/kotlin/.../common/VUIDGenerator.kt`

VUID format types for detection.

```kotlin
enum class VuidFormat {
    COMPACT,         // a3f2e1-b917cc9dc (16 chars)
    SIMPLE,          // ava:msg:a7f3e2c1
    LEGACY_UUID,     // 550e8400-e29b-41d4-a716-446655440000
    LEGACY_VOICEOS,  // com.pkg.v1.0.0.button-a7f3e2c1d4b5
    UNKNOWN
}
```

**Example Usage:**

```kotlin
// Generate compact VUID
val vuid = VUIDGenerator.generate(
    packageName = "com.example.myapp",
    typeCode = VUIDTypeCode.BUTTON,
    elementHash = "submit_button"
)
// Result: "a3f2e1-b917cc9dc"

// Auto-detect type from class name
val typeCode = VUIDGenerator.getTypeCode("MaterialButton")
// Result: VUIDTypeCode.BUTTON

// Parse existing VUID
val components = VUIDGenerator.parseVUID("a3f2e1-b917cc9dc")
// Result: VUIDComponents(packageHash="a3f2e1", typeCode=BUTTON, elementHash="917cc9dc")

// Generate simple format for internal use
val messageId = VUIDGenerator.generateMessageVuid()
// Result: "ava:elm:f2c1a7e3"

// Migrate legacy VUID
val newVuid = VUIDGenerator.migrateToCompact("com.app.v1.0.0.button-a7f3e2c1d4b5")
// Result: "a3f2e1-ba7f3e2c1"
```

---

## 4. Command System

### CommandGenerator

**Location:** `commonMain/kotlin/.../command/CommandGenerator.kt`

Generates voice commands from UI elements.

```kotlin
object CommandGenerator {
    /**
     * Generate command from element during extraction.
     * Returns null if element is not actionable or has no useful label.
     */
    fun fromElement(
        element: ElementInfo,
        packageName: String
    ): QuantizedCommand?
}
```

### QuantizedCommand

**Location:** `commonMain/kotlin/.../avu/QuantizedCommand.kt`

Voice command representation for AVU format.

```kotlin
data class QuantizedCommand(
    val uuid: String = "",
    val phrase: String,
    val actionType: CommandActionType,
    val targetVuid: String?,
    val confidence: Float,
    val metadata: Map<String, String> = emptyMap()
) {
    val vuid: String           // Alias for uuid
    val packageName: String?   // From metadata
    val screenId: String?      // From metadata
    val appVersion: String?    // From metadata

    fun withMetadata(key: String, value: String): QuantizedCommand
    fun withMetadata(entries: Map<String, String>): QuantizedCommand
    fun toCmdLine(): String    // AVU format

    companion object {
        fun fromCmdLine(line: String): QuantizedCommand?
    }
}
```

### CommandActionType

**Location:** `commonMain/kotlin/.../avu/CommandActionType.kt`

Types of actions that can be performed by voice commands.

```kotlin
enum class CommandActionType {
    CLICK,
    LONG_CLICK,
    TYPE,
    NAVIGATE,
    CUSTOM;

    companion object {
        fun fromString(value: String): CommandActionType
    }
}
```

### CommandRegistry

**Location:** `commonMain/kotlin/.../command/CommandRegistry.kt`

Registry for dynamic command management.

```kotlin
interface CommandRegistry {
    fun register(command: QuantizedCommand)
    fun unregister(vuid: String)
    fun getByPhrase(phrase: String): QuantizedCommand?
    fun getByVuid(vuid: String): QuantizedCommand?
    fun getAll(): List<QuantizedCommand>
    fun clear()
}
```

### CommandMatcher

**Location:** `commonMain/kotlin/.../command/CommandMatcher.kt`

Matches spoken phrases to registered commands.

```kotlin
object CommandMatcher {
    fun findBestMatch(
        phrase: String,
        commands: List<QuantizedCommand>,
        threshold: Float = 0.7f
    ): QuantizedCommand?

    fun findAllMatches(
        phrase: String,
        commands: List<QuantizedCommand>,
        threshold: Float = 0.5f
    ): List<Pair<QuantizedCommand, Float>>
}
```

**Example Usage:**

```kotlin
// Generate command from element
val element = ElementInfo(
    className = "Button",
    text = "Submit Order",
    resourceId = "com.app:id/submit_btn",
    bounds = Bounds(0, 0, 200, 50),
    isClickable = true,
    packageName = "com.example.shop"
)

val command = CommandGenerator.fromElement(element, "com.example.shop")
// Result: QuantizedCommand(
//     phrase = "click Submit Order",
//     actionType = CLICK,
//     targetVuid = "a3f2e1-b917cc9dc",
//     confidence = 0.95
// )

// Convert to AVU format
val avuLine = command.toCmdLine()
// Result: "CMD::click Submit Order:CLICK:a3f2e1-b917cc9dc:0.95"
```

---

## 5. Element Processing

### ElementParser

**Location:** `commonMain/kotlin/.../extraction/ElementParser.kt`

Shared HTML and accessibility parsing for KMP.

```kotlin
object ElementParser {
    // HTML parsing
    fun parseHtml(html: String): List<ElementInfo>

    // Accessibility JSON parsing
    fun parseAccessibilityJson(json: String): List<ElementInfo>

    // XPath generation
    fun generateXPath(element: ElementInfo): String

    // Filtering
    fun filterActionable(elements: List<ElementInfo>): List<ElementInfo>
    fun filterWithContent(elements: List<ElementInfo>): List<ElementInfo>
    fun deduplicate(elements: List<ElementInfo>): List<ElementInfo>
}
```

### QuantizedElement

**Location:** `commonMain/kotlin/.../avu/QuantizedElement.kt`

Compact representation of a UI element for AVU format.

```kotlin
data class QuantizedElement(
    val vuid: String,
    val type: ElementType,
    val label: String,
    val aliases: List<String> = emptyList(),
    val bounds: String = "",
    val actions: String = "",
    val category: String = ""
) {
    fun toElmLine(): String  // AVU ELM format

    companion object {
        fun fromElementInfo(elementInfo: ElementInfo, vuid: String): QuantizedElement
    }
}
```

### ElementType

**Location:** `commonMain/kotlin/.../avu/ElementType.kt`

Classified element types.

```kotlin
enum class ElementType {
    BUTTON,
    TEXT_FIELD,
    CHECKBOX,
    RADIO,
    SWITCH,
    SLIDER,
    DROPDOWN,
    LIST_ITEM,
    IMAGE,
    TEXT,
    CONTAINER,
    UNKNOWN;

    companion object {
        fun fromClassName(className: String): ElementType
    }
}
```

**Example Usage:**

```kotlin
// Parse HTML from WebView
val html = "<button id='submit'>Submit</button><input name='email' placeholder='Email'>"
val elements = ElementParser.parseHtml(html)
// Result: [
//     ElementInfo(className="button", resourceId="submit", text="Submit", ...),
//     ElementInfo(className="input", resourceId="email", contentDescription="Email", ...)
// ]

// Filter actionable elements
val actionable = ElementParser.filterActionable(elements)

// Generate XPath for targeting
val xpath = ElementParser.generateXPath(elements[0])
// Result: "//button[@id='submit']"
```

---

## 6. Framework Detection

### FrameworkDetector

**Location:** `commonMain/kotlin/.../common/FrameworkInfo.kt`

Detects application framework type.

```kotlin
object FrameworkDetector {
    /**
     * Detect framework based on package name and class names.
     *
     * Priority order:
     * 1. Flutter (io.flutter.)
     * 2. Unity (com.unity3d.)
     * 3. Unreal Engine (com.epicgames.)
     * 4. React Native (com.facebook.react.)
     * 5. WebView (android.webkit., org.xwalk., org.chromium., org.apache.cordova.)
     * 6. Native (default)
     */
    fun detect(packageName: String, classNames: List<String>): FrameworkInfo
}
```

### FrameworkHandler

**Location:** `commonMain/kotlin/.../handlers/FrameworkHandler.kt`

Interface for framework-specific element handlers.

```kotlin
interface FrameworkHandler {
    val frameworkType: FrameworkType

    fun canHandle(elements: List<ElementInfo>): Boolean
    fun processElements(elements: List<ElementInfo>): List<ElementInfo>
    fun getSelectors(): List<String>
    fun isActionable(element: ElementInfo): Boolean
    fun getPriority(): Int
}

data class FrameworkHandlingResult(
    val frameworkType: FrameworkType,
    val processedElements: List<ElementInfo>,
    val actionableCount: Int,
    val metadata: Map<String, String> = emptyMap()
)

object FrameworkHandlerRegistry {
    fun register(handler: FrameworkHandler)
    fun unregister(handler: FrameworkHandler)
    fun getHandlers(): List<FrameworkHandler>
    fun findHandler(elements: List<ElementInfo>): FrameworkHandler?
    fun getHandler(type: FrameworkType): FrameworkHandler?
    fun clear()
    fun registerDefaults()
}
```

**Available Handlers:**
- `FlutterHandler` - Handles Flutter apps
- `UnityHandler` - Handles Unity games
- `ReactNativeHandler` - Handles React Native apps
- `WebViewHandler` - Handles WebView-based hybrid apps
- `NativeHandler` - Handles native Android/iOS apps

**Example Usage:**

```kotlin
// Detect framework
val framework = FrameworkDetector.detect(
    packageName = "com.flutter.myapp",
    classNames = listOf(
        "io.flutter.embedding.FlutterActivity",
        "io.flutter.view.FlutterView"
    )
)
// Result: FrameworkInfo(type=FLUTTER, packageIndicators=[...])

// Use framework-specific handler
FrameworkHandlerRegistry.registerDefaults()
val handler = FrameworkHandlerRegistry.findHandler(elements)
val processed = handler?.processElements(elements) ?: elements
```

---

## 7. Feature Configuration

### LearnAppDevToggle

**Location:** `commonMain/kotlin/.../features/LearnAppDevToggle.kt`

Feature flag system for VoiceOSCoreNG.

```kotlin
object LearnAppDevToggle {
    enum class Tier {
        LITE,  // Free tier with basic features
        DEV    // Full feature set
    }

    enum class Category {
        CORE, JIT, EXPLORATION, AI, DEV_TOOLS, ANALYTICS, EXPERIMENTAL
    }

    enum class Feature(
        val tier: Tier,
        val category: Category,
        val description: String,
        val defaultEnabled: Boolean = true
    ) {
        // Core (LITE)
        ELEMENT_SCRAPING, VUID_GENERATION, NATIVE_DETECTION, VOICE_COMMANDS,

        // JIT (LITE)
        JIT_PROCESSING, JIT_COMMANDS,

        // Exploration (DEV)
        EXPLORATION_MODE, BATCH_PROCESSING, SCREEN_CACHING,

        // Framework Detection (DEV)
        FLUTTER_DETECTION, UNITY_DETECTION, REACT_NATIVE_DETECTION, WEBVIEW_HANDLING,

        // AI (DEV)
        AI_CLASSIFICATION, AI_NAMING, AI_SUGGESTIONS,

        // Dev Tools (DEV)
        DEBUG_OVERLAY, ELEMENT_INSPECTOR, VUID_VIEWER, PERFORMANCE_PROFILER,

        // Analytics (DEV)
        USAGE_ANALYTICS, COMMAND_METRICS,

        // Experimental (DEV)
        HIERARCHY_MAP, CROSS_APP_LEARNING;

        fun isAvailableIn(checkTier: Tier): Boolean
    }

    // Configuration
    fun initialize(tier: Tier, isDebug: Boolean)
    fun setTier(tier: Tier)
    fun getCurrentTier(): Tier
    fun isDebug(): Boolean
    fun isDevMode(): Boolean
    fun isLiteMode(): Boolean
    fun toggle()

    // Feature checking
    fun isEnabled(feature: Feature): Boolean
    inline fun isFeatureEnabled(feature: Feature): Boolean
    inline fun <T> ifEnabled(feature: Feature, action: () -> T): T?
    inline fun <T> ifEnabledOrElse(feature: Feature, enabled: () -> T, disabled: () -> T): T

    // Overrides
    fun setOverride(feature: Feature, enabled: Boolean)
    fun removeOverride(feature: Feature)
    fun clearOverrides()
    fun getOverrides(): Map<Feature, Boolean>

    // Querying
    fun getFeaturesByCategory(category: Category): List<Feature>
    fun getFeaturesByTier(tier: Tier): List<Feature>
    fun getEnabledFeatures(): List<Feature>
    fun getDisabledFeatures(): List<Feature>
    fun isCategoryEnabled(category: Category): Boolean

    // Listeners
    fun addTierChangeListener(listener: (Tier) -> Unit)
    fun removeTierChangeListener(listener: (Tier) -> Unit)

    // Testing
    fun reset()
}
```

### LearnAppConfig

**Location:** `commonMain/kotlin/.../features/LearnAppConfig.kt`

Configuration for LearnApp feature variants.

```kotlin
object LearnAppConfig {
    enum class ProcessingMode {
        IMMEDIATE,  // Process elements immediately
        BATCH,      // Process in batches
        HYBRID      // Immediate for common, batch for complex
    }

    data class VariantConfig(
        val name: String,
        val tier: LearnAppDevToggle.Tier,
        var processingMode: ProcessingMode,
        var maxElementsPerScan: Int,
        var maxAppsLearned: Int,
        var enableAI: Boolean,
        var enableNLU: Boolean,
        var enableExploration: Boolean,
        var enableFrameworkDetection: Boolean,
        var cacheEnabled: Boolean,
        var analyticsEnabled: Boolean,
        var batchTimeoutMs: Long = 5000L,
        var explorationDepth: Int = 10,
        var enableDebugOverlay: Boolean = false
    )

    object DeveloperSettings {
        var enabled: Boolean
        var maxElementsPerScan: Int?
        var maxAppsLearned: Int?
        var forceEnableAI: Boolean
        var forceEnableNLU: Boolean
        var forceEnableExploration: Boolean
        var forceEnableFrameworkDetection: Boolean
        var forceEnableCaching: Boolean
        var forceEnableAnalytics: Boolean
        var batchTimeoutMs: Long?
        var explorationDepth: Int?
        var enableDebugOverlay: Boolean
        var processingModeOverride: ProcessingMode?

        fun enable(unlockAll: Boolean = false)
        fun disable()
        fun reset()
        fun getSummary(): String
    }

    const val UNLIMITED = -1

    object LiteDefaults {
        const val MAX_ELEMENTS_PER_SCAN = 100
        const val MAX_APPS_LEARNED = 25
        const val BATCH_TIMEOUT_MS = 3000L
        const val EXPLORATION_DEPTH = 5
    }

    object DevDefaults {
        const val MAX_ELEMENTS_PER_SCAN = 500
        const val MAX_APPS_LEARNED = UNLIMITED
        const val BATCH_TIMEOUT_MS = 5000L
        const val EXPLORATION_DEPTH = 20
    }

    // Configuration access
    fun getConfig(): VariantConfig
    fun getEffectiveConfig(): VariantConfig
    fun setVariant(tier: LearnAppDevToggle.Tier)
    fun applyCustomConfig(config: VariantConfig)

    // Mode checking
    fun isLite(): Boolean
    fun isDev(): Boolean

    // Feature checking
    fun getProcessingMode(): ProcessingMode
    fun getMaxElementsPerScan(): Int
    fun getMaxAppsLearned(): Int
    fun isAIEnabled(): Boolean
    fun isNLUEnabled(): Boolean
    fun isExplorationEnabled(): Boolean
    fun isFrameworkDetectionEnabled(): Boolean
    fun isCacheEnabled(): Boolean
    fun isAnalyticsEnabled(): Boolean
    fun isDebugOverlayEnabled(): Boolean
    fun getBatchTimeoutMs(): Long
    fun getExplorationDepth(): Int

    // Summary
    fun getSummary(): String

    // Listeners
    fun addConfigChangeListener(listener: (VariantConfig) -> Unit)
    fun removeConfigChangeListener(listener: (VariantConfig) -> Unit)

    // Testing
    fun reset()
    fun enableTestMode()
}
```

**Example Usage:**

```kotlin
// Check feature availability
if (LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.FLUTTER_DETECTION)) {
    // Use Flutter detection
}

// Conditional execution
LearnAppDevToggle.ifEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION) {
    classifyWithAI(elements)
}

// Get configuration limits
val maxElements = LearnAppConfig.getMaxElementsPerScan()
val mode = LearnAppConfig.getProcessingMode()

// Enable developer overrides
LearnAppConfig.DeveloperSettings.enable(unlockAll = false)
LearnAppConfig.DeveloperSettings.maxElementsPerScan = 300

// Print configuration summary
println(LearnAppConfig.getSummary())
```

---

## 8. Speech Engine

### ISpeechEngine

**Location:** `commonMain/kotlin/.../speech/ISpeechEngine.kt`

Unified interface for speech recognition engines.

```kotlin
interface ISpeechEngine {
    val state: StateFlow<EngineState>
    val results: Flow<SpeechResult>
    val errors: Flow<SpeechError>

    suspend fun initialize(config: SpeechConfig): Result<Unit>
    suspend fun startListening(): Result<Unit>
    suspend fun stopListening()
    suspend fun updateCommands(commands: List<String>): Result<Unit>
    suspend fun updateConfiguration(config: SpeechConfig): Result<Unit>

    fun isRecognizing(): Boolean
    fun isInitialized(): Boolean
    fun getEngineType(): SpeechEngine
    fun getSupportedFeatures(): Set<EngineFeature>

    suspend fun destroy()
}

sealed class EngineState {
    data object Uninitialized : EngineState()
    data object Initializing : EngineState()
    data class Ready(val engineType: SpeechEngine) : EngineState()
    data object Listening : EngineState()
    data object Processing : EngineState()
    data class Error(val message: String, val recoverable: Boolean) : EngineState()
    data object Destroyed : EngineState()

    val isReady: Boolean
    val isListening: Boolean
    val isProcessing: Boolean
}

data class SpeechResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val timestamp: Long,
    val alternatives: List<Alternative> = emptyList()
)

data class SpeechError(
    val code: ErrorCode,
    val message: String,
    val recoverable: Boolean,
    val timestamp: Long
) {
    enum class ErrorCode {
        NOT_INITIALIZED, AUDIO_ERROR, NETWORK_ERROR, PERMISSION_DENIED,
        NO_SPEECH_DETECTED, RECOGNITION_FAILED, MODEL_NOT_FOUND,
        ENGINE_BUSY, TIMEOUT, UNKNOWN
    }
}

enum class EngineFeature {
    OFFLINE_MODE, CONTINUOUS_RECOGNITION, WORD_TIMESTAMPS, SPEAKER_DIARIZATION,
    LANGUAGE_DETECTION, TRANSLATION, CUSTOM_VOCABULARY, WAKE_WORD,
    PUNCTUATION, PROFANITY_FILTER
}
```

### SpeechConfig

**Location:** `commonMain/kotlin/.../speech/SpeechConfig.kt`

Configuration for speech engines.

```kotlin
data class SpeechConfig(
    val language: String = "en-US",
    val mode: SpeechMode = SpeechMode.COMMAND,
    val continuous: Boolean = true,
    val offlineMode: Boolean = false,
    val maxResults: Int = 5,
    val partialResults: Boolean = true,
    val silenceTimeoutMs: Long = 2000,
    val commands: List<String> = emptyList()
)

enum class SpeechMode {
    COMMAND,     // Optimized for short commands
    DICTATION,   // Optimized for longer text
    HYBRID       // Mixed mode
}
```

### ISpeechEngineFactory

**Location:** `commonMain/kotlin/.../speech/ISpeechEngineFactory.kt`

Factory interface for creating speech engines.

```kotlin
interface ISpeechEngineFactory {
    fun create(engineType: SpeechEngine): ISpeechEngine
    fun isAvailable(engineType: SpeechEngine): Boolean
    fun getAvailableEngines(): List<SpeechEngine>
}

enum class SpeechEngine {
    ANDROID_STT,    // Android SpeechRecognizer
    VIVOKA,         // Vivoka speech engine
    WHISPER,        // OpenAI Whisper
    VOSK,           // Vosk offline
    CUSTOM          // Custom implementation
}
```

**Example Usage:**

```kotlin
// Initialize speech engine
val engine = factory.create(SpeechEngine.ANDROID_STT)
engine.initialize(SpeechConfig(
    language = "en-US",
    mode = SpeechMode.COMMAND,
    commands = listOf("click submit", "scroll down", "go back")
))

// Collect results
engine.results.collect { result ->
    if (result.isFinal) {
        handleCommand(result.text)
    }
}

// Start listening
engine.startListening()

// Update commands when screen changes
engine.updateCommands(newCommands)
```

---

## 9. Action Execution

### IActionExecutor

**Location:** `commonMain/kotlin/.../execution/IActionExecutor.kt`

Interface for executing voice command actions.

```kotlin
interface IActionExecutor {
    // Element Actions
    suspend fun tap(vuid: String): ActionResult
    suspend fun longPress(vuid: String, durationMs: Long = 500L): ActionResult
    suspend fun focus(vuid: String): ActionResult
    suspend fun enterText(text: String, vuid: String? = null): ActionResult

    // Scroll Actions
    suspend fun scroll(
        direction: ScrollDirection,
        amount: Float = 0.5f,
        vuid: String? = null
    ): ActionResult

    // Navigation Actions
    suspend fun back(): ActionResult
    suspend fun home(): ActionResult
    suspend fun recentApps(): ActionResult
    suspend fun appDrawer(): ActionResult

    // System Actions
    suspend fun openSettings(): ActionResult
    suspend fun showNotifications(): ActionResult
    suspend fun clearNotifications(): ActionResult
    suspend fun screenshot(): ActionResult
    suspend fun flashlight(on: Boolean): ActionResult

    // Media Actions
    suspend fun mediaPlayPause(): ActionResult
    suspend fun mediaNext(): ActionResult
    suspend fun mediaPrevious(): ActionResult
    suspend fun volume(direction: VolumeDirection): ActionResult

    // App Actions
    suspend fun openApp(appType: String): ActionResult
    suspend fun openAppByPackage(packageName: String): ActionResult
    suspend fun closeApp(): ActionResult

    // Generic Execution
    suspend fun executeCommand(command: QuantizedCommand): ActionResult
    suspend fun executeAction(
        actionType: CommandActionType,
        params: Map<String, Any> = emptyMap()
    ): ActionResult

    // Element Lookup
    suspend fun elementExists(vuid: String): Boolean
    suspend fun getElementBounds(vuid: String): ElementBounds?
}

enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }
enum class VolumeDirection { UP, DOWN, MUTE, UNMUTE }

data class ElementBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
    val height: Int
    val centerX: Int
    val centerY: Int
}
```

### ActionResult

**Location:** `commonMain/kotlin/.../execution/ActionResult.kt`

Result of an action execution.

```kotlin
sealed class ActionResult {
    data class Success(
        val message: String = "",
        val data: Map<String, Any> = emptyMap()
    ) : ActionResult()

    data class Failure(
        val error: String,
        val code: ErrorCode = ErrorCode.UNKNOWN,
        val recoverable: Boolean = true
    ) : ActionResult()

    enum class ErrorCode {
        ELEMENT_NOT_FOUND, PERMISSION_DENIED, ACTION_FAILED,
        TIMEOUT, NOT_SUPPORTED, UNKNOWN
    }

    val isSuccess: Boolean
    val isFailure: Boolean
}
```

**Example Usage:**

```kotlin
// Execute tap on element
val result = executor.tap("a3f2e1-b917cc9dc")
when (result) {
    is ActionResult.Success -> println("Tapped successfully")
    is ActionResult.Failure -> println("Failed: ${result.error}")
}

// Execute voice command
val command = QuantizedCommand(
    phrase = "click submit",
    actionType = CommandActionType.CLICK,
    targetVuid = "a3f2e1-b917cc9dc",
    confidence = 0.95f
)
executor.executeCommand(command)

// System actions
executor.back()
executor.scroll(ScrollDirection.DOWN, amount = 0.5f)
```

---

## 10. Repository Interfaces

### ICommandRepository

**Location:** `commonMain/kotlin/.../repository/ICommandRepository.kt`

Repository for voice command storage.

```kotlin
interface ICommandRepository {
    suspend fun save(command: QuantizedCommand): Result<Unit>
    suspend fun saveAll(commands: List<QuantizedCommand>): Result<Unit>
    suspend fun getByApp(packageName: String): List<QuantizedCommand>
    suspend fun getByScreen(packageName: String, screenId: String): List<QuantizedCommand>
    suspend fun getByVuid(vuid: String): QuantizedCommand?
    suspend fun deleteByScreen(packageName: String, screenId: String): Result<Unit>
    suspend fun deleteByApp(packageName: String): Result<Unit>
    fun observeByScreen(packageName: String, screenId: String): Flow<List<QuantizedCommand>>
    suspend fun countByApp(packageName: String): Long
}

// In-memory implementation for testing
class InMemoryCommandRepository : ICommandRepository
```

### IVuidRepository

**Location:** `commonMain/kotlin/.../repository/IVuidRepository.kt`

Repository for VUID mappings.

```kotlin
interface IVuidRepository {
    suspend fun save(vuid: String, elementInfo: ElementInfo): Result<Unit>
    suspend fun get(vuid: String): ElementInfo?
    suspend fun delete(vuid: String): Result<Unit>
    suspend fun getByPackage(packageName: String): List<Pair<String, ElementInfo>>
    suspend fun exists(vuid: String): Boolean
}
```

**Example Usage:**

```kotlin
// Save command
val command = CommandGenerator.fromElement(element, packageName)
commandRepository.save(command!!)

// Query commands for current screen
val commands = commandRepository.getByScreen("com.example.app", "HomeScreen")

// Observe commands reactively
commandRepository.observeByScreen("com.example.app", "HomeScreen")
    .collect { commands ->
        updateUI(commands)
    }
```

---

## 11. AVU Serialization

### AVUSerializer

**Location:** `commonMain/kotlin/.../avu/AVUSerializer.kt`

Serializes/deserializes AVU format.

```kotlin
object AVUSerializer {
    fun serialize(screen: QuantizedScreen): String
    fun deserialize(avu: String): QuantizedScreen?

    fun serializeElement(element: QuantizedElement): String
    fun deserializeElement(line: String): QuantizedElement?

    fun serializeCommand(command: QuantizedCommand): String
    fun deserializeCommand(line: String): QuantizedCommand?
}
```

### QuantizedScreen

**Location:** `commonMain/kotlin/.../avu/QuantizedScreen.kt`

Screen representation for AVU format.

```kotlin
data class QuantizedScreen(
    val screenId: String,
    val packageName: String,
    val elements: List<QuantizedElement>,
    val commands: List<QuantizedCommand>,
    val navigation: QuantizedNavigation? = null,
    val metadata: Map<String, String> = emptyMap()
)
```

### QuantizedContext

**Location:** `commonMain/kotlin/.../avu/QuantizedContext.kt`

Context representation for AVU format.

```kotlin
data class QuantizedContext(
    val screens: List<QuantizedScreen>,
    val globalCommands: List<QuantizedCommand>,
    val appInfo: Map<String, String>
)
```

**Example AVU Format:**

```
SCR:home_screen:com.example.app
ELM:a3f2e1-b917cc9dc:Submit:BUTTON:click:0,0,200,50:action
ELM:b4g3f2-i123456ab:Email:TEXT_FIELD:click:0,60,200,110:input
CMD::click Submit:CLICK:a3f2e1-b917cc9dc:0.95
CMD::type email:TYPE:b4g3f2-i123456ab:0.90
NAV:back:go_back|home:home_screen
```

---

## Thread Safety Notes

1. **Immutable Data Classes**: All data classes (`ElementInfo`, `QuantizedCommand`, etc.) are immutable and thread-safe.

2. **Object Singletons**: `VUIDGenerator`, `CommandGenerator`, `ElementParser`, `FrameworkDetector` are stateless and thread-safe.

3. **Configuration Objects**: `LearnAppDevToggle`, `LearnAppConfig` manage internal state. Access from main thread or use proper synchronization.

4. **Repository Operations**: All repository methods are `suspend` functions - use them from coroutines.

5. **Flow-Based APIs**: Speech results and reactive queries use Kotlin Flow - collect from appropriate scope.

6. **Handler Registry**: `FrameworkHandlerRegistry` uses internal synchronization for registration operations.

---

## Error Handling Patterns

```kotlin
// Repository operations return Result
val result = commandRepository.save(command)
result.fold(
    onSuccess = { /* saved */ },
    onFailure = { e -> /* handle error */ }
)

// Action execution returns sealed class
when (val result = executor.tap(vuid)) {
    is ActionResult.Success -> handleSuccess(result)
    is ActionResult.Failure -> handleFailure(result)
}

// Speech engine uses Flow for errors
engine.errors.collect { error ->
    when (error.code) {
        SpeechError.ErrorCode.PERMISSION_DENIED -> requestPermission()
        SpeechError.ErrorCode.NETWORK_ERROR -> showOfflineMessage()
        else -> logError(error)
    }
}
```

---

## Related Documentation

- README: `Modules/VoiceOSCoreNG/README.md`
- Migration Guide: `Docs/VoiceOS/manuals/developer/VoiceOSCoreNG-Migration-Guide-60106-V1.md`

---

**Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC**
