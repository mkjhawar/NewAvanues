/**
 * AndroidGestureExecutor.kt - Android implementation of GestureExecutor
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Bridges the GesturePlugin to Android AccessibilityService for
 * gesture-based actions like swipe, pinch, tap, etc.
 */
package com.augmentalis.magiccode.plugins.android.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import com.augmentalis.magiccode.plugins.android.ServiceRegistry
import com.augmentalis.magiccode.plugins.builtin.GestureExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of GestureExecutor.
 *
 * Uses AccessibilityService gesture API for complex touch gestures.
 *
 * @param serviceRegistry Registry to retrieve AccessibilityService from
 */
class AndroidGestureExecutor(
    private val serviceRegistry: ServiceRegistry
) : GestureExecutor {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

    override suspend fun tap(x: Int, y: Int): Boolean {
        val service = accessibilityService ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS))
            .build()

        return dispatchGesture(service, gesture)
    }

    override suspend fun doubleTap(x: Int, y: Int): Boolean {
        // First tap
        if (!tap(x, y)) return false

        // Brief delay
        delay(DOUBLE_TAP_INTERVAL_MS)

        // Second tap
        return tap(x, y)
    }

    override suspend fun longPress(x: Int, y: Int, duration: Long): Boolean {
        val service = accessibilityService ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(service, gesture)
    }

    override suspend fun swipeUp(distance: Int): Boolean {
        val service = accessibilityService ?: return false
        val displayMetrics = service.resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2
        val centerY = displayMetrics.heightPixels / 2

        return swipe(centerX, centerY + distance / 2, centerX, centerY - distance / 2)
    }

    override suspend fun swipeDown(distance: Int): Boolean {
        val service = accessibilityService ?: return false
        val displayMetrics = service.resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2
        val centerY = displayMetrics.heightPixels / 2

        return swipe(centerX, centerY - distance / 2, centerX, centerY + distance / 2)
    }

    override suspend fun swipeLeft(distance: Int): Boolean {
        val service = accessibilityService ?: return false
        val displayMetrics = service.resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2
        val centerY = displayMetrics.heightPixels / 2

        return swipe(centerX + distance / 2, centerY, centerX - distance / 2, centerY)
    }

    override suspend fun swipeRight(distance: Int): Boolean {
        val service = accessibilityService ?: return false
        val displayMetrics = service.resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2
        val centerY = displayMetrics.heightPixels / 2

        return swipe(centerX - distance / 2, centerY, centerX + distance / 2, centerY)
    }

    override suspend fun pinchIn(centerX: Int, centerY: Int, scale: Float): Boolean {
        // Scale < 1.0 means pinch in (fingers moving together)
        val startDistance = 200
        val endDistance = (startDistance * scale).toInt().coerceAtLeast(50)
        return pinch(centerX, centerY, startDistance, endDistance)
    }

    override suspend fun pinchOut(centerX: Int, centerY: Int, scale: Float): Boolean {
        // Scale > 1.0 means pinch out (fingers moving apart)
        val startDistance = 100
        val endDistance = (startDistance * scale).toInt().coerceAtMost(400)
        return pinch(centerX, centerY, startDistance, endDistance)
    }

    override suspend fun rotateLeft(centerX: Int, centerY: Int, angle: Float): Boolean {
        // Rotate gestures are complex; simulating with two-finger arc
        // For now, return false as full rotation support requires more complex gesture paths
        return false
    }

    override suspend fun rotateRight(centerX: Int, centerY: Int, angle: Float): Boolean {
        // Rotate gestures are complex; simulating with two-finger arc
        // For now, return false as full rotation support requires more complex gesture paths
        return false
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private suspend fun swipe(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        durationMs: Long = SWIPE_DURATION_MS
    ): Boolean {
        val service = accessibilityService ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path()
        path.moveTo(startX.toFloat(), startY.toFloat())
        path.lineTo(endX.toFloat(), endY.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        return dispatchGesture(service, gesture)
    }

    private suspend fun pinch(
        centerX: Int, centerY: Int,
        startDistance: Int, endDistance: Int,
        durationMs: Long = PINCH_DURATION_MS
    ): Boolean {
        val service = accessibilityService ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        // Calculate start and end points for two fingers
        val halfStartDist = startDistance / 2
        val halfEndDist = endDistance / 2

        val path1 = Path()
        path1.moveTo((centerX - halfStartDist).toFloat(), centerY.toFloat())
        path1.lineTo((centerX - halfEndDist).toFloat(), centerY.toFloat())

        val path2 = Path()
        path2.moveTo((centerX + halfStartDist).toFloat(), centerY.toFloat())
        path2.lineTo((centerX + halfEndDist).toFloat(), centerY.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, durationMs))
            .build()

        return dispatchGesture(service, gesture)
    }

    private suspend fun dispatchGesture(
        service: AccessibilityService,
        gesture: GestureDescription
    ): Boolean {
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
        private const val TAP_DURATION_MS = 50L
        private const val DOUBLE_TAP_INTERVAL_MS = 100L
        private const val SWIPE_DURATION_MS = 300L
        private const val PINCH_DURATION_MS = 300L
    }
}
