# VoiceOS JIT Learning Service - Developer Manual

**Version:** 1.0
**Date:** 2025-12-11
**Audience:** Developers, Contributors
**Status:** Published

---

# Book Contents

| Chapter | Title | Page |
|---------|-------|------|
| 1 | [Introduction](#chapter-1-introduction) | 1 |
| 2 | [Architecture Overview](#chapter-2-architecture-overview) | 2 |
| 3 | [Module Structure](#chapter-3-module-structure) | 3 |
| 4 | [AIDL Interface Reference](#chapter-4-aidl-interface-reference) | 4 |
| 5 | [JustInTimeLearner API](#chapter-5-justintimelearner-api) | 5 |
| 6 | [JITLearningService Implementation](#chapter-6-jitlearningservice-implementation) | 6 |
| 7 | [LearnAppIntegration Bridge](#chapter-7-learnappintegration-bridge) | 7 |
| 8 | [Event System](#chapter-8-event-system) | 8 |
| 9 | [Database Integration](#chapter-9-database-integration) | 9 |
| 10 | [Extending the System](#chapter-10-extending-the-system) | 10 |
| A | [API Quick Reference](#appendix-a-api-quick-reference) | A |
| B | [Code Examples](#appendix-b-code-examples) | B |
| C | [Troubleshooting](#appendix-c-troubleshooting) | C |

---

# Chapter 1: Introduction

## 1.1 Purpose

The JIT (Just-In-Time) Learning Service provides passive screen learning capabilities for VoiceOS. It automatically captures UI elements as users navigate apps, generating voice commands without requiring explicit exploration.

## 1.2 Key Features

- **Passive Learning**: No user interaction required
- **Cross-Process IPC**: AIDL-based communication with LearnApp
- **Real-Time Events**: Stream screen changes to listeners
- **Element Queries**: Inspect current screen structure
- **Remote Control**: Pause/resume from LearnApp

## 1.3 Target Audience

This manual is for developers who need to:
- Integrate with the JIT learning system
- Extend LearnApp functionality
- Debug JIT-related issues
- Understand the VoiceOS learning architecture

## 1.4 Prerequisites

- Kotlin proficiency
- Android Service/AIDL knowledge
- Familiarity with accessibility services
- Understanding of coroutines

---

# Chapter 2: Architecture Overview

## 2.1 System Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        VoiceOSCore Process                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    VoiceOSService                        │    │
│  │                 (AccessibilityService)                   │    │
│  │                         │                                │    │
│  │                         ▼                                │    │
│  │              LearnAppIntegration                         │    │
│  │           (implements JITLearnerProvider)                │    │
│  │                         │                                │    │
│  │            ┌────────────┼────────────┐                   │    │
│  │            ▼            ▼            ▼                   │    │
│  │   JustInTimeLearner  Database   ExplorationEngine        │    │
│  │                         │                                │    │
│  └─────────────────────────┼────────────────────────────────┘    │
│                            │                                      │
│  ┌─────────────────────────┼────────────────────────────────┐    │
│  │              JITLearningService                           │    │
│  │            (Foreground Service + AIDL)                    │    │
│  │                         │                                 │    │
│  │              IElementCaptureService.Stub                  │    │
│  └─────────────────────────┼────────────────────────────────┘    │
└────────────────────────────┼─────────────────────────────────────┘
                             │
                        AIDL IPC
                             │
┌────────────────────────────┼─────────────────────────────────────┐
│                   LearnApp Process                                │
│  ┌─────────────────────────┼────────────────────────────────┐    │
│  │              LearnAppActivity                             │    │
│  │                         │                                 │    │
│  │         IElementCaptureService (Proxy)                    │    │
│  │         IAccessibilityEventListener                       │    │
│  └───────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────┘
```

## 2.2 Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| VoiceOSService | Receives accessibility events, manages lifecycle |
| LearnAppIntegration | Coordinates all LearnApp components |
| JustInTimeLearner | Passive screen capture and command generation |
| JITLearningService | AIDL service for cross-process communication |
| LearnAppActivity | UI for controlling JIT and viewing stats |

## 2.3 Data Flow

1. **Accessibility Event** → VoiceOSService
2. VoiceOSService → **LearnAppIntegration.onAccessibilityEvent()**
3. LearnAppIntegration → **JustInTimeLearner.onAccessibilityEvent()**
4. JustInTimeLearner → **Captures elements, saves to database**
5. JustInTimeLearner → **JITEventCallback.onScreenLearned()**
6. LearnAppIntegration → **JITLearningService (via JITLearnerProvider)**
7. JITLearningService → **IAccessibilityEventListener.onScreenChanged()**
8. LearnApp receives event → **Updates UI**

## 2.4 Dependency Direction

```
LearnApp ──depends on──► JITLearning (AIDL types)
VoiceOSCore ──depends on──► JITLearning (interfaces)
JITLearning ──no dependencies on──► VoiceOSCore (clean)
```

---

# Chapter 3: Module Structure

## 3.1 JITLearning Library

**Path:** `Modules/VoiceOS/libraries/JITLearning/`

```
JITLearning/
├── src/main/
│   ├── aidl/com/augmentalis/jitlearning/
│   │   ├── IElementCaptureService.aidl
│   │   └── IAccessibilityEventListener.aidl
│   └── java/com/augmentalis/jitlearning/
│       ├── JITLearningService.kt      # Main service
│       ├── JITLearnerProvider.kt      # Provider interface
│       ├── JITEventCallback.kt        # Event callback interface
│       ├── JITState.kt                # State parcelable
│       ├── ScreenChangeEvent.kt       # Event parcelable
│       ├── ParcelableNodeInfo.kt      # Node parcelable
│       ├── ExplorationCommand.kt      # Command parcelable
│       └── CommandType.kt             # Command enum
└── build.gradle.kts
```

## 3.2 VoiceOSCore (JIT Components)

**Path:** `Modules/VoiceOS/apps/VoiceOSCore/`

```
VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/
└── learnapp/
    ├── integration/
    │   └── LearnAppIntegration.kt     # Bridge, implements JITLearnerProvider
    └── jit/
        ├── JustInTimeLearner.kt       # Learning engine
        └── JitElementCapture.kt       # Element capture
```

## 3.3 LearnApp (Client)

**Path:** `Modules/VoiceOS/apps/LearnApp/`

```
LearnApp/src/main/java/com/augmentalis/learnapp/
├── LearnAppActivity.kt                # Main UI
└── service/
    └── JITServiceConnection.kt        # AIDL binding
```

---

# Chapter 4: AIDL Interface Reference

## 4.1 IElementCaptureService

The main AIDL interface for JIT service communication.

```aidl
interface IElementCaptureService {
    // State Control
    void pauseCapture();
    void resumeCapture();
    JITState queryState();
    List<String> getLearnedScreenHashes(String packageName);

    // Event Listeners
    void registerEventListener(IAccessibilityEventListener listener);
    void unregisterEventListener(IAccessibilityEventListener listener);

    // Screen/Element Queries
    ParcelableNodeInfo getCurrentScreenInfo();
    ParcelableNodeInfo getFullMenuContent(String menuNodeId);
    List<ParcelableNodeInfo> queryElements(String selector);

    // Actions
    boolean performClick(String elementUuid);
    boolean performScroll(String direction, int distance);
    boolean performAction(in ExplorationCommand command);
    boolean performBack();

    // Element Registration
    void registerElement(in ParcelableNodeInfo nodeInfo, String uuid);
    void clearRegisteredElements();
}
```

## 4.2 IAccessibilityEventListener

Callback interface for receiving real-time events.

```aidl
interface IAccessibilityEventListener {
    void onScreenChanged(in ScreenChangeEvent event);
    void onElementAction(String elementUuid, String actionType, boolean success);
    void onScrollDetected(String direction, int distance, int newElementsCount);
    void onDynamicContentDetected(String screenHash, String regionId);
    void onMenuDiscovered(String menuId, int totalItems, int visibleItems);
    void onLoginScreenDetected(String packageName, String screenHash);
}
```

## 4.3 Parcelable Types

### JITState

```kotlin
data class JITState(
    val isActive: Boolean,
    val currentPackage: String?,
    val screensLearned: Int,
    val elementsDiscovered: Int,
    val lastCaptureTime: Long
) : Parcelable
```

### ScreenChangeEvent

```kotlin
data class ScreenChangeEvent(
    val screenHash: String,
    val activityName: String,
    val packageName: String,
    val timestamp: Long,
    val elementCount: Int,
    val isNewScreen: Boolean = true,
    val previousScreenHash: String = ""
) : Parcelable
```

### ExplorationCommand

```kotlin
data class ExplorationCommand(
    val type: CommandType,
    val elementUuid: String = "",
    val text: String = "",
    val direction: ScrollDirection = ScrollDirection.DOWN,
    val distance: Int = 0
) : Parcelable
```

---

# Chapter 5: JustInTimeLearner API

## 5.1 Class Overview

```kotlin
class JustInTimeLearner(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager,
    private val repository: LearnAppRepository,
    private val voiceOSService: IVoiceOSServiceInternal? = null,
    private val learnAppCore: LearnAppCore? = null
)
```

## 5.2 Lifecycle Methods

### Initialize Element Capture

```kotlin
fun initializeElementCapture(accessibilityService: AccessibilityService)
```

Must be called after accessibility service is ready. Initializes:
- Element capture
- Screen state manager
- UUID generator

### Activate for Package

```kotlin
suspend fun activate(packageName: String)
```

Explicitly activates JIT for a package. Records consent as "SKIPPED".

### Deactivate

```kotlin
fun deactivate()
```

Stops JIT learning completely.

### Destroy

```kotlin
fun destroy()
```

Cleans up resources, clears callbacks.

## 5.3 Event Processing

### On Accessibility Event

```kotlin
fun onAccessibilityEvent(event: AccessibilityEvent)
```

Main entry point. Called for every accessibility event. Internally:
1. Checks if active and not paused
2. Filters excluded packages
3. Debounces (500ms)
4. Launches coroutine to process screen

## 5.4 Control Methods

### Pause/Resume

```kotlin
fun pause()
fun resume()
fun isPausedState(): Boolean
fun isLearningActive(): Boolean
```

Controls JIT learning state. When paused, `onAccessibilityEvent()` returns early.

## 5.5 Query Methods

### Get Stats

```kotlin
fun getStats(): JITStats

data class JITStats(
    val screensLearned: Int,
    val elementsDiscovered: Int,
    val currentPackage: String?,
    val isActive: Boolean
)
```

### Has Screen

```kotlin
suspend fun hasScreen(screenHash: String): Boolean
```

Checks if screen already captured (deduplication).

## 5.6 Event Callback

### Set Event Callback

```kotlin
fun setEventCallback(callback: JITEventCallback?)

interface JITEventCallback {
    fun onScreenLearned(packageName: String, screenHash: String, elementCount: Int)
    fun onElementDiscovered(stableId: String, vuid: String?)
    fun onLoginDetected(packageName: String, screenHash: String)
}
```

---

# Chapter 6: JITLearningService Implementation

## 6.1 Service Lifecycle

```kotlin
class JITLearningService : Service() {
    override fun onCreate() {
        // Create notification channel
        // Start as foreground service
        // Set singleton instance
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder  // IElementCaptureService.Stub
    }

    override fun onDestroy() {
        // Clear callbacks
        // Clean up resources
    }
}
```

## 6.2 Provider Pattern

The service uses `JITLearnerProvider` interface to access JustInTimeLearner without circular dependencies.

```kotlin
interface JITLearnerProvider {
    fun pauseLearning()
    fun resumeLearning()
    fun isLearningPaused(): Boolean
    fun isLearningActive(): Boolean
    fun getScreensLearnedCount(): Int
    fun getElementsDiscoveredCount(): Int
    fun getCurrentPackage(): String?
    fun getCurrentRootNode(): AccessibilityNodeInfo?
    fun hasScreen(screenHash: String): Boolean
    fun setEventCallback(callback: JITEventCallback?)
}
```

### Setting the Provider

```kotlin
fun setLearnerProvider(provider: JITLearnerProvider) {
    learnerProvider = provider
    // Wire event callback for bidirectional communication
    provider.setEventCallback(object : JITEventCallback {
        override fun onScreenLearned(...) {
            dispatchScreenChanged(event)
        }
        // ...
    })
}
```

## 6.3 AIDL Binder Implementation

```kotlin
private val binder = object : IElementCaptureService.Stub() {
    override fun pauseCapture() {
        isPaused = true
        learnerProvider?.pauseLearning()
        dispatchStateChanged()
    }

    override fun queryState(): JITState {
        val provider = learnerProvider
        return if (provider != null) {
            JITState(
                isActive = provider.isLearningActive() && !isPaused,
                currentPackage = provider.getCurrentPackage(),
                screensLearned = provider.getScreensLearnedCount(),
                elementsDiscovered = provider.getElementsDiscoveredCount(),
                lastCaptureTime = lastCaptureTime
            )
        } else {
            // Fallback state
        }
    }
    // ... other methods
}
```

## 6.4 Event Dispatch

```kotlin
private fun dispatchScreenChanged(event: ScreenChangeEvent) {
    val deadListeners = mutableListOf<IAccessibilityEventListener>()

    for (listener in eventListeners) {
        try {
            listener.onScreenChanged(event)
        } catch (e: RemoteException) {
            deadListeners.add(listener)
        }
    }

    eventListeners.removeAll(deadListeners)
}
```

---

# Chapter 7: LearnAppIntegration Bridge

## 7.1 JITLearnerProvider Implementation

LearnAppIntegration implements JITLearnerProvider to bridge JITLearningService with JustInTimeLearner.

```kotlin
class LearnAppIntegration(...) : JITLearnerProvider {

    override fun pauseLearning() {
        justInTimeLearner.pause()
    }

    override fun resumeLearning() {
        justInTimeLearner.resume()
    }

    override fun isLearningPaused(): Boolean {
        return justInTimeLearner.isPausedState()
    }

    override fun isLearningActive(): Boolean {
        return justInTimeLearner.isLearningActive()
    }

    override fun getScreensLearnedCount(): Int {
        return justInTimeLearner.getStats().screensLearned
    }

    override fun getElementsDiscoveredCount(): Int {
        return justInTimeLearner.getStats().elementsDiscovered
    }

    override fun getCurrentPackage(): String? {
        return justInTimeLearner.getStats().currentPackage
    }

    override fun getCurrentRootNode(): AccessibilityNodeInfo? {
        return accessibilityService.rootInActiveWindow
    }

    override fun hasScreen(screenHash: String): Boolean {
        return runBlocking { justInTimeLearner.hasScreen(screenHash) }
    }

    override fun setEventCallback(callback: JITEventCallback?) {
        // Bridge callbacks between interfaces
        if (callback != null) {
            justInTimeLearner.setEventCallback(object : JustInTimeLearner.JITEventCallback {
                override fun onScreenLearned(...) {
                    callback.onScreenLearned(...)
                }
                // ...
            })
        } else {
            justInTimeLearner.setEventCallback(null)
        }
    }
}
```

## 7.2 Wiring on Initialization

```kotlin
init {
    // ... other initialization

    // Wire JITLearningService to this provider
    wireJITLearningService()
}

private fun wireJITLearningService() {
    val service = JITLearningService.getInstance()
    service?.setLearnerProvider(this)
}
```

---

# Chapter 8: Event System

## 8.1 Event Flow

```
JustInTimeLearner
    │
    ▼ (JITEventCallback)
LearnAppIntegration
    │
    ▼ (JITEventCallback - bridged)
JITLearningService
    │
    ▼ (IAccessibilityEventListener)
LearnApp
```

## 8.2 Event Types

| Event | Trigger | Data |
|-------|---------|------|
| onScreenChanged | New screen captured | ScreenChangeEvent |
| onElementAction | Click/scroll performed | elementUuid, actionType, success |
| onScrollDetected | Scroll action | direction, distance, newElementsCount |
| onDynamicContentDetected | Content changed | screenHash, regionId |
| onMenuDiscovered | Menu found | menuId, totalItems, visibleItems |
| onLoginScreenDetected | Login screen | packageName, screenHash |

## 8.3 Registering Listeners (Client Side)

```kotlin
// In LearnApp
private val eventListener = object : IAccessibilityEventListener.Stub() {
    override fun onScreenChanged(event: ScreenChangeEvent) {
        runOnUiThread {
            addLog("Screen: ${event.screenHash}, Elements: ${event.elementCount}")
        }
    }

    override fun onElementAction(uuid: String, action: String, success: Boolean) {
        runOnUiThread {
            addLog("Action: $action on $uuid = $success")
        }
    }
    // ... other callbacks
}

// Register after binding
jitService?.registerEventListener(eventListener)

// Unregister before unbinding
jitService?.unregisterEventListener(eventListener)
```

---

# Chapter 9: Database Integration

## 9.1 Tables Used

| Table | Purpose |
|-------|---------|
| scraped_elements | Captured UI elements |
| learned_apps | App learning status |
| app_consent_history | Consent tracking |
| generated_commands | Voice commands |

## 9.2 Screen Deduplication

JustInTimeLearner checks if screen already captured before processing:

```kotlin
private suspend fun isScreenAlreadyCaptured(
    packageName: String,
    screenHash: String
): Boolean {
    return withContext(Dispatchers.IO) {
        val count = databaseManager.scrapedElements.countByScreenHash(
            packageName,
            screenHash
        )
        count > 0
    }
}
```

## 9.3 Element Persistence

```kotlin
// In JitElementCapture
fun persistElements(
    packageName: String,
    elements: List<JitCapturedElement>,
    screenHash: String
): Int {
    var count = 0
    elements.forEach { element ->
        databaseManager.scrapedElements.insert(
            packageName = packageName,
            screenHash = screenHash,
            // ... element properties
        )
        count++
    }
    return count
}
```

---

# Chapter 10: Extending the System

## 10.1 Adding New AIDL Methods

1. **Add to AIDL interface:**
   ```aidl
   // IElementCaptureService.aidl
   MyReturnType myNewMethod(in MyParamType param);
   ```

2. **Implement in JITLearningService:**
   ```kotlin
   override fun myNewMethod(param: MyParamType): MyReturnType {
       return learnerProvider?.myNewMethod(param) ?: defaultValue
   }
   ```

3. **Add to JITLearnerProvider if needed:**
   ```kotlin
   interface JITLearnerProvider {
       // ...existing methods
       fun myNewMethod(param: MyParamType): MyReturnType
   }
   ```

4. **Implement in LearnAppIntegration:**
   ```kotlin
   override fun myNewMethod(param: MyParamType): MyReturnType {
       return justInTimeLearner.myNewMethod(param)
   }
   ```

## 10.2 Adding New Events

1. **Add callback method to IAccessibilityEventListener:**
   ```aidl
   void onMyNewEvent(String data);
   ```

2. **Add to JITEventCallback:**
   ```kotlin
   interface JITEventCallback {
       // ...existing
       fun onMyNewEvent(data: String)
   }
   ```

3. **Dispatch from JITLearningService:**
   ```kotlin
   fun notifyMyNewEvent(data: String) {
       for (listener in eventListeners) {
           try {
               listener.onMyNewEvent(data)
           } catch (e: RemoteException) {
               eventListeners.remove(listener)
           }
       }
   }
   ```

4. **Trigger from JustInTimeLearner:**
   ```kotlin
   eventCallback?.onMyNewEvent(data)
   ```

## 10.3 Custom Element Queries

Add selector types in `findMatchingNodes()`:

```kotlin
private fun findMatchingNodes(
    root: AccessibilityNodeInfo,
    selectorType: String,
    pattern: String,
    results: MutableList<ParcelableNodeInfo>
) {
    val matches = when (selectorType) {
        "class" -> // existing
        "id" -> // existing
        "text" -> // existing
        "desc" -> // existing
        "mySelector" -> // NEW: your custom logic
            root.myProperty?.matches(pattern) == true
        else -> false
    }
    // ...
}
```

---

# Appendix A: API Quick Reference

## JustInTimeLearner

| Method | Return | Description |
|--------|--------|-------------|
| `initializeElementCapture(service)` | void | Initialize with accessibility service |
| `activate(packageName)` | void | Activate for package (suspend) |
| `deactivate()` | void | Stop learning |
| `pause()` | void | Pause learning |
| `resume()` | void | Resume learning |
| `isPausedState()` | Boolean | Check if paused |
| `isLearningActive()` | Boolean | Check if active & not paused |
| `getStats()` | JITStats | Get current statistics |
| `hasScreen(hash)` | Boolean | Check if screen captured (suspend) |
| `setEventCallback(callback)` | void | Set event callback |
| `destroy()` | void | Clean up resources |

## JITLearningService

| Method | Return | Description |
|--------|--------|-------------|
| `getInstance()` | JITLearningService? | Get singleton instance |
| `setLearnerProvider(provider)` | void | Set provider |
| `setAccessibilityService(service)` | void | Set accessibility callbacks |

## IElementCaptureService (AIDL)

| Method | Return | Description |
|--------|--------|-------------|
| `pauseCapture()` | void | Pause JIT |
| `resumeCapture()` | void | Resume JIT |
| `queryState()` | JITState | Get current state |
| `registerEventListener(listener)` | void | Register for events |
| `unregisterEventListener(listener)` | void | Unregister |
| `getCurrentScreenInfo()` | ParcelableNodeInfo? | Get current screen |
| `queryElements(selector)` | List | Query elements |
| `performClick(uuid)` | Boolean | Click element |
| `performScroll(dir, dist)` | Boolean | Scroll |
| `performAction(cmd)` | Boolean | Execute command |
| `performBack()` | Boolean | Press back |

---

# Appendix B: Code Examples

## B.1 Binding to JITLearningService

```kotlin
class LearnAppActivity : AppCompatActivity() {
    private var jitService: IElementCaptureService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            jitService = IElementCaptureService.Stub.asInterface(binder)
            onServiceReady()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            jitService = null
        }
    }

    private fun bindToService() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.augmentalis.voiceoscore",
                "com.augmentalis.jitlearning.JITLearningService"
            )
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun onServiceReady() {
        val state = jitService?.queryState()
        updateUI(state)
    }
}
```

## B.2 Listening for Events

```kotlin
private val eventListener = object : IAccessibilityEventListener.Stub() {
    override fun onScreenChanged(event: ScreenChangeEvent) {
        runOnUiThread {
            screenCountText.text = "Screens: ${event.elementCount}"
        }
    }

    override fun onElementAction(uuid: String, action: String, success: Boolean) {
        Log.d(TAG, "Action $action on $uuid: $success")
    }

    override fun onScrollDetected(dir: String, dist: Int, count: Int) {
        Log.d(TAG, "Scroll $dir: $count new elements")
    }

    override fun onDynamicContentDetected(hash: String, region: String) {
        Log.d(TAG, "Dynamic content in $region")
    }

    override fun onMenuDiscovered(id: String, total: Int, visible: Int) {
        Log.d(TAG, "Menu $id: $visible/$total items")
    }

    override fun onLoginScreenDetected(pkg: String, hash: String) {
        Log.w(TAG, "Login screen detected in $pkg")
    }
}

// Register
jitService?.registerEventListener(eventListener)

// Unregister on destroy
override fun onDestroy() {
    jitService?.unregisterEventListener(eventListener)
    super.onDestroy()
}
```

## B.3 Querying Elements

```kotlin
// Query all buttons
val buttons = jitService?.queryElements("class:Button")
buttons?.forEach { node ->
    Log.d(TAG, "Button: ${node.text}, clickable: ${node.isClickable}")
}

// Query by ID pattern
val submitButtons = jitService?.queryElements("id:*submit*")

// Query by text
val loginElements = jitService?.queryElements("text:Login")
```

---

# Appendix C: Troubleshooting

## C.1 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Service not connecting | VoiceOS not enabled | Enable accessibility service |
| Stats always 0 | Provider not wired | Restart VoiceOS service |
| Events not received | Listener not registered | Check registration logs |
| RemoteException | Process died | Re-bind to service |
| Stale screen info | Recycled nodes | Get fresh rootInActiveWindow |
| **LearnApp "Not connected"** | **Service not started/declared** | **See C.4 below** |

## C.2 Service Binding Issues (CRITICAL)

### C.2.1 Symptom: "Not connected to JIT service"

LearnApp shows red status "Not connected to JIT service" despite VoiceOS running.

**Root Causes:**
1. JITLearningService not declared in VoiceOSCore manifest
2. Service not started by VoiceOSService
3. Binding fails due to missing permission

### C.2.2 Fix Applied (2025-12-18)

**Issue**: VoiceOS-Issue-JITServiceBinding-251218-V1

**Changes Made**:

1. **Manifest Declaration** (VoiceOSCore/AndroidManifest.xml)
   ```xml
   <service
       android:name="com.augmentalis.jitlearning.JITLearningService"
       android:enabled="true"
       android:exported="true"
       android:permission="com.augmentalis.voiceos.permission.JIT_CONTROL"
       android:foregroundServiceType="dataSync">
       <intent-filter>
           <action android:name="com.augmentalis.jitlearning.ELEMENT_CAPTURE_SERVICE" />
       </intent-filter>
   </service>
   ```

2. **Service Startup** (VoiceOSService.kt)
   - Added `startJITService()` method
   - Called after LearnAppIntegration initializes
   - Binds to service and wires JITLearnerProvider

3. **Service Cleanup** (VoiceOSService.onDestroy())
   - Unbinds from service
   - Stops foreground service

### C.2.3 Verification Steps

```bash
# 1. Check service declared
adb shell dumpsys package com.augmentalis.voiceoscore | grep JITLearning

# 2. Check service running
adb shell dumpsys activity services | grep JITLearning
# Expected: State=RUNNING

# 3. Check binding logs
adb logcat -s VoiceOSService:I JITLearningService:I | grep -E "Starting JIT|connected|wired"
# Expected logs:
#   Starting JIT Learning Service...
#   ✓ JIT Learning Service started and binding initiated
#   JIT Learning Service connected via AIDL
#   ✓ JIT Learning Service provider wired successfully

# 4. Test from LearnApp
# Launch LearnApp → Check status card
# Expected: "Connected to JIT service" (green)
```

### C.2.4 Common Failures After Fix

| Issue | Check | Fix |
|-------|-------|-----|
| Service declaration missing | `dumpsys package` | Verify manifest merge |
| Service not starting | Check VoiceOS logs | Restart accessibility service |
| Binding fails | Check permissions | Verify signature match |
| Provider null | Check initialization order | Ensure LearnAppIntegration first |

## C.3 Debug Logging

```bash
# All JIT-related logs
adb logcat -s JITLearningService,JustInTimeLearner,LearnAppIntegration

# Filter by level
adb logcat *:W | grep -i jit
```

## C.3 Verifying Service State

```bash
# Check service running
adb shell dumpsys activity services com.augmentalis.jitlearning

# Check bindings
adb shell dumpsys activity services | grep -A 5 JITLearning
```

---

**Document Version History**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-11 | Claude | Initial version |
| 1.1 | 2025-12-18 | Claude | Added C.2: Service Binding Issues troubleshooting section |
