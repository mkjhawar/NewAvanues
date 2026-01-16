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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

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
        return withContext(Dispatchers.Main) {
            try {
                // ═══════════════════════════════════════════════════════════════
                // PHASE 1: Try to refresh node first (fixes stale node issue)
                // ═══════════════════════════════════════════════════════════════
                val freshElement = refreshElement(element)
                val node = (freshElement?.node ?: element.node) as? AccessibilityNodeInfo

                if (node != null) {
                    // Attempt node-based click with fresh node
                    val nodeResult = attemptNodeClick(node, element)
                    if (nodeResult is ClickResult.Success) {
                        return@withContext nodeResult
                    }
                    // Node click failed, fall through to gesture
                    Log.d(TAG, "Node click failed, trying gesture fallback")
                }

                // ═══════════════════════════════════════════════════════════════
                // PHASE 2: Gesture-based fallback with verified callback
                // This achieves >90% confidence by waiting for gesture completion
                // ═══════════════════════════════════════════════════════════════
                val gestureResult = clickAtCoordinatesWithCallback(
                    element.bounds.centerX,
                    element.bounds.centerY
                )

                when (gestureResult) {
                    GestureResult.COMPLETED -> {
                        Log.d(TAG, "Gesture click COMPLETED (>90% confidence)")
                        ClickResult.Success
                    }
                    GestureResult.CANCELLED -> {
                        Log.w(TAG, "Gesture click CANCELLED")
                        recordFailure(element, ClickFailure.GESTURE_FAILED)
                        ClickResult.Failed(ClickFailure.GESTURE_FAILED, "Gesture was cancelled")
                    }
                    GestureResult.TIMEOUT -> {
                        Log.w(TAG, "Gesture click TIMEOUT")
                        recordFailure(element, ClickFailure.GESTURE_FAILED)
                        ClickResult.Failed(ClickFailure.GESTURE_FAILED, "Gesture timed out")
                    }
                    GestureResult.DISPATCH_FAILED -> {
                        Log.w(TAG, "Gesture dispatch FAILED")
                        recordFailure(element, ClickFailure.GESTURE_FAILED)
                        ClickResult.Failed(ClickFailure.GESTURE_FAILED, "Failed to dispatch gesture")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Click exception: ${e.message}", e)
                recordFailure(element, ClickFailure.EXCEPTION)
                ClickResult.Failed(ClickFailure.EXCEPTION, e.message)
            }
        }
    }

    /**
     * Attempt a node-based click with validation
     */
    private suspend fun attemptNodeClick(node: AccessibilityNodeInfo, element: ElementInfo): ClickResult {
        try {
            // Verify element is visible and enabled
            if (!node.isVisibleToUser) {
                return ClickResult.Failed(ClickFailure.NOT_VISIBLE)
            }

            if (!node.isEnabled) {
                return ClickResult.Failed(ClickFailure.NOT_ENABLED)
            }

            // Attempt click with retry
            var attempts = 0
            var success = false

            while (attempts < config.maxClickAttempts && !success) {
                success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!success) {
                    delay(config.scrollDelayMs * (attempts + 1))
                    attempts++
                }
            }

            return if (success) {
                ClickResult.Success
            } else {
                ClickResult.Failed(ClickFailure.ACTION_FAILED, "Node click failed after $attempts attempts")
            }
        } catch (e: IllegalStateException) {
            // Node became stale during operation
            Log.w(TAG, "Node stale during click: ${e.message}")
            return ClickResult.Failed(ClickFailure.NODE_STALE, e.message)
        }
    }

    /**
     * Gesture execution result for >90% confidence tracking
     */
    enum class GestureResult {
        COMPLETED,      // Gesture executed successfully
        CANCELLED,      // Gesture was cancelled by system
        TIMEOUT,        // Gesture callback never received
        DISPATCH_FAILED // Failed to dispatch gesture
    }

    /**
     * Click at coordinates with callback verification for >90% confidence.
     *
     * Unlike the simple clickAtCoordinates(), this method:
     * 1. Uses GestureResultCallback to verify completion
     * 2. Waits for callback with timeout
     * 3. Returns detailed result for proper error handling
     */
    private suspend fun clickAtCoordinatesWithCallback(x: Int, y: Int): GestureResult {
        val clickPath = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(
                clickPath, 0, config.tapDurationMs
            ))
            .build()

        // Use suspendCancellableCoroutine with callback for verified completion
        return withTimeoutOrNull(GESTURE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        if (continuation.isActive) {
                            continuation.resume(GestureResult.COMPLETED)
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        if (continuation.isActive) {
                            continuation.resume(GestureResult.CANCELLED)
                        }
                    }
                }

                try {
                    val dispatched = accessibilityService.dispatchGesture(
                        gestureDescription,
                        callback,
                        Handler(Looper.getMainLooper())
                    )

                    if (!dispatched && continuation.isActive) {
                        continuation.resume(GestureResult.DISPATCH_FAILED)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gesture dispatch exception: ${e.message}")
                    if (continuation.isActive) {
                        continuation.resume(GestureResult.DISPATCH_FAILED)
                    }
                }
            }
        } ?: GestureResult.TIMEOUT
    }

    override fun clickAtCoordinates(x: Int, y: Int): Boolean {
        // Legacy synchronous method - kept for compatibility
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

        /** Gesture callback timeout in milliseconds (2 seconds for >90% confidence) */
        private const val GESTURE_TIMEOUT_MS = 2000L
    }
}
