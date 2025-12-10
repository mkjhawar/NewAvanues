# VoiceOSService Developer Documentation

**File:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/VoiceOSService.kt`
**Package:** `com.augmentalis.voiceos.accessibility`
**Created:** 2025-09-02
**Updated:** 2025-09-03
**Last Documented:** 2025-10-10 10:50:00 PDT
**Status:** ACTIVE - Primary implementation

---

## Overview

`VoiceOSService` is the **primary** Android AccessibilityService implementation for VoiceOS. This is a high-performance, production-ready service that provides comprehensive voice control and accessibility features for Android devices.

### Key Features

- **Hilt Dependency Injection** - Modern DI architecture for testability
- **Hybrid Foreground Service** - Smart background management (Android 12+)
- **High Performance** - Optimized caching, lazy loading, event debouncing
- **Voice Recognition** - Integrated Vivoka speech engine
- **VoiceCursor API** - Advanced cursor control system
- **UI Scraping** - Automatic UI element extraction
- **LearnApp Integration** - Third-party app learning
- **Installed Apps Management** - Voice-controlled app launching
- **Lifecycle Awareness** - Process lifecycle monitoring

### Performance Targets

```
Startup Time:        < 1 second
Command Response:    < 100ms
Memory (Idle):       < 15MB
Memory (Active):     < 25MB
CPU (Idle):          < 2%
Event Processing:    < 50ms
UI Scraping:         < 200ms
```

### Architecture Pattern

This implementation merges best practices from multiple sources:
- **Handler Architecture** - From VOSAccessibilitySvc
- **Direct Execution** - From AccessibilityService patterns
- **Configuration** - SR6-HYBRID patterns
- **Performance** - Lazy loading, caching, debouncing

---

## Architecture Role

### System Architecture Position

```
┌────────────────────────────────────────────────────────────┐
│              Android System Framework                      │
│  ┌──────────────────────────────────────────────────┐    │
│  │         AccessibilityManager                     │    │
│  └────────────────────┬─────────────────────────────┘    │
└───────────────────────┼──────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    VoiceOSService                           │
│                @AndroidEntryPoint (Hilt)                    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Singleton Instance (WeakReference)                │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  Injected Components (Hilt):                               │
│  ├─ SpeechEngineManager         (Speech recognition)      │
│  ├─ InstalledAppsManager         (App management)         │
│  └─ ServiceConfiguration         (Settings)               │
│                                                             │
│  Lazy Components:                                          │
│  ├─ UIScrapingEngine             (UI extraction)          │
│  ├─ ActionCoordinator            (Command execution)      │
│  ├─ VoiceCursorAPI               (Cursor control)         │
│  └─ VOS4LearnAppIntegration      (App learning)           │
└───────────────────┬────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┬────────────┐
        ▼           ▼           ▼            ▼
   ┌─────────┐ ┌────────┐ ┌──────────┐ ┌────────┐
   │ Speech  │ │   UI   │ │  Cursor  │ │ Apps   │
   │ Engine  │ │Scraping│ │          │ │Manager │
   └─────────┘ └────────┘ └──────────┘ └────────┘
```

### Component Relationships

```
VoiceOSService
    │
    ├─ Manages ──> SpeechEngineManager
    │                   │
    │                   ├─ Initializes: Vivoka Engine
    │                   ├─ Monitors: Speech state flow
    │                   └─ Updates: Command vocabulary
    │
    ├─ Manages ──> UIScrapingEngine
    │                   │
    │                   ├─ Scrapes: UI elements
    │                   ├─ Generates: Commands
    │                   └─ Caches: Element data
    │
    ├─ Manages ──> ActionCoordinator
    │                   │
    │                   ├─ Executes: Global actions
    │                   ├─ Coordinates: Gestures
    │                   └─ Provides: Action list
    │
    ├─ Manages ──> VoiceCursorAPI
    │                   │
    │                   ├─ Controls: Cursor position
    │                   ├─ Handles: Cursor actions
    │                   └─ Manages: Cursor visibility
    │
    ├─ Manages ──> InstalledAppsManager
    │                   │
    │                   ├─ Tracks: Installed apps
    │                   ├─ Provides: App list flow
    │                   └─ Generates: Launch commands
    │
    └─ Manages ──> VoiceOnSentry
                        │
                        ├─ Starts: Foreground service
                        ├─ Manages: Notifications
                        └─ Handles: Background mic
```

---

## Lifecycle

### Complete Lifecycle Flow

```
onCreate()
   │
   ├─ Set WeakReference singleton
   └─ [Service Created]
   │
   ▼
onServiceConnected()
   │
   ├─ Initialize ServiceConfiguration
   ├─ Configure AccessibilityServiceInfo
   ├─ Register ProcessLifecycleOwner observer
   └─ Launch initialization coroutine:
        │
        ├─ Load static commands (ActionCoordinator)
        ├─ Observe installed apps (InstalledAppsManager)
        ├─ Delay 200ms (non-blocking startup)
        └─ Initialize components:
             │
             ├─ ActionCoordinator.initialize()
             ├─ initializeVoiceRecognition()
             ├─ initializeVoiceCursor()
             ├─ initializeLearnAppIntegration()
             └─ registerVoiceCmd() [command loop]
   │
   ▼
