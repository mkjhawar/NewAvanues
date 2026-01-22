/**
 * AndroidGestureExecutor.kt - Android implementation of GestureExecutor
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Bridges the GesturePlugin to Android AccessibilityService for
 * gesture-based actions like swipe, pinch, drag, etc.
 */
package com.augmentalis.magiccode.plugins.android.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import com.augmentalis.magiccode.plugins.android.ServiceRegistry
import com.augmentalis.magiccode.plugins.builtin.GestureExecutor
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

    override suspend fun swipe(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        durationMs: Long
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

    override suspend fun longPress(x: Int, y: Int, durationMs: Long): Boolean {
        val service = accessibilityService ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        return dispatchGesture(service, gesture)
    }

    override suspend fun doubleTap(x: Int, y: Int): Boolean {
        // First tap
        if (!tap(x, y)) return false

        // Brief delay
        kotlinx.coroutines.delay(DOUBLE_TAP_INTERVAL_MS)

        // Second tap
        return tap(x, y)
    }

    override suspend fun pinch(
        centerX: Int, centerY: Int,
        startDistance: Int, endDistance: Int,
        durationMs: Long
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

    override suspend fun drag(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        durationMs: Long
    ): Boolean {
        // Drag is similar to swipe but typically longer duration
        return swipe(startX, startY, endX, endY, durationMs)
    }

    override suspend fun fling(direction: String): Boolean {
        val service = accessibilityService ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        val flingDistance = screenHeight / 3

        val (startX, startY, endX, endY) = when (direction.lowercase()) {
            "up" -> listOf(centerX, centerY + flingDistance/2, centerX, centerY - flingDistance/2)
            "down" -> listOf(centerX, centerY - flingDistance/2, centerX, centerY + flingDistance/2)
            "left" -> listOf(centerX + flingDistance/2, centerY, centerX - flingDistance/2, centerY)
            "right" -> listOf(centerX - flingDistance/2, centerY, centerX + flingDistance/2, centerY)
            else -> return false
        }

        return swipe(startX, startY, endX, endY, FLING_DURATION_MS)
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
        private const val FLING_DURATION_MS = 150L
    }
}
