# Universal Plugin Architecture Plan
**Version:** 1.0
**Date:** 2026-01-22
**Status:** DRAFT
**Author:** Architecture Team
**Goal:** Create a universal plugin system for accessibility-first voice/gaze control

---

## Executive Summary

This document defines a **Universal Plugin Architecture** that extends the existing `Modules/PluginSystem` to work across all 22 modules in the codebase. The primary goal is enabling **hand-challenged users** to control their devices through **voice and gaze input** by leveraging the rich accessibility data captured in VoiceOSCore databases.

### Key Principles
1. **Accessibility-First**: Every plugin must consider voice/gaze input users
2. **Universal Contracts**: One plugin system, module-specific extensions
3. **AVU Format**: Line-based configuration instead of JSON
4. **KMP-First**: Kotlin Multiplatform now, optional Rust acceleration later
5. **Progressive Enhancement**: Plugins can add capabilities without breaking existing functionality

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     UNIVERSAL PLUGIN ARCHITECTURE                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ PLUGIN MANIFEST (AVU Format)                                       │ │
│  │ ─────────────────────────────────────────────────────────────────  │ │
│  │ PLG:com.example.myplugin:1.0.0:MyPlugin                           │ │
│  │ CAP:accessibility.voice|ai.nlu|speech.engine                       │ │
│  │ DEP:com.augmentalis.core:^2.0.0                                    │ │
│  │ PRM:MICROPHONE:Voice commands|STORAGE_READ:Load models             │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│                                    ▼                                     │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ UNIVERSAL PLUGIN REGISTRY                                          │ │
│  │ ─────────────────────────────────────────────────────────────────  │ │
│  │ • Load/Unload plugins at runtime                                   │ │
│  │ • Dependency resolution with semver                                │ │
│  │ • Permission management                                            │ │
│  │ • Plugin lifecycle: INIT → ACTIVE → PAUSED → STOPPED               │ │
│  │ • Index by: capability, module, state, verification                │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│           ┌────────────────────────┼────────────────────────┐           │
│           │                        │                        │           │
│           ▼                        ▼                        ▼           │
│  ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐    │
│  │ MODULE CONTRACTS │   │ MODULE CONTRACTS │   │ MODULE CONTRACTS │    │
│  │ (VoiceOSCore)    │   │ (AI)             │   │ (Speech)         │    │
│  │                  │   │                  │   │                  │    │
│  │ • IHandler       │   │ • TextGenPlugin  │   │ • ISpeechEngine  │    │
│  │ • ISynonymProv   │   │ • NLPPlugin      │   │ • ITTSEngine     │    │
│  │ • IThemeProvider │   │ • EmbeddingPlugin│   │ • IWakeWord      │    │
│  │ • IOverlay       │   │ • LLMPlugin      │   │ • IVocabulary    │    │
│  └────────┬─────────┘   └────────┬─────────┘   └────────┬─────────┘    │
│           │                      │                      │              │
│           └──────────────────────┼──────────────────────┘              │
│                                  │                                      │
│                                  ▼                                      │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ ACCESSIBILITY DATA HUB (VoiceOSCore Database)                      │ │
│  │ ─────────────────────────────────────────────────────────────────  │ │
│  │ • ScrapedElement: UI element metadata + bounds for gaze            │ │
│  │ • CommandHistory: Voice command success/failure learning           │ │
│  │ • RecognitionLearning: Personalized speech recognition             │ │
│  │ • GestureLearning: Gaze dwell calibration                          │ │
│  │ • ContextPreference: Smart command ranking                         │ │
│  │ • NavigationEdge: App navigation graph                             │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Module Inventory & Plugin Types

| # | Module | Primary Plugin Types | Accessibility Value |
|---|--------|---------------------|---------------------|
| 1 | VoiceOSCore | Handler, Synonym, Theme, Overlay, CommandPersistence | Core voice control |
| 2 | AI | LLM, NLU, Embedding, RAG | Intent understanding |
| 3 | AVA | Conversation, Memory, Intent | Assistant logic |
| 4 | SpeechRecognition | SpeechEngine, TTS, WakeWord | Voice input |
| 5 | PluginSystem | (Base infrastructure) | Plugin loading |
| 6 | Database | Repository, Migration | Data persistence |
| 7 | AvidCreator | AvidGenerator, TargetResolver | Element identification |
| 8 | AvaMagic | ComponentRenderer, Widget, Theme | UI rendering |
| 9 | Translation | Locale, Dictionary | Multi-language |
| 10 | UniversalRPC | Transport, Serializer | Cross-platform IPC |
| 11 | VoiceKeyboard | InputMethod, Prediction | Text input |
| 12 | Voice | VoiceProcessor, Filter | Audio processing |
| 13 | DeviceManager | Device, Sensor | Hardware access |
| 14 | LicenseManager | Validator, Feature | Licensing |
| 15 | Actions | ActionHandler, Gesture | Input actions |
| 16 | AVID | Fingerprinter, Matcher | Element fingerprints |
| 17 | AVUCodec | Encoder, Decoder | AVU serialization |
| 18 | Utilities | Logger, Cache | Common utilities |
| 19 | WebAvanue | WebScraper, WebCommand | Web accessibility |
| 20 | VoiceOS | (Legacy, delegates to VoiceOSCore) | - |
| 21 | LicenseSDK | (Internal) | - |
| 22 | LicenseValidation | (Internal) | - |

