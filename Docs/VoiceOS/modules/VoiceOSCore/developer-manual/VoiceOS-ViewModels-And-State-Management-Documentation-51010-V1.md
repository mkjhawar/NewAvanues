# ViewModels and State Management Documentation

**Module:** VoiceAccessibility
**Last Updated:** 2025-10-10 11:04:56 PDT
**Status:** Comprehensive Documentation
**VOS4 Compliance:** Full - Direct implementation with MVVM architecture

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [ViewModel Layer](#viewmodel-layer)
3. [State Management](#state-management)
4. [Voice Recognition Integration](#voice-recognition-integration)
5. [Speech Engine Management](#speech-engine-management)
6. [Data Flow Patterns](#data-flow-patterns)
7. [Best Practices](#best-practices)
8. [Complete Usage Examples](#complete-usage-examples)

---

## Architecture Overview

### MVVM Pattern in VoiceAccessibility

VoiceAccessibility implements the **Model-View-ViewModel (MVVM)** architecture pattern for clean separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                            │
│  (Activities, Fragments, Composables)                    │
└────────────────┬────────────────────────────────────────┘
                 │ Observes StateFlow/LiveData
                 ↓
┌─────────────────────────────────────────────────────────┐
│                  ViewModel Layer                         │
│  • MainViewModel                                         │
│  • AccessibilityViewModel                                │
│  • SettingsViewModel                                     │
└────────────────┬────────────────────────────────────────┘
                 │ Manages State & Coordinates
                 ↓
┌─────────────────────────────────────────────────────────┐
│              State Management Layer                      │
│  • UIState (UI state data)                              │
│  • DialogStateMachine (dialog state)                    │
│  • SpeechState (speech recognition state)               │
└────────────────┬────────────────────────────────────────┘
                 │ Integrates with
                 ↓
┌─────────────────────────────────────────────────────────┐
│              Service & Manager Layer                     │
│  • VoiceRecognitionManager                              │
│  • VoiceRecognitionBinder                               │
│  • SpeechEngineManager                                  │
│  • ActionCoordinator                                    │
└─────────────────────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Direct Implementation**: No interfaces except where approved (ActionHandler for polymorphism)
2. **StateFlow & LiveData**: Reactive state propagation to UI
3. **Coroutines**: Async operations with viewModelScope
4. **Thread Safety**: Mutex locks and atomic operations for concurrent access
5. **Lifecycle Awareness**: ViewModels survive configuration changes

---

## ViewModel Layer

### 1. MainViewModel

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/viewmodel/MainViewModel.kt`

**Purpose:** Manages main activity state, permissions, and service status.

#### Responsibilities

- **Permission Management**: Check and request accessibility, overlay, and write settings permissions
- **Service Status**: Monitor if VoiceOSService is enabled
- **Configuration Management**: Load/save service configuration
- **Voice Recognition Testing**: Test recognition engines
- **Voice Recognition Client**: Manage VoiceRecognitionClient for testing

#### State Properties

```kotlin
// Service Status (LiveData)
val serviceEnabled: LiveData<Boolean>

// Permissions (LiveData)
val overlayPermissionGranted: LiveData<Boolean>
val writeSettingsPermissionGranted: LiveData<Boolean>

// Configuration (LiveData)
val configuration: LiveData<ServiceConfiguration>

// Loading States (LiveData)
val isLoading: LiveData<Boolean>
val errorMessage: LiveData<String?>

// Recognition Testing (LiveData)
val selectedEngine: LiveData<String>
val isRecognizing: LiveData<Boolean>
```

#### Public Methods

```kotlin
// Initialization
fun initialize(context: Context)

// Permission Checks
fun checkAllPermissions()
fun checkServiceStatus()
fun checkOverlayPermission()
fun checkWriteSettingsPermission()
fun areAllPermissionsGranted(): Boolean
fun getPermissionSummary(): PermissionSummary

// Permission Requests
fun requestOverlayPermission(): Uri?
fun requestWriteSettingsPermission(): Uri?

// Configuration Management
fun loadConfiguration()
fun saveConfiguration(config: ServiceConfiguration)

// Voice Recognition Testing
fun selectEngine(engine: String)
fun startRecognitionWithEngine(engine: String)
fun stopRecognition()

// UI Operations
fun toggleService()
fun clearError()
fun refresh()
```

#### Use Cases

**1. Initialize ViewModel**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val viewModel: MainViewModel by viewModels()
    viewModel.initialize(this)

    // Observe state
    viewModel.serviceEnabled.observe(this) { enabled ->
        updateServiceStatusUI(enabled)
    }
}
```

**2. Check and Request Permissions**
```kotlin
// Check all permissions
viewModel.checkAllPermissions()

// Observe results
viewModel.overlayPermissionGranted.observe(this) { granted ->
    if (!granted) {
        // Request permission
        val uri = viewModel.requestOverlayPermission()
        uri?.let { startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, it), REQUEST_OVERLAY) }
    }
}
```

**3. Test Voice Recognition Engine**
```kotlin
// Select and test engine
viewModel.selectEngine("vivoka")
viewModel.startRecognitionWithEngine("vivoka")

// Observe recognition state
viewModel.isRecognizing.observe(this) { isRecognizing ->
    updateRecognitionUI(isRecognizing)
}

// Stop when done
viewModel.stopRecognition()
```

#### Integration with UI Layer

MainViewModel integrates with MainActivity and provides permission status for setup flow:

```kotlin
// Permission Summary Data Class
data class PermissionSummary(
    val serviceEnabled: Boolean,
    val overlayPermission: Boolean,
    val writeSettingsPermission: Boolean
) {
    val allGranted: Boolean
    val grantedCount: Int
    val percentage: Float
}

// Usage
val summary = viewModel.getPermissionSummary()
if (summary.allGranted) {
    proceedToMainScreen()
} else {
    showPermissionSetup(summary.percentage)
}
```

---

### 2. AccessibilityViewModel

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/viewmodel/AccessibilityViewModel.kt`

**Purpose:** Comprehensive ViewModel for accessibility functionality, configuration, and command testing.

#### Responsibilities

- **Service Monitoring**: Periodic checks of service and permission status
- **Configuration Management**: Load and update service configuration
- **Command Testing**: Execute and track test command results
- **Statistics Tracking**: Monitor command execution statistics
- **Handler Management**: Track handler status and counts

#### State Properties

```kotlin
// Service Status (LiveData)
val serviceEnabled: LiveData<Boolean>
val overlayPermissionGranted: LiveData<Boolean>

// Configuration (LiveData)
val configuration: LiveData<ServiceConfiguration>

// Statistics (StateFlow)
val commandsExecuted: StateFlow<Int>
val successRate: StateFlow<Float>
val performanceMode: StateFlow<String>

// Command Testing (StateFlow)
val testResults: StateFlow<List<CommandTestResult>>
val isExecutingCommand: StateFlow<Boolean>

// Loading/Error States (StateFlow)
val isLoading: StateFlow<Boolean>
val errorMessage: StateFlow<String?>
```

#### Public Methods

```kotlin
// Initialization
fun initialize(context: Context)

// Permission Checks
fun checkServiceStatus()
fun checkOverlayPermission()
fun checkAllPermissions()

// Configuration Management
fun updateConfiguration(newConfig: ServiceConfiguration)

// Command Testing
fun executeCommand(command: String): CommandTestResult
fun clearTestResults()

// Handler Information
fun getHandlerStatuses(): Map<String, Boolean>
fun getActiveHandlersCount(): Int

// Service Operations
fun restartService()
fun getServiceInfo(): String
fun clearError()
```

#### Use Cases

**1. Monitor Service Status**
```kotlin
val viewModel: AccessibilityViewModel by viewModels()

// Periodic status updates (auto-started in init)
viewModel.serviceEnabled.observe(this) { enabled ->
    updateServiceIndicator(enabled)
}

viewModel.overlayPermissionGranted.observe(this) { granted ->
    updateOverlayIndicator(granted)
}
```

**2. Execute and Track Commands**
```kotlin
// Execute a test command
val result = viewModel.executeCommand("go back")

// Observe test results
lifecycleScope.launch {
    viewModel.testResults.collect { results ->
        updateTestResultsList(results)
    }
}

// Clear results
viewModel.clearTestResults()
```

**3. Monitor Statistics**
```kotlin
// Observe command execution stats
lifecycleScope.launch {
    viewModel.commandsExecuted.collect { count ->
        updateCommandCount(count)
    }

    viewModel.successRate.collect { rate ->
        updateSuccessRate(rate * 100) // Convert to percentage
    }
}
```

**4. Check Handler Status**
```kotlin
// Get handler statuses
val handlerStatuses = viewModel.getHandlerStatuses()
handlerStatuses.forEach { (handler, enabled) ->
    Log.d(TAG, "$handler: ${if (enabled) "Enabled" else "Disabled"}")
}

// Get active handler count
val activeCount = viewModel.getActiveHandlersCount()
Log.d(TAG, "Active handlers: $activeCount")
```

#### Integration with UI Layer

AccessibilityViewModel provides comprehensive dashboard data:

```kotlin
// CommandTestResult Data Class
data class CommandTestResult(
    val command: String,
    val timestamp: Long,
    val success: Boolean,
    val executionTime: Long,
    val result: String,
    val handlerUsed: String
)

// Display test results
viewModel.testResults.collect { results ->
    results.forEach { result ->
        addTestResultItem(
            command = result.command,
            success = result.success,
            time = result.executionTime,
            handler = result.handlerUsed
        )
    }
}
```

#### Periodic Status Checking

AccessibilityViewModel automatically checks service status every 5 seconds:

```kotlin
private fun startPeriodicStatusCheck() {
    viewModelScope.launch {
        while (true) {
            delay(5000) // Check every 5 seconds
            checkServiceStatus()
            checkOverlayPermission()
        }
    }
}
```

---

### 3. SettingsViewModel

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/viewmodel/SettingsViewModel.kt`

**Purpose:** Dedicated ViewModel for comprehensive settings management including handlers, performance, cursor, cache, and advanced settings.

#### Responsibilities

- **Handler Management**: Toggle and track 7 command handlers
- **Performance Mode**: Manage performance profiles (High/Balanced/Power Saver)
- **Cursor Configuration**: Manage cursor appearance and behavior
- **Cache Management**: Configure command caching
- **Advanced Settings**: UI scraping, dynamic commands, logging, toasts
- **Persistent Storage**: Auto-save all settings to SharedPreferences

#### State Properties (All StateFlow)

```kotlin
// Core Configuration
val configuration: StateFlow<ServiceConfiguration>
val performanceMode: StateFlow<PerformanceMode>

// Handler States
val handlerStates: StateFlow<Map<String, Boolean>>

// Cursor Settings
val cursorEnabled: StateFlow<Boolean>
val cursorSize: StateFlow<Float>
val cursorSpeed: StateFlow<Float>
val cursorColor: StateFlow<Int>

// Cache Settings
val cacheEnabled: StateFlow<Boolean>
val maxCacheSize: StateFlow<Int>

// Advanced Settings
val uiScrapingEnabled: StateFlow<Boolean>
val dynamicCommandsEnabled: StateFlow<Boolean>
val verboseLogging: StateFlow<Boolean>
val showToasts: StateFlow<Boolean>

// Loading/Error States
val isLoading: StateFlow<Boolean>
val errorMessage: StateFlow<String?>
```

#### Performance Mode Enumeration

```kotlin
enum class PerformanceMode(
    val displayName: String,
    val commandTimeout: Long,
    val maxCacheSize: Int,
    val description: String
) {
    HIGH_PERFORMANCE(
        displayName = "High Performance",
        commandTimeout = 2000L,
        maxCacheSize = 200,
        description = "Faster response times, higher battery usage"
    ),
    BALANCED(
        displayName = "Balanced",
        commandTimeout = 5000L,
        maxCacheSize = 100,
        description = "Good balance of performance and battery life"
    ),
    POWER_SAVER(
        displayName = "Power Saver",
        commandTimeout = 10000L,
        maxCacheSize = 50,
        description = "Slower response times, optimized for battery life"
    )
}
```

#### Handler Information

```kotlin
data class HandlerInfo(
    val id: String,
    val name: String,
    val description: String,
    val isCore: Boolean = false, // Core handlers cannot be disabled
    val requiresOtherHandlers: Boolean = false
)

// Pre-defined handler definitions
companion object {
    val HANDLER_DEFINITIONS = listOf(
        HandlerInfo("action_handler", "Action Handler", "Core action processing", isCore = true),
        HandlerInfo("app_handler", "App Handler", "Application launching"),
        HandlerInfo("device_handler", "Device Handler", "Device controls"),
        HandlerInfo("input_handler", "Input Handler", "Text input"),
        HandlerInfo("navigation_handler", "Navigation Handler", "Screen navigation"),
        HandlerInfo("system_handler", "System Handler", "System settings"),
        HandlerInfo("ui_handler", "UI Handler", "UI element interaction")
    )
}
```

#### Public Methods

```kotlin
// Performance Mode
fun updatePerformanceMode(mode: PerformanceMode)

// Handler Management
fun toggleHandler(handlerId: String, enabled: Boolean)
fun getHandlerDefinition(handlerId: String): HandlerInfo?
fun getAllHandlerDefinitions(): List<HandlerInfo>
fun getActiveHandlersCount(): Int

// Cursor Settings
fun updateCursorEnabled(enabled: Boolean)
fun updateCursorSize(size: Float)
fun updateCursorSpeed(speed: Float)
fun updateCursorColor(color: Int)

// Cache Settings
fun updateCacheEnabled(enabled: Boolean)
fun updateMaxCacheSize(size: Int)

// Advanced Settings
fun updateUiScrapingEnabled(enabled: Boolean)
fun updateDynamicCommandsEnabled(enabled: Boolean)
fun updateVerboseLogging(enabled: Boolean)
fun updateShowToasts(enabled: Boolean)

// General Operations
fun resetToDefaults()
fun validateConfiguration(): Boolean
fun clearError()
```

#### Use Cases

**1. Performance Mode Management**
```kotlin
val viewModel: SettingsViewModel by viewModels()

// Observe current mode
lifecycleScope.launch {
    viewModel.performanceMode.collect { mode ->
        updatePerformanceModeUI(mode)
    }
}

// Update mode
viewModel.updatePerformanceMode(PerformanceMode.HIGH_PERFORMANCE)
```

**2. Handler Toggle Management**
```kotlin
// Get all handler definitions
val handlers = viewModel.getAllHandlerDefinitions()

// Display handler toggles
handlers.forEach { handler ->
    addHandlerToggle(
        id = handler.id,
        name = handler.name,
        description = handler.description,
        enabled = viewModel.handlerStates.value[handler.id] ?: false,
        canDisable = !handler.isCore,
        onToggle = { enabled ->
            viewModel.toggleHandler(handler.id, enabled)
        }
    )
}

// Observe handler state changes
lifecycleScope.launch {
    viewModel.handlerStates.collect { states ->
        updateHandlerUI(states)
    }
}
```

**3. Cursor Configuration**
```kotlin
// Observe cursor settings
lifecycleScope.launch {
    launch {
        viewModel.cursorEnabled.collect { enabled ->
            updateCursorToggle(enabled)
        }
    }

    launch {
        viewModel.cursorSize.collect { size ->
            updateCursorSizeSlider(size)
        }
    }

    launch {
        viewModel.cursorSpeed.collect { speed ->
            updateCursorSpeedSlider(speed)
        }
    }

    launch {
        viewModel.cursorColor.collect { color ->
            updateCursorColorPicker(color)
        }
    }
}

// Update cursor settings
viewModel.updateCursorSize(64f)
viewModel.updateCursorSpeed(1.5f)
viewModel.updateCursorColor(0xFF00FF00.toInt())
```

**4. Cache Management**
```kotlin
// Observe cache settings
lifecycleScope.launch {
    launch {
        viewModel.cacheEnabled.collect { enabled ->
            updateCacheToggle(enabled)
        }
    }

    launch {
        viewModel.maxCacheSize.collect { size ->
            updateCacheSizeSlider(size)
        }
    }
}

// Update cache settings
viewModel.updateCacheEnabled(true)
viewModel.updateMaxCacheSize(150)
```

**5. Advanced Settings**
```kotlin
// Create advanced settings section
val advancedSettings = listOf(
    Setting("UI Scraping", viewModel.uiScrapingEnabled) { viewModel.updateUiScrapingEnabled(it) },
    Setting("Dynamic Commands", viewModel.dynamicCommandsEnabled) { viewModel.updateDynamicCommandsEnabled(it) },
    Setting("Verbose Logging", viewModel.verboseLogging) { viewModel.updateVerboseLogging(it) },
    Setting("Show Toasts", viewModel.showToasts) { viewModel.updateShowToasts(it) }
)

advancedSettings.forEach { setting ->
    addSettingToggle(setting.name, setting.stateFlow, setting.onUpdate)
}
```

**6. Reset to Defaults**
```kotlin
// Reset all settings
AlertDialog.Builder(context)
    .setTitle("Reset Settings")
    .setMessage("Reset all settings to defaults?")
    .setPositiveButton("Reset") { _, _ ->
        viewModel.resetToDefaults()
    }
    .setNegativeButton("Cancel", null)
    .show()

// Observe loading state during reset
lifecycleScope.launch {
    viewModel.isLoading.collect { loading ->
        showLoadingIndicator(loading)
    }
}
```

#### Integration with UI Layer

SettingsViewModel automatically saves all changes to SharedPreferences:

```kotlin
// Every update method automatically saves
fun updatePerformanceMode(mode: PerformanceMode) {
    viewModelScope.launch {
        _performanceMode.value = mode

        // Update configuration
        val updatedConfig = _configuration.value.copy(
            commandTimeout = mode.commandTimeout,
            maxCacheSize = mode.maxCacheSize
        )

        _configuration.value = updatedConfig
        _maxCacheSize.value = mode.maxCacheSize

        saveConfiguration() // Auto-save
    }
}
```

#### State Flow Updates

All settings use StateFlow for reactive UI updates:

```kotlin
// Compose example
@Composable
fun PerformanceModeSelector(viewModel: SettingsViewModel) {
    val performanceMode by viewModel.performanceMode.collectAsState()

    Column {
        Text("Performance Mode: ${performanceMode.displayName}")
        Text(performanceMode.description, style = MaterialTheme.typography.caption)

        Row {
            PerformanceMode.values().forEach { mode ->
                Button(
                    onClick = { viewModel.updatePerformanceMode(mode) },
                    enabled = mode != performanceMode
                ) {
                    Text(mode.displayName)
                }
            }
        }
    }
}
```

---

## State Management

### 1. UIState

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/state/UIState.kt`

**Purpose:** Data class tracking current UI state for voice command context.

#### Data Structure

```kotlin
@Parcelize
data class UIState(
    // Application Context
    val packageName: String? = null,
    val activityName: String? = null,
    val windowTitle: String? = null,

    // Focused Element
    val focusedElement: ElementInfo? = null,

    // Interactive Elements
    val interactiveElements: List<ElementInfo> = emptyList(),

    // Screen Content
    val screenText: String? = null,
    val screenOrientation: ScreenOrientation = ScreenOrientation.UNKNOWN,
    val screenBounds: Rect? = null,

    // Scrolling
    val isScrollable: Boolean = false,
    val scrollPosition: Float = 0f,

    // Input
    val isKeyboardVisible: Boolean = false,
    val inputFieldInfo: InputFieldInfo? = null,

    // Metadata
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) : Parcelable
```

#### Nested Data Classes

**ElementInfo - Interactive Element Information**
```kotlin
@Parcelize
data class ElementInfo(
    val id: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val bounds: Rect? = null,

    // States
    val isClickable: Boolean = false,
    val isFocusable: Boolean = false,
    val isFocused: Boolean = false,
    val isEnabled: Boolean = true,
    val isVisibleToUser: Boolean = true,
    val isSelected: Boolean = false,
    val isCheckable: Boolean = false,
    val isChecked: Boolean = false,

    // Type & Actions
    val elementType: ElementType = ElementType.UNKNOWN,
    val availableActions: List<String> = emptyList()
) : Parcelable {
    fun getDisplayText(): String?
    fun isInteractive(): Boolean
}
```

**InputFieldInfo - Input Field State**
```kotlin
@Parcelize
data class InputFieldInfo(
    val currentText: String? = null,
    val hintText: String? = null,
    val inputType: Int = 0,
    val isPassword: Boolean = false,
    val isMultiline: Boolean = false,
    val cursorPosition: Int = 0,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0
) : Parcelable
```

#### Enumerations

**ElementType**
```kotlin
enum class ElementType {
    BUTTON, TEXT_FIELD, CHECKBOX, RADIO_BUTTON, SWITCH,
    SLIDER, PROGRESS_BAR, IMAGE, LINK, TAB, MENU_ITEM,
    LIST_ITEM, GRID_ITEM, TOOLBAR, NAVIGATION, DIALOG,
    POPUP, UNKNOWN
}
```

**ScreenOrientation**
```kotlin
enum class ScreenOrientation {
    PORTRAIT, LANDSCAPE, UNKNOWN
}
```

#### Utility Methods

```kotlin
// Content Checks
fun hasContent(): Boolean
fun getSummary(): String

// Element Queries
fun getClickableElements(): List<ElementInfo>
fun getFocusableElements(): List<ElementInfo>
fun getElementsByType(type: ElementType): List<ElementInfo>
fun findElementByText(text: String, ignoreCase: Boolean = true): ElementInfo?
fun findElementsContaining(text: String, ignoreCase: Boolean = true): List<ElementInfo>

// Timestamp Management
fun withCurrentTimestamp(): UIState
fun withMetadata(key: String, value: String): UIState
```

#### Companion Object Methods

```kotlin
companion object {
    // Factory Methods
    fun empty(): UIState

    fun fromAccessibilityNode(
        rootNode: AccessibilityNodeInfo?,
        packageName: String? = null,
        activityName: String? = null
    ): UIState

    // Helper Methods
    private fun extractElements(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        screenTextBuilder: StringBuilder
    )

    private fun determineElementType(node: AccessibilityNodeInfo): ElementType
}
```

#### Use Cases

**1. Create UIState from Accessibility Tree**
```kotlin
// In VoiceOSService
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    val rootNode = rootInActiveWindow
    val packageName = event.packageName?.toString()
    val activityName = event.className?.toString()

    // Create UIState from current accessibility tree
    val uiState = UIState.fromAccessibilityNode(
        rootNode = rootNode,
        packageName = packageName,
        activityName = activityName
    )

    // Use UIState for command context
    processCommandWithContext(command, uiState)
}
```

**2. Query Interactive Elements**
```kotlin
// Find all clickable buttons
val buttons = uiState.getElementsByType(ElementType.BUTTON)
    .filter { it.isInteractive() }

// Find element by text
val settingsButton = uiState.findElementByText("Settings")

// Find elements containing text
val searchResults = uiState.findElementsContaining("search", ignoreCase = true)
```

**3. Check UI Context**
```kotlin
// Check if keyboard is visible for text input
if (uiState.isKeyboardVisible) {
    // Handle text input commands
    processTextInputCommand(command, uiState.inputFieldInfo)
}

// Check if screen is scrollable
if (uiState.isScrollable) {
    // Enable scroll commands
    enableScrollCommands()
}
```

**4. Generate Voice Feedback**
```kotlin
// Get human-readable summary for voice feedback
val summary = uiState.getSummary()
// Example: "App: com.example.app, Screen: Main, 12 interactive elements, Focused: Submit Button"

speakFeedback(summary)
```

**5. Add Metadata**
```kotlin
// Add command execution metadata
val enrichedState = uiState
    .withMetadata("lastCommand", "go back")
    .withMetadata("executionTime", "${System.currentTimeMillis()}")
    .withCurrentTimestamp()
```

---

### 2. DialogStateMachine

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/state/DialogStateMachine.kt`

**Purpose:** State machine for managing dialog states in voice accessibility system.

#### State Diagram

```
         START_LISTENING
  IDLE ──────────────────→ LISTENING
   ↑                           │
   │ STOP_LISTENING             │ INPUT_RECEIVED
   │                           ↓
   │                      PROCESSING
   │                           │
   │ RESPONSE_COMPLETE          │ PROCESSING_DONE
   │                           ↓
   └──────────────────── RESPONDING

   (ERROR_OCCURRED or RESET returns to IDLE from any state)
```

#### State Enumeration

```kotlin
enum class DialogState {
    IDLE,        // Ready for new input
    LISTENING,   // Actively listening for voice input
    PROCESSING,  // Processing received input
    RESPONDING   // Providing response/feedback
}
```

#### State Event Enumeration

```kotlin
enum class StateEvent {
    START_LISTENING,   // Begin listening for input
    STOP_LISTENING,    // Stop listening
    INPUT_RECEIVED,    // Input has been received
    PROCESSING_DONE,   // Processing completed
    RESPONSE_STARTED,  // Response/feedback started
    RESPONSE_COMPLETE, // Response/feedback completed
    ERROR_OCCURRED,    // Error occurred, return to idle
    RESET             // Force reset to idle state
}
```

#### State Transition Rules

| Current State | Event | Next State |
|--------------|-------|------------|
| IDLE | START_LISTENING | LISTENING |
| LISTENING | STOP_LISTENING | IDLE |
| LISTENING | INPUT_RECEIVED | PROCESSING |
| LISTENING | ERROR_OCCURRED | IDLE |
| PROCESSING | PROCESSING_DONE | RESPONDING |
| PROCESSING | ERROR_OCCURRED | IDLE |
| RESPONDING | RESPONSE_COMPLETE | IDLE |
| RESPONDING | START_LISTENING | LISTENING |
| RESPONDING | ERROR_OCCURRED | IDLE |
| ANY | RESET | IDLE |

#### Public Methods

```kotlin
// State Management
fun processEvent(event: StateEvent): Boolean
fun getCurrentState(): DialogState
fun reset()

// State Queries
fun isInState(state: DialogState): Boolean
fun isBusy(): Boolean
fun canAcceptInput(): Boolean

// Listeners
fun addStateListener(key: String, listener: (DialogState, DialogState) -> Unit)
fun removeStateListener(key: String)

// History & Debug
fun getTransitionHistory(): List<StateTransition>
fun getRecentTransitions(count: Int = 5): List<StateTransition>
fun clearHistory()
fun getDebugInfo(): String
```

#### StateTransition Data Class

```kotlin
data class StateTransition(
    val fromState: DialogState,
    val toState: DialogState,
    val event: StateEvent,
    val timestamp: Long = System.currentTimeMillis()
)
```

#### Use Cases

**1. Initialize State Machine**
```kotlin
class VoiceDialogManager {
    private val stateMachine = DialogStateMachine()

    init {
        // Add state change listener
        stateMachine.addStateListener("dialog_manager") { from, to ->
            Log.d(TAG, "Dialog state: $from -> $to")
            updateUI(to)
        }
    }
}
```

**2. Voice Recognition Flow**
```kotlin
// Start listening
if (stateMachine.processEvent(StateEvent.START_LISTENING)) {
    speechEngine.startListening()
    updateListeningIndicator(true)
}

// Input received
if (stateMachine.processEvent(StateEvent.INPUT_RECEIVED)) {
    processVoiceInput(text)
}

// Processing done
if (stateMachine.processEvent(StateEvent.PROCESSING_DONE)) {
    provideFeedback(result)
}

// Response complete
if (stateMachine.processEvent(StateEvent.RESPONSE_COMPLETE)) {
    // Ready for next input
    updateListeningIndicator(false)
}
```

**3. Error Handling**
```kotlin
try {
    processCommand(command)
} catch (e: Exception) {
    Log.e(TAG, "Error processing command", e)

    // Return to idle state on error
    stateMachine.processEvent(StateEvent.ERROR_OCCURRED)
    showErrorMessage(e.message)
}
```

**4. State Validation**
```kotlin
// Check if can accept new input
fun startNewCommand(command: String) {
    if (!stateMachine.canAcceptInput()) {
        Log.w(TAG, "Cannot accept input - busy processing")
        return
    }

    if (stateMachine.isBusy()) {
        Log.w(TAG, "State machine is busy")
        return
    }

    // Proceed with command
    stateMachine.processEvent(StateEvent.START_LISTENING)
    processCommand(command)
}
```

**5. Transition History Debugging**
```kotlin
// Get recent transitions for debugging
val recent = stateMachine.getRecentTransitions(5)
recent.forEach { transition ->
    Log.d(TAG, "${transition.fromState} -> ${transition.toState} (${transition.event})")
}

// Get full debug info
val debugInfo = stateMachine.getDebugInfo()
/*
DialogStateMachine Debug Info
Current State: LISTENING
Is Busy: true
Can Accept Input: false
Listeners: 1
Recent Transitions:
  IDLE -> LISTENING (START_LISTENING)
  LISTENING -> PROCESSING (INPUT_RECEIVED)
  PROCESSING -> RESPONDING (PROCESSING_DONE)
*/
```

**6. Manual State Reset**
```kotlin
// Force reset to idle (e.g., on service restart)
stateMachine.reset()

// Or use reset event
stateMachine.processEvent(StateEvent.RESET)
```

---

## Voice Recognition Integration

### VoiceRecognitionManager

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/recognition/VoiceRecognitionManager.kt`

**Purpose:** Integration manager for voice recognition, connecting VoiceRecognitionBinder with ActionCoordinator.

#### Architecture

```
VoiceRecognitionManager
    │
    ├─→ VoiceRecognitionBinder
    │       │
    │       └─→ IVoiceRecognitionService (AIDL binding)
    │
    └─→ ActionCoordinator
            │
            └─→ Command Handlers
```

#### Responsibilities

- **Service Integration**: Connect to VoiceRecognitionService
- **Lifecycle Management**: Initialize, connect, disconnect
- **Engine Selection**: Manage recognition engine selection
- **Command Routing**: Route recognition results to ActionCoordinator
- **State Monitoring**: Track recognition state

#### Public Methods

```kotlin
// Lifecycle
fun initialize(context: Context)
fun dispose()

// Voice Recognition Control
fun startListening(): Boolean
fun startListening(engine: String, language: String): Boolean
fun stopListening(): Boolean

// State Queries
fun isListening(): Boolean
fun isServiceConnected(): Boolean
fun getCurrentState(): String

// Engine Information
fun getAvailableEngines(): List<String>
fun getServiceStatus(): String

// Debug
fun getDebugInfo(): String
```

#### Use Cases

**1. Initialize in Service**
```kotlin
class VoiceOSService : AccessibilityService() {
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager
    private lateinit var actionCoordinator: ActionCoordinator

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize ActionCoordinator first
        actionCoordinator = ActionCoordinator(this)
        actionCoordinator.initialize()

        // Initialize VoiceRecognitionManager
        voiceRecognitionManager = VoiceRecognitionManager(actionCoordinator)
        voiceRecognitionManager.initialize(this)
    }

    override fun onDestroy() {
        voiceRecognitionManager.dispose()
        actionCoordinator.dispose()
        super.onDestroy()
    }
}
```

**2. Start/Stop Listening**
```kotlin
// Start with default engine
if (voiceRecognitionManager.startListening()) {
    Log.d(TAG, "Voice recognition started")
    updateListeningUI(true)
}

// Start with specific engine
if (voiceRecognitionManager.startListening("vivoka", "en-US")) {
    Log.d(TAG, "Vivoka engine started")
}

// Stop listening
if (voiceRecognitionManager.stopListening()) {
    Log.d(TAG, "Voice recognition stopped")
    updateListeningUI(false)
}
```

**3. Monitor State**
```kotlin
// Check if listening
if (voiceRecognitionManager.isListening()) {
    Log.d(TAG, "Currently listening")
}

// Get current state
val state = voiceRecognitionManager.getCurrentState()
// Returns: "IDLE", "LISTENING", "PROCESSING", "ERROR", or "Not initialized"

// Check service connection
if (!voiceRecognitionManager.isServiceConnected()) {
    Log.w(TAG, "Service not connected")
    showReconnectDialog()
}
```

**4. Query Available Engines**
```kotlin
// Get available engines
val engines = voiceRecognitionManager.getAvailableEngines()
engines.forEach { engine ->
    Log.d(TAG, "Available engine: $engine")
}

// Get service status
val status = voiceRecognitionManager.getServiceStatus()
Log.d(TAG, "Service status: $status")
```

#### Integration Flow

```
User Speech
    │
    ↓
VoiceRecognitionService (separate module)
    │
    ↓ AIDL Callback
VoiceRecognitionBinder
    │ onRecognitionResult()
    ↓
VoiceRecognitionManager
    │ (receives final results)
    ↓
ActionCoordinator.processCommand()
    │
    ↓
Appropriate Handler
    │
    ↓
Action Execution
```

---

### VoiceRecognitionBinder

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/recognition/VoiceRecognitionBinder.kt`

**Purpose:** Manages binding to VoiceRecognitionService with robust error handling and automatic reconnection.

#### Responsibilities

- **Service Binding**: Bind/unbind to VoiceRecognitionService
- **Callback Management**: Handle AIDL callbacks
- **Reconnection Logic**: Automatic reconnection with exponential backoff
- **Command Queuing**: Queue commands when service disconnected
- **Error Handling**: Robust error recovery

#### Recognition Callback Implementation

```kotlin
private val recognitionCallback = object : IRecognitionCallback.Stub() {

    override fun onRecognitionResult(text: String?, confidence: Float, isFinal: Boolean) {
        if (text.isNullOrBlank() || isDisposed.get()) return

        Log.d(TAG, "Recognition result: '$text', confidence: $confidence, final: $isFinal")

        if (isFinal) {
            // Route final results to ActionCoordinator
            processRecognizedCommand(text)
        } else {
            // Handle partial results (optional UI feedback)
            onPartialResult(text)
        }
    }

    override fun onError(errorCode: Int, message: String?) {
        Log.e(TAG, "Recognition error: code=$errorCode, message=$message")
        currentState.set(STATE_ERROR)

        // Handle different error types
        when (errorCode) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_NETWORK -> {
                // Network errors - could retry
            }
            SpeechRecognizer.ERROR_NO_MATCH -> {
                // No speech detected - normal condition
            }
        }
    }

    override fun onStateChanged(state: Int, message: String?) {
        Log.d(TAG, "State changed: $state, message: $message")
        currentState.set(state)
    }

    override fun onPartialResult(partialText: String?) {
        // Display partial text in UI
    }
}
```

#### Service Connection Implementation

```kotlin
private val serviceConnection = object : ServiceConnection {

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.d(TAG, "Service connected: $name")

        recognitionService = IVoiceRecognitionService.Stub.asInterface(service)
        isConnected.set(true)
        isBinding.set(false)
        reconnectionAttempts.set(0)

        // Register callback
        recognitionService?.registerCallback(recognitionCallback)

        // Process any pending commands
        processPendingCommands()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.w(TAG, "Service disconnected: $name")

        recognitionService = null
        isConnected.set(false)
        currentState.set(STATE_ERROR)

        // Schedule reconnection
        if (!isDisposed.get()) {
            scheduleReconnection()
        }
    }

    override fun onBindingDied(name: ComponentName?) {
        Log.e(TAG, "Service binding died: $name")

        recognitionService = null
        isConnected.set(false)

        // Immediate reconnection attempt
        if (!isDisposed.get()) {
            scheduleReconnection()
        }
    }
}
```

#### Reconnection Strategy

```kotlin
private fun scheduleReconnection() {
    val attempts = reconnectionAttempts.incrementAndGet()
    if (attempts > MAX_RECONNECTION_ATTEMPTS) {
        Log.e(TAG, "Max reconnection attempts reached")
        return
    }

    Log.w(TAG, "Scheduling reconnection attempt $attempts/$MAX_RECONNECTION_ATTEMPTS")

    binderScope.launch {
        // Exponential backoff
        delay(RECONNECTION_DELAY_MS * attempts)

        if (!isDisposed.get() && !isConnected.get()) {
            context?.let { ctx ->
                Log.d(TAG, "Attempting reconnection...")
                connect(ctx)
            }
        }
    }
}
```

#### Command Processing Flow

```kotlin
private fun processRecognizedCommand(command: String) {
    Log.d(TAG, "Processing recognized command: '$command'")

    binderScope.launch {
        try {
            // Route to ActionCoordinator
            val handled = actionCoordinator.processCommand(command)

            if (handled) {
                Log.d(TAG, "Command handled: '$command'")
            } else {
                Log.w(TAG, "Command not handled: '$command'")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: '$command'", e)
        }
    }
}
```

#### Public Methods

```kotlin
// Connection Management
fun connect(context: Context): Boolean
fun disconnect()

// Voice Recognition Control
fun startListening(engine: String, language: String): Boolean
fun stopListening(): Boolean

// State Queries
fun isConnected(): Boolean
fun isRecognizing(): Boolean
fun getCurrentState(): Int

// Engine Information
fun getAvailableEngines(): List<String>
fun getServiceStatus(): String

// Lifecycle
fun dispose()

// Debug
fun getDebugInfo(): String
```

#### Use Cases

**1. Connect to Service**
```kotlin
val binder = VoiceRecognitionBinder(actionCoordinator)

// Connect
if (binder.connect(context)) {
    Log.d(TAG, "Connecting to service...")
} else {
    Log.e(TAG, "Failed to bind to service")
}

// Connection result will be notified via ServiceConnection callbacks
```

**2. Start Recognition**
```kotlin
// Check connection first
if (!binder.isConnected()) {
    Log.w(TAG, "Service not connected")
    return
}

// Start listening
if (binder.startListening("vivoka", "en-US")) {
    Log.d(TAG, "Recognition started")
} else {
    Log.e(TAG, "Failed to start recognition")
}
```

**3. Handle Service Disconnection**
```kotlin
// Commands are queued automatically when service disconnects
binder.startListening("vivoka", "en-US")
// If service is disconnected, this command is queued

// When service reconnects, queued commands are processed automatically
// See: processPendingCommands()
```

**4. Monitor State**
```kotlin
// Check current state
val state = when (binder.getCurrentState()) {
    0 -> "IDLE"
    1 -> "LISTENING"
    2 -> "PROCESSING"
    3 -> "ERROR"
    else -> "UNKNOWN"
}

// Check if recognizing
if (binder.isRecognizing()) {
    showListeningIndicator()
}
```

---

## Speech Engine Management

### SpeechEngineManager

**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/speech/SpeechEngineManager.kt`

**Purpose:** Manages speech recognition engine state and interaction with SpeechRecognition library.

#### Supported Engines

```kotlin
enum class SpeechEngine {
    ANDROID_STT,    // Google Android Speech-to-Text
    VOSK,          // Vosk offline recognition
    VIVOKA,        // Vivoka Voxos
    WHISPER,       // OpenAI Whisper
    GOOGLE_CLOUD   // Google Cloud Speech (temporarily disabled)
}
```

#### State Management

```kotlin
data class SpeechState(
    val isListening: Boolean = false,
    val selectedEngine: SpeechEngine = SpeechEngine.ANDROID_STT,
    val currentTranscript: String = "",
    val fullTranscript: String = "",
    val errorMessage: String? = null,
    val isInitialized: Boolean = false,
    val engineStatus: String = "Not initialized",
    val confidence: Float = 0f
)

// Exposed as StateFlow
val speechState: StateFlow<SpeechState>
```

#### Configuration

```kotlin
data class SpeechConfigurationData(
    val language: String = "en-US",
    val mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Float = 0.7f,
    val maxRecordingDuration: Long = 30000,
    val timeoutDuration: Long = 5000,
    val enableProfanityFilter: Boolean = false
)
```

#### Thread Safety

SpeechEngineManager implements comprehensive thread safety:

```kotlin
// Mutexes for different operations
private val engineMutex = Mutex()
private val initializationMutex = Mutex()
private val engineCleanupMutex = Mutex()
private val engineSwitchingMutex = Mutex()

// Atomic flags
private val isInitializing = AtomicBoolean(false)
private val isDestroying = AtomicBoolean(false)
private val lastInitializationAttempt = AtomicLong(0L)
```

#### Initialization with Race Condition Prevention

```kotlin
fun initializeEngine(engine: SpeechEngine) {
    engineScope.launch {
        // Check if system is being destroyed
        if (isDestroying.get()) {
            Log.w(TAG, "Cannot initialize - EngineManager is being destroyed")
            return@launch
        }

        // Prevent concurrent initialization
        if (!isInitializing.compareAndSet(false, true)) {
            Log.w(TAG, "Initialization already in progress")
            return@launch
        }

        // Prevent too frequent attempts
        val currentTime = System.currentTimeMillis()
        val lastAttempt = engineInitializationHistory[engine] ?: 0L
        if (currentTime - lastAttempt < 1000L) {
            delay(1000L - (currentTime - lastAttempt))
        }

        engineSwitchingMutex.withLock {
            initializationMutex.withLock {
                try {
                    // Enhanced cleanup
                    cleanupPreviousEngine()

                    // Create and initialize
                    val newEngine = createEngineInstance(engine)
                    val initSuccess = initializeEngineInstanceWithRetry(newEngine, engine)

                    if (initSuccess) {
                        engineMutex.withLock {
                            currentEngine = newEngine
                            lastSuccessfulEngine = engine
                        }
                        setupEngineListeners(newEngine)
                    } else {
                        handleInitializationFailure(engine)
                    }

                } finally {
                    isInitializing.set(false)
                }
            }
        }
    }
}
```

#### Engine-Specific Initialization

```kotlin
private fun createEngineInstance(engine: SpeechEngine): Any {
    return when (engine) {
        SpeechEngine.ANDROID_STT -> AndroidSTTEngine(context)
        SpeechEngine.VOSK -> VoskEngine(context)
        SpeechEngine.VIVOKA -> VivokaEngine(context)
        SpeechEngine.WHISPER -> WhisperEngine(context)
        SpeechEngine.GOOGLE_CLOUD -> {
            Log.w(TAG, "Google Cloud disabled, using Android STT fallback")
            AndroidSTTEngine(context)
        }
    }
}

private suspend fun initializeEngineInstance(engineInstance: Any, engineType: SpeechEngine): Boolean {
    val config = createConfig(engineType)

    return when (engineInstance) {
        is AndroidSTTEngine -> engineInstance.initialize(context, config)
        is VoskEngine -> engineInstance.initialize(config)
        is VivokaEngine -> engineInstance.initialize(config)
        is WhisperEngine -> engineInstance.initialize(config)
        else -> false
    }
}
```

#### Retry Logic

```kotlin
private suspend fun initializeEngineInstanceWithRetry(
    engineInstance: Any,
    engineType: SpeechEngine
): Boolean {
    val maxRetries = 2
    var lastError: Exception? = null

    repeat(maxRetries) { attempt ->
        try {
            val result = initializeEngineInstance(engineInstance, engineType)
            if (result) {
                Log.i(TAG, "${engineType.name} initialized on attempt ${attempt + 1}")
                return true
            }
        } catch (e: Exception) {
            lastError = e
            if (attempt < maxRetries - 1) {
                delay(1000L * (attempt + 1)) // Progressive delay
            }
        }
    }

    Log.e(TAG, "All initialization attempts failed", lastError)
    return false
}
```

#### Fallback Strategy

```kotlin
private suspend fun handleInitializationFailure(engine: SpeechEngine) {
    val fallbackEngine = lastSuccessfulEngine

    if (fallbackEngine != null && fallbackEngine != engine) {
        Log.i(TAG, "Falling back to ${fallbackEngine.name}")

        _speechState.value = _speechState.value.copy(
            errorMessage = "Failed to initialize ${engine.name}, falling back..."
        )

        delay(500)

        // Recursive fallback (with limit)
        if (initializationAttempts.get() < 5L) {
            initializeEngine(fallbackEngine)
        }
    }
}
```

#### Public Methods

```kotlin
// Engine Management
fun initializeEngine(engine: SpeechEngine)
fun updateConfiguration(config: SpeechConfigurationData)

// Voice Recognition
fun startListening()
fun stopListening()
fun updateCommands(commands: List<String>)

// Transcript Management
fun clearTranscript()

// Lifecycle
fun onDestroy()

// Diagnostics
fun getInitializationDiagnostics(): Map<String, Any>
```

#### Use Cases

**1. Initialize Engine**
```kotlin
val speechEngineManager = SpeechEngineManager(context)

// Observe state changes
lifecycleScope.launch {
    speechEngineManager.speechState.collect { state ->
        when {
            state.isInitialized -> {
                Log.d(TAG, "Engine ready: ${state.engineStatus}")
                enableVoiceCommands()
            }
            state.errorMessage != null -> {
                Log.e(TAG, "Engine error: ${state.errorMessage}")
                showError(state.errorMessage)
            }
        }
    }
}

// Initialize Vivoka engine
speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)
```

**2. Start/Stop Listening**
```kotlin
// Start listening
speechEngineManager.startListening()

// Observe listening state
lifecycleScope.launch {
    speechEngineManager.speechState.collect { state ->
        if (state.isListening) {
            showListeningIndicator()
        } else {
            hideListeningIndicator()
        }
    }
}

// Stop listening
speechEngineManager.stopListening()
```

**3. Handle Recognition Results**
```kotlin
// Observe transcript changes
lifecycleScope.launch {
    speechEngineManager.speechState.collect { state ->
        if (state.fullTranscript.isNotEmpty()) {
            Log.d(TAG, "Recognized: ${state.fullTranscript} (confidence: ${state.confidence})")
            processCommand(state.fullTranscript)
            speechEngineManager.clearTranscript()
        }
    }
}
```

**4. Update Dynamic Commands (Vivoka)**
```kotlin
// Update command list for Vivoka engine
val commands = listOf(
    "Go back",
    "Open Settings",
    "Scroll up",
    "Scroll down"
)

speechEngineManager.updateCommands(commands)
```

**5. Switch Engines**
```kotlin
// Switch to different engine
fun switchEngine(newEngine: SpeechEngine) {
    // Stop current listening
    speechEngineManager.stopListening()

    // Initialize new engine (auto-cleanup of old engine)
    speechEngineManager.initializeEngine(newEngine)
}
```

**6. Configuration Management**
```kotlin
// Update configuration
val config = SpeechConfigurationData(
    language = "en-US",
    mode = SpeechMode.DYNAMIC_COMMAND,
    enableVAD = true,
    confidenceThreshold = 0.8f,
    maxRecordingDuration = 60000,
    timeoutDuration = 10000,
    enableProfanityFilter = false
)

speechEngineManager.updateConfiguration(config)
```

**7. Diagnostics**
```kotlin
// Get initialization diagnostics
val diagnostics = speechEngineManager.getInitializationDiagnostics()

diagnostics.forEach { (key, value) ->
    Log.d(TAG, "$key: $value")
}

/*
Output:
total_attempts: 3
last_attempt: 1696876543210
is_initializing: false
is_destroying: false
last_successful_engine: VIVOKA
engine_history: {VIVOKA=1696876543000, VOSK=1696876520000}
current_engine: VivokaEngine
*/
```

---

## Data Flow Patterns

### Complete Recognition Flow

```
User Speaks
    │
    ↓
SpeechEngineManager
    │ (manages engine)
    ↓
Speech Engine (Vivoka/Vosk/Android/Whisper)
    │ (performs recognition)
    ↓
Recognition Result
    │
    ↓
SpeechEngineManager Listener
    │ onResult callback
    ↓
Update speechState.fullTranscript
    │
    ↓
UI Layer observes StateFlow
    │ (or service layer)
    ↓
Process Command
    │
    ↓
ActionCoordinator.processCommand()
    │
    ↓
Route to Appropriate Handler
    │
    ↓
Execute Action
```

### ViewModel → Service Flow

```
ViewModel (MainViewModel/AccessibilityViewModel)
    │
    ↓
VoiceRecognitionClient (for testing)
    │
    ↓ AIDL
VoiceRecognitionService
    │
    ↓ Callback
VoiceRecognitionClient
    │
    ↓ Update LiveData
ViewModel State
    │
    ↓ Observe
UI Layer
```

### Service → ViewModel Flow (State Updates)

```
VoiceOSService
    │
    ↓
Service State Change (enable/disable)
    │
    ↓
AccessibilityManager broadcasts
    │
    ↓
ViewModel periodic check (every 5s)
    │
    ↓ Update LiveData/StateFlow
ViewModel State
    │
    ↓ Observe
UI Layer
```

### Settings Flow

```
User Interaction (UI)
    │
    ↓
SettingsViewModel method call
    │ (e.g., updatePerformanceMode)
    ↓
Update StateFlow
    │
    ↓ Parallel
    ├─→ Update configuration object
    │   └─→ Save to SharedPreferences
    │
    └─→ UI observes StateFlow
        └─→ UI updates immediately
```

---

## Best Practices

### 1. StateFlow vs LiveData

**When to use StateFlow:**
- Modern Compose UI
- Kotlin coroutines integration
- Requires initial value
- Need Flow operators (map, filter, combine)

```kotlin
// StateFlow example
private val _commandsExecuted = MutableStateFlow(0)
val commandsExecuted: StateFlow<Int> = _commandsExecuted.asStateFlow()

// Collect in coroutine
lifecycleScope.launch {
    viewModel.commandsExecuted.collect { count ->
        updateUI(count)
    }
}
```

**When to use LiveData:**
- Traditional View-based UI
- Lifecycle-aware observation
- Simple value observation

```kotlin
// LiveData example
private val _serviceEnabled = MutableLiveData(false)
val serviceEnabled: LiveData<Boolean> = _serviceEnabled

// Observe with lifecycle
viewModel.serviceEnabled.observe(viewLifecycleOwner) { enabled ->
    updateUI(enabled)
}
```

### 2. ViewModel Lifecycle

```kotlin
class MyViewModel : ViewModel() {
    private val viewModelScope: CoroutineScope
        // Automatically cancelled when ViewModel is cleared

    init {
        // Initialize here
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up resources
        // viewModelScope is automatically cancelled
    }
}
```

### 3. Thread Safety in State Updates

```kotlin
// CORRECT: Update from viewModelScope
fun updateState(newValue: String) {
    viewModelScope.launch {
        _stateFlow.value = newValue
    }
}

// CORRECT: postValue for background threads (LiveData)
fun updateFromBackground(newValue: String) {
    // From background thread
    _liveData.postValue(newValue)
}

// AVOID: Direct value assignment from background threads
fun incorrect(newValue: String) {
    // DON'T DO THIS from background thread
    _stateFlow.value = newValue
}
```

### 4. Combining Multiple StateFlows

```kotlin
// Combine multiple flows
val combinedState: StateFlow<CombinedState> = combine(
    handlerStates,
    performanceMode,
    isLoading
) { handlers, mode, loading ->
    CombinedState(
        handlers = handlers,
        mode = mode,
        isLoading = loading
    )
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = CombinedState()
)
```

### 5. Error Handling Pattern

```kotlin
fun performOperation() {
    viewModelScope.launch {
        try {
            _isLoading.value = true
            _errorMessage.value = null

            // Perform operation
            val result = repository.doSomething()

            _dataState.value = result

        } catch (e: Exception) {
            Log.e(TAG, "Operation failed", e)
            _errorMessage.value = "Error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
}
```

### 6. State Machine Usage

```kotlin
// Always check state before operations
fun startNewOperation() {
    if (!stateMachine.canAcceptInput()) {
        Log.w(TAG, "Cannot start - machine is busy")
        return
    }

    if (stateMachine.processEvent(StateEvent.START_LISTENING)) {
        // Proceed with operation
    } else {
        Log.w(TAG, "Invalid state transition")
    }
}
```

### 7. Configuration Persistence

```kotlin
// Auto-save pattern in SettingsViewModel
private fun saveConfiguration() {
    viewModelScope.launch {
        try {
            _configuration.value.saveToPreferences(context)
            Log.d(TAG, "Configuration saved")
        } catch (e: Exception) {
            Log.e(TAG, "Save failed", e)
            _errorMessage.value = "Failed to save: ${e.message}"
        }
    }
}

// Every update method calls saveConfiguration()
fun updateSetting(value: Any) {
    viewModelScope.launch {
        // Update state
        _settingState.value = value

        // Update configuration
        _configuration.value = _configuration.value.copy(setting = value)

        // Auto-save
        saveConfiguration()
    }
}
```

### 8. Coroutine Scope Management

```kotlin
// ViewModel has built-in viewModelScope
class MyViewModel : ViewModel() {
    // Use viewModelScope - automatically cancelled on clear
    fun doWork() {
        viewModelScope.launch {
            // Work here
        }
    }
}

// Manager classes need custom scope
class MyManager {
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun dispose() {
        managerScope.cancel()
    }
}
```

### 9. UIState Best Practices

```kotlin
// Always check for null/empty before using
val uiState = UIState.fromAccessibilityNode(rootNode)

if (uiState.hasContent()) {
    // Use UI state
    processCommands(uiState)
}

// Query elements safely
val buttons = uiState.getElementsByType(ElementType.BUTTON)
if (buttons.isNotEmpty()) {
    // Process buttons
}

// Use helper methods
val displayElement = uiState.findElementByText("Submit")
displayElement?.let { element ->
    if (element.isInteractive()) {
        clickElement(element)
    }
}
```

### 10. Speech Engine Best Practices

```kotlin
// Initialize engine early
override fun onCreate() {
    speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)
}

// Always observe state before operations
lifecycleScope.launch {
    speechEngineManager.speechState.collect { state ->
        when {
            !state.isInitialized -> {
                // Wait for initialization
                showInitializingIndicator()
            }
            state.errorMessage != null -> {
                // Handle error
                showError(state.errorMessage)
            }
            state.isListening -> {
                // Update UI
                showListeningIndicator()
            }
        }
    }
}

// Clean up on destroy
override fun onDestroy() {
    speechEngineManager.onDestroy()
}
```

---

## Complete Usage Examples

### Example 1: Main Activity with Permissions

```kotlin
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel
        mainViewModel.initialize(this)

        // Setup observers
        setupObservers()

        // Check permissions
        mainViewModel.checkAllPermissions()
    }

    private fun setupObservers() {
        // Service status
        mainViewModel.serviceEnabled.observe(this) { enabled ->
            updateServiceStatusUI(enabled)
            if (enabled) {
                showSuccessMessage("Service enabled")
            }
        }

        // Overlay permission
        mainViewModel.overlayPermissionGranted.observe(this) { granted ->
            updatePermissionUI("overlay", granted)
            if (!granted) {
                showPermissionRequest("overlay")
            }
        }

        // Write settings permission
        mainViewModel.writeSettingsPermissionGranted.observe(this) { granted ->
            updatePermissionUI("write_settings", granted)
            if (!granted) {
                showPermissionRequest("write_settings")
            }
        }

        // Configuration
        mainViewModel.configuration.observe(this) { config ->
            displayConfiguration(config)
        }

        // Loading state
        mainViewModel.isLoading.observe(this) { loading ->
            showLoadingIndicator(loading)
        }

        // Errors
        mainViewModel.errorMessage.observe(this) { error ->
            error?.let {
                showErrorDialog(it)
                mainViewModel.clearError()
            }
        }

        // Permission summary
        val summary = mainViewModel.getPermissionSummary()
        updateProgressBar(summary.percentage)

        if (summary.allGranted) {
            navigateToMainScreen()
        }
    }

    private fun showPermissionRequest(type: String) {
        val message = when (type) {
            "overlay" -> "Overlay permission required"
            "write_settings" -> "Write settings permission required"
            else -> "Permission required"
        }

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                requestPermission(type)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPermission(type: String) {
        when (type) {
            "overlay" -> {
                val uri = mainViewModel.requestOverlayPermission()
                uri?.let {
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, it),
                        REQUEST_OVERLAY_PERMISSION
                    )
                }
            }
            "write_settings" -> {
                val uri = mainViewModel.requestWriteSettingsPermission()
                uri?.let {
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, it),
                        REQUEST_WRITE_SETTINGS
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                mainViewModel.checkOverlayPermission()
            }
            REQUEST_WRITE_SETTINGS -> {
                mainViewModel.checkWriteSettingsPermission()
            }
        }
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_WRITE_SETTINGS = 1002
    }
}
```

### Example 2: Settings Screen with Compose

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val performanceMode by viewModel.performanceMode.collectAsState()
    val handlerStates by viewModel.handlerStates.collectAsState()
    val cursorEnabled by viewModel.cursorEnabled.collectAsState()
    val cursorSize by viewModel.cursorSize.collectAsState()
    val cacheEnabled by viewModel.cacheEnabled.collectAsState()
    val maxCacheSize by viewModel.maxCacheSize.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Refresh, "Reset")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Performance Mode Section
                item {
                    SettingsSection("Performance Mode") {
                        PerformanceModeSelector(
                            currentMode = performanceMode,
                            onModeSelected = { viewModel.updatePerformanceMode(it) }
                        )
                    }
                }

                // Handlers Section
                item {
                    SettingsSection("Command Handlers") {
                        viewModel.getAllHandlerDefinitions().forEach { handler ->
                            HandlerToggle(
                                handler = handler,
                                enabled = handlerStates[handler.id] ?: false,
                                onToggle = { enabled ->
                                    viewModel.toggleHandler(handler.id, enabled)
                                }
                            )
                        }
                    }
                }

                // Cursor Section
                item {
                    SettingsSection("Cursor Settings") {
                        SwitchSetting(
                            title = "Enable Cursor",
                            checked = cursorEnabled,
                            onCheckedChange = { viewModel.updateCursorEnabled(it) }
                        )

                        if (cursorEnabled) {
                            SliderSetting(
                                title = "Cursor Size",
                                value = cursorSize,
                                valueRange = 32f..128f,
                                onValueChange = { viewModel.updateCursorSize(it) }
                            )
                        }
                    }
                }

                // Cache Section
                item {
                    SettingsSection("Cache Settings") {
                        SwitchSetting(
                            title = "Enable Caching",
                            checked = cacheEnabled,
                            onCheckedChange = { viewModel.updateCacheEnabled(it) }
                        )

                        if (cacheEnabled) {
                            SliderSetting(
                                title = "Max Cache Size",
                                value = maxCacheSize.toFloat(),
                                valueRange = 50f..200f,
                                onValueChange = { viewModel.updateMaxCacheSize(it.toInt()) }
                            )
                        }
                    }
                }
            }
        }

        // Error Snackbar
        errorMessage?.let { error ->
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
fun PerformanceModeSelector(
    currentMode: PerformanceMode,
    onModeSelected: (PerformanceMode) -> Unit
) {
    Column {
        PerformanceMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeSelected(mode) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = mode == currentMode,
                    onClick = { onModeSelected(mode) }
                )

                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = mode.description,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun HandlerToggle(
    handler: HandlerInfo,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = handler.name,
                style = MaterialTheme.typography.body1
            )
            Text(
                text = handler.description,
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
            if (handler.isCore) {
                Text(
                    text = "Core handler - always enabled",
                    style = MaterialTheme.typography.caption,
                    color = Color.Blue
                )
            }
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            enabled = !handler.isCore
        )
    }
}
```

### Example 3: Service Integration

```kotlin
class VoiceOSService : AccessibilityService() {

    private lateinit var actionCoordinator: ActionCoordinator
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager
    private lateinit var speechEngineManager: SpeechEngineManager
    private val dialogStateMachine = DialogStateMachine()

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d(TAG, "VoiceOSService connected")

        // Initialize ActionCoordinator
        actionCoordinator = ActionCoordinator(this)
        actionCoordinator.initialize()

        // Initialize VoiceRecognitionManager
        voiceRecognitionManager = VoiceRecognitionManager(actionCoordinator)
        voiceRecognitionManager.initialize(this)

        // Initialize SpeechEngineManager
        speechEngineManager = SpeechEngineManager(this)

        // Setup state machine
        setupStateMachine()

        // Observe speech state
        observeSpeechState()

        // Initialize default engine
        speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)
    }

    private fun setupStateMachine() {
        dialogStateMachine.addStateListener("service") { from, to ->
            Log.d(TAG, "Dialog state: $from -> $to")

            when (to) {
                DialogStateMachine.DialogState.LISTENING -> {
                    // Start listening
                    startVoiceRecognition()
                }
                DialogStateMachine.DialogState.IDLE -> {
                    // Stop listening
                    stopVoiceRecognition()
                }
                else -> {
                    // Other states
                }
            }
        }
    }

    private fun observeSpeechState() {
        serviceScope.launch {
            speechEngineManager.speechState.collect { state ->
                when {
                    state.isInitialized && !state.isListening -> {
                        Log.d(TAG, "Engine ready: ${state.engineStatus}")
                    }

                    state.fullTranscript.isNotEmpty() -> {
                        // Process recognized command
                        processVoiceCommand(state.fullTranscript, state.confidence)
                        speechEngineManager.clearTranscript()
                    }

                    state.errorMessage != null -> {
                        Log.e(TAG, "Speech error: ${state.errorMessage}")
                        dialogStateMachine.processEvent(
                            DialogStateMachine.StateEvent.ERROR_OCCURRED
                        )
                    }
                }
            }
        }
    }

    private fun startVoiceRecognition() {
        if (speechEngineManager.speechState.value.isInitialized) {
            speechEngineManager.startListening()
        } else {
            Log.w(TAG, "Speech engine not initialized")
        }
    }

    private fun stopVoiceRecognition() {
        speechEngineManager.stopListening()
    }

    private fun processVoiceCommand(text: String, confidence: Float) {
        Log.d(TAG, "Processing command: '$text' (confidence: $confidence)")

        // Update dialog state
        dialogStateMachine.processEvent(DialogStateMachine.StateEvent.INPUT_RECEIVED)

        // Process command
        serviceScope.launch {
            try {
                val handled = actionCoordinator.processCommand(text)

                if (handled) {
                    Log.d(TAG, "Command handled successfully")

                    // Provide feedback
                    dialogStateMachine.processEvent(
                        DialogStateMachine.StateEvent.PROCESSING_DONE
                    )

                    // Complete response
                    delay(1000)
                    dialogStateMachine.processEvent(
                        DialogStateMachine.StateEvent.RESPONSE_COMPLETE
                    )
                } else {
                    Log.w(TAG, "Command not handled")
                    dialogStateMachine.processEvent(
                        DialogStateMachine.StateEvent.ERROR_OCCURRED
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing command", e)
                dialogStateMachine.processEvent(
                    DialogStateMachine.StateEvent.ERROR_OCCURRED
                )
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Create UIState from accessibility event
        val rootNode = rootInActiveWindow
        val uiState = UIState.fromAccessibilityNode(
            rootNode = rootNode,
            packageName = event.packageName?.toString(),
            activityName = event.className?.toString()
        )

        // Update UI context for commands
        updateUIContext(uiState)
    }

    private fun updateUIContext(uiState: UIState) {
        if (uiState.hasContent()) {
            // Update command list based on UI elements
            val elements = uiState.getClickableElements()
            val dynamicCommands = elements.mapNotNull { it.getDisplayText() }

            // Update speech engine commands
            speechEngineManager.updateCommands(dynamicCommands)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
        stopVoiceRecognition()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroying")

        // Stop recognition
        stopVoiceRecognition()

        // Dispose managers
        voiceRecognitionManager.dispose()
        speechEngineManager.onDestroy()
        actionCoordinator.dispose()

        // Cancel coroutines
        serviceScope.cancel()

        super.onDestroy()
    }

    companion object {
        private const val TAG = "VoiceOSService"
    }
}
```

### Example 4: Testing Commands with AccessibilityViewModel

```kotlin
class CommandTestActivity : AppCompatActivity() {

    private val viewModel: AccessibilityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command_test)

        viewModel.initialize(this)

        setupObservers()
        setupTestButtons()
    }

    private fun setupObservers() {
        // Test results
        lifecycleScope.launch {
            viewModel.testResults.collect { results ->
                updateTestResultsList(results)
            }
        }

        // Statistics
        lifecycleScope.launch {
            launch {
                viewModel.commandsExecuted.collect { count ->
                    updateCommandCount(count)
                }
            }

            launch {
                viewModel.successRate.collect { rate ->
                    updateSuccessRate((rate * 100).toInt())
                }
            }

            launch {
                viewModel.performanceMode.collect { mode ->
                    updatePerformanceMode(mode)
                }
            }
        }

        // Handler status
        val handlerStatuses = viewModel.getHandlerStatuses()
        displayHandlerStatuses(handlerStatuses)
    }

    private fun setupTestButtons() {
        // Test commands
        val testCommands = listOf(
            "go back",
            "go home",
            "open settings",
            "scroll up",
            "scroll down",
            "volume up",
            "volume down"
        )

        testCommands.forEach { command ->
            addTestButton(command) {
                executeTestCommand(command)
            }
        }

        // Clear button
        addClearButton {
            viewModel.clearTestResults()
        }
    }

    private fun executeTestCommand(command: String) {
        val result = viewModel.executeCommand(command)

        displayTestResult(result)
    }

    private fun displayTestResult(result: CommandTestResult) {
        val statusIcon = if (result.success) "✓" else "✗"
        val statusColor = if (result.success) Color.GREEN else Color.RED

        Log.d(TAG, "$statusIcon Command: ${result.command}")
        Log.d(TAG, "  Handler: ${result.handlerUsed}")
        Log.d(TAG, "  Time: ${result.executionTime}ms")
        Log.d(TAG, "  Result: ${result.result}")

        showSnackbar(
            message = "$statusIcon ${result.command} - ${result.handlerUsed}",
            color = statusColor
        )
    }

    private fun updateTestResultsList(results: List<CommandTestResult>) {
        // Clear list
        testResultsRecyclerView.removeAllViews()

        // Add results
        results.forEach { result ->
            addTestResultItem(
                command = result.command,
                success = result.success,
                executionTime = result.executionTime,
                handler = result.handlerUsed,
                timestamp = result.timestamp
            )
        }
    }

    companion object {
        private const val TAG = "CommandTestActivity"
    }
}
```

---

## Summary

VoiceAccessibility implements a robust MVVM architecture with:

1. **Three ViewModels**:
   - MainViewModel: Permissions and service status
   - AccessibilityViewModel: Service monitoring and command testing
   - SettingsViewModel: Comprehensive settings management

2. **State Management**:
   - UIState: UI context for voice commands
   - DialogStateMachine: Dialog state transitions
   - SpeechState: Speech recognition state

3. **Voice Recognition**:
   - VoiceRecognitionManager: Integration coordinator
   - VoiceRecognitionBinder: Service binding with auto-reconnect
   - SpeechEngineManager: Multi-engine support with thread safety

4. **Best Practices**:
   - StateFlow for reactive updates
   - Thread-safe operations with Mutex
   - Automatic persistence
   - Comprehensive error handling
   - Lifecycle-aware components

This architecture provides a solid foundation for voice-controlled accessibility with clean separation of concerns and robust state management.

---

**End of Documentation**
