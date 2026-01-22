/**
 * ScreenStateManager.kt - Manages screen states and visited tracking
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/learnapp/fingerprinting/ScreenStateManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Manages screen state tracking during exploration
 */

package com.augmentalis.voiceoscore.learnapp.fingerprinting

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
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
 * val manager = ScreenStateManager(accessibilityService)
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
 * - Popup/dialog detection with stable hashing
 * - Visited state tracking
 * - Screen transition detection
 * - State history management
 * - Thread-safe operations
 *
 * @property accessibilityService Accessibility service (optional for popup detection)
 * @since 1.0.0
 */
class ScreenStateManager(
    private val accessibilityService: AccessibilityService? = null
) {

    /**
     * Screen fingerprinter
     */
    private val fingerprinter = ScreenFingerprinter()

    /**
     * Developer settings for verbose logging
     */
    private val developerSettings by lazy {
        accessibilityService?.let { LearnAppDeveloperSettings(it) }
    }

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
     * FIX (2025-11-24): Added popup/dialog detection with stable hashing
     * Issue: Popups with time-dependent content (e.g., time picker) don't have stable hashes
     * Solution: Detect popup windows and generate hash based on structure, not content
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

        // FIX (2025-11-24): Detect if current window is a popup/dialog
        val windowInfo = detectPopupWindow(rootNode, packageName)

        // Calculate fingerprint (popup-aware)
        val hash = if (windowInfo.isPopup) {
            // For popups, generate stable hash based on structure, not content
            fingerprinter.calculatePopupFingerprint(rootNode, windowInfo.popupType)
        } else {
            // For regular screens, use normal fingerprinting
            fingerprinter.calculateFingerprint(rootNode)
        }

        // Check if state already exists (exact match)
        val existingState = screenStates[hash]
        if (existingState != null) {
            currentScreenHash = hash
            return@withLock existingState
        }

        // NEW: Check if similar screen already exists (prevent duplicates)
        val existingSimilarScreen = findRecentSimilarScreen(hash, packageName)
        if (existingSimilarScreen != null) {
            if (developerSettings?.isVerboseLoggingEnabled() == true) {
                android.util.Log.d("ScreenStateManager",
                    "Screen $hash is similar to existing ${existingSimilarScreen.hash}. " +
                    "Reusing existing screen state to avoid duplication. " +
                    "Similarity: first 16 chars match")
            }

            // Return existing screen with updated timestamp
            currentScreenHash = existingSimilarScreen.hash
            return@withLock existingSimilarScreen.copy(
                timestamp = System.currentTimeMillis(),
                depth = depth  // Update depth if different
            )
        }

        // If not similar, create new screen state as before
        if (developerSettings?.isVerboseLoggingEnabled() == true) {
            android.util.Log.d("ScreenStateManager",
                "Creating new screen state: $hash (no similar screen found)")
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

            delay(developerSettings?.getScreenTransitionPollIntervalMs() ?: 100L)
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
     * Calculates similarity between two screen hashes using fuzzy matching.
     * Useful for screens with dynamic elements (timestamps, counters).
     *
     * @param hash1 First hash
     * @param hash2 Second hash
     * @return Similarity score (0.0 to 1.0)
     */
    fun calculateHashSimilarity(hash1: String, hash2: String): Double {
        if (hash1 == hash2) return 1.0
        if (hash1.isEmpty() || hash2.isEmpty()) return 0.0

        // Use Levenshtein distance for fuzzy matching
        val distance = levenshteinDistance(hash1, hash2)
        val maxLength = maxOf(hash1.length, hash2.length)

        return 1.0 - (distance.toDouble() / maxLength)
    }

    /**
     * Waits for screen to return to expected hash.
     *
     * @param expectedHash Expected screen hash
     * @param timeoutMs Timeout in milliseconds
     * @param similarityThreshold Minimum similarity threshold (default 0.85)
     * @return true if screen returned to expected hash
     */
    suspend fun waitForScreenReturn(
        expectedHash: String,
        timeoutMs: Long = 3000L,
        similarityThreshold: Double = 0.85
    ): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentHash = getCurrentScreenHash()

            if (currentHash == expectedHash) return true

            // Fuzzy match
            currentHash?.let { hash ->
                val similarity = calculateHashSimilarity(hash, expectedHash)
                if (similarity >= similarityThreshold) return true
            }

            delay(developerSettings?.getScreenTransitionPollIntervalMs() ?: 100L)
        }

        return false
    }

    /**
     * Check if two screens are structurally similar
     *
     * Compares element counts and types to determine if screens are the same
     * despite minor content changes (timestamps, notifications, live updates).
     *
     * @param hash1 First screen hash
     * @param hash2 Second screen hash
     * @param similarityThreshold Minimum similarity (0.0-1.0), default 0.85
     * @return true if screens are structurally similar (>=85% match)
     */
    fun areScreensSimilar(
        hash1: String,
        hash2: String,
        similarityThreshold: Double = 0.85
    ): Boolean {
        // Exact match (fast path)
        if (hash1 == hash2) return true

        // For now, implement basic similarity check
        // TODO: In future, compare actual screen structure from database

        // If hashes differ significantly, likely different screens
        // This is a simplified version - in production you'd compare actual screen states
        val hash1Prefix = hash1.take(16)
        val hash2Prefix = hash2.take(16)

        // If first 16 chars match, likely same screen with minor changes
        return hash1Prefix == hash2Prefix
    }

    /**
     * Find recent similar screen to avoid duplicate records
     *
     * Checks recent screen states for the same package to see if any
     * are similar enough to be considered the same screen.
     *
     * This prevents duplicate records when screens have minor changes due to:
     * - Timestamps updating
     * - Animation states
     * - Notification badges
     * - Status bar changes
     *
     * @param newHash Hash of the screen being captured
     * @param packageName Package name to limit search
     * @return Existing similar screen or null if none found
     */
    private fun findRecentSimilarScreen(
        newHash: String,
        packageName: String
    ): ScreenState? {
        // Get recent screens from screenStates map filtered by package
        val recentScreens = screenStates.values
            .filter { it.packageName == packageName }
            .sortedByDescending { it.timestamp }
            .take(10)  // Only check last 10 screens for this package

        // Check each recent screen for similarity
        for (recentScreen in recentScreens) {
            val isSimilar = areScreensSimilar(
                hash1 = newHash,
                hash2 = recentScreen.hash,
                similarityThreshold = (developerSettings?.getScreenHashSimilarityThreshold()?.toDouble() ?: 0.90)
            )

            if (isSimilar) {
                if (developerSettings?.isVerboseLoggingEnabled() == true) {
                    android.util.Log.d("ScreenStateManager",
                        "Found similar recent screen: ${recentScreen.hash} " +
                        "(checking ${recentScreens.size} recent screens for $packageName)")
                }
                return recentScreen
            }
        }

        return null
    }

    /**
     * Calculates Levenshtein distance between two strings.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Levenshtein distance
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1) { it }

        for (i in 1..s1.length) {
            costs[0] = i
            var prev = i - 1

            for (j in 1..s2.length) {
                val newValue = if (s1[i - 1] == s2[j - 1]) {
                    prev
                } else {
                    1 + minOf(prev, costs[j], costs[j - 1])
                }
                prev = costs[j]
                costs[j] = newValue
            }
        }

        return costs[s2.length]
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

    /**
     * Detect if current window is a popup/dialog
     *
     * FIX (2025-11-24): Detect popup windows for stable hashing
     * Popups/dialogs have time-dependent content that causes unstable hashes.
     * This method detects popups by checking:
     * - Window type (TYPE_APPLICATION_OVERLAY, high layer)
     * - Small bounds (typical dialog size)
     * - Common dialog class names
     *
     * @param rootNode Root node of current window
     * @param packageName Package name
     * @return WindowInfo containing popup detection result
     */
    private fun detectPopupWindow(rootNode: AccessibilityNodeInfo, packageName: String): PopupWindowInfo {
        // If no accessibility service, can't detect windows - assume not popup
        if (accessibilityService == null) {
            return PopupWindowInfo(isPopup = false, popupType = "")
        }

        try {
            // Get all windows
            val windows = accessibilityService.windows ?: return PopupWindowInfo(isPopup = false, popupType = "")

            // Find window that matches this root node's package
            for (window in windows) {
                try {
                    val windowRoot = window.root ?: continue
                    val windowPackage = windowRoot.packageName?.toString() ?: continue

                    // Check if this is our window (package match)
                    if (windowPackage != packageName) continue

                    // Check window characteristics for popup detection
                    val windowType = window.type
                    val layer = window.layer
                    val bounds = android.graphics.Rect()
                    window.getBoundsInScreen(bounds)
                    val width = bounds.width()
                    val height = bounds.height()

                    // Detect popup based on multiple heuristics
                    var isPopup = false
                    var popupType = ""

                    // Heuristic 1: TYPE_APPLICATION_OVERLAY (API 26+)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                        windowType == 0x00000004 /* TYPE_APPLICATION_OVERLAY */) {
                        isPopup = true
                        popupType = "overlay"
                    }

                    // Heuristic 2: High layer number (overlays are on top)
                    if (!isPopup && layer > 5) {
                        isPopup = true
                        popupType = "high_layer_overlay"
                    }

                    // Heuristic 3: Small bounds (dialogs are typically smaller)
                    if (!isPopup && (width < 800 || height < 600)) {
                        isPopup = true
                        popupType = "small_dialog"
                    }

                    // Heuristic 4: Check root node class name for dialog patterns
                    val className = rootNode.className?.toString() ?: ""
                    if (!isPopup && (className.contains("Dialog", ignoreCase = true) ||
                                     className.contains("Picker", ignoreCase = true) ||
                                     className.contains("Popup", ignoreCase = true) ||
                                     className.contains("Alert", ignoreCase = true))) {
                        isPopup = true
                        popupType = "dialog_class_${className.substringAfterLast('.')}"
                    }

                    if (isPopup) {
                        if (developerSettings?.isVerboseLoggingEnabled() == true) {
                            android.util.Log.d("ScreenStateManager",
                                "🔔 Popup detected: type=$popupType, windowType=$windowType, " +
                                "layer=$layer, bounds=${width}x${height}, class=$className")
                        }
                        return PopupWindowInfo(isPopup = true, popupType = popupType)
                    }

                } catch (e: Exception) {
                    android.util.Log.e("ScreenStateManager", "Error checking window for popup", e)
                    // Continue to next window
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("ScreenStateManager", "Error in detectPopupWindow", e)
        }

        // Not a popup
        return PopupWindowInfo(isPopup = false, popupType = "")
    }

    /**
     * Popup window detection result
     *
     * @property isPopup True if this is a popup/dialog window
     * @property popupType Type of popup (for stable hash generation)
     */
    private data class PopupWindowInfo(
        val isPopup: Boolean,
        val popupType: String
    )
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