---

## Universal Plugin Contract

### Base Interface (All Plugins)

```kotlin
// Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/pluginsystem/core/UniversalPlugin.kt

/**
 * Universal Plugin Interface - Base contract for all plugins.
 *
 * Every plugin in the system MUST implement this interface.
 * Module-specific contracts extend this interface.
 */
interface UniversalPlugin {
    /** Unique plugin identifier (reverse domain notation) */
    val pluginId: String

    /** Plugin version (semver) */
    val version: String

    /** Plugin capabilities (used for discovery) */
    val capabilities: Set<PluginCapability>

    /** Plugin state */
    val state: PluginState

    /**
     * Initialize plugin with configuration.
     * Called once when plugin is loaded.
     *
     * @param config AVU-format configuration
     * @param context Plugin context with access to services
     * @return InitResult indicating success/failure
     */
    suspend fun initialize(config: AvuConfig, context: PluginContext): InitResult

    /**
     * Activate plugin for use.
     * Called when plugin is ready to receive requests.
     */
    suspend fun activate()

    /**
     * Pause plugin (reduce resource usage).
     * Plugin should release non-essential resources.
     */
    suspend fun pause()

    /**
     * Stop and cleanup plugin.
     * Called before unloading.
     */
    suspend fun shutdown()

    /**
     * Health check for monitoring.
     * @return HealthStatus with current state
     */
    fun healthCheck(): HealthStatus
}

enum class PluginState {
    UNINITIALIZED,  // Not yet initialized
    INITIALIZING,   // Currently initializing
    ACTIVE,         // Ready for use
    PAUSED,         // Temporarily paused
    ERROR,          // In error state
    STOPPING,       // Currently stopping
    STOPPED         // Fully stopped
}

data class HealthStatus(
    val healthy: Boolean,
    val message: String,
    val metrics: Map<String, Any> = emptyMap()
)

data class InitResult(
    val success: Boolean,
    val error: String? = null,
    val warnings: List<String> = emptyList()
)
```

### Plugin Context

```kotlin
/**
 * Plugin Context - Services available to all plugins.
 */
interface PluginContext {
    /** Access to VoiceOSCore accessibility database */
    val accessibilityData: AccessibilityDataProvider

    /** Logging service */
    val logger: PluginLogger

    /** Event bus for inter-plugin communication */
    val eventBus: PluginEventBus

    /** Permission checker */
    val permissions: PermissionManager

    /** Platform info (Android/iOS/Desktop) */
    val platform: PlatformInfo

    /** Resource loader for plugin assets */
    val resources: ResourceLoader

    /** Secure storage for plugin state */
    val storage: PluginStorage
}
```

---

## AVU Manifest Format

Instead of YAML/JSON, plugins use AVU format for manifests:

```
# Plugin Manifest (AVU Format)
# File: plugin.avu

# Header: PLG:id:version:entrypoint:name
PLG:com.example.voiceenhancer:1.2.0:com.example.VoiceEnhancerPlugin:Voice Enhancer

# Description
DSC:Enhanced voice recognition for accessibility users with speech difficulties

# Author
AUT:Example Corp:contact@example.com:https://example.com

# Capabilities (pipe-separated)
CAP:accessibility.voice|speech.recognition|ai.nlu

# Target modules (pipe-separated)
MOD:VoiceOSCore|SpeechRecognition|AI

# Dependencies (one per line)
# Format: DEP:pluginId:versionConstraint
DEP:com.augmentalis.core:^2.0.0
DEP:com.augmentalis.speech:>=1.5.0

# Permissions (one per line)
# Format: PRM:permission:rationale
PRM:MICROPHONE:Required for voice input processing
PRM:STORAGE_READ:Load custom vocabulary models
PRM:ACCESSIBILITY:Access UI elements for command generation

# Minimum platform versions
PLT:android:26
PLT:ios:14.0
PLT:desktop:any

# Assets (one per line)
# Format: AST:type:path
AST:model:models/custom_vocab.onnx
AST:config:config/defaults.avu
AST:locale:locales/en-US.avu
AST:locale:locales/es-ES.avu

# Configuration schema (embedded AVU)
CFG:start
  # Format: KEY:type:default:description
  KEY:sensitivity:float:0.8:Voice detection sensitivity
  KEY:timeout_ms:int:5000:Command timeout in milliseconds
  KEY:language:string:en-US:Recognition language
  KEY:offline_mode:bool:true:Enable offline recognition
CFG:end

# Lifecycle hooks
HKS:on_app_foreground:handleAppForeground
HKS:on_accessibility_event:handleAccessibilityEvent
HKS:on_voice_command:handleVoiceCommand
```

### AVU Manifest Parser

