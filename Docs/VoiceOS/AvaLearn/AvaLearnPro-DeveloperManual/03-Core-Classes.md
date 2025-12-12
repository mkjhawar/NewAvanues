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

## 3.6 Screen & Element Identification System

JIT uses a sophisticated identification system to uniquely track screens (pages) and elements across exploration sessions.

### 3.6.1 ScreenFingerprint

Creates unique fingerprints for screen identification using structural and content analysis.

```kotlin
package com.augmentalis.jitlearning.identification

/**
 * Creates unique screen fingerprints for page identification.
 * Uses structural layout and content hashing to identify screens
 * even when content changes dynamically.
 */
class ScreenFingerprint {

    data class Fingerprint(
        val screenHash: String,      // Primary identifier: activityName + structuralHash
        val structuralHash: String,  // Layout-based hash (element types + positions)
        val contentHash: String,     // Text-based hash (element display text)
        val activityName: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Create a fingerprint for a screen.
     * @param activityName The activity class name
     * @param elements List of elements on the screen
     * @return Screen fingerprint with all hash components
     */
    fun create(activityName: String, elements: List<ElementInfo>): Fingerprint {
        val structuralHash = createStructuralHash(elements)
        val contentHash = createContentHash(elements)
        val screenHash = createScreenHash(activityName, structuralHash)

        return Fingerprint(
            screenHash = screenHash,
            structuralHash = structuralHash,
            contentHash = contentHash,
            activityName = activityName
        )
    }

    /**
     * Creates the primary screen hash from activity name and structure.
     * Format: 8-character hex string
     */
    private fun createScreenHash(activityName: String, structuralHash: String): String {
        val combined = "$activityName:$structuralHash"
        return combined.hashCode().toString(16).padStart(8, '0')
    }

    /**
     * Creates structural hash from element types and positions.
     * Uses top 50 elements sorted by screen position.
     * Each element contributes: "ClassName:Quadrant"
     */
    private fun createStructuralHash(elements: List<ElementInfo>): String {
        val structureSignature = elements
            .sortedBy { it.bounds.top * 10000 + it.bounds.left }  // Sort by position
            .take(50)  // Top 50 elements for performance
            .joinToString("|") { element ->
                val shortClass = element.className.substringAfterLast(".")
                val quadrant = getQuadrant(element)
                "$shortClass:$quadrant"
            }
        return structureSignature.hashCode().toString(16).padStart(8, '0')
    }

    /**
     * Creates content hash from element text.
     * Uses top 30 text-containing elements.
     */
    private fun createContentHash(elements: List<ElementInfo>): String {
        val contentSignature = elements
            .filter { it.text.isNotBlank() || it.contentDescription.isNotBlank() }
            .sortedBy { it.bounds.top }  // Sort by vertical position
            .take(30)  // Top 30 text elements
            .joinToString("|") { element ->
                element.displayName.take(20)  // Truncate long text
            }
        return contentSignature.hashCode().toString(16).padStart(8, '0')
    }

    /**
     * Determines which screen quadrant an element is in.
     * Divides screen into: TL (top-left), TR (top-right), BL (bottom-left), BR (bottom-right)
     */
    private fun getQuadrant(element: ElementInfo): String {
        val centerX = element.bounds.centerX
        val centerY = element.bounds.centerY

        // Assuming standard screen dimensions (adjusted dynamically in real implementation)
        val screenCenterX = 540  // Half of 1080
        val screenCenterY = 960  // Half of 1920

        return when {
            centerX < screenCenterX && centerY < screenCenterY -> "TL"
            centerX >= screenCenterX && centerY < screenCenterY -> "TR"
            centerX < screenCenterX && centerY >= screenCenterY -> "BL"
            else -> "BR"
        }
    }

    /**
     * Calculate similarity between two fingerprints.
     * @return Similarity score 0.0 to 1.0
     */
    fun calculateSimilarity(fp1: Fingerprint, fp2: Fingerprint): Float {
        // Same activity is required for similarity
        if (fp1.activityName != fp2.activityName) return 0f

        // Exact structural match
        if (fp1.structuralHash == fp2.structuralHash) return 1f

        // Partial match based on content
        if (fp1.contentHash == fp2.contentHash) return 0.8f

        // Different structure and content
        return 0.3f
    }

    companion object {
        // Thresholds for screen matching
        const val SIMILARITY_EXACT = 1.0f
        const val SIMILARITY_HIGH = 0.8f
        const val SIMILARITY_MEDIUM = 0.5f
        const val SIMILARITY_THRESHOLD = 0.7f  // Minimum to consider "same screen"
    }
}
```

