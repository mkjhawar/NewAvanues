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

package com.augmentalis.voiceoscore.learnapp.exploration

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.elements.ElementClassifier
import com.augmentalis.voiceoscore.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.voiceoscore.learnapp.models.ElementClassification
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import com.augmentalis.voiceoscore.learnapp.scrolling.ScrollDetector
import com.augmentalis.voiceoscore.learnapp.scrolling.ScrollExecutor
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings

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
    private val context: Context,
    private val screenStateManager: ScreenStateManager,
    private val elementClassifier: ElementClassifier,
    private val scrollDetector: ScrollDetector,
    private val scrollExecutor: ScrollExecutor
) {

    /**
     * Developer settings
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

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
        val collectionResult = collectAllElements(rootNode)
        val allElements = collectionResult.elements
        val scrollableCount = collectionResult.scrollableContainerCount

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

        // FIX (2025-11-24): Add detailed logging for bottom nav detection debugging
        // Issue: Clock app showing only 4/5 screens explored
        // Debug: Log all elements and their classifications to trace missing bottom nav item
        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d("ScreenExplorer", "=== ELEMENT CLASSIFICATION SUMMARY ===")
            android.util.Log.d("ScreenExplorer", "Total elements: ${allElements.size}")
            android.util.Log.d("ScreenExplorer", "Safe clickable: ${safeClickableElements.size}")

            // Log bottom-positioned elements specifically
            val bottomThreshold = developerSettings.getBottomNavThreshold()
            val bottomElements = allElements.filter { it.bounds.top >= bottomThreshold }
            android.util.Log.d("ScreenExplorer", "Bottom-positioned elements (Y>=$bottomThreshold): ${bottomElements.size}")
            bottomElements.forEachIndexed { i, elem ->
                val classification = classifications.find {
                    when (it) {
                        is ElementClassification.SafeClickable -> it.element == elem
                        is ElementClassification.Dangerous -> it.element == elem
                        is ElementClassification.Disabled -> it.element == elem
                        is ElementClassification.EditText -> it.element == elem
                        is ElementClassification.LoginField -> it.element == elem
                        is ElementClassification.NonClickable -> it.element == elem
                    }
                }
                val classType = when (classification) {
                    is ElementClassification.SafeClickable -> "✅ SAFE"
                    is ElementClassification.NonClickable -> "❌ NON-CLICKABLE"
                    is ElementClassification.Dangerous -> "⚠️ DANGEROUS"
                    else -> "OTHER"
                }
                android.util.Log.d("ScreenExplorer",
                    "  [$i] $classType - \"${elem.text}\" / \"${elem.contentDescription}\" " +
                    "(clickable=${elem.isClickable}, bounds=${elem.bounds})")
            }
        }

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
            elementClassifications = classifications,
            scrollableContainerCount = scrollableCount
        )
    }

    /**
     * Result of element collection including scrollable container tracking
     */
    private data class CollectionResult(
        val elements: List<ElementInfo>,
        val scrollableContainerCount: Int
    )

    /**
     * Collect all elements from screen
     *
     * Includes offscreen elements via scrolling.
     *
     * @param rootNode Root node
     * @return Collection result with elements and scrollable container count
     */
    private suspend fun collectAllElements(rootNode: AccessibilityNodeInfo): CollectionResult {
        val allElements = mutableSetOf<ElementInfo>()

        // 1. Collect visible elements
        val visibleElements = collectVisibleElements(rootNode)
        allElements.addAll(visibleElements)

        // 2. Find scrollable containers
        val scrollables = scrollDetector.findScrollableContainers(rootNode)
        val scrollableCount = scrollables.size

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

        return CollectionResult(
            elements = allElements.toList(),
            scrollableContainerCount = scrollableCount
        )
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
            elements.add(ElementInfo.fromNode(node, elementClassifier))
        }

        return elements
    }

    /**
     * Traverse tree (DFS)
     *
     * Skips animated/dynamic content containers to prevent overwhelming
     * the exploration with thousands of non-interactive nodes.
     *
     * FIX (2025-12-03): Properly recycle child nodes during traversal
     * to prevent memory leaks. AccessibilityNodeInfo holds native resources.
     *
     * FIX (2025-12-04): Limit children per container to MAX_CHILDREN_PER_CONTAINER
     * to prevent memory exhaustion and performance degradation.
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

        // Limit children per container to prevent memory exhaustion
        val maxChildren = minOf(node.childCount, developerSettings.getMaxChildrenPerContainerExploration())

        for (i in 0 until maxChildren) {
            node.getChild(i)?.let { child ->
                try {
                    traverseTree(child, visitor)
                } finally {
                    // FIX (2025-12-03): Always recycle child nodes after use
                    // On Android U+ (API 34+), recycle() is deprecated and automatic
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
     * @property scrollableContainerCount Number of scrollable containers found on this screen
     */
    data class Success(
        val screenState: ScreenState,
        val allElements: List<ElementInfo>,
        val safeClickableElements: List<ElementInfo>,
        val dangerousElements: List<Pair<ElementInfo, String>>,
        val elementClassifications: List<ElementClassification>,
        val scrollableContainerCount: Int = 0
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