```kotlin
// Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/pluginsystem/core/AvuManifestParser.kt

object AvuManifestParser {

    fun parse(content: String): PluginManifest {
        val lines = content.lines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }

        var manifest = PluginManifest.empty()
        var inConfigBlock = false
        val configKeys = mutableListOf<ConfigKey>()

        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size < 2) continue

            val tag = parts[0].uppercase()
            val value = parts[1]

            when {
                tag == "CFG" && value == "start" -> inConfigBlock = true
                tag == "CFG" && value == "end" -> {
                    inConfigBlock = false
                    manifest = manifest.copy(configSchema = configKeys.toList())
                }
                inConfigBlock && tag == "KEY" -> {
                    configKeys.add(parseConfigKey(value))
                }
                else -> manifest = parseManifestLine(manifest, tag, value)
            }
        }

        return manifest
    }

    private fun parseManifestLine(manifest: PluginManifest, tag: String, value: String): PluginManifest {
        return when (tag) {
            "PLG" -> {
                val parts = value.split(":")
                manifest.copy(
                    id = parts.getOrNull(0) ?: "",
                    version = parts.getOrNull(1) ?: "",
                    entrypoint = parts.getOrNull(2) ?: "",
                    name = parts.getOrNull(3) ?: ""
                )
            }
            "DSC" -> manifest.copy(description = value)
            "AUT" -> {
                val parts = value.split(":")
                manifest.copy(author = Author(
                    name = parts.getOrNull(0) ?: "",
                    email = parts.getOrNull(1) ?: "",
                    url = parts.getOrNull(2) ?: ""
                ))
            }
            "CAP" -> manifest.copy(capabilities = value.split("|").map { PluginCapability.fromString(it) }.toSet())
            "MOD" -> manifest.copy(targetModules = value.split("|").toSet())
            "DEP" -> {
                val parts = value.split(":")
                val dep = Dependency(parts[0], parts.getOrElse(1) { "*" })
                manifest.copy(dependencies = manifest.dependencies + dep)
            }
            "PRM" -> {
                val parts = value.split(":")
                val perm = Permission(parts[0], parts.getOrElse(1) { "" })
                manifest.copy(permissions = manifest.permissions + perm)
            }
            "PLT" -> {
                val parts = value.split(":")
                manifest.copy(platforms = manifest.platforms + (parts[0] to parts.getOrElse(1) { "any" }))
            }
            "AST" -> {
                val parts = value.split(":")
                val asset = Asset(type = parts[0], path = parts.getOrElse(1) { "" })
                manifest.copy(assets = manifest.assets + asset)
            }
            "HKS" -> {
                val parts = value.split(":")
                manifest.copy(hooks = manifest.hooks + (parts[0] to parts.getOrElse(1) { "" }))
            }
            else -> manifest
        }
    }

    private fun parseConfigKey(value: String): ConfigKey {
        val parts = value.split(":")
        return ConfigKey(
            name = parts.getOrNull(0) ?: "",
            type = parts.getOrNull(1) ?: "string",
            default = parts.getOrNull(2) ?: "",
            description = parts.getOrNull(3) ?: ""
        )
    }
}
```

---

## Module-Specific Contracts

### 1. VoiceOSCore Plugins

```kotlin
// Handler Plugin - Execute voice commands
interface HandlerPlugin : UniversalPlugin {
    /** Handler type (navigation, ui, system, etc.) */
    val handlerType: HandlerType

    /** Patterns this handler can process */
    val patterns: List<CommandPattern>

    /**
     * Check if this handler can process a command.
     * @param command Parsed voice command
     * @param context Current accessibility context
     */
    fun canHandle(command: ParsedCommand, context: AccessibilityContext): Boolean

    /**
     * Execute the command.
     * @return ActionResult with success/failure and details
     */
    suspend fun handle(command: ParsedCommand, context: AccessibilityContext): ActionResult
}

// Synonym Provider Plugin - Custom vocabulary
interface SynonymProviderPlugin : UniversalPlugin {
    /**
     * Get synonyms for a canonical phrase.
     * Used for expanding voice command vocabulary.
     */
    fun getSynonyms(phrase: String, locale: String): List<String>

    /**
     * Normalize a phrase to its canonical form.
     */
    fun normalize(phrase: String, locale: String): String
}

// Overlay Plugin - Custom accessibility overlays
interface OverlayPlugin : UniversalPlugin {
    /** Overlay priority (higher = rendered on top) */
    val priority: Int

    /**
     * Render overlay content.
     * @param context Current screen context
     * @return OverlayContent to display
     */
    fun render(context: ScreenContext): OverlayContent

    /**
     * Handle gaze/voice interaction with overlay.
     */
    suspend fun onInteraction(event: InteractionEvent): Boolean
}
```

### 2. AI Plugins (LLM/NLU)

```kotlin
// LLM Plugin - Text generation
interface LLMPlugin : UniversalPlugin {
    /** Model info */
    val modelInfo: ModelInfo

    /** Context window size */
    val contextWindow: Int

    /**
     * Generate text from prompt.
     * @param request Generation request
     * @return Generated text with metadata
     */
    suspend fun generate(request: GenerationRequest): GenerationResponse

    /**
     * Stream generated tokens.
     */
    fun generateStream(request: GenerationRequest): Flow<Token>
}

// NLU Plugin - Intent understanding
interface NLUPlugin : UniversalPlugin {
    /**
     * Classify intent from user utterance.
     * Critical for accessibility - understanding what user wants to do.
     */
    suspend fun classifyIntent(utterance: String, context: NLUContext): IntentResult

    /**
     * Extract entities from utterance.
     */
    suspend fun extractEntities(utterance: String): List<Entity>

    /**
     * Generate voice command suggestions from UI context.
     * Uses VoiceOSCore scraped elements to generate natural commands.
     */
    suspend fun suggestCommands(
        elements: List<QuantizedElement>,
        screenContext: ScreenContext
    ): List<CommandSuggestion>
}

// Embedding Plugin - Text embeddings for semantic search
interface EmbeddingPlugin : UniversalPlugin {
    /** Embedding dimension */
    val dimension: Int

    /**
     * Generate embedding vector for text.
     */
    suspend fun embed(text: String): FloatArray

    /**
     * Batch embed multiple texts.
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>
}
```