### 3.6.2 UUIDGenerator

Generates unique identifiers for elements with multiple strategies.

```kotlin
package com.augmentalis.jitlearning.identification

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Generates unique identifiers (VUIDs) for elements.
 * Supports multiple strategies for different use cases.
 */
object UUIDGenerator {

    private val sequenceCounter = AtomicLong(0)

    /**
     * Standard random UUID.
     * Use for: General elements, one-time identifiers
     * Format: "550e8400-e29b-41d4-a716-446655440000"
     */
    fun generate(): String = UUID.randomUUID().toString()

    /**
     * UUID with prefix for categorization.
     * Use for: Categorized elements (btn, txt, img)
     * Format: "btn-550e8400-e29b-41d4-a716-446655440000"
     */
    fun generateWithPrefix(prefix: String): String {
        return "${prefix}-${UUID.randomUUID()}"
    }

    /**
     * Sequential UUID for ordered elements.
     * Use for: Elements that need ordering preserved
     * Format: "seq-1-1733875234567"
     */
    fun generateSequential(prefix: String = "seq"): String {
        val sequence = sequenceCounter.incrementAndGet()
        return "${prefix}-${sequence}-${System.currentTimeMillis()}"
    }

    /**
     * Content-based UUID derived from element content.
     * Use for: Elements with stable text content
     * Format: "content-4c6f67696e-1733875234567"
     * Note: Same content produces same hash portion
     */
    fun generateFromContent(content: String): String {
        val hash = content.hashCode().toString(16).padStart(8, '0')
        return "content-${hash}-${System.currentTimeMillis()}"
    }

    /**
     * Type-based UUID with optional name.
     * Use for: Typed elements like buttons, inputs
     * Format: "button-submit-44655440"
     */
    fun generateForType(type: String, name: String? = null): String {
        val suffix = name?.let {
            "-${it.replace(" ", "-").lowercase()}"
        } ?: ""
        return "${type.lowercase()}${suffix}-${UUID.randomUUID().toString().takeLast(8)}"
    }

    /**
     * Deterministic UUID from multiple inputs.
     * Use for: Stable identification across sessions
     * Format: "elm-a3f7b2c1-1234"
     */
    fun generateDeterministic(
        screenHash: String,
        className: String,
        resourceId: String,
        bounds: Bounds
    ): String {
        val signature = "$screenHash:$className:$resourceId:${bounds.toAVU()}"
        val hash = signature.hashCode().toString(16).padStart(8, '0')
        val posHash = (bounds.left + bounds.top).toString(16).padStart(4, '0')
        return "elm-${hash}-${posHash}"
    }

    /**
     * Reset sequence counter (for testing)
     */
    fun resetSequence() {
        sequenceCounter.set(0)
    }
}
```

### 3.6.3 ScreenTracker

Tracks visited screens and detects navigation patterns.

