/**
 * ScreenExplorer.kt - Explores single screen and collects elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/exploration/ScreenExplorer.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Handles single-screen exploration: scrolling, element collection, classification
 */

package com.augmentalis.learnapp.exploration

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.learnapp.elements.ElementClassifier
import com.augmentalis.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.learnapp.models.ElementClassification
import com.augmentalis.learnapp.models.ElementInfo
import com.augmentalis.learnapp.models.ScreenState
import com.augmentalis.learnapp.scrolling.ScrollDetector
import com.augmentalis.learnapp.scrolling.ScrollExecutor

/**
 * Screen Explorer
 *
 * Explores a single screen:
 * 1. Capture screen state (fingerprint)
 * 2. Find scrollable containers
 * 3. Scroll and collect all elements
 * 4. Classify elements (safe/dangerous/login/etc.)
 * 5. Return exploration result
 *
 * ## Usage Example
 *
 * ```kotlin
 * val explorer = ScreenExplorer(...)
 *
 * val rootNode = getRootInActiveWindow()
 * val result = explorer.exploreScreen(rootNode, "com.instagram.android", depth = 0)
 *
 * when (result) {
 *     is ScreenExplorationResult.Success -> {
 *         // Process safe clickable elements
 *         result.safeClickableElements.forEach { element ->
 *             clickAndExplore(element)
 *         }
 *     }
 *     is ScreenExplorationResult.LoginScreen -> {
 *         // Pause and prompt user to login
 *     }
 *     is ScreenExplorationResult.AlreadyVisited -> {
 *         // Skip this screen
 *     }
 * }
 * ```
 *
 * @property screenStateManager Screen state manager
 * @property elementClassifier Element classifier
 * @property scrollDetector Scroll detector
 * @property scrollExecutor Scroll executor
 *
 * @since 1.0.0
 */