### 3. Speech Recognition Plugins

```kotlin
// Speech Engine Plugin - Voice-to-text
interface SpeechEnginePlugin : UniversalPlugin {
    /** Engine capabilities */
    val engineCapabilities: Set<SpeechCapability>

    /** Supported languages */
    val supportedLanguages: Set<String>

    /**
     * Start listening for voice input.
     * @param config Recognition configuration
     * @return Flow of recognition results
     */
    fun startRecognition(config: RecognitionConfig): Flow<RecognitionResult>

    /**
     * Stop listening.
     */
    fun stopRecognition()

    /**
     * Load custom vocabulary for better recognition.
     * Critical for accessibility - user's custom commands.
     */
    suspend fun loadVocabulary(vocabulary: CustomVocabulary)

    /**
     * Calibrate to user's voice.
     * Important for users with speech difficulties.
     */
    suspend fun calibrate(samples: List<AudioSample>): CalibrationResult
}

enum class SpeechCapability {
    OFFLINE,           // Works without internet
    CONTINUOUS,        // Continuous listening
    WAKE_WORD,         // Wake word detection
    CUSTOM_VOCABULARY, // Custom vocabulary support
    SPEAKER_ID,        // Speaker identification
    STREAMING,         // Streaming recognition
    NOISE_REDUCTION    // Background noise handling
}
```

---

## Accessibility Data Provider

The key integration point between plugins and VoiceOSCore database:

```kotlin
// Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/pluginsystem/accessibility/AccessibilityDataProvider.kt

/**
 * Accessibility Data Provider - Gateway to VoiceOSCore data.
 *
 * Provides plugins with read-only access to accessibility data
 * for AI processing, personalization, and command generation.
 */
interface AccessibilityDataProvider {

    // ─────────────────────────────────────────────────────────────
    // UI ELEMENT DATA (for gaze targeting and command generation)
    // ─────────────────────────────────────────────────────────────

    /**
     * Get all interactive elements on current screen.
     * Includes bounds for gaze targeting.
     */
    suspend fun getCurrentScreenElements(): List<QuantizedElement>

    /**
     * Get element by AVID.
     */
    suspend fun getElement(avid: String): QuantizedElement?

    /**
     * Get element interaction history.
     * For learning which elements user interacts with most.
     */
    suspend fun getElementInteractions(
        avid: String,
        timeRange: TimeRange? = null
    ): List<UserInteraction>

    // ─────────────────────────────────────────────────────────────
    // COMMAND DATA (for voice control)
    // ─────────────────────────────────────────────────────────────

    /**
     * Get all commands for current screen.
     */
    suspend fun getScreenCommands(): List<QuantizedCommand>

    /**
     * Get command history for personalization.
     */
    suspend fun getCommandHistory(
        limit: Int = 100,
        successOnly: Boolean = false
    ): List<CommandHistoryEntry>

    /**
     * Get user's most successful commands.
     * Weighted by recency and success rate.
     */
    suspend fun getTopCommands(
        limit: Int = 20,
        context: String? = null
    ): List<RankedCommand>

    // ─────────────────────────────────────────────────────────────
    // SPEECH RECOGNITION DATA (for personalization)
    // ─────────────────────────────────────────────────────────────

    /**
     * Get recognition learning data for a specific engine.
     * Used to personalize speech recognition.
     */
    suspend fun getRecognitionLearning(
        engine: SpeechEngine
    ): List<RecognitionLearningEntry>

    /**
     * Get user's speech patterns.
     */
    suspend fun getSpeechPatterns(): SpeechPatterns

    // ─────────────────────────────────────────────────────────────
    // GAZE DATA (for gaze input calibration)
    // ─────────────────────────────────────────────────────────────

    /**
     * Get gaze learning data.
     * For calibrating gaze dwell times and accuracy.
     */
    suspend fun getGazeLearning(): GazeLearningData

    /**
     * Get element visibility patterns.
     * For predicting when elements become gaze-selectable.
     */
    suspend fun getVisibilityPatterns(avid: String): VisibilityPattern

    // ─────────────────────────────────────────────────────────────
    // NAVIGATION DATA (for predictive commands)
    // ─────────────────────────────────────────────────────────────

    /**
     * Get navigation graph for current app.
     */
    suspend fun getNavigationGraph(packageName: String): NavigationGraph

    /**
     * Predict next screen from current context.
     */
    suspend fun predictNextScreen(): List<ScreenPrediction>

    /**
     * Get screen context.
     */
    suspend fun getScreenContext(): ScreenContext

    // ─────────────────────────────────────────────────────────────
    // CONTEXT PREFERENCES (for smart ranking)
    // ─────────────────────────────────────────────────────────────

    /**
     * Get command preferences for current context.
     * Used to rank command suggestions.
     */
    suspend fun getContextPreferences(): List<ContextPreference>

    /**
     * Get app learning status.
     */
    suspend fun getAppLearningStatus(packageName: String): AppLearningStatus
}
```

