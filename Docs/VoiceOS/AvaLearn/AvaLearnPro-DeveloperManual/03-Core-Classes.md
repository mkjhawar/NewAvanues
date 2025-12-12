# Chapter 3: Core Classes & Data Models

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch03
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 3.1 Package Structure

```
com.augmentalis.learnappdev/
├── LearnAppDevActivity.kt      # Main developer UI
├── theme/
│   └── OceanDevTheme.kt        # Dark theme colors
├── ui/
│   ├── tabs/
│   │   ├── StatusTab.kt        # Status display
│   │   ├── LogsTab.kt          # Log console
│   │   └── ElementsTab.kt      # Element inspector
│   └── components/
│       ├── DevCard.kt          # Card component
│       └── LogEntry.kt         # Log item component
└── model/
    ├── DevUiState.kt           # UI state holder
    └── LogLevel.kt             # Log level enum

com.augmentalis.learnappcore/
├── exploration/
│   ├── ExplorationManager.kt   # Exploration orchestration
│   ├── ExplorationState.kt     # State machine
│   └── ExplorationPhase.kt     # Phase enum
├── safety/
│   ├── SafetyManager.kt        # Safety orchestration
│   ├── DoNotClickManager.kt    # DNC protection
│   ├── LoginDetector.kt        # Auth screen detection
│   ├── DynamicRegionDetector.kt # Dynamic content
│   └── LoopPrevention.kt       # Loop detection
├── export/
│   ├── AVUExporter.kt          # AVU file generation
│   ├── AVUGenerator.kt         # Record generation
│   └── SynonymGenerator.kt     # Command synonyms
└── model/
    ├── ElementInfo.kt          # Element data
    ├── ScreenInfo.kt           # Screen data
    └── CommandInfo.kt          # Command data
```

---

## 3.2 UI State Classes

### 3.2.1 DevUiState

Main UI state holder for the developer edition.

```kotlin
package com.augmentalis.learnappdev.model

data class DevUiState(
    // Exploration State
    val phase: ExplorationPhase = ExplorationPhase.IDLE,
    val screensExplored: Int = 0,
    val elementsDiscovered: Int = 0,
    val elementsClicked: Int = 0,
    val coverage: Float = 0f,

    // Safety State
    val dangerousElementsSkipped: Int = 0,
    val dynamicRegionsDetected: Int = 0,
    val menusDiscovered: Int = 0,
    val isOnLoginScreen: Boolean = false,
    val loginType: String = "",

    // Service State
    val isServiceBound: Boolean = false,
    val jitActive: Boolean = false,
    val jitPaused: Boolean = false,
    val currentPackage: String = "",

    // Export State
    val lastExportPath: String? = null,
    val exportInProgress: Boolean = false,

    // Developer Features
    val neo4jConnected: Boolean = false,
    val eventStreamActive: Boolean = false,
    val logCount: Int = 0
)
```

### 3.2.2 ExplorationPhase

```kotlin
package com.augmentalis.learnappcore.exploration

enum class ExplorationPhase {
    IDLE,           // Not started, ready
    INITIALIZING,   // Setting up exploration
    EXPLORING,      // Actively exploring
    WAITING_USER,   // Paused for user action
    PAUSED,         // Manually paused
    COMPLETED,      // Finished successfully
    ERROR;          // Failed

    fun isActive(): Boolean = this == EXPLORING || this == INITIALIZING

    fun canStart(): Boolean = this == IDLE || this == COMPLETED || this == ERROR

    fun canStop(): Boolean = this == EXPLORING || this == WAITING_USER
}
```

### 3.2.3 LogLevel