[Service Running]
   │
   ├─ onAccessibilityEvent()
   │  │
   │  ├─ Track event counts
   │  ├─ Apply debouncing
   │  ├─ Forward to LearnApp
   │  └─ Process event types:
   │       │
   │       ├─ TYPE_WINDOW_CONTENT_CHANGED
   │       │    └─> Scrape UI elements
   │       │
   │       ├─ TYPE_WINDOW_STATE_CHANGED
   │       │    └─> Scrape UI elements
   │       │
   │       └─ TYPE_VIEW_CLICKED
   │            └─> Light UI refresh
   │
   ├─ Speech State Collection
   │  │
   │  └─> speechEngineManager.speechState.collect()
   │       │
   │       ├─ Initialize and start listening
   │       └─ Handle transcripts:
   │            └─> handleVoiceCommand()
   │
   ├─ Command Processing Loop
   │  │
   │  └─> registerVoiceCmd() [every 500ms]
   │       │
   │       ├─ Check command cache changes
   │       └─ Update speech engine vocabulary
   │
   ├─ Lifecycle Events
   │  │
   │  ├─ onStart() → App foreground
   │  │    └─> evaluateForegroundServiceNeed()
   │  │
   │  └─ onStop() → App background
   │       └─> evaluateForegroundServiceNeed()
   │            │
   │            └─> Start/Stop VoiceOnSentry
   │
   └─ Static Command Execution
      │
      └─> executeCommand() [from external callers]
   │
   ▼
onInterrupt()
   │
   └─ Log interruption
   │
   ▼
onDestroy()
   │
   ├─ Cleanup LearnApp integration
   ├─ Cleanup VoiceCursor API
   ├─ Destroy UIScrapingEngine
   ├─ Cancel coroutine scopes
   ├─ Clear caches and debouncer
   ├─ Clear singleton reference
   └─ [Service Destroyed]
```

### Initialization Sequence

**Phase 1: Service Creation (onCreate)**
```
onCreate() [Main Thread]
   │
   ├─ instanceRef = WeakReference(this)
   └─ Duration: < 10ms
```

**Phase 2: Service Connection (onServiceConnected)**
```
onServiceConnected() [Main Thread]
   │
   ├─ Load ServiceConfiguration           [~10ms]
   ├─ Configure AccessibilityServiceInfo  [~5ms]
   ├─ Register lifecycle observer         [~1ms]
   └─ Launch async initialization         [~1ms]
   │
   Total: ~17ms (main thread blocking)
```

**Phase 3: Async Initialization**
```
serviceScope.launch [Coroutine]
   │
   ├─ Load static commands               [~50ms]
   ├─ Start observing installed apps     [~20ms]
   ├─ delay(200ms)                       [200ms non-blocking]
   └─ initializeComponents()             [~500ms total]
        │
        ├─ ActionCoordinator              [~50ms]
        ├─ Voice Recognition              [~200ms]
        ├─ VoiceCursor                    [~100ms]
        ├─ LearnApp                       [~100ms]
        └─ Command registration loop      [~50ms]
   │
   Total: ~750ms (non-blocking)
```

**Phase 4: Ready State**
```
isServiceReady = true
   │
   ├─ All components initialized
   ├─ Voice recognition active
   ├─ UI scraping operational
   └─ Command processing active
```

---

## Public API

### Static Methods

#### `getInstance(): VoiceOSService?`

```kotlin
@JvmStatic
fun getInstance(): VoiceOSService?
```

**Purpose:** Retrieve the singleton service instance.

**Returns:**
- `VoiceOSService?` - Current service instance or null if not running

**Thread Safety:** Thread-safe via volatile WeakReference

**Usage:**
```kotlin
val service = VoiceOSService.getInstance()
service?.let {
    // Service is available
    val cursorVisible = it.isCursorVisible()
    Log.d(TAG, "Cursor visible: $cursorVisible")
}
```

---

#### `isServiceRunning(): Boolean`

```kotlin
@JvmStatic
fun isServiceRunning(): Boolean
```

**Purpose:** Check if service is currently active.

**Returns:** `true` if service instance exists

**Usage:**
```kotlin
if (VoiceOSService.isServiceRunning()) {
    VoiceOSService.executeCommand("home")
} else {
    promptUserToEnableService()
}
```

---

#### `executeCommand(commandText: String): Boolean`

```kotlin
@JvmStatic
fun executeCommand(commandText: String): Boolean
```

**Purpose:** Execute global action commands (system-level operations).

**Parameters:**
- `commandText` - Command to execute (case-insensitive)

**Returns:** `true` if command executed successfully

**Supported Commands:**

| Command | Action | Android API |
|---------|--------|-------------|
| `"back"`, `"go back"` | Navigate back | GLOBAL_ACTION_BACK |
| `"home"`, `"go home"` | Go to home screen | GLOBAL_ACTION_HOME |
| `"recent"`, `"recent apps"` | Open recents | GLOBAL_ACTION_RECENTS |
| `"notifications"` | Open notifications | GLOBAL_ACTION_NOTIFICATIONS |
| `"settings"`, `"quick settings"` | Open quick settings | GLOBAL_ACTION_QUICK_SETTINGS |
| `"power"`, `"power menu"` | Open power menu | GLOBAL_ACTION_POWER_DIALOG |
| `"screenshot"` | Take screenshot | GLOBAL_ACTION_TAKE_SCREENSHOT |

**Usage:**
```kotlin
// System navigation
VoiceOSService.executeCommand("back")
VoiceOSService.executeCommand("home")

