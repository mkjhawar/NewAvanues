/**
 * DFSExplorer.kt - Depth-First Search exploration algorithm
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/DFSExplorer.kt
 *
 * Author: Manoj Jhawar (refactored by Claude)
 * Created: 2025-12-04
 * Refactored: 2026-01-15 (SOLID extraction from ExplorationEngine.kt)
 *
 * Single Responsibility: Implements the iterative DFS exploration algorithm
 * including stack management, backtracking, and loop prevention.
 *
 * Extracted from ExplorationEngine.kt to improve maintainability and testability.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.augmentalis.voiceoscore.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.learnapp.tracking.ElementClickTracker
import com.augmentalis.voiceoscore.learnapp.ui.ChecklistManager
import kotlinx.coroutines.delay
import java.util.Stack
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a screen state in the exploration stack (for iterative DFS).
 *
 * @property screenHash Unique identifier for this screen
 * @property screenState Full screen state capture
 * @property elements All clickable elements on this screen (with fresh nodes)
 * @property currentElementIndex Index of next element to click
 * @property depth Depth in navigation hierarchy
 * @property parentScreenHash Hash of parent screen (for BACK navigation)
 */
data class ExplorationFrame(
    val screenHash: String,
    val screenState: ScreenState,
    val elements: MutableList<ElementInfo>,
    var currentElementIndex: Int = 0,
    val depth: Int,
    val parentScreenHash: String? = null
) {
    fun hasMoreElements(): Boolean = currentElementIndex < elements.size
    fun getNextElement(): ElementInfo? {
        return if (hasMoreElements()) {
            elements[currentElementIndex].also { currentElementIndex++ }
        } else null
    }
}

/**
 * Result of a single screen exploration cycle
 */
sealed class ExploreScreenResult {
    /** Continue to next iteration (may have navigated or completed screen) */
    data class Continue(val clickedCount: Int = 0) : ExploreScreenResult()

    /** Screen changed, need to handle navigation */
    data class Navigated(val newScreenHash: String) : ExploreScreenResult()

    /** External app navigation detected */
    data class ExternalApp(val packageName: String) : ExploreScreenResult()

    /** Exploration should stop */
    object Stop : ExploreScreenResult()
}

/**
 * DFS exploration state container
 */
data class DFSState(
    val explorationStack: Stack<ExplorationFrame> = Stack(),
    val visitedScreens: MutableSet<String> = mutableSetOf(),
    val navigationPaths: MutableMap<String, Int> = mutableMapOf(),
    val elementToDestinationScreen: MutableMap<String, String> = mutableMapOf(),
    val registeredElementUuids: MutableSet<String> = mutableSetOf(),
    val clickedStableIds: MutableSet<String> = mutableSetOf()
)

/**
 * Cumulative VUID tracking for exploration progress
 *
 * Uses thread-safe ConcurrentHashMap.newKeySet() to handle concurrent access
 * from multiple threads (AccessibilityService, Main, Dispatchers.Default)
 */
class CumulativeTracking {
    val discoveredVuids: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val clickedVuids: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val blockedVuids: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun clear() {
        discoveredVuids.clear()
        clickedVuids.clear()
        blockedVuids.clear()
    }

    fun getCompleteness(): Float {
        return if (discoveredVuids.isNotEmpty()) {
            (clickedVuids.size.toFloat() / discoveredVuids.size.toFloat()) * 100f
        } else {
            0f
        }
    }
}