```kotlin
package com.augmentalis.learnappdev.model

enum class LogLevel(val prefix: String, val color: Long) {
    DEBUG("D", 0xFF9E9E9E),    // Gray
    INFO("I", 0xFF60A5FA),     // Blue
    WARN("W", 0xFFFBBF24),     // Yellow
    ERROR("E", 0xFFF87171),    // Red
    EVENT("E", 0xFFA78BFA);    // Purple

    companion object {
        fun fromPrefix(prefix: String): LogLevel {
            return entries.find { it.prefix == prefix } ?: INFO
        }
    }
}
```

### 3.2.4 LogEntry

```kotlin
package com.augmentalis.learnappdev.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val message: String = ""
) {
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun formatted(): String {
        val time = dateFormat.format(Date(timestamp))
        return "[$time] ${level.prefix} $tag: $message"
    }

    fun shortFormatted(): String {
        val time = dateFormat.format(Date(timestamp))
        return "[$time] ${level.prefix}: ${message.take(50)}"
    }
}
```

---

## 3.3 Data Model Classes

### 3.3.1 ElementInfo

```kotlin
package com.augmentalis.learnappcore.model

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
        get() = contentDescription.ifEmpty { text.ifEmpty { shortClassName } }

    val shortClassName: String
        get() = className.substringAfterLast(".")

    val isInteractive: Boolean
        get() = actions.isNotEmpty()

    fun toAVURecord(): String {
        val actionsStr = actions.joinToString("") { it.code }
        val stateStr = state.code
        return "ELM:$uuid:$displayName:$shortClassName:$actionsStr:${bounds.toAVU()}:$stateStr"
    }
}

data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2

    fun toAVU(): String = "$left,$top,$right,$bottom"

    fun contains(x: Int, y: Int): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }
}

enum class ElementAction(val code: String) {
    CLICK("C"),
    LONG_CLICK("L"),
    EDIT("E"),
    SCROLL("S");

    companion object {
        fun fromCode(code: String): ElementAction? {
            return entries.find { it.code == code }
        }
    }
}

enum class ElementState(val code: String) {
    ACTIVE("ACT"),
    DISABLED("DIS"),
    HIDDEN("HID"),
    DANGEROUS("DNG");

    companion object {
        fun fromCode(code: String): ElementState {
            return entries.find { it.code == code } ?: ACTIVE
        }
    }
}
```

### 3.3.2 ScreenInfo

```kotlin
package com.augmentalis.learnappcore.model

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
        get() = elements.filter { it.isInteractive }

    val elementCount: Int
        get() = elements.size

    val clickableCount: Int
        get() = elements.count { ElementAction.CLICK in it.actions }

    fun toAVURecord(): String {
        return "SCR:$screenHash:$activityName:$timestamp:$elementCount"
    }
}

data class DynamicRegion(
    val regionId: String,
    val bounds: Bounds,
    val changeFrequency: Int,  // changes per second
    val lastObserved: Long = System.currentTimeMillis()
)
```

### 3.3.3 CommandInfo

```kotlin
package com.augmentalis.learnappcore.model

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
    fun toAVURecord(): String {
        return "CMD:$commandId:$phrase:$action:$targetElementUuid:$confidence"
    }

    fun toSynonymRecord(): String {
        if (synonyms.isEmpty()) return ""
        return "  $phrase: [${synonyms.joinToString(", ")}]"
    }
}
```

---

## 3.4 Manager Classes

### 3.4.1 ExplorationManager

