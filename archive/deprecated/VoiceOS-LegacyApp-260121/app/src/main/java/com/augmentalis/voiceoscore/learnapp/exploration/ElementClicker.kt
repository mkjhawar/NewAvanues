/**
 * ElementClicker.kt - Element click operations with retry and fallback
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ElementClicker.kt
 *
 * Author: Manoj Jhawar (refactored by Claude)
 * Created: 2025-12-04
 * Refactored: 2026-01-15 (SOLID extraction from ExplorationEngine.kt)
 *
 * Single Responsibility: Handles all element click operations including
 * node-based clicks, gesture-based fallbacks, and retry logic.
 *
 * Extracted from ExplorationEngine.kt to improve maintainability and testability.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Click failure reason tracking for telemetry
 *
 * @property elementDesc Description of the element
 * @property elementType Type of the element (class name)
 * @property reason Failure reason code
 * @property timestamp When the failure occurred
 */
data class ClickFailureReason(
    val elementDesc: String,
    val elementType: String,
    val reason: String,
    val timestamp: Long
)

/**
 * Handles element click operations with retry logic and fallback mechanisms.
 *
 * This class provides:
 * - Node-based click with validation and retry
 * - Gesture-based (coordinate) click as fallback
 * - Node refresh and bounds-based lookup
 * - Click failure tracking for telemetry
 *
 * ## Click Strategy
 *
 * 1. Verify element is visible and enabled
 * 2. Ensure element is on screen (scroll if needed)
 * 3. Attempt node click with exponential backoff retry
 * 4. Fall back to gesture click if node fails
 *
 * ## Usage Example
 *
 * ```kotlin
 * val clicker = ElementClicker(context, accessibilityService)
 *
 * // Click with node
 * val success = clicker.clickElement(node, "Submit", "Button")
 *
 * // Gesture click at coordinates
 * val gestureSuccess = clicker.performCoordinateClick(bounds)
 *
 * // Get failure telemetry
 * val failures = clicker.getClickFailures()
 * ```
 *
 * @property context Android context
 * @property accessibilityService Accessibility service for actions
 */
class ElementClicker(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) {
    /**
     * Developer settings for tunable parameters
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    /**
     * List of click failures for telemetry
     */
    private val clickFailures = mutableListOf<ClickFailureReason>()

