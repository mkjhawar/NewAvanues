# Chapter 2: AIDL Interface Reference

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch02
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 2.1 AIDL Overview

### 2.1.1 Interface Files

| File | Purpose | Location |
|------|---------|----------|
| `IElementCaptureService.aidl` | Main service interface | JITLearning/src/main/aidl/ |
| `IAccessibilityEventListener.aidl` | Event callback interface | JITLearning/src/main/aidl/ |

### 2.1.2 Parcelable Files

| File | Purpose |
|------|---------|
| `JITState.aidl` | JIT service state |
| `ScreenChangeEvent.aidl` | Screen transition events |
| `ParcelableNodeInfo.aidl` | Element tree data |
| `ExplorationCommand.aidl` | Exploration control |

---

## 2.2 IElementCaptureService

### 2.2.1 Interface Definition

```aidl
// IElementCaptureService.aidl
package com.augmentalis.jitlearning;

import com.augmentalis.jitlearning.JITState;
import com.augmentalis.jitlearning.ScreenChangeEvent;
import com.augmentalis.jitlearning.ParcelableNodeInfo;
import com.augmentalis.jitlearning.ExplorationCommand;
import com.augmentalis.jitlearning.IAccessibilityEventListener;

interface IElementCaptureService {
    // State Management
    JITState queryState();
    void pauseCapture();
    void resumeCapture();
    boolean isCapturing();

    // Screen Information
    ParcelableNodeInfo getCurrentScreenInfo();
    String getCurrentPackageName();
    String getCurrentActivityName();
    String getCurrentScreenHash();

    // Event Streaming
    void registerEventListener(IAccessibilityEventListener listener);
    void unregisterEventListener(IAccessibilityEventListener listener);

    // Exploration Control
    void startExploration(in ExplorationCommand command);
    void stopExploration();
    void executeAction(String elementUuid, String actionType);

    // Safety Queries
    boolean isLoginScreen();
    List<String> getDoNotClickElements();
    int getDynamicRegionCount();
}
```

### 2.2.2 Method Reference

#### queryState()

Returns current JIT service state.

```kotlin
// Usage
val state: JITState? = jitService?.queryState()
state?.let {
    println("Screens: ${it.totalScreens}")
    println("Elements: ${it.totalElements}")
    println("Active: ${it.isActive}")
}
```

**Returns:** `JITState` containing:
- `totalScreens: Int`
- `totalElements: Int`
- `isActive: Boolean`
- `currentPackage: String`
- `lastUpdateTimestamp: Long`

---

#### pauseCapture()

Pauses JIT element capture. Use before manual exploration.

```kotlin
// Usage
jitService?.pauseCapture()
// JIT stops capturing new elements
```

**Returns:** `void`

**Side Effects:**
- JIT stops processing accessibility events
- Existing data retained
- Event listeners still receive notifications

---

#### resumeCapture()

Resumes JIT element capture after pause.

```kotlin
// Usage
jitService?.resumeCapture()
// JIT resumes capturing
```

**Returns:** `void`

---

#### isCapturing()

Checks if JIT is actively capturing.

```kotlin
// Usage
val active = jitService?.isCapturing() ?: false
```

**Returns:** `Boolean` - true if actively capturing

---

#### getCurrentScreenInfo()

Returns complete element tree for current screen.

```kotlin
// Usage
val screenInfo: ParcelableNodeInfo? = jitService?.getCurrentScreenInfo()
screenInfo?.let { root ->
    processNode(root)
    root.children.forEach { child ->
        processNode(child)
    }
}
```

**Returns:** `ParcelableNodeInfo` - root node with children

---

#### getCurrentPackageName()

Returns package name of foreground app.

```kotlin
// Usage
val pkg = jitService?.getCurrentPackageName()
// Returns: "com.example.app"
```

**Returns:** `String` - package name

---

#### getCurrentActivityName()

Returns activity class name of current screen.

```kotlin
// Usage
val activity = jitService?.getCurrentActivityName()
// Returns: "com.example.app.MainActivity"
```

**Returns:** `String` - fully qualified activity name

---

#### getCurrentScreenHash()

Returns unique hash identifying current screen.

```kotlin
// Usage
val hash = jitService?.getCurrentScreenHash()
// Returns: "a1b2c3d4e5f6"
```

**Returns:** `String` - screen fingerprint hash

---

#### registerEventListener(listener)

Registers callback for real-time events.

```kotlin
// Usage
val listener = object : IAccessibilityEventListener.Stub() {
    override fun onScreenChanged(event: ScreenChangeEvent) {
        // Handle screen change
    }
    // ... other callbacks
}
jitService?.registerEventListener(listener)
```

