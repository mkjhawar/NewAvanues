# Chapter 5: Event Streaming System

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch05
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 5.1 Event Architecture

### 5.1.1 Event Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Android       │     │   VoiceOS       │     │   JIT           │
│ Accessibility   │────▶│   Core          │────▶│   Service       │
│   System        │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         │ AIDL Callback
                                                         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Log Console   │◀────│   Event         │◀────│ AvaLearnPro     │
│   Display       │     │   Processor     │     │   Listener      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### 5.1.2 Event Types

| Event | Source | Frequency | Data Size |
|-------|--------|-----------|-----------|
| Screen Changed | Window change | Medium | ~500 bytes |
| Element Action | User interaction | High | ~100 bytes |
| Scroll Detected | Gesture | Medium | ~50 bytes |
| Dynamic Content | Content change | Low | ~100 bytes |
| Menu Discovered | Menu open | Low | ~150 bytes |
| Login Detected | Auth screen | Rare | ~100 bytes |
| Error | System | Rare | ~200 bytes |

---

## 5.2 Event Listener Implementation

### 5.2.1 Listener Registration

```kotlin
class LearnAppDevActivity : ComponentActivity() {

    private val eventListener = object : IAccessibilityEventListener.Stub() {

        override fun onScreenChanged(event: ScreenChangeEvent) {
            runOnUiThread {
                addLog(
                    LogLevel.EVENT,
                    "SCREEN",
                    "Changed to ${event.activityName} (${event.elementCount} elements)"
                )
                // Update UI state
                uiState.value = uiState.value.copy(
                    currentPackage = event.packageName
                )
            }
        }

        override fun onElementAction(
            elementUuid: String,
            actionType: String,
            success: Boolean
        ) {
            runOnUiThread {
                val status = if (success) "OK" else "FAIL"
                addLog(
                    LogLevel.EVENT,
                    "ACTION",
                    "$actionType on ${elementUuid.take(8)}: $status"
                )
                if (success) {
                    uiState.value = uiState.value.copy(
                        elementsClicked = uiState.value.elementsClicked + 1
                    )
                }
            }
        }

        override fun onScrollDetected(
            direction: String,
            distance: Int,
            newElementsCount: Int
        ) {
            runOnUiThread {
                addLog(
                    LogLevel.EVENT,
                    "SCROLL",
                    "$direction ${distance}px, $newElementsCount new"
                )
            }
        }

        override fun onDynamicContentDetected(
            screenHash: String,
            regionId: String
        ) {
            runOnUiThread {
                addLog(
                    LogLevel.EVENT,
                    "DYNAMIC",
                    "Screen ${screenHash.take(8)}, region $regionId"
                )
                uiState.value = uiState.value.copy(
                    dynamicRegionsDetected = uiState.value.dynamicRegionsDetected + 1
                )
            }
        }

        override fun onMenuDiscovered(
            menuId: String,
            totalItems: Int,
            visibleItems: Int
        ) {
            runOnUiThread {
                addLog(
                    LogLevel.EVENT,
                    "MENU",
                    "$menuId: $visibleItems/$totalItems items"
                )
                uiState.value = uiState.value.copy(
                    menusDiscovered = uiState.value.menusDiscovered + 1
                )
            }
        }

        override fun onLoginScreenDetected(
            packageName: String,
            screenHash: String
        ) {
            runOnUiThread {
                addLog(
                    LogLevel.WARN,
                    "LOGIN",
                    "Detected in $packageName"
                )
                uiState.value = uiState.value.copy(
                    isOnLoginScreen = true
                )
            }
        }

        override fun onError(errorCode: String, message: String) {
            runOnUiThread {
                addLog(
                    LogLevel.ERROR,
                    "JIT",
                    "[$errorCode] $message"
                )
            }
        }
    }

    private fun bindToService() {
        // ... binding code ...
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        jitService = IElementCaptureService.Stub.asInterface(service)
        isBound = true

        // Register event listener
        try {
            jitService?.registerEventListener(eventListener)
            uiState.value = uiState.value.copy(eventStreamActive = true)
            addLog(LogLevel.INFO, TAG, "Event listener registered")
        } catch (e: RemoteException) {
            addLog(LogLevel.ERROR, TAG, "Failed to register listener: ${e.message}")
        }
    }

    override fun onDestroy() {
        if (isBound) {
            try {
                jitService?.unregisterEventListener(eventListener)
            } catch (e: RemoteException) {
                // Service may already be dead
            }
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }
}
```

### 5.2.2 Thread Safety

```kotlin
// Events arrive on Binder thread, must dispatch to main
private fun runOnUiThread(block: () -> Unit) {
    handler.post(block)
}

// Alternative: Use coroutines
private fun handleEvent(block: suspend () -> Unit) {
    lifecycleScope.launch(Dispatchers.Main) {
        block()
    }
}
```

---

## 5.3 Log Management

### 5.3.1 Log Buffer

```kotlin
class LogBuffer(private val maxSize: Int = 500) {

    private val buffer = mutableStateListOf<LogEntry>()

    fun add(entry: LogEntry) {
        if (buffer.size >= maxSize) {
            buffer.removeAt(0)  // Remove oldest
        }
        buffer.add(entry)
    }

    fun clear() {
        buffer.clear()
    }

    fun getAll(): List<LogEntry> = buffer.toList()

    fun getFiltered(level: LogLevel): List<LogEntry> {
        return buffer.filter { it.level == level }
    }

    fun search(query: String): List<LogEntry> {
        return buffer.filter {
            it.message.contains(query, ignoreCase = true) ||
            it.tag.contains(query, ignoreCase = true)
        }
    }

    val size: Int get() = buffer.size
}
```

