# VoiceOS Infrastructure Components - Developer Manual

**Version:** 1.0
**Date:** 2025-12-22
**Author:** VOS4 Development Team (with Claude AI assistance)
**Audience:** Developers, Contributors
**Status:** Published
**Related:** VoiceOS-P2-Features-Developer-Manual-51211-V1.md

---

# Table of Contents

| Chapter | Title | Page |
|---------|-------|------|
| 1 | [Introduction](#chapter-1-introduction) | Overview of infrastructure components |
| 2 | [Service Infrastructure](#chapter-2-service-infrastructure) | Core service components and utilities |
| 3 | [Managers & Utilities](#chapter-3-managers--utilities) | Application managers and helper classes |
| 4 | [AI & Semantic Analysis](#chapter-4-ai--semantic-analysis) | Semantic inference and AI helpers |
| 5 | [LearnWeb Components](#chapter-5-learnweb-components) | WebView scraping and command generation |
| 6 | [UI Components](#chapter-6-ui-components) | Theme system and widget overlays |
| 7 | [Integration Guide](#chapter-7-integration-guide) | How to use these components |
| A | [API Quick Reference](#appendix-a-api-quick-reference) | Component API summary |
| B | [Code Examples](#appendix-b-code-examples) | Usage examples |

---

# Chapter 1: Introduction

## 1.1 Purpose

This manual documents the infrastructure components created to support VoiceOSCore compilation and functionality. These components provide:

1. **Service Infrastructure**: Core utilities for service health monitoring and event management
2. **Managers**: App tracking, version detection, and lifecycle management
3. **AI Capabilities**: Semantic analysis for UI elements and voice command matching
4. **LearnWeb Support**: WebView DOM extraction and voice command generation
5. **UI Components**: Material3 theming and overlay management

## 1.2 Component Overview

| Component | Purpose | Package |
|-----------|---------|---------|
| IVoiceOSService | Public service API | `accessibility` |
| IVoiceOSServiceInternal | Internal component coordination | `accessibility` |
| Const | Service configuration constants | `accessibility.utils` |
| Debouncer | Event throttling utility | `accessibility.utils` |
| ResourceMonitor | CPU/memory monitoring | `accessibility.utils` |
| EventPriorityManager | Priority-based event queue | `accessibility.utils` |
| InstalledAppsManager | App installation tracking | `accessibility.managers` |
| AppVersionDetector | Version change detection | `version` |
| SemanticInferenceHelper | UI semantic analysis | `learnapp.ai` |
| WebViewScrapingEngine | DOM extraction | `learnweb` |
| WebCommandGenerator | Web command generation | `learnweb` |
| WidgetOverlayHelper | Overlay management | `learnapp.ui.widgets` |
| VUIDCreationMetrics | Metrics tracking | `learnapp.metrics` |

## 1.3 Architecture Context

These components integrate with the existing VoiceOS architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    VoiceOSService                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           IVoiceOSService (Public API)              │   │
│  │  - Service lifecycle, voice control                 │   │
│  │  - Command execution, health checks                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │      IVoiceOSServiceInternal (Internal API)         │   │
│  │  - Component coordination, resource access          │   │
│  │  - Command lifecycle notifications                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│  ┌──────────────┬────────┴────────┬─────────────────┐     │
│  │              │                  │                  │     │
│  ▼              ▼                  ▼                  ▼     │
│ Utils       Managers          LearnApp          LearnWeb   │
│ - Debouncer  - InstalledApps  - Semantic       - Scraping  │
│ - Resource   - VersionDetect  - JIT Learning   - Commands  │
│ - EventQueue                   - Metrics                    │
└─────────────────────────────────────────────────────────────┘
```

## 1.4 Key Design Principles

- **Manual Dependency Injection**: No Hilt (AccessibilityService incompatibility)
- **Coroutine-Based**: All async operations use kotlinx.coroutines
- **Thread-Safe**: Atomic operations and proper synchronization
- **Material3**: Modern UI with Jetpack Compose
- **SQLDelight**: Database operations (KMP compatible)
- **SOLID Principles**: Clean architecture throughout

---

# Chapter 2: Service Infrastructure

## 2.1 IVoiceOSService - Public API

### Overview

Public interface for external components to interact with VoiceOSService.

**Location:** `com.augmentalis.voiceoscore.accessibility.IVoiceOSService`

### API Surface

```kotlin
interface IVoiceOSService {
    // Lifecycle
    fun isServiceReady(): Boolean
    fun isServiceRunning(): Boolean
    fun getStatus(): String
    fun observeStatus(): StateFlow<String>

    // Voice Recognition
    fun startListening(): Boolean
    fun stopListening(): Boolean
    fun isListening(): Boolean

    // Command Execution
    fun executeCommand(commandText: String): Boolean
    fun executeCommand(command: Command): Boolean
    fun isCommandProcessing(): Boolean

    // Accessibility
    fun getRootNodeInActiveWindow(): AccessibilityNodeInfo?
    fun performGlobalAction(action: Int): Boolean

    // Commands & Apps
    fun getAvailableCommands(): List<String>
    fun getAppCommands(): Map<String, String>
    fun getCommandForApp(packageName: String): String?

    // Health Check
    fun checkHealth(): HealthStatus
}
```

### Health Status

```kotlin
data class HealthStatus(
    val status: Status,
    val message: String,
    val details: Map<String, Any> = emptyMap()
) {
    enum class Status {
        HEALTHY,    // All systems operational
        DEGRADED,   // Some issues but functional
        UNHEALTHY   // Critical issues
    }
}
```

### Usage Example

```kotlin
class MyComponent(private val service: IVoiceOSService) {
    fun checkAndExecute() {
        // Check service health
        val health = service.checkHealth()
        if (!health.isHealthy()) {
            Log.w(TAG, "Service unhealthy: ${health.message}")
            return
        }

        // Execute command
        if (service.isServiceReady()) {
            service.executeCommand("open settings")
        }
    }
}
```

## 2.2 IVoiceOSServiceInternal - Internal API

### Overview

Internal interface for component coordination within VoiceOSCore.

**Location:** `com.augmentalis.voiceoscore.accessibility.IVoiceOSServiceInternal`

### API Surface

```kotlin
interface IVoiceOSServiceInternal {
    // Context & Resources
    fun getApplicationContext(): Context
    fun getAccessibilityService(): AccessibilityService
    fun getWindowManager(): WindowManager
    fun getServiceScope(): CoroutineScope
    fun getCommandScope(): CoroutineScope

    // Core Components
    fun getCommandManager(): CommandManager
    fun getDatabaseManager(): DatabaseManager
    fun getIPCManager(): IPCManager
    fun getSpeechEngineManager(): SpeechEngineManager
    fun getVoiceRecognitionManager(): VoiceRecognitionManager?
    fun getUIScrapingEngine(): UIScrapingEngine
    fun getOverlayManager(): OverlayManager
    fun getActionCoordinator(): ActionCoordinator

    // Command Lifecycle
    fun onNewCommandsGenerated()

    // Event Handling
    fun queueAccessibilityEvent(event: AccessibilityEvent, priority: Int = 2)
    fun processQueuedEvents()
}
```

### Usage Example

```kotlin
class LearnAppIntegration(
    private val service: IVoiceOSServiceInternal
) {
    suspend fun processNewCommands() {
        // Get components
        val database = service.getDatabaseManager()
        val commands = database.generatedCommands.getAll()

        // Process commands...

        // Notify service
        service.onNewCommandsGenerated()
    }
}
```

## 2.3 Const - Service Constants

### Overview

Centralized constants for service configuration.

**Location:** `com.augmentalis.voiceoscore.accessibility.utils.Const`

### Categories

#### Broadcast Actions
```kotlin
const val ACTION_CONFIG_UPDATE = "com.augmentalis.voiceoscore.ACTION_CONFIG_UPDATE"
const val ACTION_SERVICE_STATUS = "com.augmentalis.voiceoscore.ACTION_SERVICE_STATUS"
const val ACTION_EXECUTE_COMMAND = "com.augmentalis.voiceoscore.ACTION_EXECUTE_COMMAND"
```

#### Notification Channels
```kotlin
const val CHANNEL_ID_SERVICE = "voiceos_service_channel"
const val CHANNEL_ID_COMMANDS = "voiceos_commands_channel"
const val CHANNEL_ID_ERRORS = "voiceos_errors_channel"
```

#### Resource Thresholds
```kotlin
const val MEMORY_WARNING_THRESHOLD = 80  // Percentage
const val MEMORY_CRITICAL_THRESHOLD = 90
const val CPU_WARNING_THRESHOLD = 70
const val CPU_CRITICAL_THRESHOLD = 85
```

#### Event Priorities
```kotlin
const val PRIORITY_CRITICAL = 0
const val PRIORITY_HIGH = 1
const val PRIORITY_NORMAL = 2
const val PRIORITY_LOW = 3
```

## 2.4 Debouncer - Event Throttling

### Overview

Coroutine-based utility for throttling rapid events.

**Location:** `com.augmentalis.voiceoscore.accessibility.utils.Debouncer`

### Features

- Thread-safe with atomic operations
- Statistics tracking (suppression rate)
- Factory methods for common use cases
- Immediate execution bypass

### API

```kotlin
class Debouncer(
    private val scope: CoroutineScope,
    private val delayMs: Long = 300L
) {
    fun debounce(action: suspend () -> Unit)
    fun immediate(action: suspend () -> Unit)
    fun cancel()
    fun isPending(): Boolean
    fun getStats(): DebouncerStats
    fun resetStats()
}
```

### Usage Example

```kotlin
class VoiceInputHandler(scope: CoroutineScope) {
    private val debouncer = DebouncerFactory.forVoiceEvents(scope)

    fun onVoiceInput(text: String) {
        debouncer.debounce {
            processVoiceCommand(text)
        }
    }

    fun onCriticalCommand(text: String) {
        debouncer.immediate {
            processVoiceCommand(text)
        }
    }
}
```

### Statistics

```kotlin
data class DebouncerStats(
    val eventsReceived: Long,
    val eventsExecuted: Long,
    val eventsSuppressed: Long,
    val isPending: Boolean,
    val timeSinceLastEvent: Long
) {
    val suppressionRate: Double
        get() = (eventsSuppressed.toDouble() / eventsReceived) * 100.0
}
```

## 2.5 ResourceMonitor - Health Monitoring

### Overview

Monitors service health metrics (CPU, memory) with configurable thresholds.

**Location:** `com.augmentalis.voiceoscore.accessibility.utils.ResourceMonitor`

### Features

- Memory monitoring (heap, native, total)
- CPU usage via /proc filesystem
- Health levels: NORMAL, WARNING, CRITICAL
- Periodic monitoring with callbacks
- Garbage collection requests

### API

```kotlin
class ResourceMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val intervalMs: Long = 30000L
) {
    fun start(callback: StatusCallback? = null)
    fun stop()
    fun getStatus(forceUpdate: Boolean = false): ResourceStatus
    fun getMemoryPressure(): String
    fun requestGarbageCollection()
}
```

### Resource Status

```kotlin
data class ResourceStatus(
    val memoryUsedMb: Long,
    val memoryMaxMb: Long,
    val memoryUsagePercent: Int,
    val cpuUsagePercent: Double,
    val level: ResourceLevel,
    val warnings: List<String>
) {
    enum class ResourceLevel {
        NORMAL, WARNING, CRITICAL
    }
}
```

### Usage Example

```kotlin
class VoiceOSService : AccessibilityService() {
    private lateinit var resourceMonitor: ResourceMonitor

    override fun onCreate() {
        super.onCreate()

        resourceMonitor = ResourceMonitor(this, serviceScope)
        resourceMonitor.start { status ->
            when (status.level) {
                ResourceLevel.CRITICAL -> {
                    Log.e(TAG, "Critical resources: ${status.getSummary()}")
                    resourceMonitor.requestGarbageCollection()
                }
                ResourceLevel.WARNING -> {
                    Log.w(TAG, "Resource warning: ${status.getSummary()}")
                }
                else -> {
                    Log.d(TAG, "Resources normal")
                }
            }
        }
    }
}
```

## 2.6 EventPriorityManager - Priority Queue

### Overview

Priority-based event queue for processing events in order of importance.

**Location:** `com.augmentalis.voiceoscore.accessibility.utils.EventPriorityManager`

### Features

- Four priority levels (CRITICAL, HIGH, NORMAL, LOW)
- PriorityBlockingQueue with FIFO per priority
- Bounded queue size (prevents memory issues)
- Statistics tracking
- Thread-safe operations

### API

```kotlin
class EventPriorityManager<T>(
    private val scope: CoroutineScope,
    private val maxQueueSize: Int = 100
) {
    enum class Priority(val value: Int) {
        CRITICAL(0), HIGH(1), NORMAL(2), LOW(3)
    }

    fun start(processor: EventProcessor<T>)
    fun stop()
    fun enqueue(event: T, priority: Priority = Priority.NORMAL): Boolean
    fun clear(): Int
    fun getStats(): QueueStats
}
```

### Usage Example

```kotlin
class AccessibilityEventHandler(scope: CoroutineScope) {
    private val eventQueue = EventPriorityManager<AccessibilityEvent>(scope)

    init {
        eventQueue.start { event, priority ->
            processEvent(event)
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val priority = when (event.eventType) {
            TYPE_VIEW_CLICKED -> Priority.HIGH
            TYPE_WINDOW_STATE_CHANGED -> Priority.NORMAL
            else -> Priority.LOW
        }

        eventQueue.enqueue(event, priority)
    }
}
```

---

# Chapter 3: Managers & Utilities

## 3.1 InstalledAppsManager

### Overview

Tracks and queries installed applications using Android PackageManager.

**Location:** `com.augmentalis.voiceoscore.accessibility.managers.InstalledAppsManager`

### Features

- System vs user app filtering
- Flow-based app observation
- App info retrieval
- Installation status checks

### API

```kotlin
class InstalledAppsManager(private val context: Context) {
    fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfo>
    fun observeAppInstalls(): Flow<List<AppInfo>>
    fun getAppInfo(packageName: String): AppInfo?
    fun isAppInstalled(packageName: String): Boolean
    fun refresh()
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean
)
```

### Usage Example

```kotlin
class AppMonitor(context: Context, scope: CoroutineScope) {
    private val manager = InstalledAppsManager(context)

    init {
        scope.launch {
            manager.observeAppInstalls().collect { apps ->
                Log.d(TAG, "Installed apps: ${apps.size}")
                apps.forEach { app ->
                    if (!app.isSystemApp) {
                        processUserApp(app)
                    }
                }
            }
        }
    }
}
```

## 3.2 AppVersionDetector

### Overview

Detects app version changes (upgrades, downgrades) with persistent tracking.

**Location:** `com.augmentalis.voiceoscore.version.AppVersionDetector`

### Features

- Version change detection
- IAppVersionRepository integration
- Stale app tracking
- Version comparison utilities

### API

```kotlin
class AppVersionDetector(
    private val context: Context,
    private val repository: IAppVersionRepository
) {
    suspend fun getVersion(packageName: String): AppVersion?
    suspend fun detectVersionChange(packageName: String): VersionChange
    fun compareVersions(version1: AppVersion, version2: AppVersion): Int
    suspend fun checkAllTrackedApps(): List<VersionChange>
    suspend fun getStaleApps(maxAgeMillis: Long): Map<String, AppVersionDTO>
}
```

### Version Change Types

```kotlin
sealed class VersionChange {
    data class AppNotInstalled(val packageName: String) : VersionChange()
    data class NewApp(val packageName: String, val version: AppVersion) : VersionChange()
    data class Upgraded(
        val packageName: String,
        val oldVersion: AppVersion,
        val newVersion: AppVersion
    ) : VersionChange()
    data class Downgraded(...) : VersionChange()
    data class NoChange(...) : VersionChange()
    data class Error(...) : VersionChange()
}
```

### Usage Example

```kotlin
class VersionTracker(
    context: Context,
    repository: IAppVersionRepository
) {
    private val detector = AppVersionDetector(context, repository)

    suspend fun checkAppUpdate(packageName: String) {
        when (val change = detector.detectVersionChange(packageName)) {
            is VersionChange.Upgraded -> {
                Log.i(TAG, "App upgraded: ${change.packageName}")
                invalidateOldCommands(packageName, change.oldVersion)
            }
            is VersionChange.NewApp -> {
                Log.i(TAG, "New app detected: ${change.packageName}")
                scheduleExploration(packageName)
            }
            else -> Log.d(TAG, "No version change")
        }
    }
}
```

## 3.3 RenameCommandHandler

### Overview

Handles voice command renaming with validation and TTS feedback.

**Location:** `com.augmentalis.voiceoscore.learnapp.commands.RenameCommandHandler`

### Features

- Name validation (length, characters)
- TTS feedback integration
- Database update placeholder

### Validation Rules

- Minimum length: 2 characters
- Maximum length: 50 characters
- Allowed: alphanumeric and spaces only

### API

```kotlin
class RenameCommandHandler(private val context: Context) {
    suspend fun handleRename(oldName: String, newName: String): RenameResult
    fun validateName(name: String): Boolean
    suspend fun updateCommand(oldName: String, newName: String): Boolean
    fun shutdown()
}

sealed class RenameResult {
    data class Success(val oldName: String, val newName: String) : RenameResult()
    data class Failure(val reason: String) : RenameResult()
}
```

---

# Chapter 4: AI & Semantic Analysis

## 4.1 SemanticInferenceHelper

### Overview

Provides semantic analysis for UI elements and voice command matching.

**Location:** `com.augmentalis.voiceoscore.learnapp.ai.SemanticInferenceHelper`

### Features

- Intent inference (login, signup, submit, etc.)
- Context analysis (form, dialog, list detection)
- Match scoring for voice commands
- Element function inference

### Intent Patterns

```kotlin
val INTENT_PATTERNS = mapOf(
    "login" to listOf("sign in", "log in", "login", "authentication"),
    "signup" to listOf("sign up", "register", "create account", "join"),
    "submit" to listOf("submit", "send", "confirm", "ok", "done"),
    "cancel" to listOf("cancel", "dismiss", "close", "back"),
    "search" to listOf("search", "find", "query", "lookup"),
    "filter" to listOf("filter", "sort", "refine"),
    "settings" to listOf("settings", "preferences", "options"),
    "help" to listOf("help", "support", "faq", "about")
)
```

### API

```kotlin
class SemanticInferenceHelper {
    fun inferIntent(
        text: String?,
        contentDescription: String?,
        className: String?,
        resourceId: String?
    ): String?

    fun analyzeContext(node: AccessibilityNodeInfo?): ContextAnalysis

    fun scoreMatch(
        voiceCommand: String,
        elementText: String?,
        elementDescription: String?,
        elementId: String?
    ): Float

    fun inferElementFunction(node: AccessibilityNodeInfo): ElementFunction
}
```

### Scoring System

| Match Type | Score |
|------------|-------|
| Exact match | 1.0 |
| Partial match | 0.7 |
| Semantic match | 0.6 |
| Context match | 0.5 |

### Usage Example

```kotlin
class CommandMatcher(private val helper: SemanticInferenceHelper) {
    fun findBestMatch(
        voiceCommand: String,
        elements: List<UIElement>
    ): UIElement? {
        return elements
            .map { element ->
                val score = helper.scoreMatch(
                    voiceCommand,
                    element.text,
                    element.contentDescription,
                    element.resourceId
                )
                element to score
            }
            .maxByOrNull { it.second }
            ?.takeIf { it.second > 0.5 }
            ?.first
    }
}
```

---

# Chapter 5: LearnWeb Components

## 5.1 WebViewScrapingEngine

### Overview

Extracts DOM structure from WebView using JavaScript injection.

**Location:** `com.augmentalis.voiceoscore.learnweb.WebViewScrapingEngine`

### Features

- JavaScript DOM traversal
- XPath generation
- ARIA label extraction
- Element visibility detection
- Click and scroll actions

### API

```kotlin
class WebViewScrapingEngine(private val context: Context) {
    suspend fun extractDOMStructure(webView: WebView): List<ScrapedWebElement>
    suspend fun getPageTitle(webView: WebView): String
    suspend fun clickElement(webView: WebView, xpath: String): Boolean
    suspend fun scrollToElement(webView: WebView, xpath: String): Boolean
}
```

### ScrapedWebElement

```kotlin
data class ScrapedWebElement(
    val id: Long,
    val websiteUrlHash: String,
    val elementHash: String,
    val tagName: String,
    val xpath: String,
    val text: String?,
    val ariaLabel: String?,
    val role: String?,
    val parentElementHash: String?,
    val clickable: Boolean,
    val visible: Boolean,
    val bounds: String  // JSON: {"x":..., "y":..., "width":..., "height":...}
)
```

### Usage Example

```kotlin
class LearnWebActivity : AppCompatActivity() {
    private val engine = WebViewScrapingEngine(this)

    private suspend fun scrapeCurrentPage() {
        val elements = engine.extractDOMStructure(webView)
        Log.d(TAG, "Scraped ${elements.size} elements")

        elements.filter { it.clickable }.forEach { element ->
            Log.d(TAG, "Clickable: ${element.text ?: element.ariaLabel}")
        }
    }

    private suspend fun clickButton(buttonText: String) {
        val elements = engine.extractDOMStructure(webView)
        val button = elements.find { it.text == buttonText }

        if (button != null) {
            engine.clickElement(webView, button.xpath)
        }
    }
}
```

## 5.2 WebCommandGenerator

### Overview

Generates voice commands from scraped web elements with natural language variants.

**Location:** `com.augmentalis.voiceoscore.learnweb.WebCommandGenerator`

### Features

- Natural language command text
- Synonym generation
- Action type detection (CLICK, SCROLL_TO, INPUT, FOCUS)
- Quality validation
- Duplicate filtering

### Action Types

```kotlin
const val ACTION_CLICK = "CLICK"        // Buttons, links
const val ACTION_SCROLL_TO = "SCROLL_TO"  // Visible elements
const val ACTION_FOCUS = "FOCUS"        // Focusable elements
const val ACTION_INPUT = "INPUT"        // Input fields
```

### API

```kotlin
class WebCommandGenerator {
    fun generateCommands(
        elements: List<ScrapedWebElement>,
        websiteUrlHash: String
    ): List<GeneratedWebCommand>

    fun generateFromElement(
        element: ScrapedWebElement,
        websiteUrlHash: String,
        timestamp: Long = System.currentTimeMillis()
    ): List<GeneratedWebCommand>

    fun validateCommand(commandText: String, element: ScrapedWebElement): Boolean
    fun filterCommands(commands: List<GeneratedWebCommand>): List<GeneratedWebCommand>
    fun getStatistics(commands: List<GeneratedWebCommand>): Map<String, Any>
}
```

### Command Generation Strategy

Priority for element description:
1. ARIA label (most accessible)
2. Text content
3. ARIA role
4. Tag name

### Usage Example

```kotlin
class WebCommandManager {
    private val engine = WebViewScrapingEngine(context)
    private val generator = WebCommandGenerator()

    suspend fun learnWebPage(webView: WebView, url: String) {
        // Extract elements
        val elements = engine.extractDOMStructure(webView)

        // Generate commands
        val urlHash = HashUtils.generateHash(url)
        val rawCommands = generator.generateCommands(elements, urlHash)

        // Filter duplicates
        val commands = generator.filterCommands(rawCommands)

        // Store in database
        database.saveWebCommands(commands)

        // Log statistics
        val stats = generator.getStatistics(commands)
        Log.d(TAG, "Generated ${stats["total"]} commands")
    }
}
```

---

# Chapter 6: UI Components

## 6.1 VoiceOSTheme - Material3 Theme

### Overview

Material3 theme system for VoiceOS Compose UI.

**Location:** `com.augmentalis.voiceoscore.ui.Theme.kt`

### Features

- Light and dark theme support
- Material3 color schemes
- System theme detection

### API

```kotlin
@Composable
fun VoiceOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
)
```

### Usage Example

```kotlin
class LearnAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LearnAppScreen()
                }
            }
        }
    }
}
```

## 6.2 GlassmorphismUtils

### Overview

Glassmorphism UI effects for modern interfaces.

**Location:** `com.augmentalis.voiceoscore.ui.GlassmorphismUtils.kt`

### API

```kotlin
fun Modifier.glassMorphism(
    config: GlassMorphismConfig = GlassMorphismConfig()
): Modifier

data class GlassMorphismConfig(
    val backgroundColor: Color = Color.White.copy(alpha = 0.1f),
    val alpha: Float = 0.8f,
    val blurRadius: Dp = 16.dp
)

enum class DepthLevel {
    SURFACE, ELEVATED, OVERLAY
}
```

## 6.3 WidgetOverlayHelper

### Overview

Helper for managing widget overlays with WindowManager integration.

**Location:** `com.augmentalis.voiceoscore.learnapp.ui.widgets.WidgetOverlayHelper`

### Features

- WindowManager integration
- Centered dialog positioning
- Overlay lifecycle management
- Position updates
- Active overlay tracking

### API

```kotlin
object WidgetOverlayHelper {
    fun createOverlay(context: Context): View?
    fun createCenteredDialogParams(): WindowManager.LayoutParams
    fun addOverlay(context: Context, view: View, params: LayoutParams? = null): Boolean
    fun removeOverlay(context: Context, view: View): Boolean
    fun showOverlay(view: View)
    fun hideOverlay(view: View)
    fun updateOverlay(view: View, data: Any)
    fun updateOverlayPosition(context: Context, view: View, x: Int, y: Int): Boolean
    fun removeAllOverlays(context: Context)
    fun isOverlayActive(view: View): Boolean
    fun getActiveOverlayCount(): Int
}
```

### Usage Example

```kotlin
class ProgressWidget(private val context: Context) {
    private var overlayView: View? = null

    fun show(message: String) {
        overlayView = WidgetOverlayHelper.createOverlay(context)
        if (overlayView != null) {
            WidgetOverlayHelper.addOverlay(context, overlayView!!)
            WidgetOverlayHelper.updateOverlay(overlayView!!, message)
            WidgetOverlayHelper.showOverlay(overlayView!!)
        }
    }

    fun hide() {
        overlayView?.let {
            WidgetOverlayHelper.removeOverlay(context, it)
            overlayView = null
        }
    }
}
```

## 6.4 VUIDCreationMetrics

### Overview

Metrics tracking for VUID creation during exploration.

**Location:** `com.augmentalis.voiceoscore.learnapp.metrics.VUIDCreationMetrics`

### Features

- Creation rate calculation
- Filter rate tracking
- Error rate monitoring
- Health checks
- Builder pattern
- Aggregation support

### API

```kotlin
data class VUIDCreationMetrics(
    val vuidsCreated: Int = 0,
    val elementsDetected: Int = 0,
    val filteredCount: Int = 0,
    val errorCount: Int = 0,
    val explorationTimestamp: Long = System.currentTimeMillis()
) {
    fun getCreationRate(): Float
    fun getFilterRate(): Float
    fun getErrorRate(): Float
    fun isHealthy(): Boolean
    fun toSummaryString(): String

    companion object {
        fun empty(): VUIDCreationMetrics
        fun combine(metricsList: List<VUIDCreationMetrics>): VUIDCreationMetrics
    }
}
```

### Usage Example

```kotlin
class ExplorationEngine {
    private val metricsBuilder = VUIDCreationMetricsBuilder()

    suspend fun exploreScreen(packageName: String): ScreenExplorationResult {
        val elements = detectElements()
        metricsBuilder.setElementsDetected(elements.size)

        elements.forEach { element ->
            if (shouldFilter(element)) {
                metricsBuilder.incrementFilteredCount()
            } else {
                try {
                    createVUID(element)
                    metricsBuilder.incrementVuidsCreated()
                } catch (e: Exception) {
                    metricsBuilder.incrementErrorCount()
                }
            }
        }

        val metrics = metricsBuilder.build()
        Log.d(TAG, metrics.toSummaryString())

        if (!metrics.isHealthy()) {
            Log.w(TAG, "Exploration health issues detected")
        }

        return ScreenExplorationResult(packageName, metrics)
    }
}
```

---

# Chapter 7: Integration Guide

## 7.1 Service Integration

### Step 1: Access Service API

```kotlin
class MyIntegration(private val service: IVoiceOSServiceInternal) {
    init {
        // Check service readiness
        if (service.isInitialized()) {
            initialize()
        }
    }
}
```

### Step 2: Use Utilities

```kotlin
class EventHandler(private val scope: CoroutineScope) {
    private val debouncer = DebouncerFactory.forVoiceEvents(scope)
    private val eventQueue = EventPriorityManager<AccessibilityEvent>(scope)

    init {
        eventQueue.start { event, priority ->
            debouncer.debounce {
                handleEvent(event)
            }
        }
    }
}
```

### Step 3: Monitor Resources

```kotlin
class ServiceMonitor(context: Context, scope: CoroutineScope) {
    private val monitor = ResourceMonitor(context, scope)

    init {
        monitor.start { status ->
            if (status.level == ResourceLevel.CRITICAL) {
                handleCritical(status)
            }
        }
    }
}
```

## 7.2 LearnWeb Integration

### Step 1: Scrape Web Page

```kotlin
class WebLearner(context: Context) {
    private val engine = WebViewScrapingEngine(context)
    private val generator = WebCommandGenerator()

    suspend fun learnPage(webView: WebView, url: String) {
        val elements = engine.extractDOMStructure(webView)
        val urlHash = HashUtils.generateHash(url)
        val commands = generator.generateCommands(elements, urlHash)
        storeCommands(commands)
    }
}
```

## 7.3 Metrics Integration

### Track Exploration Metrics

```kotlin
class MetricsTracker {
    private val builder = VUIDCreationMetricsBuilder()

    fun trackExploration(exploration: Exploration) {
        builder.setElementsDetected(exploration.totalElements)
        builder.setVuidsCreated(exploration.successfulVUIDs)
        builder.setFilteredCount(exploration.filteredElements)
        builder.setErrorCount(exploration.errors)

        val metrics = builder.build()
        repository.saveMetrics(metrics)
    }
}
```

---

# Appendix A: API Quick Reference

## Service Infrastructure

| Component | Key Methods | Purpose |
|-----------|-------------|---------|
| IVoiceOSService | `executeCommand()`, `checkHealth()` | Public API |
| IVoiceOSServiceInternal | `getCommandManager()`, `onNewCommandsGenerated()` | Internal coordination |
| Debouncer | `debounce()`, `immediate()` | Event throttling |
| ResourceMonitor | `start()`, `getStatus()` | Health monitoring |
| EventPriorityManager | `enqueue()`, `start()` | Priority queue |

## Managers

| Component | Key Methods | Purpose |
|-----------|-------------|---------|
| InstalledAppsManager | `getInstalledApps()`, `observeAppInstalls()` | App tracking |
| AppVersionDetector | `detectVersionChange()`, `checkAllTrackedApps()` | Version detection |
| RenameCommandHandler | `handleRename()`, `validateName()` | Command renaming |

## AI & LearnWeb

| Component | Key Methods | Purpose |
|-----------|-------------|---------|
| SemanticInferenceHelper | `inferIntent()`, `scoreMatch()` | Semantic analysis |
| WebViewScrapingEngine | `extractDOMStructure()`, `clickElement()` | DOM extraction |
| WebCommandGenerator | `generateCommands()`, `filterCommands()` | Command generation |

## UI & Metrics

| Component | Key Methods | Purpose |
|-----------|-------------|---------|
| VoiceOSTheme | `VoiceOSTheme { }` | Material3 theme |
| WidgetOverlayHelper | `addOverlay()`, `removeOverlay()` | Overlay management |
| VUIDCreationMetrics | `isHealthy()`, `combine()` | Metrics tracking |

---

# Appendix B: Code Examples

## Complete Integration Example

```kotlin
class ComprehensiveIntegration(
    context: Context,
    private val service: IVoiceOSServiceInternal
) {
    private val scope = service.getServiceScope()

    // Infrastructure
    private val debouncer = DebouncerFactory.forVoiceEvents(scope)
    private val resourceMonitor = ResourceMonitor(context, scope)
    private val eventQueue = EventPriorityManager<AccessibilityEvent>(scope)

    // Managers
    private val appsManager = InstalledAppsManager(context)
    private val versionDetector = AppVersionDetector(
        context,
        service.getDatabaseManager().appVersions
    )

    // AI & LearnWeb
    private val semanticHelper = SemanticInferenceHelper()
    private val webEngine = WebViewScrapingEngine(context)
    private val commandGenerator = WebCommandGenerator()

    init {
        setupMonitoring()
        setupEventProcessing()
    }

    private fun setupMonitoring() {
        resourceMonitor.start { status ->
            when (status.level) {
                ResourceLevel.CRITICAL -> handleCritical(status)
                ResourceLevel.WARNING -> handleWarning(status)
                else -> Log.d(TAG, "Resources OK")
            }
        }
    }

    private fun setupEventProcessing() {
        eventQueue.start { event, priority ->
            debouncer.debounce {
                processAccessibilityEvent(event)
            }
        }
    }

    suspend fun learnWebPage(webView: WebView, url: String) {
        // Extract DOM
        val elements = webEngine.extractDOMStructure(webView)

        // Generate commands
        val urlHash = HashUtils.generateHash(url)
        val rawCommands = commandGenerator.generateCommands(elements, urlHash)
        val commands = commandGenerator.filterCommands(rawCommands)

        // Store commands
        service.getDatabaseManager().webCommands.insertAll(commands)
        service.onNewCommandsGenerated()
    }

    suspend fun checkAppUpdates() {
        val apps = appsManager.getInstalledApps()

        apps.forEach { app ->
            when (val change = versionDetector.detectVersionChange(app.packageName)) {
                is VersionChange.Upgraded -> handleUpgrade(app, change)
                is VersionChange.NewApp -> handleNewApp(app)
                else -> {}
            }
        }
    }
}
```

---

**End of Developer Manual**

For updates and contributions, contact: Manoj Jhawar
Last Updated: 2025-12-22
