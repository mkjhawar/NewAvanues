/**
 * ExplorationState.kt - State management for app exploration
 *
 * Part of LearnApp Exploration System.
 * Tracks exploration progress, visited elements, and navigation history.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.exploration

import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.safety.*

/**
 * Exploration phase enum.
 */
enum class ExplorationPhase {
    /** Not started */
    IDLE,

    /** Initializing - connecting to JIT service */
    INITIALIZING,

    /** Capturing initial screen */
    INITIAL_CAPTURE,

    /** Actively exploring screens */
    EXPLORING,

    /** Waiting for user action (login, etc.) */
    WAITING_USER,

    /** Paused by user or system */
    PAUSED,

    /** Generating commands from captured data */
    GENERATING,

    /** Exporting to AVU format */
    EXPORTING,

    /** Exploration complete */
    COMPLETED,

    /** Error state */
    ERROR
}

/**
 * Navigation record for tracking screen transitions.
 *
 * @property fromScreenHash Source screen hash
 * @property toScreenHash Destination screen hash
 * @property triggerElementUuid Element that triggered navigation
 * @property triggerLabel Element label
 * @property timestamp When navigation occurred
 */
data class NavigationRecord(
    val fromScreenHash: String,
    val toScreenHash: String,
    val triggerElementUuid: String,
    val triggerLabel: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Generate NAV IPC line for AVU export.
     *
     * Format: NAV:from_hash:to_hash:trigger_uuid:trigger_label:timestamp
     */
    fun toNavLine(): String {
        val safeLabel = triggerLabel.take(30).replace(":", "_")
        return "NAV:$fromScreenHash:$toScreenHash:$triggerElementUuid:$safeLabel:$timestamp"
    }

    companion object {
        /**
         * Parse from AVU NAV line.
         */
        fun fromAvuLine(line: String): NavigationRecord? {
            if (!line.startsWith("NAV:")) return null
            val parts = line.split(":")
            if (parts.size < 6) return null

            return NavigationRecord(
                fromScreenHash = parts[1],
                toScreenHash = parts[2],
                triggerElementUuid = parts[3],
                triggerLabel = parts[4],
                timestamp = parts[5].toLongOrNull() ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Exploration statistics.
 */
data class ExplorationStats(
    val screensExplored: Int = 0,
    val elementsDiscovered: Int = 0,
    val elementsClicked: Int = 0,
    val commandsGenerated: Int = 0,
    val navigationCount: Int = 0,
    val dangerousElementsSkipped: Int = 0,
    val dynamicRegionsDetected: Int = 0,
    val avgDepth: Float = 0f,
    val maxDepth: Int = 0,
    val durationMs: Long = 0,
    val coverage: Float = 0f
) {
    /**
     * Generate STA IPC line for AVU export.
     *
     * Format: STA:screens:elements:commands:avg_depth:max_depth:coverage
     */
    fun toStaLine(): String {
        return "STA:$screensExplored:$elementsDiscovered:$commandsGenerated:${"%.1f".format(avgDepth)}:$maxDepth:${"%.1f".format(coverage)}"
    }

    companion object {
        /**
         * Parse from AVU STA line.
         */
        fun fromAvuLine(line: String): ExplorationStats? {
            if (!line.startsWith("STA:")) return null
            val parts = line.split(":")
            if (parts.size < 7) return null

            return ExplorationStats(
                screensExplored = parts[1].toIntOrNull() ?: 0,
                elementsDiscovered = parts[2].toIntOrNull() ?: 0,
                commandsGenerated = parts[3].toIntOrNull() ?: 0,
                avgDepth = parts[4].toFloatOrNull() ?: 0f,
                maxDepth = parts[5].toIntOrNull() ?: 0,
                coverage = parts[6].toFloatOrNull() ?: 0f
            )
        }
    }
}

/**
 * Complete exploration state.
 *
 * Central state management for exploration session.
 */
class ExplorationState(
    val packageName: String,
    val appName: String
) {
    // Phase tracking
    var phase: ExplorationPhase = ExplorationPhase.IDLE
        private set

    // Timestamps
    var startTimestamp: Long = 0
        private set
    var lastActionTimestamp: Long = 0
        private set

    // Current position
    var currentScreenHash: String = ""
        private set
    var currentActivityName: String = ""
        private set

    // Depth tracking (for DFS)
    var currentDepth: Int = 0
        private set
    private var maxDepth: Int = 0
    private val depthHistory = mutableListOf<Int>()

    // Element tracking
    private val discoveredElements = mutableMapOf<String, ElementInfo>() // uuid -> element
    private val clickedElements = mutableSetOf<String>() // stable IDs of clicked elements
    private val dangerousElements = mutableListOf<Pair<ElementInfo, DoNotClickReason>>()

    // Screen tracking
    private val screenFingerprints = mutableMapOf<String, ScreenFingerprint>()
    private val screenElements = mutableMapOf<String, MutableSet<String>>() // screenHash -> element UUIDs

    // Navigation tracking
    private val navigationHistory = mutableListOf<NavigationRecord>()
    private val backStack = mutableListOf<String>() // Screen hash stack for back navigation

    // Safety tracking
    private val dynamicRegions = mutableListOf<DynamicRegion>()
    private val discoveredMenus = mutableListOf<DiscoveredMenu>()

    // Generated commands (uuid -> command trigger)
    private val generatedCommands = mutableMapOf<String, String>()

    // Callbacks
    private var stateCallback: ExplorationStateCallback? = null

    /**
     * Set state callback.
     */
    fun setCallback(callback: ExplorationStateCallback) {
        this.stateCallback = callback
    }

    /**
     * Start exploration.
     */
    fun start() {
        require(phase == ExplorationPhase.IDLE) { "Cannot start exploration in phase $phase" }

        phase = ExplorationPhase.INITIALIZING
        startTimestamp = System.currentTimeMillis()
        lastActionTimestamp = startTimestamp

        stateCallback?.onPhaseChanged(ExplorationPhase.IDLE, phase)
    }

    /**
     * Transition to exploration phase.
     */
    fun beginExploring() {
        val oldPhase = phase
        phase = ExplorationPhase.EXPLORING
        stateCallback?.onPhaseChanged(oldPhase, phase)
    }

    /**
     * Pause exploration.
     */
    fun pause() {
        if (phase == ExplorationPhase.EXPLORING || phase == ExplorationPhase.WAITING_USER) {
            val oldPhase = phase
            phase = ExplorationPhase.PAUSED
            stateCallback?.onPhaseChanged(oldPhase, phase)
        }
    }

    /**
     * Resume exploration.
     */
    fun resume() {
        if (phase == ExplorationPhase.PAUSED) {
            val oldPhase = phase
            phase = ExplorationPhase.EXPLORING
            stateCallback?.onPhaseChanged(oldPhase, phase)
        }
    }

    /**
     * Enter waiting state (login screen, etc.)
     */
    fun waitForUser(reason: String) {
        val oldPhase = phase
        phase = ExplorationPhase.WAITING_USER
        stateCallback?.onPhaseChanged(oldPhase, phase)
        stateCallback?.onWaitingForUser(reason)
    }

    /**
     * Complete exploration.
     */
    fun complete() {
        val oldPhase = phase
        phase = ExplorationPhase.COMPLETED
        stateCallback?.onPhaseChanged(oldPhase, phase)
        stateCallback?.onExplorationComplete(getStats())
    }

    /**
     * Set error state.
     */
    fun setError(message: String) {
        val oldPhase = phase
        phase = ExplorationPhase.ERROR
        stateCallback?.onPhaseChanged(oldPhase, phase)
        stateCallback?.onError(message)
    }

    /**
     * Record screen visit.
     */
    fun recordScreen(fingerprint: ScreenFingerprint) {
        val previousScreen = currentScreenHash
        currentScreenHash = fingerprint.screenHash
        currentActivityName = fingerprint.activityName

        screenFingerprints[fingerprint.screenHash] = fingerprint

        // Initialize element set for screen
        if (fingerprint.screenHash !in screenElements) {
            screenElements[fingerprint.screenHash] = mutableSetOf()
        }

        // Track navigation if we came from another screen
        if (previousScreen.isNotEmpty() && previousScreen != fingerprint.screenHash) {
            // Navigation will be recorded by recordNavigation()
            backStack.add(previousScreen)
            currentDepth++
            if (currentDepth > maxDepth) {
                maxDepth = currentDepth
            }
        }

        lastActionTimestamp = System.currentTimeMillis()
        stateCallback?.onScreenChanged(previousScreen, fingerprint)
    }

    /**
     * Record element discovery.
     */
    fun recordElement(element: ElementInfo) {
        val uuid = element.uuid ?: element.stableId()
        discoveredElements[uuid] = element

        screenElements[currentScreenHash]?.add(uuid)

        lastActionTimestamp = System.currentTimeMillis()
    }

    /**
     * Record multiple elements.
     */
    fun recordElements(elements: List<ElementInfo>) {
        for (element in elements) {
            recordElement(element)
        }
        stateCallback?.onElementsDiscovered(elements.size)
    }

    /**
     * Record element click.
     */
    fun recordClick(element: ElementInfo) {
        clickedElements.add(element.stableId())
        lastActionTimestamp = System.currentTimeMillis()
        stateCallback?.onElementClicked(element)
    }

    /**
     * Check if element has been clicked.
     */
    fun hasClicked(element: ElementInfo): Boolean {
        return element.stableId() in clickedElements
    }

    /**
     * Record dangerous element.
     */
    fun recordDangerousElement(element: ElementInfo, reason: DoNotClickReason) {
        dangerousElements.add(element to reason)
    }

    /**
     * Record navigation transition.
     */
    fun recordNavigation(fromScreen: String, toScreen: String, triggerElement: ElementInfo) {
        val record = NavigationRecord(
            fromScreenHash = fromScreen,
            toScreenHash = toScreen,
            triggerElementUuid = triggerElement.uuid ?: triggerElement.stableId(),
            triggerLabel = triggerElement.getDisplayName()
        )
        navigationHistory.add(record)
        stateCallback?.onNavigation(record)
    }

    /**
     * Record back navigation.
     */
    fun recordBackNavigation(): String? {
        if (backStack.isEmpty()) return null

        val previousScreen = backStack.removeAt(backStack.size - 1)
        currentDepth = maxOf(0, currentDepth - 1)

        return previousScreen
    }

    /**
     * Record dynamic region.
     */
    fun recordDynamicRegion(region: DynamicRegion) {
        dynamicRegions.add(region)
    }

    /**
     * Record discovered menu.
     */
    fun recordMenu(menu: DiscoveredMenu) {
        discoveredMenus.add(menu)
    }

    /**
     * Record generated command.
     */
    fun recordCommand(uuid: String, trigger: String) {
        generatedCommands[uuid] = trigger
    }

    /**
     * Get exploration statistics.
     */
    fun getStats(): ExplorationStats {
        val duration = if (startTimestamp > 0) {
            System.currentTimeMillis() - startTimestamp
        } else 0

        val avgDepth = if (depthHistory.isNotEmpty()) {
            depthHistory.average().toFloat()
        } else {
            currentDepth.toFloat()
        }

        return ExplorationStats(
            screensExplored = screenFingerprints.size,
            elementsDiscovered = discoveredElements.size,
            elementsClicked = clickedElements.size,
            commandsGenerated = generatedCommands.size,
            navigationCount = navigationHistory.size,
            dangerousElementsSkipped = dangerousElements.size,
            dynamicRegionsDetected = dynamicRegions.size,
            avgDepth = avgDepth,
            maxDepth = maxDepth,
            durationMs = duration,
            coverage = calculateCoverage()
        )
    }

    /**
     * Calculate exploration coverage.
     */
    private fun calculateCoverage(): Float {
        if (discoveredElements.isEmpty()) return 0f

        val clickedCount = clickedElements.size.toFloat()
        val clickableCount = discoveredElements.values.count {
            it.isClickable && !dangerousElements.any { (e, _) -> e.stableId() == it.stableId() }
        }.toFloat()

        return if (clickableCount > 0) {
            (clickedCount / clickableCount) * 100f
        } else {
            100f
        }
    }

    /**
     * Get all discovered elements.
     */
    fun getElements(): List<ElementInfo> = discoveredElements.values.toList()

    /**
     * Get elements for a specific screen.
     */
    fun getElementsForScreen(screenHash: String): List<ElementInfo> {
        val uuids = screenElements[screenHash] ?: return emptyList()
        return uuids.mapNotNull { discoveredElements[it] }
    }

    /**
     * Get navigation history.
     */
    fun getNavigationHistory(): List<NavigationRecord> = navigationHistory.toList()

    /**
     * Get dangerous elements.
     */
    fun getDangerousElements(): List<Pair<ElementInfo, DoNotClickReason>> = dangerousElements.toList()

    /**
     * Get screen fingerprints.
     */
    fun getScreenFingerprints(): List<ScreenFingerprint> = screenFingerprints.values.toList()

    /**
     * Get dynamic regions.
     */
    fun getDynamicRegions(): List<DynamicRegion> = dynamicRegions.toList()

    /**
     * Get menus.
     */
    fun getMenus(): List<DiscoveredMenu> = discoveredMenus.toList()

    /**
     * Reset exploration state.
     */
    fun reset() {
        phase = ExplorationPhase.IDLE
        startTimestamp = 0
        lastActionTimestamp = 0
        currentScreenHash = ""
        currentActivityName = ""
        currentDepth = 0
        maxDepth = 0
        depthHistory.clear()

        discoveredElements.clear()
        clickedElements.clear()
        dangerousElements.clear()
        screenFingerprints.clear()
        screenElements.clear()
        navigationHistory.clear()
        backStack.clear()
        dynamicRegions.clear()
        discoveredMenus.clear()
        generatedCommands.clear()
    }
}

/**
 * Callback interface for exploration state changes.
 */
interface ExplorationStateCallback {
    fun onPhaseChanged(oldPhase: ExplorationPhase, newPhase: ExplorationPhase)
    fun onScreenChanged(previousHash: String, newFingerprint: ScreenFingerprint)
    fun onElementsDiscovered(count: Int)
    fun onElementClicked(element: ElementInfo)
    fun onNavigation(record: NavigationRecord)
    fun onWaitingForUser(reason: String)
    fun onError(message: String)
    fun onExplorationComplete(stats: ExplorationStats)
}
