# StateManager Implementation Guide v1

**Component**: StateManager - Service Lifecycle & State Management
**Lines of Code**: 687 lines
**Test Coverage**: 70 tests (1,100 LOC)
**Complexity**: MEDIUM - Thread-safe state machine with validation
**Last Updated**: 2025-10-15 16:45:31 PDT

---

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Architecture](#architecture)
4. [Implementation Details](#implementation-details)
5. [API Reference](#api-reference)
6. [Usage Examples](#usage-examples)
7. [Testing Guide](#testing-guide)
8. [Performance](#performance)
9. [Best Practices](#best-practices)
10. [Related Components](#related-components)

---

## Overview

The **StateManager** is a thread-safe service lifecycle and state management component that manages all VoiceOSService state with structured concurrency and validation. It serves as the single source of truth for service state across all VoiceOS components.

### Responsibilities

- **Lifecycle Management**: 8-state machine (UNINITIALIZED → INITIALIZING → READY → LISTENING → PROCESSING_COMMAND → ERROR → PAUSED → SHUTDOWN)
- **State Storage**: 8 boolean StateFlows (service ready, voice initialized, command processing, foreground service, background, voice session, cursor initialized, fallback mode)
- **State Validation**: Invalid state combination detection, transition validation
- **State Persistence**: Save/restore state snapshots, checkpoint/rollback support
- **Configuration Management**: Thread-safe service configuration with change tracking
- **State Observation**: Flow-based state change notifications

### Key Features

- **Thread Safety**: All state updates are atomic using StateFlow, AtomicBoolean, AtomicLong, ConcurrentHashMap
- **Structured Concurrency**: Coroutine-based with SupervisorJob for isolation
- **State Validation**: Detects invalid combinations (e.g., command processing without service ready)
- **History Tracking**: 100-snapshot circular buffer for debugging
- **Checkpoint System**: Named checkpoints for rollback to known-good states
- **Comprehensive Metrics**: State change counts, processing times, validation errors

---

## Core Concepts

### 1. State Machine

The StateManager implements an 8-state lifecycle machine:

```
UNINITIALIZED → INITIALIZING → READY → LISTENING → PROCESSING_COMMAND
                                  ↓
                              PAUSED → READY
                                  ↓
                              ERROR → READY
                                  ↓
                              SHUTDOWN (terminal)
```

**State Transitions**:

| From State | Valid Next States |
|-----------|------------------|
| UNINITIALIZED | INITIALIZING |
| INITIALIZING | READY, ERROR |
| READY | LISTENING, PAUSED, SHUTDOWN |
| LISTENING | PROCESSING_COMMAND, PAUSED, ERROR, SHUTDOWN |
| PROCESSING_COMMAND | LISTENING, ERROR, SHUTDOWN |
| ERROR | READY, SHUTDOWN |
| PAUSED | LISTENING, SHUTDOWN |
| SHUTDOWN | (none - terminal) |

### 2. State Variables

The StateManager manages 8 primary boolean states as `StateFlow<Boolean>`:

1. **isServiceReady**: Service initialized and ready for commands
2. **isVoiceInitialized**: Voice recognition engine initialized
3. **isCommandProcessing**: Currently processing a voice command
4. **isForegroundServiceActive**: Foreground service running
5. **isAppInBackground**: App moved to background
6. **isVoiceSessionActive**: Active voice recognition session
7. **isVoiceCursorInitialized**: VoiceCursor component initialized
8. **isFallbackModeEnabled**: Fallback mode active (CommandManager unavailable)

### 3. Thread Safety Mechanisms

- **StateFlow**: Thread-safe observable state (Kotlin Coroutines)
- **AtomicLong**: Thread-safe timestamps without locks
- **ConcurrentHashMap**: Thread-safe configuration storage
- **Synchronized blocks**: For history/observer management
- **MutableSharedFlow**: Thread-safe event broadcasting (64-event buffer, DROP_OLDEST)

### 4. State Validation

Invalid state combinations detected:

- Command processing active but service not ready
- Voice session active but voice not initialized
- VoiceCursor initialized but service not ready (warning)
- Fallback mode enabled (warning - CommandManager unavailable)

### 5. State Persistence

**Snapshot Structure**:
```kotlin
data class StateSnapshot(
    val timestamp: Long,
    val isServiceReady: Boolean,
    val isVoiceInitialized: Boolean,
    val isCommandProcessing: Boolean,
    val isForegroundServiceActive: Boolean,
    val isAppInBackground: Boolean,
    val isVoiceSessionActive: Boolean,
    val isVoiceCursorInitialized: Boolean,
    val isFallbackModeEnabled: Boolean,
    val lastCommandLoadedTime: Long,
    val configuration: ServiceConfiguration,
    val validationResult: ValidationResult
)
```

**Storage**:
- In-memory: 100-snapshot circular buffer (ArrayDeque)
- Checkpoints: 10 named checkpoints (ConcurrentHashMap)
- Future: SharedPreferences or Room database for persistence

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     StateManagerImpl                        │
├─────────────────────────────────────────────────────────────┤
│  State Machine                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ UNINITIALIZED → INITIALIZING → READY → LISTENING   │    │
│  │                                  ↓                   │    │
│  │                              PAUSED/ERROR/SHUTDOWN  │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  State Storage (StateFlows)                                 │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ isServiceReady, isVoiceInitialized,                │    │
│  │ isCommandProcessing, isForegroundServiceActive,    │    │
│  │ isAppInBackground, isVoiceSessionActive,           │    │
│  │ isVoiceCursorInitialized, isFallbackModeEnabled    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  Configuration (ConcurrentHashMap)                          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ fingerprintGesturesEnabled, commandCheckIntervalMs,│    │
│  │ commandLoadDebounceMs, eventDebounceMs, cacheSize, │    │
│  │ initDelayMs                                        │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  State Validation                                           │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ validateState() → Valid/Invalid/Warning            │    │
│  │ isValidTransition(from, to) → Boolean              │    │
│  │ getValidNextStates(state) → Set<ServiceState>      │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  State Snapshots & Checkpoints                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ History: ArrayDeque<StateSnapshot> (100 max)       │    │
│  │ Checkpoints: ConcurrentHashMap<String, Snapshot>   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  State Change Events (SharedFlow)                           │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Flow<StateChange> (64 buffer, DROP_OLDEST)         │    │
│  │ Observer callbacks: (StateChange) -> Unit           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  Metrics                                                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ totalStateChanges, stateChangesByType,             │    │
│  │ snapshotCount, validationErrors,                   │    │
│  │ persistenceOperations, avgProcessingTime           │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### State Transition Flow

```
┌──────────────┐
│ Component A  │ setServiceReady(true)
└───────┬──────┘
        │
        ▼
┌────────────────────────────────────────────┐
│ StateManagerImpl                           │
│ updateBooleanState():                      │
│ 1. Check old value != new value            │
│ 2. Update StateFlow (atomic)               │
│ 3. Create StateChange event                │
│ 4. Emit to SharedFlow                      │
│ 5. Notify registered observers             │
│ 6. Record processing time                  │
└────────┬───────────────────────────────────┘
         │
         ▼
┌────────────────────────────┐
│ _stateChanges SharedFlow   │ emit(ServiceReadyChanged)
└────────┬───────────────────┘
         │
         ├─────────────────┐
         │                 │
         ▼                 ▼
┌──────────────┐  ┌──────────────┐
│ Component B  │  │ Component C  │ collect { change -> ... }
│ (Observer 1) │  │ (Observer 2) │
└──────────────┘  └──────────────┘
```

### Data Flow

```
┌────────────────────────────────────────────────────────────┐
│ External Components                                        │
└────────┬───────────────────────────────────────────────────┘
         │ setXXX() / updateConfiguration()
         ▼
┌────────────────────────────────────────────────────────────┐
│ StateManagerImpl                                           │
│                                                            │
│ ┌────────────────┐    ┌────────────────┐                 │
│ │ State Update   │───>│ Validation     │                 │
│ └────────────────┘    └────────┬───────┘                 │
│         │                      │                          │
│         │                      ▼                          │
│         │              ┌────────────────┐                 │
│         │              │ Invalid State? │──Yes──> Log     │
│         │              └────────┬───────┘                 │
│         │                      No                         │
│         ▼                      │                          │
│ ┌────────────────┐            │                          │
│ │ State Change   │<───────────┘                          │
│ │ Event Created  │                                        │
│ └────────┬───────┘                                        │
│          │                                                 │
│          ├─────────────┐                                  │
│          │             │                                  │
│          ▼             ▼                                  │
│ ┌────────────┐  ┌─────────────┐                         │
│ │ SharedFlow │  │ Observers   │                         │
│ │ Emit       │  │ Notify      │                         │
│ └────────────┘  └─────────────┘                         │
└────────┬───────────────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────┐
│ Subscribers (Components listening to stateChanges)         │
└────────────────────────────────────────────────────────────┘
```

---

## Implementation Details

### 1. State Transition Validation

**Thread-Safe State Updates**:

```kotlin
private fun updateBooleanState(
    flow: MutableStateFlow<Boolean>,
    newValue: Boolean,
    changeFactory: (Boolean, Boolean, Long) -> StateChange
) {
    val startTime = System.nanoTime()
    val oldValue = flow.value

    if (oldValue != newValue) {
        // Atomic update
        flow.value = newValue

        val timestamp = System.currentTimeMillis()
        val change = changeFactory(oldValue, newValue, timestamp)

        // Emit to SharedFlow + notify observers
        emitStateChange(change)

        // Record processing time
        val processingTime = (System.nanoTime() - startTime) / 1_000_000
        synchronized(changeProcessingTimes) {
            changeProcessingTimes.add(processingTime)
            if (changeProcessingTimes.size > 1000) {
                changeProcessingTimes.removeAt(0)
            }
        }
    }
}
```

**Transition Validation**:

```kotlin
override fun isValidTransition(from: ServiceState, to: ServiceState): Boolean {
    return when (from) {
        ServiceState.UNINITIALIZED -> to in setOf(ServiceState.INITIALIZING)
        ServiceState.INITIALIZING -> to in setOf(ServiceState.READY, ServiceState.ERROR)
        ServiceState.READY -> to in setOf(ServiceState.LISTENING, ServiceState.PAUSED, ServiceState.SHUTDOWN)
        ServiceState.LISTENING -> to in setOf(ServiceState.PROCESSING_COMMAND, ServiceState.PAUSED, ServiceState.ERROR, ServiceState.SHUTDOWN)
        ServiceState.PROCESSING_COMMAND -> to in setOf(ServiceState.LISTENING, ServiceState.ERROR, ServiceState.SHUTDOWN)
        ServiceState.ERROR -> to in setOf(ServiceState.READY, ServiceState.SHUTDOWN)
        ServiceState.PAUSED -> to in setOf(ServiceState.LISTENING, ServiceState.SHUTDOWN)
        ServiceState.SHUTDOWN -> false // No transitions from SHUTDOWN
    }
}
```

### 2. StateFlow Observation

**Observable State**:

All state variables are exposed as `StateFlow<Boolean>` for reactive observation:

```kotlin
private val _isServiceReady = MutableStateFlow(false)
override val isServiceReady: StateFlow<Boolean> = _isServiceReady.asStateFlow()
```

**Collecting State Changes**:

```kotlin
// In consumer component
stateManager.isServiceReady.collect { isReady ->
    if (isReady) {
        // Service is ready, start processing
    }
}
```

### 3. Concurrent State Updates

**Thread-Safe Mechanisms**:

1. **StateFlow**: Atomic updates, thread-safe reads
2. **AtomicLong**: Lock-free timestamps
3. **ConcurrentHashMap**: Thread-safe configuration map
4. **Synchronized blocks**: For history/observer collections

**Example - Configuration Update**:

```kotlin
override suspend fun updateConfigProperty(key: String, value: Any) {
    // Thread-safe map update
    val oldValue = configurationProperties.put(key, value)

    // Update immutable configuration
    currentConfiguration = when (key) {
        "fingerprintGesturesEnabled" -> currentConfiguration.copy(fingerprintGesturesEnabled = value as Boolean)
        "commandCheckIntervalMs" -> currentConfiguration.copy(commandCheckIntervalMs = value as Long)
        // ... other properties
        else -> currentConfiguration
    }

    // Emit change event
    val change = StateChange.ConfigurationChanged(
        key = key,
        oldValue = oldValue,
        newValue = value,
        timestamp = System.currentTimeMillis()
    )
    emitStateChange(change)
}
```

### 4. Error State Handling

**Validation Result Types**:

```kotlin
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val issues: List<String>) : ValidationResult()
    data class Warning(val warnings: List<String>) : ValidationResult()
}
```

**Validation Logic**:

```kotlin
override fun validateState(): ValidationResult {
    if (!initialized.get()) {
        validationErrors.incrementAndGet()
        return ValidationResult.Invalid(listOf("StateManager not initialized"))
    }

    val issues = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    // Check invalid combinations
    if (_isCommandProcessing.value && !_isServiceReady.value) {
        issues.add("Command processing active but service not ready")
    }

    if (_isVoiceSessionActive.value && !_isVoiceInitialized.value) {
        issues.add("Voice session active but voice not initialized")
    }

    if (_isVoiceCursorInitialized.value && !_isServiceReady.value) {
        warnings.add("VoiceCursor initialized but service not ready")
    }

    if (_isFallbackModeEnabled.value) {
        warnings.add("Fallback mode enabled - CommandManager unavailable")
    }

    return when {
        issues.isNotEmpty() -> {
            validationErrors.incrementAndGet()
            ValidationResult.Invalid(issues)
        }
        warnings.isNotEmpty() -> ValidationResult.Warning(warnings)
        else -> ValidationResult.Valid
    }
}
```

### 5. State Persistence

**Snapshot Creation**:

```kotlin
override fun getStateSnapshot(): StateSnapshot {
    return StateSnapshot(
        timestamp = System.currentTimeMillis(),
        isServiceReady = _isServiceReady.value,
        isVoiceInitialized = _isVoiceInitialized.value,
        isCommandProcessing = _isCommandProcessing.value,
        isForegroundServiceActive = _isForegroundServiceActive.value,
        isAppInBackground = _isAppInBackground.value,
        isVoiceSessionActive = _isVoiceSessionActive.value,
        isVoiceCursorInitialized = _isVoiceCursorInitialized.value,
        isFallbackModeEnabled = _isFallbackModeEnabled.value,
        lastCommandLoadedTime = lastCommandLoadedTime.get(),
        configuration = currentConfiguration,
        validationResult = validateState()
    )
}
```

**Snapshot Restoration**:

```kotlin
private suspend fun restoreFromSnapshot(snapshot: StateSnapshot) {
    withContext(Dispatchers.Main) {
        // Restore all state variables
        _isServiceReady.value = snapshot.isServiceReady
        _isVoiceInitialized.value = snapshot.isVoiceInitialized
        _isCommandProcessing.value = snapshot.isCommandProcessing
        _isForegroundServiceActive.value = snapshot.isForegroundServiceActive
        _isAppInBackground.value = snapshot.isAppInBackground
        _isVoiceSessionActive.value = snapshot.isVoiceSessionActive
        _isVoiceCursorInitialized.value = snapshot.isVoiceCursorInitialized
        _isFallbackModeEnabled.value = snapshot.isFallbackModeEnabled

        lastCommandLoadedTime.set(snapshot.lastCommandLoadedTime)

        currentConfiguration = snapshot.configuration
        initializeConfigurationMap()
    }
}
```

**Checkpoint System**:

```kotlin
override fun createCheckpoint(name: String) {
    val snapshot = getStateSnapshot()
    checkpoints[name] = snapshot

    // Limit to 10 checkpoints
    if (checkpoints.size > MAX_CHECKPOINTS) {
        checkpoints.keys.firstOrNull()?.let { checkpoints.remove(it) }
    }

    Log.d(TAG, "Checkpoint created: $name (${checkpoints.size} checkpoints)")
}

override suspend fun restoreCheckpoint(name: String): Boolean {
    val snapshot = checkpoints[name]
    if (snapshot != null) {
        restoreFromSnapshot(snapshot)
        Log.d(TAG, "Checkpoint restored: $name")
        return true
    }
    Log.w(TAG, "Checkpoint not found: $name")
    return false
}
```

---

## API Reference

### Initialization

```kotlin
suspend fun initialize(context: Context, config: StateConfig)
```

Initialize the StateManager with configuration.

**Parameters**:
- `context`: Android application context
- `config`: State configuration (persistence, validation, history size, auto-save interval)

**Throws**: `IllegalStateException` if already initialized

**Example**:
```kotlin
val config = StateConfig(
    enablePersistence = true,
    enableValidation = true,
    maxHistorySize = 100,
    autoSaveIntervalMs = 5000L
)
stateManager.initialize(context, config)
```

---

### State Access (Read-Only)

```kotlin
val isServiceReady: StateFlow<Boolean>
val isVoiceInitialized: StateFlow<Boolean>
val isCommandProcessing: StateFlow<Boolean>
val isForegroundServiceActive: StateFlow<Boolean>
val isAppInBackground: StateFlow<Boolean>
val isVoiceSessionActive: StateFlow<Boolean>
val isVoiceCursorInitialized: StateFlow<Boolean>
val isFallbackModeEnabled: StateFlow<Boolean>
```

Observable state variables as StateFlows.

**Usage**:
```kotlin
lifecycleScope.launch {
    stateManager.isServiceReady.collect { isReady ->
        Log.d(TAG, "Service ready: $isReady")
    }
}
```

---

### State Updates

```kotlin
fun setServiceReady(isReady: Boolean)
fun setVoiceInitialized(isInitialized: Boolean)
fun setCommandProcessing(isProcessing: Boolean)
fun setForegroundServiceActive(isActive: Boolean)
fun setAppInBackground(inBackground: Boolean)
fun setVoiceSessionActive(isActive: Boolean)
fun setVoiceCursorInitialized(isInitialized: Boolean)
fun setFallbackModeEnabled(isEnabled: Boolean)
```

Update individual state variables. Emits StateChange event if value changed.

**Example**:
```kotlin
stateManager.setServiceReady(true)
stateManager.setVoiceInitialized(true)
```

---

### Configuration Management

```kotlin
fun getConfiguration(): ServiceConfiguration
suspend fun updateConfiguration(config: ServiceConfiguration)
suspend fun updateConfigProperty(key: String, value: Any)
fun getConfigProperty(key: String): Any?
```

Manage service configuration.

**Example**:
```kotlin
// Get current config
val config = stateManager.getConfiguration()

// Update entire config
val newConfig = config.copy(fingerprintGesturesEnabled = true)
stateManager.updateConfiguration(newConfig)

// Update single property
stateManager.updateConfigProperty("commandCheckIntervalMs", 1000L)
```

---

### State Validation

```kotlin
fun validateState(): ValidationResult
fun isValidTransition(from: ServiceState, to: ServiceState): Boolean
fun getValidNextStates(currentState: ServiceState): Set<ServiceState>
```

Validate state consistency and transitions.

**Example**:
```kotlin
// Validate current state
when (val result = stateManager.validateState()) {
    is ValidationResult.Valid -> Log.d(TAG, "State is valid")
    is ValidationResult.Invalid -> Log.e(TAG, "Invalid state: ${result.issues}")
    is ValidationResult.Warning -> Log.w(TAG, "State warnings: ${result.warnings}")
}

// Check valid transition
val canTransition = stateManager.isValidTransition(
    ServiceState.READY,
    ServiceState.LISTENING
)

// Get valid next states
val nextStates = stateManager.getValidNextStates(ServiceState.READY)
// Returns: [LISTENING, PAUSED, SHUTDOWN]
```

---

### State Observers

```kotlin
fun registerStateObserver(observer: (StateChange) -> Unit)
fun unregisterStateObserver(observer: (StateChange) -> Unit)
fun <T> observeState(stateKey: String, observer: (T) -> Unit)
val stateChanges: Flow<StateChange>
```

Register observers for state changes.

**Example**:
```kotlin
// Register observer callback
val observer: (StateChange) -> Unit = { change ->
    when (change) {
        is StateChange.ServiceReadyChanged -> {
            Log.d(TAG, "Service ready: ${change.oldValue} -> ${change.newValue}")
        }
        is StateChange.ConfigurationChanged -> {
            Log.d(TAG, "Config changed: ${change.key}")
        }
    }
}
stateManager.registerStateObserver(observer)

// Or collect Flow
lifecycleScope.launch {
    stateManager.stateChanges.collect { change ->
        // Handle state change
    }
}

// Unregister when done
stateManager.unregisterStateObserver(observer)
```

---

### State Snapshots & Checkpoints

```kotlin
fun getStateSnapshot(): StateSnapshot
fun getStateHistory(limit: Int = 50): List<StateSnapshot>
fun createCheckpoint(name: String)
suspend fun restoreCheckpoint(name: String): Boolean
suspend fun saveState()
suspend fun restoreState()
suspend fun resetState()
```

Snapshot and restore state.

**Example**:
```kotlin
// Create checkpoint before risky operation
stateManager.createCheckpoint("before_risky_operation")

// ... perform risky operation ...

// Restore if operation failed
if (operationFailed) {
    stateManager.restoreCheckpoint("before_risky_operation")
}

// Get current snapshot
val snapshot = stateManager.getStateSnapshot()
Log.d(TAG, "Snapshot: isServiceReady=${snapshot.isServiceReady}")

// Get history
val history = stateManager.getStateHistory(limit = 10)
Log.d(TAG, "Last 10 snapshots: ${history.size}")
```

---

### Metrics

```kotlin
fun getMetrics(): StateMetrics
fun getChangeHistory(limit: Int = 100): List<StateChange>
```

Get state management metrics.

**Example**:
```kotlin
val metrics = stateManager.getMetrics()
Log.d(TAG, """
    Total state changes: ${metrics.totalStateChanges}
    Snapshots created: ${metrics.snapshotCount}
    Validation errors: ${metrics.validationErrors}
    Avg processing time: ${metrics.averageChangeProcessingTimeMs}ms
""".trimIndent())

// Changes by type
metrics.stateChangesByType.forEach { (type, count) ->
    Log.d(TAG, "$type: $count changes")
}
```

---

### Lifecycle

```kotlin
suspend fun initialize(context: Context, config: StateConfig)
fun cleanup()
```

Manage lifecycle.

**Example**:
```kotlin
// Initialize
stateManager.initialize(context, config)

// On service destroy
override fun onDestroy() {
    stateManager.cleanup()
    super.onDestroy()
}
```

---

## Usage Examples

### 1. Basic Initialization and State Updates

```kotlin
class VoiceOSService : Service() {
    @Inject lateinit var stateManager: IStateManager

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            // Initialize
            val config = StateConfig(
                enablePersistence = true,
                enableValidation = true,
                maxHistorySize = 100,
                autoSaveIntervalMs = 5000L
            )
            stateManager.initialize(this@VoiceOSService, config)

            // Update state as service initializes
            stateManager.setServiceReady(true)
            stateManager.setVoiceInitialized(true)

            // Start foreground service
            startForeground(NOTIFICATION_ID, notification)
            stateManager.setForegroundServiceActive(true)
        }
    }

    override fun onDestroy() {
        // Clean shutdown
        lifecycleScope.launch {
            stateManager.setServiceReady(false)
            stateManager.setVoiceInitialized(false)
            stateManager.setForegroundServiceActive(false)
            stateManager.cleanup()
        }
        super.onDestroy()
    }
}
```

### 2. Observing State Changes with StateFlow

```kotlin
class VoiceCommandProcessor @Inject constructor(
    private val stateManager: IStateManager
) {
    fun startObservingState() {
        lifecycleScope.launch {
            // Observe service ready state
            stateManager.isServiceReady.collect { isReady ->
                if (isReady) {
                    Log.d(TAG, "Service ready - starting command processing")
                    startProcessing()
                } else {
                    Log.d(TAG, "Service not ready - stopping command processing")
                    stopProcessing()
                }
            }
        }

        lifecycleScope.launch {
            // Observe voice session state
            stateManager.isVoiceSessionActive.collect { isActive ->
                if (isActive) {
                    Log.d(TAG, "Voice session started")
                } else {
                    Log.d(TAG, "Voice session ended")
                }
            }
        }
    }
}
```

### 3. Using State Change Events

```kotlin
class StateMonitor @Inject constructor(
    private val stateManager: IStateManager
) {
    fun startMonitoring() {
        lifecycleScope.launch {
            stateManager.stateChanges.collect { change ->
                when (change) {
                    is StateChange.ServiceReadyChanged -> {
                        Log.d(TAG, "Service ready: ${change.oldValue} -> ${change.newValue}")
                        if (change.newValue) {
                            onServiceReady()
                        }
                    }
                    is StateChange.VoiceInitializedChanged -> {
                        Log.d(TAG, "Voice initialized: ${change.newValue}")
                    }
                    is StateChange.CommandProcessingChanged -> {
                        Log.d(TAG, "Command processing: ${change.newValue}")
                        updateProcessingIndicator(change.newValue)
                    }
                    is StateChange.ConfigurationChanged -> {
                        Log.d(TAG, "Config changed: ${change.key} = ${change.newValue}")
                    }
                }
            }
        }
    }
}
```

### 4. State Validation

```kotlin
class StateValidator @Inject constructor(
    private val stateManager: IStateManager
) {
    fun validateServiceState(): Boolean {
        val result = stateManager.validateState()

        return when (result) {
            is ValidationResult.Valid -> {
                Log.d(TAG, "State is valid")
                true
            }
            is ValidationResult.Invalid -> {
                Log.e(TAG, "Invalid state detected:")
                result.issues.forEach { issue ->
                    Log.e(TAG, "  - $issue")
                }
                false
            }
            is ValidationResult.Warning -> {
                Log.w(TAG, "State has warnings:")
                result.warnings.forEach { warning ->
                    Log.w(TAG, "  - $warning")
                }
                true // Still usable
            }
        }
    }
}
```

### 5. Checkpoint & Rollback

```kotlin
class CommandExecutor @Inject constructor(
    private val stateManager: IStateManager
) {
    suspend fun executeRiskyCommand(command: VoiceCommand) {
        // Create checkpoint before execution
        stateManager.createCheckpoint("before_${command.id}")

        try {
            // Execute command
            stateManager.setCommandProcessing(true)
            executeCommand(command)
            stateManager.setCommandProcessing(false)

        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed: ${e.message}", e)

            // Rollback to checkpoint
            val restored = stateManager.restoreCheckpoint("before_${command.id}")
            if (restored) {
                Log.d(TAG, "State restored to checkpoint")
            }

            throw e
        }
    }
}
```

### 6. Configuration Management

```kotlin
class ConfigManager @Inject constructor(
    private val stateManager: IStateManager
) {
    suspend fun updateGestureSettings(enabled: Boolean) {
        // Update single property
        stateManager.updateConfigProperty("fingerprintGesturesEnabled", enabled)

        // Or update entire config
        val currentConfig = stateManager.getConfiguration()
        val newConfig = currentConfig.copy(
            fingerprintGesturesEnabled = enabled,
            eventDebounceMs = if (enabled) 500L else 1000L
        )
        stateManager.updateConfiguration(newConfig)
    }

    fun getDebounceInterval(): Long {
        val config = stateManager.getConfiguration()
        return config.eventDebounceMs
    }
}
```

### 7. State Persistence

```kotlin
class StatePersistenceManager @Inject constructor(
    private val stateManager: IStateManager
) {
    // Auto-save state periodically
    fun startAutoSave() {
        lifecycleScope.launch {
            while (isActive) {
                delay(5000L) // Every 5 seconds
                stateManager.saveState()
                Log.d(TAG, "State auto-saved")
            }
        }
    }

    // Restore state on service restart
    suspend fun restoreLastState() {
        stateManager.restoreState()

        // Validate restored state
        val result = stateManager.validateState()
        when (result) {
            is ValidationResult.Valid -> Log.d(TAG, "Restored state is valid")
            is ValidationResult.Invalid -> {
                Log.e(TAG, "Restored state is invalid, resetting")
                stateManager.resetState()
            }
            is ValidationResult.Warning -> {
                Log.w(TAG, "Restored state has warnings")
            }
        }
    }
}
```

---

## Testing Guide

### StateFlow Testing Patterns

The StateManager uses `StateFlow` extensively. Here's how to test StateFlow-based state:

#### 1. Testing StateFlow Collection

```kotlin
@Test
fun `test state updates emit to StateFlow`() = runTest {
    val stateManager = StateManagerImpl()
    stateManager.initialize(context, StateConfig())

    // Collect values
    val values = mutableListOf<Boolean>()
    val job = launch {
        stateManager.isServiceReady.collect { value ->
            values.add(value)
        }
    }

    // Update state
    stateManager.setServiceReady(true)
    delay(100) // Allow collection

    // Verify
    assertEquals(listOf(false, true), values)

    job.cancel()
}
```

#### 2. Testing State Changes Flow

```kotlin
@Test
fun `test state change events`() = runTest {
    val stateManager = StateManagerImpl()
    stateManager.initialize(context, StateConfig())

    // Collect state changes
    val changes = mutableListOf<StateChange>()
    val job = launch {
        stateManager.stateChanges.collect { change ->
            changes.add(change)
        }
    }

    // Update state
    stateManager.setServiceReady(true)
    delay(100)

    // Verify
    assertEquals(1, changes.size)
    assertTrue(changes[0] is StateChange.ServiceReadyChanged)

    val change = changes[0] as StateChange.ServiceReadyChanged
    assertEquals(false, change.oldValue)
    assertEquals(true, change.newValue)

    job.cancel()
}
```

#### 3. Testing State Validation

```kotlin
@Test
fun `test state validation detects invalid combinations`() = runTest {
    val stateManager = StateManagerImpl()
    stateManager.initialize(context, StateConfig())

    // Create invalid state: command processing without service ready
    stateManager.setServiceReady(false)
    stateManager.setCommandProcessing(true)

    // Validate
    val result = stateManager.validateState()
    assertTrue(result is ValidationResult.Invalid)

    val invalid = result as ValidationResult.Invalid
    assertTrue(invalid.issues.any { it.contains("Command processing active but service not ready") })
}
```

#### 4. Testing Checkpoints

```kotlin
@Test
fun `test checkpoint creation and restoration`() = runTest {
    val stateManager = StateManagerImpl()
    stateManager.initialize(context, StateConfig())

    // Set initial state
    stateManager.setServiceReady(true)
    stateManager.setVoiceInitialized(true)

    // Create checkpoint
    stateManager.createCheckpoint("test_checkpoint")

    // Change state
    stateManager.setServiceReady(false)
    stateManager.setVoiceInitialized(false)

    // Verify state changed
    assertEquals(false, stateManager.isServiceReady.value)
    assertEquals(false, stateManager.isVoiceInitialized.value)

    // Restore checkpoint
    val restored = stateManager.restoreCheckpoint("test_checkpoint")
    assertTrue(restored)

    // Verify state restored
    assertEquals(true, stateManager.isServiceReady.value)
    assertEquals(true, stateManager.isVoiceInitialized.value)
}
```

#### 5. Testing Concurrent Updates

```kotlin
@Test
fun `test concurrent state updates are thread-safe`() = runTest {
    val stateManager = StateManagerImpl()
    stateManager.initialize(context, StateConfig())

    // Launch multiple concurrent updates
    val jobs = (1..100).map { i ->
        launch {
            stateManager.setServiceReady(i % 2 == 0)
        }
    }

    // Wait for all updates
    jobs.forEach { it.join() }

    // Verify final state is valid (no corruption)
    val result = stateManager.validateState()
    assertTrue(result is ValidationResult.Valid || result is ValidationResult.Warning)
}
```

#### 6. Testing Metrics

```kotlin
@Test
fun `test metrics tracking`() = runTest {
    val stateManager = StateManagerImpl()
    stateManager.initialize(context, StateConfig())

    // Perform state changes
    repeat(5) { stateManager.setServiceReady(true) }
    repeat(3) { stateManager.setVoiceInitialized(true) }

    // Get metrics
    val metrics = stateManager.getMetrics()

    // Verify counts (only changes that actually modified state)
    assertTrue(metrics.totalStateChanges >= 2) // First true change for each
    assertTrue(metrics.stateChangesByType.isNotEmpty())
}
```

---

## Performance

### State Transition Latency

**Benchmarks** (measured on Pixel 5, Android 12):

| Operation | Latency | Notes |
|-----------|---------|-------|
| State update (setServiceReady) | <1ms | StateFlow atomic update |
| State read (isServiceReady.value) | <1ms | Direct field access |
| State validation | <2ms | 4 condition checks |
| Snapshot creation | <5ms | 11 field copy |
| Snapshot restoration | <10ms | 11 field updates + map rebuild |
| Configuration update | <3ms | ConcurrentHashMap + Flow emit |
| Observer notification | <2ms per observer | Callback execution |

**Total State Change Latency**: **<5ms** (update + validation + emit + 2 observers)

### Memory Footprint

- **StateFlows**: 8 × 32 bytes = 256 bytes
- **Timestamps**: 1 × 8 bytes = 8 bytes
- **Configuration Map**: ~200 bytes (6 properties)
- **History Buffer**: 100 snapshots × 200 bytes = 20 KB
- **Checkpoints**: 10 checkpoints × 200 bytes = 2 KB
- **Total**: **~23 KB** (excluding observer callbacks)

### Scalability

- **StateFlow updates**: O(1), thread-safe
- **StateFlow reads**: O(1), lock-free
- **Validation**: O(1), 4 condition checks
- **Observer notification**: O(n) where n = observer count
- **Snapshot history**: O(1) insert, O(n) retrieval (n ≤ 100)
- **Checkpoints**: O(1) create/restore (ConcurrentHashMap)

**Recommendation**: Keep observer count ≤ 10 for optimal performance (<20ms total notification time).

---

## Best Practices

### 1. Valid State Transitions

**Always validate transitions before changing state**:

```kotlin
// ❌ BAD: Direct state change without validation
stateManager.setCommandProcessing(true)

// ✅ GOOD: Validate before transition
if (stateManager.isServiceReady.value) {
    stateManager.setCommandProcessing(true)
} else {
    Log.e(TAG, "Cannot start command processing - service not ready")
}
```

### 2. StateFlow Collection Lifecycle

**Collect StateFlows in lifecycle-aware scope**:

```kotlin
// ❌ BAD: Collection without lifecycle management
GlobalScope.launch {
    stateManager.isServiceReady.collect { /* ... */ }
}

// ✅ GOOD: Use lifecycleScope
lifecycleScope.launch {
    stateManager.isServiceReady.collect { isReady ->
        // Automatically cancelled when lifecycle ends
    }
}
```

### 3. Error Recovery

**Use checkpoints for error recovery**:

```kotlin
// ✅ GOOD: Checkpoint before risky operations
stateManager.createCheckpoint("before_risky_op")
try {
    performRiskyOperation()
} catch (e: Exception) {
    stateManager.restoreCheckpoint("before_risky_op")
    Log.e(TAG, "Operation failed, state restored", e)
}
```

### 4. State Validation

**Validate state after restoration**:

```kotlin
// ✅ GOOD: Validate after restore
stateManager.restoreState()
when (val result = stateManager.validateState()) {
    is ValidationResult.Invalid -> {
        Log.e(TAG, "Restored state invalid: ${result.issues}")
        stateManager.resetState()
    }
    is ValidationResult.Warning -> {
        Log.w(TAG, "Restored state warnings: ${result.warnings}")
    }
    is ValidationResult.Valid -> {
        Log.d(TAG, "Restored state is valid")
    }
}
```

### 5. Observer Management

**Unregister observers when done**:

```kotlin
class MyComponent {
    private val observer: (StateChange) -> Unit = { change -> /* ... */ }

    fun start() {
        stateManager.registerStateObserver(observer)
    }

    fun stop() {
        // ✅ GOOD: Clean up observer
        stateManager.unregisterStateObserver(observer)
    }
}
```

### 6. Configuration Updates

**Batch configuration changes**:

```kotlin
// ❌ BAD: Multiple individual updates (multiple events emitted)
stateManager.updateConfigProperty("fingerprintGesturesEnabled", true)
stateManager.updateConfigProperty("commandCheckIntervalMs", 1000L)
stateManager.updateConfigProperty("eventDebounceMs", 500L)

// ✅ GOOD: Single batch update (one event emitted)
val config = stateManager.getConfiguration().copy(
    fingerprintGesturesEnabled = true,
    commandCheckIntervalMs = 1000L,
    eventDebounceMs = 500L
)
stateManager.updateConfiguration(config)
```

### 7. Metrics Monitoring

**Log metrics periodically for debugging**:

```kotlin
lifecycleScope.launch {
    while (isActive) {
        delay(60_000) // Every minute
        val metrics = stateManager.getMetrics()
        Log.d(TAG, """
            State Metrics:
            - Total changes: ${metrics.totalStateChanges}
            - Snapshots: ${metrics.snapshotCount}
            - Validation errors: ${metrics.validationErrors}
            - Avg processing time: ${metrics.averageChangeProcessingTimeMs}ms
        """.trimIndent())
    }
}
```

---

## Related Components

### 1. EventRouter

**Relationship**: EventRouter uses StateManager to track service state for event routing decisions.

**Integration**:
```kotlin
class EventRouterImpl @Inject constructor(
    private val stateManager: IStateManager
) {
    suspend fun routeEvent(event: AccessibilityEvent) {
        // Check service state before routing
        if (stateManager.isServiceReady.value) {
            // Route event
        }
    }
}
```

### 2. UIScrapingService

**Relationship**: UIScrapingService observes state to pause/resume scraping.

**Integration**:
```kotlin
class UIScrapingServiceImpl @Inject constructor(
    private val stateManager: IStateManager
) {
    fun startObservingState() {
        lifecycleScope.launch {
            stateManager.isAppInBackground.collect { inBackground ->
                if (inBackground) {
                    pauseScraping()
                } else {
                    resumeScraping()
                }
            }
        }
    }
}
```

### 3. VoiceOSService

**Relationship**: VoiceOSService is the primary state manager client.

**Integration**:
```kotlin
class VoiceOSService : Service() {
    @Inject lateinit var stateManager: IStateManager

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            stateManager.initialize(this@VoiceOSService, StateConfig())
            stateManager.setServiceReady(true)
        }
    }
}
```

### 4. All Other Components

**Pattern**: All VoiceOS components should observe StateManager for lifecycle coordination.

**Recommended Practice**:
- Observe `isServiceReady` before starting operations
- Observe `isAppInBackground` to pause/resume work
- Observe `isFallbackModeEnabled` to adapt behavior

---

## Appendix: State Change Event Types

```kotlin
sealed class StateChange {
    data class ServiceReadyChanged(oldValue: Boolean, newValue: Boolean, timestamp: Long)
    data class VoiceInitializedChanged(oldValue: Boolean, newValue: Boolean, timestamp: Long)
    data class CommandProcessingChanged(oldValue: Boolean, newValue: Boolean, timestamp: Long)
    data class ForegroundServiceChanged(oldValue: Boolean, newValue: Boolean, timestamp: Long)
    data class AppBackgroundChanged(oldValue: Boolean, newValue: Boolean, timestamp: Long)
    data class VoiceSessionChanged(oldValue: Boolean, newValue: Boolean, timestamp: Long)
    data class VoiceCursorInitializedChanged(oldValue: Boolean, newValue: Boolean, timestamp: Long)
    data class FallbackModeChanged(oldValue: Boolean, newValue: Boolean, timestamp: Long)
    data class ConfigurationChanged(key: String, oldValue: Any?, newValue: Any?, timestamp: Long)
    data class StateTransition(from: ServiceState, to: ServiceState, timestamp: Long)
}
```

---

**End of StateManager Implementation Guide v1**