---

## Plugin Loading & Discovery

### Plugin Loader

```kotlin
// Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/pluginsystem/core/UniversalPluginLoader.kt

class UniversalPluginLoader(
    private val registry: UniversalPluginRegistry,
    private val accessibilityData: AccessibilityDataProvider,
    private val permissionManager: PermissionManager
) {

    /**
     * Scan and load plugins from directory.
     *
     * @param pluginDir Directory containing plugin packages
     * @return List of loaded plugin IDs
     */
    suspend fun scanAndLoad(pluginDir: Path): List<String> {
        val loadedPlugins = mutableListOf<String>()

        // Find all plugin.avu files
        val manifests = findManifests(pluginDir)

        // Sort by dependencies
        val sorted = topologicalSort(manifests)

        for (manifest in sorted) {
            try {
                val result = loadPlugin(manifest)
                if (result.success) {
                    loadedPlugins.add(manifest.id)
                } else {
                    logger.warn("Failed to load plugin ${manifest.id}: ${result.error}")
                }
            } catch (e: Exception) {
                logger.error("Exception loading plugin ${manifest.id}", e)
            }
        }

        return loadedPlugins
    }

    /**
     * Load a single plugin from manifest.
     */
    suspend fun loadPlugin(manifest: PluginManifest): LoadResult {
        // Check dependencies
        val missingDeps = manifest.dependencies.filter { dep ->
            !registry.hasPlugin(dep.id, dep.versionConstraint)
        }
        if (missingDeps.isNotEmpty()) {
            return LoadResult.failure("Missing dependencies: ${missingDeps.map { it.id }}")
        }

        // Check permissions
        val deniedPerms = manifest.permissions.filter { perm ->
            !permissionManager.isGranted(perm.name)
        }
        if (deniedPerms.isNotEmpty()) {
            return LoadResult.failure("Permissions denied: ${deniedPerms.map { it.name }}")
        }

        // Create plugin instance
        val plugin = createPluginInstance(manifest)

        // Create context
        val context = createPluginContext(manifest)

        // Load config
        val config = loadPluginConfig(manifest)

        // Initialize
        val initResult = plugin.initialize(config, context)
        if (!initResult.success) {
            return LoadResult.failure("Initialization failed: ${initResult.error}")
        }

        // Register
        registry.register(plugin, manifest)

        // Activate
        plugin.activate()

        return LoadResult.success(plugin.pluginId)
    }

    private fun createPluginContext(manifest: PluginManifest): PluginContext {
        return PluginContextImpl(
            accessibilityData = accessibilityData,
            logger = PluginLoggerImpl(manifest.id),
            eventBus = registry.eventBus,
            permissions = permissionManager.scopedFor(manifest.id),
            platform = Platform.current(),
            resources = ResourceLoaderImpl(manifest.assetPaths),
            storage = PluginStorageImpl(manifest.id)
        )
    }
}
```

### Plugin Registry

```kotlin
// Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/pluginsystem/core/UniversalPluginRegistry.kt

class UniversalPluginRegistry {

    private val plugins = ConcurrentHashMap<String, RegisteredPlugin>()
    private val byCapability = ConcurrentHashMap<PluginCapability, MutableSet<String>>()
    private val byModule = ConcurrentHashMap<String, MutableSet<String>>()
    private val byState = ConcurrentHashMap<PluginState, MutableSet<String>>()

    val eventBus = PluginEventBus()

    /**
     * Register a plugin.
     */
    fun register(plugin: UniversalPlugin, manifest: PluginManifest) {
        val registered = RegisteredPlugin(plugin, manifest, System.currentTimeMillis())
        plugins[plugin.pluginId] = registered

        // Index by capability
        plugin.capabilities.forEach { cap ->
            byCapability.getOrPut(cap) { mutableSetOf() }.add(plugin.pluginId)
        }

        // Index by module
        manifest.targetModules.forEach { mod ->
            byModule.getOrPut(mod) { mutableSetOf() }.add(plugin.pluginId)
        }

        // Index by state
        byState.getOrPut(plugin.state) { mutableSetOf() }.add(plugin.pluginId)

        // Emit event
        eventBus.emit(PluginRegistered(plugin.pluginId))
    }

    /**
     * Find plugins by capability.
     */
    fun findByCapability(capability: PluginCapability): List<UniversalPlugin> {
        return byCapability[capability]?.mapNotNull { plugins[it]?.plugin } ?: emptyList()
    }

    /**
     * Find plugins for a module.
     */
    fun findByModule(moduleName: String): List<UniversalPlugin> {
        return byModule[moduleName]?.mapNotNull { plugins[it]?.plugin } ?: emptyList()
    }

    /**
     * Get plugin by ID.
     */
    fun getPlugin(pluginId: String): UniversalPlugin? = plugins[pluginId]?.plugin

    /**
     * Check if plugin exists with version constraint.
     */
    fun hasPlugin(pluginId: String, versionConstraint: String): Boolean {
        val registered = plugins[pluginId] ?: return false
        return SemVer.satisfies(registered.plugin.version, versionConstraint)
    }

    /**
     * Get all active plugins.
     */
    fun getActivePlugins(): List<UniversalPlugin> {
        return byState[PluginState.ACTIVE]?.mapNotNull { plugins[it]?.plugin } ?: emptyList()
    }
}
```

