# Chapter 8: Debugging & Analysis Tools

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch08
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 8.1 Developer Tools Overview

AvaLearnPro provides three main debugging tools:

| Tool | Purpose | Access |
|------|---------|--------|
| **Log Console** | Real-time event/debug logs | Logs Tab |
| **Element Inspector** | UI element tree analysis | Elements Tab |
| **Event Viewer** | AIDL event streaming | Integrated in Logs |

---

## 8.2 Log Console

### 8.2.1 Console Features

```
+--------------------------------------------------+
| 156 entries                          [ Clear ]   |
+--------------------------------------------------+
| [14:32:05.123] I SCREEN: Hash: a1b2c3d4         |
| [14:32:05.089] E ACTION: click on btn1: OK      |
| [14:32:04.967] W LOGIN: Detected in com.ex      |
| [14:32:04.845] I ELEMENTS: Discovered 15        |
| [14:32:04.723] E SCROLL: down 200px, 3 new      |
+--------------------------------------------------+
```

### 8.2.2 Log Level Colors

| Level | Prefix | Color | Use Case |
|-------|--------|-------|----------|
| DEBUG | D | #9E9E9E (Gray) | Verbose diagnostics |
| INFO | I | #60A5FA (Blue) | Normal operations |
| WARN | W | #FBBF24 (Yellow) | Potential issues |
| ERROR | E | #F87171 (Red) | Failures |
| EVENT | E | #A78BFA (Purple) | AIDL events |

### 8.2.3 Log Filtering

```kotlin
// Filter by level
fun filterLogs(level: LogLevel): List<LogEntry> {
    return logs.filter { it.level == level }
}

// Filter by tag
fun filterByTag(tag: String): List<LogEntry> {
    return logs.filter { it.tag.contains(tag, ignoreCase = true) }
}

// Search content
fun searchLogs(query: String): List<LogEntry> {
    return logs.filter {
        it.message.contains(query, ignoreCase = true) ||
        it.tag.contains(query, ignoreCase = true)
    }
}
```

### 8.2.4 Common Log Patterns

| Pattern | Meaning | Action |
|---------|---------|--------|
| `SCREEN: Hash: xxx [NEW]` | New screen discovered | None |
| `ACTION: click on xxx: OK` | Click succeeded | None |
| `ACTION: click on xxx: FAIL` | Click failed | Investigate element |
| `LOGIN: Detected` | Auth screen found | May need manual action |
| `SCROLL: down Xpx, N new` | Scroll revealed elements | None |
| `ERROR: [E001]` | Service issue | Check connection |

---

## 8.3 Element Inspector

### 8.3.1 Inspector Layout

```
+--------------------------------------------------+
| 23 elements                          [ Query ]   |
+--------------------------------------------------+
| +----------------------------------------------+ |
| | Login Button                                 | |
| | Button                                       | |
| | com.example:id/btn_login                     | |
| | [click] [long]                               | |
| | [0,540,1080,640]                            | |
| +----------------------------------------------+ |
+--------------------------------------------------+
```

### 8.3.2 Element Properties

| Property | Description | Example |
|----------|-------------|---------|
| **Display Name** | contentDescription or text | "Login Button" |
| **Class** | Short class name | "Button" |
| **Resource ID** | Android resource ID | "com.example:id/btn_login" |
| **Actions** | Available actions | [click] [long] |
| **Bounds** | Position [l,t,r,b] | [0,540,1080,640] |

### 8.3.3 Querying Elements

```kotlin
private fun queryCurrentElements() {
    try {
        val screenInfo = jitService?.getCurrentScreenInfo()
        if (screenInfo != null) {
            elements.clear()
            elements.add(screenInfo)  // Root node

            // Flatten tree
            fun flatten(node: ParcelableNodeInfo) {
                elements.add(node)
                node.children.forEach { flatten(it) }
            }
            screenInfo.children.forEach { flatten(it) }

            addLog(LogLevel.INFO, TAG, "Queried ${elements.size} elements")
        }
    } catch (e: RemoteException) {
        addLog(LogLevel.ERROR, TAG, "Query failed: ${e.message}")
    }
}
```

### 8.3.4 Element Analysis

```kotlin
fun analyzeElement(element: ParcelableNodeInfo): ElementAnalysis {
    return ElementAnalysis(
        isInteractive = element.isClickable || element.isEditable,
        actionCount = countActions(element),
        hasLabel = element.contentDescription.isNotEmpty() || element.text.isNotEmpty(),
        hasResourceId = element.resourceId.isNotEmpty(),
        depth = element.depth,
        childCount = element.childCount,
        commandConfidence = calculateCommandConfidence(element)
    )
}

data class ElementAnalysis(
    val isInteractive: Boolean,
    val actionCount: Int,
    val hasLabel: Boolean,
    val hasResourceId: Boolean,
    val depth: Int,
    val childCount: Int,
    val commandConfidence: Float
)
```

---

## 8.4 Event Streaming

### 8.4.1 Event Types

