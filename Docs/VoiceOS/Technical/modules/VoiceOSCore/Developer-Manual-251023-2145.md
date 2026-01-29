# VoiceOS Developer Manual

**Module**: VoiceOSCore (`/app`)
**Created**: 2025-10-23 21:45:25 PDT
**Version**: 1.0.0
**Target Audience**: Android developers, VOS4 contributors

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [Handler Architecture](#handler-architecture)
4. [Extension Guide](#extension-guide)
5. [Integration Points](#integration-points)
6. [API Reference](#api-reference)
7. [Testing Guide](#testing-guide)
8. [APK Size Optimization](#apk-size-optimization)

---

## Architecture Overview

VoiceOS is an Android Accessibility Service that enables comprehensive voice control of Android devices. The application follows a handler-based architecture with a three-tier command execution system.

### Key Architectural Principles

1. **Direct Implementation**: No unnecessary interfaces except where strategic value exists (ActionHandler for polymorphism)
2. **Lazy Initialization**: Components initialized only when needed
3. **Performance-First**: Caching, debouncing, concurrent data structures
4. **Hybrid Foreground Service**: Only active on Android 12+ when needed for background mic access

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     VoiceOS Service                         │
│                  (AccessibilityService)                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ Voice Input  │  │  UI Scraping │  │  Action         │  │
│  │ (Speech Eng) │  │  Engine      │  │  Coordinator    │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬────────┘  │
│         │                  │                    │           │
│         └──────────────────┴────────────────────┘           │
│                          │                                  │
│         ┌────────────────┴─────────────────┐               │
│         │    Command Processing Tiers      │               │
│         │  1. CommandManager (Primary)     │               │
│         │  2. VoiceCommandProcessor        │               │
│         │  3. ActionCoordinator (Fallback) │               │
│         └────────────────┬─────────────────┘               │
│                          │                                  │
│         ┌────────────────┴─────────────────┐               │
│         │         Handler Layer            │               │
│         │  ┌───────────────────────────┐   │               │
│         │  │ AppHandler                │   │               │
│         │  │ DeviceHandler             │   │               │
│         │  │ GestureHandler            │   │               │
│         │  │ NavigationHandler         │   │               │
│         │  │ SystemHandler             │   │               │
│         │  │ InputHandler              │   │               │
│         │  │ SelectHandler             │   │               │
│         │  │ NumberHandler             │   │               │
│         │  │ UIHandler                 │   │               │
│         │  │ BluetoothHandler          │   │               │
│         │  └───────────────────────────┘   │               │
│         └──────────────────────────────────┘               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Command Execution Flow

```
Voice Command → Speech Recognition → Command Processing
                                            ↓
                        ┌───────────────────┴────────────────┐
                        │                                    │
                   Web Tier?                            Regular Tiers
                (Browser only)                               ↓
                        ↓                         ┌──────────┴──────────┐
              WebCommandCoordinator               │                     │
                        ↓                     Tier 1             Success?
                   Success?              CommandManager              ↓
                        ↓                         ↓                  Done
                     Done                   Success?
                                                  ↓                   ↓
                                              Tier 2              Tier 3
                                       VoiceCommandProcessor  ActionCoordinator
                                                  ↓                   ↓
                                             Success?            Handler Execution
                                                  ↓
                                              Tier 3
                                         ActionCoordinator
```

---

## Core Components

### 1. VoiceOSService

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`

Main accessibility service that coordinates all VoiceOS functionality.

#### Lifecycle Methods

##### `onCreate()`

**Purpose**: Initialize database and service instance

**Signature**:
```kotlin
override fun onCreate()
```

**Description**:
- Sets up singleton instance reference
- Initializes AppScrapingDatabase early for command persistence
- Falls back to in-memory cache if database initialization fails

**Example**:
```kotlin
// Called automatically by Android system
// No direct invocation needed
```

---

##### `onServiceConnected()`

**Purpose**: Configure service and initialize all components

**Signature**:
```kotlin
override fun onServiceConnected()
```

**Description**:
- Loads service configuration from preferences
- Configures accessibility service flags and capabilities
- Registers lifecycle observer for foreground service management
- Initializes all subsystems asynchronously:
  - Action coordinator and static commands
  - Installed apps observer
  - VoiceCursor API
  - LearnApp integration
  - CommandManager with ServiceMonitor
  - Voice recognition with speech engine

**Call Flow**:
```kotlin
onServiceConnected()
  ↓
configureServiceInfo()
  ↓
observeInstalledApps()
  ↓
initializeComponents()
  ├─ actionCoordinator.initialize()
  ├─ scrapingIntegration initialization
  ├─ voiceCommandProcessor initialization
  └─ initializeVoiceRecognition()
  ↓
initializeVoiceCursor()
  ↓
initializeLearnAppIntegration()
  ↓
initializeCommandManager()
  ↓
registerVoiceCmd()
```

---

##### `onAccessibilityEvent(event: AccessibilityEvent?)`

**Purpose**: Process accessibility events from Android system

**Signature**:
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?)
```

**Parameters**:
- `event`: Accessibility event from system (can be null)

**Returns**: Nothing (void)

**Description**:
Processes accessibility events to:
- Forward to AccessibilityScrapingIntegration for UI scraping
- Forward to LearnApp integration for app learning
- Track event counts for performance monitoring
- Update UI element caches based on window/content changes
- Apply debouncing to prevent excessive processing

**Event Types Handled**:
- `TYPE_WINDOW_CONTENT_CHANGED`: Update scraped UI elements
- `TYPE_WINDOW_STATE_CHANGED`: Trigger scraping on new windows
- `TYPE_VIEW_CLICKED`: Refresh commands after clicks

**Example Flow**:
```kotlin
User interacts with app
  ↓
Android sends TYPE_WINDOW_CONTENT_CHANGED event
  ↓
onAccessibilityEvent() receives event
  ↓
Forward to scrapingIntegration.onAccessibilityEvent()
  ↓
Forward to learnAppIntegration.onAccessibilityEvent()
  ↓
Extract UI elements via uiScrapingEngine
  ↓
Update nodeCache and commandCache
  ↓
speechEngineManager auto-updates vocabulary
```

---

##### `onDestroy()`

**Purpose**: Clean up all resources and cancel operations

**Signature**:
```kotlin
override fun onDestroy()
```

**Description**:
Cleanup order (safe degradation):
1. AccessibilityScrapingIntegration cleanup
2. VoiceCommandProcessor reference cleared
3. Scraping database reference cleared (Room manages lifecycle)
4. LearnApp integration cleanup
5. VoiceCursor API disposal
6. UIScrapingEngine destruction
7. Coroutine scope cancellation
8. Cache clearing (command, node, debouncer)
9. ServiceMonitor cleanup
10. CommandManager cleanup
11. Instance reference cleared

**Example**:
```kotlin
// Called automatically by Android when service stops
// All cleanup is automatic
```

---

#### Command Processing Methods

##### `handleVoiceCommand(command: String, confidence: Float)`

**Purpose**: Process voice command with confidence scoring

**Signature**:
```kotlin
private fun handleVoiceCommand(command: String, confidence: Float)
```

**Parameters**:
- `command`: Raw voice command text
- `confidence`: Recognition confidence (0.0 to 1.0)

**Returns**: Nothing (void)

**Description**:
Entry point for all voice commands. Routes to appropriate tier:
- Rejects commands with confidence < 0.5
- Checks for web commands (browser context)
- Routes to regular tier system for non-web commands

**Example**:
```kotlin
// Internal flow triggered by speech recognition
speechState.collect { state ->
    if (state.confidence > 0 && state.fullTranscript.isNotBlank()) {
        handleVoiceCommand(
            confidence = state.confidence,
            command = state.fullTranscript
        )
    }
}
```

---

##### `executeCommand(commandText: String): Boolean`

**Purpose**: Public static method to execute commands from external sources

**Signature**:
```kotlin
companion object {
    @JvmStatic
    fun executeCommand(commandText: String): Boolean
}
```

**Parameters**:
- `commandText`: Command to execute

**Returns**: `true` if command executed successfully, `false` otherwise

**Description**:
Static method for executing simple system commands:
- back, go back
- home, go home
- recent, recent apps
- notifications
- settings, quick settings
- power, power menu
- screenshot

For complex commands, delegates to handler architecture.

**Example**:
```kotlin
// From external component
val success = VoiceOSService.executeCommand("go home")
if (success) {
    Log.d(TAG, "Navigation command executed")
}
```

---

##### `createCommandContext(): CommandContext`

**Purpose**: Create snapshot of current accessibility state

**Signature**:
```kotlin
private fun createCommandContext(): CommandContext
```

**Returns**: CommandContext with current state

**Description**:
Captures current accessibility service state for command execution:
- Package name of active window
- Activity class name
- Currently focused element
- Device state (has root, child count, accessibility focus)
- Custom data (cache sizes, fallback mode)

**Example**:
```kotlin
// Internal use when creating Command objects
val cmd = Command(
    id = normalizedCommand,
    text = normalizedCommand,
    source = CommandSource.VOICE,
    context = createCommandContext(), // <-- Creates snapshot
    confidence = confidence,
    timestamp = System.currentTimeMillis()
)
```

---

#### Tier Execution Methods

##### `handleRegularCommand(normalizedCommand: String, confidence: Float)`

**Purpose**: Route command through tier system

**Signature**:
```kotlin
private fun handleRegularCommand(normalizedCommand: String, confidence: Float)
```

**Parameters**:
- `normalizedCommand`: Lowercase, trimmed command
- `confidence`: Recognition confidence

**Description**:
Executes three-tier fallback system:
1. Try CommandManager (if available and not in fallback mode)
2. Try VoiceCommandProcessor (app-specific commands)
3. Try ActionCoordinator (handler-based fallback)

---

##### `executeTier2Command(normalizedCommand: String)`

**Purpose**: Execute via VoiceCommandProcessor (database lookup)

**Signature**:
```kotlin
private suspend fun executeTier2Command(normalizedCommand: String)
```

**Parameters**:
- `normalizedCommand`: Command text

**Description**:
Attempts execution via VoiceCommandProcessor for app-specific commands from scraping database. Falls through to Tier 3 on failure.

---

##### `executeTier3Command(normalizedCommand: String)`

**Purpose**: Execute via ActionCoordinator (final fallback)

**Signature**:
```kotlin
private suspend fun executeTier3Command(normalizedCommand: String)
```

**Parameters**:
- `normalizedCommand`: Command text

**Description**:
Final fallback tier using handler-based ActionCoordinator. Always executes (no further fallback).

---

#### Database Command Management

##### `registerDatabaseCommands()`

**Purpose**: Load commands from all databases and register with speech engine

**Signature**:
```kotlin
private suspend fun registerDatabaseCommands()
```

**Description**:
Loads commands from three sources:
1. **CommandDatabase**: VOSCommandIngestion data (94 commands)
   - Primary texts and synonyms
   - Filtered by current locale
2. **AppScrapingDatabase**: Generated app-specific commands
   - Commands from scraped apps
   - Synonyms for variations
3. **WebScrapingDatabase**: Learned web commands
   - Browser-specific commands

All commands are:
- Normalized to lowercase
- Deduplicated via Set
- Added to staticCommandCache
- Registered with speechEngineManager

**Example**:
```kotlin
// Automatically called after CommandManager initialization
initializeCommandManager()
  ↓
serviceScope.launch {
    delay(500) // Ensure systems ready
    registerDatabaseCommands()
}
  ↓
Speech engine vocabulary updated with all commands
```

---

##### `onNewCommandsGenerated()`

**Purpose**: Trigger re-registration after new commands are created

**Signature**:
```kotlin
fun onNewCommandsGenerated()
```

**Description**:
Called by scraping integration when new commands are generated from app exploration. Triggers `registerDatabaseCommands()` to update speech vocabulary.

**Example**:
```kotlin
// Called by AccessibilityScrapingIntegration after scraping
scrapingIntegration.onScrapingComplete { newCommands ->
    if (newCommands.isNotEmpty()) {
        service.onNewCommandsGenerated()
    }
}
```

---

#### VoiceCursor Integration

##### `initializeVoiceCursor()`

**Purpose**: Initialize VoiceCursor API for cursor-based control

**Signature**:
```kotlin
private fun initializeVoiceCursor()
```

**Description**:
Initializes VoiceCursor module for mouse-like cursor control via voice. Shows cursor on successful initialization.

---

##### `showCursor(): Boolean`

**Purpose**: Display voice cursor overlay

**Signature**:
```kotlin
fun showCursor(): Boolean
```

**Returns**: `true` if successful, `false` if VoiceCursor not initialized

**Example**:
```kotlin
if (service.showCursor()) {
    Log.d(TAG, "Cursor displayed")
}
```

---

##### `hideCursor(): Boolean`

**Purpose**: Hide voice cursor overlay

**Signature**:
```kotlin
fun hideCursor(): Boolean
```

**Returns**: `true` if successful

---

##### `toggleCursor(): Boolean`

**Purpose**: Toggle cursor visibility

**Signature**:
```kotlin
fun toggleCursor(): Boolean
```

**Returns**: `true` if successful

---

##### `centerCursor(): Boolean`

**Purpose**: Move cursor to screen center

**Signature**:
```kotlin
fun centerCursor(): Boolean
```

**Returns**: `true` if successful

---

##### `clickCursor(): Boolean`

**Purpose**: Perform click at current cursor position

**Signature**:
```kotlin
fun clickCursor(): Boolean
```

**Returns**: `true` if successful

**Example**:
```kotlin
// Voice command "cursor click"
service.clickCursor()
```

---

##### `getCursorPosition(): CursorOffset`

**Purpose**: Get current cursor screen coordinates

**Signature**:
```kotlin
fun getCursorPosition(): CursorOffset
```

**Returns**: CursorOffset(x, y) or screen center if unavailable

---

##### `isCursorVisible(): Boolean`

**Purpose**: Check cursor visibility state

**Signature**:
```kotlin
fun isCursorVisible(): Boolean
```

**Returns**: `true` if cursor is visible

---

#### Foreground Service Management

##### `evaluateForegroundServiceNeed()`

**Purpose**: Determine if foreground service is needed (hybrid approach)

**Signature**:
```kotlin
private fun evaluateForegroundServiceNeed()
```

**Description**:
Starts foreground service only when ALL conditions met:
- Android 12+ (Build.VERSION_CODES.S)
- App in background
- Voice session active

Stops foreground service when no longer needed.

---

##### `startForegroundServiceHelper()`

**Purpose**: Start VoiceOnSentry foreground service

**Signature**:
```kotlin
private fun startForegroundServiceHelper()
```

**Description**:
Starts VoiceOnSentry foreground service for background mic access on Android 12+.

---

##### `stopForegroundServiceHelper()`

**Purpose**: Stop foreground service

**Signature**:
```kotlin
private fun stopForegroundServiceHelper()
```

---

#### Utility Methods

##### `getAppCommands(): Map<String, String>`

**Purpose**: Get map of app voice commands to package names

**Signature**:
```kotlin
fun getAppCommands(): Map<String, String>
```

**Returns**: Map of command text to package name

**Example**:
```kotlin
val appCommands = service.getAppCommands()
// {"open chrome" -> "com.android.chrome", ...}
```

---

##### `enableFallbackMode()`

**Purpose**: Enable fallback mode when CommandManager unavailable

**Signature**:
```kotlin
fun enableFallbackMode()
```

**Description**:
Called by ServiceMonitor during graceful degradation. Bypasses Tier 1 (CommandManager) and routes directly to Tier 2/3.

---

#### Companion Object (Static Methods)

##### `getInstance(): VoiceOSService?`

**Purpose**: Get current service instance

**Signature**:
```kotlin
companion object {
    @JvmStatic
    fun getInstance(): VoiceOSService?
}
```

**Returns**: Current service instance or null

**Example**:
```kotlin
VoiceOSService.getInstance()?.let { service ->
    service.showCursor()
}
```

---

##### `isServiceRunning(): Boolean`

**Purpose**: Check if service is active

**Signature**:
```kotlin
companion object {
    @JvmStatic
    fun isServiceRunning(): Boolean
}
```

**Returns**: `true` if service instance exists

---

### 2. VoiceOnSentry

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOnSentry.kt`

Lightweight foreground service for background microphone access on Android 12+.

#### Purpose

- Provides foreground notification for microphone access
- Required only on Android 12+ (API 31+) when app is in background
- Automatically started/stopped based on need
- Minimal battery impact with low-priority notification

#### Key Methods

##### `onStartCommand(intent: Intent?, flags: Int, startId: Int): Int`

**Purpose**: Handle service start requests

**Actions**:
- `ACTION_START_MIC`: Start foreground service with mic notification
- `ACTION_STOP_MIC`: Stop service and notification
- `ACTION_UPDATE_STATE`: Update notification state (IDLE, LISTENING, PROCESSING, ERROR)

**Returns**: `START_NOT_STICKY` (don't restart if killed - saves battery)

---

##### `startMicService()`

**Purpose**: Start foreground service with MICROPHONE type

**Signature**:
```kotlin
private fun startMicService()
```

**Description**:
- Creates notification showing mic state
- Starts foreground service with `FOREGROUND_SERVICE_TYPE_MICROPHONE`
- Updates currentState to LISTENING

---

##### `buildNotification(state: MicState): Notification`

**Purpose**: Create minimal notification for mic access

**Signature**:
```kotlin
private fun buildNotification(state: MicState): Notification
```

**Parameters**:
- `state`: Current mic state (IDLE, LISTENING, PROCESSING, ERROR)

**Returns**: Notification object

**Notification Characteristics**:
- Low priority (PRIORITY_LOW)
- Silent (no sound)
- No vibration
- No timestamp
- Ongoing (can't swipe away)
- Click returns to app

---

### 3. ActionCoordinator

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/ActionCoordinator.kt`

Central routing system for all action execution.

#### Purpose

- Routes actions to appropriate handlers
- Manages handler lifecycle
- Tracks performance metrics
- Provides voice command interpretation

#### Key Methods

##### `initialize()`

**Purpose**: Register and initialize all handlers

**Signature**:
```kotlin
fun initialize()
```

**Description**:
Registers handlers for all categories:
- SYSTEM: SystemHandler
- APP: AppHandler
- DEVICE: DeviceHandler, BluetoothHandler
- INPUT: InputHandler
- NAVIGATION: NavigationHandler
- UI: UIHandler, HelpMenuHandler, SelectHandler, NumberHandler
- GESTURE: GestureHandler, DragHandler

Then initializes all handlers.

---

##### `executeAction(action: String, params: Map<String, Any> = emptyMap()): Boolean`

**Purpose**: Execute action by finding and invoking appropriate handler

**Signature**:
```kotlin
fun executeAction(
    action: String,
    params: Map<String, Any> = emptyMap()
): Boolean
```

**Parameters**:
- `action`: Action identifier
- `params`: Optional parameters for action

**Returns**: `true` if executed successfully

**Description**:
1. Finds handler that can process action
2. Determines action category
3. Executes with timeout (5000ms)
4. Records performance metrics
5. Logs slow actions (>100ms)

**Example**:
```kotlin
val success = actionCoordinator.executeAction(
    action = "volume_up",
    params = emptyMap()
)
```

---

##### `processCommand(commandText: String): Boolean`

**Purpose**: Process voice command text (natural language)

**Signature**:
```kotlin
fun processCommand(commandText: String): Boolean
```

**Parameters**:
- `commandText`: Natural language command

**Returns**: `true` if processed successfully

**Description**:
Called by UnifiedCommandProcessor from speech recognition. Tries:
1. Direct execution if handler found
2. Command interpretation (natural language → action)
3. Execution of interpreted action

**Example**:
```kotlin
// Natural language input
coordinator.processCommand("turn up the volume")
  ↓
interpretVoiceCommand("turn up the volume")
  ↓
Returns "volume_up"
  ↓
executeAction("volume_up")
```

---

##### `interpretVoiceCommand(command: String): String?`

**Purpose**: Convert natural language to action identifier

**Signature**:
```kotlin
private fun interpretVoiceCommand(command: String): String?
```

**Parameters**:
- `command`: Natural language command

**Returns**: Action identifier or null

**Supported Patterns**:
```kotlin
"go back", "back" → "navigate_back"
"go home", "home" → "navigate_home"
"scroll up" → "scroll_up"
"volume up" → "volume_up"
"open Chrome" → "launch_app:chrome"
"type hello" → "input_text:hello"
"swipe left" → "swipe left"
"tap 5" → "click_number:5"
// ... and many more
```

---

##### `processVoiceCommand(text: String, confidence: Float): Boolean`

**Purpose**: Process voice command with confidence metadata

**Signature**:
```kotlin
fun processVoiceCommand(text: String, confidence: Float): Boolean
```

**Parameters**:
- `text`: Command text
- `confidence`: Recognition confidence (0.0 to 1.0)

**Returns**: `true` if successful

**Description**:
Enhanced version that:
- Normalizes command
- Creates voice-specific parameters (source, confidence, timestamp)
- Tries direct execution or voice-specific preprocessing
- Generates command variations
- Records metrics with "voice:" prefix

---

##### `getAllSupportedActions(): List<String>`

**Purpose**: Get all actions across all handlers

**Signature**:
```kotlin
fun getAllSupportedActions(): List<String>
```

**Returns**: List of "category: action" strings

**Example Output**:
```
["system: back", "system: home", "app: launch",
 "device: volume up", "gesture: swipe left", ...]
```

---

##### `getSupportedActions(category: ActionCategory): List<String>`

**Purpose**: Get actions for specific category

**Signature**:
```kotlin
fun getSupportedActions(category: ActionCategory): List<String>
```

**Parameters**:
- `category`: Target category (SYSTEM, APP, DEVICE, etc.)

**Returns**: List of actions for that category

---

##### `getMetrics(): Map<String, MetricData>`

**Purpose**: Get performance metrics for all actions

**Signature**:
```kotlin
fun getMetrics(): Map<String, MetricData>
```

**Returns**: Map of action to MetricData

**MetricData Fields**:
- `count`: Number of executions
- `totalTimeMs`: Total execution time
- `successCount`: Number of successful executions
- `lastExecutionMs`: Time of last execution
- `averageTimeMs`: Calculated average
- `successRate`: Calculated success rate (0.0 to 1.0)

**Example**:
```kotlin
val metrics = coordinator.getMetrics()
metrics.forEach { (action, data) ->
    Log.d(TAG, "$action: ${data.count} calls, " +
               "${data.averageTimeMs}ms avg, " +
               "${(data.successRate * 100).toInt()}% success")
}
```

---

##### `dispose()`

**Purpose**: Clean up coordinator and all handlers

**Signature**:
```kotlin
fun dispose()
```

**Description**:
- Disposes all handlers
- Clears handler registry
- Cancels coroutine scope
- Clears metrics

---

---

## Handler Architecture

All handlers implement the `ActionHandler` interface:

```kotlin
interface ActionHandler {
    fun initialize() { }
    fun canHandle(action: String): Boolean
    fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean
    fun getSupportedActions(): List<String>
    fun dispose() { }
}
```

### Handler Categories

```kotlin
enum class ActionCategory {
    SYSTEM,      // System actions (back, home, etc.)
    APP,         // App launching and switching
    DEVICE,      // Device controls (volume, brightness)
    INPUT,       // Text input and editing
    NAVIGATION,  // Scrolling, swiping
    UI,          // UI element interaction
    GESTURE,     // Complex gestures
    GAZE,        // Gaze-based interaction
    CUSTOM       // Custom extensions
}
```

---

### 1. SystemHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/SystemHandler.kt`

Handles system-level accessibility actions.

#### Supported Actions

```kotlin
val SUPPORTED_ACTIONS = listOf(
    "back", "go back",
    "home", "go home",
    "recent", "recent apps", "recents",
    "notifications", "notification panel",
    "settings", "quick settings",
    "power", "power menu",
    "screenshot", "take screenshot",
    "split screen", "split",
    "assistant", "voice assistant",
    "lock", "lock screen",
    "all apps", "app drawer"
)
```

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Description**: Execute system-level action

**Actions**:
- **"back", "go back"**: Performs GLOBAL_ACTION_BACK
- **"home", "go home"**: Performs GLOBAL_ACTION_HOME
- **"recent", "recent apps", "recents"**: Performs GLOBAL_ACTION_RECENTS
- **"notifications", "notification panel"**: Performs GLOBAL_ACTION_NOTIFICATIONS
- **"settings", "quick settings"**: Performs GLOBAL_ACTION_QUICK_SETTINGS
- **"power", "power menu"**: Performs GLOBAL_ACTION_POWER_DIALOG
- **"screenshot", "take screenshot"**: Performs GLOBAL_ACTION_TAKE_SCREENSHOT (Android P+)
- **"split screen", "split"**: Performs GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN (Android N+)
- **"assistant", "voice assistant"**: Performs GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT (Android S+)
- **"lock", "lock screen"**: Performs GLOBAL_ACTION_LOCK_SCREEN (Android P+)
- **"all apps", "app drawer"**: Performs GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS (Android S+)
- **"open settings [type]"**: Opens specific settings screen (wifi, bluetooth, accessibility, etc.)

**Example**:
```kotlin
systemHandler.execute(
    category = ActionCategory.SYSTEM,
    action = "screenshot",
    params = emptyMap()
) // Returns true, takes screenshot
```

---

### 2. AppHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/AppHandler.kt`

Handles application launching and switching.

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Description**: Launch app by voice command

**Parameters**:
- `action`: App name or launch command (matched against installed apps)

**Example**:
```kotlin
appHandler.execute(
    category = ActionCategory.APP,
    action = "chrome",
    params = emptyMap()
) // Launches Chrome browser
```

---

##### `canHandle(action: String): Boolean`

**Description**: Check if action matches an installed app

**Returns**: `true` if app found in installed apps list

---

##### `getSupportedActions(): List<String>`

**Description**: Get list of all launchable app names

**Returns**: List of app voice commands

---

### 3. DeviceHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/DeviceHandler.kt`

Handles device control actions (volume, brightness, connectivity).

#### Supported Actions

```kotlin
val SUPPORTED_ACTIONS = listOf(
    "volume up", "volume down", "volume mute", "volume unmute",
    "brightness up", "brightness down", "brightness max", "brightness min",
    "wifi on", "wifi off", "bluetooth on", "bluetooth off",
    "airplane mode on", "airplane mode off", "flashlight on", "flashlight off",
    "do not disturb on", "do not disturb off", "silent mode", "vibrate mode"
)
```

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Actions**:

**Volume Controls**:
- **"volume up"**: Increase system volume
- **"volume down"**: Decrease system volume
- **"volume mute", "mute"**: Mute audio
- **"volume unmute", "unmute"**: Unmute audio

**Brightness Controls**:
- **"brightness up"**: Increase screen brightness by 25 units
- **"brightness down"**: Decrease screen brightness by 25 units
- **"brightness max", "maximum brightness"**: Set brightness to 255
- **"brightness min", "minimum brightness"**: Set brightness to 10

**Connectivity** (opens settings):
- **"wifi on", "turn on wifi"**: Open WiFi settings
- **"wifi off", "turn off wifi"**: Open WiFi settings
- **"bluetooth on", "turn on bluetooth"**: Open Bluetooth settings
- **"bluetooth off", "turn off bluetooth"**: Open Bluetooth settings
- **"airplane mode on", "flight mode on"**: Open airplane mode settings
- **"airplane mode off", "flight mode off"**: Open airplane mode settings

**Sound Modes**:
- **"silent mode", "silent"**: Set ringer to silent
- **"vibrate mode", "vibrate"**: Set ringer to vibrate
- **"normal mode", "sound on"**: Set ringer to normal

**Do Not Disturb**:
- **"do not disturb on", "dnd on"**: Open DND settings
- **"do not disturb off", "dnd off"**: Open DND settings

**Example**:
```kotlin
deviceHandler.execute(
    category = ActionCategory.DEVICE,
    action = "volume up",
    params = emptyMap()
) // Increases volume
```

---

### 4. GestureHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/GestureHandler.kt`

Handles complex gesture interactions (pinch, zoom, drag, swipe).

#### Supported Actions

```kotlin
val SUPPORTED_ACTIONS = listOf(
    "pinch open", "zoom in", "pinch in",
    "pinch close", "zoom out", "pinch out",
    "drag", "drag to", "drag from",
    "gesture", "path gesture",
    "swipe", "swipe up", "swipe down", "swipe left", "swipe right"
)
```

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Actions**:

**Pinch/Zoom**:
- **"pinch open", "zoom in", "pinch in"**
  - Params: `x` (Int, optional), `y` (Int, optional) - center point
  - Default: Screen center
  - Description: Two-finger pinch outward gesture (zoom in)

- **"pinch close", "zoom out", "pinch out"**
  - Params: `x` (Int, optional), `y` (Int, optional) - center point
  - Default: Screen center
  - Description: Two-finger pinch inward gesture (zoom out)

**Drag**:
- **"drag", "drag to", "drag from"**
  - Params (required):
    - `startX` (Int): Starting X coordinate
    - `startY` (Int): Starting Y coordinate
    - `endX` (Int): Ending X coordinate
    - `endY` (Int): Ending Y coordinate
    - `duration` (Long, optional): Gesture duration in ms (default: 500)

**Swipe**:
- **"swipe", "swipe [direction]"**
  - Direction: up, down, left, right (optional)
  - Params:
    - `x` (Int, optional): Center X (default: screen center)
    - `y` (Int, optional): Center Y (default: screen center)
    - `distance` (Int, optional): Swipe distance in pixels (default: 400)
  - Default direction: right

**Path Gesture**:
- **"gesture", "path gesture"**
  - Params (required):
    - `path` (List<Point>): List of coordinates to follow
    - `duration` (Long, optional): Total duration (default: 500)

**Example**:
```kotlin
// Pinch zoom at coordinates
gestureHandler.execute(
    category = ActionCategory.GESTURE,
    action = "pinch open",
    params = mapOf("x" to 500, "y" to 800)
)

// Drag gesture
gestureHandler.execute(
    category = ActionCategory.GESTURE,
    action = "drag",
    params = mapOf(
        "startX" to 100,
        "startY" to 200,
        "endX" to 500,
        "endY" to 600,
        "duration" to 800L
    )
)

// Swipe
gestureHandler.execute(
    category = ActionCategory.GESTURE,
    action = "swipe left",
    params = mapOf("distance" to 600)
)
```

---

##### `performClickAt(x: Float, y: Float): Boolean`

**Purpose**: Perform single tap at coordinates

**Parameters**:
- `x`: X coordinate
- `y`: Y coordinate

**Returns**: `true` if successful

**Example**:
```kotlin
gestureHandler.performClickAt(500f, 800f)
```

---

##### `performLongPressAt(x: Float, y: Float): Boolean`

**Purpose**: Perform long press at coordinates

**Parameters**:
- `x`: X coordinate
- `y`: Y coordinate

**Returns**: `true` if successful

---

##### `performDoubleClickAt(x: Float, y: Float): Boolean`

**Purpose**: Perform double tap at coordinates

**Parameters**:
- `x`: X coordinate
- `y`: Y coordinate

**Returns**: `true` if successful

**Description**: Executes two taps with appropriate timing between them.

---

### 5. NavigationHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/NavigationHandler.kt`

Handles scrolling and navigation actions.

#### Supported Actions

```kotlin
val SUPPORTED_ACTIONS = listOf(
    "scroll up", "scroll down",
    "scroll left", "scroll right",
    "swipe up", "swipe down",
    "swipe left", "swipe right",
    "next", "previous",
    "page up", "page down"
)
```

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Actions**:
- **"scroll up", "page up"**: Scroll backward (up)
- **"scroll down", "page down"**: Scroll forward (down)
- **"scroll left"**: Horizontal scroll left
- **"scroll right"**: Horizontal scroll right
- **"swipe up"**: Swipe up (content moves up, equivalent to scroll down)
- **"swipe down"**: Swipe down (content moves down, equivalent to scroll up)
- **"swipe left"**: Swipe left (horizontal forward)
- **"swipe right"**: Swipe right (horizontal backward)
- **"next"**: Next granularity movement
- **"previous"**: Previous granularity movement

**Example**:
```kotlin
navigationHandler.execute(
    category = ActionCategory.NAVIGATION,
    action = "scroll down",
    params = emptyMap()
) // Scrolls page down
```

---

### 6. InputHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/InputHandler.kt`

Handles text input and editing actions.

#### Supported Actions

```kotlin
val SUPPORTED_ACTIONS = listOf(
    "type", "enter text", "input",
    "delete", "backspace", "clear text",
    "select all", "copy", "cut", "paste",
    "undo", "redo",
    "search", "find"
)
```

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Actions**:

**Text Input**:
- **"type [text]", "enter text [text]", "input [text]"**
  - Description: Enter text in focused field
  - Example: "type hello world"

**Deletion**:
- **"delete", "backspace"**: Delete last character
- **"clear text", "clear all"**: Clear all text

**Selection**:
- **"select all"**: Select all text in field

**Clipboard**:
- **"copy"**: Copy selected text
- **"cut"**: Cut selected text
- **"paste"**: Paste clipboard content

**Undo/Redo**:
- **"undo"**: Undo last action (not yet implemented)
- **"redo"**: Redo last undone action (not yet implemented)

**Search**:
- **"search [query]", "find [query]"**: Find and focus search field, enter query

**Example**:
```kotlin
inputHandler.execute(
    category = ActionCategory.INPUT,
    action = "type hello",
    params = emptyMap()
) // Types "hello" in focused field

inputHandler.execute(
    category = ActionCategory.INPUT,
    action = "select all",
    params = emptyMap()
) // Selects all text
```

---

### 7. SelectHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/SelectHandler.kt`

Handles selection mode and context menu operations.

#### Supported Actions

```kotlin
val SUPPORTED_ACTIONS = listOf(
    "select",
    "select mode",
    "selection mode",
    "context menu",
    "menu",
    "back",
    "cancel selection",
    "select all",
    "select text",
    "clear selection",
    "copy",
    "cut",
    "paste",
    "edit menu",
    "action menu"
)
```

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Actions**:

**Selection Mode**:
- **"select mode", "selection mode"**: Enter selection mode
- **"back", "cancel selection"**: Exit selection mode or perform back

**Selection**:
- **"select"**: Context-aware select (shows context menu if cursor visible, otherwise selects focused element)
- **"select all"**: Select all text in editable field
- **"select text"**: Select specific text (requires `text` param)
- **"clear selection"**: Clear current selection

**Context Menu**:
- **"menu", "context menu", "action menu"**: Show context menu
- **"edit menu"**: Show edit menu

**Clipboard** (in selection context):
- **"copy"**: Copy selected text
- **"cut"**: Cut selected text
- **"paste"**: Paste at selection

**Example**:
```kotlin
selectHandler.execute(
    category = ActionCategory.UI,
    action = "select mode",
    params = emptyMap()
) // Enter selection mode

selectHandler.execute(
    category = ActionCategory.UI,
    action = "select all",
    params = emptyMap()
) // Select all text

selectHandler.execute(
    category = ActionCategory.UI,
    action = "copy",
    params = emptyMap()
) // Copy selected text
```

---

##### `isInSelectionMode(): Boolean`

**Purpose**: Check if selection mode is active

**Returns**: `true` if in selection mode

---

##### `getCurrentSelection(): SelectionContext?`

**Purpose**: Get current selection context

**Returns**: SelectionContext or null

**SelectionContext**:
```kotlin
data class SelectionContext(
    val node: AccessibilityNodeInfo?,
    val bounds: Rect?,
    val selectionStart: Int = -1,
    val selectionEnd: Int = -1,
    val isTextSelection: Boolean = false
)
```

---

### 8. NumberHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/NumberHandler.kt`

Handles number overlay display and numbered element selection.

#### Supported Actions

```kotlin
val SUPPORTED_ACTIONS = listOf(
    "show numbers",
    "hide numbers",
    "numbers on",
    "numbers off",
    "toggle numbers",
    "number overlay",
    "label elements",
    "click number",
    "select number",
    "tap number"
)
```

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Actions**:

**Overlay Control**:
- **"show numbers", "numbers on", "label elements"**: Show numbered overlay on interactive elements
- **"hide numbers", "numbers off"**: Hide number overlay
- **"toggle numbers"**: Toggle overlay visibility

**Number Commands**:
- **"click [number]", "tap [number]", "select [number]"**: Click numbered element
- **"scroll [number]"**: Scroll numbered element
- **"long [number]"**: Long press numbered element

**Example**:
```kotlin
// Show overlay
numberHandler.execute(
    category = ActionCategory.UI,
    action = "show numbers",
    params = emptyMap()
)
// Now screen shows numbers 1-N on interactive elements

// Click element #5
numberHandler.execute(
    category = ActionCategory.UI,
    action = "tap 5",
    params = emptyMap()
)
// Clicks element labeled "5", then hides overlay
```

---

##### `isNumberOverlayActive(): Boolean`

**Purpose**: Check if overlay is visible

**Returns**: `true` if overlay active

---

##### `getNumberedElements(): Map<Int, ElementInfo>`

**Purpose**: Get map of numbered elements

**Returns**: Map of number to ElementInfo

**ElementInfo**:
```kotlin
data class ElementInfo(
    val node: AccessibilityNodeInfo,
    val bounds: Rect,
    val description: String,
    val isClickable: Boolean,
    val isScrollable: Boolean
)
```

---

### 9. UIHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/UIHandler.kt`

Handles UI element interaction by text/description.

#### Supported Actions

```kotlin
val SUPPORTED_ACTIONS = listOf(
    "click", "tap", "press",
    "long click", "long press",
    "double tap", "double click",
    "expand", "collapse",
    "check", "uncheck", "toggle",
    "focus", "dismiss", "close"
)
```

#### Public Methods

##### `execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean`

**Actions**:

**Click Actions**:
- **"click [element]", "tap [element]", "press [element]"**: Click element by text/description
- **"long click [element]", "long press [element]"**: Long press element
- **"double tap [element]", "double click [element]"**: Double tap element

**Expand/Collapse**:
- **"expand [element]"**: Expand expandable element
- **"collapse [element]"**: Collapse expanded element

**Check/Toggle**:
- **"check [element]"**: Check checkbox/toggle
- **"uncheck [element]"**: Uncheck checkbox/toggle
- **"toggle [element]"**: Toggle state

**Focus**:
- **"focus [element]"**: Focus element

**Dismiss**:
- **"dismiss", "close"**: Dismiss dialog/close screen

**Example**:
```kotlin
uiHandler.execute(
    category = ActionCategory.UI,
    action = "click submit button",
    params = emptyMap()
) // Finds and clicks element with text/description containing "submit button"

uiHandler.execute(
    category = ActionCategory.UI,
    action = "expand settings",
    params = emptyMap()
) // Expands element containing "settings"
```

---

### 10. BluetoothHandler

**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/BluetoothHandler.kt`

Handles Bluetooth-specific actions.

**Note**: Implementation details not fully documented. Typically handles:
- Bluetooth on/off
- Bluetooth settings
- Device pairing

---

## Extension Guide

### Adding a New Handler

#### Step 1: Create Handler Class

```kotlin
package com.augmentalis.voiceoscore.accessibility.handlers

import android.util.Log
import com.augmentalis.voiceoscore.accessibility.VoiceOSService

class MyCustomHandler(
    private val service: VoiceOSService
) : ActionHandler {

    companion object {
        private const val TAG = "MyCustomHandler"

        val SUPPORTED_ACTIONS = listOf(
            "my action 1",
            "my action 2"
        )
    }

    override fun initialize() {
        Log.d(TAG, "Initializing MyCustomHandler")
        // Initialize handler resources
    }

    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.contains(it) }
    }

    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()

        Log.d(TAG, "Executing action: $normalizedAction")

        return when {
            normalizedAction == "my action 1" -> {
                performMyAction1()
            }
            normalizedAction == "my action 2" -> {
                performMyAction2(params)
            }
            else -> {
                Log.w(TAG, "Unknown action: $normalizedAction")
                false
            }
        }
    }

    override fun dispose() {
        Log.d(TAG, "Disposing MyCustomHandler")
        // Clean up resources
    }

    private fun performMyAction1(): Boolean {
        return try {
            // Implementation
            Log.i(TAG, "Action 1 executed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in action 1", e)
            false
        }
    }

    private fun performMyAction2(params: Map<String, Any>): Boolean {
        val param1 = params["param1"] as? String
        return try {
            // Implementation using param1
            Log.i(TAG, "Action 2 executed with param: $param1")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in action 2", e)
            false
        }
    }
}
```

#### Step 2: Register Handler in ActionCoordinator

Edit `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/ActionCoordinator.kt`:

```kotlin
fun initialize() {
    Log.d(TAG, "Initializing ActionCoordinator")

    // Existing handlers...
    registerHandler(ActionCategory.SYSTEM, SystemHandler(service))
    // ... other handlers ...

    // Add your handler
    registerHandler(ActionCategory.CUSTOM, MyCustomHandler(service))

    // Initialize all handlers
    handlers.values.flatten().forEach { handler ->
        try {
            handler.initialize()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize handler", e)
        }
    }

    Log.i(TAG, "ActionCoordinator initialized with ${handlers.size} handlers")
}
```

#### Step 3: Add Voice Command Interpretation (Optional)

To support natural language commands, add patterns to `interpretVoiceCommand()` in ActionCoordinator:

```kotlin
private fun interpretVoiceCommand(command: String): String? {
    return when {
        // Existing patterns...

        // Your custom patterns
        command.contains("do my thing") -> "my action 1"
        command.startsWith("custom ") -> {
            val param = command.removePrefix("custom ").trim()
            "my action 2:$param"
        }

        else -> {
            Log.d(TAG, "No interpretation found for: '$command'")
            null
        }
    }
}
```

#### Step 4: Update getAllActions() (Optional)

For command listing, update `getAllActions()` in ActionCoordinator:

```kotlin
fun getAllActions(): List<String> {
    return BluetoothHandler.SUPPORTED_ACTIONS +
            DeviceHandler.SUPPORTED_ACTIONS +
            // ... other handlers ...
            MyCustomHandler.SUPPORTED_ACTIONS // Add yours
}
```

---

### Testing Your Handler

```kotlin
@Test
fun testMyCustomHandler() {
    val service = VoiceOSService.getInstance()
    assertNotNull(service)

    val coordinator = ActionCoordinator(service!!)
    coordinator.initialize()

    // Test action handling
    assertTrue(coordinator.canHandle("my action 1"))

    // Test execution
    val success = coordinator.executeAction(
        action = "my action 1",
        params = emptyMap()
    )
    assertTrue(success)

    // Test with parameters
    val success2 = coordinator.executeAction(
        action = "my action 2",
        params = mapOf("param1" to "test value")
    )
    assertTrue(success2)
}
```

---

## Integration Points

### 1. CommandManager Integration

VoiceOSService integrates with CommandManager for Tier 1 execution:

```kotlin
// In VoiceOSService
private fun initializeCommandManager() {
    commandManagerInstance = CommandManager.getInstance(this)
    commandManagerInstance?.initialize()

    serviceMonitor = ServiceMonitor(this, applicationContext)
    commandManagerInstance?.let { manager ->
        serviceMonitor?.bindCommandManager(manager)
        serviceMonitor?.startHealthCheck()
    }

    // Register database commands
    serviceScope.launch {
        delay(500)
        registerDatabaseCommands()
    }
}
```

**Usage from External Components**:
```kotlin
// Access CommandManager via VoiceOSService
val service = VoiceOSService.getInstance()
val commandManager = service?.let {
    CommandManager.getInstance(it)
}

// Or directly
val commandManager = CommandManager.getInstance(context)
```

---

### 2. LearnApp Integration

For third-party app learning:

```kotlin
// Initialized automatically
private fun initializeLearnAppIntegration() {
    UUIDCreator.initialize(applicationContext)
    learnAppIntegration = LearnAppIntegration.initialize(
        applicationContext,
        this
    )
}

// Events forwarded automatically
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    learnAppIntegration?.onAccessibilityEvent(event)
}
```

**Consent Flow**:
1. User launches new third-party app
2. LearnApp detects launch
3. Shows consent dialog
4. On consent, starts exploration
5. Generates UUIDs and commands
6. Updates database

---

### 3. VoiceCursor Integration

```kotlin
// Initialization
VoiceCursorAPI.initialize(context, service)

// Usage
VoiceCursorAPI.showCursor()
VoiceCursorAPI.moveCursor(dx = 10f, dy = 0f)
VoiceCursorAPI.click()
VoiceCursorAPI.hideCursor()
```

**Via VoiceOSService**:
```kotlin
val service = VoiceOSService.getInstance()
service?.showCursor()
service?.clickCursor()
service?.centerCursor()
```

---

### 4. Speech Recognition Integration

```kotlin
// SpeechEngineManager integration
speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)

// Update vocabulary
speechEngineManager.updateCommands(
    commandCache + staticCommandCache + appsCommand.keys
)

// Listen for results
speechEngineManager.speechState.collectLatest { state ->
    if (state.confidence > 0 && state.fullTranscript.isNotBlank()) {
        handleVoiceCommand(
            confidence = state.confidence,
            command = state.fullTranscript
        )
    }
}
```

---

### 5. UI Scraping Integration

```kotlin
// UIScrapingEngine extracts commands from UI
val commands = uiScrapingEngine.extractUIElementsAsync(event)

// Commands cached for speech recognition
nodeCache.clear()
nodeCache.addAll(commands)

val normalizedCommand = commands.map { element -> element.normalizedText }
commandCache.clear()
commandCache.addAll(normalizedCommand)

// Auto-registered with speech engine
speechEngineManager.updateCommands(
    commandCache + staticCommandCache + appsCommand.keys
)
```

---

## API Reference

### VoiceOSService Public API

```kotlin
// Static methods
VoiceOSService.getInstance(): VoiceOSService?
VoiceOSService.isServiceRunning(): Boolean
VoiceOSService.executeCommand(commandText: String): Boolean

// Instance methods
showCursor(): Boolean
hideCursor(): Boolean
toggleCursor(): Boolean
centerCursor(): Boolean
clickCursor(): Boolean
getCursorPosition(): CursorOffset
isCursorVisible(): Boolean
getAppCommands(): Map<String, String>
enableFallbackMode()
onNewCommandsGenerated()
```

### ActionCoordinator Public API

```kotlin
initialize()
canHandle(action: String): Boolean
executeAction(action: String, params: Map<String, Any> = emptyMap()): Boolean
executeActionAsync(action: String, params: Map<String, Any> = emptyMap(), callback: (Boolean) -> Unit = {})
processCommand(commandText: String): Boolean
processVoiceCommand(text: String, confidence: Float): Boolean
getAllSupportedActions(): List<String>
getSupportedActions(category: ActionCategory): List<String>
getMetrics(): Map<String, MetricData>
getMetricsForAction(action: String): MetricData?
clearMetrics()
dispose()
getDebugInfo(): String
getAllActions(): List<String>
```

### Handler Interface

```kotlin
interface ActionHandler {
    fun initialize()
    fun canHandle(action: String): Boolean
    fun execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean
    fun getSupportedActions(): List<String>
    fun dispose()
}
```

---

## Testing Guide

### Unit Testing Handlers

```kotlin
@RunWith(AndroidJUnit4::class)
class DeviceHandlerTest {

    private lateinit var service: VoiceOSService
    private lateinit var handler: DeviceHandler

    @Before
    fun setup() {
        service = VoiceOSService.getInstance()!!
        handler = DeviceHandler(service)
        handler.initialize()
    }

    @Test
    fun testVolumeUp() {
        val result = handler.execute(
            category = ActionCategory.DEVICE,
            action = "volume up",
            params = emptyMap()
        )
        assertTrue(result)
    }

    @Test
    fun testBrightnessControl() {
        val result = handler.execute(
            category = ActionCategory.DEVICE,
            action = "brightness up",
            params = emptyMap()
        )
        assertTrue(result)
    }

    @After
    fun tearDown() {
        handler.dispose()
    }
}
```

### Integration Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class VoiceCommandIntegrationTest {

    private lateinit var service: VoiceOSService
    private lateinit var coordinator: ActionCoordinator

    @Before
    fun setup() {
        service = VoiceOSService.getInstance()!!
        coordinator = ActionCoordinator(service)
        coordinator.initialize()
    }

    @Test
    fun testNaturalLanguageCommand() {
        val result = coordinator.processCommand("turn up the volume")
        assertTrue(result)
    }

    @Test
    fun testCommandInterpretation() {
        val result = coordinator.processCommand("go back")
        assertTrue(result)
    }

    @Test
    fun testAppLaunch() {
        val result = coordinator.processCommand("open chrome")
        assertTrue(result)
    }
}
```

### Testing Voice Recognition Flow

```kotlin
@Test
fun testVoiceCommandFlow() {
    val service = VoiceOSService.getInstance()!!

    // Simulate voice input
    val command = "volume up"
    val confidence = 0.95f

    // This would be called by speech recognition
    val method = VoiceOSService::class.java.getDeclaredMethod(
        "handleVoiceCommand",
        String::class.java,
        Float::class.javaPrimitiveType
    )
    method.isAccessible = true
    method.invoke(service, command, confidence)

    // Verify action was executed
    // (would need to mock AudioManager or verify side effects)
}
```

---

## Performance Optimization

### Caching

VoiceOSService uses multiple caching strategies:

```kotlin
// UI element cache (thread-safe)
private val nodeCache: MutableList<UIElement> = CopyOnWriteArrayList()

// Command cache (thread-safe)
private val commandCache: MutableList<String> = CopyOnWriteArrayList()

// Static commands (rarely change)
private val staticCommandCache: MutableList<String> = CopyOnWriteArrayList()

// App commands (concurrent access)
private val appsCommand = ConcurrentHashMap<String, String>()
```

### Debouncing

Event debouncing prevents excessive processing:

```kotlin
private val eventDebouncer = Debouncer(EVENT_DEBOUNCE_MS)

// In onAccessibilityEvent
val debounceKey = "$packageName-${event.className}-${event.eventType}"
if (!eventDebouncer.shouldProceed(debounceKey)) {
    Log.v(TAG, "Event debounced for: $debounceKey")
    return
}
```

### Lazy Initialization

Components initialized only when needed:

```kotlin
private val uiScrapingEngine by lazy {
    UIScrapingEngine(this).also {
        Log.d(TAG, "UIScrapingEngine initialized (lazy)")
    }
}

private val actionCoordinator by lazy {
    ActionCoordinator(this).also {
        Log.d(TAG, "ActionCoordinator initialized (lazy)")
    }
}
```

### Metrics

Track performance with built-in metrics:

```kotlin
val metrics = actionCoordinator.getMetrics()
metrics.forEach { (action, data) ->
    println("$action:")
    println("  Calls: ${data.count}")
    println("  Avg time: ${data.averageTimeMs}ms")
    println("  Success rate: ${(data.successRate * 100).toInt()}%")
}
```

---

## Troubleshooting

### Common Issues

**Issue**: Commands not recognized
**Solution**: Check if commands registered with speech engine:
```kotlin
val allCommands = commandCache + staticCommandCache + appsCommand.keys
Log.d(TAG, "Registered commands: $allCommands")
```

**Issue**: Handler not executing
**Solution**: Verify handler registered and can handle action:
```kotlin
val coordinator = ActionCoordinator(service)
coordinator.initialize()
Log.d(TAG, "Can handle 'volume up': ${coordinator.canHandle("volume up")}")
```

**Issue**: Database commands not loading
**Solution**: Check database initialization:
```kotlin
val database = AppScrapingDatabase.getInstance(context)
val commands = database.generatedCommandDao().getAllCommands()
Log.d(TAG, "Database has ${commands.size} commands")
```

**Issue**: Accessibility events not received
**Solution**: Verify service connected and permissions granted:
```kotlin
val isRunning = VoiceOSService.isServiceRunning()
Log.d(TAG, "Service running: $isRunning")
```

---

## APK Size Optimization

### Overview

The VoiceOS APK includes multiple native libraries and ML frameworks that significantly impact the final package size. This section documents the size breakdown and optimization strategies.

### APK Size Breakdown (as of 2026-01-29)

#### Total Size: ~173 MB (arm64-v8a only)

| Component | Size | Description |
|-----------|------|-------------|
| **Native Libraries** | ~76 MB | Vivoka SDK, ONNX, TensorFlow, SQLCipher |
| **DEX Files** | ~97 MB | Compiled Kotlin/Java code |

#### Native Library Details (lib/arm64-v8a/)

| Library | Size | Purpose |
|---------|------|---------|
| `libonnxruntime.so` | 16 MB | ONNX Runtime for AI/ML inference |
| `libnds_asr5.so` | 7 MB | Vivoka ASR (speech recognition) |
| `libnds_asr5_stub_textproc.so` | 7 MB | Vivoka text processing |
| `libtextproc.so` | 5.5 MB | Text processing engine |
| `libvocon_pron.so` | 5 MB | Vivoka pronunciation |
| `libtensorflowlite_jni.so` | 3.8 MB | TensorFlow Lite inference |
| `libsqlcipher.so` | 3.8 MB | Encrypted SQLite database |
| `libdd_common.so` | 3.8 MB | Vivoka common library |
| `libvocon_asr2.so` | 1.9 MB | Vivoka ASR v2 |
| Other Vivoka libs | ~22 MB | Audio, lexicon, cloud services |

#### DEX File Details (~97 MB total)

| File | Size | Likely Content |
|------|------|----------------|
| `classes.dex` | 32.7 MB | Main app + core dependencies |
| `classes23.dex` | 20.4 MB | ONNX Runtime Java bindings |
| `classes24.dex` | 9.8 MB | TensorFlow Lite bindings |
| `classes25.dex` | 8.3 MB | Firebase/Analytics |
| `classes26.dex` | 8.2 MB | Compose + Material |
| Other DEX files | ~17 MB | App code, utilities |

### Large Dependencies

| Dependency | Est. Size | Module | Purpose |
|------------|-----------|--------|---------|
| ONNX Runtime | ~20 MB | AI:NLU | ML inference for NLU |
| TensorFlow Lite | ~15 MB | AI:NLU | Alternative ML inference |
| Firebase BOM | ~10 MB | App | Config, analytics |
| Compose + Icons | ~15 MB | App | UI framework |
| Media3/ExoPlayer | ~8 MB | SpeechRecognition | Audio playback |
| Hilt/Dagger | ~5 MB | App | Dependency injection |
| Vivoka SDK | ~54 MB | SpeechRecognition | Offline speech recognition |

### ABI Filtering

The app is configured to include only `arm64-v8a` architecture to reduce APK size:

```kotlin
// android/apps/VoiceOS/build.gradle.kts
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
}
```

**Impact of ABI filtering:**
- Before (all ABIs): ~343 MB
- After (arm64-v8a only): ~173 MB
- Savings: **170 MB (50%)**

### Size Optimization Strategies

#### 1. Replace material-icons-extended

The `material-icons-extended` library (~8 MB) contains thousands of icons. Replace with specific icon imports:

```kotlin
// Instead of:
implementation("androidx.compose.material:material-icons-extended")

// Use specific icons:
implementation("androidx.compose.material:material-icons-core")
// Then copy only needed icons to local project
```

#### 2. Make AI:NLU Optional

If on-device NLU isn't required, exclude the AI:NLU module to remove ONNX and TensorFlow:

```kotlin
// In app build.gradle.kts, use feature flags:
if (project.hasProperty("includeNLU")) {
    implementation(project(":Modules:AI:NLU"))
}
```

#### 3. Enable R8 Minification (Release Builds)

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

#### 4. Use App Bundles for Play Store

```bash
./gradlew :android:apps:VoiceOS:bundleRelease
```

App Bundles allow Google Play to generate optimized APKs per device, further reducing download size.

#### 5. Lazy Load Speech Engines

Only initialize Vivoka when needed:

```kotlin
// Check availability before initialization
if (VivokaEngineFactory.isAvailable()) {
    val engine = VivokaEngineFactory.create(config)
    // Use engine
}
```

### Size Targets

| Build Type | Target Size | Notes |
|------------|-------------|-------|
| Debug APK | ~173 MB | Current (arm64-v8a only) |
| Release APK | ~120 MB | With R8 minification |
| App Bundle | ~80 MB | Per-device optimization |
| Lite Build (no NLU) | ~60 MB | Without AI:NLU module |

### Monitoring APK Size

Use the APK Analyzer in Android Studio or command line:

```bash
# List largest files in APK
unzip -l VoiceOS-debug.apk | awk '{print $1, $4}' | sort -rn | head -30

# Analyze with bundletool
bundletool build-apks --bundle=app.aab --output=app.apks
bundletool get-size total --apks=app.apks
```

---

## Cross-References

**Related Documentation**:
- [User Manual](User-Manual-251023-2145.md) - End-user voice command reference
- [CommandManager](../CommandManager/reference/api/) - Command database and execution
- [VoiceCursor](../VoiceCursor/architecture/) - Cursor-based control
- [LearnApp](../LearnApp/architecture/) - Third-party app learning
- [SpeechRecognition](../SpeechRecognition/) - Speech engine integration

**Key Source Files**:
- `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`
- `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOnSentry.kt`
- `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/ActionCoordinator.kt`
- `/app/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/` (all handlers)

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-23 21:45:25 PDT
**Total Functions Documented**: 100+
**Handlers Documented**: 10