---

## Accessibility Data Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ACCESSIBILITY DATA FLOW                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  USER INPUT                                                              │
│  ──────────                                                              │
│       │                                                                  │
│       │  ┌─────────────┐      ┌─────────────┐                           │
│       ├──│ Voice Input │      │ Gaze Input  │                           │
│       │  └──────┬──────┘      └──────┬──────┘                           │
│       │         │                    │                                   │
│       │         ▼                    ▼                                   │
│       │  ┌────────────────────────────────────────┐                     │
│       │  │ Speech Recognition Plugin               │                     │
│       │  │ • calibrate() from RecognitionLearning  │                     │
│       │  │ • loadVocabulary() from GeneratedCommand│                     │
│       │  └────────────────┬───────────────────────┘                     │
│       │                   │                                              │
│       │                   ▼                                              │
│       │  ┌────────────────────────────────────────┐                     │
│       │  │ NLU Plugin                              │                     │
│       │  │ • classifyIntent() with context         │                     │
│       │  │ • extractEntities() for parameters      │                     │
│       │  │ • Uses ContextPreference for ranking    │                     │
│       │  └────────────────┬───────────────────────┘                     │
│       │                   │                                              │
│       │                   ▼                                              │
│       │  ┌────────────────────────────────────────┐                     │
│       │  │ Command Matching                        │                     │
│       │  │ • Match against GeneratedCommand        │                     │
│       │  │ • Match against VoiceCommand (static)   │                     │
│       │  │ • Expand synonyms from SynonymProvider  │                     │
│       │  └────────────────┬───────────────────────┘                     │
│       │                   │                                              │
│       │                   ▼                                              │
│       │  ┌────────────────────────────────────────┐                     │
│       │  │ Handler Plugin                          │                     │
│       │  │ • Execute action on UI element          │                     │
│       │  │ • Uses ScrapedElement for targeting     │                     │
│       │  │ • Uses NavigationEdge for navigation    │                     │
│       │  └────────────────┬───────────────────────┘                     │
│       │                   │                                              │
│       │                   ▼                                              │
│       │  ┌────────────────────────────────────────┐                     │
│       │  │ FEEDBACK LOOP                           │                     │
│       │  │ • Log to CommandHistory (success/fail)  │                     │
│       │  │ • Update ContextPreference              │                     │
│       │  │ • Update RecognitionLearning            │                     │
│       │  │ • Update GestureLearning (gaze)         │                     │
│       │  │ • Update UserInteraction                │                     │
│       │  └────────────────────────────────────────┘                     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Plugin Types for Accessibility Users

### Priority 1: Critical for Voice/Gaze Control

| Plugin Type | Purpose | Data Used |
|-------------|---------|-----------|
| **SpeechEnginePlugin** | Voice-to-text | RecognitionLearning, CustomVocabulary |
| **NLUPlugin** | Intent understanding | ScreenContext, GeneratedCommand |
| **HandlerPlugin** | Execute actions | ScrapedElement, NavigationEdge |
| **SynonymProviderPlugin** | Expand vocabulary | CommandHistory, GeneratedCommand |

### Priority 2: Personalization

| Plugin Type | Purpose | Data Used |
|-------------|---------|-----------|
| **CalibrationPlugin** | Voice/Gaze calibration | RecognitionLearning, GestureLearning |
| **PredictionPlugin** | Predict next command | ContextPreference, NavigationEdge |
| **RankingPlugin** | Rank command suggestions | CommandHistory, UserInteraction |

### Priority 3: Enhancement

| Plugin Type | Purpose | Data Used |
|-------------|---------|-----------|
| **LLMPlugin** | Generate command synonyms | GeneratedCommand, ScreenContext |
| **EmbeddingPlugin** | Semantic command matching | GeneratedCommand, VoiceCommand |
| **ThemePlugin** | High-contrast themes for gaze | ScrapedElement (colors, bounds) |
| **OverlayPlugin** | Visual feedback | ScrapedElement, AvidAnalytics |

---

## Example Plugin: Voice Enhancer

Complete example of an accessibility plugin:

```kotlin
// plugins/voice-enhancer/src/commonMain/kotlin/com/example/VoiceEnhancerPlugin.kt

class VoiceEnhancerPlugin : NLUPlugin {

    override val pluginId = "com.example.voiceenhancer"
    override val version = "1.2.0"
    override val capabilities = setOf(
        PluginCapability.ACCESSIBILITY_VOICE,
        PluginCapability.AI_NLU
    )

    override var state = PluginState.UNINITIALIZED
        private set

    private lateinit var context: PluginContext
    private lateinit var accessibilityData: AccessibilityDataProvider
    private var sensitivity = 0.8f

    override suspend fun initialize(config: AvuConfig, context: PluginContext): InitResult {
        state = PluginState.INITIALIZING
        this.context = context
        this.accessibilityData = context.accessibilityData

        // Load configuration
        sensitivity = config.getFloat("sensitivity", 0.8f)

        context.logger.info("VoiceEnhancer initialized with sensitivity=$sensitivity")
        return InitResult(success = true)
    }

    override suspend fun activate() {
        state = PluginState.ACTIVE
        context.logger.info("VoiceEnhancer activated")
    }

    override suspend fun pause() {
        state = PluginState.PAUSED
    }

    override suspend fun shutdown() {
        state = PluginState.STOPPING
        // Cleanup resources
        state = PluginState.STOPPED
    }

    override fun healthCheck(): HealthStatus {
        return HealthStatus(
            healthy = state == PluginState.ACTIVE,
            message = "State: $state",
            metrics = mapOf(
                "sensitivity" to sensitivity,
                "commandsProcessed" to commandsProcessed
            )
        )
    }

    // ─────────────────────────────────────────────────────────────
    // NLU PLUGIN IMPLEMENTATION
    // ─────────────────────────────────────────────────────────────

    private var commandsProcessed = 0L

    override suspend fun classifyIntent(utterance: String, context: NLUContext): IntentResult {
        commandsProcessed++

        // Get user's command history for personalization
        val history = accessibilityData.getCommandHistory(limit = 50, successOnly = true)

        // Get context preferences for ranking
        val preferences = accessibilityData.getContextPreferences()

        // Get current screen commands
        val screenCommands = accessibilityData.getScreenCommands()

        // Match utterance against known commands
        val matches = screenCommands.mapNotNull { cmd ->
            val score = calculateMatchScore(utterance, cmd, history, preferences)
            if (score >= sensitivity) {
                IntentMatch(
                    intent = cmd.actionType.toIntent(),
                    confidence = score,
                    targetAvid = cmd.targetAvid,
                    phrase = cmd.phrase
                )
            } else null
        }.sortedByDescending { it.confidence }

        return if (matches.isNotEmpty()) {
            IntentResult(
                intent = matches.first().intent,
                confidence = matches.first().confidence,
                alternatives = matches.drop(1),
                metadata = mapOf(
                    "targetAvid" to (matches.first().targetAvid ?: ""),
                    "matchedPhrase" to matches.first().phrase
                )
            )
        } else {
            IntentResult(
                intent = Intent.UNKNOWN,
                confidence = 0.0f,
                alternatives = emptyList()
            )
        }
    }

    private fun calculateMatchScore(
        utterance: String,
        command: QuantizedCommand,
        history: List<CommandHistoryEntry>,
        preferences: List<ContextPreference>
    ): Float {
        var score = 0.0f

        // Base similarity
        val similarity = levenshteinSimilarity(utterance.lowercase(), command.phrase.lowercase())
        score += similarity * 0.5f

        // Boost from history (user has used this command successfully)
        val historyBoost = history.count { it.processedCommand == command.phrase } * 0.05f
        score += minOf(historyBoost, 0.25f)

        // Boost from context preferences
        val contextBoost = preferences.find { it.commandId == command.avid }?.let {
            (it.successCount.toFloat() / maxOf(it.usageCount, 1)) * 0.25f
        } ?: 0.0f
        score += contextBoost

        return minOf(score, 1.0f)
    }

    override suspend fun extractEntities(utterance: String): List<Entity> {
        // Extract entities like numbers, directions, etc.
        val entities = mutableListOf<Entity>()

        // Number extraction
        val numberRegex = "\\b(\\d+)\\b".toRegex()
        numberRegex.findAll(utterance).forEach { match ->
            entities.add(Entity(
                type = "NUMBER",
                value = match.value,
                start = match.range.first,
                end = match.range.last
            ))
        }

        // Direction extraction
        val directions = listOf("up", "down", "left", "right", "next", "previous")
        directions.forEach { dir ->
            if (utterance.lowercase().contains(dir)) {
                entities.add(Entity(
                    type = "DIRECTION",
                    value = dir,
                    start = utterance.lowercase().indexOf(dir),
                    end = utterance.lowercase().indexOf(dir) + dir.length
                ))
            }
        }

        return entities
    }

    override suspend fun suggestCommands(
        elements: List<QuantizedElement>,
        screenContext: ScreenContext
    ): List<CommandSuggestion> {
        // Generate natural voice commands from UI elements
        return elements.mapNotNull { element ->
            if (element.actions.contains("click")) {
                val phrase = generateNaturalPhrase(element)
                CommandSuggestion(
                    phrase = phrase,
                    targetAvid = element.avid,
                    confidence = calculateSuggestionConfidence(element, screenContext),
                    synonyms = generateSynonyms(phrase, element)
                )
            } else null
        }.sortedByDescending { it.confidence }
    }

    private fun generateNaturalPhrase(element: QuantizedElement): String {
        val label = element.label.lowercase()
        return when (element.type) {
            ElementType.BUTTON -> label
            ElementType.CHECKBOX -> "toggle $label"
            ElementType.TEXT_FIELD -> "enter $label"
            ElementType.IMAGE -> "select $label"
            else -> label
        }
    }

    private fun generateSynonyms(phrase: String, element: QuantizedElement): List<String> {
        val synonyms = mutableListOf<String>()

        // Add aliases
        synonyms.addAll(element.aliases.map { it.lowercase() })

        // Add common variations
        if (phrase.startsWith("toggle")) {
            synonyms.add(phrase.replace("toggle", "check"))
            synonyms.add(phrase.replace("toggle", "uncheck"))
        }
        if (phrase.startsWith("enter")) {
            synonyms.add(phrase.replace("enter", "type"))
            synonyms.add(phrase.replace("enter", "input"))
        }

        // Add tap/click variations
        synonyms.add("tap ${element.label.lowercase()}")
        synonyms.add("click ${element.label.lowercase()}")
        synonyms.add("press ${element.label.lowercase()}")

        return synonyms.distinct()
    }

    private fun calculateSuggestionConfidence(
        element: QuantizedElement,
        screenContext: ScreenContext
    ): Float {
        var confidence = 0.7f

        // Boost for elements with clear labels
        if (element.label.length in 3..20) {
            confidence += 0.1f
        }

        // Boost for action elements
        if (element.category == "action") {
            confidence += 0.1f
        }

        // Boost if element matches screen's primary action
        if (screenContext.primaryAction?.let { element.label.lowercase().contains(it) } == true) {
            confidence += 0.1f
        }

        return minOf(confidence, 1.0f)
    }
}
```