```kotlin
package com.augmentalis.jitlearning.identification

/**
 * Tracks screen visits and detects navigation patterns.
 * Used for loop prevention and exploration coverage.
 */
class ScreenTracker {

    data class ScreenVisit(
        val screenHash: String,
        val fingerprint: ScreenFingerprint.Fingerprint,
        val visitCount: Int,
        val firstVisit: Long,
        val lastVisit: Long
    )

    private val visitedScreens = mutableMapOf<String, ScreenVisit>()
    private val navigationHistory = mutableListOf<String>()
    private val fingerprinter = ScreenFingerprint()

    /**
     * Record a screen visit.
     * @return ScreenVisit with updated count
     */
    fun recordVisit(fingerprint: ScreenFingerprint.Fingerprint): ScreenVisit {
        val hash = fingerprint.screenHash
        navigationHistory.add(hash)

        val existing = visitedScreens[hash]
        val visit = if (existing != null) {
            existing.copy(
                visitCount = existing.visitCount + 1,
                lastVisit = System.currentTimeMillis()
            )
        } else {
            ScreenVisit(
                screenHash = hash,
                fingerprint = fingerprint,
                visitCount = 1,
                firstVisit = System.currentTimeMillis(),
                lastVisit = System.currentTimeMillis()
            )
        }

        visitedScreens[hash] = visit
        return visit
    }

    /**
     * Check if a screen has been visited.
     */
    fun hasVisited(screenHash: String): Boolean = screenHash in visitedScreens

    /**
     * Get visit count for a screen.
     */
    fun getVisitCount(screenHash: String): Int = visitedScreens[screenHash]?.visitCount ?: 0

    /**
     * Detect if we're in a navigation loop.
     * Checks last N navigation entries for repetition.
     */
    fun detectLoop(windowSize: Int = 10): LoopDetection? {
        if (navigationHistory.size < windowSize) return null

        val recent = navigationHistory.takeLast(windowSize)
        val frequency = recent.groupingBy { it }.eachCount()
        val maxFreq = frequency.maxByOrNull { it.value }

        return if (maxFreq != null && maxFreq.value >= 3) {
            LoopDetection(
                screenHash = maxFreq.key,
                occurrences = maxFreq.value,
                windowSize = windowSize,
                severity = when {
                    maxFreq.value >= 5 -> LoopSeverity.CRITICAL
                    maxFreq.value >= 4 -> LoopSeverity.WARNING
                    else -> LoopSeverity.INFO
                }
            )
        } else null
    }

    /**
     * Find similar screens to a given fingerprint.
     */
    fun findSimilar(fingerprint: ScreenFingerprint.Fingerprint): List<ScreenVisit> {
        return visitedScreens.values.filter { visit ->
            fingerprinter.calculateSimilarity(fingerprint, visit.fingerprint) >=
                ScreenFingerprint.SIMILARITY_THRESHOLD
        }
    }

    /**
     * Get exploration coverage statistics.
     */
    fun getCoverage(): CoverageStats {
        return CoverageStats(
            uniqueScreens = visitedScreens.size,
            totalVisits = navigationHistory.size,
            avgVisitsPerScreen = if (visitedScreens.isNotEmpty()) {
                navigationHistory.size.toFloat() / visitedScreens.size
            } else 0f
        )
    }

    fun reset() {
        visitedScreens.clear()
        navigationHistory.clear()
    }

    data class LoopDetection(
        val screenHash: String,
        val occurrences: Int,
        val windowSize: Int,
        val severity: LoopSeverity
    )

    enum class LoopSeverity { INFO, WARNING, CRITICAL }

    data class CoverageStats(
        val uniqueScreens: Int,
        val totalVisits: Int,
        val avgVisitsPerScreen: Float
    )
}
```

