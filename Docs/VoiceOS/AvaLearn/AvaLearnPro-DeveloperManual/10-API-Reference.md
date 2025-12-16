# Chapter 10: Complete API Reference

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch10
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 10.1 AIDL Interfaces

### IElementCaptureService

```kotlin
interface IElementCaptureService {
    // State Management
    fun queryState(): JITState
    fun pauseCapture(): Unit
    fun resumeCapture(): Unit
    fun isCapturing(): Boolean

    // Screen Information
    fun getCurrentScreenInfo(): ParcelableNodeInfo
    fun getCurrentPackageName(): String
    fun getCurrentActivityName(): String
    fun getCurrentScreenHash(): String

    // Event Streaming
    fun registerEventListener(listener: IAccessibilityEventListener): Unit
    fun unregisterEventListener(listener: IAccessibilityEventListener): Unit

    // Exploration Control
    fun startExploration(command: ExplorationCommand): Unit
    fun stopExploration(): Unit
    fun executeAction(elementUuid: String, actionType: String): Unit

    // Safety Queries
    fun isLoginScreen(): Boolean
    fun getDoNotClickElements(): List<String>
    fun getDynamicRegionCount(): Int
}
```

### IAccessibilityEventListener

```kotlin
interface IAccessibilityEventListener {
    fun onScreenChanged(event: ScreenChangeEvent): Unit
    fun onElementAction(elementUuid: String, actionType: String, success: Boolean): Unit
    fun onScrollDetected(direction: String, distance: Int, newElementsCount: Int): Unit
    fun onDynamicContentDetected(screenHash: String, regionId: String): Unit
    fun onMenuDiscovered(menuId: String, totalItems: Int, visibleItems: Int): Unit
    fun onLoginScreenDetected(packageName: String, screenHash: String): Unit
    fun onError(errorCode: String, message: String): Unit
}
```

---

## 10.2 Parcelable Classes

### JITState

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
) : Parcelable
```

### ScreenChangeEvent

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
) : Parcelable
```

### ParcelableNodeInfo

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
    fun getDisplayName(): String
    fun getShortClassName(): String
    fun getActionFlags(): String
    fun getBoundsString(): String
}
```

### ExplorationCommand

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
    val timeout: Long = 300000L
) : Parcelable
```

---

## 10.3 Core Classes

### ExplorationManager

```kotlin
class ExplorationManager(
    safetyManager: SafetyManager,
    callback: ExplorationCallback
) {
    fun startExploration(targetPackage: String): Unit
    fun stopExploration(): Unit
    fun processScreen(screenInfo: ScreenInfo): Unit
    fun markElementClicked(uuid: String): Unit
    fun getStatistics(): ExplorationStatistics
    fun getAllElements(): List<ElementInfo>
    fun getVisitedScreens(): Set<String>
}

interface ExplorationCallback {
    fun onPhaseChanged(phase: ExplorationPhase): Unit
    fun onStateUpdated(stats: ExplorationStatistics): Unit
    fun onLoginDetected(packageName: String, screenHash: String): Unit
    fun onLoopDetected(screenHash: String): Unit
    fun onExplorationComplete(stats: ExplorationStatistics): Unit
    fun onError(message: String): Unit
}
```

### SafetyManager

```kotlin
class SafetyManager(callback: SafetyCallback? = null) {
    fun isDangerous(element: ElementInfo): Boolean
    fun checkForLogin(screenInfo: ScreenInfo): LoginType?
    fun checkForDynamicRegions(previous: ScreenInfo, current: ScreenInfo): List<DynamicRegion>
    fun checkForLoop(screenHash: String): Boolean
    fun recordMenuDiscovery(): Unit
    fun getStatistics(): SafetyStatistics
    fun reset(): Unit
}

interface SafetyCallback {
    fun onDangerousElement(element: ElementInfo, reason: DoNotClickReason): Unit
    fun onLoginDetected(loginType: LoginType, screenHash: String): Unit
    fun onDynamicRegionConfirmed(region: DynamicRegion): Unit
    fun onLoopDetected(screenHash: String, visitCount: Int): Unit
}
```

### AVUExporter

```kotlin
class AVUExporter(context: Context, mode: ExportMode = ExportMode.USER) {
    suspend fun export(
        packageName: String,
        appName: String,
        screens: List<ScreenInfo>,
        elements: List<ElementInfo>,
        navigations: List<NavigationRecord>,
        statistics: ExplorationStatistics
    ): ExportResult
}

sealed class ExportResult {
    data class Success(val path: String) : ExportResult()
    data class Failure(val error: String) : ExportResult()
}

enum class ExportMode { USER, DEVELOPER }
```

---

## 10.4 Data Models

### ElementInfo

```kotlin
data class ElementInfo(
    val uuid: String,
    val className: String,
    val packageName: String,
    val text: String,
    val contentDescription: String,
    val resourceId: String,
    val actions: Set<ElementAction>,
    val bounds: Bounds,
    val state: ElementState,
    val depth: Int,
    val screenHash: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayName: String
    val shortClassName: String
    val isInteractive: Boolean
    fun toAVURecord(): String
}

data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
    val height: Int
    val centerX: Int
    val centerY: Int
    fun toAVU(): String
    fun contains(x: Int, y: Int): Boolean
}

enum class ElementAction(val code: String) {
    CLICK("C"), LONG_CLICK("L"), EDIT("E"), SCROLL("S")
}

enum class ElementState(val code: String) {
    ACTIVE("ACT"), DISABLED("DIS"), HIDDEN("HID"), DANGEROUS("DNG")
}
```