```kotlin
package com.augmentalis.learnappcore.exploration

class ExplorationManager(
    private val safetyManager: SafetyManager,
    private val callback: ExplorationCallback
) {
    private var state: ExplorationState = ExplorationState()
    private var phase: ExplorationPhase = ExplorationPhase.IDLE

    // State tracking
    private val visitedScreens = mutableSetOf<String>()
    private val discoveredElements = mutableMapOf<String, ElementInfo>()
    private val clickedElements = mutableSetOf<String>()

    fun startExploration(targetPackage: String) {
        if (!phase.canStart()) {
            callback.onError("Cannot start: current phase is $phase")
            return
        }

        state = ExplorationState(targetPackage = targetPackage)
        phase = ExplorationPhase.INITIALIZING
        callback.onPhaseChanged(phase)

        // Begin exploration
        phase = ExplorationPhase.EXPLORING
        callback.onPhaseChanged(phase)
    }

    fun stopExploration() {
        if (!phase.canStop()) {
            callback.onError("Cannot stop: current phase is $phase")
            return
        }

        phase = ExplorationPhase.COMPLETED
        callback.onPhaseChanged(phase)
        callback.onExplorationComplete(getStatistics())
    }

    fun processScreen(screenInfo: ScreenInfo) {
        if (phase != ExplorationPhase.EXPLORING) return

        // Track visit
        val screenHash = screenInfo.screenHash
        val isNewScreen = screenHash !in visitedScreens
        visitedScreens.add(screenHash)

        if (isNewScreen) {
            state = state.copy(screensExplored = state.screensExplored + 1)
        }

        // Process elements
        screenInfo.elements.forEach { element ->
            processElement(element, screenHash)
        }

        // Safety checks
        if (screenInfo.hasLoginScreen) {
            handleLoginScreen(screenInfo)
        }

        // Loop check
        val visitCount = visitedScreens.count { it == screenHash }
        if (visitCount > LOOP_THRESHOLD) {
            handleLoopDetected(screenHash)
        }

        callback.onStateUpdated(getStatistics())
    }

    private fun processElement(element: ElementInfo, screenHash: String) {
        val uuid = element.uuid

        if (uuid !in discoveredElements) {
            discoveredElements[uuid] = element
            state = state.copy(elementsDiscovered = state.elementsDiscovered + 1)
        }

        // Safety check
        if (safetyManager.isDangerous(element)) {
            state = state.copy(dangerousSkipped = state.dangerousSkipped + 1)
            return
        }
    }

    fun markElementClicked(uuid: String) {
        if (uuid !in clickedElements) {
            clickedElements.add(uuid)
            state = state.copy(elementsClicked = state.elementsClicked + 1)
        }
    }

    private fun handleLoginScreen(screenInfo: ScreenInfo) {
        phase = ExplorationPhase.WAITING_USER
        callback.onPhaseChanged(phase)
        callback.onLoginDetected(screenInfo.packageName, screenInfo.screenHash)
    }

    private fun handleLoopDetected(screenHash: String) {
        callback.onLoopDetected(screenHash)
    }

    fun getStatistics(): ExplorationStatistics {
        val coverage = if (discoveredElements.isNotEmpty()) {
            clickedElements.size.toFloat() / discoveredElements.size * 100
        } else 0f

        return ExplorationStatistics(
            phase = phase,
            screensExplored = visitedScreens.size,
            elementsDiscovered = discoveredElements.size,
            elementsClicked = clickedElements.size,
            coverage = coverage,
            dangerousSkipped = state.dangerousSkipped,
            targetPackage = state.targetPackage
        )
    }

    fun getAllElements(): List<ElementInfo> = discoveredElements.values.toList()

    fun getVisitedScreens(): Set<String> = visitedScreens.toSet()

    companion object {
        private const val LOOP_THRESHOLD = 5
    }
}

interface ExplorationCallback {
    fun onPhaseChanged(phase: ExplorationPhase)
    fun onStateUpdated(stats: ExplorationStatistics)
    fun onLoginDetected(packageName: String, screenHash: String)
    fun onLoopDetected(screenHash: String)
    fun onExplorationComplete(stats: ExplorationStatistics)
    fun onError(message: String)
}

data class ExplorationState(
    val targetPackage: String = "",
    val screensExplored: Int = 0,
    val elementsDiscovered: Int = 0,
    val elementsClicked: Int = 0,
    val dangerousSkipped: Int = 0,
    val startTime: Long = System.currentTimeMillis()
)

data class ExplorationStatistics(
    val phase: ExplorationPhase,
    val screensExplored: Int,
    val elementsDiscovered: Int,
    val elementsClicked: Int,
    val coverage: Float,
    val dangerousSkipped: Int,
    val targetPackage: String
)
```