### 3.6.4 Identification Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      SCREEN CHANGE EVENT                                │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  1. EXTRACT ACTIVITY NAME                                               │
│     ActivityName = "com.example.app.MainActivity" → "MainActivity"      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  2. COLLECT ELEMENTS from AccessibilityNodeInfo tree                    │
│     └─ Traverse tree, extract: className, text, bounds, resourceId      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  3. CREATE STRUCTURAL HASH                                              │
│     a. Sort elements by position (top*10000 + left)                     │
│     b. Take top 50 elements                                             │
│     c. For each: "ClassName:Quadrant" (e.g., "Button:TL|TextView:TR")   │
│     d. Join with "|", hash to 8-char hex → "a3f7b2c1"                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  4. CREATE CONTENT HASH                                                 │
│     a. Filter elements with text/contentDescription                     │
│     b. Take top 30 by vertical position                                 │
│     c. Join display names (truncated to 20 chars)                       │
│     d. Hash to 8-char hex → "b2c1d3e4"                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  5. CREATE SCREEN HASH                                                  │
│     Combined = "MainActivity:a3f7b2c1"                                  │
│     Hash to 8-char hex → "f5e6d7c8"                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  6. GENERATE ELEMENT VUIDs                                              │
│     For each element:                                                   │
│     ├─ Deterministic: "elm-{hash}-{posHash}" (stable across sessions)   │
│     ├─ Type-based: "button-submit-44655440"                             │
│     └─ Content-based: "content-4c6f67696e-timestamp"                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  7. SCREEN TRACKER LOOKUP                                               │
│     ├─ If NEW screen → Add to visited map, visitCount = 1               │
│     ├─ If EXISTING → Increment visitCount                               │
│     └─ Check for LOOPS → If same screen 3+ times in last 10 → Warning   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.6.5 Hash Examples

| Screen State | Activity | Structural Hash | Content Hash | Screen Hash |
|-------------|----------|-----------------|--------------|-------------|
| Login page | LoginActivity | `a1b2c3d4` | `e5f6g7h8` | `f1a2b3c4` |
| Same login, different error | LoginActivity | `a1b2c3d4` | `i9j0k1l2` | `f1a2b3c4` |
| Home page | MainActivity | `m3n4o5p6` | `q7r8s9t0` | `d5e6f7g8` |
| Home scrolled | MainActivity | `m3n4o5p6` | `u1v2w3x4` | `d5e6f7g8` |
| Settings page | SettingsActivity | `y5z6a7b8` | `c9d0e1f2` | `a9b8c7d6` |

**Key observations:**
- Same structural hash + activity → Same screen hash (ignores content changes)
- Scrolling doesn't change screen identity (structural layout preserved)
- Different activities always produce different screen hashes

### 3.6.6 VUID Strategy Selection

| Scenario | Strategy | Example Output |
|----------|----------|----------------|
| General element | `generate()` | `550e8400-e29b-41d4-a716-446655440000` |
| Button with label | `generateForType("button", "Submit")` | `button-submit-44655440` |
| Text element | `generateFromContent("Login")` | `content-4c6f67696e-1733875234567` |
| Stable ID needed | `generateDeterministic(...)` | `elm-a3f7b2c1-0540` |
| Ordered list items | `generateSequential("item")` | `item-1-1733875234567` |

---

## 3.7 Dynamic Content & Scroll Tracking

JIT efficiently handles scrollable containers and dynamic content without full rescans.

### 3.7.1 The Challenge

| Scenario | Problem |
|----------|---------|
| RecyclerView with 100+ items | Can't scan all items at once |
| Cards with dynamic content | Content changes between visits |
| Infinite scroll feeds | New items continuously added |
| ViewPager/TabLayout | Content changes on swipe |

### 3.7.2 ScrollableContainerTracker

