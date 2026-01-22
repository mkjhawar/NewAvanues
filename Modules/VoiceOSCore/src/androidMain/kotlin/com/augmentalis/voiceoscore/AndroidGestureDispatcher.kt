/**
 * AndroidGestureDispatcher.kt - Executes gestures via AccessibilityService
 *
 * Implements gesture execution using Android's AccessibilityService
 * dispatchGesture API for click, scroll, and other actions.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 */
package com.augmentalis.voiceoscore

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Dispatcher for executing gestures via AccessibilityService.
 *
 * Supports:
 * - Click (tap) at coordinates or on element
 * - Long press at coordinates
 * - Scroll in any direction
 * - Swipe gestures
 *
 * @param service The AccessibilityService instance for dispatching gestures
 */
class AndroidGestureDispatcher(
    private val service: AccessibilityService
) {
    companion object {
        private const val TAG = "AndroidGestureDispatcher"
        private const val TAP_DURATION_MS = 50L
        private const val LONG_PRESS_DURATION_MS = 500L
        private const val SCROLL_DURATION_MS = 300L
        private const val SWIPE_DISTANCE = 500
    }

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Click/tap at the center of the given bounds.
     *
     * @param bounds The bounds to click within
     * @return true if gesture was dispatched successfully
     */
    suspend fun click(bounds: Bounds): Boolean {
        val x = bounds.centerX.toFloat()
        val y = bounds.centerY.toFloat()
        return tap(x, y)
    }

    /**
     * Click on an AccessibilityNodeInfo using its action if available,
     * falls back to gesture if not clickable.
     *
     * @param node The node to click
     * @return true if click was successful
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        // Try native click action first
        if (node.isClickable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (result) {
                Log.d(TAG, "Clicked via ACTION_CLICK")
                return true
            }
        }

        // Fall back to gesture tap
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        val bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom)

        // Use runBlocking for sync context - in real usage call suspend version
        return try {
            kotlinx.coroutines.runBlocking { click(bounds) }
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking node: ${e.message}")
            false
        }
    }

    /**
     * Tap at specific coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if gesture was dispatched successfully
     */
    suspend fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS))
            .build()

        return dispatchGesture(gesture)
    }

    /**
     * Long press at specific coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if gesture was dispatched successfully
     */
    suspend fun longPress(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, LONG_PRESS_DURATION_MS))
            .build()

        return dispatchGesture(gesture)
    }

    /**
     * Scroll in the specified direction.
     *
     * @param direction Scroll direction ("up", "down", "left", "right")
     * @param startBounds Optional bounds to start scroll from (defaults to screen center)
     * @return true if gesture was dispatched successfully
     */
    suspend fun scroll(direction: String, startBounds: Bounds? = null): Boolean {
        val metrics = service.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val startX = startBounds?.centerX?.toFloat() ?: (screenWidth / 2f)
        val startY = startBounds?.centerY?.toFloat() ?: (screenHeight / 2f)

        val (endX, endY) = when (direction.lowercase()) {
            "up" -> startX to (startY - SWIPE_DISTANCE)
            "down" -> startX to (startY + SWIPE_DISTANCE)
            "left" -> (startX - SWIPE_DISTANCE) to startY
            "right" -> (startX + SWIPE_DISTANCE) to startY
            else -> {
                Log.w(TAG, "Unknown scroll direction: $direction")
                return false
            }
        }

        return swipe(startX, startY, endX.toFloat(), endY.toFloat())
    }

    /**
     * Scroll using AccessibilityNodeInfo action if available.
     *
     * @param node Node to scroll
     * @param forward true for forward/down, false for backward/up
     * @return true if scroll was successful
     */
    fun scrollNode(node: AccessibilityNodeInfo, forward: Boolean): Boolean {
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }

        return node.performAction(action).also { result ->
            Log.d(TAG, "Scroll ${if (forward) "forward" else "backward"}: $result")
        }
    }

    /**
     * Swipe from one point to another.
     *
     * @param startX Start X coordinate
     * @param startY Start Y coordinate
     * @param endX End X coordinate
     * @param endY End Y coordinate
     * @return true if gesture was dispatched successfully
     */
    suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, SCROLL_DURATION_MS))
            .build()

        return dispatchGesture(gesture)
    }

    /**
     * Focus on a node.
     *
     * @param node Node to focus
     * @return true if focus was successful
     */
    fun focus(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS).also { result ->
            Log.d(TAG, "Focus: $result")
        }
    }

    /**
     * Dispatch a gesture and wait for completion.
     *
     * @param gesture The gesture to dispatch
     * @return true if gesture completed successfully
     */
    private suspend fun dispatchGesture(gesture: GestureDescription): Boolean {
        return suspendCoroutine { continuation ->
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Gesture completed")
                    continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Gesture cancelled")
                    continuation.resume(false)
                }
            }

            val dispatched = service.dispatchGesture(gesture, callback, handler)
            if (!dispatched) {
                Log.e(TAG, "Failed to dispatch gesture")
                continuation.resume(false)
            }
        }
    }

    /**
     * Perform a global action (back, home, recents, etc.)
     *
     * @param action Global action constant from AccessibilityService
     * @return true if action was performed
     */
    fun performGlobalAction(action: Int): Boolean {
        return service.performGlobalAction(action).also { result ->
            Log.d(TAG, "Global action $action: $result")
        }
    }
}