/**
 * Implements the iterative DFS exploration algorithm.
 *
 * **Why Iterative vs Recursive:**
 * - Recursive DFS blocked click loop after 1-2 elements per screen
 * - Iterative DFS completes ALL elements on current screen FIRST
 * - Nodes stay fresh (no long recursion delays)
 * - Easy to add progress tracking and element checklists
 *
 * **Algorithm:**
 * 1. Push initial screen onto stack
 * 2. While stack not empty:
 *    a. Peek current frame
 *    b. If has more elements: click next element
 *    c. If screen changed: push new screen onto stack
 *    d. Else: pop frame, press BACK (if not root)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val explorer = DFSExplorer(context, accessibilityService, screenStateManager, ...)
 *
 * // Initialize state
 * val state = explorer.initializeState(rootNode, packageName)
 *
 * // Run exploration loop
 * while (!state.explorationStack.isEmpty() && !shouldStop()) {
 *     val result = explorer.exploreIteration(state, packageName)
 *     when (result) {
 *         is ExploreScreenResult.Stop -> break
 *         // ... handle other cases
 *     }
 * }
 * ```
 *
 * @property context Android context
 * @property accessibilityService Accessibility service for UI operations
 * @property screenStateManager Screen state management
 * @property screenExplorer Screen element exploration
 * @property clickTracker Element click tracking
 * @property checklistManager UI checklist management
 * @property clicker Element click operations
 * @property registrar Element registration
 * @property dangerDetector Dangerous element detection
 */