// Quick actions
VoiceOSService.executeCommand("screenshot")
VoiceOSService.executeCommand("notifications")
```

**Performance:** ~10-50ms for global actions

---

### Instance Methods

#### Cursor Control Methods

##### `showCursor(): Boolean`

```kotlin
fun showCursor(): Boolean
```

**Purpose:** Show the VoiceCursor overlay on screen.

**Returns:**
- `true` if cursor shown successfully
- `false` if VoiceCursor not initialized

**Usage:**
```kotlin
val service = VoiceOSService.getInstance()
val success = service?.showCursor()
if (success == true) {
    Log.d(TAG, "Cursor visible")
}
```

**Prerequisites:** VoiceCursor must be initialized

---

##### `hideCursor(): Boolean`

```kotlin
fun hideCursor(): Boolean
```

**Purpose:** Hide the VoiceCursor overlay.

**Returns:** `true` if successful, `false` if not initialized

**Usage:**
```kotlin
service?.hideCursor()
```

---

##### `toggleCursor(): Boolean`

```kotlin
fun toggleCursor(): Boolean
```

**Purpose:** Toggle cursor visibility (show if hidden, hide if shown).

**Returns:** `true` if toggled successfully

**Usage:**
```kotlin
service?.toggleCursor()  // Toggle visibility
```

---

##### `centerCursor(): Boolean`

```kotlin
fun centerCursor(): Boolean
```

**Purpose:** Move cursor to center of screen.

**Returns:** `true` if centered successfully

**Usage:**
```kotlin
service?.centerCursor()  // Move to center
```

---

##### `clickCursor(): Boolean`

```kotlin
fun clickCursor(): Boolean
```

**Purpose:** Perform click at current cursor position.

**Returns:** `true` if click dispatched successfully

**Usage:**
```kotlin
// Position cursor, then click
service?.showCursor()
// ... user positions cursor with voice/gaze ...
service?.clickCursor()  // Click at current position
```

---

##### `getCursorPosition(): CursorOffset`

```kotlin
fun getCursorPosition(): CursorOffset
```

**Purpose:** Get current cursor screen position.

**Returns:** `CursorOffset` with x, y coordinates

**Usage:**
```kotlin
val position = service?.getCursorPosition()
Log.d(TAG, "Cursor at: (${position.x}, ${position.y})")
```

---

##### `isCursorVisible(): Boolean`

```kotlin
fun isCursorVisible(): Boolean
```

**Purpose:** Check if cursor is currently visible.

**Returns:** `true` if cursor is visible

**Usage:**
```kotlin
if (service?.isCursorVisible() == true) {
    // Cursor is visible
    service.clickCursor()
}
```

---

#### `getAppCommands(): ConcurrentHashMap<String, String>`

```kotlin
fun getAppCommands(): ConcurrentHashMap<String, String>
```

**Purpose:** Get map of voice commands to launch installed apps.

**Returns:** Map of command strings to package names

**Usage:**
```kotlin
val service = VoiceOSService.getInstance()
val appCommands = service?.getAppCommands()

appCommands?.forEach { (command, packageName) ->
    Log.d(TAG, "Command: '$command' -> Package: $packageName")
}

// Example output:
// "open chrome" -> "com.android.chrome"
// "open settings" -> "com.android.settings"
```

---

## Event Handling

### Event Processing Overview

VoiceOSService processes accessibility events with **intelligent debouncing** and **selective scraping** to optimize performance.

### Event Types Processed

#### 1. TYPE_WINDOW_CONTENT_CHANGED

**Trigger:** Window content updated dynamically

**Processing:**
```kotlin
AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
    serviceScope.launch {
        val commands = uiScrapingEngine.extractUIElementsAsync(event)
        nodeCache.clear()
        nodeCache.addAll(commands)

        val normalizedCommand = commands.map { it.normalizedText }
        commandCache.clear()
        commandCache.addAll(normalizedCommand)
    }
}
```

**Use Cases:**
- List items loaded
- Dynamic content updated
- Progress bars changed
- Notifications appeared

**Debouncing:** 1000ms per package-class-type combination

---

#### 2. TYPE_WINDOW_STATE_CHANGED

**Trigger:** New window or activity shown

**Processing:** Same as TYPE_WINDOW_CONTENT_CHANGED (full UI scraping)

**Use Cases:**
- Activity launched
- Dialog shown
- Fragment changed
- App switched

---

#### 3. TYPE_VIEW_CLICKED

**Trigger:** User clicked a UI element

**Processing:** Light UI refresh after click

**Use Cases:**
- Button clicked
- List item selected
- Navigation performed

---

### Event Debouncing

**Purpose:** Prevent excessive UI scraping in apps with dynamic content

**Implementation:**
```kotlin
private val eventDebouncer = Debouncer(EVENT_DEBOUNCE_MS)

// In onAccessibilityEvent()
val debounceKey = "$packageName-${event.className}-${event.eventType}"