### ScreenInfo

```kotlin
data class ScreenInfo(
    val screenHash: String,
    val activityName: String,
    val packageName: String,
    val elements: List<ElementInfo>,
    val timestamp: Long = System.currentTimeMillis(),
    val visitCount: Int = 1,
    val hasLoginScreen: Boolean = false,
    val dynamicRegions: List<DynamicRegion> = emptyList()
) {
    val interactiveElements: List<ElementInfo>
    val elementCount: Int
    val clickableCount: Int
    fun toAVURecord(): String
}
```

### CommandInfo

```kotlin
data class CommandInfo(
    val commandId: String,
    val phrase: String,
    val action: String,
    val targetElementUuid: String,
    val confidence: Float,
    val synonyms: List<String> = emptyList(),
    val screenHash: String,
    val packageName: String
) {
    fun toAVURecord(): String
    fun toSynonymRecord(): String
}
```

---

## 10.5 Enumerations

### ExplorationPhase

```kotlin
enum class ExplorationPhase {
    IDLE, INITIALIZING, EXPLORING, WAITING_USER, PAUSED, COMPLETED, ERROR;

    fun isActive(): Boolean
    fun canStart(): Boolean
    fun canStop(): Boolean
}
```

### LogLevel

```kotlin
enum class LogLevel(val prefix: String, val color: Long) {
    DEBUG("D", 0xFF9E9E9E),
    INFO("I", 0xFF60A5FA),
    WARN("W", 0xFFFBBF24),
    ERROR("E", 0xFFF87171),
    EVENT("E", 0xFFA78BFA)
}
```

### DoNotClickReason

```kotlin
enum class DoNotClickReason {
    DESTRUCTIVE, FINANCIAL, ACCOUNT, SYSTEM, CONFIRMATION
}
```

### LoginType

```kotlin
enum class LoginType {
    PASSWORD, BIOMETRIC, PIN, PATTERN, TWO_FACTOR
}
```

### LoopType

```kotlin
enum class LoopType {
    WARNING, CRITICAL, RAPID
}
```

---

## 10.6 UI State Classes

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
    val isServiceBound: Boolean = false,
    val jitActive: Boolean = false,
    val jitPaused: Boolean = false,
    val currentPackage: String = "",
    val lastExportPath: String? = null,
    val exportInProgress: Boolean = false,
    val neo4jConnected: Boolean = false,
    val eventStreamActive: Boolean = false,
    val logCount: Int = 0
)
```

### LogEntry

```kotlin
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val message: String = ""
) {
    fun formatted(): String
    fun shortFormatted(): String
}
```

---

## 10.7 Statistics Classes

### ExplorationStatistics

```kotlin
data class ExplorationStatistics(
    val phase: ExplorationPhase,
    val screensExplored: Int,
    val elementsDiscovered: Int,
    val elementsClicked: Int,
    val coverage: Float,
    val dangerousSkipped: Int,
    val targetPackage: String,
    val explorationTime: Float = 0f,
    val loopsDetected: Int = 0
)
```

### SafetyStatistics

```kotlin
data class SafetyStatistics(
    val dncSkipped: Int,
    val dynamicRegions: Int,
    val menusFound: Int
)
```

### EventStatistics

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
    val eventsPerMinute: Float
}
```

---

## 10.8 Error Codes

| Code | Name | Description |
|------|------|-------------|
| E001 | SERVICE_NOT_AVAILABLE | VoiceOS service not running |
| E002 | ELEMENT_NOT_FOUND | Target element doesn't exist |
| E003 | ACTION_FAILED | Requested action couldn't execute |
| E004 | SCREEN_CAPTURE_FAILED | Unable to capture screen |
| E005 | PERMISSION_DENIED | Missing required permission |
| E006 | EXPORT_FAILED | File export error |
| E007 | PARSE_ERROR | AVU parsing failed |
| E008 | TIMEOUT | Operation timed out |
| E009 | LOOP_DETECTED | Exploration stuck in loop |
| E010 | LOGIN_BLOCKED | Login screen blocking |

---

## 10.9 Constants

### Safety Thresholds

```kotlin
object SafetyConstants {
    const val LOOP_WARNING_THRESHOLD = 3
    const val LOOP_CRITICAL_THRESHOLD = 5
    const val LOGIN_SCORE_THRESHOLD = 3.0f
    const val MAX_SCROLL_DEPTH = 10
    const val SCROLL_DELAY_MS = 500L
    const val DYNAMIC_CHECK_INTERVAL_MS = 500L
}
```

### Export Constants

```kotlin
object ExportConstants {
    const val AVU_VERSION = "1.0"
    const val FILE_EXTENSION = ".vos"
    const val MAX_SYNONYMS = 5
    const val CONFIDENCE_THRESHOLD = 0.5f
}
```

### UI Constants

```kotlin
object UIConstants {
    const val MAX_LOG_ENTRIES = 500
    const val REFRESH_INTERVAL_MS = 1000L
    const val ANIMATION_DURATION_MS = 300L
}
```

---

## 10.10 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-11 | Initial API documentation |

---

**End of Chapter 10**

---

**End of AvaLearnPro Developer Manual**

**Copyright 2025 Augmentalis. All rights reserved.**
