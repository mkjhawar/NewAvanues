/**
 * ScreenExplorer.kt - Explores single screen and collects elements
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Updated: 2025-12-22 - Simplified constructor
 *
 * Handles single-screen exploration: element collection, classification
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.elements.ElementClassifier
import com.augmentalis.voiceoscore.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.voiceoscore.learnapp.models.ElementClassification
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ScreenState

/**
 * Screen Explorer
 *
 * Explores a single screen:
 * 1. Capture screen state (fingerprint)
 * 2. Collect all elements
 * 3. Classify elements (safe/dangerous/login/etc.)
 * 4. Return exploration result
 *
 * @property elementClassifier Element classifier
 *
 * @since 1.0.0
 */
class ScreenExplorer(
    private val elementClassifier: ElementClassifier
) {
    private val screenStateManager = ScreenStateManager()

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

        // 3. Collect all visible elements
        val allElements = collectVisibleElements(rootNode)

        // 4. Classify elements
        val classifications = elementClassifier.classifyAll(allElements)

        // 5. Check for login screen
        if (elementClassifier.isLoginScreen(allElements)) {
            return ScreenExplorationResult.LoginScreen(
                screenState = screenState,
                loginElements = classifications
                    .filterIsInstance<ElementClassification.LoginField>()
                    .map { it.element },
                allElements = allElements
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
     * @param node Current node
     * @param visitor Visitor function
     */
    private fun traverseTree(
        node: AccessibilityNodeInfo,
        visitor: (AccessibilityNodeInfo) -> Unit
    ) {
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
     * @property loginElements Login-related elements
     * @property allElements All elements on the login screen
     */
    data class LoginScreen(
        val screenState: ScreenState,
        val loginElements: List<ElementInfo>,
        val allElements: List<ElementInfo> = emptyList()
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