class DFSExplorer(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val screenStateManager: ScreenStateManager,
    private val screenExplorer: ScreenExplorer,
    private val clickTracker: ElementClickTracker,
    private val checklistManager: ChecklistManager,
    private val clicker: ElementClicker,
    private val registrar: ElementRegistrar,
    private val dangerDetector: DangerDetector,
    private val metrics: ExplorationMetrics? = null
) {
    /**
     * Developer settings
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    /**
     * Debug callback for exploration events
     */
    private var debugCallback: ExplorationDebugCallback? = null

    /**
     * Cumulative VUID tracking (thread-safe)
     */
    val cumulativeTracking = CumulativeTracking()

    /**
     * Maximum navigation path revisits allowed
     */
    private val maxPathRevisits = 2

    /**
     * Set the debug callback for exploration events
     *
     * @param callback Callback to receive debug events
     */
    fun setDebugCallback(callback: ExplorationDebugCallback?) {
        debugCallback = callback
        android.util.Log.d(TAG, "Debug callback ${if (callback != null) "enabled" else "disabled"}")
    }

    /**
     * Initialize DFS state with the root screen
     *
     * @param rootNode Root accessibility node
     * @param packageName Target package name
     * @return Initialized DFS state, or null if initialization fails
     */
    suspend fun initializeState(
        rootNode: android.view.accessibility.AccessibilityNodeInfo,
        packageName: String
    ): DFSState? {
        val state = DFSState()

        // Clear cumulative tracking for new session
        cumulativeTracking.clear()

        // Capture root screen state
        val rootScreenState = screenStateManager.captureScreenState(rootNode, packageName, 0)
        val rootExploration = screenExplorer.exploreScreen(rootNode, packageName, 0)

        if (rootExploration !is ScreenExplorationResult.Success) {
            android.util.Log.e(TAG, "Failed to explore root screen")
            return null
        }

        // Pre-generate UUIDs for root elements
        val rootElementsWithUuids = registrar.preGenerateUuids(
            rootExploration.safeClickableElements,
            packageName
        )

        val rootFrame = ExplorationFrame(
            screenHash = rootScreenState.hash,
            screenState = rootScreenState,
            elements = rootElementsWithUuids.toMutableList(),
            currentElementIndex = 0,
            depth = 0,
            parentScreenHash = null
        )

        state.explorationStack.push(rootFrame)
        state.visitedScreens.add(rootScreenState.hash)

        // Add to checklist
        checklistManager.addScreen(
            screenHash = rootScreenState.hash,
            screenName = rootScreenState.activityName ?: "Root Screen",
            elementCount = rootElementsWithUuids.size
        )

        // Register clickable elements (exclude critical dangerous)
        val criticalElements = rootElementsWithUuids.filter { dangerDetector.isCriticalDangerous(it) }
        val clickableElements = rootElementsWithUuids.filter { !dangerDetector.isCriticalDangerous(it) }
        val clickableUuids = clickableElements.mapNotNull { it.uuid }

        clickTracker.registerScreen(rootScreenState.hash, clickableUuids)
        cumulativeTracking.discoveredVuids.addAll(clickableUuids)

        // Track blocked VUIDs separately
        val blockedUuids = criticalElements.mapNotNull { it.uuid }
        cumulativeTracking.blockedVuids.addAll(blockedUuids)

        // Log skipped critical elements
        if (criticalElements.isNotEmpty()) {
            android.util.Log.i(TAG,
                "Screen ${rootScreenState.hash.take(8)}: Registered ${clickableUuids.size} clickable, " +
                "excluded ${criticalElements.size} critical dangerous elements")

            criticalElements.forEach { elem ->
                val desc = elem.text.ifEmpty { elem.contentDescription.ifEmpty { elem.className } }
                val reason = dangerDetector.getCriticalDangerReason(elem) ?: "Critical dangerous element"
                debugCallback?.onElementBlocked(elem.stableId(), rootScreenState.hash, reason)
            }
        }

        android.util.Log.i(TAG,
            "Starting ITERATIVE DFS exploration of $packageName (max depth: ${developerSettings.getMaxExplorationDepth()})")

        return state
    }

    /**
     * Explore elements on current screen using Hybrid C-Lite fresh-scrape strategy.
     *
     * Algorithm:
     * 1. Fresh scrape the screen (get new nodes)
     * 2. Filter out already-clicked elements by stableId
     * 3. Sort remaining by stabilityScore (highest first)
     * 4. Click top element
     * 5. Repeat until no unclicked elements remain OR screen changes
     *
     * @param frame Current exploration frame
     * @param packageName Target package name
     * @param state DFS state
     * @return Number of elements successfully clicked
     */
    suspend fun exploreScreenWithFreshScrape(
        frame: ExplorationFrame,
        packageName: String,
        state: DFSState
    ): Int {
        var clickCount = 0
        var consecutiveFailures = 0
        val maxConsecutiveFailures = developerSettings.getMaxConsecutiveClickFailures()
        val screenStartHash = frame.screenHash

        // Per-screen exploration timeout (2 minutes)
        val screenExplorationTimeout = 120_000L
        val screenStartTime = System.currentTimeMillis()

        // Click cap based on registered elements
        val registeredProgress = clickTracker.getScreenProgress(screenStartHash)
        val maxClicksForScreen = if (registeredProgress != null) {
            registeredProgress.totalClickableElements + 2
        } else {
            frame.elements.size + 2
        }

        android.util.Log.i(TAG,
            "Starting fresh-scrape exploration for screen ${screenStartHash.take(8)}... (max clicks: $maxClicksForScreen)")

        while (consecutiveFailures < maxConsecutiveFailures) {
            // Check click cap
            if (clickCount >= maxClicksForScreen) {
                android.util.Log.i(TAG,
                    "Click cap reached ($clickCount/$maxClicksForScreen) on ${screenStartHash.take(8)}...")
                break
            }

            // Check per-screen timeout
            if (System.currentTimeMillis() - screenStartTime > screenExplorationTimeout) {
                android.util.Log.w(TAG,
                    "Per-screen timeout reached on ${screenStartHash.take(8)}...")
                break
            }

            // Fresh scrape
            val rootNode = clicker.getRootNode()
            if (rootNode == null) {
                android.util.Log.w(TAG, "Root node null, aborting")
                break
            }

            // Check package
            val currentPackage = rootNode.packageName?.toString()
            if (currentPackage != packageName) {
                android.util.Log.w(TAG, "Package changed from $packageName to $currentPackage")
                break
            }

            // Fresh exploration with timeout protection
            val freshExploration = kotlinx.coroutines.withTimeoutOrNull(45_000L) {
                screenExplorer.exploreScreen(rootNode, packageName, frame.depth)
            }

            if (freshExploration == null || freshExploration !is ScreenExplorationResult.Success) {
                consecutiveFailures++
                delay(developerSettings.getScrollDelayMs())
                continue
            }

            // Track element detection
            freshExploration.allElements.forEach { _ ->
                metrics?.onElementDetected()
            }

            // Fire debug callback
            debugCallback?.onScreenExplored(
                elements = freshExploration.safeClickableElements,
                screenHash = screenStartHash,
                activityName = frame.screenState.activityName ?: "Unknown",
                packageName = packageName,
                parentScreenHash = frame.parentScreenHash
            )

            // Filter and sort elements
            val unclickedElements = freshExploration.safeClickableElements
                .filter { it.stableId() !in state.clickedStableIds }
                .filter { element ->
                    if (dangerDetector.isCriticalDangerous(element)) {
                        metrics?.onElementFiltered(element, "Critical dangerous element")
                        false
                    } else true
                }
                .filter { element ->
                    val elementKey = "${screenStartHash}:${element.stableId()}"
                    val destinationScreen = state.elementToDestinationScreen[elementKey]
                    if (destinationScreen != null && destinationScreen in state.visitedScreens) {
                        metrics?.onElementFiltered(element, "Leads to already-visited screen")
                        false
                    } else true
                }
                .sortedWith(compareBy<ElementInfo> {
                    if (dangerDetector.isDangerous(it)) 1 else 0
                }.thenByDescending { it.stabilityScore() })

            if (unclickedElements.isEmpty()) {
                android.util.Log.i(TAG, "All elements clicked on this screen ($clickCount total)")
                break
            }

            // Click the most stable unclicked element
            val element = unclickedElements.first()
            val stableId = element.stableId()
            val elementDesc = element.text.ifEmpty { element.contentDescription.ifEmpty { element.className } }

            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d(TAG,
                    ">>> CLICKING: \"$elementDesc\" [stability: ${element.stabilityScore()}]")
            }

            // Attempt click
            var clicked = false
            element.node?.let { node ->
                clicked = clicker.clickElement(node, elementDesc, element.className.substringAfterLast('.'))
            }

            // Gesture fallback
            if (!clicked) {
                clicked = clicker.performCoordinateClick(element.bounds)
            }

            if (clicked) {
                state.clickedStableIds.add(stableId)
                clickCount++
                consecutiveFailures = 0

                // Update tracking
                element.uuid?.let { vuid ->
                    clickTracker.markElementClicked(frame.screenHash, vuid)
                    checklistManager.markElementCompleted(frame.screenHash, vuid)
                    cumulativeTracking.clickedVuids.add(vuid)
                    debugCallback?.onElementClicked(stableId, screenStartHash, vuid)
                }

                // Wait for UI settle
                delay(developerSettings.getClickDelayMs())

                // Check if screen changed
                val postClickRoot = clicker.getRootNode()
                if (postClickRoot != null) {
                    val postClickState = screenStateManager.captureScreenState(
                        postClickRoot, packageName, frame.depth
                    )
                    if (postClickState.hash != screenStartHash) {
                        val elementKey = "${screenStartHash}:${stableId}"
                        state.elementToDestinationScreen[elementKey] = postClickState.hash
                        debugCallback?.onElementNavigated(elementKey, postClickState.hash)

                        android.util.Log.i(TAG,
                            "Screen changed from ${screenStartHash.take(8)}... to ${postClickState.hash.take(8)}...")
                        break
                    }
                }
            } else {
                android.util.Log.w(TAG, "Both node and gesture click failed for \"$elementDesc\"")
                consecutiveFailures++
                state.clickedStableIds.add(stableId) // Avoid infinite retry
            }
        }

        if (consecutiveFailures >= maxConsecutiveFailures) {
            android.util.Log.w(TAG,
                "Reached $maxConsecutiveFailures consecutive failures, stopping screen exploration")
        }

        android.util.Log.i(TAG, "Fresh-scrape exploration complete: $clickCount elements clicked")
        return clickCount
    }

    /**
     * Handle navigation to a new screen discovered during exploration
     *
     * @param state DFS state
     * @param currentFrame Current exploration frame
     * @param packageName Target package name
     * @param newScreenHash Hash of the newly discovered screen
     * @return true if new screen was pushed onto stack
     */
    suspend fun handleNewScreen(
        state: DFSState,
        currentFrame: ExplorationFrame,
        packageName: String,
        newScreenHash: String
    ): Boolean {
        val maxDepth = developerSettings.getMaxExplorationDepth()

        // Check navigation path for loop prevention
        val navPathKey = "${currentFrame.screenHash.take(8)}->${newScreenHash.take(8)}"
        val pathVisitCount = state.navigationPaths.getOrDefault(navPathKey, 0)

        val shouldExplore = newScreenHash != currentFrame.screenHash &&
            (newScreenHash !in state.visitedScreens || pathVisitCount < maxPathRevisits) &&
            currentFrame.depth + 1 <= maxDepth

        if (!shouldExplore) {
            if (pathVisitCount >= maxPathRevisits) {
                android.util.Log.i(TAG,
                    "Blocking re-entry loop: $navPathKey exceeded max revisits")
            }
            return false
        }

        // Increment path visit count
        state.navigationPaths[navPathKey] = pathVisitCount + 1

        // Get fresh root node
        val postExploreRoot = clicker.getRootNode() ?: return false

        val newExploration = screenExplorer.exploreScreen(
            postExploreRoot, packageName, currentFrame.depth + 1
        )

        if (newExploration !is ScreenExplorationResult.Success) {
            return false
        }

        val newElementsWithUuids = registrar.preGenerateUuids(
            newExploration.safeClickableElements, packageName
        )

        val newScreenState = screenStateManager.captureScreenState(
            postExploreRoot, packageName, currentFrame.depth + 1
        )

        val newFrame = ExplorationFrame(
            screenHash = newScreenState.hash,
            screenState = newScreenState,
            elements = newElementsWithUuids.toMutableList(),
            currentElementIndex = 0,
            depth = currentFrame.depth + 1,
            parentScreenHash = currentFrame.screenHash
        )

        state.explorationStack.push(newFrame)
        state.visitedScreens.add(newScreenState.hash)

        // Register elements
        val newClickableUuids = newElementsWithUuids
            .filter { !dangerDetector.isCriticalDangerous(it) }
            .mapNotNull { it.uuid }

        clickTracker.registerScreen(newScreenState.hash, newClickableUuids)
        cumulativeTracking.discoveredVuids.addAll(newClickableUuids)

        checklistManager.addScreen(
            screenHash = newScreenState.hash,
            screenName = newScreenState.activityName ?: "Screen #${state.visitedScreens.size}",
            elementCount = newElementsWithUuids.size
        )

        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d(TAG,
                "Pushed new screen onto stack: ${newScreenState.hash.take(8)}... " +
                "(${newElementsWithUuids.size} elements, depth: ${newFrame.depth})")
        }

        return true
    }

    /**
     * Pop current frame and navigate back
     *
     * @param state DFS state
     */
    suspend fun popAndNavigateBack(state: DFSState) {
        if (state.explorationStack.isNotEmpty()) {
            val frame = state.explorationStack.pop()

            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d(TAG,
                    "Completed screen ${frame.screenHash.take(8)}... (${frame.elements.size} elements)")
            }

            if (state.explorationStack.isNotEmpty()) {
                clicker.pressBack()
                delay(developerSettings.getScreenProcessingDelayMs())
            }
        }
    }

    /**
     * Get exploration statistics
     *
     * @return Formatted statistics string
     */
    fun getExplorationStats(): String {
        val nonBlockedCount = cumulativeTracking.discoveredVuids.size
        val clickedCount = cumulativeTracking.clickedVuids.size
        val blockedCount = cumulativeTracking.blockedVuids.size
        val completeness = cumulativeTracking.getCompleteness()

        return "Final Stats: ${completeness.toInt()}% of non-blocked items " +
               "($clickedCount/$nonBlockedCount clicked), $blockedCount blocked items skipped"
    }

    companion object {
        private const val TAG = "DFSExplorer"
    }
}
