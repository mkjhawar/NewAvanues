/**
 * ElementClicker.kt - Android element click operations
 *
 * Handles all element click operations including node-based clicks,
 * gesture-based fallbacks, and retry logic.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Android implementation of element click operations.
 *
 * Uses AccessibilityService for:
 * - Node-based clicks via performAction()
 * - Gesture-based clicks via dispatchGesture()
 * - Global actions (back, home)
 *
 * @property accessibilityService Accessibility service for UI actions
 * @property config Exploration configuration
 */
class ElementClicker(
    private val accessibilityService: AccessibilityService,
    private val config: ExplorationConfig = ExplorationConfig.DEFAULT
) : IElementClicker {

    private val clickFailures = mutableListOf<ClickFailureReason>()

    override suspend fun clickElement(element: ElementInfo): ClickResult {
        val node = element.node as? AccessibilityNodeInfo
            ?: return ClickResult.Failed(ClickFailure.NODE_STALE, "Node is null or invalid type")

        return withContext(Dispatchers.Main) {
            try {
                // 1. Verify element is visible and enabled
                if (!node.isVisibleToUser) {
                    recordFailure(element, ClickFailure.NOT_VISIBLE)
                    return@withContext ClickResult.Failed(ClickFailure.NOT_VISIBLE)
                }

                if (!node.isEnabled) {
                    recordFailure(element, ClickFailure.NOT_ENABLED)
                    return@withContext ClickResult.Failed(ClickFailure.NOT_ENABLED)
                }

                // 2. Get bounds and verify on screen
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                // 3. Attempt click with retry
                var attempts = 0
                var success = false

                while (attempts < config.maxClickAttempts && !success) {
                    success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                    if (!success) {
                        delay(config.scrollDelayMs * (attempts + 1)) // Exponential backoff
                        attempts++
                    }
                }

                if (success) {
                    ClickResult.Success
                } else {
                    // Try gesture fallback
                    val gestureSuccess = clickAtCoordinates(bounds.centerX(), bounds.centerY())
                    if (gestureSuccess) {
                        ClickResult.Success
                    } else {
                        recordFailure(element, ClickFailure.ACTION_FAILED)
                        ClickResult.Failed(ClickFailure.ACTION_FAILED, "Click failed after $attempts attempts")
                    }
                }

            } catch (e: Exception) {
                recordFailure(element, ClickFailure.EXCEPTION)
                ClickResult.Failed(ClickFailure.EXCEPTION, e.message)
            }
        }
    }

    override fun clickAtCoordinates(x: Int, y: Int): Boolean {
        val clickPath = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(
                clickPath, 0, config.tapDurationMs
            ))
            .build()

        return try {
            accessibilityService.dispatchGesture(
                gestureDescription,
                null,
                null
            )
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun pressBack() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override fun dismissKeyboard() {
        try {
            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun refreshElement(element: ElementInfo): ElementInfo? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return null
            val freshNode = findNodeByBounds(rootNode, element.bounds)

            if (freshNode != null) {
                element.copy(node = freshNode)
            } else {
                rootNode.recycle()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getClickFailures(): List<ClickFailureReason> = clickFailures.toList()

    override fun clearClickFailures() {
        clickFailures.clear()
    }

    /**
     * Get the current root accessibility node
     */
    fun getRootNode(): AccessibilityNodeInfo? = accessibilityService.rootInActiveWindow

    /**
     * Find node at specific bounds by traversing UI tree.
     */
    fun findNodeByBounds(
        root: AccessibilityNodeInfo,
        targetBounds: Bounds,
        tolerance: Int = config.boundsTolerance
    ): AccessibilityNodeInfo? {
        val bounds = Rect()
        root.getBoundsInScreen(bounds)

        // Convert target Bounds to Rect for comparison
        val targetRect = Rect(targetBounds.left, targetBounds.top, targetBounds.right, targetBounds.bottom)

        if (bounds == targetRect || areBoundsSimilar(bounds, targetRect, tolerance)) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findNodeByBounds(child, targetBounds, tolerance)
            if (result != null) {
                return result
            }
            child.recycle()
        }

        return null
    }

    private fun areBoundsSimilar(bounds1: Rect, bounds2: Rect, tolerance: Int): Boolean {
        return kotlin.math.abs(bounds1.left - bounds2.left) <= tolerance &&
               kotlin.math.abs(bounds1.top - bounds2.top) <= tolerance &&
               kotlin.math.abs(bounds1.right - bounds2.right) <= tolerance &&
               kotlin.math.abs(bounds1.bottom - bounds2.bottom) <= tolerance
    }

    private fun recordFailure(element: ElementInfo, reason: ClickFailure) {
        val desc = element.text ?: element.contentDescription ?: element.className
        clickFailures.add(ClickFailureReason(
            elementDesc = desc ?: "Unknown",
            elementType = element.className,
            reason = reason,
            timestamp = System.currentTimeMillis()
        ))
    }

    companion object {
        private const val TAG = "ElementClicker"
    }
}