    /**
     * Click element with enhanced retry logic and validation.
     *
     * Enhancements:
     * 1. Verify element is visible and enabled before clicking
     * 2. Check element is on screen, scroll into view if needed
     * 3. Retry with exponential backoff (500ms, 1000ms)
     * 4. Track failure reasons for telemetry
     *
     * @param node Node to click
     * @param elementDesc Element description for logging (optional)
     * @param elementType Element type for logging (optional)
     * @return true if clicked successfully
     */
    suspend fun clickElement(
        node: AccessibilityNodeInfo?,
        elementDesc: String? = null,
        elementType: String? = null
    ): Boolean {
        if (node == null) return false

        return withContext(Dispatchers.Main) {
            try {
                // 1. Verify element is visible and enabled
                if (!node.isVisibleToUser) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d(TAG, "Element not visible to user")
                    }
                    if (elementDesc != null && elementType != null) {
                        recordFailure(elementDesc, elementType, "not_visible")
                    }
                    return@withContext false
                }

                if (!node.isEnabled) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d(TAG, "Element not enabled")
                    }
                    if (elementDesc != null && elementType != null) {
                        recordFailure(elementDesc, elementType, "not_enabled")
                    }
                    return@withContext false
                }

                // 2. Get bounds and verify on screen
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                val displayMetrics = context.resources.displayMetrics
                val screenBounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)

                if (!screenBounds.contains(bounds)) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d(TAG, "Element not on screen, attempting scroll")
                    }
                    // Try to scroll into view
                    val scrolled = node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                    if (!scrolled) {
                        if (elementDesc != null && elementType != null) {
                            recordFailure(elementDesc, elementType, "scroll_failed")
                        }
                        return@withContext false
                    }
                    delay(developerSettings.getClickDelayMs())
                }

                // 3. Attempt click with retry
                var attempts = 0
                var success = false

                while (attempts < MAX_CLICK_ATTEMPTS && !success) {
                    success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                    if (!success) {
                        delay(developerSettings.getScrollDelayMs() * (attempts + 1)) // Exponential backoff
                        attempts++
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d(TAG, "Click attempt $attempts failed, retrying...")
                        }
                    }
                }

                if (success) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d(TAG, "Click succeeded on attempt ${attempts + 1}")
                    }
                } else {
                    android.util.Log.w(TAG, "Click failed after $MAX_CLICK_ATTEMPTS attempts")
                    if (elementDesc != null && elementType != null) {
                        recordFailure(elementDesc, elementType, "action_failed")
                    }
                }

                return@withContext success

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Click failed with exception: ${e.message}", e)
                if (elementDesc != null && elementType != null) {
                    recordFailure(elementDesc, elementType, "exception:${e.message}")
                }
                return@withContext false
            }
        }
    }

    /**
     * Perform coordinate-based click using gesture dispatch
     *
     * This is used as a fallback when AccessibilityNodeInfo cannot be refreshed,
     * but we still have the element's bounds from the original scrape.
     *
     * @param bounds The bounds of the element to click
     * @return true if gesture was dispatched successfully, false otherwise
     */
    fun performCoordinateClick(bounds: Rect): Boolean {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        val clickPath = Path().apply {
            moveTo(centerX, centerY)
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(
                clickPath, 0, TAP_DURATION_MS
            ))
            .build()

        return try {
            accessibilityService.dispatchGesture(
                gestureDescription,
                null,  // callback
                null   // handler
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Coordinate-based click failed: ${e.message}", e)
            false
        }
    }

    /**
     * Refresh an AccessibilityNodeInfo by re-scraping at the same bounds.
     *
     * This function addresses stale node issues by refreshing nodes
     * immediately before clicking. AccessibilityNodeInfo references become invalid
     * within 500ms, so we re-scrape the UI tree to get a fresh reference.
     *
     * @param element Element whose node needs refreshing
     * @return Fresh AccessibilityNodeInfo or null if element no longer exists
     */
    fun refreshAccessibilityNode(element: ElementInfo): AccessibilityNodeInfo? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return null
            val result = findNodeByBounds(rootNode, element.bounds)

            // Don't recycle rootNode here - findNodeByBounds returns a child node or rootNode itself
            // Only recycle if findNodeByBounds returns null (no match found)
            if (result == null) {
                rootNode.recycle()
            }

            result
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to refresh node: ${e.message}")
            null
        }
    }

    /**
     * Scrape element by bounds as last resort when refresh fails.
     *
     * This is a fallback for when findNodeByBounds fails to locate an element.
     * We get a fresh root node and try one more time.
     *
     * @param bounds Target bounds to scrape
     * @return Fresh node or null if element no longer exists
     */
    fun scrapeElementByBounds(bounds: Rect): AccessibilityNodeInfo? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return null
            val result = findNodeByBounds(rootNode, bounds)

            // MEMORY FIX: Recycle rootNode unless it IS the result (caller owns result)
            if (result == null || result !== rootNode) {
                @Suppress("DEPRECATION")
                rootNode.recycle()
            }

            result
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to scrape element by bounds: ${e.message}")
            null
        }
    }

    /**
     * Find node at specific bounds coordinates by traversing UI tree.
     *
     * Uses fuzzy bounds matching with tolerance to handle UI shifts.
     *
     * @param root Root node to start search from
     * @param targetBounds Target bounds to match
     * @param tolerance Maximum pixel difference allowed (default: 5px)
     * @return Matching node or null if not found
     */
    fun findNodeByBounds(
        root: AccessibilityNodeInfo,
        targetBounds: Rect,
        tolerance: Int = DEFAULT_BOUNDS_TOLERANCE
    ): AccessibilityNodeInfo? {
        val bounds = Rect()
        root.getBoundsInScreen(bounds)

        // Check if this node matches (exact match first for performance)
        if (bounds == targetBounds) {
            return root
        }

        // Check fuzzy match within tolerance
        if (tolerance > 0 && areBoundsSimilar(bounds, targetBounds, tolerance)) {
            return root
        }

        // Search children recursively
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val childBounds = Rect()
            child.getBoundsInScreen(childBounds)

            if (childBounds == targetBounds) {
                return child
            }

            val result = findNodeByBounds(child, targetBounds, tolerance)
            if (result != null) {
                return result
            }
            // Recycle child only if it didn't match
            child.recycle()
        }

        return null
    }

    /**
     * Check if two bounds rectangles are similar within a tolerance.
     *
     * @param bounds1 First bounds rectangle
     * @param bounds2 Second bounds rectangle
     * @param tolerance Maximum pixel difference allowed on each edge
     * @return true if bounds are within tolerance
     */
    fun areBoundsSimilar(bounds1: Rect, bounds2: Rect, tolerance: Int): Boolean {
        return kotlin.math.abs(bounds1.left - bounds2.left) <= tolerance &&
               kotlin.math.abs(bounds1.top - bounds2.top) <= tolerance &&
               kotlin.math.abs(bounds1.right - bounds2.right) <= tolerance &&
               kotlin.math.abs(bounds1.bottom - bounds2.bottom) <= tolerance
    }

    /**
     * Press back button
     */
    suspend fun pressBack() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Dismiss soft keyboard if visible.
     */
    fun dismissKeyboard() {
        try {
            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d(TAG, "Attempted keyboard dismissal via BACK action")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to dismiss keyboard: ${e.message}")
        }
    }

    /**
     * Record a click failure for telemetry
     */
    private fun recordFailure(elementDesc: String, elementType: String, reason: String) {
        clickFailures.add(ClickFailureReason(elementDesc, elementType, reason, System.currentTimeMillis()))
    }

    /**
     * Get all recorded click failures
     *
     * @return List of click failure reasons
     */
    fun getClickFailures(): List<ClickFailureReason> = clickFailures.toList()

    /**
     * Clear click failures (call at start of each screen exploration)
     */
    fun clearClickFailures() {
        clickFailures.clear()
    }

    /**
     * Get the current root node
     *
     * @return Root AccessibilityNodeInfo or null
     */
    fun getRootNode(): AccessibilityNodeInfo? = accessibilityService.rootInActiveWindow

    companion object {
        private const val TAG = "ElementClicker"
        private const val MAX_CLICK_ATTEMPTS = 3
        private const val TAP_DURATION_MS = 50L
        private const val DEFAULT_BOUNDS_TOLERANCE = 5
    }
}