if (!eventDebouncer.shouldProceed(debounceKey)) {
    Log.v(TAG, "Event debounced for: $debounceKey")
    return
}
```

**Configuration:**
- Debounce window: 1000ms
- Per-package debouncing
- Per-class debouncing
- Per-event-type debouncing

**Metrics:**
```kotlin
eventDebouncer.getMetrics()
// Returns:
// {
//   "debouncedEvents": 1234,
//   "allowedEvents": 567,
//   "debounceRatio": 0.69
// }
```

---

### Event Count Tracking

**Purpose:** Monitor event processing performance

**Tracked Events:**
```kotlin
private val eventCounts = ArrayMap<Int, AtomicLong>().apply {
    put(AccessibilityEvent.TYPE_VIEW_CLICKED, AtomicLong(0))
    put(AccessibilityEvent.TYPE_VIEW_FOCUSED, AtomicLong(0))
    put(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AtomicLong(0))
    put(AccessibilityEvent.TYPE_VIEW_SCROLLED, AtomicLong(0))
    put(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AtomicLong(0))
    put(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AtomicLong(0))
}
```

**Accessing Metrics:**
```kotlin
// In logPerformanceMetrics()
eventCounts.forEach { (eventType, count) ->
    val eventName = when (eventType) {
        AccessibilityEvent.TYPE_VIEW_CLICKED -> "clicks"
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "windowChanges"
        // ...
    }
    metrics["event_$eventName"] = count.get()
}
```

---

### Valid Packages for Window Change Content

**Purpose:** Allow window content change events only for specific apps

**Configured Packages:**
```kotlin
private val VALID_PACKAGES_WINDOW_CHANGE_CONTENT = setOf(
    "com.realwear.deviceinfo",
    "com.realwear.sysinfo",
    "com.android.systemui"
)
```

**Why:** These apps have dynamic content that requires frequent updates

---

### LearnApp Integration Event Forwarding

**Purpose:** Forward events to LearnApp for third-party app learning

```kotlin
learnAppIntegration?.let { integration ->
    try {
        integration.onAccessibilityEvent(event)
    } catch (e: Exception) {
        Log.e(TAG, "Error forwarding event to LearnApp integration", e)
    }
}
```

---

## Integration Points

### 1. SpeechEngineManager (Hilt Injected)

**Purpose:** Speech recognition and voice command processing

**Injection:**
```kotlin
@javax.inject.Inject
lateinit var speechEngineManager: SpeechEngineManager
```

**Initialization:**
```kotlin
private fun initializeVoiceRecognition() {
    speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)

    serviceScope.launch {
        speechEngineManager.speechState.collect { state ->
            if (state.isInitialized && !state.isListening) {
                isVoiceInitialized = true
                speechEngineManager.startListening()
            }

            if (state.confidence > 0 && state.fullTranscript.isNotBlank()) {
                handleVoiceCommand(
                    confidence = state.confidence,
                    command = state.fullTranscript
                )
            }
        }
    }
}
```

**Speech State Flow:**
```kotlin
data class SpeechState(
    val isInitialized: Boolean,
    val isListening: Boolean,
    val fullTranscript: String,
    val confidence: Float,
    val errorMessage: String?
)
```

**Command Update:**
```kotlin
speechEngineManager.updateCommands(
    commandCache + staticCommandCache + appsCommand.keys
)
```

**Dependencies:** `SpeechEngineManager` from `com.augmentalis.speechrecognition`

---

### 2. InstalledAppsManager (Hilt Injected)

**Purpose:** Track installed apps and provide voice launch commands

**Injection:**
```kotlin
@javax.inject.Inject
lateinit var installedAppsManager: InstalledAppsManager
```

**Observation:**
```kotlin
private fun observeInstalledApps() {
    serviceScope.launch {
        withContext(Dispatchers.Main) {
            installedAppsManager.appList.collectLatest { result ->
                if (result.isNotEmpty()) {
                    appsCommand.apply {
                        clear()
                        putAll(result)
                    }
                    staticCommandCache.addAll(appsCommand.keys)
                }
            }
        }
    }
}
```

**Flow Type:** `Flow<Map<String, String>>`
- Key: Voice command (e.g., "open chrome")
- Value: Package name (e.g., "com.android.chrome")

---

### 3. UIScrapingEngine (Lazy Initialized)

**Purpose:** Extract UI elements and generate voice commands

**Initialization:**
```kotlin
private val uiScrapingEngine by lazy {
    UIScrapingEngine(this).also {
        Log.d(TAG, "UIScrapingEngine initialized (lazy)")
    }
}
```

**Usage:**
```kotlin
val commands = uiScrapingEngine.extractUIElementsAsync(event)
// Returns: List<UIElement>

data class UIElement(
    val normalizedText: String,
    val bounds: Rect,
    val nodeInfo: AccessibilityNodeInfo?
)
```

**Performance Metrics:**
```kotlin
val metrics = uiScrapingEngine.getPerformanceMetrics()
// Returns:
// {
//   "scrapingTime": 120,
//   "elementsExtracted": 47,
//   "cacheHits": 12
// }
```

**Cleanup:**
```kotlin
override fun onDestroy() {
    uiScrapingEngine.destroy()
}
```

---

### 4. ActionCoordinator (Lazy Initialized)

**Purpose:** Execute global actions and coordinate gestures

**Initialization:**
```kotlin
private val actionCoordinator by lazy {
    ActionCoordinator(this).also {
        Log.d(TAG, "ActionCoordinator initialized (lazy)")
    }
}
```

**Usage:**
```kotlin
// Get all available actions
staticCommandCache.addAll(actionCoordinator.getAllActions())