### 3.4.2 SafetyManager

```kotlin
package com.augmentalis.learnappcore.safety

class SafetyManager(
    private val callback: SafetyCallback? = null
) {
    private val doNotClickManager = DoNotClickManager()
    private val loginDetector = LoginDetector()
    private val dynamicRegionDetector = DynamicRegionDetector()
    private val loopPrevention = LoopPrevention()

    // Tracking
    private var dncCount = 0
    private var dynamicCount = 0
    private var menuCount = 0

    fun isDangerous(element: ElementInfo): Boolean {
        val reason = doNotClickManager.checkElement(element)
        if (reason != null) {
            dncCount++
            callback?.onDangerousElement(element, reason)
            return true
        }
        return false
    }

    fun checkForLogin(screenInfo: ScreenInfo): LoginType? {
        val result = loginDetector.analyze(screenInfo)
        if (result != null) {
            callback?.onLoginDetected(result, screenInfo.screenHash)
        }
        return result
    }

    fun checkForDynamicRegions(
        previous: ScreenInfo,
        current: ScreenInfo
    ): List<DynamicRegion> {
        val regions = dynamicRegionDetector.detect(previous, current)
        dynamicCount += regions.size
        regions.forEach { region ->
            callback?.onDynamicRegionConfirmed(region)
        }
        return regions
    }

    fun checkForLoop(screenHash: String): Boolean {
        val isLoop = loopPrevention.recordVisit(screenHash)
        if (isLoop) {
            callback?.onLoopDetected(screenHash, loopPrevention.getVisitCount(screenHash))
        }
        return isLoop
    }

    fun recordMenuDiscovery() {
        menuCount++
    }

    fun getStatistics(): SafetyStatistics {
        return SafetyStatistics(
            dncSkipped = dncCount,
            dynamicRegions = dynamicCount,
            menusFound = menuCount
        )
    }

    fun reset() {
        dncCount = 0
        dynamicCount = 0
        menuCount = 0
        loopPrevention.reset()
    }
}

interface SafetyCallback {
    fun onDangerousElement(element: ElementInfo, reason: DoNotClickReason)
    fun onLoginDetected(loginType: LoginType, screenHash: String)
    fun onDynamicRegionConfirmed(region: DynamicRegion)
    fun onLoopDetected(screenHash: String, visitCount: Int)
}

data class SafetyStatistics(
    val dncSkipped: Int,
    val dynamicRegions: Int,
    val menusFound: Int
)
```

---

## 3.5 Safety Implementation Classes

### 3.5.1 DoNotClickManager

```kotlin
package com.augmentalis.learnappcore.safety

class DoNotClickManager {

    fun checkElement(element: ElementInfo): DoNotClickReason? {
        val text = element.displayName.lowercase()
        val resourceId = element.resourceId.lowercase()

        // Check each category
        DESTRUCTIVE_KEYWORDS.forEach { keyword ->
            if (text.contains(keyword) || resourceId.contains(keyword)) {
                return DoNotClickReason.DESTRUCTIVE
            }
        }

        FINANCIAL_KEYWORDS.forEach { keyword ->
            if (text.contains(keyword) || resourceId.contains(keyword)) {
                return DoNotClickReason.FINANCIAL
            }
        }

        ACCOUNT_KEYWORDS.forEach { keyword ->
            if (text.contains(keyword) || resourceId.contains(keyword)) {
                return DoNotClickReason.ACCOUNT
            }
        }

        SYSTEM_KEYWORDS.forEach { keyword ->
            if (text.contains(keyword) || resourceId.contains(keyword)) {
                return DoNotClickReason.SYSTEM
            }
        }

        return null
    }

    companion object {
        val DESTRUCTIVE_KEYWORDS = listOf(
            "delete", "remove", "erase", "clear", "wipe",
            "destroy", "discard", "trash", "eliminate", "purge",
            "empty", "reset", "permanently"
        )

        val FINANCIAL_KEYWORDS = listOf(
            "pay", "purchase", "buy", "subscribe", "checkout",
            "order", "confirm payment", "add to cart",
            "proceed to payment", "complete purchase"
        )

        val ACCOUNT_KEYWORDS = listOf(
            "logout", "log out", "sign out", "signout",
            "deactivate", "disable account", "close account",
            "delete account", "remove account"
        )

        val SYSTEM_KEYWORDS = listOf(
            "uninstall", "factory reset", "format", "wipe device",
            "reset all", "clear all data", "restore defaults"
        )
    }
}

enum class DoNotClickReason {
    DESTRUCTIVE,
    FINANCIAL,
    ACCOUNT,
    SYSTEM
}
```

