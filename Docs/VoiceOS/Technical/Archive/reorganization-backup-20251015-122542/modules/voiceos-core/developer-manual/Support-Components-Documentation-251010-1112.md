# VoiceAccessibility Support Components - Developer Documentation

**Last Updated:** 2025-10-10 11:12:00 PDT
**Module:** VoiceAccessibility
**Package:** `com.augmentalis.voiceos.accessibility`
**VOS4 Compliance:** Full (Direct implementation, no abstractions)

---

## Table of Contents

1. [Overview](#overview)
2. [Action Coordination](#action-coordination)
3. [App Management](#app-management)
4. [Configuration Management](#configuration-management)
5. [Dependency Injection](#dependency-injection)
6. [UI Scraping](#ui-scraping)
7. [Voice Recognition Client](#voice-recognition-client)
8. [Utilities](#utilities)
9. [Integration Examples](#integration-examples)
10. [Performance Considerations](#performance-considerations)

---

## Overview

The VoiceAccessibility support components provide the infrastructure and utilities that enable the main service functionality. These components handle cross-cutting concerns like action coordination, configuration management, dependency injection, UI extraction, and service communication.

### Support Components Architecture

```
Support Components
├── Managers
│   ├── ActionCoordinator - Routes commands to handlers
│   └── InstalledAppsManager - Manages app list and commands
├── Configuration
│   └── ServiceConfiguration - Settings and preferences
├── Dependency Injection
│   └── AccessibilityModule - Hilt DI setup
├── Extractors
│   └── UIScrapingEngine - UI element extraction
├── Client
│   └── VoiceRecognitionClient - AIDL service client
└── Utilities
    ├── Debouncer - Event debouncing
    ├── DisplayUtils - Display metrics
    └── ThemeUtils - Glassmorphism UI styling
```

### Component Relationships

```
VoiceOSService
    ├── [Injected] AccessibilityModule
    │   ├── SpeechEngineManager
    │   └── InstalledAppsManager
    ├── [Lazy Init] ActionCoordinator
    │   └── [Registers] All ActionHandlers
    ├── [Lazy Init] UIScrapingEngine
    │   └── [Uses] Debouncer for event throttling
    ├── [Uses] VoiceRecognitionClient
    │   └── [AIDL] VoiceRecognitionService
    └── [Loads] ServiceConfiguration
        └── [Persists] SharedPreferences
```

---

## Action Coordination

### ActionCoordinator

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/managers/ActionCoordinator.kt`

**Purpose:** Central routing mechanism for all accessibility actions. Coordinates command execution across multiple specialized handlers with performance tracking and timeout management.

#### Architecture

The ActionCoordinator uses the **Handler Pattern** (VOS4 approved interface exception) to achieve polymorphic dispatch of commands to specialized handlers. This is one of the few interface-based patterns allowed in VOS4 because it enables essential runtime command routing.

```kotlin
// Handler registry - polymorphic storage
private val handlers = ConcurrentHashMap<ActionCategory, MutableList<ActionHandler>>()

// Coroutine scope for async operations
private val coordinatorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

// Performance tracking
private val metrics = ConcurrentHashMap<String, MetricData>()
```

#### Handler Registration

Handlers are registered by category during initialization:

```kotlin
fun initialize() {
    // Core handlers
    registerHandler(ActionCategory.SYSTEM, SystemHandler(service))
    registerHandler(ActionCategory.APP, AppHandler(service))
    registerHandler(ActionCategory.DEVICE, DeviceHandler(service))
    registerHandler(ActionCategory.INPUT, InputHandler(service))
    registerHandler(ActionCategory.NAVIGATION, NavigationHandler(service))
    registerHandler(ActionCategory.UI, UIHandler(service))
    registerHandler(ActionCategory.GESTURE, GestureHandler(service))

    // Multiple handlers per category supported
    registerHandler(ActionCategory.GESTURE, DragHandler(service))

    // Migrated legacy handlers
    registerHandler(ActionCategory.DEVICE, BluetoothHandler(service))
    registerHandler(ActionCategory.UI, HelpMenuHandler(service))
    registerHandler(ActionCategory.UI, SelectHandler(service))
    registerHandler(ActionCategory.UI, NumberHandler(service))

    // Initialize all handlers
    handlers.values.flatten().forEach { it.initialize() }
}
```

#### Command Processing Pipeline

The ActionCoordinator provides a sophisticated multi-stage command processing pipeline:

**Stage 1: Voice Command Entry**
```kotlin
fun processCommand(commandText: String): Boolean {
    val cleanCommand = commandText.trim().lowercase()

    // Try direct execution first
    if (executeAction(cleanCommand)) return true

    // Try command interpretation
    val interpretedAction = interpretVoiceCommand(cleanCommand)
    if (interpretedAction != null) {
        return executeAction(interpretedAction)
    }

    return false
}
```

**Stage 2: Voice Command Interpretation**

The coordinator includes extensive natural language interpretation:

```kotlin
private fun interpretVoiceCommand(command: String): String? {
    return when {
        // Navigation
        command.contains("go back") -> "navigate_back"
        command.contains("scroll up") -> "scroll_up"

        // System
        command.contains("volume up") -> "volume_up"

        // Apps
        command.startsWith("open ") -> {
            val appName = command.removePrefix("open ").trim()
            "launch_app:$appName"
        }

        // Gestures
        command.contains("swipe left") -> "swipe left"
        command.contains("pinch open") -> "pinch open"

        // Input
        command.startsWith("type ") -> {
            val text = command.removePrefix("type ").trim()
            "input_text:$text"
        }

        // Legacy Avenue migrations
        command.contains("show numbers") -> "show_numbers"
        command.matches(Regex("(tap|click)\\s+(\\d+)")) -> {
            val number = Regex("(tap|click)\\s+(\\d+)").find(command)!!.groupValues[2]
            "click_number:$number"
        }

        // Gaze commands
        command.contains("gaze on") -> "gaze_on"
        command.contains("look and click") -> "look_and_click"

        else -> null
    }
}
```

**Stage 3: Handler Selection**

Handlers are selected using priority-based routing:

```kotlin
private fun findHandler(action: String): ActionHandler? {
    // Priority order for categories
    val priorityOrder = listOf(
        ActionCategory.SYSTEM,      // Highest priority
        ActionCategory.NAVIGATION,
        ActionCategory.APP,
        ActionCategory.GAZE,
        ActionCategory.GESTURE,
        ActionCategory.UI,
        ActionCategory.DEVICE,
        ActionCategory.INPUT,
        ActionCategory.CUSTOM       // Lowest priority
    )

    // Search by priority
    for (category in priorityOrder) {
        handlers[category]?.let { handlerList ->
            for (handler in handlerList) {
                if (handler.canHandle(action)) return handler
            }
        }
    }

    return null
}
```

**Stage 4: Action Execution**

Actions are executed with timeout protection and performance tracking:

```kotlin
fun executeAction(action: String, params: Map<String, Any> = emptyMap()): Boolean {
    val startTime = System.currentTimeMillis()

    // Find appropriate handler
    val handler = findHandler(action) ?: return false

    val category = handlers.entries.find { it.value.contains(handler) }?.key
        ?: ActionCategory.CUSTOM

    return try {
        // Execute with 5-second timeout
        val result = runBlocking {
            withTimeoutOrNull(5000L) {
                handler.execute(category, action, params)
            }
        } ?: false

        val executionTime = System.currentTimeMillis() - startTime
        recordMetric(action, executionTime, result)

        // Warn about slow execution
        if (executionTime > 100) {
            Log.w(TAG, "Slow action execution: $action took ${executionTime}ms")
        }

        result
    } catch (e: Exception) {
        Log.e(TAG, "Error executing action: $action", e)
        recordMetric(action, System.currentTimeMillis() - startTime, false)
        false
    }
}
```

#### Voice Command Enhancement

For voice-specific processing, the coordinator provides additional sophistication:

```kotlin
fun processVoiceCommand(text: String, confidence: Float): Boolean {
    val normalizedCommand = text.lowercase().trim()

    // Enhanced parameters with voice metadata
    val voiceParams = mapOf(
        "source" to "voice",
        "confidence" to confidence,
        "originalText" to text,
        "timestamp" to System.currentTimeMillis()
    )

    // Try as direct action first
    if (canHandle(normalizedCommand)) {
        return executeAction(normalizedCommand, voiceParams)
    }

    // Process with context and variations
    return processVoiceCommandWithContext(normalizedCommand, voiceParams)
}
```

**Voice Command Variations:**

The coordinator generates common variations to improve recognition:

```kotlin
private fun generateVoiceCommandVariations(command: String): List<String> {
    val variations = mutableListOf(command)

    // Remove common prefixes
    val prefixesToRemove = listOf("please ", "can you ", "could you ")
    for (prefix in prefixesToRemove) {
        if (command.startsWith(prefix)) {
            variations.add(command.removePrefix(prefix).trim())
        }
    }

    // Verb transformations
    val verbMappings = mapOf(
        "open up" to "open",
        "launch" to "open",
        "press" to "click",
        "tap" to "click"
    )

    for ((from, to) in verbMappings) {
        if (command.contains(from)) {
            variations.add(command.replace(from, to))
        }
    }

    return variations.distinct()
}
```

#### Asynchronous Execution

For non-blocking execution:

```kotlin
fun executeActionAsync(
    action: String,
    params: Map<String, Any> = emptyMap(),
    callback: (Boolean) -> Unit = {}
) {
    coordinatorScope.launch {
        val result = executeAction(action, params)
        withContext(Dispatchers.Main) {
            callback(result)
        }
    }
}
```

#### Performance Metrics

The coordinator tracks detailed performance metrics:

```kotlin
data class MetricData(
    var count: Long = 0,
    var totalTimeMs: Long = 0,
    var successCount: Long = 0,
    var lastExecutionMs: Long = 0
) {
    val averageTimeMs: Long
        get() = if (count > 0) totalTimeMs / count else 0

    val successRate: Float
        get() = if (count > 0) successCount.toFloat() / count else 0f
}

private fun recordMetric(action: String, timeMs: Long, success: Boolean) {
    metrics.getOrPut(action) { MetricData() }.apply {
        count++
        totalTimeMs += timeMs
        lastExecutionMs = timeMs
        if (success) successCount++
    }
}
```

#### Supported Actions Query

Handlers can report their capabilities:

```kotlin
fun getAllSupportedActions(): List<String> {
    return handlers.flatMap { (category, handlerList) ->
        handlerList.flatMap { handler ->
            handler.getSupportedActions().map { action ->
                "${category.name.lowercase()}: $action"
            }
        }
    }
}

fun getSupportedActions(category: ActionCategory): List<String> {
    return handlers[category]?.flatMap { it.getSupportedActions() } ?: emptyList()
}
```

#### Disposal

Proper cleanup of all handlers and resources:

```kotlin
fun dispose() {
    // Dispose all handlers
    handlers.values.flatten().forEach { handler ->
        try {
            handler.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing handler", e)
        }
    }

    handlers.clear()
    coordinatorScope.cancel()
    metrics.clear()
}
```

#### Integration with Command Pipeline

```
Voice Input → UnifiedCommandProcessor → ActionCoordinator → Handler → Action
                                            ↓
                                    Voice Interpretation
                                            ↓
                                    Command Variations
                                            ↓
                                    Priority Routing
                                            ↓
                                    Timeout Protection
                                            ↓
                                    Performance Tracking
```

---

## App Management

### InstalledAppsManager

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/managers/InstalledAppsManager.kt`

**Purpose:** Discovers installed applications and generates voice commands for launching them. Provides reactive state management for app list updates.

#### Architecture

```kotlin
class InstalledAppsManager(private val context: Context) {
    // Reactive state for app list
    private val _appList = MutableStateFlow<Map<String, String>>(mutableMapOf())
    val appList: StateFlow<Map<String, String>> = _appList.asStateFlow()

    init {
        // Load apps on background thread
        CoroutineScope(Dispatchers.IO).launch {
            loadInstalledApps()
        }
    }
}
```

#### App Discovery

The manager queries the PackageManager for launchable apps:

```kotlin
private fun loadInstalledApps() {
    CoroutineScope(Dispatchers.IO).launch {
        val packageManager = context.applicationContext.packageManager ?: return@launch

        // Query for all launchable apps
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(mainIntent, 0)
            .mapNotNull { resolveInfo ->
                try {
                    val appName = resolveInfo.loadLabel(packageManager)
                        .toString().lowercase().trim()
                    val packageName = resolveInfo.activityInfo.packageName

                    // Clean app name (remove non-alphanumeric)
                    val cleanedAppName = appName.replace(ALPHABETS_PATTERN, "")

                    AppsName(
                        name = cleanedAppName,
                        packageName = packageName,
                        commands = getInstalledAppCommands(cleanedAppName)
                    )
                } catch (e: Exception) {
                    null // Skip apps that cause errors
                }
            }
            .sortedBy { it.name }
            .filter { it.packageName != context.packageName } // Exclude self

        // Group commands by package name
        _appList.value = groupCommandsByPackageName(apps.toMutableList())
    }
}
```

#### Command Generation

For each app, multiple voice command variations are generated:

```kotlin
private fun getInstalledAppCommands(name: String): List<String> {
    return listOf(
        "open $name",
        "start $name",
        "go to $name"
    )
}
```

#### Command-to-Package Mapping

Commands are flattened into a map for quick lookup:

```kotlin
private fun groupCommandsByPackageName(appList: List<AppsName>): Map<String, String> =
    appList
        .flatMap { app -> app.commands.map { command -> command to app.packageName } }
        .toMap()
```

**Result Structure:**
```kotlin
// Map<Command, PackageName>
{
    "open chrome" to "com.android.chrome",
    "start chrome" to "com.android.chrome",
    "go to chrome" to "com.android.chrome",
    "open settings" to "com.android.settings",
    // ... etc
}
```

#### Reactive Updates

Components can observe the app list:

```kotlin
// In consuming component
installedAppsManager.appList.collect { apps ->
    // React to app list changes
    updateCommandRegistry(apps)
}
```

#### Manual Refresh

Apps can be reloaded on demand:

```kotlin
fun reloadInstalledApps() {
    CoroutineScope(Dispatchers.IO).launch {
        loadInstalledApps()
    }
}
```

#### Data Model

```kotlin
data class AppsName(
    var name: String,
    val packageName: String,
    val commands: List<String>
)
```

#### Text Normalization

```kotlin
companion object {
    private val ALPHABETS_PATTERN = Regex("[^A-Za-z0-9 ]")
}
```

This removes special characters while preserving alphanumeric characters and spaces.

#### Integration with ActionCoordinator

```
InstalledAppsManager
    ↓ (discovers apps)
    ↓ (generates commands)
    ↓ (publishes via StateFlow)
    ↓
UnifiedCommandProcessor
    ↓ (registers commands)
    ↓
ActionCoordinator
    ↓ (interprets "open chrome")
    ↓ (routes to AppHandler)
    ↓
AppHandler.execute("launch_app:chrome")
```

---

## Configuration Management

### ServiceConfiguration

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/config/ServiceConfiguration.kt`

**Purpose:** Manages all service settings, feature flags, and preferences using a data class with SharedPreferences persistence. Follows SR6-HYBRID patterns for complete configuration lifecycle management.

#### Configuration Structure

```kotlin
data class ServiceConfiguration(
    // Core settings
    val isEnabled: Boolean = true,
    val verboseLogging: Boolean = true,
    val showToasts: Boolean = true,

    // Feature flags
    val handlersEnabled: Boolean = true,
    val appLaunchingEnabled: Boolean = true,
    val dynamicCommandsEnabled: Boolean = true,
    val cursorEnabled: Boolean = true,
    val uiScrapingEnabled: Boolean = false,
    val fingerprintGesturesEnabled: Boolean = false,
    val commandCachingEnabled: Boolean = true,

    // Performance settings
    val maxCacheSize: Int = 100,
    val commandTimeout: Long = 100L,
    val initTimeout: Long = 1000L,

    // Cursor settings
    val cursorSize: Float = 48f,
    val cursorColor: Int = 0xFF4285F4.toInt(),
    val cursorSpeed: Float = 1.0f,

    // Voice Recognition settings
    val voiceRecognitionEnabled: Boolean = true,
    val voiceAutoStart: Boolean = false,
    val voiceEngine: String = "google",
    val voiceLanguage: String = "en-US",
    val voiceMinConfidence: Float = 0.7f,
    val voiceCommandFeedback: Boolean = true,
    val showPartialResults: Boolean = false,

    // Version for migration
    val configVersion: Int = 1
)
```

#### SR6-HYBRID Pattern Compliance

The configuration follows all required SR6-HYBRID patterns:

**1. createDefault():**
```kotlin
companion object {
    @JvmStatic
    fun createDefault(): ServiceConfiguration {
        return ServiceConfiguration()
    }
}
```

**2. fromMap():**
```kotlin
@JvmStatic
fun fromMap(map: Map<String, Any>): ServiceConfiguration {
    return ServiceConfiguration(
        isEnabled = map["enabled"] as? Boolean ?: true,
        verboseLogging = map["verbose_logging"] as? Boolean ?: true,
        // ... all properties with safe casts and defaults
        configVersion = (map["config_version"] as? Number)?.toInt() ?: CURRENT_VERSION
    )
}
```

**3. toMap():**
```kotlin
fun toMap(): Map<String, Any> {
    return mapOf(
        "enabled" to isEnabled,
        "verbose_logging" to verboseLogging,
        // ... all properties
        "config_version" to configVersion
    )
}
```

**4. mergeWith():**
```kotlin
fun mergeWith(other: ServiceConfiguration): ServiceConfiguration {
    return ServiceConfiguration(
        // For boolean flags, prefer enabled state
        isEnabled = isEnabled || other.isEnabled,
        verboseLogging = verboseLogging || other.verboseLogging,

        // For numeric values, use appropriate logic
        maxCacheSize = maxOf(maxCacheSize, other.maxCacheSize),
        commandTimeout = minOf(commandTimeout, other.commandTimeout),

        // For object values, prefer newer
        cursorSize = other.cursorSize,
        voiceEngine = other.voiceEngine,

        configVersion = maxOf(configVersion, other.configVersion)
    )
}
```

**5. isEquivalentTo():**
```kotlin
fun isEquivalentTo(other: ServiceConfiguration): Boolean {
    // Check functional equivalence, ignoring version
    return copy(configVersion = 0) == other.copy(configVersion = 0)
}
```

#### SharedPreferences Persistence

**Loading:**
```kotlin
@JvmStatic
fun loadFromPreferences(context: Context): ServiceConfiguration {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    return ServiceConfiguration(
        isEnabled = prefs.getBoolean("enabled", true),
        verboseLogging = true, // Force enabled
        showToasts = prefs.getBoolean("show_toasts", true),
        // ... load all properties
        configVersion = prefs.getInt("config_version", CURRENT_VERSION)
    ).migrateIfNeeded()
}
```

**Saving:**
```kotlin
fun saveToPreferences(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        putBoolean("enabled", isEnabled)
        putBoolean("verbose_logging", verboseLogging)
        // ... save all properties
        putInt("config_version", configVersion)
        apply()
    }
}
```

#### Version Migration

Configuration supports version migration:

```kotlin
private fun migrateIfNeeded(): ServiceConfiguration {
    if (configVersion >= CURRENT_VERSION) return this

    Log.i(TAG, "Migrating configuration from v$configVersion to v$CURRENT_VERSION")

    var migrated = this

    // Migration from v0 to v1
    if (configVersion < 1) {
        migrated = migrated.copy(
            fingerprintGesturesEnabled = false,
            commandCachingEnabled = true,
            configVersion = 1
        )
    }

    // Future migrations would go here
    // if (configVersion < 2) { ... }

    return migrated
}
```

#### Validation

Configuration can be validated:

```kotlin
fun validate(): Boolean {
    return when {
        maxCacheSize < 0 -> {
            Log.e(TAG, "Invalid max cache size: $maxCacheSize")
            false
        }
        commandTimeout < 0 -> {
            Log.e(TAG, "Invalid command timeout: $commandTimeout")
            false
        }
        voiceMinConfidence < 0.0f || voiceMinConfidence > 1.0f -> {
            Log.e(TAG, "Invalid voice confidence: $voiceMinConfidence")
            false
        }
        voiceEngine.isBlank() -> {
            Log.e(TAG, "Voice engine cannot be blank")
            false
        }
        else -> true
    }
}
```

#### Usage in Service

```kotlin
class VoiceOSService : AccessibilityService() {
    private lateinit var config: ServiceConfiguration

    override fun onServiceConnected() {
        // Load configuration
        config = ServiceConfiguration.loadFromPreferences(this)

        // Validate
        if (!config.validate()) {
            Log.e(TAG, "Invalid configuration, using defaults")
            config = ServiceConfiguration.createDefault()
        }

        // Use configuration
        if (config.handlersEnabled) {
            actionCoordinator.initialize()
        }

        if (config.voiceRecognitionEnabled && config.voiceAutoStart) {
            startVoiceRecognition()
        }
    }

    fun updateConfiguration(newConfig: ServiceConfiguration) {
        config = newConfig
        config.saveToPreferences(this)
        // Apply changes...
    }
}
```

#### Configuration Categories

**Core Settings:**
- `isEnabled` - Master enable flag
- `verboseLogging` - Debug logging level
- `showToasts` - User feedback via toasts

**Feature Flags:**
- `handlersEnabled` - Enable handler system
- `appLaunchingEnabled` - Enable app launching
- `dynamicCommandsEnabled` - Enable dynamic command generation
- `cursorEnabled` - Enable voice cursor
- `uiScrapingEnabled` - Enable UI element extraction
- `fingerprintGesturesEnabled` - Enable fingerprint sensor gestures
- `commandCachingEnabled` - Enable command caching

**Performance:**
- `maxCacheSize` - Maximum command cache size
- `commandTimeout` - Command execution timeout (ms)
- `initTimeout` - Initialization timeout (ms)

**Voice Recognition:**
- `voiceRecognitionEnabled` - Enable voice recognition
- `voiceAutoStart` - Auto-start on service connect
- `voiceEngine` - Preferred engine (e.g., "google", "vosk")
- `voiceLanguage` - Recognition language (e.g., "en-US")
- `voiceMinConfidence` - Minimum confidence threshold (0.0-1.0)
- `voiceCommandFeedback` - Show command feedback
- `showPartialResults` - Show partial recognition results

---

## Dependency Injection

### AccessibilityModule

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/di/AccessibilityModule.kt`

**Purpose:** Hilt dependency injection module providing service-scoped dependencies for VoiceAccessibility. Uses ServiceComponent instead of SingletonComponent to match accessibility service lifecycle.

#### Module Structure

```kotlin
@Module
@InstallIn(ServiceComponent::class)
object AccessibilityModule {
    // Providers for service-scoped dependencies
}
```

**Key Differences from Application-Level Modules:**
- Uses `ServiceComponent` instead of `SingletonComponent`
- Uses `@ServiceScoped` instead of `@Singleton`
- Provides service-specific components
- Lifecycle tied to accessibility service

#### Provided Dependencies

**1. SpeechEngineManager**

```kotlin
@Provides
@ServiceScoped
fun provideSpeechEngineManager(
    @ApplicationContext context: Context
): SpeechEngineManager {
    return SpeechEngineManager(context)
}
```

**Responsibilities:**
- Initialize speech engines (Vivoka, Vosk, etc.)
- Manage engine lifecycle
- Handle speech recognition state
- Register and update commands
- Dispatch speech events

**Scope:** Tied to service lifecycle - created when service starts, destroyed when service stops

**2. InstalledAppsManager**

```kotlin
@Provides
@ServiceScoped
fun provideInstalledAppsManager(
    @ApplicationContext context: Context
): InstalledAppsManager {
    return InstalledAppsManager(context)
}
```

**Responsibilities:**
- Detect installed applications
- Generate voice commands for apps
- Monitor app installation/uninstallation
- Cache app information for performance

**Scope:** Service-scoped for consistent app list across service lifetime

#### Not Provided by Hilt

Some components are NOT provided by Hilt because they require specific service references:

**UIScrapingEngine:**
```kotlin
// Requires AccessibilityService instance, not just Context
// Lazy-initialized in VoiceOSService
private val uiScrapingEngine by lazy {
    UIScrapingEngine(this) // 'this' is AccessibilityService
}
```

**ActionCoordinator:**
```kotlin
// Requires VoiceOSService instance for gesture dispatch
// Lazy-initialized in VoiceOSService
private val actionCoordinator by lazy {
    ActionCoordinator(this) // 'this' is VoiceOSService
}
```

#### Integration with VoiceOSService

```kotlin
@AndroidEntryPoint
class VoiceOSService : AccessibilityService() {

    // Injected dependencies
    @Inject lateinit var speechEngineManager: SpeechEngineManager
    @Inject lateinit var installedAppsManager: InstalledAppsManager

    // Lazy-initialized (require service reference)
    private val actionCoordinator by lazy { ActionCoordinator(this) }
    private val uiScrapingEngine by lazy { UIScrapingEngine(this) }

    override fun onServiceConnected() {
        // Injected components are ready to use
        speechEngineManager.initialize()

        // Observe installed apps
        lifecycleScope.launch {
            installedAppsManager.appList.collect { apps ->
                updateCommandRegistry(apps)
            }
        }

        // Initialize lazy components
        actionCoordinator.initialize()
    }
}
```

#### Scope Management

**ServiceComponent Lifecycle:**
```
Service Created
    ↓
Hilt constructs ServiceComponent
    ↓
@ServiceScoped dependencies created
    ↓
Dependencies injected into service
    ↓
Service runs
    ↓
Service destroyed
    ↓
@ServiceScoped dependencies destroyed
```

**Why ServiceScoped?**
1. Dependencies live as long as the accessibility service
2. Avoid recreating on configuration changes
3. Maintain state across accessibility events
4. Share instances across all service components

#### Future Enhancements

The module includes TODO comments for future providers:

```kotlin
// TODO: Add future accessibility-related providers
// - GestureHandler for touch gesture recognition
// - VoiceCommandProcessor for command parsing
// - AccessibilityEventFilter for event optimization
// - CommandCacheManager for performance optimization
// - ActionCoordinator factory if refactored to separate gesture dispatch
```

#### Testing with Hilt

For testing, use HiltAndroidTest:

```kotlin
@HiltAndroidTest
class VoiceOSServiceTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var installedAppsManager: InstalledAppsManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testInstalledAppsManager() {
        // Test with injected instance
    }
}
```

---

## UI Scraping

### UIScrapingEngine

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/extractors/UIScrapingEngine.kt`

**Purpose:** Advanced UI element extraction engine that scrapes accessibility node tree to generate voice commands for interactive elements. Combines performance optimizations, Legacy Avenue algorithms, and intelligent caching.

#### Architecture Overview

The UIScrapingEngine is a comprehensive system with multiple layers:

```
UIScrapingEngine
├── Element Extraction
│   ├── Recursive tree traversal
│   ├── Visibility filtering
│   ├── Clickability detection (inherited)
│   └── Text normalization
├── Caching System
│   ├── Profile cache (app-specific)
│   ├── Element cache (LRU)
│   └── Node cache (weak references)
├── Legacy Avenue Integration
│   ├── Text parsing algorithms
│   ├── Command replacement profiles
│   └── Package-specific rules
└── Performance Tracking
    ├── Atomic metrics
    ├── StateFlow monitoring
    └── Debounced extraction
```

#### Core Data Structures

**UIElement:**
```kotlin
data class UIElement(
    val text: String,
    val contentDescription: String?,
    val className: String?,
    val isClickable: Boolean,
    val bounds: Rect,
    val nodeInfo: WeakReference<AccessibilityNodeInfo>,
    val depth: Int = 0,
    val parentClass: String? = null,
    val siblingIndex: Int = 0,
    val hash: String = "",
    val normalizedText: String = "",              // Command-ready text
    val isInheritedClickable: Boolean = false,    // Inherited from parent
    val targetNodeRef: WeakReference<AccessibilityNodeInfo>? = null,
    val confidence: Float = 0.5f                  // Confidence score
)
```

**UIProfile (App-Specific):**
```kotlin
data class UIProfile(
    val packageName: String = "unknown",
    val timestamp: Long = 0,
    val commonElements: Set<String> = emptySet(),
    val staticCommands: List<String> = emptyList(),
    val layoutSignature: String = "",
    val commandReplacements: Map<String, String> = emptyMap(),
    val elementCount: Int = 0,
    val clickableCount: Int = 0,
    val textElements: List<String> = emptyList(),
    val processingTimeMs: Long = 0,
    val confidence: Float = 0f,
    val hasDuplicates: Boolean = false
)
```

#### Caching System

**Three-Level Cache:**

```kotlin
// LRU cache for profiles (20 apps)
private val profileCache = LruCache<String, UIProfile>(20)

// LRU cache for elements (1000 elements)
private val elementCache = LruCache<String, CachedElement>(1000)

// Weak reference cache for nodes (thread-safe)
private val nodeCache = ConcurrentHashMap<String, WeakReference<AccessibilityNodeInfo>>()
```

**Cache Duration:**
```kotlin
private const val CACHE_DURATION_MS = 1000L  // 1 second

fun extractUIElements(event: AccessibilityEvent? = null): List<UIElement> {
    val currentTime = System.currentTimeMillis()

    // Use cache if recent
    if (currentTime - lastScrapeTime.get() < CACHE_DURATION_MS && cachedElements.isNotEmpty()) {
        cacheHits.incrementAndGet()
        return cachedElements
    }

    // Extract fresh data
    cacheMisses.incrementAndGet()
    // ... extraction logic
}
```

#### Element Extraction Algorithm

**Main Entry Point:**

```kotlin
fun extractUIElements(event: AccessibilityEvent? = null): List<UIElement> {
    val rootNode = service.rootInActiveWindow ?: return emptyList()
    val elements = mutableListOf<UIElement>()
    val currentPackage = getCurrentPackageName()
    val replacements = commandReplacementProfiles[currentPackage]

    try {
        extractElementsRecursiveEnhanced(
            rootNode,
            elements,
            depth = 0,
            parentNode = null,
            siblingIndex = 0,
            replacements,
            currentPackage,
            isParentClickable = false
        )
    } finally {
        // Android handles node recycling automatically
    }

    // Filter duplicates
    val filteredElements = applyIntelligentDuplicateDetection(elements)

    cachedElements = filteredElements
    lastScrapeTime.set(System.currentTimeMillis())

    return filteredElements
}
```

**Recursive Extraction:**

```kotlin
private fun extractElementsRecursiveEnhanced(
    node: AccessibilityNodeInfo,
    elements: MutableList<UIElement>,
    depth: Int,
    parentNode: AccessibilityNodeInfo?,
    siblingIndex: Int,
    commandReplacements: Map<String, String>?,
    packageName: String?,
    isParentClickable: Boolean
) {
    // Depth protection
    if (depth > MAX_DEPTH) return

    // Visibility check
    if (!node.isVisibleToUser) return

    // Extract text using Legacy Avenue logic
    val rawText = extractRawTextFromNode(node)

    if (!rawText.isNullOrBlank()) {
        // Normalize text
        val normalizedText = normalizeTextAdvanced(rawText, commandReplacements)

        if (normalizedText.isNotBlank()) {
            // Determine target node
            val targetNode = determineTargetNode(node, parentNode, isParentClickable)
            val isEffectivelyClickable = isNodeEffectivelyClickable(node, isParentClickable)
            val isNumericAndNotClickable = isNumeric(normalizedText) && !node.isPerformClickable()

            // Package-specific filtering
            val shouldInclude = shouldIncludeElement(
                isEffectivelyClickable,
                isNumericAndNotClickable,
                packageName,
                node
            )

            if (shouldInclude && isUsefulNodeEnhanced(node, normalizedText)) {
                val element = createEnhancedUIElement(
                    node, depth, node.className?.toString(), siblingIndex,
                    normalizedText, isParentClickable, targetNode
                )

                // Add if not duplicate
                if (elements.none { it.bounds.approximatelyEquals(element.bounds) }) {
                    elements.add(element)
                    elementCache.put(element.hash, CachedElement(element, System.currentTimeMillis()))
                }
            }
        }
    }

    // Process children with clickability inheritance
    val canChildrenInheritClickability = isParentClickable || node.isPerformClickable()
    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child != null) {
            extractElementsRecursiveEnhanced(
                child, elements, depth + 1, node, i,
                commandReplacements, packageName,
                canChildrenInheritClickability
            )
        }
    }
}
```

#### Legacy Avenue Text Normalization

**Text Extraction Priority:**

```kotlin
private fun extractRawTextFromNode(node: AccessibilityNodeInfo): String? {
    // Priority: contentDescription > text > hintText (for EditText)
    return node.contentDescription?.toString()?.takeIf { it.isValidDescription() }
        ?: node.text?.toString()
        ?: node.hintText?.takeIf { node.isEditText() }?.toString()
}
```

**Advanced Parsing:**

```kotlin
private fun normalizeTextAdvanced(
    rawText: String,
    commandReplacements: Map<String, String>?
): String {
    // Extract first line
    val firstLine = rawText.extractFirstLine().toLowerCaseTrimmed()

    // Parse with delimiters
    val parsedText = parseDescriptionAdvanced(firstLine)

    // Apply command replacements
    return if (!commandReplacements.isNullOrEmpty()) {
        parsedText.findScrapingItems(commandReplacements)
    } else {
        parsedText
    }
}
```

**Description Parsing:**

```kotlin
private fun parseDescriptionAdvanced(text: String): String {
    var processedText = text
        .replace("&", "and")
        .replace("_", " ")

    // Find delimiter
    val delimiters = listOf(":", "|", ",", ".")
    val foundDelimiter = delimiters.firstOrNull { processedText.contains(it) }

    if (foundDelimiter != null) {
        val parts = processedText.split(foundDelimiter, limit = 2)
        processedText = if ("hf_" in text) {
            // Take part after delimiter
            if (parts.size > 1) parts[1] else parts[0]
        } else {
            // Take part before delimiter
            parts[0]
        }
    }

    // Remove non-alphanumeric (except spaces)
    return Regex("[^\\p{Alnum}\\s]").replace(processedText, "").trim()
}
```

#### Clickability Detection

**Inherited Clickability:**

```kotlin
private fun determineTargetNode(
    currentNode: AccessibilityNodeInfo,
    parentNode: AccessibilityNodeInfo?,
    isParentClickable: Boolean
): AccessibilityNodeInfo {
    // If parent is clickable and current isn't, target parent
    return if (isParentClickable && parentNode != null && !currentNode.isPerformClickable()) {
        parentNode
    } else {
        currentNode
    }
}

private fun isNodeEffectivelyClickable(
    node: AccessibilityNodeInfo,
    isParentClickable: Boolean
): Boolean {
    return node.isClickable || node.isPerformClickable() || isParentClickable
}

private fun AccessibilityNodeInfo.isPerformClickable(): Boolean {
    return isClickable || isEditable || isSelected ||
           isCheckable || isLongClickable || isContextClickable
}
```

#### Package-Specific Rules

**Numeric Content Support:**

```kotlin
companion object {
    val NUMERIC_SUPPORT_PACKAGES = setOf(
        "com.realwear.filebrowser",      // Numbered files/folders
        "com.realwear.devicecontrol",    // Volume/brightness levels
        "com.realwear.camera",           // Zoom/exposure levels
        "com.realwear.sysinfo",          // System metrics
        "com.android.camera2",
        "com.android.settings"
    )
}

private fun shouldIncludeElement(
    isEffectivelyClickable: Boolean,
    isNumericAndNotClickable: Boolean,
    packageName: String?,
    node: AccessibilityNodeInfo
): Boolean {
    val supportsNumericContent = packageName in NUMERIC_SUPPORT_PACKAGES

    val shouldIncludeNumeric = if (supportsNumericContent && isNumericAndNotClickable) {
        // Include numeric elements if app supports them
        isValidNumericElement(node, packageName)
    } else {
        // Exclude numeric non-clickable elements
        !isNumericAndNotClickable
    }

    return (isEffectivelyClickable || supportsNumericContent) && shouldIncludeNumeric
}
```

**Numeric Validation:**

```kotlin
private fun isValidNumericElement(node: AccessibilityNodeInfo, packageName: String?): Boolean {
    val bounds = Rect()
    node.getBoundsInScreen(bounds)

    // Must be visible size
    if (bounds.width() < 20 || bounds.height() < 20) return false

    val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
    if (text.isBlank()) return false

    return when (packageName) {
        "com.realwear.filebrowser" -> {
            text.matches(Regex("\\d+")) || text.contains(Regex("\\d+"))
        }
        "com.realwear.devicecontrol" -> {
            text.matches(Regex("(?i).*level\\s*\\d+.*")) ||
            text.matches(Regex("\\d+%?"))
        }
        "com.realwear.camera" -> {
            text.matches(Regex("(?i).*(zoom|exposure)\\s*\\d+.*")) ||
            text.matches(Regex("\\d+x"))
        }
        in NUMERIC_SUPPORT_PACKAGES -> {
            text.matches(Regex("\\d+"))
        }
        else -> false
    }
}
```

#### Command Generation

**Enhanced Command Generation:**

```kotlin
fun generateCommandsEnhanced(event: AccessibilityEvent): List<String> {
    val elements = extractUIElements(event)
    val commands = mutableSetOf<String>()
    val currentPackage = getCurrentPackageName()

    for (element in elements) {
        if (element.normalizedText.isNotBlank()) {
            // App-specific commands
            generateAppSpecificCommands(element, currentPackage, commands)

            // Standard commands
            commands.add("click ${element.normalizedText}")
            commands.add("tap ${element.normalizedText}")

            if (element.isClickable || element.isInheritedClickable) {
                commands.add("select ${element.normalizedText}")
                commands.add("activate ${element.normalizedText}")
            }
        }
    }

    return commands.sorted()
}
```

**App-Specific Commands:**

```kotlin
private fun generateAppSpecificCommands(
    element: UIElement,
    packageName: String?,
    commands: MutableSet<String>
) {
    val text = element.normalizedText.lowercase()

    when (packageName) {
        "com.realwear.devicecontrol" -> {
            if (text.matches(Regex("\\d+"))) {
                commands.add("set level $text")
                commands.add("level $text")
            }
            if (text.contains("%")) {
                val number = text.replace("%", "")
                commands.add("set $number percent")
            }
        }

        "com.realwear.camera" -> {
            if (text.matches(Regex("\\d+"))) {
                commands.add("zoom level $text")
                commands.add("exposure level $text")
            }
        }

        "com.realwear.filebrowser" -> {
            if (text.matches(Regex("\\d+"))) {
                commands.add("item $text")
                commands.add("file $text")
                commands.add("folder $text")
            }
        }
    }
}
```

#### Duplicate Detection

**Approximate Rectangle Equality:**

```kotlin
private fun Rect.approximatelyEquals(other: Rect): Boolean {
    val epsilon = 8  // pixels tolerance
    return (left - other.left).absoluteValue <= epsilon &&
           (right - other.right).absoluteValue <= epsilon &&
           (top - other.top).absoluteValue <= epsilon &&
           (bottom - other.bottom).absoluteValue <= epsilon
}

private fun applyIntelligentDuplicateDetection(elements: List<UIElement>): List<UIElement> {
    val uniqueElements = mutableListOf<UIElement>()
    var duplicateCount = 0

    for (element in elements) {
        val hasDuplicate = uniqueElements.any { existing ->
            existing.bounds.approximatelyEquals(element.bounds)
        }

        if (!hasDuplicate) {
            uniqueElements.add(element)
        } else {
            duplicateCount++
        }
    }

    duplicatesFiltered.set(duplicateCount.toLong())
    return uniqueElements
}
```

#### Asynchronous Extraction

```kotlin
suspend fun extractUIElementsAsync(event: AccessibilityEvent? = null): List<UIElement> =
    withContext(Dispatchers.Default) {
        _extractionState.value = _extractionState.value.copy(isExtracting = true)

        try {
            val elements = extractUIElements(event)
            val commands = elements.map { it.normalizedText }

            _extractionState.value = _extractionState.value.copy(
                elementCount = elements.size,
                lastExtractionTime = System.currentTimeMillis() - lastScrapeTime.get(),
                duplicatesFiltered = duplicatesFiltered.get(),
                uiElements = commands
            )

            elements
        } finally {
            _extractionState.value = _extractionState.value.copy(isExtracting = false)
        }
    }
```

#### Performance Monitoring

**Metrics:**

```kotlin
fun getPerformanceMetrics(): Map<String, Any> {
    return mapOf(
        "scrapeCount" to scrapeCount.get(),
        "cacheHitRate" to _extractionState.value.cacheHitRate,
        "elementCacheSize" to elementCache.size(),
        "profileCacheSize" to profileCache.size(),
        "lastExtractionTime" to _extractionState.value.lastExtractionTime,
        "nodeCacheSize" to nodeCache.size,
        "duplicatesFiltered" to duplicatesFiltered.get(),
        "commandReplacementProfiles" to commandReplacementProfiles.size
    )
}
```

**StateFlow Monitoring:**

```kotlin
data class ExtractionState(
    val isExtracting: Boolean = false,
    val elementCount: Int = 0,
    val cacheHitRate: Float = 0f,
    val lastExtractionTime: Long = 0,
    val duplicatesFiltered: Long = 0,
    val uiElements: List<String> = emptyList()
)

val extractionState: StateFlow<ExtractionState>
```

#### Cache Management

```kotlin
fun clearCache() {
    nodeCache.clear()
    synchronized(elementCache) {
        elementCache.evictAll()
    }
    synchronized(profileCache) {
        profileCache.evictAll()
    }
    commandReplacementProfiles.clear()
    cachedElements = emptyList()
    lastScrapeTime.set(0)
    cacheHits.set(0)
    cacheMisses.set(0)
    duplicatesFiltered.set(0)
}
```

#### Threading Considerations

1. **Extraction runs on IO thread** (accessibility events)
2. **Caches are thread-safe** (ConcurrentHashMap, synchronized)
3. **Atomic metrics** prevent race conditions
4. **WeakReferences** prevent memory leaks
5. **Debouncer** prevents excessive extraction

---

## Voice Recognition Client

### VoiceRecognitionClient

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/client/VoiceRecognitionClient.kt`

**Purpose:** AIDL-based client for communicating with the VoiceRecognitionService. Manages service binding, recognition control, and result callbacks.

#### Architecture

```kotlin
class VoiceRecognitionClient(private val context: Context) {
    private var service: IVoiceRecognitionService? = null
    private var isConnected = false

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var connectionCallback: ConnectionCallback? = null
    private var recognitionCallback: RecognitionCallback? = null
}
```

#### AIDL Interface

The client communicates via AIDL interface:

```
IVoiceRecognitionService.aidl:
    - startRecognition(engine, language, mode)
    - stopRecognition()
    - isRecognizing()
    - getAvailableEngines()
    - getStatus()
    - registerCallback(IRecognitionCallback)
    - unregisterCallback(IRecognitionCallback)
```

#### Service Connection

**Binding:**

```kotlin
fun connect() {
    if (isConnected) return

    val intent = Intent().apply {
        component = ComponentName(
            "com.augmentalis.voicerecognition",
            "com.augmentalis.voicerecognition.service.VoiceRecognitionService"
        )
    }

    val bound = context.bindService(
        intent,
        serviceConnection,
        Context.BIND_AUTO_CREATE
    )

    if (!bound) {
        connectionCallback?.onError("Failed to bind to VoiceRecognition service")
    }
}
```

**Connection Handler:**

```kotlin
private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        Log.d(TAG, "Service connected")
        service = IVoiceRecognitionService.Stub.asInterface(binder)
        isConnected = true

        // Register callback
        try {
            service?.registerCallback(aidlCallback)
            connectionCallback?.onConnected()
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to register callback", e)
            connectionCallback?.onError("Failed to register callback: ${e.message}")
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "Service disconnected")
        service = null
        isConnected = false
        connectionCallback?.onDisconnected()
    }
}
```

**Disconnection:**

```kotlin
fun disconnect() {
    if (isConnected) {
        try {
            service?.unregisterCallback(aidlCallback)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to unregister callback", e)
        }

        context.unbindService(serviceConnection)
        service = null
        isConnected = false
        connectionCallback?.onDisconnected()
    }
}
```

#### AIDL Callback Implementation

```kotlin
private val aidlCallback = object : IRecognitionCallback.Stub() {
    override fun onRecognitionResult(text: String, confidence: Float, isFinal: Boolean) {
        Log.d(TAG, "Recognition result: $text (confidence: $confidence, final: $isFinal)")
        scope.launch {
            recognitionCallback?.onResult(text, confidence, isFinal)
        }
    }

    override fun onError(errorCode: Int, message: String?) {
        Log.e(TAG, "Recognition error: $errorCode - $message")
        scope.launch {
            recognitionCallback?.onError(errorCode, message)
        }
    }

    override fun onStateChanged(state: Int, message: String?) {
        Log.d(TAG, "State changed: $state - $message")
        scope.launch {
            recognitionCallback?.onStateChanged(state, message)
        }
    }

    override fun onPartialResult(partialText: String?) {
        Log.d(TAG, "Partial result: $partialText")
        scope.launch {
            partialText?.let {
                recognitionCallback?.onPartialResult(it)
            }
        }
    }
}
```

**Note:** Callbacks are dispatched to main thread using coroutine scope.

#### Recognition Control

**Start Recognition:**

```kotlin
fun startRecognition(
    engine: String = "",       // Empty for default
    language: String = "en-US",
    mode: Int = 0              // 0=continuous, 1=single_shot, 2=streaming
): Boolean {
    return try {
        service?.startRecognition(engine, language, mode) ?: false
    } catch (e: RemoteException) {
        Log.e(TAG, "Failed to start recognition", e)
        recognitionCallback?.onError(500, "Failed to start: ${e.message}")
        false
    }
}
```

**Stop Recognition:**

```kotlin
fun stopRecognition(): Boolean {
    return try {
        service?.stopRecognition() ?: false
    } catch (e: RemoteException) {
        Log.e(TAG, "Failed to stop recognition", e)
        false
    }
}
```

**Check Status:**

```kotlin
fun isRecognizing(): Boolean {
    return try {
        service?.isRecognizing() ?: false
    } catch (e: RemoteException) {
        Log.e(TAG, "Failed to check recognition status", e)
        false
    }
}
```

#### Engine Management

**Get Available Engines:**

```kotlin
fun getAvailableEngines(): List<String> {
    return try {
        service?.getAvailableEngines() ?: emptyList()
    } catch (e: RemoteException) {
        Log.e(TAG, "Failed to get available engines", e)
        emptyList()
    }
}
```

**Get Service Status:**

```kotlin
fun getStatus(): String {
    return try {
        service?.getStatus() ?: "Not connected"
    } catch (e: RemoteException) {
        Log.e(TAG, "Failed to get status", e)
        "Error: ${e.message}"
    }
}
```

#### Callback Interfaces

**Connection Callback:**

```kotlin
interface ConnectionCallback {
    fun onConnected()
    fun onDisconnected()
    fun onError(error: String)
}
```

**Recognition Callback:**

```kotlin
interface RecognitionCallback {
    fun onResult(text: String, confidence: Float, isFinal: Boolean)
    fun onPartialResult(text: String)
    fun onError(errorCode: Int, message: String?)
    fun onStateChanged(state: Int, message: String?)
}
```

#### Usage Example

```kotlin
class VoiceOSService : AccessibilityService() {
    private lateinit var voiceClient: VoiceRecognitionClient

    override fun onServiceConnected() {
        voiceClient = VoiceRecognitionClient(this)

        // Set callbacks
        voiceClient.setConnectionCallback(object : VoiceRecognitionClient.ConnectionCallback {
            override fun onConnected() {
                Log.i(TAG, "Voice recognition service connected")

                // Check available engines
                val engines = voiceClient.getAvailableEngines()
                Log.d(TAG, "Available engines: $engines")

                // Start recognition
                voiceClient.startRecognition(
                    engine = "vosk",
                    language = "en-US",
                    mode = 0  // continuous
                )
            }

            override fun onDisconnected() {
                Log.w(TAG, "Voice recognition service disconnected")
            }

            override fun onError(error: String) {
                Log.e(TAG, "Connection error: $error")
            }
        })

        voiceClient.setRecognitionCallback(object : VoiceRecognitionClient.RecognitionCallback {
            override fun onResult(text: String, confidence: Float, isFinal: Boolean) {
                if (isFinal && confidence >= 0.7f) {
                    actionCoordinator.processVoiceCommand(text, confidence)
                }
            }

            override fun onPartialResult(text: String) {
                // Show partial results in UI
                updatePartialResultsUI(text)
            }

            override fun onError(errorCode: Int, message: String?) {
                Log.e(TAG, "Recognition error: $errorCode - $message")
            }

            override fun onStateChanged(state: Int, message: String?) {
                updateRecognitionStateUI(state, message)
            }
        })

        // Connect to service
        voiceClient.connect()
    }

    override fun onDestroy() {
        voiceClient.disconnect()
        super.onDestroy()
    }
}
```

#### Error Handling

All AIDL calls are wrapped in try-catch for RemoteException:

```kotlin
return try {
    service?.someMethod() ?: defaultValue
} catch (e: RemoteException) {
    Log.e(TAG, "AIDL error", e)
    defaultValue
}
```

#### Threading Model

1. **Service binding** - Main thread
2. **AIDL calls** - Synchronous (blocking)
3. **Callbacks** - Dispatched to main thread via coroutine scope
4. **All client methods** - Thread-safe

---

## Utilities

### Debouncer

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/utils/Debouncer.kt`

**Purpose:** Event debouncing utility to prevent excessive processing of rapid successive events. Essential for apps with dynamic content (Device Info, SysInfo) that generate continuous accessibility events.

#### Implementation

```kotlin
class Debouncer(private val cooldownMillis: Long = 500L) {
    private val lastExecutionMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    fun shouldProceed(key: String): Boolean {
        val now = SystemClock.uptimeMillis()
        val last = lastExecutionMap[key] ?: 0L

        return if (now - last >= cooldownMillis) {
            lastExecutionMap[key] = now
            true
        } else {
            false
        }
    }
}
```

#### Usage Patterns

**Pattern 1: Event Debouncing**

```kotlin
private val debouncer = Debouncer(500L) // 500ms cooldown

override fun onAccessibilityEvent(event: AccessibilityEvent) {
    val eventKey = "${event.packageName}:${event.eventType}"

    if (debouncer.shouldProceed(eventKey)) {
        // Process event
        uiScrapingEngine.extractUIElements(event)
    } else {
        // Skip - too soon since last processing
    }
}
```

**Pattern 2: Action Throttling**

```kotlin
private val actionDebouncer = Debouncer(1000L) // 1 second

fun performSearch(query: String) {
    if (actionDebouncer.shouldProceed("search")) {
        executeSearch(query)
    }
}
```

**Pattern 3: Multi-Key Debouncing**

```kotlin
private val debouncer = Debouncer(300L)

fun handleEvent(category: String, id: String) {
    val key = "$category:$id"
    if (debouncer.shouldProceed(key)) {
        processEvent(category, id)
    }
}
```

#### Management

**Reset Specific Key:**

```kotlin
fun reset(key: String) {
    lastExecutionMap.remove(key)
}

// Usage
debouncer.reset("com.example.app:32")
```

**Clear All:**

```kotlin
fun clearAll() {
    lastExecutionMap.clear()
}

// Usage
debouncer.clearAll()  // Reset all debounce states
```

#### Monitoring

```kotlin
fun getMetrics(): Map<String, Any> {
    return mapOf(
        "cooldownMillis" to cooldownMillis,
        "activeKeys" to lastExecutionMap.size,
        "keys" to lastExecutionMap.keys.toList()
    )
}

// Usage
val metrics = debouncer.getMetrics()
Log.d(TAG, "Active debounce keys: ${metrics["activeKeys"]}")
```

#### Thread Safety

Uses `ConcurrentHashMap` for thread-safe operation in coroutine environment.

---

### DisplayUtils

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/utils/DisplayUtils.kt`

**Purpose:** Modern display metrics utilities with Android 11+ WindowMetrics API and backward compatibility for older versions.

#### API Level Handling

```kotlin
object DisplayUtils {
    fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Modern API
            val bounds = windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            // Older versions - Deprecated API
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val point = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(point)
            point
        }
    }
}
```

#### Display Metrics

```kotlin
fun getRealDisplayMetrics(context: Context): DisplayMetrics {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.currentWindowMetrics.bounds
        val metrics = DisplayMetrics()

        // Set size from bounds
        metrics.widthPixels = bounds.width()
        metrics.heightPixels = bounds.height()

        // Copy density properties from system
        val systemMetrics = Resources.getSystem().displayMetrics
        metrics.density = systemMetrics.density
        metrics.densityDpi = systemMetrics.densityDpi
        metrics.scaledDensity = systemMetrics.scaledDensity
        metrics.xdpi = systemMetrics.xdpi
        metrics.ydpi = systemMetrics.ydpi

        metrics
    } else {
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        metrics
    }
}
```

#### Usage Examples

```kotlin
// Get screen size
val screenSize = DisplayUtils.getRealScreenSize(context)
Log.d(TAG, "Screen: ${screenSize.x} x ${screenSize.y}")

// Get full metrics
val metrics = DisplayUtils.getRealDisplayMetrics(context)
val density = metrics.density
val dpi = metrics.densityDpi

// Use in cursor positioning
val cursorX = screenSize.x / 2
val cursorY = screenSize.y / 2
```

---

### ThemeUtils

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/utils/ThemeUtils.kt`

**Purpose:** Glassmorphism theme utilities for VoiceOS Accessibility UI. Provides consistent visual styling matching VoiceCursor design.

#### Glassmorphism Configuration

```kotlin
data class GlassMorphismConfig(
    val cornerRadius: Dp,
    val backgroundOpacity: Float,
    val borderOpacity: Float,
    val borderWidth: Dp,
    val tintColor: Color,
    val tintOpacity: Float,
    val noiseOpacity: Float = 0.05f
)
```

#### Default Configurations

```kotlin
object GlassMorphismDefaults {
    val Primary = GlassMorphismConfig(
        cornerRadius = 16.dp,
        backgroundOpacity = 0.1f,
        borderOpacity = 0.2f,
        borderWidth = 1.dp,
        tintColor = Color(0xFF4285F4),  // Google Blue
        tintOpacity = 0.15f
    )

    val Secondary = GlassMorphismConfig(
        cornerRadius = 12.dp,
        backgroundOpacity = 0.08f,
        borderOpacity = 0.15f,
        borderWidth = 0.5.dp,
        tintColor = Color(0xFF673AB7),  // Deep Purple
        tintOpacity = 0.12f
    )

    val Card = GlassMorphismConfig(...)
    val Button = GlassMorphismConfig(...)
}
```

#### Modifier Extensions

**Basic Glassmorphism:**

```kotlin
fun Modifier.glassMorphism(
    config: GlassMorphismConfig,
    depth: DepthLevel,
    isDarkTheme: Boolean = true
): Modifier {
    val shape = RoundedCornerShape(config.cornerRadius)

    return this
        .clip(shape)
        .background(
            brush = createGlassBrush(config, depth, isDarkTheme),
            shape = shape
        )
        .border(
            width = config.borderWidth,
            brush = createBorderBrush(config, isDarkTheme),
            shape = shape
        )
}
```

**Floating Card:**

```kotlin
fun Modifier.floatingCard(
    elevation: Dp = 8.dp,
    cornerRadius: Dp = 16.dp,
    tintColor: Color = Color(0xFF4285F4)
): Modifier {
    return this.glassMorphism(
        config = GlassMorphismConfig(
            cornerRadius = cornerRadius,
            backgroundOpacity = 0.12f,
            borderOpacity = 0.25f,
            borderWidth = 1.dp,
            tintColor = tintColor,
            tintOpacity = 0.18f
        ),
        depth = DepthLevel(elevation.value / 8f)
    )
}
```

**Interactive Button:**

```kotlin
fun Modifier.interactiveGlass(
    isPressed: Boolean = false,
    tintColor: Color = Color(0xFF4CAF50)
): Modifier {
    val config = if (isPressed) {
        GlassMorphismConfig(
            backgroundOpacity = 0.2f,
            borderOpacity = 0.4f,
            borderWidth = 1.5.dp,
            tintOpacity = 0.3f,
            // ...
        )
    } else {
        GlassMorphismConfig(
            backgroundOpacity = 0.15f,
            borderOpacity = 0.3f,
            borderWidth = 1.dp,
            tintOpacity = 0.2f,
            // ...
        )
    }

    return this.glassMorphism(
        config = config,
        depth = DepthLevel(if (isPressed) 0.2f else 0.6f)
    )
}
```

**Status Indicator:**

```kotlin
fun Modifier.statusIndicator(
    isActive: Boolean,
    activeColor: Color = Color(0xFF00C853),
    inactiveColor: Color = Color(0xFFFF5722)
): Modifier {
    val tintColor = if (isActive) activeColor else inactiveColor

    return this.glassMorphism(
        config = GlassMorphismConfig(
            cornerRadius = 8.dp,
            backgroundOpacity = 0.1f,
            borderOpacity = 0.3f,
            borderWidth = 1.dp,
            tintColor = tintColor,
            tintOpacity = 0.25f
        ),
        depth = DepthLevel(0.4f)
    )
}
```

#### Usage in Compose

```kotlin
@Composable
fun VoiceAccessibilityUI() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Service status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .statusIndicator(isActive = serviceEnabled)
        ) {
            Text("Service Status")
        }

        // Command button
        Button(
            onClick = { /* ... */ },
            modifier = Modifier.interactiveGlass(isPressed = isPressed)
        ) {
            Text("Execute Command")
        }

        // Floating action card
        Card(
            modifier = Modifier.floatingCard(
                elevation = 12.dp,
                tintColor = Color(0xFF2196F3)
            )
        ) {
            Text("Voice Commands Available")
        }
    }
}
```

#### Text Color Utilities

```kotlin
object ThemeUtils {
    fun getTextColor(isDarkTheme: Boolean = true, alpha: Float = 1f): Color {
        return if (isDarkTheme) {
            Color.White.copy(alpha = alpha)
        } else {
            Color.Black.copy(alpha = alpha)
        }
    }

    fun getSecondaryTextColor(isDarkTheme: Boolean = true): Color {
        return getTextColor(isDarkTheme, alpha = 0.7f)
    }

    fun getDisabledTextColor(isDarkTheme: Boolean = true): Color {
        return getTextColor(isDarkTheme, alpha = 0.4f)
    }
}
```

---

## Integration Examples

### Complete Command Flow

```kotlin
// 1. User speaks "open chrome"
VoiceRecognitionClient receives result
    ↓
onResult("open chrome", confidence=0.85, isFinal=true)
    ↓
// 2. Process in ActionCoordinator
actionCoordinator.processVoiceCommand("open chrome", 0.85)
    ↓
interpretVoiceCommand("open chrome") → "launch_app:chrome"
    ↓
// 3. Route to handler
findHandler("launch_app:chrome") → AppHandler
    ↓
// 4. Execute action
AppHandler.execute(APP, "launch_app:chrome", params)
    ↓
// 5. Lookup package name
InstalledAppsManager.appList["open chrome"] → "com.android.chrome"
    ↓
// 6. Launch app
context.startActivity(launchIntent)
```

### UI Scraping Integration

```kotlin
class VoiceOSService : AccessibilityService() {
    @Inject lateinit var installedAppsManager: InstalledAppsManager
    private lateinit var uiScrapingEngine: UIScrapingEngine
    private lateinit var actionCoordinator: ActionCoordinator
    private val debouncer = Debouncer(500L)

    override fun onServiceConnected() {
        // Initialize components
        uiScrapingEngine = UIScrapingEngine(this)
        actionCoordinator = ActionCoordinator(this)
        actionCoordinator.initialize()

        // Observe installed apps
        lifecycleScope.launch {
            installedAppsManager.appList.collect { apps ->
                // Register app commands
                updateCommandRegistry(apps)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Debounce events
        val eventKey = "${event.packageName}:${event.eventType}"
        if (!debouncer.shouldProceed(eventKey)) return

        // Extract UI elements
        lifecycleScope.launch {
            val elements = uiScrapingEngine.extractUIElementsAsync(event)
            val commands = uiScrapingEngine.generateCommandsEnhanced(event)

            // Update command processor
            updateDynamicCommands(commands)
        }
    }

    private fun updateCommandRegistry(apps: Map<String, String>) {
        // Register app launch commands
        for ((command, packageName) in apps) {
            // Commands like "open chrome" → "com.android.chrome"
        }
    }
}
```

### Configuration-Driven Initialization

```kotlin
override fun onServiceConnected() {
    // Load configuration
    val config = ServiceConfiguration.loadFromPreferences(this)

    if (!config.validate()) {
        config = ServiceConfiguration.createDefault()
        config.saveToPreferences(this)
    }

    // Initialize based on configuration
    if (config.handlersEnabled) {
        actionCoordinator = ActionCoordinator(this)
        actionCoordinator.initialize()
    }

    if (config.uiScrapingEnabled) {
        uiScrapingEngine = UIScrapingEngine(this)

        // Load app-specific profiles
        val myFilesReplacements = mapOf(
            "hf_overlay_number" to "",
            "realwear_" to ""
        )
        uiScrapingEngine.loadCommandReplacementProfile(
            "com.realwear.filebrowser",
            myFilesReplacements
        )
    }

    if (config.voiceRecognitionEnabled) {
        voiceClient = VoiceRecognitionClient(this)
        voiceClient.setConnectionCallback(voiceConnectionCallback)
        voiceClient.setRecognitionCallback(voiceRecognitionCallback)
        voiceClient.connect()

        if (config.voiceAutoStart) {
            voiceClient.startRecognition(
                engine = config.voiceEngine,
                language = config.voiceLanguage,
                mode = 0 // continuous
            )
        }
    }
}
```

### Performance Monitoring Integration

```kotlin
class PerformanceMonitor {
    private val debouncer = Debouncer(5000L) // 5 seconds

    fun logPerformanceMetrics(
        coordinator: ActionCoordinator,
        scrapingEngine: UIScrapingEngine
    ) {
        if (!debouncer.shouldProceed("metrics_log")) return

        // ActionCoordinator metrics
        val coordinatorMetrics = coordinator.getMetrics()
        for ((action, data) in coordinatorMetrics) {
            Log.i(TAG, "Action: $action, " +
                "Count: ${data.count}, " +
                "Avg: ${data.averageTimeMs}ms, " +
                "Success: ${(data.successRate * 100).toInt()}%")
        }

        // UIScrapingEngine metrics
        val scrapingMetrics = scrapingEngine.getPerformanceMetrics()
        Log.i(TAG, "UI Scraping: " +
            "Scrapes: ${scrapingMetrics["scrapeCount"]}, " +
            "Cache Hit Rate: ${scrapingMetrics["cacheHitRate"]}, " +
            "Duplicates Filtered: ${scrapingMetrics["duplicatesFiltered"]}")
    }
}
```

---

## Performance Considerations

### ActionCoordinator Performance

**1. Handler Timeout Protection**
- 5-second timeout on all handler executions
- Prevents hung operations from blocking service
- Logged warnings for slow actions (>100ms)

**2. Metrics Tracking**
- Per-action performance metrics
- Success rate tracking
- Average execution time
- Last execution time

**3. Concurrent Handler Registry**
- Thread-safe ConcurrentHashMap
- Efficient category-based lookup
- Priority-based routing

### UIScrapingEngine Performance

**1. Multi-Level Caching**
```
Cache Level 1: Recent extraction (1 second)
    ↓ Cache miss
Cache Level 2: Element cache (1000 elements, LRU)
    ↓ Cache miss
Cache Level 3: Profile cache (20 apps, LRU)
    ↓ Cache miss
Full extraction
```

**2. Debouncing**
- Prevents excessive scraping in dynamic content apps
- 500ms default cooldown
- Per-package event keys

**3. Duplicate Detection**
- Approximate rectangle equality (8px tolerance)
- Prevents duplicate command generation
- Tracked metric: duplicatesFiltered

**4. Performance Limits**
```kotlin
private const val MAX_DEPTH = 50              // Tree depth limit
private const val MIN_ELEMENT_SIZE = 10       // Minimum visible size
private const val MAX_TEXT_LENGTH = 50        // Maximum command text
private const val CACHE_DURATION_MS = 1000L   // 1 second cache
private const val ELEMENT_CACHE_SIZE = 1000   // Max cached elements
private const val PROFILE_CACHE_SIZE = 20     // Max cached profiles
```

### InstalledAppsManager Performance

**1. Background Loading**
- Apps loaded on IO thread
- Non-blocking initialization
- Reactive StateFlow updates

**2. Command Flattening**
- Pre-computed command→package map
- O(1) lookup for app commands
- Sorted app list for consistency

### Configuration Performance

**1. Lazy Loading**
- Loaded once on service start
- Cached for service lifetime
- In-memory configuration object

**2. SharedPreferences Batching**
- Single apply() for all properties
- Batch writes reduce I/O
- Atomic updates

### VoiceRecognitionClient Performance

**1. Coroutine Dispatch**
- Callbacks dispatched to main thread
- Non-blocking AIDL calls
- SupervisorJob prevents cascade failures

**2. Connection Pooling**
- Single service connection
- Reused across recognition sessions
- Proper cleanup on disconnect

### Debouncer Performance

**1. Thread-Safe Operations**
- ConcurrentHashMap for lock-free reads
- Atomic timestamp updates
- No synchronization overhead

**2. Memory Management**
- Automatic cleanup via clearAll()
- Per-key reset capability
- Bounded size via monitoring

### General Optimization Strategies

**1. Lazy Initialization**
```kotlin
private val uiScrapingEngine by lazy { UIScrapingEngine(this) }
private val actionCoordinator by lazy { ActionCoordinator(this) }
```

**2. Weak References**
```kotlin
val nodeInfo: WeakReference<AccessibilityNodeInfo>
```
Prevents memory leaks from retained accessibility nodes.

**3. Coroutine Scoping**
```kotlin
private val coordinatorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
```
Background processing with supervisor for fault tolerance.

**4. StateFlow Instead of LiveData**
```kotlin
private val _appList = MutableStateFlow<Map<String, String>>(mutableMapOf())
val appList: StateFlow<Map<String, String>> = _appList.asStateFlow()
```
Better performance for high-frequency updates.

---

## Summary

The VoiceAccessibility support components provide a robust infrastructure for:

1. **ActionCoordinator** - Sophisticated command routing with timeout protection, performance tracking, and voice command interpretation
2. **InstalledAppsManager** - Reactive app discovery with multi-variant command generation
3. **ServiceConfiguration** - Complete configuration lifecycle with SR6-HYBRID compliance and version migration
4. **AccessibilityModule** - Hilt DI setup with service-scoped dependencies
5. **UIScrapingEngine** - Advanced UI extraction with Legacy Avenue algorithms, multi-level caching, and app-specific rules
6. **VoiceRecognitionClient** - AIDL-based service communication with async callbacks
7. **Utilities** - Debouncing, display metrics, and glassmorphism theming

These components work together to enable sophisticated voice-controlled accessibility functionality with excellent performance characteristics and maintainability.

---

**Document Version:** 1.0
**Created:** 2025-10-10 11:12:00 PDT
**VOS4 Compliance:** Full