```kotlin
package com.augmentalis.jitlearning.tracking

/**
 * Tracks scrollable containers and their visible content.
 * Enables incremental updates without full rescans.
 */
class ScrollableContainerTracker {

    data class Container(
        val containerId: String,
        val className: String,           // RecyclerView, ScrollView, etc.
        val bounds: Bounds,
        val scrollState: ScrollState,
        val visibleItems: List<String>,  // VUIDs of visible elements
        val estimatedTotalItems: Int,
        val scrollPosition: Float        // 0.0 to 1.0
    )

    data class ScrollState(
        val canScrollUp: Boolean,
        val canScrollDown: Boolean,
        val canScrollLeft: Boolean,
        val canScrollRight: Boolean
    )

    private val containers = mutableMapOf<String, Container>()
    private val elementToContainer = mutableMapOf<String, String>()

    // Scrollable container class patterns
    private val SCROLLABLE_CLASSES = listOf(
        "RecyclerView", "ListView", "ScrollView", "HorizontalScrollView",
        "NestedScrollView", "ViewPager", "ViewPager2", "LazyColumn", "LazyRow"
    )

    /**
     * Detect if an element is a scrollable container.
     */
    fun isScrollableContainer(element: ElementInfo): Boolean {
        return SCROLLABLE_CLASSES.any {
            element.className.contains(it, ignoreCase = true)
        } || element.actions.contains(ElementAction.SCROLL)
    }

    /**
     * Register a scrollable container for tracking.
     */
    fun registerContainer(element: ElementInfo, children: List<ElementInfo>): Container {
        val containerId = generateContainerId(element)

        val scrollState = ScrollState(
            canScrollUp = element.className.contains("RecyclerView") ||
                         element.className.contains("ScrollView"),
            canScrollDown = true,  // Determined from AccessibilityNodeInfo
            canScrollLeft = element.className.contains("Horizontal") ||
                           element.className.contains("ViewPager"),
            canScrollRight = element.className.contains("Horizontal")
        )

        val container = Container(
            containerId = containerId,
            className = element.shortClassName,
            bounds = element.bounds,
            scrollState = scrollState,
            visibleItems = children.map { it.uuid },
            estimatedTotalItems = estimateItemCount(element, children.size),
            scrollPosition = 0f
        )

        containers[containerId] = container
        children.forEach { child ->
            elementToContainer[child.uuid] = containerId
        }

        return container
    }

    /**
     * Update container after scroll event.
     * Only processes NEW elements that scrolled into view.
     */
    fun onScrollEvent(
        containerId: String,
        newVisibleElements: List<ElementInfo>,
        scrollDelta: Int
    ): ScrollUpdate {
        val container = containers[containerId] ?: return ScrollUpdate.empty()

        val previousVUIDs = container.visibleItems.toSet()
        val currentVUIDs = newVisibleElements.map { it.uuid }.toSet()

        // Find elements that are NEW (scrolled into view)
        val addedVUIDs = currentVUIDs - previousVUIDs
        val removedVUIDs = previousVUIDs - currentVUIDs

        // Update container state
        val newScrollPosition = calculateScrollPosition(container, scrollDelta)
        containers[containerId] = container.copy(
            visibleItems = newVisibleElements.map { it.uuid },
            scrollPosition = newScrollPosition
        )

        // Only return the NEW elements for processing
        val addedElements = newVisibleElements.filter { it.uuid in addedVUIDs }

        return ScrollUpdate(
            containerId = containerId,
            addedElements = addedElements,
            removedVUIDs = removedVUIDs.toList(),
            scrollPosition = newScrollPosition,
            isIncremental = true  // Not a full rescan
        )
    }

    /**
     * Create container-aware structural hash.
     * Hashes container TYPE + approximate item count, not individual items.
     */
    fun createContainerHash(element: ElementInfo, visibleItemCount: Int): String {
        // Use container type and approximate count instead of item content
        val signature = "${element.shortClassName}:~${roundToNearest(visibleItemCount, 10)}items"
        return signature.hashCode().toString(16).padStart(8, '0')
    }

    private fun generateContainerId(element: ElementInfo): String {
        return if (element.resourceId.isNotEmpty()) {
            "container-${element.resourceId.hashCode().toString(16)}"
        } else {
            "container-${element.bounds.toAVU().hashCode().toString(16)}"
        }
    }

    private fun estimateItemCount(container: ElementInfo, visibleCount: Int): Int {
        // Heuristic: if we can scroll, assume more items exist
        return if (container.actions.contains(ElementAction.SCROLL)) {
            visibleCount * 3  // Estimate 3x visible as total
        } else {
            visibleCount
        }
    }

    private fun calculateScrollPosition(container: Container, delta: Int): Float {
        val estimatedHeight = container.estimatedTotalItems * 100  // ~100px per item
        return (container.scrollPosition + delta.toFloat() / estimatedHeight)
            .coerceIn(0f, 1f)
    }

    private fun roundToNearest(value: Int, nearest: Int): Int {
        return ((value + nearest / 2) / nearest) * nearest
    }

    data class ScrollUpdate(
        val containerId: String,
        val addedElements: List<ElementInfo>,
        val removedVUIDs: List<String>,
        val scrollPosition: Float,
        val isIncremental: Boolean
    ) {
        companion object {
            fun empty() = ScrollUpdate("", emptyList(), emptyList(), 0f, false)
        }
    }
}
```