class ScreenExplorer(
    private val screenStateManager: ScreenStateManager,
    private val elementClassifier: ElementClassifier,
    private val scrollDetector: ScrollDetector,
    private val scrollExecutor: ScrollExecutor
) {

    /**
     * Explore screen
     *
     * Main entry point. Explores screen and returns result.
     *
     * @param rootNode Root accessibility node
     * @param packageName Package name
     * @param depth Current DFS depth
     * @return Exploration result
     */
    suspend fun exploreScreen(
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        depth: Int
    ): ScreenExplorationResult {
        if (rootNode == null) {
            return ScreenExplorationResult.Error("Root node is null")
        }

        // 1. Capture screen state
        val screenState = screenStateManager.captureScreenState(rootNode, packageName, depth)

        // 2. Check if already visited
        if (screenStateManager.isVisited(screenState.hash)) {
            return ScreenExplorationResult.AlreadyVisited(screenState)
        }

        // 3. Collect all elements (including offscreen via scrolling)
        val allElements = collectAllElements(rootNode)

        // 4. Classify elements
        val classifications = elementClassifier.classifyAll(allElements)

        // 5. Check for login screen
        if (elementClassifier.isLoginScreen(allElements)) {
            return ScreenExplorationResult.LoginScreen(
                screenState = screenState,
                allElements = allElements,
                loginElements = classifications
                    .filterIsInstance<ElementClassification.LoginField>()
                    .map { it.element }
            )
        }

        // 6. Extract safe clickable elements
        val safeClickableElements = classifications
            .filterIsInstance<ElementClassification.SafeClickable>()
            .map { it.element }

        // 7. Extract dangerous elements (for logging)
        val dangerousElements = classifications
            .filterIsInstance<ElementClassification.Dangerous>()
            .map { it.element to it.reason }

        // 8. Return success result
        return ScreenExplorationResult.Success(
            screenState = screenState,
            allElements = allElements,
            safeClickableElements = safeClickableElements,
            dangerousElements = dangerousElements,
            elementClassifications = classifications
        )
    }

    /**
     * Collect all elements from screen
     *
     * Includes offscreen elements via scrolling.
     *
     * @param rootNode Root node
     * @return List of all elements
     */
    private suspend fun collectAllElements(rootNode: AccessibilityNodeInfo): List<ElementInfo> {
        val allElements = mutableSetOf<ElementInfo>()

        // 1. Collect visible elements
        val visibleElements = collectVisibleElements(rootNode)
        allElements.addAll(visibleElements)

        // 2. Find scrollable containers
        val scrollables = scrollDetector.findScrollableContainers(rootNode)

        // 3. Scroll each container and collect offscreen elements
        for (scrollable in scrollables) {
            try {
                val scrolledElements = scrollExecutor.scrollAndCollectAll(scrollable)
                allElements.addAll(scrolledElements)
            } catch (e: Exception) {
                // Handle scrolling errors gracefully
                // Continue with other scrollables
            }
        }

        return allElements.toList()
    }

    /**
     * Collect visible elements
     *
     * Traverses tree and collects all elements.
     *
     * @param rootNode Root node
     * @return List of visible elements
     */
    private fun collectVisibleElements(rootNode: AccessibilityNodeInfo): List<ElementInfo> {
        val elements = mutableListOf<ElementInfo>()

        traverseTree(rootNode) { node ->
            elements.add(ElementInfo.fromNode(node))
        }

        return elements
    }

    /**
     * Traverse tree (DFS)
     *
     * Skips animated/dynamic content containers to prevent overwhelming
     * the exploration with thousands of non-interactive nodes.
     *
     * @param node Current node
     * @param visitor Visitor function
     */
    private fun traverseTree(
        node: AccessibilityNodeInfo,
        visitor: (AccessibilityNodeInfo) -> Unit
    ) {
        // Skip animated/dynamic content containers
        if (shouldSkipNode(node)) {
            return
        }

        visitor(node)

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                try {
                    traverseTree(child, visitor)
                } finally {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        }
    }

    /**
     * Check if node should be skipped during traversal
     *
     * Skips non-interactive animated content that generates excessive events.
     *
     * @param node Node to check
     * @return true if should skip
     */
    private fun shouldSkipNode(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""

        // Skip known animated/dynamic content types
        val animatedTypes = listOf(
            "SurfaceView",           // Video/game rendering
            "TextureView",           // Video playback
            "GLSurfaceView",         // 3D graphics
            "VideoView",             // Video
            "WebView",               // Web content (often has animations)
            "ProgressBar",           // Animated progress indicators
            "SeekBar",               // Can animate
            "RatingBar",             // Can animate
            "AnimationDrawable",     // Explicit animations
            "Canvas",                // Custom drawing (often animated)
            "Chart",                 // Graph libraries
            "Graph"                  // Graph libraries
        )

        // Skip if matches animated type
        if (animatedTypes.any { className.contains(it, ignoreCase = true) }) {
            return true
        }

        // Skip if not visible
        if (!node.isVisibleToUser) {
            return true
        }

        return false
    }
}

/**
 * Screen Exploration Result
 *
 * Sealed class representing result of screen exploration.
 *
 * @since 1.0.0
 */
sealed class ScreenExplorationResult {

    /**
     * Success - screen explored successfully
     *
     * @property screenState Screen state
     * @property allElements All elements on screen
     * @property safeClickableElements Safe clickable elements
     * @property dangerousElements Dangerous elements (element, reason)
     * @property elementClassifications All element classifications
     */
    data class Success(
        val screenState: ScreenState,
        val allElements: List<ElementInfo>,
        val safeClickableElements: List<ElementInfo>,
        val dangerousElements: List<Pair<ElementInfo, String>>,
        val elementClassifications: List<ElementClassification>
    ) : ScreenExplorationResult()

    /**
     * Already visited - screen was previously explored
     *
     * @property screenState Screen state
     */
    data class AlreadyVisited(
        val screenState: ScreenState
    ) : ScreenExplorationResult()

    /**
     * Login screen - pause for user login
     *
     * @property screenState Screen state
     * @property allElements All elements on screen (for registration)
     * @property loginElements Login-related elements
     */
    data class LoginScreen(
        val screenState: ScreenState,
        val allElements: List<ElementInfo>,
        val loginElements: List<ElementInfo>
    ) : ScreenExplorationResult()

    /**
     * Error - exploration failed
     *
     * @property message Error message
     */
    data class Error(
        val message: String
    ) : ScreenExplorationResult()
}