### Plugin Manifest (AVU Format)

```
# plugins/voice-enhancer/plugin.avu

PLG:com.example.voiceenhancer:1.2.0:com.example.VoiceEnhancerPlugin:Voice Enhancer
DSC:Enhanced voice recognition for accessibility users with speech difficulties
AUT:Example Corp:contact@example.com:https://example.com

CAP:accessibility.voice|ai.nlu|speech.recognition
MOD:VoiceOSCore|AI

DEP:com.augmentalis.core:^2.0.0
DEP:com.augmentalis.pluginsystem:^1.0.0

PRM:ACCESSIBILITY:Access UI elements for command generation
PRM:MICROPHONE:Voice input processing

PLT:android:26
PLT:ios:14.0

CFG:start
  KEY:sensitivity:float:0.8:Voice detection sensitivity (0.0-1.0)
  KEY:max_history:int:50:Maximum command history entries to use
CFG:end

HKS:on_voice_command:classifyIntent
HKS:on_screen_change:suggestCommands
```

---

## Migration Path

### Phase 1: Extend Existing PluginSystem (Week 1-2)
1. Add `UniversalPlugin` base interface
2. Add AVU manifest parser alongside YAML
3. Add `AccessibilityDataProvider` interface
4. Migrate existing plugins to new base

### Phase 2: Module Contracts (Week 3-4)
1. Define contracts for VoiceOSCore plugins
2. Define contracts for AI plugins
3. Define contracts for Speech plugins
4. Create plugin SDK with templates

### Phase 3: Data Integration (Week 5-6)
1. Implement `AccessibilityDataProvider` using existing repositories
2. Create read-only data access layer
3. Add caching for hot data (current screen elements)
4. Add real-time updates via event bus

### Phase 4: Reference Plugins (Week 7-8)
1. Create reference speech engine plugin
2. Create reference NLU plugin
3. Create reference handler plugin
4. Document patterns and best practices

### Phase 5: Testing & Hardening (Week 9-10)
1. Plugin sandboxing
2. Permission enforcement
3. Performance testing
4. Accessibility testing with real users

---

## Related Documents

- [RustAI-Architecture-Plan-260122-V3-KMPFirst.md](./RustAI-Architecture-Plan-260122-V3-KMPFirst.md) - KMP-first AI architecture
- [RustAI-Architecture-Plan-260122-V2-PluginFirst.md](./RustAI-Architecture-Plan-260122-V2-PluginFirst.md) - Plugin-first LLM system
- Existing PluginSystem: `/Modules/PluginSystem/`
- VoiceOSCore Database: `/Modules/VoiceOS/core/database/`

---

## Appendix: Capability Strings

```kotlin
object PluginCapability {
    // Accessibility
    const val ACCESSIBILITY_VOICE = "accessibility.voice"
    const val ACCESSIBILITY_GAZE = "accessibility.gaze"
    const val ACCESSIBILITY_SWITCH = "accessibility.switch"

    // AI
    const val AI_LLM = "ai.llm"
    const val AI_NLU = "ai.nlu"
    const val AI_NLP = "ai.nlp"
    const val AI_EMBEDDING = "ai.embedding"
    const val AI_RAG = "ai.rag"

    // Speech
    const val SPEECH_RECOGNITION = "speech.recognition"
    const val SPEECH_TTS = "speech.tts"
    const val SPEECH_WAKEWORD = "speech.wakeword"

    // UI
    const val UI_OVERLAY = "ui.overlay"
    const val UI_THEME = "ui.theme"
    const val UI_COMPONENT = "ui.component"

    // Handler
    const val HANDLER_NAVIGATION = "handler.navigation"
    const val HANDLER_UI = "handler.ui"
    const val HANDLER_SYSTEM = "handler.system"
    const val HANDLER_CUSTOM = "handler.custom"

    // Data
    const val DATA_SYNC = "data.sync"
    const val DATA_EXPORT = "data.export"
    const val DATA_IMPORT = "data.import"
}
```

---

**End of Document**
