# VoiceOnSentry - Developer Documentation

**Module**: VoiceAccessibility
**File**: VoiceOnSentry.kt
**Package**: com.augmentalis.voiceos.accessibility
**Type**: Lightweight Foreground Service (Microphone Access)
**Last Updated**: 2025-10-10 11:46:38 PDT

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Role](#architecture-role)
3. [Design Philosophy](#design-philosophy)
4. [Class Reference](#class-reference)
5. [Service Lifecycle](#service-lifecycle)
6. [State Management](#state-management)
7. [Notification System](#notification-system)
8. [Integration Guide](#integration-guide)
9. [Android 12+ Compliance](#android-12-compliance)
10. [Code Examples](#code-examples)
11. [Troubleshooting](#troubleshooting)
12. [Related Components](#related-components)

---

## Overview

**VoiceOnSentry** is a specialized lightweight foreground service designed exclusively for maintaining microphone access when VoiceAccessibility is running in the background on Android 12+ devices. It represents a hybrid architecture approach that balances functionality with resource efficiency.

### Key Characteristics

- **Conditional Activation**: Only runs when ALL conditions are met:
  - Android 12+ (API 31+)
  - App is in background
  - Voice session is active
- **Minimal Resource Footprint**: No polling, no watchdog, no health checks
- **Foreground Service Type**: `FOREGROUND_SERVICE_TYPE_MICROPHONE`
- **Lifecycle**: `LifecycleService` for lifecycle-aware components
- **Start Mode**: `START_NOT_STICKY` - does not restart if killed

### What VoiceOnSentry Is NOT

Despite its name "Sentry", this is **NOT**:
- A service monitoring/watchdog component
- A health check system
- A recovery mechanism
- An always-on background service

The name is historical - it acts as a "sentinel" for microphone access permissions, not as a system monitor.

---

## Architecture Role

### Position in VoiceAccessibility Architecture

```
VoiceOSService (AccessibilityService)
    └── VoiceOnSentry (Foreground Service)
        └── Notification (Microphone Access Indicator)
```

### Responsibilities

1. **Primary**: Maintain foreground service status to keep microphone permission active in background
2. **Secondary**: Display minimal user notification indicating voice service is active
3. **Tertiary**: Update notification state to reflect current processing status

### Dependencies

- **Parent Service**: `VoiceOSService` (controls start/stop lifecycle)
- **Android APIs**: NotificationManager, ServiceInfo (Android 12+)
- **Resources**: Notification icons, channel configuration
- **Coroutines**: Service-scoped coroutine management

### Components That Depend On This

- `VoiceOSService` - Starts/stops this service based on app state and Android version
- None others directly - this is a leaf node in the architecture

---

## Design Philosophy

### Hybrid Architecture Approach

VoiceOnSentry implements a **hybrid service strategy**:

**Before Android 12 (API < 31)**:
- Not used at all
- `VoiceOSService` can maintain microphone access without foreground service

**Android 12+ (API >= 31)**:
- Required only when app is in background
- Automatically started/stopped based on app visibility
- Minimal notification to comply with Android foreground service requirements

### Resource Optimization

**Battery Savings**:
- No background threads or polling loops
- No wake locks
- `START_NOT_STICKY` prevents unwanted restarts
- `PRIORITY_LOW` notification minimizes system overhead

**Memory Savings**:
- No persistent state storage
- Minimal class properties
- SupervisorJob + Main dispatcher (lightweight)
- No cached data or buffers

### User Experience

**Minimal Intrusion**:
- Silent notifications (no sound/vibration)
- Low priority (minimal visibility)
- No timestamp display
- Can't be swiped away (ongoing status)
- Clear, simple status messages

---

## Class Reference

### Class Signature

```kotlin
class VoiceOnSentry : LifecycleService()
```

**Inheritance**:
- Extends `LifecycleService` (provides lifecycle-aware capabilities)
- Not a regular `Service` - allows observing lifecycle events

**Visibility**: Public (must be declared in AndroidManifest.xml)

---

### Companion Object Constants

#### Logging
```kotlin
private const val TAG = "VoiceOnSentry"
```
Used for all Log statements to identify this service.

#### Notification IDs
```kotlin
private const val NOTIFICATION_ID = 9001
private const val CHANNEL_ID = "voiceos_mic_channel"
```
- `NOTIFICATION_ID`: Unique notification identifier for this service
- `CHANNEL_ID`: Notification channel for Android O+ (API 26+)

#### Actions

```kotlin
const val ACTION_START_MIC = "com.augmentalis.voiceos.START_MIC"
const val ACTION_STOP_MIC = "com.augmentalis.voiceos.STOP_MIC"
const val ACTION_UPDATE_STATE = "com.augmentalis.voiceos.UPDATE_STATE"
```

**ACTION_START_MIC**:
- Starts the foreground service
- Creates notification
- Registers microphone service type

**ACTION_STOP_MIC**:
- Stops the foreground service
- Removes notification
- Cleans up resources

**ACTION_UPDATE_STATE**:
- Updates notification to reflect current state
- Does not start/stop service
- Requires "state" extra in Intent

---

### State Enumeration

```kotlin
enum class MicState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}
```

**IDLE**: Service running but not actively processing
**LISTENING**: Actively listening for voice input
**PROCESSING**: Processing captured voice input
**ERROR**: Error state (microphone unavailable, permission denied, etc.)

---

### Instance Properties

#### notificationManager
```kotlin
private lateinit var notificationManager: NotificationManager
```
- **Type**: `NotificationManager`
- **Initialization**: `onCreate()`
- **Purpose**: Manages notification creation and updates
- **Lifecycle**: Available after `onCreate()`, valid until `onDestroy()`

#### currentState
```kotlin
private var currentState = MicState.IDLE
```
- **Type**: `MicState`
- **Default**: `IDLE`
- **Thread Safety**: Main thread only (updated via service callbacks)
- **Purpose**: Tracks current notification state

#### serviceScope
```kotlin
private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```
- **Type**: `CoroutineScope`
- **Dispatcher**: Main (UI thread)
- **Job**: SupervisorJob (failures don't cancel entire scope)
- **Lifecycle**: Created at initialization, cancelled in `onDestroy()`
- **Purpose**: Currently unused but available for future async operations

---

### Methods

#### onCreate()

```kotlin
override fun onCreate()
```

**Purpose**: Initialize the service when first created
**Thread**: Main thread
**Called By**: Android system when service is first created

**Behavior**:
1. Calls `super.onCreate()` (required for LifecycleService)
2. Logs creation event
3. Obtains `NotificationManager` system service
4. Creates notification channel (Android O+)

**Side Effects**:
- Initializes `notificationManager`
- Creates persistent notification channel

**Error Handling**: None (system service guaranteed to exist)

---

#### onStartCommand()

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
```

**Purpose**: Handle service start requests and action intents
**Thread**: Main thread
**Return**: `START_NOT_STICKY` (don't restart if killed)

**Parameters**:
- `intent`: Intent containing action and extras
- `flags`: Service start flags (not used)
- `startId`: Unique start ID (not used)

**Behavior**:
1. Calls `super.onStartCommand()` (required for LifecycleService)
2. Logs action received
3. Routes to appropriate handler based on intent action:
   - `ACTION_START_MIC` → `startMicService()`
   - `ACTION_STOP_MIC` → `stopMicService()`
   - `ACTION_UPDATE_STATE` → `updateState(intent)`
   - Unknown action → logs warning

**Return Value**: `START_NOT_STICKY`
- Service will NOT be restarted if killed by system
- Saves battery and prevents unwanted restarts
- Parent service (VoiceOSService) controls lifecycle

**Error Handling**: Logs warning for unknown actions but continues

---

#### startMicService()

```kotlin
private fun startMicService()
```

**Purpose**: Start foreground service with microphone service type
**Thread**: Main thread
**Visibility**: Private (called only from `onStartCommand()`)

**Behavior**:
1. Logs start event
2. Builds notification with `LISTENING` state
3. Calls `startForeground()` with proper parameters:
   - **Android 12+**: Uses `FOREGROUND_SERVICE_TYPE_MICROPHONE`
   - **Pre-Android 12**: Uses basic foreground service
4. Updates `currentState` to `LISTENING`

**Side Effects**:
- Service becomes foreground service
- Notification appears in status bar
- Microphone permission maintained

**Error Handling**:
- Try-catch around `startForeground()` (Android 12+)
- On failure: Logs error and calls `stopSelf()`
- Prevents zombie service state

**Android 12+ Compliance**:
```kotlin
startForeground(
    NOTIFICATION_ID,
    notification,
    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
)
```
Must declare `android.permission.FOREGROUND_SERVICE_MICROPHONE` in manifest.

---

#### stopMicService()

```kotlin
private fun stopMicService()
```

**Purpose**: Stop foreground service and clean up
**Thread**: Main thread
**Visibility**: Private

**Behavior**:
1. Logs stop event
2. Cancels `serviceScope` (stops any coroutines)
3. Stops foreground state and removes notification
4. Stops service completely

**Side Effects**:
- Notification removed from status bar
- Service transitions to stopped state
- All coroutines cancelled

**Cleanup Order**:
1. Cancel coroutines first (prevent race conditions)
2. Remove foreground status
3. Stop service

**Error Handling**: None (these operations are safe to call multiple times)

---

#### updateState()

```kotlin
private fun updateState(intent: Intent)
```

**Purpose**: Update notification to reflect current processing state
**Thread**: Main thread
**Visibility**: Private

**Parameters**:
- `intent`: Must contain "state" extra (Int, state ordinal)

**Behavior**:
1. Extracts state ordinal from intent extras
2. Converts ordinal to `MicState` enum (with null safety)
3. Checks if state actually changed
4. If changed:
   - Updates `currentState`
   - Rebuilds notification
   - Updates existing notification (doesn't create new one)
   - Logs state change

**Intent Extras**:
```kotlin
intent.putExtra("state", MicState.PROCESSING.ordinal)
```

**State Change Detection**: Only updates if state differs from `currentState`

**Error Handling**: Falls back to `IDLE` if invalid ordinal provided

---

#### buildNotification()

```kotlin
private fun buildNotification(state: MicState): Notification
```

**Purpose**: Create notification reflecting current service state
**Thread**: Main thread
**Visibility**: Private
**Return**: `Notification` object ready to display

**Parameters**:
- `state`: The `MicState` to display

**Notification Content**:

| State | Title | Icon |
|-------|-------|------|
| IDLE | "Voice ready" | ic_mic_off |
| LISTENING | "Listening..." | ic_mic_on |
| PROCESSING | "Processing..." | ic_processing |
| ERROR | "Voice error" | ic_mic_off |

**Notification Properties**:
- **Content Text**: "VoiceOS voice service active" (constant)
- **Ongoing**: `true` (can't be swiped away)
- **Silent**: `true` (no sound/vibration)
- **Priority**: `PRIORITY_LOW` (minimal intrusion)
- **Show When**: `false` (no timestamp)
- **Content Intent**: Opens app when tapped

**Pending Intent**:
- Uses app's launch intent
- `FLAG_UPDATE_CURRENT` - updates existing intent
- `FLAG_IMMUTABLE` - required for Android 12+

**Return**: Complete `Notification` object

---

#### createNotificationChannel()

```kotlin
private fun createNotificationChannel()
```

**Purpose**: Create notification channel for Android O+ (API 26+)
**Thread**: Main thread
**Visibility**: Private
**Called From**: `onCreate()` only

**Channel Configuration**:
```kotlin
NotificationChannel(
    CHANNEL_ID,
    "Voice Microphone Service",
    NotificationManager.IMPORTANCE_LOW
)
```

**Channel Properties**:
- **Name**: "Voice Microphone Service"
- **Description**: "Background microphone access for voice commands"
- **Importance**: `IMPORTANCE_LOW` (minimal intrusion)
- **Badge**: Disabled
- **Lights**: Disabled
- **Vibration**: Disabled
- **Sound**: Disabled (null)

**Version Check**: Only creates channel on Android O+ (API 26+)

**Idempotency**: Safe to call multiple times (Android handles duplicates)

---

#### onBind()

```kotlin
override fun onBind(intent: Intent): IBinder?
```

**Purpose**: Handle binding requests (not supported)
**Thread**: Main thread
**Return**: `null`

**Behavior**:
1. Calls `super.onBind()` (required for LifecycleService)
2. Returns `null` (no binding supported)

**Why No Binding**: This service is controlled entirely via start commands, not binding.

---

#### onDestroy()

```kotlin
override fun onDestroy()
```

**Purpose**: Clean up when service is destroyed
**Thread**: Main thread
**Called By**: Android system when service is being destroyed

**Behavior**:
1. Logs destruction event
2. Cancels `serviceScope` (stops any running coroutines)
3. Calls `super.onDestroy()` (required for LifecycleService)

**Cleanup**:
- Cancels all coroutines
- Releases coroutine scope
- Allows garbage collection

**Error Handling**: None (cleanup operations are safe)

---

## Service Lifecycle

### Standard Lifecycle Flow

```
1. startService(ACTION_START_MIC)
   ↓
2. onCreate() [if first start]
   ↓
3. onStartCommand(ACTION_START_MIC)
   ↓
4. startMicService()
   ↓
5. [Service running in foreground]
   ↓
6. startService(ACTION_UPDATE_STATE) [multiple times]
   ↓
7. onStartCommand(ACTION_UPDATE_STATE)
   ↓
8. updateState()
   ↓
9. [State updated, notification refreshed]
   ↓
10. stopService(ACTION_STOP_MIC)
    ↓
11. onStartCommand(ACTION_STOP_MIC)
    ↓
12. stopMicService()
    ↓
13. onDestroy()
```

### Lifecycle States

**Not Created**:
- Service class not instantiated
- No resources allocated

**Created (onCreate complete)**:
- `notificationManager` initialized
- Notification channel created
- Ready to start foreground

**Foreground (after startMicService)**:
- Notification visible
- Microphone service type registered
- Maintaining microphone access

**Destroyed (onDestroy complete)**:
- All resources released
- Coroutines cancelled
- Service stopped

---

## State Management

### State Transitions

```
IDLE ←→ LISTENING ←→ PROCESSING
  ↓         ↓            ↓
  └────── ERROR ─────────┘
```

**Valid Transitions**:
- `IDLE` → `LISTENING` (voice session starts)
- `LISTENING` → `PROCESSING` (voice detected)
- `PROCESSING` → `LISTENING` (processing complete, listening again)
- `PROCESSING` → `IDLE` (voice session paused)
- Any state → `ERROR` (error occurs)
- `ERROR` → `LISTENING` (recovery)

### State Update Flow

1. **VoiceOSService** detects state change
2. Creates Intent with `ACTION_UPDATE_STATE`
3. Adds state ordinal as extra: `intent.putExtra("state", newState.ordinal)`
4. Calls `startService(intent)`
5. **VoiceOnSentry** receives intent in `onStartCommand()`
6. Extracts and validates state
7. Updates `currentState` if different
8. Rebuilds and updates notification
9. Logs state change

### State Persistence

**No Persistence**: State is not saved to disk or shared preferences.

**Reasoning**:
- Service lifecycle is short and controlled
- State is always set by parent service on start
- No need to recover state after restart

---

## Notification System

### Channel Configuration

**Channel ID**: `voiceos_mic_channel`
**Channel Name**: "Voice Microphone Service"
**Importance**: `IMPORTANCE_LOW`

**Why Low Importance**:
- Doesn't need immediate user attention
- Background status indicator only
- Reduces notification prominence

### Notification Behavior

**Ongoing Notification**:
- Cannot be swiped away by user
- Remains visible while service is running
- Required for foreground services

**Silent Notification**:
- No sound
- No vibration
- No LED lights
- Minimal distraction

**User Interaction**:
- Tapping notification opens app
- No action buttons (not needed)
- No dismissal (ongoing status)

### Notification Update Strategy

**Update vs Replace**:
- Uses `notificationManager.notify()` with same ID
- Android updates existing notification in place
- No visual disruption (no removal/re-add)

**Update Frequency**:
- Only on actual state changes
- No periodic updates
- No unnecessary rebuilds

---

## Integration Guide

### Prerequisites

1. **Manifest Declaration**:
```xml
<service
    android:name=".accessibility.VoiceOnSentry"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="microphone">
</service>
```

2. **Permissions Required**:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

3. **Resources Required**:
- `R.drawable.ic_mic_on` - Microphone active icon
- `R.drawable.ic_mic_off` - Microphone inactive icon
- `R.drawable.ic_processing` - Processing icon

### Starting the Service

**From VoiceOSService**:
```kotlin
private fun startForegroundMicService() {
    if (foregroundServiceActive) return

    try {
        val intent = Intent(this, VoiceOnSentry::class.java).apply {
            action = VoiceOnSentry.ACTION_START_MIC
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        foregroundServiceActive = true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to start VoiceOnSentry", e)
    }
}
```

**When to Start**:
- Android 12+ only
- App goes to background
- Voice session is active
- Microphone permission granted

### Stopping the Service

**From VoiceOSService**:
```kotlin
private fun stopForegroundMicService() {
    if (!foregroundServiceActive) return

    try {
        val intent = Intent(this, VoiceOnSentry::class.java).apply {
            action = VoiceOnSentry.ACTION_STOP_MIC
        }
        stopService(intent)

        foregroundServiceActive = false
    } catch (e: Exception) {
        Log.e(TAG, "Failed to stop VoiceOnSentry", e)
    }
}
```

**When to Stop**:
- App comes to foreground
- Voice session ends
- User disables voice service
- Error state requires shutdown

### Updating Service State

**From VoiceOSService**:
```kotlin
private fun updateSentryState(newState: VoiceOnSentry.MicState) {
    if (!foregroundServiceActive) return

    val intent = Intent(this, VoiceOnSentry::class.java).apply {
        action = VoiceOnSentry.ACTION_UPDATE_STATE
        putExtra("state", newState.ordinal)
    }

    try {
        startService(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update sentry state", e)
    }
}

// Usage
updateSentryState(VoiceOnSentry.MicState.LISTENING)
updateSentryState(VoiceOnSentry.MicState.PROCESSING)
```

**When to Update**:
- Voice recognition starts listening
- Voice input detected and processing begins
- Processing completes, returning to listening
- Error occurs

---

## Android 12+ Compliance

### Foreground Service Type System

**Android 12 (API 31)** introduced foreground service types to improve transparency and user control.

**VoiceOnSentry Compliance**:
1. **Service Type**: `FOREGROUND_SERVICE_TYPE_MICROPHONE`
2. **Manifest Declaration**: `android:foregroundServiceType="microphone"`
3. **Runtime Declaration**: Passed to `startForeground()` on API 31+

### Permission Requirements

**Pre-Android 12**:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

**Android 12+**:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### Notification Requirements

**All Foreground Services (All Versions)**:
- MUST display notification
- Notification cannot be dismissed while service runs
- Notification must be visible within 5 seconds of `startForeground()`

**Android 13+ (API 33)**:
- May require `POST_NOTIFICATIONS` permission
- User can revoke notification permission (service continues but notification hidden)

### Background Start Restrictions

**Android 12+** restricts starting foreground services from background:

**Allowed Scenarios**:
1. Service started from foreground activity
2. Service started from another foreground service
3. App has ongoing notification
4. Accessibility service (VoiceAccessibility qualifies)

**VoiceOnSentry Compliance**:
- Started from `VoiceOSService` (always allowed)
- No background start restrictions apply

### Error Handling

**startForeground() Failures**:
```kotlin
try {
    startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    )
} catch (e: Exception) {
    Log.e(TAG, "Failed to start foreground service", e)
    stopSelf()
    return
}
```

**Common Failure Causes**:
- Missing `FOREGROUND_SERVICE_MICROPHONE` permission
- Missing microphone service type in manifest
- System denied foreground service start (rare with accessibility service)

---

## Code Examples

### Example 1: Complete Service Lifecycle

```kotlin
class VoiceOSService : Service() {

    private var sentryActive = false

    // Called when app goes to background on Android 12+
    private fun onAppBackgrounded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startVoiceSentry()
        }
    }

    // Called when app returns to foreground
    private fun onAppForegrounded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopVoiceSentry()
        }
    }

    private fun startVoiceSentry() {
        if (sentryActive) return

        val intent = Intent(this, VoiceOnSentry::class.java).apply {
            action = VoiceOnSentry.ACTION_START_MIC
        }

        try {
            startForegroundService(intent)
            sentryActive = true
            Log.d(TAG, "VoiceOnSentry started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VoiceOnSentry", e)
        }
    }

    private fun stopVoiceSentry() {
        if (!sentryActive) return

        val intent = Intent(this, VoiceOnSentry::class.java).apply {
            action = VoiceOnSentry.ACTION_STOP_MIC
        }

        stopService(intent)
        sentryActive = false
        Log.d(TAG, "VoiceOnSentry stopped")
    }
}
```

### Example 2: State Update During Processing

```kotlin
class VoiceOSService : Service() {

    private fun onVoiceRecognitionStarted() {
        updateSentryState(VoiceOnSentry.MicState.LISTENING)
    }

    private fun onVoiceInputDetected() {
        updateSentryState(VoiceOnSentry.MicState.PROCESSING)
    }

    private fun onVoiceProcessingComplete() {
        updateSentryState(VoiceOnSentry.MicState.LISTENING)
    }

    private fun onVoiceRecognitionError(error: Exception) {
        updateSentryState(VoiceOnSentry.MicState.ERROR)
        Log.e(TAG, "Voice recognition error", error)
    }

    private fun updateSentryState(state: VoiceOnSentry.MicState) {
        if (!sentryActive) return

        val intent = Intent(this, VoiceOnSentry::class.java).apply {
            action = VoiceOnSentry.ACTION_UPDATE_STATE
            putExtra("state", state.ordinal)
        }

        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update sentry state", e)
        }
    }
}
```

### Example 3: Version-Aware Service Control

```kotlin
class VoiceOSService : Service() {

    private val needsSentryService: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private fun manageBackgroundService(isBackground: Boolean) {
        if (!needsSentryService) {
            // Android 11 and below don't need foreground service
            return
        }

        if (isBackground && isVoiceSessionActive()) {
            startVoiceSentry()
        } else {
            stopVoiceSentry()
        }
    }

    private fun isVoiceSessionActive(): Boolean {
        // Your voice session logic
        return voiceRecognitionActive && hasPermissions()
    }
}
```

### Example 4: Error Recovery Pattern

```kotlin
class VoiceOSService : Service() {

    private var sentryStartAttempts = 0
    private val maxSentryAttempts = 3

    private fun startVoiceSentryWithRetry() {
        if (sentryStartAttempts >= maxSentryAttempts) {
            Log.e(TAG, "Max sentry start attempts exceeded")
            notifyUserOfServiceError()
            return
        }

        val intent = Intent(this, VoiceOnSentry::class.java).apply {
            action = VoiceOnSentry.ACTION_START_MIC
        }

        try {
            startForegroundService(intent)
            sentryActive = true
            sentryStartAttempts = 0 // Reset on success
            Log.d(TAG, "VoiceOnSentry started")
        } catch (e: Exception) {
            sentryStartAttempts++
            Log.e(TAG, "Failed to start VoiceOnSentry (attempt $sentryStartAttempts)", e)

            // Retry after delay
            Handler(Looper.getMainLooper()).postDelayed({
                startVoiceSentryWithRetry()
            }, 1000L * sentryStartAttempts) // Exponential backoff
        }
    }
}
```

---

## Troubleshooting

### Service Not Starting

**Symptom**: `startForegroundService()` called but service doesn't start

**Possible Causes**:
1. Missing manifest declaration
2. Missing permissions
3. Service class not found (wrong package)

**Solution**:
```kotlin
// Check manifest has:
// <service android:name=".accessibility.VoiceOnSentry" ... />

// Verify permissions:
if (checkSelfPermission(RECORD_AUDIO) != PERMISSION_GRANTED) {
    Log.e(TAG, "Missing RECORD_AUDIO permission")
}

// Full qualified name if in doubt:
val intent = Intent(this,
    com.augmentalis.voiceos.accessibility.VoiceOnSentry::class.java)
```

### ANR (Application Not Responding)

**Symptom**: "Context.startForegroundService() did not then call Service.startForeground()"

**Cause**: `startForeground()` not called within 5 seconds of service start

**VoiceOnSentry Protection**:
- `startForeground()` called immediately in `startMicService()`
- No async operations before foreground call
- No delays or blocking operations

**Solution**: Ensure no blocking code before `startForeground()` call

### Notification Not Appearing

**Symptom**: Service starts but no notification visible

**Possible Causes**:
1. Notification channel not created (Android O+)
2. Channel importance too low (IMPORTANCE_NONE)
3. User disabled notification channel

**Diagnostics**:
```kotlin
// Check channel exists:
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
    Log.d(TAG, "Channel: $channel, Importance: ${channel?.importance}")
}

// Check notification settings:
val enabled = notificationManager.areNotificationsEnabled()
Log.d(TAG, "Notifications enabled: $enabled")
```

### Android 12+ Start Failure

**Symptom**: ForegroundServiceStartNotAllowedException on Android 12+

**Cause**: Missing `FOREGROUND_SERVICE_MICROPHONE` permission or service type

**Solution**:
```xml
<!-- Add to manifest -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />

<service
    android:name=".accessibility.VoiceOnSentry"
    android:foregroundServiceType="microphone">
</service>
```

### State Updates Not Reflecting

**Symptom**: Calling `ACTION_UPDATE_STATE` but notification doesn't change

**Possible Causes**:
1. Service not running (state update requires running service)
2. Invalid state ordinal
3. Same state as current (optimization skips update)

**Diagnostics**:
```kotlin
// Log state changes:
Log.d(TAG, "Updating state from $currentState to $newState")

// Verify service running:
val isRunning = isServiceRunning(VoiceOnSentry::class.java)
Log.d(TAG, "VoiceOnSentry running: $isRunning")
```

### Memory Leaks

**Symptom**: Service instances not garbage collected

**VoiceOnSentry Protection**:
- `serviceScope.cancel()` in `onDestroy()`
- No static references to service instance
- No long-lived listeners or callbacks

**Verification**:
```kotlin
// Use LeakCanary or similar:
// implementation 'com.squareup.leakcanary:leakcanary-android:2.x'

// Service should be collected after stopService() call
```

---

## Related Components

### VoiceOSService
**File**: `VoiceOSService.kt`
**Relationship**: Parent service that controls VoiceOnSentry lifecycle
**Responsibilities**:
- Detects app background/foreground state
- Starts/stops VoiceOnSentry on Android 12+
- Updates VoiceOnSentry state during voice processing

### VoiceOSService (Parent Service)
**File**: `VoiceOSService.kt`
**Relationship**: Root accessibility service
**Responsibilities**:
- Controls VoiceOnSentry lifecycle
- Provides accessibility service context for starting VoiceOnSentry
- Ensures foreground service start restrictions don't apply

### NotificationManager
**Platform**: Android System Service
**Relationship**: System service used by VoiceOnSentry
**Responsibilities**:
- Manages notification channels
- Displays and updates notifications
- Handles user notification settings

### LifecycleService
**Platform**: AndroidX Lifecycle Library
**Relationship**: Base class for VoiceOnSentry
**Responsibilities**:
- Provides lifecycle-aware service capabilities
- Allows lifecycle observers to monitor service state
- Integrates with Lifecycle architecture components

---

## Design Patterns Used

### 1. Command Pattern
**Intent Actions** serve as commands:
- `ACTION_START_MIC` - Start command
- `ACTION_STOP_MIC` - Stop command
- `ACTION_UPDATE_STATE` - Update command

### 2. State Pattern
**MicState Enum** represents service states with associated UI (notifications)

### 3. Builder Pattern
**NotificationCompat.Builder** constructs notifications with fluent API

### 4. Template Method Pattern
**LifecycleService** provides template for lifecycle callbacks (`onCreate`, `onDestroy`, etc.)

---

## Performance Characteristics

### Resource Usage

**Memory**:
- Service instance: ~5-10 KB
- Notification: ~2-5 KB
- CoroutineScope: ~1-2 KB
- **Total**: ~10-20 KB (minimal footprint)

**CPU**:
- onCreate: ~5-10ms (one-time)
- onStartCommand: ~1-2ms per call
- Notification update: ~2-5ms
- **Idle CPU**: 0% (no background threads)

**Battery**:
- No wake locks
- No polling loops
- No network activity
- **Impact**: Negligible (< 0.1% battery/hour)

### Performance Optimizations

1. **START_NOT_STICKY**: Prevents unwanted restarts
2. **PRIORITY_LOW**: Reduces system overhead
3. **State Change Detection**: Only updates on actual changes
4. **SupervisorJob**: Prevents coroutine cascading failures
5. **No Persistence**: Eliminates disk I/O

---

## Future Enhancements

### Potential Improvements

1. **Analytics Integration**: Track service start/stop frequency
2. **Battery Monitoring**: Report battery impact to parent service
3. **State History**: Track state transitions for debugging
4. **Custom Actions**: Add notification action buttons (pause/resume)

### Not Recommended

1. **Health Checks**: Not needed - parent service controls lifecycle
2. **Automatic Restart**: Conflicts with START_NOT_STICKY design
3. **Persistent State**: Adds complexity without benefit

---

## Summary

**VoiceOnSentry** is a specialized, lightweight foreground service designed exclusively for maintaining microphone access on Android 12+ devices when VoiceAccessibility runs in the background. Despite its name suggesting monitoring capabilities, it is simply a compliance component that satisfies Android's foreground service requirements for background microphone usage.

**Key Takeaways**:
- Conditional activation (Android 12+, background, active session)
- Minimal resource footprint (no polling, no persistence)
- State-driven notification system
- Controlled entirely by parent VoiceOSService
- Android 12+ foreground service type compliance
- Simple command-based API (start/stop/update)

**Integration**: Start when app backgrounds on Android 12+, stop when foregrounded or session ends. Update state to reflect voice processing status for user awareness.

---

**Document Version**: 1.0
**Created**: 2025-10-10 11:46:38 PDT
**Author**: VOS4 Documentation System
**Status**: Complete
