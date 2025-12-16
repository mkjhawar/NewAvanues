# LearnApp Developer Manual

**Document ID:** VoiceOS-LearnApp-Developer-Manual-51211-V1
**Version:** 1.0
**Created:** 2025-12-11
**Audience:** Developers

---

## Table of Contents

1. [Overview](#overview)
2. [Developer Edition Features](#developer-edition-features)
3. [User Interface](#user-interface)
4. [Tabs Reference](#tabs-reference)
5. [Event Streaming](#event-streaming)
6. [Log Analysis](#log-analysis)
7. [Element Inspection](#element-inspection)
8. [AVU Export](#avu-export)
9. [AIDL Integration](#aidl-integration)
10. [Debugging Guide](#debugging-guide)
11. [API Reference](#api-reference)

---

## Overview

LearnAppDev is the developer edition of LearnApp, providing advanced debugging and analysis tools for developing and testing the VoiceOS learning system.

### Key Differences from User Edition

| Feature | User Edition | Developer Edition |
|---------|--------------|-------------------|
| Theme | Ocean Blue Light | Ocean Blue Dark + Cyan |
| Logging | Toast only | Full console (500 entries) |
| Elements | Hidden | Inspector with properties |
| Events | Not visible | Real-time stream viewer |
| AVU Export | Encrypted | Plain text (readable) |
| Neo4j | None | Graph visualization (WIP) |

---

## Developer Edition Features

### Visual Identification

The developer edition is identifiable by:
1. **Dark theme** with Ocean Blue accents
2. **Cyan "DEV" badge** in the title bar
3. **Three-tab interface** (Status, Logs, Elements)

### Package Details

| Property | Value |
|----------|-------|
| Package Name | `com.augmentalis.learnappdev` |
| Debug Suffix | `.debug` |
| Build Flags | `IS_DEVELOPER_EDITION=true`, `ENABLE_NEO4J=true`, `ENABLE_LOGGING=true` |

---

## User Interface

### Ocean Blue XR Dark Theme

```kotlin
// Theme colors used in Developer Edition
object OceanDevTheme {
    // Primary (Ocean Blue Dark Mode)
    val Primary = Color(0xFF60A5FA)
    val PrimaryContainer = Color(0xFF1E3A5F)

    // Developer Accent (Cyan)
    val Accent = Color(0xFF22D3EE)

    // Semantic
    val Success = Color(0xFF34D399)
    val Error = Color(0xFFF87171)
    val Warning = Color(0xFFFBBF24)

    // Surface
    val Surface = Color(0xFF0F172A)
    val SurfaceVariant = Color(0xFF1E293B)
    val Background = Color(0xFF0C1929)
    val ConsoleBackground = Color(0xFF0D0D0D)
}
```

### Layout Structure

```
+--------------------------------------------------+
| LearnApp Dev [DEV]                               |
+--------------------------------------------------+
|  [Status]    [Logs]    [Elements]                |  <- TabRow
+--------------------------------------------------+
|                                                  |
|          Tab Content Area                        |
|                                                  |
+--------------------------------------------------+
```

---

## Tabs Reference

### Status Tab

Same as User Edition but with developer styling:

| Card | Content |
|------|---------|
| JIT Service | Connection status, screens, elements |
| Exploration | Phase, coverage, controls |
| Safety | DNC count, dynamic regions, menus |
| Export | Last export, export button |

### Logs Tab

Real-time logging console:

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

**Log Levels:**

| Level | Prefix | Color | Use Case |
|-------|--------|-------|----------|
| DEBUG | D | Gray (#9E9E9E) | Verbose information |
| INFO | I | Blue (#60A5FA) | Normal operations |
| WARN | W | Yellow (#FBBF24) | Potential issues |
| ERROR | E | Red (#F87171) | Failures |
| EVENT | E | Purple (#A78BFA) | AIDL events |

### Elements Tab

Element tree inspector:

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

**Element Properties:**

| Property | Description |
|----------|-------------|
| Display Name | contentDescription or text |
| Class | Short class name (e.g., "Button") |
| Resource ID | Android resource identifier |
| Actions | Available actions (click, long, edit, scroll) |
| Bounds | [left, top, right, bottom] |

---

## Event Streaming

### Registration

```kotlin
// In LearnAppDevActivity
private val eventListener = object : IAccessibilityEventListener.Stub() {
    override fun onScreenChanged(event: ScreenChangeEvent) {
        addLog(LogLevel.EVENT, "SCREEN", "Screen changed: ${event.toIpcString()}")
    }

    override fun onElementAction(elementUuid: String, actionType: String, success: Boolean) {
        addLog(LogLevel.EVENT, "ACTION", "$actionType on $elementUuid: ${if(success) "OK" else "FAIL"}")
    }

    override fun onScrollDetected(direction: String, distance: Int, newElementsCount: Int) {
        addLog(LogLevel.EVENT, "SCROLL", "$direction ${distance}px, $newElementsCount new")
    }

    override fun onDynamicContentDetected(screenHash: String, regionId: String) {
        addLog(LogLevel.EVENT, "DYNAMIC", "Screen $screenHash, region $regionId")
    }

    override fun onMenuDiscovered(menuId: String, totalItems: Int, visibleItems: Int) {
        addLog(LogLevel.EVENT, "MENU", "$menuId: $visibleItems/$totalItems items")
    }

    override fun onLoginScreenDetected(packageName: String, screenHash: String) {
        addLog(LogLevel.WARN, "LOGIN", "Detected in $packageName, screen $screenHash")
    }

    override fun onError(errorCode: String, message: String) {
        addLog(LogLevel.ERROR, "JIT", "[$errorCode] $message")
    }
}

// Register on service connect
jitService?.registerEventListener(eventListener)

// Unregister on destroy
jitService?.unregisterEventListener(eventListener)
```

### Event Types

| Event | When Fired | Data |
|-------|------------|------|
| `onScreenChanged` | Activity/window change | ScreenChangeEvent |
| `onElementAction` | Click/action performed | uuid, type, success |
| `onScrollDetected` | Scroll gesture | direction, distance, new count |
| `onDynamicContentDetected` | Content update | screenHash, regionId |
| `onMenuDiscovered` | Menu opened | menuId, total, visible |
| `onLoginScreenDetected` | Login UI detected | packageName, screenHash |
| `onError` | Error occurred | errorCode, message |

---

## Log Analysis

### Log Entry Structure

```kotlin
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val message: String = ""
)

// Formatted output
fun formatted(): String {
    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
    return "[$time] ${level.prefix} $tag: $message"
}
```

### Common Log Patterns

| Pattern | Meaning |
|---------|---------|
| `SCREEN: Hash: abc123` | New screen detected |
| `ELEMENTS: Discovered 15` | Elements found on screen |
| `ACTION: click on btn1: OK` | Click succeeded |
| `LOGIN: Detected` | Login screen found |
| `SCROLL: down 200px, 3 new` | Scroll revealed elements |
| `DYNAMIC: region xyz` | Dynamic content detected |

### Filtering Tips

- Look for `ERROR` level for failures
- Track `SCREEN` entries for navigation flow
- Monitor `ACTION` for click results
- Check `LOGIN` for auth screen handling

---

## Element Inspection

### Query Current Screen

```kotlin
private fun queryCurrentElements() {
    try {
        val screenInfo = jitService?.getCurrentScreenInfo()
        if (screenInfo != null) {
            currentElements.clear()
            currentElements.add(screenInfo)
            currentElements.addAll(screenInfo.children)
            addLog(LogLevel.INFO, TAG, "Queried ${currentElements.size} elements")
        }
    } catch (e: Exception) {
        addLog(LogLevel.ERROR, TAG, "Query failed: ${e.message}")
    }
}
```

### ParcelableNodeInfo Structure

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
        return contentDescription.ifEmpty { text.ifEmpty { getShortClassName() } }
    }

    fun getShortClassName(): String {
        return className.substringAfterLast(".")
    }
}
```

---

## AVU Export

### Developer Mode Export

Developer edition exports in **plain text** for debugging:

```kotlin
// ExportMode determines encryption
avuExporter = AVUExporter(this, ExportMode.DEVELOPER)
```

### Export File Location

```
/storage/emulated/0/Android/data/com.augmentalis.learnappdev/files/learned_apps/
```

### AVU File Structure

```
# Avanues Universal Format v1.0
# Type: VOS
---
schema: avu-1.0
version: 1.0.0
locale: en-US
metadata:
  file: com.example.app.vos
  category: learned_app
  count: 87
---
APP:com.example.app:Example App:1702300800000
STA:5:87:42:2.3:5:75.5
SCR:abc123:MainActivity:1702300801000:15
ELM:uuid1:Login:Button:C:0,540,1080,640:ACT
ELM:uuid2:Username:EditText:CE:0,300,1080,380:INP
CMD:cmd1:login:click:uuid1:0.95
CMD:cmd2:enter username:setText:uuid2:0.90
---
synonyms:
  login: [sign in, log in]
  username: [user, email]
```

---

## AIDL Integration

### Service Binding

```kotlin
private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        jitService = IElementCaptureService.Stub.asInterface(service)
        isBound = true

        // Register event listener for streaming
        jitService?.registerEventListener(eventListener)

        // Initial state query
        updateJITState()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        jitService = null
        isBound = false
    }
}

// Bind to service
val intent = Intent().apply {
    component = ComponentName(
        "com.augmentalis.voiceoscore",
        "com.augmentalis.jitlearning.JITLearningService"
    )
}
bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
```

### Key AIDL Methods

| Method | Purpose | Usage |
|--------|---------|-------|
| `queryState()` | Get JIT statistics | Status display |
| `pauseCapture()` | Stop JIT during exploration | Start exploration |
| `resumeCapture()` | Resume JIT after exploration | Stop exploration |
| `getCurrentScreenInfo()` | Get element tree | Element inspector |
| `registerEventListener()` | Start event streaming | Logs tab |

---

## Debugging Guide

### Common Issues

| Issue | Check | Solution |
|-------|-------|----------|
| Service not binding | VoiceOS running? | Start VoiceOS service |
| No events streaming | Listener registered? | Check onServiceConnected |
| Empty element list | Query timing | Wait for screen load |
| Export fails | Permissions | Grant storage permission |

### Debug Checklist

1. **Service Connection**
   - Check "JIT Service" card status
   - Should show "ACTIVE" in green

2. **Event Streaming**
   - Switch to Logs tab
   - Should see EVENT entries

3. **Element Query**
   - Switch to Elements tab
   - Press "Query" button
   - Should populate element list

4. **Export**
   - Complete an exploration
   - Press Export button
   - Check logs for success/failure

### Logcat Tags

```bash
adb logcat -s LearnAppDevActivity:V JITLearningService:V
```

---

## API Reference

### ExplorationPhase

```kotlin
enum class ExplorationPhase {
    IDLE,           // Not started
    INITIALIZING,   // Setting up
    EXPLORING,      // Active exploration
    WAITING_USER,   // Paused for user input
    COMPLETED,      // Finished successfully
    ERROR           // Failed
}
```

### DevUiState

```kotlin
data class DevUiState(
    val phase: ExplorationPhase = ExplorationPhase.IDLE,
    val screensExplored: Int = 0,
    val elementsDiscovered: Int = 0,
    val elementsClicked: Int = 0,
    val coverage: Float = 0f,
    val dangerousElementsSkipped: Int = 0,
    val dynamicRegionsDetected: Int = 0,
    val menusDiscovered: Int = 0,
    val isOnLoginScreen: Boolean = false,
    val loginType: String = "",
    val lastExportPath: String? = null,
    val neo4jConnected: Boolean = false,
    val eventStreamActive: Boolean = false
)
```

### SafetyCallback

```kotlin
interface SafetyCallback {
    fun onLoginDetected(loginType: LoginType, message: String)
    fun onDangerousElement(element: ElementInfo, reason: DoNotClickReason)
    fun onDynamicRegionConfirmed(region: DynamicRegion)
    fun onLoopDetected(screenHash: String, visitCount: Int)
}
```

---

## Future Features

### Neo4j Graph Visualization (Planned)

The Neo4j integration is prepared but not yet fully implemented:

```kotlin
// Neo4j connection config
data class Neo4jConfig(
    val uri: String = "bolt://localhost:7687",
    val username: String = "neo4j",
    val password: String = "password",
    val database: String = "voiceos"
)
```

**Planned Features:**
- Screen relationship graph
- Navigation path visualization
- Element hierarchy tree
- Cypher query execution

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-11 | Initial developer manual |

---

**End of Developer Manual**