### 5.3.2 Log Entry Creation

```kotlin
private val logBuffer = LogBuffer(500)

private fun addLog(level: LogLevel, tag: String, message: String) {
    val entry = LogEntry(
        timestamp = System.currentTimeMillis(),
        level = level,
        tag = tag,
        message = message
    )

    logBuffer.add(entry)

    // Also log to Logcat in debug builds
    if (BuildConfig.DEBUG) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
            LogLevel.EVENT -> Log.v(tag, "[EVENT] $message")
        }
    }
}
```

---

## 5.4 Event Processing

### 5.4.1 Screen Change Processing

```kotlin
private fun processScreenChange(event: ScreenChangeEvent) {
    // Track screen visit
    val isNew = event.screenHash !in visitedScreens
    visitedScreens.add(event.screenHash)

    // Log event
    addLog(
        LogLevel.EVENT,
        "SCREEN",
        buildString {
            append("Hash: ${event.screenHash.take(8)}")
            if (isNew) append(" [NEW]")
            append(" | ${event.activityName}")
            append(" | ${event.elementCount} elements")
        }
    )

    // Update state
    if (isNew) {
        uiState.value = uiState.value.copy(
            screensExplored = uiState.value.screensExplored + 1
        )
    }

    // Query elements for inspector
    if (selectedTab == 2) {  // Elements tab
        queryElements()
    }
}
```

### 5.4.2 Error Handling

```kotlin
private fun handleServiceError(errorCode: String, message: String) {
    addLog(LogLevel.ERROR, "SERVICE", "[$errorCode] $message")

    when (errorCode) {
        "E001" -> {
            // Service not available
            uiState.value = uiState.value.copy(
                isServiceBound = false,
                jitActive = false
            )
            attemptReconnect()
        }
        "E002" -> {
            // Element not found - log only
        }
        "E003" -> {
            // Action failed - may retry
            addLog(LogLevel.WARN, "SERVICE", "Action failed, may retry")
        }
        "E004" -> {
            // Screen capture failed
            addLog(LogLevel.WARN, "SERVICE", "Screen capture failed")
        }
        "E005" -> {
            // Permission denied
            showPermissionDialog()
        }
    }
}

private fun attemptReconnect() {
    handler.postDelayed({
        if (!isBound) {
            addLog(LogLevel.INFO, TAG, "Attempting reconnect...")
            bindToService()
        }
    }, RECONNECT_DELAY)
}

companion object {
    private const val RECONNECT_DELAY = 3000L
}
```

---

## 5.5 Event Statistics

### 5.5.1 Event Counters

```kotlin
data class EventStatistics(
    val screenChanges: Int = 0,
    val elementActions: Int = 0,
    val scrollEvents: Int = 0,
    val dynamicDetections: Int = 0,
    val menuDiscoveries: Int = 0,
    val loginDetections: Int = 0,
    val errors: Int = 0,
    val startTime: Long = System.currentTimeMillis()
) {
    val totalEvents: Int
        get() = screenChanges + elementActions + scrollEvents +
                dynamicDetections + menuDiscoveries + loginDetections + errors

    val eventsPerMinute: Float
        get() {
            val minutes = (System.currentTimeMillis() - startTime) / 60000f
            return if (minutes > 0) totalEvents / minutes else 0f
        }
}

private var eventStats = EventStatistics()

private fun updateEventStats(eventType: String) {
    eventStats = when (eventType) {
        "SCREEN" -> eventStats.copy(screenChanges = eventStats.screenChanges + 1)
        "ACTION" -> eventStats.copy(elementActions = eventStats.elementActions + 1)
        "SCROLL" -> eventStats.copy(scrollEvents = eventStats.scrollEvents + 1)
        "DYNAMIC" -> eventStats.copy(dynamicDetections = eventStats.dynamicDetections + 1)
        "MENU" -> eventStats.copy(menuDiscoveries = eventStats.menuDiscoveries + 1)
        "LOGIN" -> eventStats.copy(loginDetections = eventStats.loginDetections + 1)
        "ERROR" -> eventStats.copy(errors = eventStats.errors + 1)
        else -> eventStats
    }
}
```

---

## 5.6 Debugging Tips

### 5.6.1 Common Event Patterns

| Pattern | Indicates |
|---------|-----------|
| Rapid SCREEN events | User navigating quickly |
| No ACTION events | Elements not clickable |
| Many DYNAMIC events | Content-heavy screen |
| LOGIN followed by pause | Auth screen detected |
| ERROR E001 | Service disconnected |

### 5.6.2 Logcat Commands

```bash
# All LearnApp events
adb logcat -s LearnAppDevActivity:V

# JIT service events
adb logcat -s JITLearningService:V

# Both with timestamps
adb logcat -v time -s LearnAppDevActivity:V JITLearningService:V

# Filter errors only
adb logcat -s LearnAppDevActivity:E JITLearningService:E
```

---

## 5.7 Next Steps

Continue to [Chapter 6: Safety System Implementation](./06-Safety-Implementation.md) for detailed safety coverage.

---

**End of Chapter 5**