| Event | Callback | Typical Rate |
|-------|----------|--------------|
| Screen Change | onScreenChanged | 1-10/min |
| Element Action | onElementAction | 10-50/min |
| Scroll | onScrollDetected | 5-20/min |
| Dynamic Content | onDynamicContentDetected | 0-5/min |
| Menu Discovery | onMenuDiscovered | 0-5/min |
| Login Detection | onLoginScreenDetected | 0-2/min |
| Error | onError | 0-5/min |

### 8.4.2 Event Statistics Display

```kotlin
@Composable
fun EventStatisticsCard(stats: EventStatistics) {
    DevCard(title = "Event Statistics") {
        StatRow("Screen Changes", stats.screenChanges.toString())
        StatRow("Element Actions", stats.elementActions.toString())
        StatRow("Scroll Events", stats.scrollEvents.toString())
        StatRow("Dynamic Regions", stats.dynamicDetections.toString())
        StatRow("Menus Found", stats.menuDiscoveries.toString())
        StatRow("Login Detections", stats.loginDetections.toString())
        StatRow("Errors", stats.errors.toString())

        Spacer(modifier = Modifier.height(8.dp))

        StatRow("Total Events", stats.totalEvents.toString())
        StatRow("Events/Min", String.format("%.1f", stats.eventsPerMinute))
    }
}
```

---

## 8.5 Debugging Workflows

### 8.5.1 Service Connection Issues

```
1. Check Logs Tab for errors
   - Look for "E001" (Service not available)
   - Look for "Service disconnected"

2. Check Status Tab
   - JIT Status should show "Active"
   - If "Inactive", VoiceOS service is not running

3. Verify Accessibility
   - Settings > Accessibility > VoiceOS
   - Should be enabled

4. Restart Flow
   - Close AvaLearnPro
   - Toggle VoiceOS accessibility off/on
   - Reopen AvaLearnPro
```

### 8.5.2 Element Not Found Issues

```
1. Query current screen
   - Press "Query" in Elements Tab
   - Check element count

2. Search for element
   - Look for target by name/ID
   - Verify it exists in tree

3. Check element state
   - isEnabled should be true
   - isVisible should be true
   - bounds should be on-screen

4. Check timing
   - Element may appear after animation
   - Try querying after delay
```

### 8.5.3 Export Issues

```
1. Check logs for export errors
   - "Storage permission required"
   - "Export failed: IO error"

2. Verify prerequisites
   - Elements discovered > 0
   - Storage permission granted

3. Check storage
   - Sufficient free space
   - App has write access

4. Manual verification
   - adb shell ls /sdcard/Android/data/com.augmentalis.learnappdev/files/learned_apps/
```

---

## 8.6 Logcat Integration

### 8.6.1 Useful Logcat Commands

```bash
# All AvaLearnPro logs
adb logcat -s LearnAppDevActivity:V

# JIT service logs
adb logcat -s JITLearningService:V

# Combined with timestamps
adb logcat -v time -s LearnAppDevActivity:V JITLearningService:V

# Filter errors only
adb logcat -s LearnAppDevActivity:E JITLearningService:E

# Save to file
adb logcat -s LearnAppDevActivity:V JITLearningService:V > debug.log

# Clear and start fresh
adb logcat -c && adb logcat -s LearnAppDevActivity:V
```

### 8.6.2 Logcat Tags

| Tag | Source | Content |
|-----|--------|---------|
| LearnAppDevActivity | UI Layer | UI events, user actions |
| JITLearningService | Service | Accessibility events |
| ElementCapture | Service | Element tree capture |
| SafetyManager | Core | Safety checks |
| AVUExporter | Core | Export operations |

---

## 8.7 Performance Analysis

### 8.7.1 Timing Analysis

```kotlin
class TimingAnalyzer {

    private val timings = mutableMapOf<String, MutableList<Long>>()

    fun start(operation: String): Long {
        return System.currentTimeMillis()
    }

    fun end(operation: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        timings.getOrPut(operation) { mutableListOf() }.add(duration)

        if (BuildConfig.DEBUG) {
            Log.d("TIMING", "$operation took ${duration}ms")
        }
    }

    fun getAverage(operation: String): Double {
        return timings[operation]?.average() ?: 0.0
    }

    fun getReport(): String {
        return buildString {
            appendLine("Performance Report")
            appendLine("==================")
            timings.forEach { (op, times) ->
                appendLine("$op: avg=${times.average().toLong()}ms, " +
                          "min=${times.minOrNull()}ms, max=${times.maxOrNull()}ms")
            }
        }
    }
}
```

### 8.7.2 Memory Monitoring

```kotlin
fun logMemoryUsage() {
    val runtime = Runtime.getRuntime()
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val maxMemory = runtime.maxMemory() / 1024 / 1024

    addLog(LogLevel.DEBUG, "MEMORY", "Used: ${usedMemory}MB / Max: ${maxMemory}MB")
}
```

---

## 8.8 Next Steps

Continue to [Chapter 9: Architecture Decision Records](./09-ADR-Decisions.md).

---

**End of Chapter 8**