**Parameters:**
- `listener: IAccessibilityEventListener` - callback interface

---

#### unregisterEventListener(listener)

Removes previously registered listener.

```kotlin
// Usage
jitService?.unregisterEventListener(listener)
```

**Parameters:**
- `listener: IAccessibilityEventListener` - same instance used to register

---

#### startExploration(command)

Initiates guided exploration session.

```kotlin
// Usage
val command = ExplorationCommand(
    targetPackage = "com.example.app",
    maxDepth = 10,
    includeMenus = true,
    respectSafety = true
)
jitService?.startExploration(command)
```

**Parameters:**
- `command: ExplorationCommand` - exploration configuration

---

#### stopExploration()

Ends current exploration session.

```kotlin
// Usage
jitService?.stopExploration()
```

---

#### executeAction(elementUuid, actionType)

Performs action on specific element.

```kotlin
// Usage
jitService?.executeAction("uuid-123", "click")
jitService?.executeAction("uuid-456", "long_click")
jitService?.executeAction("uuid-789", "set_text:Hello")
```

**Parameters:**
- `elementUuid: String` - target element identifier
- `actionType: String` - action to perform (click, long_click, set_text:value, scroll:direction)

---

#### isLoginScreen()

Checks if current screen is login/auth.

```kotlin
// Usage
if (jitService?.isLoginScreen() == true) {
    // Handle login screen
}
```

**Returns:** `Boolean` - true if login detected

---

#### getDoNotClickElements()

Returns list of dangerous element IDs on current screen.

```kotlin
// Usage
val dangerous = jitService?.getDoNotClickElements() ?: emptyList()
dangerous.forEach { uuid ->
    // Skip this element
}
```

**Returns:** `List<String>` - UUIDs of dangerous elements

---

#### getDynamicRegionCount()

Returns count of detected dynamic regions.

```kotlin
// Usage
val count = jitService?.getDynamicRegionCount() ?: 0
```

**Returns:** `Int` - number of dynamic regions

---

## 2.3 IAccessibilityEventListener

### 2.3.1 Interface Definition

```aidl
// IAccessibilityEventListener.aidl
package com.augmentalis.jitlearning;

import com.augmentalis.jitlearning.ScreenChangeEvent;

interface IAccessibilityEventListener {
    void onScreenChanged(in ScreenChangeEvent event);
    void onElementAction(String elementUuid, String actionType, boolean success);
    void onScrollDetected(String direction, int distance, int newElementsCount);
    void onDynamicContentDetected(String screenHash, String regionId);
    void onMenuDiscovered(String menuId, int totalItems, int visibleItems);
    void onLoginScreenDetected(String packageName, String screenHash);
    void onError(String errorCode, String message);
}
```

### 2.3.2 Callback Reference

#### onScreenChanged(event)

Called when screen/activity changes.

```kotlin
override fun onScreenChanged(event: ScreenChangeEvent) {
    val hash = event.screenHash
    val activity = event.activityName
    val elementCount = event.elementCount
    val timestamp = event.timestamp

    addLog(LogLevel.EVENT, "SCREEN", "Changed to $activity (${elementCount} elements)")
}
```

**Event Data:**
- `screenHash: String`
- `activityName: String`
- `packageName: String`
- `elementCount: Int`
- `timestamp: Long`

---

#### onElementAction(elementUuid, actionType, success)

Called when action performed on element.

```kotlin
override fun onElementAction(elementUuid: String, actionType: String, success: Boolean) {
    val status = if (success) "OK" else "FAIL"
    addLog(LogLevel.EVENT, "ACTION", "$actionType on $elementUuid: $status")
}
```

---

#### onScrollDetected(direction, distance, newElementsCount)

Called when scroll gesture detected.

```kotlin
override fun onScrollDetected(direction: String, distance: Int, newElementsCount: Int) {
    addLog(LogLevel.EVENT, "SCROLL", "$direction ${distance}px, $newElementsCount new elements")
}
```

**Parameters:**
- `direction: String` - "up", "down", "left", "right"
- `distance: Int` - pixels scrolled
- `newElementsCount: Int` - new elements revealed

---

#### onDynamicContentDetected(screenHash, regionId)

Called when dynamic content area identified.

```kotlin
override fun onDynamicContentDetected(screenHash: String, regionId: String) {
    addLog(LogLevel.EVENT, "DYNAMIC", "Screen $screenHash, region $regionId")
}
```

---

#### onMenuDiscovered(menuId, totalItems, visibleItems)

Called when menu structure discovered.

```kotlin
override fun onMenuDiscovered(menuId: String, totalItems: Int, visibleItems: Int) {
    addLog(LogLevel.EVENT, "MENU", "$menuId: $visibleItems/$totalItems items")
}
```

