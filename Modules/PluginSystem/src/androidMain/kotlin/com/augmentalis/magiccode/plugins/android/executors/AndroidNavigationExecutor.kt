/**
 * AndroidNavigationExecutor.kt - Android implementation of NavigationPluginExecutor
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Bridges the NavigationHandlerPlugin to Android AccessibilityService for
 * scroll/swipe/navigation gestures.
 */
package com.augmentalis.magiccode.plugins.android.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.magiccode.plugins.android.ServiceRegistry
import com.augmentalis.magiccode.plugins.builtin.NavigationPluginExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of NavigationPluginExecutor.
 *
 * Uses AccessibilityService gesture API for scroll/swipe operations.
 * Falls back to AccessibilityNodeInfo.ACTION_SCROLL_* for direct scroll actions.
 *
 * @param serviceRegistry Registry to retrieve AccessibilityService from
 */
// recycle() deprecated API 34+ (no-op on 34+, still needed for minSdk 29)
@Suppress("DEPRECATION")
class AndroidNavigationExecutor(
    private val serviceRegistry: ServiceRegistry
) : NavigationPluginExecutor {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

    override suspend fun scrollUp(): Boolean {
        val service = accessibilityService ?: return false

        // Try to find scrollable node and use ACTION_SCROLL_BACKWARD
        val rootNode = service.rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(rootNode)
        rootNode.recycle()

        return if (scrollable != null) {
            val result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            scrollable.recycle()
            result
        } else {
            // Fallback to gesture-based scroll
            performSwipeGesture(service, SwipeDirection.DOWN) // Swipe down to scroll up
        }
    }

    override suspend fun scrollDown(): Boolean {
        val service = accessibilityService ?: return false

        val rootNode = service.rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(rootNode)
        rootNode.recycle()

        return if (scrollable != null) {
            val result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            scrollable.recycle()
            result
        } else {
            performSwipeGesture(service, SwipeDirection.UP)
        }
    }

    override suspend fun scrollLeft(): Boolean {
        val service = accessibilityService ?: return false

        val rootNode = service.rootInActiveWindow ?: return false
        val scrollable = findHorizontalScrollableNode(rootNode)
        rootNode.recycle()

        return if (scrollable != null) {
            val result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            scrollable.recycle()
            result
        } else {
            performSwipeGesture(service, SwipeDirection.RIGHT) // Swipe right to scroll left
        }
    }

    override suspend fun scrollRight(): Boolean {
        val service = accessibilityService ?: return false

        val rootNode = service.rootInActiveWindow ?: return false
        val scrollable = findHorizontalScrollableNode(rootNode)
        rootNode.recycle()

        return if (scrollable != null) {
            val result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            scrollable.recycle()
            result
        } else {
            performSwipeGesture(service, SwipeDirection.LEFT)
        }
    }

    override suspend fun next(): Boolean {
        val service = accessibilityService ?: return false

        // Try AccessibilityNodeInfo.ACTION_SCROLL_FORWARD first
        val rootNode = service.rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(rootNode)
        rootNode.recycle()

        return if (scrollable != null) {
            val result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            scrollable.recycle()
            result
        } else {
            // Fallback to page-style swipe
            performSwipeGesture(service, SwipeDirection.UP)
        }
    }

    override suspend fun previous(): Boolean {
        val service = accessibilityService ?: return false

        val rootNode = service.rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(rootNode)
        rootNode.recycle()

        return if (scrollable != null) {
            val result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            scrollable.recycle()
            result
        } else {
            performSwipeGesture(service, SwipeDirection.DOWN)
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private enum class SwipeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) {
            return AccessibilityNodeInfo.obtain(root)
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val scrollable = findScrollableNode(child)
            child.recycle()
            if (scrollable != null) {
                return scrollable
            }
        }
        return null
    }

    private fun findHorizontalScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = root.className?.toString() ?: ""

        // Check for horizontal scrollable containers
        val isHorizontal = className.contains("HorizontalScrollView") ||
                className.contains("ViewPager") ||
                className.contains("RecyclerView") // Could be horizontal

        if (root.isScrollable && isHorizontal) {
            return AccessibilityNodeInfo.obtain(root)
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val scrollable = findHorizontalScrollableNode(child)
            child.recycle()
            if (scrollable != null) {
                return scrollable
            }
        }
        return null
    }

    private suspend fun performSwipeGesture(
        service: AccessibilityService,
        direction: SwipeDirection
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f
        val swipeDistance = screenHeight / 3f

        val path = Path()
        when (direction) {
            SwipeDirection.UP -> {
                path.moveTo(centerX, centerY + swipeDistance / 2)
                path.lineTo(centerX, centerY - swipeDistance / 2)
            }
            SwipeDirection.DOWN -> {
                path.moveTo(centerX, centerY - swipeDistance / 2)
                path.lineTo(centerX, centerY + swipeDistance / 2)
            }
            SwipeDirection.LEFT -> {
                val horizontalDistance = screenWidth / 3f
                path.moveTo(centerX + horizontalDistance / 2, centerY)
                path.lineTo(centerX - horizontalDistance / 2, centerY)
            }
            SwipeDirection.RIGHT -> {
                val horizontalDistance = screenWidth / 3f
                path.moveTo(centerX - horizontalDistance / 2, centerY)
                path.lineTo(centerX + horizontalDistance / 2, centerY)
            }
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_DURATION_MS))
            .build()

        return suspendCancellableCoroutine { continuation ->
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    continuation.resume(false)
                }
            }

            if (!service.dispatchGesture(gesture, callback, null)) {
                continuation.resume(false)
            }
        }
    }

    companion object {
        private const val SWIPE_DURATION_MS = 300L
    }
}
