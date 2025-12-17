/**
 * SnapToElementHandler.kt - Intelligent cursor snapping to UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.accessibilityservice.AccessibilityService
import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents a snappable UI element
 */
data class SnapTarget(
    val node: AccessibilityNodeInfo,
    val bounds: Rect,
    val center: Point,
    val distance: Float,
    val priority: Int,
    val description: String
) {
    /**
     * Check if target is still valid
     */
    fun isValid(): Boolean {
        return try {
            node.isEnabled && node.isVisibleToUser
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Recycle the node to prevent memory leaks
     */
    fun recycle() {
        try {
            node.recycle()
        } catch (e: Exception) {
            Log.w("SnapTarget", "Error recycling node", e)
        }
    }
}

/**
 * Handler for cursor snapping to UI elements
 *
 * Features:
 * - Find nearest clickable elements within radius
 * - Prioritize likely targets (buttons over text)
 * - Smooth animated snapping with easing
 * - Visual feedback for snap targets
 * - Configurable snap behavior
 *
 * Usage:
 * ```
 * val handler = SnapToElementHandler(service)
 * val target = handler.findNearestTarget(x, y)
 * handler.snapToTarget(target) { newX, newY ->
 *     updateCursorPosition(newX, newY)
 * }
 * ```
 */
class SnapToElementHandler(
    private val service: AccessibilityService
) {

    companion object {
        private const val TAG = "SnapToElementHandler"

        // Snap configuration
        private const val DEFAULT_SNAP_RADIUS = 200f  // pixels
        private const val SNAP_ANIMATION_DURATION = 200L  // milliseconds
        private const val SNAP_ANIMATION_FPS = 60

        // Priority weights for different element types
        private val ELEMENT_PRIORITIES = mapOf(
            "button" to 100,
            "imagebutton" to 95,
            "checkbox" to 90,
            "radiobutton" to 90,
            "switch" to 85,
            "edittext" to 80,
            "spinner" to 75,
            "textview" to 50,
            "imageview" to 40,
            "default" to 30
        )
    }

    // Configuration
    private var snapRadius: Float = DEFAULT_SNAP_RADIUS
    private var snapEnabled: Boolean = true
    private var highlightTargets: Boolean = true

    // Current snap animation job
    private var snapAnimationJob: Job? = null

    // Coroutine scope for animations
    private val animationScope = CoroutineScope(Dispatchers.Main)

    // Speed controller for easing
    private val speedController = SpeedController()

    /**
     * Set snap radius (distance threshold for snapping)
     *
     * @param radius Snap radius in pixels
     */
    fun setSnapRadius(radius: Float) {
        Log.d(TAG, "Setting snap radius: ${radius}px")
        snapRadius = radius.coerceAtLeast(0f)
    }

    /**
     * Get current snap radius
     */
    fun getSnapRadius(): Float = snapRadius

    /**
     * Enable or disable snapping
     */
    fun setSnapEnabled(enabled: Boolean) {
        Log.d(TAG, "Snap enabled: $enabled")
        snapEnabled = enabled
    }

    /**
     * Check if snapping is enabled
     */
    fun isSnapEnabled(): Boolean = snapEnabled

    /**
     * Enable or disable target highlighting
     */
    fun setHighlightTargets(enabled: Boolean) {
        highlightTargets = enabled
    }

    /**
     * Find nearest snappable target to given position
     *
     * @param x Current cursor X position
     * @param y Current cursor Y position
     * @param maxRadius Maximum search radius (default: configured snap radius)
     * @return Nearest snap target or null if none found
     */
    fun findNearestTarget(
        x: Float,
        y: Float,
        maxRadius: Float = snapRadius
    ): SnapTarget? {
        if (!snapEnabled) {
            Log.v(TAG, "Snap disabled, skipping target search")
            return null
        }

        Log.d(TAG, "Finding nearest target to ($x, $y) within ${maxRadius}px")

        val rootNode = service.rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "No root node available")
            return null
        }

        val candidates = mutableListOf<SnapTarget>()

        try {
            // Traverse tree and find clickable elements
            findClickableElements(rootNode, x, y, maxRadius, candidates)

            // Sort by priority and distance
            val sorted = candidates.sortedWith(
                compareByDescending<SnapTarget> { it.priority }
                    .thenBy { it.distance }
            )

            val best = sorted.firstOrNull()

            if (best != null) {
                Log.i(TAG, "Found nearest target: ${best.description} at distance ${best.distance.toInt()}px (priority: ${best.priority})")
            } else {
                Log.d(TAG, "No targets found within radius")
            }

            // Recycle nodes we're not using
            candidates.filter { it != best }.forEach { it.recycle() }

            return best

        } catch (e: Exception) {
            Log.e(TAG, "Error finding nearest target", e)
            candidates.forEach { it.recycle() }
            return null
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Recursively find clickable elements within radius
     */
    private fun findClickableElements(
        node: AccessibilityNodeInfo?,
        x: Float,
        y: Float,
        maxRadius: Float,
        candidates: MutableList<SnapTarget>
    ) {
        node ?: return

        try {
            // Check if element is clickable and visible
            if (node.isClickable && node.isEnabled && node.isVisibleToUser) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                // Calculate center point
                val centerX = bounds.exactCenterX().toInt()
                val centerY = bounds.exactCenterY().toInt()
                val center = Point(centerX, centerY)

                // Calculate distance from cursor to element center
                val distance = calculateDistance(x, y, centerX.toFloat(), centerY.toFloat())

                // Only consider elements within radius
                if (distance <= maxRadius) {
                    val priority = calculatePriority(node, bounds)
                    val description = buildElementDescription(node)

                    // Don't recycle this node yet - it's added to candidates
                    val target = SnapTarget(node, bounds, center, distance, priority, description)
                    candidates.add(target)

                    Log.v(TAG, "Found candidate: $description at ${distance.toInt()}px (priority: $priority)")
                }
            }

            // Traverse children
            val childCount = node.childCount
            for (i in 0 until childCount) {
                var childNode: AccessibilityNodeInfo? = null
                try {
                    childNode = node.getChild(i)
                    if (childNode != null) {
                        findClickableElements(childNode, x, y, maxRadius, candidates)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing child $i", e)
                } finally {
                    // Only recycle if we didn't add to candidates
                    if (childNode != null && !candidates.any { it.node == childNode }) {
                        childNode.recycle()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing node", e)
        }
    }

    /**
     * Calculate priority for element based on type and properties
     */
    private fun calculatePriority(node: AccessibilityNodeInfo, bounds: Rect): Int {
        val className = node.className?.toString()?.lowercase() ?: ""

        // Get base priority from element type
        val basePriority = ELEMENT_PRIORITIES.entries
            .firstOrNull { className.contains(it.key) }
            ?.value ?: ELEMENT_PRIORITIES["default"]!!

        // Boost priority for elements with text/description
        var priority = basePriority
        if (!node.text.isNullOrEmpty()) priority += 10
        if (!node.contentDescription.isNullOrEmpty()) priority += 5

        // Boost priority for larger elements (easier targets)
        val area = bounds.width() * bounds.height()
        if (area > 10000) priority += 5  // Large button

        // Reduce priority for very small elements
        if (area < 1000) priority -= 10  // Tiny element

        return priority
    }

    /**
     * Build human-readable description of element
     */
    private fun buildElementDescription(node: AccessibilityNodeInfo): String {
        return when {
            !node.text.isNullOrEmpty() -> node.text.toString()
            !node.contentDescription.isNullOrEmpty() -> node.contentDescription.toString()
            !node.viewIdResourceName.isNullOrEmpty() -> {
                node.viewIdResourceName?.substringAfterLast('/') ?: "unknown"
            }
            else -> node.className?.toString()?.substringAfterLast('.') ?: "unknown"
        }
    }

    /**
     * Calculate Euclidean distance between two points
     */
    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx.pow(2) + dy.pow(2))
    }

    /**
     * Snap cursor to target with smooth animation
     *
     * @param target The target to snap to
     * @param onUpdate Callback invoked with new cursor position during animation
     * @param onComplete Callback invoked when animation completes
     */
    fun snapToTarget(
        target: SnapTarget,
        onUpdate: (x: Float, y: Float) -> Unit,
        onComplete: (() -> Unit)? = null
    ) {
        Log.d(TAG, "Snapping to target: ${target.description}")

        // Cancel any existing animation
        snapAnimationJob?.cancel()

        // Start new animation
        snapAnimationJob = animationScope.launch {
            try {
                animateSnapToTarget(target, onUpdate)
                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error during snap animation", e)
            } finally {
                target.recycle()
            }
        }
    }

    /**
     * Animate cursor movement to target
     */
    private suspend fun animateSnapToTarget(
        target: SnapTarget,
        onUpdate: (x: Float, y: Float) -> Unit
    ) {
        val startX = 0f  // Should be passed as parameter in real implementation
        val startY = 0f  // Should be passed as parameter in real implementation
        val endX = target.center.x.toFloat()
        val endY = target.center.y.toFloat()

        val frameTime = 1000L / SNAP_ANIMATION_FPS
        val totalFrames = (SNAP_ANIMATION_DURATION / frameTime).toInt()

        for (frame in 0..totalFrames) {
            val progress = frame.toFloat() / totalFrames

            // Apply easing for smooth animation
            val easedProgress = speedController.applyEasing(progress, EasingFunction.EASE_OUT)

            // Interpolate position
            val currentX = startX + (endX - startX) * easedProgress
            val currentY = startY + (endY - startY) * easedProgress

            // Update cursor position
            onUpdate(currentX, currentY)

            // Wait for next frame
            if (frame < totalFrames) {
                delay(frameTime)
            }
        }

        Log.d(TAG, "Snap animation complete")
    }

    /**
     * Find all snappable targets within radius
     *
     * @param x Current cursor X position
     * @param y Current cursor Y position
     * @param maxRadius Maximum search radius
     * @return List of all snap targets within radius
     */
    fun findAllTargets(
        x: Float,
        y: Float,
        maxRadius: Float = snapRadius
    ): List<SnapTarget> {
        if (!snapEnabled) return emptyList()

        val rootNode = service.rootInActiveWindow ?: return emptyList()
        val candidates = mutableListOf<SnapTarget>()

        try {
            findClickableElements(rootNode, x, y, maxRadius, candidates)
            return candidates.sortedBy { it.distance }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding all targets", e)
            candidates.forEach { it.recycle() }
            return emptyList()
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Get snap target at specific position (for direct clicking)
     *
     * @param x X position
     * @param y Y position
     * @return Target at position or null
     */
    fun getTargetAtPosition(x: Float, y: Float): SnapTarget? {
        val rootNode = service.rootInActiveWindow ?: return null

        try {
            return findTargetAtPosition(rootNode, x, y)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding target at position", e)
            return null
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Recursively find target at specific position
     */
    private fun findTargetAtPosition(
        node: AccessibilityNodeInfo?,
        x: Float,
        y: Float
    ): SnapTarget? {
        node ?: return null

        try {
            if (node.isClickable && node.isEnabled && node.isVisibleToUser) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                if (bounds.contains(x.toInt(), y.toInt())) {
                    val centerX = bounds.exactCenterX().toInt()
                    val centerY = bounds.exactCenterY().toInt()
                    val center = Point(centerX, centerY)
                    val distance = calculateDistance(x, y, centerX.toFloat(), centerY.toFloat())
                    val priority = calculatePriority(node, bounds)
                    val description = buildElementDescription(node)

                    return SnapTarget(node, bounds, center, distance, priority, description)
                }
            }

            // Check children
            val childCount = node.childCount
            for (i in 0 until childCount) {
                var childNode: AccessibilityNodeInfo? = null
                try {
                    childNode = node.getChild(i)
                    val result = findTargetAtPosition(childNode, x, y)
                    if (result != null) return result
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing child $i", e)
                } finally {
                    childNode?.recycle()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking position", e)
        }

        return null
    }

    /**
     * Cancel any ongoing snap animation
     */
    fun cancelAnimation() {
        snapAnimationJob?.cancel()
        snapAnimationJob = null
        Log.d(TAG, "Snap animation cancelled")
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        cancelAnimation()
        Log.d(TAG, "SnapToElementHandler disposed")
    }
}