### 3.5.2 LoginDetector

```kotlin
package com.augmentalis.learnappcore.safety

class LoginDetector {

    fun analyze(screenInfo: ScreenInfo): LoginType? {
        var score = 0
        var detectedType: LoginType? = null

        screenInfo.elements.forEach { element ->
            // Check for password field
            if (element.className.contains("EditText", ignoreCase = true)) {
                val hints = listOf(element.text, element.contentDescription, element.resourceId)
                    .map { it.lowercase() }

                if (hints.any { it.contains("password") || it.contains("pin") }) {
                    score += 3
                    detectedType = LoginType.PASSWORD
                }

                if (hints.any { it.contains("username") || it.contains("email") }) {
                    score += 2
                }
            }

            // Check for login button
            if (element.className.contains("Button", ignoreCase = true)) {
                val text = element.displayName.lowercase()
                if (LOGIN_BUTTON_KEYWORDS.any { text.contains(it) }) {
                    score += 2
                }
            }

            // Check for biometric
            if (element.className.contains("Fingerprint", ignoreCase = true) ||
                element.displayName.lowercase().contains("fingerprint") ||
                element.displayName.lowercase().contains("face")) {
                score += 3
                detectedType = LoginType.BIOMETRIC
            }
        }

        // Check screen title/activity
        if (LOGIN_ACTIVITY_KEYWORDS.any {
            screenInfo.activityName.lowercase().contains(it)
        }) {
            score += 2
        }

        return if (score >= 3) detectedType ?: LoginType.PASSWORD else null
    }

    companion object {
        val LOGIN_BUTTON_KEYWORDS = listOf(
            "login", "log in", "sign in", "signin",
            "authenticate", "verify", "continue"
        )

        val LOGIN_ACTIVITY_KEYWORDS = listOf(
            "login", "signin", "auth", "credential",
            "password", "security"
        )
    }
}

enum class LoginType {
    PASSWORD,
    BIOMETRIC,
    PIN,
    PATTERN,
    TWO_FACTOR
}
```

---

## 3.6 Export Classes

### 3.6.1 AVUExporter