---

#### onLoginScreenDetected(packageName, screenHash)

Called when login/auth screen detected.

```kotlin
override fun onLoginScreenDetected(packageName: String, screenHash: String) {
    addLog(LogLevel.WARN, "LOGIN", "Detected in $packageName")
    // Trigger safety pause
}
```

---

#### onError(errorCode, message)

Called when JIT encounters error.

```kotlin
override fun onError(errorCode: String, message: String) {
    addLog(LogLevel.ERROR, "JIT", "[$errorCode] $message")
}
```

**Error Codes:**
- `E001` - Service not available
- `E002` - Element not found
- `E003` - Action failed
- `E004` - Screen capture failed
- `E005` - Permission denied

---

## 2.4 Parcelable Data Types

### 2.4.1 JITState

```kotlin
@Parcelize
data class JITState(
    val totalScreens: Int = 0,
    val totalElements: Int = 0,
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val currentPackage: String = "",
    val lastUpdateTimestamp: Long = 0L,
    val loopsDetected: Int = 0,
    val dynamicRegions: Int = 0
) : Parcelable {
    fun toIpcString(): String {
        return "JIT:$totalScreens:$totalElements:${if(isActive) 1 else 0}:$currentPackage"
    }
}
```

### 2.4.2 ScreenChangeEvent

```kotlin
@Parcelize
data class ScreenChangeEvent(
    val screenHash: String = "",
    val activityName: String = "",
    val packageName: String = "",
    val elementCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isNewScreen: Boolean = false,
    val navigationSource: String = ""
) : Parcelable {
    fun toIpcString(): String {
        return "SCR:$screenHash:$activityName:$timestamp:$elementCount"
    }
}
```

### 2.4.3 ParcelableNodeInfo

```kotlin
@Parcelize
data class ParcelableNodeInfo(
    val nodeId: String = "",
    val className: String = "",
    val packageName: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val resourceId: String = "",
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isScrollable: Boolean = false,
    val isCheckable: Boolean = false,
    val isChecked: Boolean = false,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
    val depth: Int = 0,
    val childCount: Int = 0,
    val children: List<ParcelableNodeInfo> = emptyList()
) : Parcelable {

    fun getDisplayName(): String {
        return contentDescription.ifEmpty {
            text.ifEmpty {
                getShortClassName()
            }
        }
    }

    fun getShortClassName(): String {
        return className.substringAfterLast(".")
    }

    fun getActionFlags(): String {
        return buildString {
            if (isClickable) append("C")
            if (isLongClickable) append("L")
            if (isEditable) append("E")
            if (isScrollable) append("S")
        }
    }

    fun getBoundsString(): String {
        return "[$boundsLeft,$boundsTop,$boundsRight,$boundsBottom]"
    }
}
```

### 2.4.4 ExplorationCommand

```kotlin
@Parcelize
data class ExplorationCommand(
    val targetPackage: String = "",
    val maxDepth: Int = 10,
    val maxScreens: Int = 50,
    val includeMenus: Boolean = true,
    val includeScrollable: Boolean = true,
    val respectSafety: Boolean = true,
    val skipLoginScreens: Boolean = true,
    val timeout: Long = 300000L  // 5 minutes
) : Parcelable
```

---

## 2.5 Service Binding

### 2.5.1 Connection Implementation

```kotlin
class LearnAppDevActivity : ComponentActivity() {

    private var jitService: IElementCaptureService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            jitService = IElementCaptureService.Stub.asInterface(service)
            isBound = true

            // Register event listener
            jitService?.registerEventListener(eventListener)

            // Query initial state
            updateState()

            addLog(LogLevel.INFO, TAG, "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            jitService = null
            isBound = false
            addLog(LogLevel.WARN, TAG, "Service disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindToService()
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

    override fun onDestroy() {
        if (isBound) {
            jitService?.unregisterEventListener(eventListener)
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }
}
```

### 2.5.2 Error Handling

```kotlin
private fun safeServiceCall(block: () -> Unit) {
    try {
        if (isBound && jitService != null) {
            block()
        } else {
            addLog(LogLevel.WARN, TAG, "Service not bound")
        }
    } catch (e: RemoteException) {
        addLog(LogLevel.ERROR, TAG, "IPC failed: ${e.message}")
        handleServiceDisconnection()
    }
}

// Usage
safeServiceCall {
    val state = jitService?.queryState()
    updateUI(state)
}
```

---

## 2.6 Next Steps

Continue to [Chapter 3: Core Classes & Data Models](./03-Core-Classes.md) for complete class documentation.

---

**End of Chapter 2**
