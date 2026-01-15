/**
 * DFSExplorer.kt - Android DFS exploration algorithm
 *
 * Implements the iterative DFS exploration algorithm including
 * stack management, backtracking, and loop prevention.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.functions.IScreenFingerprinter
import kotlinx.coroutines.delay

/**
 * Implements the iterative DFS exploration algorithm for Android.
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
 * @property accessibilityService Accessibility service for UI operations
 * @property clicker Element click operations
 * @property registrar Element registration
 * @property fingerprinter Screen fingerprinting
 * @property dangerDetector Dangerous element detection
 * @property metrics Optional metrics collector
 * @property config Exploration configuration
 */
class DFSExplorer(
    private val accessibilityService: AccessibilityService,
    private val clicker: ElementClicker,
    private val registrar: ElementRegistrar,
    private val fingerprinter: IScreenFingerprinter,
    private val dangerDetector: DangerDetector,
    private val metrics: ExplorationMetrics? = null,
    private val config: ExplorationConfig = ExplorationConfig.DEFAULT
) {
    /**
     * Debug callback for exploration events
     */
    private var debugCallback: ExplorationDebugCallback? = null

    /**
     * Cumulative VUID tracking (thread-safe)
     */
    val cumulativeTracking = CumulativeTracking()

    /**
     * Set the debug callback for exploration events
     */
    fun setDebugCallback(callback: ExplorationDebugCallback?) {
        debugCallback = callback
        Log.d(TAG, "Debug callback ${if (callback != null) "enabled" else "disabled"}")
    }

    /**
     * Initialize DFS state with the root screen
     *
     * @param rootNode Root accessibility node
     * @param packageName Target package name
     * @return Initialized DFS state, or null if initialization fails
     */
    suspend fun initializeState(
        rootNode: AccessibilityNodeInfo,
        packageName: String
    ): DFSState? {
        val state = DFSState()

        // Clear cumulative tracking for new session
        cumulativeTracking.clear()

        // Capture root screen state
        val rootScreenHash = fingerprinter.calculateFingerprint(
            extractElementInfoList(rootNode, packageName)
        )

        val rootElements = extractClickableElements(rootNode, packageName)

        // Pre-generate UUIDs for root elements
        val rootElementsWithUuids = registrar.preGenerateUuids(rootElements, packageName)

        val rootFrame = ExplorationFrame(
            screenHash = rootScreenHash,
            activityName = getActivityName(rootNode),
            elements = rootElementsWithUuids.toMutableList(),
            currentElementIndex = 0,
            depth = 0,
            parentScreenHash = null
        )

        state.push(rootFrame)
        state.visitedScreens.add(rootScreenHash)

        // Register clickable elements (exclude critical dangerous)
        val criticalElements = rootElementsWithUuids.filter { dangerDetector.isCriticalDangerous(it) }
        val clickableElements = rootElementsWithUuids.filter { !dangerDetector.isCriticalDangerous(it) }
        val clickableUuids = clickableElements.mapNotNull { it.uuid }

        cumulativeTracking.addAllDiscovered(clickableUuids)

        // Track blocked VUIDs separately
        val blockedUuids = criticalElements.mapNotNull { it.uuid }
        cumulativeTracking.addAllBlocked(blockedUuids)

        // Notify debug callback
        debugCallback?.onExplorationStarted(packageName)
        debugCallback?.onStackPush(rootScreenHash, 0, rootElementsWithUuids.size)

        Log.i(TAG, "Starting ITERATIVE DFS exploration of $packageName " +
                  "(max depth: ${config.maxExplorationDepth})")

        return state
    }

    /**
     * Explore elements on current screen using fresh-scrape strategy.
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
     * @return Result of exploration
     */
    suspend fun exploreScreen(
        frame: ExplorationFrame,
        packageName: String,
        state: DFSState
    ): ExploreScreenResult {
        var clickCount = 0
        var consecutiveFailures = 0
        val screenStartHash = frame.screenHash

        val screenStartTime = System.currentTimeMillis()
        val maxClicksForScreen = frame.elements.size + 2

        Log.i(TAG, "Exploring screen ${screenStartHash.take(8)}... " +
                  "(${frame.elements.size} elements)")

        while (consecutiveFailures < config.maxConsecutiveClickFailures) {
            // Check click cap
            if (clickCount >= maxClicksForScreen) {
                Log.i(TAG, "Click cap reached ($clickCount/$maxClicksForScreen)")
                break
            }

            // Check per-screen timeout
            if (System.currentTimeMillis() - screenStartTime > config.screenExplorationTimeoutMs) {
                Log.w(TAG, "Per-screen timeout reached")
                return ExploreScreenResult.Timeout
            }

            // Get fresh root node
            val rootNode = clicker.getRootNode()
            if (rootNode == null) {
                Log.w(TAG, "Root node null, aborting screen")
                break
            }

            // Check package hasn't changed
            val currentPackage = rootNode.packageName?.toString()
            if (currentPackage != packageName) {
                Log.w(TAG, "External app detected: $currentPackage")
                return ExploreScreenResult.ExternalApp(currentPackage ?: "unknown")
            }

            // Get fresh elements
            val freshElements = extractClickableElements(rootNode, packageName)

            // Fire debug callback
            debugCallback?.onScreenExplored(
                elements = freshElements,
                screenHash = screenStartHash,
                activityName = frame.activityName ?: "Unknown",
                packageName = packageName,
                parentScreenHash = frame.parentScreenHash
            )

            // Filter and sort elements
            val unclickedElements = freshElements
                .filter { it.stableId() !in state.clickedStableIds }
                .filter { element ->
                    if (dangerDetector.isCriticalDangerous(element)) {
                        metrics?.onElementFiltered(element, "Critical dangerous")
                        debugCallback?.onElementBlocked(
                            element.stableId(),
                            screenStartHash,
                            dangerDetector.getCriticalDangerReason(element) ?: "Critical"
                        )
                        false
                    } else true
                }
                .filter { element ->
                    // Skip elements that lead to already-visited screens
                    !state.wouldLeadToVisitedScreen(screenStartHash, element.stableId())
                }
                .sortedWith(compareBy<ElementInfo> {
                    dangerDetector.getDangerLevel(it)
                }.thenByDescending { it.stabilityScore() })

            if (unclickedElements.isEmpty()) {
                Log.i(TAG, "All elements clicked on this screen ($clickCount total)")
                return ExploreScreenResult.ScreenComplete
            }

            // Click the safest unclicked element
            val element = unclickedElements.first()
            val stableId = element.stableId()

            if (config.verboseLogging) {
                val desc = element.text ?: element.contentDescription ?: element.className
                Log.d(TAG, ">>> CLICKING: \"$desc\" [stability: ${element.stabilityScore()}]")
            }

            // Attempt click
            val clickResult = clicker.clickElement(element)

            when (clickResult) {
                is ClickResult.Success -> {
                    state.clickedStableIds.add(stableId)
                    clickCount++
                    consecutiveFailures = 0

                    // Track clicked VUID
                    element.uuid?.let { vuid ->
                        cumulativeTracking.addClicked(vuid)
                        debugCallback?.onElementClicked(stableId, screenStartHash, vuid)
                    }

                    // Wait for UI settle
                    delay(config.clickDelayMs)

                    // Check if screen changed
                    val postClickRoot = clicker.getRootNode()
                    if (postClickRoot != null) {
                        val postClickElements = extractElementInfoList(postClickRoot, packageName)
                        val postClickHash = fingerprinter.calculateFingerprint(postClickElements)

                        if (postClickHash != screenStartHash) {
                            state.recordElementDestination(screenStartHash, stableId, postClickHash)
                            debugCallback?.onElementNavigated("$screenStartHash:$stableId", postClickHash)

                            Log.i(TAG, "Screen changed: ${screenStartHash.take(8)} -> ${postClickHash.take(8)}")
                            return ExploreScreenResult.Navigated(postClickHash)
                        }
                    }
                }

                is ClickResult.Failed -> {
                    Log.w(TAG, "Click failed: ${clickResult.reason}")
                    consecutiveFailures++
                    state.clickedStableIds.add(stableId) // Avoid infinite retry
                }
            }
        }

        if (consecutiveFailures >= config.maxConsecutiveClickFailures) {
            Log.w(TAG, "Max consecutive failures reached")
        }

        return ExploreScreenResult.Continue(clickCount)
    }

    /**
     * Handle navigation to a new screen
     *
     * @param state DFS state
     * @param currentFrame Current exploration frame
     * @param packageName Target package name
     * @param newScreenHash Hash of the new screen
     * @return true if new screen was pushed onto stack
     */
    suspend fun handleNewScreen(
        state: DFSState,
        currentFrame: ExplorationFrame,
        packageName: String,
        newScreenHash: String
    ): Boolean {
        // Check depth limit
        if (currentFrame.depth + 1 > config.maxExplorationDepth) {
            Log.i(TAG, "Max depth reached (${config.maxExplorationDepth})")
            return false
        }

        // Check navigation path for loop prevention
        val pathVisitCount = state.getPathVisitCount(currentFrame.screenHash, newScreenHash)
        if (pathVisitCount >= config.maxPathRevisits) {
            Log.i(TAG, "Blocking re-entry loop (visited $pathVisitCount times)")
            return false
        }

        // Record navigation
        state.recordNavigation(currentFrame.screenHash, newScreenHash)

        // Get fresh root and elements
        val rootNode = clicker.getRootNode() ?: return false
        val newElements = extractClickableElements(rootNode, packageName)
        val newElementsWithUuids = registrar.preGenerateUuids(newElements, packageName)

        val newFrame = ExplorationFrame(
            screenHash = newScreenHash,
            activityName = getActivityName(rootNode),
            elements = newElementsWithUuids.toMutableList(),
            currentElementIndex = 0,
            depth = currentFrame.depth + 1,
            parentScreenHash = currentFrame.screenHash
        )

        state.push(newFrame)
        state.visitedScreens.add(newScreenHash)

        // Register clickable VUIDs
        val clickableUuids = newElementsWithUuids
            .filter { !dangerDetector.isCriticalDangerous(it) }
            .mapNotNull { it.uuid }

        cumulativeTracking.addAllDiscovered(clickableUuids)

        debugCallback?.onStackPush(newScreenHash, newFrame.depth, newElementsWithUuids.size)

        if (config.verboseLogging) {
            Log.d(TAG, "Pushed screen ${newScreenHash.take(8)} (depth: ${newFrame.depth})")
        }

        return true
    }

    /**
     * Pop current frame and navigate back
     */
    suspend fun popAndNavigateBack(state: DFSState) {
        if (state.isNotEmpty()) {
            val frame = state.pop()

            debugCallback?.onStackPop(frame.screenHash, state.stackDepth())

            if (config.verboseLogging) {
                Log.d(TAG, "Popped screen ${frame.screenHash.take(8)}")
            }

            if (state.isNotEmpty()) {
                clicker.pressBack()
                delay(config.screenProcessingDelayMs)
            }
        }
    }

    /**
     * Get exploration statistics
     */
    fun getStats(): String {
        val stats = cumulativeTracking.getStats()
        return "Final Stats: ${stats.completeness.toInt()}% complete " +
               "(${stats.clicked}/${stats.discovered} clicked, ${stats.blocked} blocked)"
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    private fun extractClickableElements(
        rootNode: AccessibilityNodeInfo,
        packageName: String
    ): List<ElementInfo> {
        val elements = mutableListOf<ElementInfo>()
        extractElementsRecursive(rootNode, elements, packageName)
        return elements.filter { it.isClickable }
    }

    private fun extractElementInfoList(
        rootNode: AccessibilityNodeInfo,
        packageName: String
    ): List<ElementInfo> {
        val elements = mutableListOf<ElementInfo>()
        extractElementsRecursive(rootNode, elements, packageName)
        return elements
    }

    private fun extractElementsRecursive(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        packageName: String
    ) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        // Convert Android Rect to KMP Bounds
        val bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom)

        val element = ElementInfo(
            text = node.text?.toString() ?: "",
            contentDescription = node.contentDescription?.toString() ?: "",
            className = node.className?.toString() ?: "Unknown",
            resourceId = node.viewIdResourceName ?: "",
            bounds = bounds,
            isClickable = node.isClickable,
            isEnabled = node.isEnabled,
            isScrollable = node.isScrollable,
            packageName = packageName,
            node = node
        )

        elements.add(element)
        metrics?.onElementDetected()

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractElementsRecursive(child, elements, packageName)
        }
    }

    private fun getActivityName(node: AccessibilityNodeInfo): String? {
        return try {
            val windowInfo = node.window
            windowInfo?.title?.toString()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "DFSExplorer"
    }
}