### 3.7.3 DynamicContentDetector

```kotlin
package com.augmentalis.jitlearning.tracking

/**
 * Detects and tracks dynamic content regions (ads, feeds, notifications).
 * Avoids re-learning content that changes frequently.
 */
class DynamicContentDetector {

    data class DynamicRegion(
        val regionId: String,
        val bounds: Bounds,
        val changeFrequency: ChangeFrequency,
        val contentType: DynamicContentType,
        val lastContent: String,
        val changeCount: Int
    )

    enum class ChangeFrequency { STATIC, LOW, MEDIUM, HIGH, REALTIME }

    enum class DynamicContentType {
        ADVERTISEMENT,      // Ad banners, promoted content
        LIVE_FEED,          // Social feeds, news
        NOTIFICATION,       // Toast, snackbar, badges
        TIMER,              // Countdown, elapsed time
        ANIMATION,          // Loading spinners, progress
        USER_CONTENT        // User-generated, frequently updated
    }

    private val regions = mutableMapOf<String, DynamicRegion>()
    private val changeHistory = mutableMapOf<String, MutableList<Long>>()

    // Patterns that indicate dynamic content
    private val DYNAMIC_INDICATORS = mapOf(
        "ad" to DynamicContentType.ADVERTISEMENT,
        "banner" to DynamicContentType.ADVERTISEMENT,
        "promoted" to DynamicContentType.ADVERTISEMENT,
        "feed" to DynamicContentType.LIVE_FEED,
        "timeline" to DynamicContentType.LIVE_FEED,
        "notification" to DynamicContentType.NOTIFICATION,
        "toast" to DynamicContentType.NOTIFICATION,
        "snackbar" to DynamicContentType.NOTIFICATION,
        "timer" to DynamicContentType.TIMER,
        "countdown" to DynamicContentType.TIMER,
        "progress" to DynamicContentType.ANIMATION,
        "loading" to DynamicContentType.ANIMATION,
        "spinner" to DynamicContentType.ANIMATION
    )

    /**
     * Compare two screen snapshots to detect dynamic regions.
     */
    fun detectDynamicRegions(
        previous: List<ElementInfo>,
        current: List<ElementInfo>,
        timeDelta: Long
    ): List<DynamicRegion> {
        val previousMap = previous.associateBy { it.uuid }
        val detected = mutableListOf<DynamicRegion>()

        current.forEach { element ->
            val prev = previousMap[element.uuid]
            if (prev != null && hasContentChanged(prev, element)) {
                val region = trackChange(element, timeDelta)
                if (region.changeFrequency != ChangeFrequency.STATIC) {
                    detected.add(region)
                }
            }
        }

        return detected
    }

    /**
     * Check if an element's content has changed.
     */
    private fun hasContentChanged(prev: ElementInfo, current: ElementInfo): Boolean {
        return prev.text != current.text ||
               prev.contentDescription != current.contentDescription
    }

    /**
     * Track content change and update frequency classification.
     */
    private fun trackChange(element: ElementInfo, timeDelta: Long): DynamicRegion {
        val regionId = element.uuid
        val now = System.currentTimeMillis()

        // Record change timestamp
        val history = changeHistory.getOrPut(regionId) { mutableListOf() }
        history.add(now)

        // Keep only last 10 changes for frequency calculation
        while (history.size > 10) history.removeAt(0)

        val frequency = calculateFrequency(history)
        val contentType = detectContentType(element)

        val region = DynamicRegion(
            regionId = regionId,
            bounds = element.bounds,
            changeFrequency = frequency,
            contentType = contentType,
            lastContent = element.displayName,
            changeCount = history.size
        )

        regions[regionId] = region
        return region
    }

    /**
     * Calculate change frequency from history.
     */
    private fun calculateFrequency(history: List<Long>): ChangeFrequency {
        if (history.size < 2) return ChangeFrequency.STATIC

        val intervals = history.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()

        return when {
            avgInterval < 1000 -> ChangeFrequency.REALTIME    // < 1 second
            avgInterval < 5000 -> ChangeFrequency.HIGH        // < 5 seconds
            avgInterval < 30000 -> ChangeFrequency.MEDIUM     // < 30 seconds
            avgInterval < 300000 -> ChangeFrequency.LOW       // < 5 minutes
            else -> ChangeFrequency.STATIC
        }
    }

    /**
     * Detect content type from element properties.
     */
    private fun detectContentType(element: ElementInfo): DynamicContentType {
        val identifiers = listOf(
            element.resourceId.lowercase(),
            element.className.lowercase(),
            element.contentDescription.lowercase()
        ).joinToString(" ")

        DYNAMIC_INDICATORS.forEach { (pattern, type) ->
            if (identifiers.contains(pattern)) return type
        }

        return DynamicContentType.USER_CONTENT
    }

    /**
     * Check if an element should be excluded from learning due to high dynamism.
     */
    fun shouldExcludeFromLearning(elementId: String): Boolean {
        val region = regions[elementId] ?: return false
        return region.changeFrequency in listOf(
            ChangeFrequency.HIGH,
            ChangeFrequency.REALTIME
        )
    }

    /**
     * Get all detected dynamic regions.
     */
    fun getDynamicRegions(): List<DynamicRegion> = regions.values.toList()
}
```

