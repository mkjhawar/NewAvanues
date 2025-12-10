/**
 * VOS4 Developer Reference - Complete System Guide
 * Path: /ProjectDocs/VOS4-Developer-Reference.md
 * 
 * Created: 2024-08-22
 * Last Modified: 2024-08-22
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Comprehensive reference for all VOS4 components and methods
 * Module: System
 * 
 * Changelog:
 * - v1.0.0 (2024-08-22): Initial creation
 */

# VOS4 Developer Reference
## The Complete Guide to VOS4 System Components

---

## Table of Contents

1. [Application Structure](#application-structure)
2. [Device Management](#device-management)
3. [Audio Services](#audio-services)
4. [Speech Recognition](#speech-recognition)
5. [Commands System](#commands-system)
6. [Data Management](#data-management)
7. [Voice UI System](#voice-ui-system)
8. [Accessibility Services](#accessibility-services)
9. [UUID Targeting System](#uuid-targeting-system)
10. [Event System](#event-system)
11. [Common Patterns](#common-patterns)

---

## Application Structure

### Main Application Class

```kotlin
class VoiceOSApplication : Application() {
    // Direct access to all system components
    lateinit var deviceManager: DeviceManager
    lateinit var audioServices: AudioServices
    lateinit var dataManager: DataManager
    lateinit var speechRecognition: SpeechRecognitionManager
    lateinit var commandsManager: CommandsManager
    val eventBus: EventBus = EventBus.getDefault()
}
```

### Accessing Components

```kotlin
// In any Activity or Fragment
private val app get() = application as VoiceOSApplication

// Direct usage
app.speechRecognition.startListening()
app.deviceManager.audio.setVolume(50)
app.dataManager.commands.save(command)
```

---

## Device Management

### DeviceManager
**Location**: `/libraries/DeviceMGR/src/main/java/com/ai/DeviceManager.kt`

```kotlin
class DeviceManager(context: Context) {
    val info: DeviceInfo           // Device information
    val display: DisplayManager    // Display control
    val glasses: GlassesManager    // Smart glasses support
    val xr: XRManager             // Android XR support
    val audio: AudioDeviceManager // Audio device control
    val audioSession: AudioSessionManager
    val audioCapture: AudioCapture
    
    // Methods
    fun initialize()
    fun shutdown()
    fun isReady(): Boolean
}
```

#### DeviceInfo
```kotlin
class DeviceInfo(context: Context) {
    fun getDeviceModel(): String
    fun getAndroidVersion(): Int
    fun getRAM(): Long
    fun getCPUCores(): Int
    fun getScreenResolution(): Pair<Int, Int>
    fun hasNFC(): Boolean
    fun hasBluetooth(): Boolean
    fun getBatteryLevel(): Int
    fun isCharging(): Boolean
}
```

#### DisplayManager
```kotlin
class DisplayManager(context: Context) {
    fun getBrightness(): Int
    fun setBrightness(level: Int)
    fun getRotation(): Int
    fun keepScreenOn(enabled: Boolean)
    fun isScreenOn(): Boolean
    fun getRefreshRate(): Float
}
```

#### GlassesManager
```kotlin
class GlassesManager(context: Context) {
    fun isConnected(): Boolean
    fun getGlassesModel(): String?
    fun sendNotification(text: String)
    fun displayOverlay(content: View)
    fun clearOverlay()
    fun vibrateGlasses(duration: Long)
}
```

#### XRManager
```kotlin
class XRManager(context: Context) {
    fun isXRSupported(): Boolean
    fun enterXRMode()
    fun exitXRMode()
    fun trackHeadPosition(): Flow<Position>
    fun renderSpatialUI(element: View, position: Position)
}
```

---

## Audio Services

### AudioServices
**Location**: `/libraries/DeviceMGR/src/main/java/com/ai/AudioServices/`

```kotlin
class AudioServices(
    val capture: AudioCapture,
    val deviceManager: AudioDeviceManager,
    val sessionManager: AudioSessionManager
) {
    fun startCapture(): Flow<ByteArray>
    fun stopCapture()
    fun configureForSpeech()
    fun configureForMusic()
}
```

#### AudioCapture
```kotlin
class AudioCapture(context: Context, config: AudioConfig) {
    fun startRecording(): Flow<ByteArray>
    fun stopRecording()
    fun pauseRecording()
    fun resumeRecording()
    fun isRecording(): Boolean
    fun getAudioLevel(): Float
}
```

#### AudioConfig
```kotlin
data class AudioConfig(
    val sampleRate: Int = 16000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val noiseSuppression: Boolean = true,
    val echoCancellation: Boolean = false,
    val automaticGainControl: Boolean = true
) {
    companion object {
        fun forSpeechRecognition(): AudioConfig
        fun forDictation(): AudioConfig
        fun forWakeWord(): AudioConfig
        fun forMusic(): AudioConfig
    }
}
```

#### AudioDeviceManager
```kotlin
class AudioDeviceManager(context: Context) {
    fun setVolume(level: Int)
    fun getVolume(): Int
    fun mute()
    fun unmute()
    fun isMuted(): Boolean
    fun setSpeakerphone(enabled: Boolean)
    fun isHeadsetConnected(): Boolean
    fun getActiveAudioDevice(): AudioDevice
    fun routeAudioTo(device: AudioDevice)
}
```

---

## Speech Recognition

### SpeechRecognitionManager
**Location**: `/apps/SpeechRecognition/src/main/java/com/ai/speechrecognition/`

```kotlin
class SpeechRecognitionManager(
    audioServices: AudioServices,
    dataManager: DataManager
) {
    var currentEngine: RecognitionEngine = RecognitionEngine.VOSK
    var isListening: Boolean = false
    val results: Flow<RecognitionResult>
    
    // Core Methods
    fun startListening(engine: RecognitionEngine = currentEngine)
    fun stopListening()
    fun pauseListening()
    fun resumeListening()
    
    // Configuration
    fun setLanguage(language: String)
    fun setEngine(engine: RecognitionEngine)
    fun setVocabulary(words: List<String>)
    fun setMode(mode: RecognitionMode)
    
    // Engine Management
    fun getAvailableEngines(): List<RecognitionEngine>
    fun isEngineAvailable(engine: RecognitionEngine): Boolean
    fun downloadEngineModels(engine: RecognitionEngine)
}
```

#### Recognition Engines
```kotlin
enum class RecognitionEngine {
    VOSK,           // Offline, 42 languages
    VIVOKA,         // Offline, dynamic compilation
    ANDROID_STT,    // Google's Android STT
    GOOGLE_CLOUD,   // Cloud-based
    AZURE           // Microsoft Azure
}
```

#### Recognition Modes
```kotlin
enum class RecognitionMode {
    COMMAND,        // Short commands
    DICTATION,      // Long-form text
    CONVERSATION,   // Interactive dialog
    WAKE_WORD      // Wake word detection
}
```

#### RecognitionResult
```kotlin
data class RecognitionResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val alternatives: List<Alternative> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val engine: RecognitionEngine
)
```

---

## Commands System

### CommandsManager
**Location**: `/managers/CommandsMGR/src/main/java/com/ai/`

```kotlin
class CommandsManager(dataManager: DataManager) {
    val registry: CommandRegistry
    val processor: CommandProcessor
    val validator: CommandValidator
    val history: CommandHistory
    
    // Command Execution
    fun execute(command: String): ActionResult
    fun executeCommand(command: Command): ActionResult
    fun executeAsync(command: String): Flow<ActionResult>
    
    // Command Registration
    fun registerCommand(definition: CommandDefinition)
    fun unregisterCommand(phrase: String)
    fun getRegisteredCommands(): List<CommandDefinition>
    
    // Command Validation
    fun isValidCommand(phrase: String): Boolean
    fun getSuggestions(partial: String): List<String>
}
```

#### Command Actions
```kotlin
// Available action categories
object AppActions {
    fun openApp(packageName: String): ActionResult
    fun closeApp(packageName: String): ActionResult
    fun switchApp(packageName: String): ActionResult
    fun listApps(): ActionResult
}

object NavigationActions {
    fun goBack(): ActionResult
    fun goHome(): ActionResult
    fun goToRecents(): ActionResult
    fun openNotifications(): ActionResult
    fun openSettings(): ActionResult
}

object SystemActions {
    fun takeScreenshot(): ActionResult
    fun toggleFlashlight(): ActionResult
    fun setBrightness(level: Int): ActionResult
    fun setVolume(level: Int): ActionResult
}

object TextActions {
    fun typeText(text: String): ActionResult
    fun copyText(): ActionResult
    fun pasteText(): ActionResult
    fun selectAll(): ActionResult
    fun deleteText(): ActionResult
}

object CursorActions {
    fun moveCursor(direction: Direction, amount: Int): ActionResult
    fun click(x: Int, y: Int): ActionResult
    fun longClick(x: Int, y: Int): ActionResult
    fun doubleClick(x: Int, y: Int): ActionResult
}

object ScrollActions {
    fun scrollUp(amount: Int): ActionResult
    fun scrollDown(amount: Int): ActionResult
    fun scrollToTop(): ActionResult
    fun scrollToBottom(): ActionResult
}

object GestureActions {
    fun swipe(direction: Direction): ActionResult
    fun pinchZoom(scale: Float): ActionResult
    fun rotate(degrees: Float): ActionResult
}

object DictationActions {
    fun startDictation(): ActionResult
    fun stopDictation(): ActionResult
    fun insertPunctuation(type: PunctuationType): ActionResult
}

object OverlayActions {
    fun showOverlay(type: OverlayType): ActionResult
    fun hideOverlay(): ActionResult
    fun toggleOverlay(): ActionResult
}

object VolumeActions {
    fun volumeUp(amount: Int = 1): ActionResult
    fun volumeDown(amount: Int = 1): ActionResult
    fun mute(): ActionResult
    fun unmute(): ActionResult
}

object DragActions {
    fun startDrag(x: Int, y: Int): ActionResult
    fun continueDrag(x: Int, y: Int): ActionResult
    fun endDrag(x: Int, y: Int): ActionResult
}
```

---

## Data Management

### DataManager
**Location**: `/managers/DataMGR/src/main/java/com/ai/`

```kotlin
class DataManager(context: Context) {
    val commands: CommandRepository
    val preferences: PreferenceRepository
    val history: HistoryRepository
    val analytics: AnalyticsRepository
    val gestures: GestureRepository
    val sequences: SequenceRepository
    
    // ObjectBox Store
    val boxStore: BoxStore
    
    // Data Operations
    fun backup(): File
    fun restore(backupFile: File)
    fun clearAll()
    fun export(format: ExportFormat): File
    fun import(file: File)
}
```

#### Repository Pattern
```kotlin
class CommandRepository(box: Box<CommandEntity>) {
    fun save(command: CommandEntity): Long
    fun get(id: Long): CommandEntity?
    fun getAll(): List<CommandEntity>
    fun delete(id: Long)
    fun update(command: CommandEntity)
    fun findByPhrase(phrase: String): CommandEntity?
    fun getFrequentCommands(limit: Int): List<CommandEntity>
}
```

---

## Voice UI System

### VoiceUIManager
**Location**: `/apps/VoiceUI/src/main/java/com/ai/voiceui/`

```kotlin
class VoiceUIManager(context: Context) {
    val overlay: OverlayManager
    val hud: HUDSystem
    val gestures: GestureManager
    val feedback: FeedbackManager
    
    // Overlay Management
    fun showCommandOverlay()
    fun showDictationOverlay()
    fun showStatusOverlay()
    fun hideAllOverlays()
    
    // HUD System
    fun displayHUD(content: HUDContent)
    fun updateHUD(updates: HUDUpdate)
    fun animateHUD(animation: HUDAnimation)
    
    // Gesture Support
    fun enableGestureControl()
    fun registerGesture(gesture: Gesture, action: () -> Unit)
}
```

---

## Accessibility Services

### VoiceAccessibilityService
**Location**: `/apps/VoiceAccessibility/src/main/java/com/ai/voiceaccessibility/`

```kotlin
class VoiceAccessibilityService : AccessibilityService() {
    companion object {
        fun executeCommand(command: String): Boolean
        fun isServiceEnabled(): Boolean
        fun getUIElements(): List<UIElement>
    }
    
    // Direct command execution
    fun performClick(x: Int, y: Int)
    fun performScroll(direction: Direction)
    fun performBack()
    fun performHome()
    fun extractText(): String
    fun findElement(text: String): UIElement?
}
```

---

## UUID Targeting System

### UUIDManager
**Location**: `/libraries/UUIDManager/src/main/java/com/ai/uuidmgr/`

```kotlin
class UUIDManager {
    // Targeting Methods
    fun targetByText(text: String): UUIDElement?
    fun targetByPosition(x: Int, y: Int): UUIDElement?
    fun targetBySpatial(direction: Direction): UUIDElement?
    fun targetByType(type: ElementType): List<UUIDElement>
    fun targetByHierarchy(path: String): UUIDElement?
    fun targetByVoice(command: String): UUIDElement?
    fun targetByContext(): UUIDElement?
    
    // UUID Operations
    fun generateUUID(element: View): String
    fun registerElement(element: UUIDElement)
    fun unregisterElement(uuid: String)
    fun getAllElements(): List<UUIDElement>
}
```

---

## Event System

### EventBus Usage
```kotlin
// Publishing Events
EventBus.getDefault().post(CommandExecutedEvent(command))
EventBus.getDefault().postSticky(ConfigurationChangedEvent(config))

// Subscribing to Events
@Subscribe(threadMode = ThreadMode.MAIN)
fun onCommandExecuted(event: CommandExecutedEvent) {
    // Handle event
}

// Common Events
data class CommandExecutedEvent(val command: Command, val result: ActionResult)
data class RecognitionResultEvent(val result: RecognitionResult)
data class EngineStateChangedEvent(val state: EngineState)
data class AudioLevelEvent(val level: Float)
data class ErrorEvent(val error: Throwable, val source: String)
```

---

## Common Patterns

### Direct Access Pattern
```kotlin
// Always use direct access
app.deviceManager.audio.setVolume(50)  // Good
// Not through unnecessary methods
getAudioManager().setVolume(50)  // Bad
```

### Dependency Injection
```kotlin
// Pass dependencies via constructor
class MyService(
    private val audioServices: AudioServices,
    private val dataManager: DataManager
) {
    // Direct usage of injected dependencies
}
```

### Coroutine Usage
```kotlin
// Structured concurrency
class MyManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    fun doAsync() = scope.launch {
        withContext(Dispatchers.IO) {
            // IO operations
        }
    }
}
```

### Error Handling
```kotlin
// Comprehensive error handling
fun riskyOperation(): Result<Data> {
    return try {
        Result.success(performOperation())
    } catch (e: Exception) {
        Log.e(TAG, "Operation failed", e)
        Result.failure(e)
    }
}
```

---

## Quick Reference

### Common Tasks

```kotlin
// Start speech recognition
app.speechRecognition.startListening()

// Execute a command
app.commandsManager.execute("open settings")

// Capture audio
val audioFlow = app.audioServices.startCapture()

// Save data
app.dataManager.commands.save(commandEntity)

// Show overlay
app.voiceUI.overlay.show(OverlayType.COMMAND)

// Access device info
val model = app.deviceManager.info.getDeviceModel()

// Target UI element
val element = app.uuidManager.targetByText("Submit")

// Perform accessibility action
VoiceAccessibilityService.executeCommand("click submit")

// Publish event
EventBus.getDefault().post(MyEvent(data))
```

### Component Initialization Order

1. DeviceManager
2. AudioServices  
3. DataManager
4. SpeechRecognition (needs audio & data)
5. CommandsManager (needs data)
6. VoiceUI
7. Accessibility (if enabled)

---

## Performance Guidelines

- **Initialization**: < 1 second total
- **Command execution**: < 100ms
- **Speech recognition latency**: < 100ms
- **Memory usage**: < 30MB (Vosk) or < 60MB (Vivoka)
- **Battery drain**: < 2% per hour active use

---

## Testing Access

```kotlin
// In tests, create test application
class TestApplication : VoiceOSApplication() {
    override fun onCreate() {
        super.onCreate()
        // Use test/mock components
        audioServices = MockAudioServices()
        dataManager = MockDataManager()
    }
}
```

---

*This is a living document. Update when adding new components or methods.*