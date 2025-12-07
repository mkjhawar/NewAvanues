/**
 * ScreenStateManager.kt - Manages screen states and visited tracking
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/fingerprinting/ScreenStateManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Manages screen state tracking during exploration
 */

package com.augmentalis.learnapp.fingerprinting

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.learnapp.models.ScreenState
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Screen State Manager
 *
 * Manages screen state tracking during exploration.
 * Tracks visited screens, detects transitions, and manages state history.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val manager = ScreenStateManager()
 *
 * // Get current screen state
 * val rootNode = getRootInActiveWindow()
 * val state = manager.captureScreenState(rootNode, "com.instagram.android")
 *
 * // Check if visited
 * if (manager.isVisited(state.hash)) {
 *     println("Already explored this screen")
 * } else {
 *     manager.markAsVisited(state.hash)
 *     // Explore screen...
 * }
 *
 * // Wait for screen transition
 * manager.waitForScreenTransition(previousHash)
 * ```
 *
 * ## Features
 *
 * - SHA-256 fingerprinting for screen identification
 * - Visited state tracking
 * - Screen transition detection
 * - State history management
 * - Thread-safe operations
 *
 * @since 1.0.0
 */
class ScreenStateManager {

    /**
     * Screen fingerprinter
     */
    private val fingerprinter = ScreenFingerprinter()

    /**
     * Set of visited screen hashes
     */
    private val visitedScreens = mutableSetOf<String>()

    /**
     * Map of screen hashes to ScreenState objects
     */
    private val screenStates = mutableMapOf<String, ScreenState>()

    /**
     * History of screen hashes (navigation history)
     */
    private val screenHistory = mutableListOf<String>()

    /**
     * Mutex for thread safety
     */
    private val mutex = Mutex()

    /**
     * Current screen hash
     */
    private var currentScreenHash: String? = null

    /**
     * Capture screen state
     *
     * Calculates fingerprint and creates ScreenState object.
     *
     * @param rootNode Root accessibility node
     * @param packageName Package name of app
     * @param depth DFS depth (optional)
     * @return Screen state
     */
    suspend fun captureScreenState(
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        depth: Int = 0
    ): ScreenState = mutex.withLock {
        if (rootNode == null) {
            return@withLock createEmptyScreenState(packageName)
        }

        // Calculate fingerprint
        val hash = fingerprinter.calculateFingerprint(rootNode)

        // Check if state already exists
        val existingState = screenStates[hash]
        if (existingState != null) {
            currentScreenHash = hash
            return@withLock existingState
        }

        // Count elements
        val elementCount = countElements(rootNode)

        // Extract activity name (if available)
        val activityName = extractActivityName(rootNode)

        // Create new screen state
        val state = ScreenState(
            hash = hash,
            packageName = packageName,
            activityName = activityName,
            timestamp = System.currentTimeMillis(),
            elementCount = elementCount,
            isVisited = visitedScreens.contains(hash),
            depth = depth
        )

        // Save state
        screenStates[hash] = state
        currentScreenHash = hash

        // Add to history
        if (screenHistory.isEmpty() || screenHistory.last() != hash) {
            screenHistory.add(hash)
        }

        return@withLock state
    }

    /**
     * Check if screen has been visited
     *
     * @param hash Screen hash to check
     * @return true if visited
     */
    fun isVisited(hash: String): Boolean {
        return visitedScreens.contains(hash)
    }

    /**
     * Mark screen as visited
     *
     * @param hash Screen hash to mark
     */
    suspend fun markAsVisited(hash: String) = mutex.withLock {
        visitedScreens.add(hash)

        // Update state object
        screenStates[hash]?.let { state ->
            screenStates[hash] = state.markAsVisited()
        }
    }

    /**
     * Get screen state by hash
     *
     * @param hash Screen hash
     * @return Screen state or null if not found
     */
    fun getScreenState(hash: String): ScreenState? {
        return screenStates[hash]
    }

    /**
     * Get current screen hash
     *
     * @return Current screen hash or null
     */
    fun getCurrentScreenHash(): String? {
        return currentScreenHash
    }

    /**
     * Get all visited screens
     *
     * @return Set of visited screen hashes
     */
    fun getVisitedScreens(): Set<String> {
        return visitedScreens.toSet()
    }

    /**
     * Get all screen states
     *
     * @return Map of hash → ScreenState
     */
    fun getAllScreenStates(): Map<String, ScreenState> {
        return screenStates.toMap()
    }

    /**
     * Get screen history
     *
     * @return List of screen hashes in visit order
     */
    fun getScreenHistory(): List<String> {
        return screenHistory.toList()
    }

    /**
     * Wait for screen transition
     *
     * Polls until screen hash changes from previous hash.
     *
     * @param previousHash Previous screen hash
     * @param timeoutMs Timeout in milliseconds (default 5 seconds)
     * @return New screen hash or null if timeout
     */
    suspend fun waitForScreenTransition(
        previousHash: String,
        timeoutMs: Long = 5000L
    ): String? {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentHash = currentScreenHash
            if (currentHash != null && currentHash != previousHash) {
                return currentHash
            }

            delay(100)  // Poll every 100ms
        }

        return null  // Timeout
    }

    /**
     * Detect screen change from root node
     *
     * Checks if current root node represents a different screen.
     *
     * @param rootNode Root accessibility node
     * @return true if screen changed
     */
    suspend fun hasScreenChanged(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        val newHash = fingerprinter.calculateFingerprint(rootNode)
        val oldHash = currentScreenHash

        return newHash != oldHash
    }

    /**
     * Get statistics
     *
     * @return Screen state manager stats
     */
    fun getStats(): ScreenStateStats {
        return ScreenStateStats(
            totalScreensDiscovered = screenStates.size,
            totalScreensVisited = visitedScreens.size,
            currentDepth = screenHistory.size,
            averageElementsPerScreen = if (screenStates.isEmpty()) 0f else {
                screenStates.values.map { it.elementCount }.average().toFloat()
            }
        )
    }

    /**
     * Clear all state (for new exploration)
     */
    suspend fun clear() = mutex.withLock {
        visitedScreens.clear()
        screenStates.clear()
        screenHistory.clear()
        currentScreenHash = null
    }

    /**
     * Count elements in node tree
     *
     * @param rootNode Root node
     * @return Number of nodes in tree
     */
    private fun countElements(rootNode: AccessibilityNodeInfo): Int {
        var count = 1  // Count root

        for (i in 0 until rootNode.childCount) {
            rootNode.getChild(i)?.let { child ->
                count += countElements(child)
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
            }
        }

        return count
    }

    /**
     * Extract activity name from node tree
     *
     * @param rootNode Root node
     * @return Activity name or null
     */
    private fun extractActivityName(rootNode: AccessibilityNodeInfo): String? {
        // Try to get window title (often contains activity)
        return rootNode.window?.title?.toString()
    }

    /**
     * Create empty screen state (fallback)
     *
     * @param packageName Package name
     * @return Empty screen state
     */
    private fun createEmptyScreenState(packageName: String): ScreenState {
        return ScreenState(
            hash = "empty",
            packageName = packageName,
            activityName = null,
            timestamp = System.currentTimeMillis(),
            elementCount = 0,
            isVisited = false,
            depth = 0
        )
    }
}

/**
 * Screen State Statistics
 *
 * @property totalScreensDiscovered Total unique screens discovered
 * @property totalScreensVisited Total screens visited (explored)
 * @property currentDepth Current navigation depth
 * @property averageElementsPerScreen Average number of elements per screen
 */
data class ScreenStateStats(
    val totalScreensDiscovered: Int,
    val totalScreensVisited: Int,
    val currentDepth: Int,
    val averageElementsPerScreen: Float
) {
    override fun toString(): String {
        return """
            Screen State Stats:
            - Screens Discovered: $totalScreensDiscovered
            - Screens Visited: $totalScreensVisited
            - Current Depth: $currentDepth
            - Avg Elements/Screen: ${"%.1f".format(averageElementsPerScreen)}
        """.trimIndent()
    }
}