### 3.7.4 ElementAnchor System

```kotlin
package com.augmentalis.jitlearning.tracking

/**
 * Creates stable anchors for elements to enable re-identification
 * across scroll positions and screen revisits.
 */
class ElementAnchor {

    data class Anchor(
        val primaryKey: String,          // Most stable identifier
        val fallbackKeys: List<String>,  // Alternative identifiers
        val confidence: Float,           // 0.0 to 1.0
        val anchorType: AnchorType
    )

    enum class AnchorType {
        RESOURCE_ID,        // Android resource ID (most stable)
        CONTENT_HASH,       // Hash of text content
        STRUCTURAL,         // Class + relative position
        COMPOSITE           // Multiple signals combined
    }

    /**
     * Create an anchor for element re-identification.
     */
    fun createAnchor(element: ElementInfo, parent: ElementInfo?): Anchor {
        val keys = mutableListOf<String>()
        var primaryKey: String
        var anchorType: AnchorType
        var confidence: Float

        // Priority 1: Resource ID (most stable)
        if (element.resourceId.isNotEmpty()) {
            primaryKey = "res:${element.resourceId}"
            anchorType = AnchorType.RESOURCE_ID
            confidence = 0.95f
            keys.add(primaryKey)
        }
        // Priority 2: Content description (stable for accessibility-aware apps)
        else if (element.contentDescription.isNotEmpty()) {
            primaryKey = "desc:${element.contentDescription.hashCode().toString(16)}"
            anchorType = AnchorType.CONTENT_HASH
            confidence = 0.85f
            keys.add(primaryKey)
        }
        // Priority 3: Text content (less stable, may change)
        else if (element.text.isNotEmpty()) {
            primaryKey = "text:${element.text.take(50).hashCode().toString(16)}"
            anchorType = AnchorType.CONTENT_HASH
            confidence = 0.7f
            keys.add(primaryKey)
        }
        // Priority 4: Structural position (fallback)
        else {
            val relativePos = parent?.let {
                calculateRelativePosition(element, it)
            } ?: "root"
            primaryKey = "struct:${element.shortClassName}:$relativePos"
            anchorType = AnchorType.STRUCTURAL
            confidence = 0.5f
            keys.add(primaryKey)
        }

        // Add fallback keys
        if (element.text.isNotEmpty() && anchorType != AnchorType.CONTENT_HASH) {
            keys.add("text:${element.text.take(50).hashCode().toString(16)}")
        }
        keys.add("class:${element.shortClassName}:${element.bounds.centerX},${element.bounds.centerY}")

        return Anchor(
            primaryKey = primaryKey,
            fallbackKeys = keys.drop(1),
            confidence = confidence,
            anchorType = anchorType
        )
    }

    /**
     * Match an element to existing anchors.
     */
    fun findMatch(
        element: ElementInfo,
        existingAnchors: Map<String, Anchor>
    ): MatchResult? {
        val newAnchor = createAnchor(element, null)

        // Try primary key match
        existingAnchors.forEach { (vuid, anchor) ->
            if (anchor.primaryKey == newAnchor.primaryKey) {
                return MatchResult(vuid, anchor.confidence, MatchType.PRIMARY)
            }
        }

        // Try fallback key matches
        existingAnchors.forEach { (vuid, anchor) ->
            val fallbackMatch = newAnchor.fallbackKeys.intersect(anchor.fallbackKeys.toSet())
            if (fallbackMatch.isNotEmpty()) {
                return MatchResult(vuid, anchor.confidence * 0.8f, MatchType.FALLBACK)
            }
        }

        return null
    }

    private fun calculateRelativePosition(element: ElementInfo, parent: ElementInfo): String {
        val relX = ((element.bounds.centerX - parent.bounds.left).toFloat() /
                   parent.bounds.width * 10).toInt()
        val relY = ((element.bounds.centerY - parent.bounds.top).toFloat() /
                   parent.bounds.height * 10).toInt()
        return "${relX}x${relY}"
    }

    data class MatchResult(
        val matchedVUID: String,
        val confidence: Float,
        val matchType: MatchType
    )

    enum class MatchType { PRIMARY, FALLBACK }
}
```

