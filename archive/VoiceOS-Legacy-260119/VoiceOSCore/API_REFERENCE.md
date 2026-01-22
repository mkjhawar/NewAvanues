# VoiceOS Accessibility API Reference

## Complete API Documentation

### Version: 1.0.0
### Package: `com.augmentalis.voiceaccessibility`

---

## Table of Contents
1. [Core Service APIs](#core-service-apis)
2. [Configuration APIs](#configuration-apis)
3. [Handler APIs](#handler-apis)
4. [Manager APIs](#manager-apis)
5. [Data Classes](#data-classes)
6. [Enumerations](#enumerations)
7. [Extension Functions](#extension-functions)
8. [Constants](#constants)

---

## Core Service APIs

### VoiceAccessibility

Main accessibility service class providing voice command execution.

#### Public Methods

##### executeCommand
```kotlin
@JvmStatic
fun executeCommand(commandText: String): Boolean
```
**Description**: Executes a voice command using fast path or handler routing.

**Parameters**:
- `commandText: String` - The command to execute (e.g., "go back", "open chrome")

**Returns**: `Boolean` - true if command executed successfully

**Example**:
```kotlin
val success = VoiceAccessibility.executeCommand("go back")
```

**Performance**: Fast path commands execute in <10ms, complex commands in <50ms

---

##### executeCommand (with parameters)
```kotlin
@JvmStatic
fun executeCommand(
    commandText: String, 
    params: Map<String, Any>
): Boolean
```
**Description**: Executes a command with additional parameters.

**Parameters**:
- `commandText: String` - The command to execute
- `params: Map<String, Any>` - Additional parameters for the command

**Returns**: `Boolean` - true if successful

**Example**:
```kotlin
val params = mapOf(
    "target" to "submit button",
    "timeout" to 5000L
)
VoiceAccessibility.executeCommand("click", params)
```

---

##### isServiceEnabled
```kotlin
@JvmStatic
fun isServiceEnabled(): Boolean
```
**Description**: Checks if the accessibility service is enabled.

**Returns**: `Boolean` - true if service is enabled in system settings

**Example**:
```kotlin
if (!VoiceAccessibility.isServiceEnabled()) {
    // Prompt user to enable service
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}
```

---

##### isServiceConnected
```kotlin
@JvmStatic
fun isServiceConnected(): Boolean
```
**Description**: Checks if the service is connected and ready.

**Returns**: `Boolean` - true if service is connected

---

##### updateConfiguration
```kotlin
@JvmStatic
fun updateConfiguration(config: ServiceConfiguration): Boolean
```
**Description**: Updates service configuration.

**Parameters**:
- `config: ServiceConfiguration` - New configuration to apply

**Returns**: `Boolean` - true if configuration updated successfully

**Example**:
```kotlin
val config = ServiceConfiguration(
    performanceMode = PerformanceMode.HIGH,
    commandCacheDuration = 10000L
)
VoiceAccessibility.updateConfiguration(config)
```

---

##### getConfiguration
```kotlin
@JvmStatic
fun getConfiguration(): ServiceConfiguration
```
**Description**: Gets current service configuration.

**Returns**: `ServiceConfiguration` - Current configuration

---

##### getPerformanceMetrics
```kotlin
@JvmStatic
fun getPerformanceMetrics(): PerformanceMetrics
```
**Description**: Gets performance metrics for the service.

**Returns**: `PerformanceMetrics` - Current performance statistics

**Example**:
```kotlin
val metrics = VoiceAccessibility.getPerformanceMetrics()
println("Success rate: ${metrics.successRate}%")
println("Avg execution: ${metrics.averageExecutionTime}ms")
```

---

##### getDynamicCommands
```kotlin
@JvmStatic
fun getDynamicCommands(): List<DynamicCommand>
```
**Description**: Gets dynamically generated commands for current UI.

**Returns**: `List<DynamicCommand>` - Available commands

---

##### getAllSupportedActions
```kotlin
@JvmStatic
fun getAllSupportedActions(): List<String>
```
**Description**: Gets all supported action commands.

**Returns**: `List<String>` - List of all supported commands

---

##### registerCustomCommand
```kotlin
@JvmStatic
fun registerCustomCommand(
    trigger: String, 
    action: (Map<String, Any>) -> Boolean
): Boolean
```
**Description**: Registers a custom command handler.

**Parameters**:
- `trigger: String` - Command trigger phrase
- `action: (Map<String, Any>) -> Boolean` - Action to execute

**Returns**: `Boolean` - true if registered successfully

**Example**:
```kotlin
VoiceAccessibility.registerCustomCommand("my action") { params ->
    // Custom implementation
    println("Executing custom action with params: $params")
    true
}
```

---

## Configuration APIs

### ServiceConfiguration

Configuration data class for the accessibility service.

#### Constructor
```kotlin
data class ServiceConfiguration(
    val isEnabled: Boolean = true,
    val handlersEnabled: Boolean = true,
    val cursorEnabled: Boolean = true,
    val dynamicCommandsEnabled: Boolean = true,
    val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    val commandCacheDuration: Long = 5000L,
    val maxCachedCommands: Int = 100,
    val debugLogging: Boolean = false,
    val metricsEnabled: Boolean = true,
    val version: Int = 1
)
```

#### Companion Object Methods

##### createDefault
```kotlin
fun createDefault(): ServiceConfiguration
```
**Description**: Creates default configuration.

**Returns**: `ServiceConfiguration` with default values

---

##### fromMap
```kotlin
fun fromMap(map: Map<String, Any>): ServiceConfiguration
```
**Description**: Creates configuration from a map (SR6-HYBRID pattern).

**Parameters**:
- `map: Map<String, Any>` - Configuration values

**Returns**: `ServiceConfiguration`

**Example**:
```kotlin
val map = mapOf(
    "isEnabled" to true,
    "performanceMode" to "HIGH",
    "commandCacheDuration" to 10000L
)
val config = ServiceConfiguration.fromMap(map)
```

---

#### Instance Methods

##### toMap
```kotlin
fun toMap(): Map<String, Any>
```
**Description**: Converts configuration to map for serialization.

**Returns**: `Map<String, Any>` - Configuration as map

---

##### mergeWith
```kotlin
fun mergeWith(other: ServiceConfiguration): ServiceConfiguration
```
**Description**: Merges with another configuration, other values take precedence.

**Parameters**:
- `other: ServiceConfiguration` - Configuration to merge

**Returns**: `ServiceConfiguration` - Merged configuration

---

##### isEquivalentTo
```kotlin
fun isEquivalentTo(other: ServiceConfiguration): Boolean
```
**Description**: Checks if functionally equivalent to another configuration.

**Parameters**:
- `other: ServiceConfiguration` - Configuration to compare

**Returns**: `Boolean` - true if equivalent

---

##### validate
```kotlin
fun validate(): ValidationResult
```
**Description**: Validates configuration values.

**Returns**: `ValidationResult` - Validation results

---

## Handler APIs

### ActionHandler Interface

Base interface for all command handlers.

```kotlin
interface ActionHandler {
    fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any> = emptyMap()
    ): Boolean
    
    fun canHandle(action: String): Boolean
    fun getSupportedActions(): List<String>
    fun initialize() {}
    fun dispose() {}
}
```

### Handler Implementations

#### SystemHandler

Handles system-level accessibility actions.

##### Supported Commands
- `"back"`, `"go back"` - Navigate back
- `"home"`, `"go home"` - Go to home screen
- `"recent"`, `"recent apps"` - Show recent applications
- `"notifications"` - Show notification panel
- `"settings"`, `"quick settings"` - Show quick settings
- `"power"`, `"power menu"` - Show power dialog
- `"screenshot"` - Take screenshot (Android 9+)
- `"split screen"` - Toggle split screen (Android 7+)
- `"lock screen"` - Lock device (Android 9+)

---

#### AppHandler

Manages application launching and control.

##### Supported Commands
- `"open [app name]"` - Launch application
- `"launch [app name]"` - Launch application
- `"start [app name]"` - Start application
- `"close [app name]"` - Close application
- `"switch to [app name]"` - Switch to app

##### Common App Aliases
```kotlin
"chrome" -> "com.android.chrome"
"camera" -> "com.android.camera2"
"phone" -> "com.android.dialer"
"messages" -> "com.google.android.apps.messaging"
"settings" -> "com.android.settings"
"maps" -> "com.google.android.apps.maps"
"youtube" -> "com.google.android.youtube"
"gmail" -> "com.google.android.gm"
```

---

#### DeviceHandler

Controls device settings and hardware.

##### Supported Commands
- `"volume up"` - Increase volume
- `"volume down"` - Decrease volume
- `"mute"`, `"volume mute"` - Mute audio
- `"unmute"`, `"volume unmute"` - Unmute audio
- `"brightness up"` - Increase brightness
- `"brightness down"` - Decrease brightness
- `"brightness max"` - Maximum brightness
- `"brightness min"` - Minimum brightness
- `"wifi on/off"` - Toggle WiFi
- `"bluetooth on/off"` - Toggle Bluetooth
- `"airplane mode on/off"` - Toggle airplane mode
- `"silent mode"` - Enable silent mode
- `"vibrate mode"` - Enable vibrate mode

---

#### InputHandler

Manages text input and keyboard operations.

##### Supported Commands
- `"type [text]"` - Enter text
- `"enter text [text]"` - Enter text
- `"delete"`, `"backspace"` - Delete character
- `"clear text"` - Clear all text
- `"select all"` - Select all text
- `"copy"` - Copy selected text
- `"cut"` - Cut selected text
- `"paste"` - Paste from clipboard
- `"search [query]"` - Search for text

---

#### NavigationHandler

Controls UI navigation and scrolling.

##### Supported Commands
- `"scroll up"` - Scroll content up
- `"scroll down"` - Scroll content down
- `"scroll left"` - Scroll content left
- `"scroll right"` - Scroll content right
- `"swipe up/down/left/right"` - Swipe gestures
- `"page up"` - Page up
- `"page down"` - Page down
- `"next"` - Next item
- `"previous"` - Previous item

---

#### UIHandler

Handles UI element interactions.

##### Supported Commands
- `"click [element]"` - Click element
- `"tap [element]"` - Tap element
- `"long click [element]"` - Long press
- `"double tap [element]"` - Double tap
- `"expand [element]"` - Expand element
- `"collapse [element]"` - Collapse element
- `"check [element]"` - Check checkbox
- `"uncheck [element]"` - Uncheck checkbox
- `"toggle [element]"` - Toggle element
- `"focus [element]"` - Focus element
- `"dismiss"` - Dismiss dialog

---

## Manager APIs

### ActionCoordinator

Coordinates action execution across handlers.

#### Methods

##### executeAction
```kotlin
fun executeAction(
    action: String,
    params: Map<String, Any> = emptyMap()
): Boolean
```
**Description**: Routes and executes action through appropriate handler.

---

##### executeActionAsync
```kotlin
fun executeActionAsync(
    action: String,
    params: Map<String, Any> = emptyMap(),
    callback: (Boolean) -> Unit = {}
)
```
**Description**: Executes action asynchronously.

---

##### getMetrics
```kotlin
fun getMetrics(): Map<String, MetricData>
```
**Description**: Gets performance metrics for all actions.

---

##### getAllSupportedActions
```kotlin
fun getAllSupportedActions(): List<String>
```
**Description**: Gets all supported actions from all handlers.

---

### CursorManager

Manages cursor overlay for precise selection.

#### Methods

##### showCursor
```kotlin
fun showCursor(): Boolean
```
**Description**: Shows cursor overlay.

---

##### hideCursor
```kotlin
fun hideCursor(): Boolean
```
**Description**: Hides cursor overlay.

---

##### moveCursor
```kotlin
fun moveCursor(direction: Direction): Boolean
```
**Description**: Moves cursor in specified direction.

**Parameters**:
- `direction: Direction` - UP, DOWN, LEFT, RIGHT, etc.

---

##### clickAtCursor
```kotlin
fun clickAtCursor(): Boolean
```
**Description**: Performs click at cursor position.

---

##### setMovementMode
```kotlin
fun setMovementMode(mode: MovementMode)
```
**Description**: Sets cursor movement mode.

**Parameters**:
- `mode: MovementMode` - NORMAL, FAST, or PRECISION

---

### DynamicCommandGenerator

Generates contextual commands based on UI.

#### Methods

##### generateCommands
```kotlin
fun generateCommands(useCache: Boolean = true): List<DynamicCommand>
```
**Description**: Generates available commands for current UI.

**Parameters**:
- `useCache: Boolean` - Whether to use cached results

**Returns**: `List<DynamicCommand>` - Generated commands

---

##### getSuggestions
```kotlin
fun getSuggestions(partialCommand: String): List<DynamicCommand>
```
**Description**: Gets command suggestions for partial input.

---

##### clearCache
```kotlin
fun clearCache()
```
**Description**: Clears command cache.

---

### AppCommandManager

Manages application-specific commands.

#### Methods

##### getPackageName
```kotlin
fun getPackageName(appName: String): String?
```
**Description**: Resolves app name to package name.

---

##### registerCustomCommand
```kotlin
fun registerCustomCommand(trigger: String, action: String): Boolean
```
**Description**: Registers custom command mapping.

---

##### getAppCommands
```kotlin
fun getAppCommands(packageName: String): List<String>
```
**Description**: Gets commands for specific app.

---

##### findAppByPartialName
```kotlin
fun findAppByPartialName(partialName: String): List<Pair<String, String>>
```
**Description**: Finds apps matching partial name.

**Returns**: List of (app name, package name) pairs

---

## Data Classes

### PerformanceMetrics
```kotlin
data class PerformanceMetrics(
    val totalCommands: Long,
    val successfulCommands: Long,
    val failedCommands: Long,
    val averageExecutionTime: Long,
    val minExecutionTime: Long,
    val maxExecutionTime: Long,
    val fastPathHits: Long,
    val handlerRoutedCommands: Long,
    val cacheHitRate: Float,
    val successRate: Float,
    val memoryUsage: Long,
    val timestamp: Long
)
```

### DynamicCommand
```kotlin
data class DynamicCommand(
    val command: String,
    val description: String,
    val confidence: Float,
    val category: ActionCategory? = null,
    val nodeInfo: AccessibilityNodeInfo? = null
)
```

### MetricData
```kotlin
data class MetricData(
    var count: Long = 0,
    var totalTimeMs: Long = 0,
    var successCount: Long = 0,
    var lastExecutionMs: Long = 0
) {
    val averageTimeMs: Long
    val successRate: Float
}
```

### ValidationResult
```kotlin
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)
```

---

## Enumerations

### ActionCategory
```kotlin
enum class ActionCategory {
    SYSTEM,      // System-level actions
    APP,         // Application control
    DEVICE,      // Device settings
    INPUT,       // Text input
    NAVIGATION,  // UI navigation
    UI,          // UI interaction
    CUSTOM       // Custom actions
}
```

### PerformanceMode
```kotlin
enum class PerformanceMode {
    BALANCED,    // Default balanced mode
    POWER_SAVER, // Reduced power consumption
    HIGH         // Maximum performance
}
```

### MovementMode (CursorManager)
```kotlin
enum class MovementMode {
    NORMAL,      // Default movement (50px)
    FAST,        // Fast movement (150px)
    PRECISION    // Precision movement (10px)
}
```

### Direction (CursorManager)
```kotlin
enum class Direction {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT,
    DOWN_LEFT, DOWN_RIGHT
}
```

---

## Extension Functions

### AccessibilityNodeInfo Extensions

```kotlin
fun AccessibilityNodeInfo.findNodeByText(
    text: String,
    ignoreCase: Boolean = true
): AccessibilityNodeInfo?
```
**Description**: Finds child node with matching text.

---

```kotlin
fun AccessibilityNodeInfo.findNodeByDescription(
    description: String,
    ignoreCase: Boolean = true
): AccessibilityNodeInfo?
```
**Description**: Finds child node with matching content description.

---

```kotlin
fun AccessibilityNodeInfo.getAllClickableNodes(): List<AccessibilityNodeInfo>
```
**Description**: Gets all clickable descendant nodes.

---

```kotlin
fun AccessibilityNodeInfo.getAllEditableNodes(): List<AccessibilityNodeInfo>
```
**Description**: Gets all editable descendant nodes.

---

### VoiceAccessibility Extensions

```kotlin
fun VoiceAccessibility.findNodeAtCoordinates(
    node: AccessibilityNodeInfo,
    x: Int,
    y: Int
): AccessibilityNodeInfo?
```
**Description**: Finds node at specific screen coordinates.

---

## Constants

### Performance Constants
```kotlin
const val DEFAULT_CACHE_DURATION = 5000L        // 5 seconds
const val MAX_CACHED_COMMANDS = 100            
const val HANDLER_TIMEOUT_MS = 5000L           
const val FAST_PATH_THRESHOLD_MS = 20L         
const val MAX_RETRY_ATTEMPTS = 3               
```

### UI Constants
```kotlin
const val CURSOR_SIZE = 48                     // dp
const val DEFAULT_MOVE_STEP = 50               // pixels
const val FAST_MOVE_STEP = 150                 // pixels
const val PRECISION_MOVE_STEP = 10             // pixels
```

### Command Constants
```kotlin
const val MAX_COMMAND_LENGTH = 500             
const val MAX_DYNAMIC_COMMANDS = 100           
const val SUGGESTION_LIMIT = 10                
```

---

## Error Codes

### Service Errors
- `1001` - Service not enabled
- `1002` - Service not connected
- `1003` - Service initialization failed

### Command Errors
- `2001` - Command not recognized
- `2002` - Command execution failed
- `2003` - Command timeout
- `2004` - Invalid parameters

### Handler Errors
- `3001` - Handler not found
- `3002` - Handler initialization failed
- `3003` - Handler execution error

### Configuration Errors
- `4001` - Invalid configuration
- `4002` - Configuration update failed
- `4003` - Version mismatch

---

## Usage Examples

### Basic Usage
```kotlin
// Check service and execute command
if (VoiceAccessibility.isServiceEnabled()) {
    val success = VoiceAccessibility.executeCommand("open settings")
    if (success) {
        Log.d("VOS", "Settings opened")
    }
}
```

### Advanced Usage
```kotlin
// Configure service for high performance
val config = ServiceConfiguration(
    performanceMode = PerformanceMode.HIGH,
    commandCacheDuration = 10000L,
    maxCachedCommands = 200
)
VoiceAccessibility.updateConfiguration(config)

// Execute with parameters
val params = mapOf(
    "target" to "submit",
    "timeout" to 3000L
)
VoiceAccessibility.executeCommand("click", params)

// Get performance metrics
val metrics = VoiceAccessibility.getPerformanceMetrics()
Log.d("Metrics", "Success rate: ${metrics.successRate}%")
```

### Custom Commands
```kotlin
// Register custom command
VoiceAccessibility.registerCustomCommand("take photo") { _ ->
    // Launch camera and take photo
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    context.startActivity(intent)
    true
}

// Use custom command
VoiceAccessibility.executeCommand("take photo")
```

---

## Thread Safety

All public APIs are thread-safe and can be called from any thread. Internal synchronization ensures safe concurrent access.

## Performance Guarantees

- Fast path commands: < 10ms
- Handler-routed commands: < 50ms
- Dynamic command generation: < 100ms
- Configuration update: < 20ms
- Metrics calculation: < 5ms

---

*This API reference is comprehensive and suitable for developers at all levels. For implementation details, see the source code.*