```kotlin
package com.augmentalis.learnappcore.export

class AVUExporter(
    private val context: Context,
    private val mode: ExportMode = ExportMode.USER
) {
    private val generator = AVUGenerator()
    private val synonymGenerator = SynonymGenerator()

    fun export(
        packageName: String,
        appName: String,
        screens: List<ScreenInfo>,
        elements: List<ElementInfo>,
        statistics: ExplorationStatistics
    ): ExportResult {
        try {
            // Generate AVU content
            val content = buildString {
                // Header
                append(generator.generateHeader())
                append("\n")

                // Metadata
                append(generator.generateMetadata(packageName, elements.size))
                append("\n---\n")

                // App record
                append(generator.generateAppRecord(packageName, appName))
                append("\n")

                // Statistics record
                append(generator.generateStatsRecord(statistics))
                append("\n")

                // Screen records
                screens.forEach { screen ->
                    append(generator.generateScreenRecord(screen))
                    append("\n")
                }

                // Element records
                elements.forEach { element ->
                    append(generator.generateElementRecord(element))
                    append("\n")
                }

                // Command records
                val commands = generateCommands(elements)
                commands.forEach { command ->
                    append(generator.generateCommandRecord(command))
                    append("\n")
                }

                // Synonyms section
                append("---\n")
                append("synonyms:\n")
                commands.filter { it.synonyms.isNotEmpty() }.forEach { cmd ->
                    append(cmd.toSynonymRecord())
                    append("\n")
                }
            }

            // Write file
            val filename = "$packageName.vos"
            val file = getExportFile(filename)

            val finalContent = when (mode) {
                ExportMode.DEVELOPER -> content  // Plain text
                ExportMode.USER -> encrypt(content)  // Encrypted
            }

            file.writeText(finalContent)

            return ExportResult.Success(file.absolutePath)

        } catch (e: Exception) {
            return ExportResult.Failure(e.message ?: "Export failed")
        }
    }

    private fun generateCommands(elements: List<ElementInfo>): List<CommandInfo> {
        return elements
            .filter { it.isInteractive }
            .map { element ->
                val phrase = generatePhrase(element)
                val action = determineAction(element)
                val synonyms = synonymGenerator.generate(phrase)

                CommandInfo(
                    commandId = "cmd_${element.uuid.take(8)}",
                    phrase = phrase,
                    action = action,
                    targetElementUuid = element.uuid,
                    confidence = calculateConfidence(element),
                    synonyms = synonyms,
                    screenHash = element.screenHash,
                    packageName = element.packageName
                )
            }
    }

    private fun generatePhrase(element: ElementInfo): String {
        val name = element.displayName.lowercase()
        return when {
            ElementAction.CLICK in element.actions -> "click $name"
            ElementAction.EDIT in element.actions -> "enter $name"
            ElementAction.SCROLL in element.actions -> "scroll $name"
            else -> name
        }
    }

    private fun determineAction(element: ElementInfo): String {
        return when {
            ElementAction.CLICK in element.actions -> "click"
            ElementAction.EDIT in element.actions -> "setText"
            ElementAction.SCROLL in element.actions -> "scroll"
            ElementAction.LONG_CLICK in element.actions -> "longClick"
            else -> "click"
        }
    }

    private fun calculateConfidence(element: ElementInfo): Float {
        var score = 0.5f

        // Has content description
        if (element.contentDescription.isNotEmpty()) score += 0.2f

        // Has text
        if (element.text.isNotEmpty()) score += 0.15f

        // Has resource ID
        if (element.resourceId.isNotEmpty()) score += 0.1f

        // Standard component
        if (element.shortClassName in STANDARD_COMPONENTS) score += 0.05f

        return score.coerceAtMost(1.0f)
    }

    private fun getExportFile(filename: String): File {
        val dir = File(context.getExternalFilesDir(null), "learned_apps")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, filename)
    }

    private fun encrypt(content: String): String {
        // Simple encryption for user edition
        // In production, use proper encryption
        return Base64.encodeToString(content.toByteArray(), Base64.DEFAULT)
    }

    companion object {
        val STANDARD_COMPONENTS = setOf(
            "Button", "TextView", "EditText", "ImageButton",
            "CheckBox", "RadioButton", "Switch", "SeekBar"
        )
    }
}

enum class ExportMode {
    USER,      // Encrypted output
    DEVELOPER  // Plain text output
}

sealed class ExportResult {
    data class Success(val path: String) : ExportResult()
    data class Failure(val error: String) : ExportResult()
}
```

---

## 3.7 Next Steps

Continue to [Chapter 4: UI Components & Theme](./04-UI-Components.md) for UI implementation details.

---

**End of Chapter 3**