### 3.7.5 Incremental Update Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SCROLL EVENT RECEIVED                                │
│                AccessibilityEvent.TYPE_VIEW_SCROLLED                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  1. IDENTIFY CONTAINER                                                  │
│     └─ Find scrollable parent (RecyclerView, ScrollView, etc.)          │
│     └─ Get containerId from ScrollableContainerTracker                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  2. GET VISIBLE VIEWPORT                                                │
│     └─ Only query elements within container bounds                      │
│     └─ Skip elements outside visible viewport                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  3. DIFFERENTIAL COMPARISON                                             │
│     ├─ Previous visible: [A, B, C, D, E]                                │
│     ├─ Current visible:  [C, D, E, F, G]                                │
│     ├─ REMOVED (scrolled out): [A, B] → Cache, don't delete             │
│     └─ ADDED (scrolled in): [F, G] → Process these ONLY                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  4. ANCHOR MATCHING for NEW elements                                    │
│     └─ Check if F, G were previously seen (different scroll position)   │
│     └─ Match by resourceId → contentDescription → text → structure      │
│     └─ If match found: reuse existing VUID, merge data                  │
│     └─ If no match: assign new VUID                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  5. UPDATE TRACKING STATE                                               │
│     └─ Update container.visibleItems = [C, D, E, F, G]                  │
│     └─ Update container.scrollPosition                                  │
│     └─ Keep A, B in cache (may scroll back)                             │
│     └─ Screen hash UNCHANGED (container structure same)                 │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.7.6 Dynamic Content Handling Summary

| Content Type | Detection | Handling |
|--------------|-----------|----------|
| **Static Content** | No changes between scans | Full learning, high confidence |
| **Scrollable Lists** | `RecyclerView`, `ScrollView` | Incremental viewport tracking |
| **Dynamic Cards** | Content changes within bounds | Anchor by resourceId, not content |
| **Live Feeds** | High change frequency detected | Mark region, lower confidence |
| **Ads/Promotions** | resourceId contains "ad", "banner" | Exclude from command learning |
| **Animations** | Rapid changes (<1s interval) | Exclude from element tracking |
| **User Input** | EditText content changes | Track field, not content |

---

## 3.8 Export Classes

### 3.8.1 AVUExporter

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