// Execute action
actionCoordinator.executeAction(command)
```

**Available Actions:**
- Navigation: back, home, recent
- System: notifications, settings, power
- Actions: screenshot, lock screen

---

### 5. VoiceCursorAPI (Lazy Initialized)

**Purpose:** Control on-screen cursor for voice/gaze interaction

**Initialization:**
```kotlin
private fun initializeVoiceCursor() {
    try {
        voiceCursorInitialized = VoiceCursorAPI.initialize(this, this)
        if (voiceCursorInitialized) {
            showCursor()
            Log.d(TAG, "VoiceCursor API initialized successfully")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error initializing VoiceCursor API", e)
        voiceCursorInitialized = false
    }
}
```

**API Methods:**
```kotlin
VoiceCursorAPI.showCursor()
VoiceCursorAPI.hideCursor()
VoiceCursorAPI.toggleCursor()
VoiceCursorAPI.centerCursor()
VoiceCursorAPI.click()
VoiceCursorAPI.getCurrentPosition()
VoiceCursorAPI.isVisible()
```

**Cleanup:**
```kotlin
override fun onDestroy() {
    if (voiceCursorInitialized) {
        VoiceCursorAPI.dispose()
        voiceCursorInitialized = false
    }
}
```

---

### 6. VOS4LearnAppIntegration

**Purpose:** Learn third-party apps automatically

**Initialization:**
```kotlin
private fun initializeLearnAppIntegration() {
    try {
        learnAppIntegration = VOS4LearnAppIntegration.initialize(
            applicationContext,
            this
        )
        Log.i(TAG, "✓ LearnApp integration initialized successfully")
    } catch (e: Exception) {
        Log.e(TAG, "✗ Failed to initialize LearnApp integration", e)
        learnAppIntegration = null
    }
}
```

**Event Forwarding:**
```kotlin
learnAppIntegration?.onAccessibilityEvent(event)
```

**Cleanup:**
```kotlin
learnAppIntegration?.cleanup()
learnAppIntegration = null
```

---

### 7. VoiceOnSentry (Foreground Service)

**Purpose:** Maintain microphone access in background (Android 12+)

**Start Condition:**
```kotlin
private fun evaluateForegroundServiceNeed() {
    val needsForeground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && appInBackground
            && voiceSessionActive
            && !foregroundServiceActive

    if (needsForeground) {
        startForegroundServiceHelper()
    }
}
```

**Start:**
```kotlin
private fun startForegroundServiceHelper() {
    val intent = Intent(this, VoiceOnSentry::class.java).apply {
        action = ACTION_START_MIC
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }

    foregroundServiceActive = true
}
```

**Stop:**
```kotlin
private fun stopForegroundServiceHelper() {
    val intent = Intent(this, VoiceOnSentry::class.java).apply {
        action = ACTION_STOP_MIC
    }
    stopService(intent)
    foregroundServiceActive = false
}
```

---

## Threading Model

### Thread Usage Summary

```
Main Thread:
  ├─ Service lifecycle (onCreate, onServiceConnected, onDestroy)
  ├─ Accessibility events (onAccessibilityEvent)
  ├─ Global actions (performGlobalAction)
  └─ UI updates

Coroutine Scope (serviceScope):
  ├─ Dispatcher: Dispatchers.Main
  ├─ Job: SupervisorJob
  ├─ UI scraping
  ├─ Component initialization
  └─ Speech state collection

Coroutine Scope (coroutineScopeCommands):
  ├─ Dispatcher: Dispatchers.IO
  ├─ Job: SupervisorJob
  └─ Command registration loop

Background Threads (via integrations):
  ├─ Speech recognition processing
  ├─ LearnApp exploration
  └─ App list monitoring
```

### Coroutine Scopes

#### 1. serviceScope

**Purpose:** Main service operations

```kotlin
private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
```

**Usage:**
```kotlin
serviceScope.launch {
    val commands = uiScrapingEngine.extractUIElementsAsync(event)
    nodeCache.clear()
    nodeCache.addAll(commands)
}
```

**Lifecycle:** Created implicitly, cancelled in onDestroy()

---

#### 2. coroutineScopeCommands

**Purpose:** Command processing loop

```kotlin
private val coroutineScopeCommands = CoroutineScope(Dispatchers.IO + SupervisorJob())
```

**Usage:**
```kotlin
coroutineScopeCommands.launch {
    try {
        while (isActive) {
            delay(COMMAND_CHECK_INTERVAL_MS)
            if (commandCache != allRegisteredCommands) {
                speechEngineManager.updateCommands(...)
            }
        }
    } catch (e: CancellationException) {
        Log.e(TAG, "Command processing loop cancelled", e)
    }
}
```

**Loop Interval:** 500ms

**Lifecycle:** Started in registerVoiceCmd(), cancelled in onDestroy()

---

### Thread Safety Mechanisms

1. **Volatile Singleton**
   ```kotlin
   @Volatile
   private var instanceRef: WeakReference<VoiceOSService>? = null
   ```

2. **Thread-Safe Collections**
   ```kotlin
   private val nodeCache: MutableList<UIElement> = CopyOnWriteArrayList()
   private val commandCache: MutableList<String> = CopyOnWriteArrayList()
   private val appsCommand = ConcurrentHashMap<String, String>()
   ```

3. **Atomic Operations**
   ```kotlin
   private val isCommandProcessing = AtomicBoolean(false)
   private val eventCounts = ArrayMap<Int, AtomicLong>()
   ```

---

## State Management

### Service State Variables

#### 1. Initialization State

```kotlin
private var isServiceReady = false
@Volatile
private var isVoiceInitialized = false
```

**Purpose:** Track service initialization progress

**Usage:**
```kotlin
if (!isServiceReady || event == null) return

if (isVoiceInitialized) {
    speechEngineManager.updateCommands(...)
}
```

---

#### 2. Singleton Instance

```kotlin
@Volatile
private var instanceRef: WeakReference<VoiceOSService>? = null
```

**Set:** `onCreate()`
**Clear:** `onDestroy()`
**Access:** `getInstance()`

---

#### 3. Foreground Service State

```kotlin
private var foregroundServiceActive = false
private var appInBackground = false
private var voiceSessionActive = false
```

**Purpose:** Track hybrid foreground service requirements

**Logic:**
```kotlin
needsForeground = Android12+ && appInBackground && voiceSessionActive
```

---

#### 4. Cursor State

```kotlin
private var voiceCursorInitialized = false
```

**Purpose:** Track VoiceCursor API initialization status

---

#### 5. Integration State

```kotlin
private var learnAppIntegration: VOS4LearnAppIntegration? = null
```

**Purpose:** Track optional integration availability

---

### Cache State

#### 1. UI Element Cache

```kotlin
private val nodeCache: MutableList<UIElement> = CopyOnWriteArrayList()
```

**Purpose:** Cache scraped UI elements

**Update:** On window state/content changes

**Clear:** Before each scraping operation

---

#### 2. Command Cache

```kotlin
private val commandCache: MutableList<String> = CopyOnWriteArrayList()
```

**Purpose:** Cache voice commands for current screen

**Update:** Synchronized with nodeCache

**Usage:** Speech engine vocabulary update

---

#### 3. Static Command Cache

```kotlin
private val staticCommandCache: MutableList<String> = CopyOnWriteArrayList()
```

**Purpose:** Cache global commands (back, home, etc.)

**Source:** ActionCoordinator.getAllActions()

**Update:** Once at initialization

---

#### 4. App Commands Cache

```kotlin
private val appsCommand = ConcurrentHashMap<String, String>()
```

**Purpose:** Map voice commands to app packages

**Source:** InstalledAppsManager.appList flow

**Update:** Whenever installed apps change

---

#### 5. All Registered Commands

```kotlin
private val allRegisteredCommands: MutableList<String> = CopyOnWriteArrayList()
```

**Purpose:** Track commands already sent to speech engine

**Usage:** Avoid duplicate updates

---

### Command Processing State

```kotlin
private var lastCommandLoaded = 0L
private val isCommandProcessing = AtomicBoolean(false)
```

**Purpose:** Control command registration loop

**Debounce:** 500ms between updates

---

### Configuration State

```kotlin
private lateinit var config: ServiceConfiguration
```

**Loading:**
```kotlin
config = ServiceConfiguration.loadFromPreferences(this)
```

**Properties:**
- `verboseLogging: Boolean`
- `fingerprintGesturesEnabled: Boolean`

---

## Error Handling

### Error Handling Strategy

#### 1. Critical Errors (Crash Service)

**None in current implementation** - Service designed to gracefully handle all errors

#### 2. Component Initialization Errors (Continue Without Feature)

```kotlin
try {
    learnAppIntegration = VOS4LearnAppIntegration.initialize(...)
    Log.i(TAG, "✓ LearnApp integration initialized successfully")
} catch (e: Exception) {
    Log.e(TAG, "✗ Failed to initialize LearnApp integration", e)
    Log.w(TAG, "Service will continue without LearnApp integration")
    learnAppIntegration = null
}
```

**Philosophy:** Optional features should not crash the service

---

#### 3. Event Processing Errors (Log and Continue)

```kotlin
try {
    learnAppIntegration?.onAccessibilityEvent(event)
} catch (e: Exception) {
    Log.e(TAG, "Error forwarding event to LearnApp integration", e)
    Log.e(TAG, "LearnApp error type: ${e.javaClass.simpleName}")
}
```

**Philosophy:** Event processing errors should not interrupt the service

---

#### 4. Cleanup Errors (Best Effort)

```kotlin
try {
    if (learnAppIntegration != null) {
        learnAppIntegration?.cleanup()
        learnAppIntegration = null
    }
} catch (e: Exception) {
    Log.e(TAG, "Error cleaning up LearnApp integration", e)
    Log.e(TAG, "Cleanup error type: ${e.javaClass.simpleName}")
    Log.e(TAG, "Cleanup error message: ${e.message}")
}
```

**Philosophy:** Cleanup errors are logged but don't prevent other cleanup

---

### Logging Standards

**Success:**
```kotlin
Log.i(TAG, "✓ Operation successful")
```

**Failure:**
```kotlin
Log.e(TAG, "✗ Operation failed", exception)
Log.e(TAG, "Error type: ${exception.javaClass.simpleName}")
Log.e(TAG, "Error message: ${exception.message}")
```

**Warning:**
```kotlin
Log.w(TAG, "Warning message")
```

**Debug:**
```kotlin
Log.d(TAG, "Debug information")
```

**Verbose:**
```kotlin
Log.v(TAG, "Verbose details")
```

---

## Examples

### Example 1: Voice Command Processing

```kotlin
class VoiceCommandHandler(
    private val context: Context
) {

    private val service: VoiceOSService?
        get() = VoiceOSService.getInstance()

    suspend fun processVoiceCommand(transcript: String, confidence: Float) {
        // Minimum confidence threshold
        if (confidence < 0.5f) {
            Log.d(TAG, "Low confidence: $confidence")
            return
        }

        val command = transcript.lowercase().trim()

        // Try global actions first (fastest)
        val globalResult = VoiceOSService.executeCommand(command)
        if (globalResult) {
            provideFeedback("Executed: $command")
            return
        }

        // Check if it's an app launch command
        val service = this.service ?: return
        val appCommands = service.getAppCommands()
        if (appCommands.containsKey(command)) {
            val packageName = appCommands[command]
            launchApp(packageName)
            provideFeedback("Opening app")
            return
        }

        // UI interaction commands will be handled by UIScrapingEngine
        // via the command cache system
        provideFeedback("Processing: $command")
    }

    private fun provideFeedback(message: String) {
        // Haptic + visual feedback
    }
}
```

---

### Example 2: Cursor Control Integration

```kotlin
class GazeControlHandler {

    private val service: VoiceOSService?
        get() = VoiceOSService.getInstance()

    fun onGazePositionUpdate(x: Float, y: Float) {
        val service = this.service ?: return

        // Show cursor if not visible
        if (!service.isCursorVisible()) {
            service.showCursor()
        }

        // VoiceCursor API will handle position updates internally
    }

    fun onGazeDwell(duration: Long) {
        val service = this.service ?: return

        // Dwell for 1 second = click
        if (duration >= 1000 && service.isCursorVisible()) {
            service.clickCursor()
            provideFeedback()
        }
    }

    fun onVoiceCommand(command: String) {
        when (command.lowercase()) {
            "click" -> service?.clickCursor()
            "center cursor" -> service?.centerCursor()
            "hide cursor" -> service?.hideCursor()
            "show cursor" -> service?.showCursor()
        }
    }
}
```

---

### Example 3: Monitor Service Performance

```kotlin
class ServiceMonitor : CoroutineScope by CoroutineScope(Dispatchers.Main) {

    fun startMonitoring() {
        launch {
            while (isActive) {
                delay(30_000) // Every 30 seconds

                val service = VoiceOSService.getInstance()
                if (service != null) {
                    logServiceMetrics(service)
                } else {
                    Log.w(TAG, "Service not running")
                }
            }
        }
    }

    private fun logServiceMetrics(service: VoiceOSService) {
        // Check cursor state
        val cursorVisible = service.isCursorVisible()
        val cursorPosition = service.getCursorPosition()

        // Check app commands
        val appCommandCount = service.getAppCommands().size

        // Check service running state
        val isRunning = VoiceOSService.isServiceRunning()

        Log.i(TAG, """
            Service Metrics:
            - Running: $isRunning
            - Cursor Visible: $cursorVisible
            - Cursor Position: (${cursorPosition.x}, ${cursorPosition.y})
            - App Commands: $appCommandCount
        """.trimIndent())
    }
}
```

---

### Example 4: Custom Command Integration

```kotlin
class CustomCommandIntegration(
    private val context: Context
) {

    fun executeCustomWorkflow(workflowName: String) {
        val service = VoiceOSService.getInstance() ?: run {
            showError("Service not available")
            return
        }

        when (workflowName) {
            "morning routine" -> executeMorningRoutine(service)
            "end day" -> executeEndDayRoutine(service)
            "quick notes" -> executeQuickNotes(service)
        }
    }

    private suspend fun executeMorningRoutine(service: VoiceOSService) {
        // Show cursor for user interaction
        service.showCursor()
        delay(500)

        // Open notifications
        VoiceOSService.executeCommand("notifications")
        delay(1000)

        // Go home
        VoiceOSService.executeCommand("home")
        delay(500)

        // Launch calendar app
        val appCommands = service.getAppCommands()
        val calendarCommand = appCommands.keys.find { it.contains("calendar") }
        if (calendarCommand != null) {
            VoiceOSService.executeCommand(calendarCommand)
        }
    }

    private suspend fun executeQuickNotes(service: VoiceOSService) {
        // Open notes app
        val appCommands = service.getAppCommands()
        val notesCommand = appCommands.keys.find { it.contains("notes") }

        if (notesCommand != null) {
            VoiceOSService.executeCommand(notesCommand)
            delay(1000)

            // Position cursor at "New Note" button location
            service.centerCursor()
            delay(500)
            service.clickCursor()
        }
    }
}
```

---

## Related Components

### Core Dependencies

1. **Hilt (Dependency Injection)**
   - `@AndroidEntryPoint` annotation
   - `@Inject` for dependencies
   - Module: `com.google.dagger:hilt-android`

2. **SpeechRecognition Module**
   - Package: `com.augmentalis.speechrecognition`
   - Provides: SpeechEngineManager, SpeechEngine
   - Module: `libraries/SpeechRecognition`

3. **VoiceCursor Module**
   - Package: `com.augmentalis.voiceos.cursor`
   - Provides: VoiceCursorAPI, CursorOffset
   - Module: `apps/VoiceCursor`

4. **UIScrapingEngine**
   - Package: `com.augmentalis.voiceos.accessibility.extractors`
   - Provides: UI element extraction
   - Module: `apps/VoiceAccessibility`

5. **LearnApp Integration**
   - Package: `com.augmentalis.learnapp.integration`
   - Provides: Third-party app learning
   - Module: `integration/learnapp`

### Optional Dependencies

1. **VoiceOnSentry** (Foreground Service)
   - Package: `com.augmentalis.voiceos.accessibility`
   - Purpose: Background microphone access
   - Android 12+ only

2. **InstalledAppsManager**
   - Purpose: Track installed apps
   - Provides: Flow<Map<String, String>>

3. **ActionCoordinator**
   - Purpose: Execute global actions
   - Provides: Command execution

### Successor to

**VoiceAccessibilityService** (Deprecated)
- Legacy implementation
- Lacks Hilt integration
- Lacks hybrid foreground service
- Lacks VoiceCursor integration

---

## Performance Considerations

### Startup Performance

**Optimization Strategies:**
1. **Lazy Initialization**
   - UIScrapingEngine: Lazy property
   - ActionCoordinator: Lazy property
   - VoiceCursor: Async initialization

2. **Staggered Loading**
   - 200ms delay before heavy components
   - Non-blocking coroutine initialization
   - Background thread for IO operations

3. **Minimal Main Thread Blocking**
   - onCreate(): ~10ms
   - onServiceConnected(): ~17ms
   - Total blocking: ~27ms

---

### Runtime Performance

**Optimization Strategies:**
1. **Event Debouncing**
   - 1000ms debounce window
   - Per-package, per-class, per-type
   - Reduces redundant scraping by ~70%

2. **Efficient Caching**
   - CopyOnWriteArrayList for thread-safe caching
   - ConcurrentHashMap for app commands
   - AtomicLong for event counts

3. **Async Processing**
   - UI scraping in coroutines
   - Speech processing in background
   - Non-blocking event handling

---

### Memory Management

**Strategies:**
1. **WeakReference Singleton**
   - Prevents memory leaks
   - Allows GC when service stops

2. **Cache Size Limits**
   - Command cache: Dynamic (screen-dependent)
   - Node cache: Dynamic (screen-dependent)
   - Static cache: Fixed (~20-50 items)

3. **Proper Cleanup**
   - Cancel coroutines in onDestroy()
   - Clear all caches
   - Dispose integrations

**Memory Profile:**
- Idle: < 15MB
- Active: 15-25MB
- Peak: ~30MB (heavy scraping)

---

## Testing Considerations

### Unit Testing

**Testable Components:**
1. Command parsing logic
2. Debouncing algorithm
3. Cache management
4. Event count tracking

**Challenges:**
- Hilt dependency injection
- AccessibilityService framework
- Singleton state

**Solutions:**
- Use Hilt testing libraries
- Mock AccessibilityEvent
- Reset singleton between tests

---

### Integration Testing

**Test Scenarios:**
1. **Service Lifecycle**
   - onCreate → onServiceConnected → onDestroy
   - Verify proper cleanup

2. **Event Processing**
   - Send mock AccessibilityEvents
   - Verify scraping and caching

3. **Voice Commands**
   - Test command execution
   - Verify app launching

4. **Cursor Control**
   - Test cursor visibility
   - Test cursor positioning
   - Test click dispatch

---

### Manual Testing

**Test Checklist:**
- [ ] Service connects on enable
- [ ] Voice recognition initializes
- [ ] UI scraping works on app launch
- [ ] Commands execute correctly
- [ ] Cursor shows and moves
- [ ] App launching works
- [ ] Background mic access (Android 12+)
- [ ] LearnApp integration (if enabled)
- [ ] Performance metrics logged

---

## Migration from VoiceAccessibilityService

### Key Differences

| Feature | VoiceAccessibilityService | VoiceOSService |
|---------|--------------------------|----------------|
| DI Framework | Manual | Hilt |
| Foreground Service | No | Yes (hybrid) |
| VoiceCursor | No | Yes |
| Event Debouncing | No | Yes |
| Performance Metrics | Basic | Advanced |
| App Management | No | Yes |
| Speech Engine | External | Integrated |

### Migration Steps

1. **Update Service Reference**
   ```kotlin
   // Old
   val service = VoiceAccessibilityService.getInstance()

   // New
   val service = VoiceOSService.getInstance()
   ```

2. **Use Cursor API**
   ```kotlin
   // New feature
   service?.showCursor()
   service?.clickCursor()
   ```

3. **Update Manifest**
   ```xml
   <service
       android:name=".accessibility.VoiceOSService"
       android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
   </service>
   ```

4. **Test Thoroughly**
   - All existing commands should work
   - New features available
   - Performance improved

---

## See Also

- [VoiceOnSentry Developer Documentation](./VoiceOnSentry-Developer-Documentation-251010-1050.md)
- [VoiceAccessibilityService Developer Documentation](./VoiceAccessibilityService-Developer-Documentation-251010-1050.md)
- [UIScrapingEngine Documentation](../reference/ui-scraping-engine.md)
- [VoiceCursor API Guide](../../voice-cursor/developer-manual/api-guide.md)
- [SpeechEngine Integration](../../speech-recognition/developer-manual/integration.md)
- [Hilt Dependency Injection Guide](../guides/hilt-setup.md)

---

**Document Version:** 1.0
**Last Updated:** 2025-10-10 10:50:00 PDT
**Author:** VOS4 Documentation Team
**Status:** Complete
