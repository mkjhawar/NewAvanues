# Appendix A: Complete API Reference
## VOS4 Developer Manual

**Version:** 4.0.0
**Last Updated:** 2025-11-12
**Status:** Living Document
**Framework:** IDEACODE v5.3

---

## Table of Contents

### Part I: Core Services
- [A.1 VoiceOSCore APIs](#a1-voiceoscore-apis)
  - [A.1.1 IVoiceOSService Interface](#a11-ivoiceosservice-interface)
  - [A.1.2 AIDL IPC Service API (Phase 3)](#a12-aidl-ipc-service-api-phase-3)
  - [A.1.3 UI Scraping Engine](#a13-ui-scraping-engine)
- [A.2 Accessibility Service APIs](#a2-accessibility-service-apis)
- [A.3 UI Scraping APIs](#a3-ui-scraping-apis)

### Part II: Speech Recognition
- [A.4 SpeechRecognition Library](#a4-speechrecognition-library)
- [A.5 Engine APIs](#a5-engine-apis)
- [A.6 Recognition Callbacks](#a6-recognition-callbacks)

### Part III: Manager APIs
- [A.7 CommandManager](#a7-commandmanager)
- [A.8 VoiceDataManager](#a8-voicedatamanager)
- [A.9 LocalizationManager](#a9-localizationmanager)
- [A.10 LicenseManager](#a10-licensemanager)

### Part IV: Library APIs
- [A.11 UUIDCreator](#a11-uuidcreator)
- [A.12 DeviceManager](#a12-devicemanager)
- [A.13 VoiceKeyboard](#a13-voicekeyboard)
- [A.14 VoiceUIElements](#a14-voiceuielements)

### Part V: Database APIs
- [A.15 Room Database APIs](#a15-room-database-apis)
- [A.16 DAO Interfaces](#a16-dao-interfaces)

---

## A.1 VoiceOSCore APIs

### A.1.1 IVoiceOSService Interface

**Package:** `com.augmentalis.voiceoscore.accessibility`

The primary interface for all VoiceOS accessibility service implementations.

#### Interface Definition

```kotlin
interface IVoiceOSService {
    // Lifecycle methods
    fun onCreate()
    fun onServiceConnected()
    fun onDestroy()

    // Event handling
    fun onAccessibilityEvent(event: AccessibilityEvent?)
    fun onInterrupt()

    // Cursor control
    fun showCursor(): Boolean
    fun hideCursor(): Boolean
    fun toggleCursor(): Boolean
    fun centerCursor(): Boolean
    fun clickCursor(): Boolean

    // State queries
    fun getCursorPosition(): CursorOffset
    fun isCursorVisible(): Boolean

    // Command handling
    fun onNewCommandsGenerated()
    fun enableFallbackMode()
    fun getAppCommands(): Map<String, String>

    companion object {
        @JvmStatic fun isServiceRunning(): Boolean
        @JvmStatic fun executeCommand(commandText: String): Boolean
        @JvmStatic fun getInstance(): AccessibilityService?
    }
}
```

#### Method Reference

##### onCreate()
**Purpose:** Initialize service components
**Called:** When service is first created
**Thread:** Main thread
**Return:** void

**Implementation Requirements:**
- Initialize Hilt dependency injection
- Setup coroutine scopes
- Initialize managers (CommandManager, VoiceDataManager)
- Configure logging

**Example:**
```kotlin
override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "VoiceOSService created")

    // Initialize DI
    AndroidEntryPoint.inject(this)

    // Setup coroutine scope
    serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Initialize managers
    commandManager.initialize()
    voiceDataManager.initialize()
}
```

##### onServiceConnected()
**Purpose:** Configure service after connection to accessibility framework
**Called:** After Android binds the accessibility service
**Thread:** Main thread
**Return:** void

**Implementation Requirements:**
- Configure AccessibilityServiceInfo
- Set event types to monitor
- Initialize voice recognition
- Start command registration

**AccessibilityServiceInfo Configuration:**
```kotlin
override fun onServiceConnected() {
    serviceInfo = AccessibilityServiceInfo().apply {
        eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                     AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                     AccessibilityEvent.TYPE_VIEW_CLICKED

        feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
        flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS

        notificationTimeout = 100L
    }

    // Initialize voice recognition
    voiceRecognitionManager.start()
}
```

##### onAccessibilityEvent(event: AccessibilityEvent?)
**Purpose:** Process accessibility events from the system
**Called:** Whenever monitored event occurs
**Thread:** Main thread
**Parameters:**
- `event: AccessibilityEvent?` - Event to process (nullable)

**Return:** void

**Event Types:**
- `TYPE_WINDOW_STATE_CHANGED` - Window/screen changed
- `TYPE_WINDOW_CONTENT_CHANGED` - Content within window changed
- `TYPE_VIEW_CLICKED` - User clicked a view
- `TYPE_VIEW_FOCUSED` - View received focus
- `TYPE_VIEW_TEXT_CHANGED` - Text in view changed

**Example:**
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return

    when (event.eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
            // New screen detected
            val packageName = event.packageName?.toString()
            val className = event.className?.toString()

            Log.d(TAG, "Window changed: $packageName/$className")

            // Trigger screen scraping
            scrapingEngine.scrapeCurrentScreen(packageName)
        }

        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
            // Content update
            updateScreenContext(event)
        }

        AccessibilityEvent.TYPE_VIEW_CLICKED -> {
            // Track user interaction
            trackUserClick(event)
        }
    }
}
```

##### showCursor(): Boolean
**Purpose:** Display the voice cursor overlay
**Thread:** Main thread
**Return:** `true` if successful, `false` otherwise

**Implementation:**
```kotlin
override fun showCursor(): Boolean {
    return try {
        cursorOverlay?.show()
        cursorVisible = true
        Log.d(TAG, "Cursor shown")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to show cursor", e)
        false
    }
}
```

##### hideCursor(): Boolean
**Purpose:** Hide the voice cursor overlay
**Thread:** Main thread
**Return:** `true` if successful, `false` otherwise

**Implementation:**
```kotlin
override fun hideCursor(): Boolean {
    return try {
        cursorOverlay?.hide()
        cursorVisible = false
        Log.d(TAG, "Cursor hidden")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to hide cursor", e)
        false
    }
}
```

##### toggleCursor(): Boolean
**Purpose:** Toggle cursor visibility
**Thread:** Main thread
**Return:** `true` if successful, `false` otherwise

**Example:**
```kotlin
override fun toggleCursor(): Boolean {
    return if (isCursorVisible()) {
        hideCursor()
    } else {
        showCursor()
    }
}
```

##### centerCursor(): Boolean
**Purpose:** Move cursor to screen center
**Thread:** Main thread
**Return:** `true` if successful, `false` otherwise

**Implementation:**
```kotlin
override fun centerCursor(): Boolean {
    return try {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2
        val centerY = displayMetrics.heightPixels / 2

        cursorPositionTracker.setPosition(centerX, centerY)
        Log.d(TAG, "Cursor centered at ($centerX, $centerY)")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to center cursor", e)
        false
    }
}
```

##### clickCursor(): Boolean
**Purpose:** Perform click at current cursor position
**Thread:** Main thread
**Return:** `true` if successful, `false` otherwise

**Implementation:**
```kotlin
override fun clickCursor(): Boolean {
    return try {
        val position = getCursorPosition()

        // Create gesture path
        val path = Path().apply {
            moveTo(position.x, position.y)
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        // Dispatch gesture
        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d(TAG, "Click gesture completed at (${position.x}, ${position.y})")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.w(TAG, "Click gesture cancelled")
            }
        }, null)

        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to click cursor", e)
        false
    }
}
```

##### getCursorPosition(): CursorOffset
**Purpose:** Get current cursor coordinates
**Thread:** Any thread (thread-safe)
**Return:** `CursorOffset` with X,Y coordinates

**Return Type:**
```kotlin
data class CursorOffset(
    val x: Float,
    val y: Float
)
```

**Example:**
```kotlin
override fun getCursorPosition(): CursorOffset {
    return cursorPositionTracker.getCurrentPosition()
}
```

##### isCursorVisible(): Boolean
**Purpose:** Check if cursor is currently visible
**Thread:** Any thread (thread-safe)
**Return:** `true` if visible, `false` otherwise

**Example:**
```kotlin
override fun isCursorVisible(): Boolean {
    return cursorVisible
}
```

##### onNewCommandsGenerated()
**Purpose:** Notify service that new commands have been generated
**Called:** After app scraping completes
**Thread:** Any thread
**Return:** void

**Implementation:**
```kotlin
override fun onNewCommandsGenerated() {
    serviceScope.launch {
        Log.d(TAG, "New commands generated - refreshing registry")

        // Reload commands from database
        val commands = commandDao.getAllCommands()

        // Re-register with speech engine
        voiceRecognitionManager.updateCommandRegistry(commands)

        Log.d(TAG, "Command registry updated with ${commands.size} commands")
    }
}
```

##### enableFallbackMode()
**Purpose:** Enable fallback mode when CommandManager unavailable
**Called:** By ServiceMonitor during graceful degradation
**Thread:** Any thread
**Return:** void

**Implementation:**
```kotlin
override fun enableFallbackMode() {
    Log.w(TAG, "Enabling fallback mode")

    // Switch to static command set
    voiceRecognitionManager.setCommandSet(StaticCommands.getBasicCommands())

    // Disable dynamic scraping
    scrapingEngine.disable()

    // Show notification to user
    showFallbackNotification()
}
```

##### getAppCommands(): Map<String, String>
**Purpose:** Get map of app launch commands
**Thread:** Any thread (suspending)
**Return:** Map of command text to package name

**Example:**
```kotlin
override fun getAppCommands(): Map<String, String> {
    return installedAppsManager.getAppCommands()
    // Returns: {"Open Chrome" -> "com.android.chrome", ...}
}
```

---

### A.1.2 AIDL IPC Service API (Phase 3)

**Package:** `com.augmentalis.voiceoscore.accessibility`
**Added:** November 2025 (Phase 3)
**Documentation:** [Chapter 38: IPC Architecture Guide](38-IPC-Architecture-Guide.md)

The AIDL-based IPC interface for external applications to interact with VoiceOSService.

#### Interface Definition (AIDL)

```aidl
// IVoiceOSService.aidl
package com.augmentalis.voiceoscore.accessibility;

interface IVoiceOSService {
    // Public API (12 methods)
    boolean executeCommand(String commandText);
    boolean executeAccessibilityAction(String actionType, String parameters);
    void registerCallback(IVoiceOSCallback callback);
    void unregisterCallback(IVoiceOSCallback callback);
    String getServiceStatus();
    List<String> getAvailableCommands();

    // Phase 3 Extended Methods
    boolean startVoiceRecognition(String language, String recognizerType);
    boolean stopVoiceRecognition();
    String learnCurrentApp();
    List<String> getLearnedApps();
    List<String> getCommandsForApp(String packageName);
    boolean registerDynamicCommand(String commandText, String actionJson);

    // Internal methods (hidden with @hide)
    /** @hide Internal use only */
    boolean isServiceReady();
    /** @hide Internal use only */
    String scrapeCurrentScreen();
}
```

#### Binding to Service

```kotlin
// In your client application
class VoiceOSClient : AppCompatActivity() {
    private var voiceOSService: IVoiceOSService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            voiceOSService = IVoiceOSService.Stub.asInterface(service)
            Log.i(TAG, "Connected to VoiceOS IPC Service")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceOSService = null
        }
    }

    fun bindToVoiceOS() {
        val intent = Intent().apply {
            action = "com.augmentalis.voiceoscore.BIND_IPC"
            `package` = "com.augmentalis.voiceoscore"
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
}
```

#### Method Reference

##### executeCommand(commandText: String): Boolean
**Purpose:** Execute a voice command programmatically
**Parameters:**
- `commandText` - The command to execute (e.g., "go back", "go home")
**Returns:** `true` if command executed successfully
**Thread:** Binder thread (safe to call from any thread)

**Example:**
```kotlin
val success = voiceOSService?.executeCommand("go home")
```

##### startVoiceRecognition(language: String, recognizerType: String): Boolean
**Purpose:** Start voice recognition with specified configuration
**Parameters:**
- `language` - Language code (e.g., "en-US", "es-ES")
- `recognizerType` - Recognition mode ("continuous", "command", "static")
**Returns:** `true` if started successfully

**Example:**
```kotlin
voiceOSService?.startVoiceRecognition("en-US", "continuous")
```

##### stopVoiceRecognition(): Boolean
**Purpose:** Stop active voice recognition
**Returns:** `true` if stopped successfully

##### learnCurrentApp(): String
**Purpose:** Trigger UI scraping for the current foreground app
**Returns:** JSON with UI elements (max 50 elements)
**Format:**
```json
{
  "packageName": "com.example.app",
  "elements": [
    {"text": "Submit", "type": "button", "clickable": true, ...},
    ...
  ]
}
```

##### getLearnedApps(): List<String>
**Purpose:** Get list of apps with learned commands
**Returns:** List of package names

##### getCommandsForApp(packageName: String): List<String>
**Purpose:** Get available commands for a specific app
**Parameters:**
- `packageName` - App package name
**Returns:** List of command strings

##### registerDynamicCommand(commandText: String, actionJson: String): Boolean
**Purpose:** Register a runtime voice command
**Parameters:**
- `commandText` - The voice command phrase
- `actionJson` - JSON defining the command action
**Returns:** `true` if registered successfully

##### getServiceStatus(): String
**Purpose:** Get current service status
**Returns:** JSON status object
**Format:**
```json
{
  "ready": true,
  "running": true
}
```

##### getAvailableCommands(): List<String>
**Purpose:** Get all available voice commands
**Returns:** List of command strings

#### Callback Interface

```aidl
// IVoiceOSCallback.aidl
interface IVoiceOSCallback {
    void onCommandExecuted(String command, boolean success);
    void onServiceStatusChanged(String status);
}
```

**Example:**
```kotlin
private val callback = object : IVoiceOSCallback.Stub() {
    override fun onCommandExecuted(command: String, success: Boolean) {
        Log.i(TAG, "Command: $command, Success: $success")
    }

    override fun onServiceStatusChanged(status: String) {
        Log.i(TAG, "Status: $status")
    }
}

voiceOSService?.registerCallback(callback)
```

#### Security

**Protection Level:** Signature
- Only apps signed with the same certificate can bind
- Automatic for all `com.augmentalis.*` packages

**Manifest Declaration:**
```xml
<service
    android:name=".accessibility.VoiceOSIPCService"
    android:permission="signature">
    <intent-filter>
        <action android:name="com.augmentalis.voiceoscore.BIND_IPC" />
    </intent-filter>
</service>
```

---

### A.1.3 UI Scraping Engine

**Package:** `com.augmentalis.voiceoscore.accessibility.extractors`

#### Class: UIScrapingEngine

```kotlin
class UIScrapingEngine @Inject constructor(
    private val context: Context,
    private val scrapedElementDao: ScrapedElementDao,
    private val screenContextDao: ScreenContextDao,
    private val commandGenerator: CommandGenerator
) {
    suspend fun scrapeCurrentScreen(packageName: String?): ScrapeResult
    suspend fun scrapeWindow(windowInfo: AccessibilityWindowInfo): WindowScrapeResult
    suspend fun scrapeNode(node: AccessibilityNodeInfo, depth: Int): NodeScrapeResult

    fun isScrapingEnabled(): Boolean
    fun setScrapingEnabled(enabled: Boolean)
}
```

#### Method: scrapeCurrentScreen()

**Signature:**
```kotlin
suspend fun scrapeCurrentScreen(packageName: String?): ScrapeResult
```

**Purpose:** Scrape all interactive elements from current screen
**Thread:** Background (suspending function)
**Parameters:**
- `packageName: String?` - Package name of current app (nullable)

**Return Type:**
```kotlin
data class ScrapeResult(
    val success: Boolean,
    val elementCount: Int,
    val commandCount: Int,
    val screenHash: String,
    val errorMessage: String? = null
)
```

**Process Flow:**
1. Get root accessibility node
2. Traverse accessibility tree
3. Extract interactive elements
4. Generate voice commands
5. Save to database
6. Return result

**Example:**
```kotlin
val result = scrapingEngine.scrapeCurrentScreen("com.example.app")

if (result.success) {
    Log.d(TAG, "Scraped ${result.elementCount} elements")
    Log.d(TAG, "Generated ${result.commandCount} commands")
} else {
    Log.e(TAG, "Scraping failed: ${result.errorMessage}")
}
```

**Error Handling:**
```kotlin
suspend fun scrapeCurrentScreen(packageName: String?): ScrapeResult {
    return try {
        // Get root node
        val rootNode = serviceInstance.rootInActiveWindow
            ?: return ScrapeResult(
                success = false,
                elementCount = 0,
                commandCount = 0,
                screenHash = "",
                errorMessage = "No root node available"
            )

        // Scrape elements
        val elements = mutableListOf<ScrapedElementEntity>()
        scrapeNodeRecursive(rootNode, elements, depth = 0)

        // Generate commands
        val commands = commandGenerator.generateCommands(elements)

        // Save to database
        scrapedElementDao.insertBatch(elements)
        generatedCommandDao.insertBatch(commands)

        ScrapeResult(
            success = true,
            elementCount = elements.size,
            commandCount = commands.size,
            screenHash = calculateScreenHash(elements)
        )
    } catch (e: Exception) {
        Log.e(TAG, "Scraping error", e)
        ScrapeResult(
            success = false,
            elementCount = 0,
            commandCount = 0,
            screenHash = "",
            errorMessage = e.message
        )
    }
}
```

---

## A.2 Accessibility Service APIs

### A.2.1 ServiceMonitor

**Package:** `com.augmentalis.voiceoscore.accessibility.monitor`

Monitors service health and connection state.

#### Class Definition

```kotlin
class ServiceMonitor @Inject constructor(
    private val context: Context
) {
    fun startMonitoring(callback: ServiceCallback)
    fun stopMonitoring()
    fun getConnectionState(): ConnectionState
    fun isServiceEnabled(): Boolean
    fun isServiceRunning(): Boolean
}
```

#### Enum: ConnectionState

```kotlin
enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR
}
```

#### Interface: ServiceCallback

```kotlin
interface ServiceCallback {
    fun onServiceConnected()
    fun onServiceDisconnected()
    fun onServiceError(error: String)
    fun onServiceRecovering()
}
```

---

## A.3 UI Scraping APIs

### A.3.1 ScrapedElementEntity

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

Represents a scraped UI element.

```kotlin
@Entity(tableName = "scraped_elements")
data class ScrapedElementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "element_hash") val elementHash: String,
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "uuid") val uuid: String?,
    @ColumnInfo(name = "class_name") val className: String,
    @ColumnInfo(name = "view_id_resource_name") val viewIdResourceName: String?,
    @ColumnInfo(name = "text") val text: String?,
    @ColumnInfo(name = "content_description") val contentDescription: String?,
    @ColumnInfo(name = "bounds") val bounds: String,
    @ColumnInfo(name = "is_clickable") val isClickable: Boolean,
    @ColumnInfo(name = "is_long_clickable") val isLongClickable: Boolean,
    @ColumnInfo(name = "is_editable") val isEditable: Boolean,
    @ColumnInfo(name = "is_scrollable") val isScrollable: Boolean,
    @ColumnInfo(name = "is_checkable") val isCheckable: Boolean,
    @ColumnInfo(name = "is_focusable") val isFocusable: Boolean,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "depth") val depth: Int,
    @ColumnInfo(name = "index_in_parent") val indexInParent: Int,
    @ColumnInfo(name = "scraped_at") val scrapedAt: Long,

    // AI Context Fields (Phase 1)
    @ColumnInfo(name = "semantic_role") val semanticRole: String?,
    @ColumnInfo(name = "input_type") val inputType: String?,
    @ColumnInfo(name = "visual_weight") val visualWeight: String?,
    @ColumnInfo(name = "is_required") val isRequired: Boolean?,

    // Form Context Fields (Phase 2)
    @ColumnInfo(name = "form_group_id") val formGroupId: String?,
    @ColumnInfo(name = "placeholder_text") val placeholderText: String?,
    @ColumnInfo(name = "validation_pattern") val validationPattern: String?,
    @ColumnInfo(name = "background_color") val backgroundColor: String?
)
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `elementHash` | String | SHA-256 hash of element properties |
| `appId` | String | Foreign key to apps table |
| `uuid` | String? | Universal element identifier |
| `className` | String | Android view class name |
| `viewIdResourceName` | String? | Resource ID (e.g., "com.app:id/button1") |
| `text` | String? | Visible text content |
| `contentDescription` | String? | Accessibility description |
| `bounds` | String | Element bounds (format: "[x1,y1][x2,y2]") |
| `isClickable` | Boolean | Can be clicked |
| `isLongClickable` | Boolean | Can be long-clicked |
| `isEditable` | Boolean | Can receive text input |
| `isScrollable` | Boolean | Can be scrolled |
| `isCheckable` | Boolean | Has checkable state |
| `isFocusable` | Boolean | Can receive focus |
| `isEnabled` | Boolean | Currently enabled |
| `depth` | Int | Depth in accessibility tree |
| `indexInParent` | Int | Index within parent container |
| `scrapedAt` | Long | Timestamp (milliseconds) |
| `semanticRole` | String? | Inferred role (button, input, label, etc.) |
| `inputType` | String? | Input type for text fields |
| `visualWeight` | String? | Visual prominence (primary, secondary) |
| `isRequired` | Boolean? | Required field indicator |
| `formGroupId` | String? | Associated form group |
| `placeholderText` | String? | Placeholder text for inputs |
| `validationPattern` | String? | Expected input pattern |
| `backgroundColor` | String? | Background color (hex) |

---

## A.4 SpeechRecognition Library

### A.4.1 Multi-Engine Architecture

**Package:** `com.augmentalis.voiceos.speech`

#### Interface: SpeechEngine

```kotlin
interface SpeechEngine {
    suspend fun initialize(context: Context, config: EngineConfig): InitResult
    suspend fun start(): Boolean
    suspend fun stop(): Boolean
    fun isListening(): Boolean

    fun addListener(listener: RecognitionListener)
    fun removeListener(listener: RecognitionListener)

    suspend fun setCommands(commands: List<Command>)
    suspend fun updateLanguage(languageCode: String)
}
```

#### Implementations

**Available Engines:**
1. **AndroidSTTEngine** - Google on-device speech recognition
2. **VivokaEngine** - Vivoka SDK (offline, multilingual)
3. **WhisperEngine** - OpenAI Whisper (offline, high accuracy)
4. **VoskEngine** - Vosk speech recognition (lightweight offline)
5. **GoogleCloudEngine** - Google Cloud Speech-to-Text (cloud-based)

**Engine Selection Matrix:**

| Engine | Offline | Accuracy | Latency | Languages | Memory | Use Case |
|--------|---------|----------|---------|-----------|--------|----------|
| Android | No | Medium | Low | 100+ | Low | Default, battery-friendly |
| Vivoka | Yes | High | Medium | 20+ | Medium | Offline command recognition |
| Whisper | Yes | Very High | High | 99+ | High | High-accuracy offline |
| Vosk | Yes | Medium | Low | 20+ | Low | Lightweight offline |
| Google Cloud | No | Very High | Medium | 120+ | Low | Cloud-based, high accuracy |

---

### A.4.2 Recognition Callbacks

#### Interface: RecognitionListener

```kotlin
interface RecognitionListener {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onPartialResults(partialResults: List<String>)
    fun onResults(results: RecognitionResult)
    fun onError(error: SpeechError)
    fun onEndOfSpeech()
}
```

#### Data Class: RecognitionResult

```kotlin
data class RecognitionResult(
    val text: String,
    val confidence: Float,
    val alternatives: List<Alternative>,
    val isFinal: Boolean,
    val timestamp: Long
) {
    data class Alternative(
        val text: String,
        val confidence: Float
    )
}
```

#### Enum: SpeechError

```kotlin
enum class SpeechError {
    NETWORK_ERROR,
    AUDIO_ERROR,
    SERVER_ERROR,
    CLIENT_ERROR,
    SPEECH_TIMEOUT,
    NO_MATCH,
    INSUFFICIENT_PERMISSIONS,
    ENGINE_NOT_INITIALIZED
}
```

---

## A.5 Engine APIs

### A.5.1 AndroidSTTEngine

**Package:** `com.augmentalis.voiceos.speech.engines.android`

#### Class: AndroidSTTEngine

```kotlin
class AndroidSTTEngine : SpeechEngine {
    suspend fun initialize(context: Context, config: EngineConfig): InitResult
    suspend fun start(): Boolean
    suspend fun stop(): Boolean
    fun isListening(): Boolean

    fun setPreferOffline(offline: Boolean)
    fun setMaxResults(maxResults: Int)
    fun setPartialResults(enabled: Boolean)
}
```

**Configuration:**
```kotlin
data class AndroidConfig(
    val preferOffline: Boolean = false,
    val maxResults: Int = 5,
    val partialResultsEnabled: Boolean = true,
    val languagePreference: String = "en-US"
)
```

**Usage Example:**
```kotlin
val engine = AndroidSTTEngine()

val config = AndroidConfig(
    preferOffline = true,
    maxResults = 3,
    partialResultsEnabled = true
)

val result = engine.initialize(context, config)

if (result.success) {
    engine.addListener(object : RecognitionListener {
        override fun onResults(results: RecognitionResult) {
            Log.d(TAG, "Recognized: ${results.text}")
        }

        override fun onError(error: SpeechError) {
            Log.e(TAG, "Recognition error: $error")
        }

        // ... implement other methods
    })

    engine.start()
}
```

---

### A.5.2 VivokaEngine

**Package:** `com.augmentalis.voiceos.speech.engines.vivoka`

#### Class: VivokaEngine

```kotlin
class VivokaEngine : SpeechEngine {
    suspend fun initialize(context: Context, config: VivokaConfig): InitResult
    suspend fun downloadModel(languageCode: String): DownloadResult
    suspend fun setVoiceCommands(commands: List<VoiceCommand>)

    fun enableWakeWord(wakeWord: String)
    fun disableWakeWord()
    fun setConfidenceThreshold(threshold: Float)
}
```

**Configuration:**
```kotlin
data class VivokaConfig(
    val apiKey: String,
    val languageModel: String,
    val enableWakeWord: Boolean = false,
    val wakeWord: String = "Hey Vivoka",
    val confidenceThreshold: Float = 0.7f
)
```

**Model Management:**
```kotlin
// Download language model
val downloadResult = vivokaEngine.downloadModel("en-US")

if (downloadResult.success) {
    Log.d(TAG, "Model downloaded successfully")
} else {
    Log.e(TAG, "Download failed: ${downloadResult.error}")
}

// Check available models
val models = vivokaEngine.getAvailableModels()
models.forEach { model ->
    Log.d(TAG, "Model: ${model.language} - ${model.size}MB")
}
```

---

## A.6 Recognition Callbacks

### A.6.1 Command Processing

#### Class: CommandProcessor

**Package:** `com.augmentalis.voiceos.speech.engines.common`

```kotlin
class CommandProcessor {
    fun processRecognitionResult(
        result: RecognitionResult,
        commands: List<Command>
    ): CommandMatch?

    fun findBestMatch(
        text: String,
        commands: List<Command>,
        threshold: Float = 0.7f
    ): CommandMatch?

    fun calculateSimilarity(text1: String, text2: String): Float
}
```

**Data Types:**
```kotlin
data class Command(
    val id: String,
    val text: String,
    val synonyms: List<String>,
    val action: CommandAction,
    val requiresConfirmation: Boolean = false
)

data class CommandMatch(
    val command: Command,
    val confidence: Float,
    val matchedText: String
)

sealed class CommandAction {
    data class Navigate(val target: String) : CommandAction()
    data class Click(val elementId: String) : CommandAction()
    data class Input(val text: String) : CommandAction()
    data class Scroll(val direction: ScrollDirection) : CommandAction()
    object Back : CommandAction()
    object Home : CommandAction()
}
```

**Usage:**
```kotlin
val processor = CommandProcessor()

// Process recognition result
val match = processor.processRecognitionResult(
    result = recognitionResult,
    commands = registeredCommands
)

if (match != null) {
    if (match.confidence > 0.8f) {
        // High confidence - execute immediately
        executeCommand(match.command)
    } else {
        // Low confidence - ask for confirmation
        showConfirmationDialog(match.command)
    }
} else {
    Log.w(TAG, "No command matched")
}
```

---

## A.7 CommandManager

### A.7.1 Command Registry

**Package:** `com.augmentalis.commandmanager`

#### Class: CommandManager

```kotlin
class CommandManager @Inject constructor(
    private val commandDao: VoiceCommandDao,
    private val commandUsageDao: CommandUsageDao
) {
    suspend fun registerCommand(command: VoiceCommand)
    suspend fun registerCommands(commands: List<VoiceCommand>)
    suspend fun unregisterCommand(commandId: String)

    suspend fun getCommand(commandId: String): VoiceCommand?
    suspend fun getAllCommands(): List<VoiceCommand>
    suspend fun getCommandsByCategory(category: String): List<VoiceCommand>

    suspend fun executeCommand(commandText: String): ExecutionResult
    suspend fun trackCommandUsage(commandId: String, successful: Boolean)
}
```

**Entities:**
```kotlin
@Entity(tableName = "voice_commands")
data class VoiceCommandEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "command_text") val commandText: String,
    @ColumnInfo(name = "action_type") val actionType: String,
    @ColumnInfo(name = "action_data") val actionData: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "synonyms") val synonyms: String,
    @ColumnInfo(name = "requires_confirmation") val requiresConfirmation: Boolean,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "command_usage")
data class CommandUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "command_id") val commandId: String,
    @ColumnInfo(name = "used_at") val usedAt: Long,
    @ColumnInfo(name = "successful") val successful: Boolean,
    @ColumnInfo(name = "execution_time_ms") val executionTimeMs: Long
)
```

---

## A.8 VoiceDataManager

### A.8.1 Data Synchronization

**Package:** `com.augmentalis.datamanager`

#### Class: VoiceDataManager

```kotlin
class VoiceDataManager @Inject constructor(
    private val database: VoiceOSDatabase,
    private val syncService: SyncService
) {
    suspend fun syncData(): SyncResult
    suspend fun exportData(outputPath: String): ExportResult
    suspend fun importData(inputPath: String): ImportResult

    suspend fun clearCache()
    suspend fun getDataSize(): Long

    fun enableAutoSync(enabled: Boolean)
    fun setSyncInterval(intervalMinutes: Int)
}
```

---

## A.9 LocalizationManager

### A.9.1 Multi-Language Support

**Package:** `com.augmentalis.localizationmanager`

#### Class: LocalizationManager

```kotlin
class LocalizationManager @Inject constructor(
    private val context: Context
) {
    fun setLanguage(languageCode: String)
    fun getAvailableLanguages(): List<Language>
    fun getCurrentLanguage(): Language

    fun getString(key: String, vararg formatArgs: Any): String
    fun getQuantityString(key: String, quantity: Int): String
}
```

**Data Types:**
```kotlin
data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val rtl: Boolean
)
```

---

## A.10 LicenseManager

### A.10.1 License Validation

**Package:** `com.augmentalis.licensemanager`

#### Class: LicenseManager

```kotlin
class LicenseManager @Inject constructor(
    private val context: Context
) {
    suspend fun validateLicense(): LicenseStatus
    suspend fun activateLicense(licenseKey: String): ActivationResult
    suspend fun deactivateLicense()

    fun isFeatureEnabled(featureId: String): Boolean
    fun getRemainingTrialDays(): Int
}
```

---

## A.11 UUIDCreator

### A.11.1 UUID Generation

**Package:** `com.augmentalis.uuidcreator`

#### Class: UUIDCreator

```kotlin
class UUIDCreator {
    fun generateUUID(): String
    fun generateUUIDForElement(element: ScrapedElementEntity): String
    fun validateUUID(uuid: String): Boolean
}
```

---

## A.12 DeviceManager

### A.12.1 Device Detection

**Package:** `com.augmentalis.devicemanager`

#### Class: DeviceManager

```kotlin
class DeviceManager {
    fun getDeviceInfo(): DeviceInfo
    fun isXRDevice(): Boolean
    fun getDeviceCapabilities(): DeviceCapabilities
}
```

---

## A.13 VoiceKeyboard

### A.13.1 Input Method

**Package:** `com.augmentalis.voicekeyboard`

#### Class: VoiceKeyboardService

```kotlin
class VoiceKeyboardService : InputMethodService() {
    fun enableVoiceInput()
    fun disableVoiceInput()
    fun insertText(text: String)
}
```

---

## A.14 VoiceUIElements

### A.14.1 UI Components

**Package:** `com.augmentalis.voiceuielements`

Reusable voice-accessible UI components.

---

## A.15 Room Database APIs

### A.15.1 Database Instance

**Package:** `com.augmentalis.voiceoscore.database`

#### Class: VoiceOSAppDatabase

```kotlin
@Database(
    entities = [
        AppEntity::class,
        ScreenEntity::class,
        ExplorationSessionEntity::class,
        ScrapedElementEntity::class,
        ScrapedHierarchyEntity::class,
        GeneratedCommandEntity::class,
        ScreenContextEntity::class,
        ScreenTransitionEntity::class,
        ElementRelationshipEntity::class,
        UserInteractionEntity::class,
        ElementStateHistoryEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class VoiceOSAppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun screenDao(): ScreenDao
    abstract fun explorationSessionDao(): ExplorationSessionDao
    abstract fun scrapedElementDao(): ScrapedElementDao
    abstract fun scrapedHierarchyDao(): ScrapedHierarchyDao
    abstract fun generatedCommandDao(): GeneratedCommandDao
    abstract fun screenContextDao(): ScreenContextDao
    abstract fun screenTransitionDao(): ScreenTransitionDao
    abstract fun elementRelationshipDao(): ElementRelationshipDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun elementStateHistoryDao(): ElementStateHistoryDao
}
```

---

## A.16 DAO Interfaces

### A.16.1 AppDao

Full API reference for AppDao (see database file read earlier for complete implementation).

**Key Methods:**
- `getApp(packageName: String): AppEntity?`
- `getAllApps(): List<AppEntity>`
- `insert(app: AppEntity)`
- `update(app: AppEntity)`
- `deleteApp(packageName: String)`
- `getAppsByExplorationStatus(status: String): List<AppEntity>`
- `getFullyLearnedApps(): List<AppEntity>`
- `markAsFullyLearned(packageName: String, timestamp: Long)`

### A.16.2 ScrapedElementDao

```kotlin
@Dao
interface ScrapedElementDao {
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId")
    suspend fun getElementsByAppId(appId: String): List<ScrapedElementEntity>

    @Query("SELECT * FROM scraped_elements WHERE element_hash = :hash LIMIT 1")
    suspend fun getElementByHash(hash: String): ScrapedElementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(element: ScrapedElementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(elements: List<ScrapedElementEntity>)

    @Query("DELETE FROM scraped_elements WHERE app_id = :appId")
    suspend fun deleteElementsByAppId(appId: String)
}
```

---

## Summary

This API reference covers the complete public API surface of VOS4, including:

- **Core Services:** 15+ classes
- **Speech Recognition:** 5 engine implementations
- **Managers:** 4 manager modules
- **Libraries:** 6 library modules
- **Database:** 11 entity types, 11 DAO interfaces

**Total API Surface:**
- **Interfaces:** 25+
- **Classes:** 75+
- **Methods:** 500+
- **Data Types:** 100+

---

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Complete
**Next Appendix:** [Appendix B: Database Schema Reference](Appendix-B-Database-Schema.md)
