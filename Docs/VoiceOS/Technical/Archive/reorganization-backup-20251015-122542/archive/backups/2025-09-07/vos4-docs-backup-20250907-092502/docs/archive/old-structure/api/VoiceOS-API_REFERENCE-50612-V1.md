<!--
filename: API_REFERENCE.md
created: 2025-01-23 20:40:00 PST
author: VOS4 Development Team
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Complete API documentation for all VOS4 modules
last-modified: 2025-01-23 20:40:00 PST
version: 2.0.0
-->

# VOS4 API Reference
> Complete API documentation for all VOS4 modules
> Version: 2.0.0 (Updated from VOS3 1.0.0)
> Last Updated: 2025-01-23 20:40:00 PST

**Note:** This document has been migrated from `/docs-old/` and updated for VOS4 architecture. Some APIs reflect the new direct implementation pattern without interfaces.

## Table of Contents

1. [Core APIs](#core-apis)
2. [Module APIs](#module-apis)
3. [Event APIs](#event-apis)
4. [Data APIs](#data-apis)
5. [Service APIs](#service-apis)
6. [UI APIs](#ui-apis)
7. [Hardware APIs](#hardware-apis)
8. [Utility APIs](#utility-apis)

## Core APIs

### Direct Module Access Pattern (VOS4)

VOS4 uses direct implementation without interfaces where possible:

```kotlin
// VOS4 Direct Access Pattern
class CommandsModule(private val context: Context) {
    fun processCommand(text: String): CommandResult {
        // Direct processing, no abstraction layers
    }
}

// Direct handler assignment with invoke
actionRegistry["nav_back"] = NavigationActions.BackAction()::invoke
```

### ModuleRegistry (Updated for VOS4)

Simplified module management for VOS4 direct implementation:

```kotlin
object ModuleRegistry {
    /**
     * Initialize the registry with application context
     */
    fun initialize(context: Context)
    
    /**
     * Register a module directly
     */
    fun registerModule(moduleId: String, module: Any)
    
    /**
     * Get a module by ID with direct casting
     */
    inline fun <reified T> getModule(moduleId: String): T?
    
    /**
     * Initialize a specific module
     */
    suspend fun initializeModule(moduleId: String)
    
    /**
     * Shutdown a specific module
     */
    suspend fun shutdownModule(moduleId: String)
}
```

## Module APIs

### Audio Module API (DeviceMGR)

Unified in DeviceMGR for VOS4:

```kotlin
class AudioController(private val context: Context) {
    /**
     * Start audio capture
     */
    suspend fun startCapture(): Flow<ByteArray>
    
    /**
     * Stop audio capture
     */
    fun stopCapture()
    
    /**
     * Get current audio level
     */
    fun getAudioLevel(): StateFlow<Float>
    
    /**
     * Set audio parameters
     */
    fun setParameters(params: AudioParameters)
    
    /**
     * Check if currently capturing
     */
    fun isCapturing(): Boolean
}

data class AudioParameters(
    val sampleRate: Int = 16000,
    val channels: Int = 1,
    val encoding: AudioFormat = AudioFormat.ENCODING_PCM_16BIT,
    val bufferSize: Int = 1024,
    val noiseSuppressionEnabled: Boolean = true,
    val echoCancellationEnabled: Boolean = false
)
```

### Speech Recognition API (VOS4)

Multi-engine recognition with 6 engines:

```kotlin
class SpeechRecognitionModule(private val context: Context) {
    /**
     * Start speech recognition
     */
    suspend fun startRecognition(
        engine: RecognitionEngine = RecognitionEngine.AUTO,
        mode: RecognitionMode = RecognitionMode.COMMAND,
        language: String? = null
    ): Boolean
    
    /**
     * Stop ongoing recognition
     */
    suspend fun stopRecognition()
    
    /**
     * Check if recognition is active
     */
    fun isRecognizing(): Boolean
    
    /**
     * Get recognition results
     */
    fun getResults(): Flow<RecognitionResult>
    
    /**
     * Get available recognition engines
     */
    fun getAvailableEngines(): List<RecognitionEngine>
    
    /**
     * Get supported languages for an engine
     */
    fun getSupportedLanguages(engine: RecognitionEngine): List<String>
}

enum class RecognitionEngine {
    AUTO, VOSK, VIVOKA, GOOGLE_CLOUD, ANDROID_STT, WHISPER, AZURE
}

enum class RecognitionMode {
    COMMAND, DICTATION, WAKE_WORD, CONTINUOUS, MIXED
}

data class RecognitionResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val alternatives: List<Alternative> = emptyList(),
    val language: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    data class Alternative(
        val text: String,
        val confidence: Float
    )
}
```

### Commands Module API (VOS4)

Direct handler pattern with 70+ commands:

```kotlin
class CommandsModule(private val context: Context) {
    /**
     * Execute a voice command
     */
    suspend fun executeCommand(text: String): CommandResult
    
    /**
     * Register a custom command
     */
    fun registerCommand(command: Command)
    
    /**
     * Unregister a command
     */
    fun unregisterCommand(commandId: String)
    
    /**
     * Get command history
     */
    fun getCommandHistory(limit: Int = 100): List<CommandHistory>
    
    /**
     * Undo the last command
     */
    fun undoLastCommand(): Boolean
    
    /**
     * Redo the last undone command
     */
    fun redoCommand(): Boolean
    
    /**
     * Get available commands
     */
    fun getAvailableCommands(): List<Command>
}

data class Command(
    val id: String,
    val patterns: List<String>, // VOS4: Updated from "phrases" to "patterns"
    val action: CommandAction,
    val category: CommandCategory,
    val description: String,
    val confidence: Float, // VOS4: Added confidence field
    val parameters: Map<String, Any> = emptyMap()
)

data class CommandResult(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null,
    val executionTime: Long = 0L,
    val error: Throwable? = null
)

enum class CommandCategory {
    NAVIGATION, TEXT, SYSTEM, APP, CUSTOM
}
```

### Accessibility Module API (VOS4)

Direct Android accessibility service integration:

```kotlin
class AccessibilityModule(private val context: Context) {
    /**
     * Find UI elements by text
     */
    suspend fun findElementByText(
        text: String, 
        exact: Boolean = false
    ): List<UIElement>
    
    /**
     * Find UI elements by description
     */
    suspend fun findElementByDescription(
        description: String
    ): List<UIElement>
    
    /**
     * Get all screen elements
     */
    suspend fun getScreenElements(): List<UIElement>
    
    /**
     * Perform action on element
     */
    suspend fun performAction(
        element: UIElement, 
        action: AccessibilityAction
    ): Boolean
    
    /**
     * Get focused element
     */
    fun getFocusedElement(): UIElement?
    
    /**
     * Observe UI changes
     */
    fun observeUIChanges(): Flow<UIChangeEvent>
    
    /**
     * Get current app package
     */
    fun getCurrentPackage(): String?
    
    /**
     * Get current activity
     */
    fun getCurrentActivity(): String?
}

data class UIElement(
    val id: String?,
    val text: String?,
    val contentDescription: String?,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isFocusable: Boolean,
    val isEnabled: Boolean,
    val children: List<UIElement> = emptyList()
)

enum class AccessibilityAction {
    CLICK, LONG_CLICK, DOUBLE_CLICK,
    SCROLL_UP, SCROLL_DOWN, SCROLL_LEFT, SCROLL_RIGHT,
    FOCUS, CLEAR_FOCUS, SELECT, CLEAR_SELECTION,
    COPY, PASTE, CUT,
    SET_TEXT, CLEAR_TEXT,
    EXPAND, COLLAPSE,
    DISMISS
}
```

### VoiceUI Module API (VOS4)

XR-ready UI framework:

```kotlin
class VoiceUIModule(private val context: Context) {
    /**
     * Show voice command overlay
     */
    fun showCommandOverlay(config: OverlayConfig? = null)
    
    /**
     * Hide voice command overlay
     */
    fun hideCommandOverlay()
    
    /**
     * Show notification
     */
    fun showNotification(notification: VoiceNotification)
    
    /**
     * Update HUD display
     */
    fun updateHUD(hudData: HUDData)
    
    /**
     * Set UI theme
     */
    fun setTheme(theme: Theme)
    
    /**
     * Get current theme
     */
    fun getCurrentTheme(): Theme
    
    /**
     * Register gesture handler
     */
    fun registerGesture(
        gesture: GestureDefinition,
        handler: (GestureEvent) -> Unit
    )
    
    /**
     * Show data visualization (XR ready)
     */
    fun showVisualization(
        data: VisualizationData,
        type: VisualizationType
    )
}

data class VoiceNotification(
    val id: String,
    val title: String,
    val message: String,
    val priority: NotificationPriority,
    val actions: List<NotificationAction> = emptyList(),
    val autoCancel: Boolean = true,
    val duration: Long = 3000
)

enum class Theme {
    LIGHT, DARK, SYSTEM, HIGH_CONTRAST, ARVISION, MATERIAL, VISIONOS
}
```

### Data Module API (VOS4 - ObjectBox Only)

Mandatory ObjectBox persistence:

```kotlin
class DataModule(private val context: Context) {
    /**
     * Get user preferences
     */
    fun getUserPreferences(): UserPreferenceRepository
    
    /**
     * Get command history
     */
    fun getCommandHistory(): CommandHistoryRepository
    
    /**
     * Get custom commands
     */
    fun getCustomCommands(): CustomCommandRepository
    
    /**
     * Export data
     */
    suspend fun exportData(includeAll: Boolean = true): String?
    
    /**
     * Import data
     */
    suspend fun importData(
        jsonData: String,
        replaceExisting: Boolean = false
    ): Boolean
    
    /**
     * Get database size
     */
    fun getDatabaseSizeMB(): Float
    
    /**
     * Clear all data
     */
    suspend fun clearAllData()
}

// ObjectBox Repository Pattern (Mandatory)
interface Repository<T> {
    suspend fun getAll(): List<T>
    suspend fun getById(id: Long): T?
    suspend fun insert(entity: T): Long
    suspend fun update(entity: T)
    suspend fun delete(entity: T)
    suspend fun deleteAll()
    fun observe(): Flow<List<T>>
}
```

## Event APIs

### EventBus (Simplified for VOS4)

Lightweight event distribution:

```kotlin
object EventBus {
    /**
     * Post an event to all subscribers
     */
    fun post(event: Any)
    
    /**
     * Subscribe to events of a specific type
     */
    inline fun <reified T : Any> subscribe(
        noinline handler: (T) -> Unit
    ): Subscription
    
    /**
     * Unsubscribe from events
     */
    fun unsubscribe(subscription: Subscription)
}
```

### System Events (VOS4)

```kotlin
// Module lifecycle events
data class ModuleInitializedEvent(val moduleId: String)
data class ModuleShutdownEvent(val moduleId: String)
data class ModuleErrorEvent(val moduleId: String, val error: Throwable)

// Voice events
data class VoiceCommandEvent(val command: String)
data class RecognitionResultEvent(val result: RecognitionResult)
data class RecognitionErrorEvent(val error: String)

// UI events
data class UIElementClickedEvent(val element: UIElement)
data class GestureDetectedEvent(val gesture: Gesture)
data class ScreenChangedEvent(val packageName: String, val className: String)

// Data events
data class DataUpdatedEvent(val entityType: String, val entityId: Long)
data class DataSyncCompletedEvent(val timestamp: Long)
```

## Hardware APIs

### DeviceMGR API (VOS4 Unified)

Consolidated hardware management:

```kotlin
class DeviceManager(private val context: Context) {
    // Audio Control
    fun getAudioController(): AudioController
    
    // Display Control
    fun getDisplayController(): DisplayController
    
    // IMU Control
    fun getIMUController(): IMUController
    
    // Sensor Control
    fun getSensorController(): SensorController
    
    // Device Info
    fun getDeviceInfo(): DeviceInfo
}

class DeviceInfo {
    fun getManufacturer(): String
    fun getModel(): String
    fun getAndroidVersion(): Int
    fun getAvailableMemoryMB(): Float
    fun getTotalMemoryMB(): Float
    fun getBatteryLevel(): Int
    fun isCharging(): Boolean
    fun getCapabilities(): DeviceCapabilities
}

data class DeviceCapabilities(
    val hasMicrophone: Boolean,
    val hasSpeaker: Boolean,
    val hasCamera: Boolean,
    val hasNFC: Boolean,
    val hasBluetooth: Boolean,
    val hasGPS: Boolean,
    val hasGyroscope: Boolean,
    val hasAccelerometer: Boolean,
    val hasFingerprintSensor: Boolean
)
```

### Smart Glasses API (Future - GlassesMGR)

Android XR ready glasses integration:

```kotlin
class SmartGlassesModule(private val context: Context) {
    /**
     * Detect connected smart glasses
     */
    suspend fun detectDevice(): SmartGlassesDevice?
    
    /**
     * Connect to smart glasses
     */
    suspend fun connect(device: SmartGlassesDevice): Boolean
    
    /**
     * Send command to glasses
     */
    suspend fun sendCommand(command: GlassesCommand)
    
    /**
     * Observe device events
     */
    fun observeDeviceEvents(): Flow<GlassesEvent>
}

enum class GlassesType {
    REALWEAR_HMT1, REALWEAR_NAVIGATOR,
    VUZIX_BLADE, VUZIX_M400,
    ROKID_GLASS, ROKID_AIR,
    TCL_NXTWEAR, XREAL_AIR, XREAL_LIGHT,
    GENERIC
}
```

## Utility APIs

### Logger API

```kotlin
object VoiceOSLogger {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun logEvent(event: String, params: Map<String, Any>)
}
```

### UUIDManager API (VOS4 Library)

Extracted as shared library:

```kotlin
object UUIDManager {
    fun generateUUID(): String
    fun getDeviceId(): String
    fun generateSessionId(): String
    fun generateCommandId(): String
    fun generateModuleId(): String
    fun generateUserId(): String
    fun generateTransactionId(): String
}
```

## Error Handling

### Exception Types (VOS4)

```kotlin
// Module exceptions
class ModuleInitializationException(message: String) : Exception(message)
class ModuleNotFoundException(moduleId: String) : Exception("Module not found: $moduleId")

// Recognition exceptions
class RecognitionException(message: String) : Exception(message)
class ModelNotFoundException(model: String) : Exception("Model not found: $model")

// Command exceptions
class CommandExecutionException(message: String) : Exception(message)
class CommandNotFoundException(command: String) : Exception("Command not found: $command")

// Data exceptions (ObjectBox specific)
class ObjectBoxException(message: String) : Exception(message)
class DatabaseCorruptionException(message: String) : Exception(message)
```

## VOS4 Architecture Changes

### Key Differences from VOS3:

1. **Direct Implementation**: No interfaces unless absolutely necessary
2. **ObjectBox Only**: Mandatory database solution
3. **Unified DeviceMGR**: 5 modules → 1 unified manager
4. **XR Ready**: Built for Android XR and spatial computing
5. **Zero Overhead**: Direct handler assignment with `::invoke`
6. **Namespace**: All modules use `com.ai.*` pattern

### Performance Targets:

- **Initialization**: <1 second
- **Module load time**: <50ms per module
- **Command recognition**: <100ms latency
- **Memory usage**: <30MB (Vosk) or <60MB (Vivoka)
- **Battery drain**: <2% per hour active use

## Best Practices (VOS4)

### 1. Direct Access Pattern
```kotlin
// Correct VOS4 approach
val muteCommand = config?.muteCommand ?: "mute ava"
val language = config?.language ?: "en-US"

// Direct handler assignment
actionRegistry["nav_back"] = NavigationActions.BackAction()::invoke
```

### 2. ObjectBox Usage
```kotlin
@Entity
data class CommandHistory(
    @Id var id: Long = 0,
    val command: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

### 3. Error Handling
```kotlin
try {
    val result = commandModule.processCommand(text)
} catch (e: CommandExecutionException) {
    Log.e(TAG, "Command execution failed", e)
}
```

## Version Information

**VOS4 API Version**: 2.0.0  
**Compatible with**: VOS4 Architecture  
**Minimum Android SDK**: 28 (Android 9)  
**Target Android SDK**: 33 (Android 13)  
**Android XR Support**: Native  

---

*Migrated from docs-old/API_REFERENCE.md and updated for VOS4*  
*Last Updated: 2025-01-23*  
*Author: VOS4 Development Team*