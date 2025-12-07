/**
 * CursorGestureHandler.kt - Dispatch gestures via cursor position
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import android.view.ViewConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Gesture type enum
 */
enum class GestureType {
    CLICK,          // Single tap
    DOUBLE_CLICK,   // Double tap
    LONG_PRESS,     // Long press/hold
    SWIPE_UP,       // Swipe up
    SWIPE_DOWN,     // Swipe down
    SWIPE_LEFT,     // Swipe left
    SWIPE_RIGHT,    // Swipe right
    DRAG,           // Drag gesture
    SCROLL_UP,      // Scroll up
    SCROLL_DOWN     // Scroll down
}

/**
 * Gesture result
 */
data class GestureResult(
    val success: Boolean,
    val gestureType: GestureType,
    val message: String? = null,
    val executionTime: Long = 0
)

/**
 * Gesture configuration
 *
 * @param tapDuration Duration of tap gesture
 * @param longPressDuration Duration of long press
 * @param doubleTapDelay Max delay between double taps
 * @param swipeDistance Default swipe distance in pixels
 * @param swipeDuration Duration of swipe gesture
 */
data class GestureConfig(
    val tapDuration: Long = ViewConfiguration.getTapTimeout().toLong(),
    val longPressDuration: Long = ViewConfiguration.getLongPressTimeout().toLong(),
    val doubleTapDelay: Long = ViewConfiguration.getDoubleTapTimeout().toLong(),
    val swipeDistance: Float = DEFAULT_SWIPE_DISTANCE,
    val swipeDuration: Long = DEFAULT_SWIPE_DURATION,
    val scrollDistance: Float = DEFAULT_SCROLL_DISTANCE
) {
    companion object {
        const val DEFAULT_SWIPE_DISTANCE = 400f // pixels
        const val DEFAULT_SWIPE_DURATION = 300L // ms
        const val DEFAULT_SCROLL_DISTANCE = 200f // pixels
    }
}

/**
 * Cursor gesture handler
 *
 * Handles gesture dispatch via cursor position with:
 * - Click gesture (tap at cursor)
 * - Long-press gesture (hold at cursor)
 * - Swipe gestures (directional from cursor)
 * - Scroll gestures (vertical/horizontal)
 * - Uses AccessibilityService.dispatchGesture() API
 */
class CursorGestureHandler(
    private val service: AccessibilityService,
    private val positionTracker: CursorPositionTracker,
    private val config: GestureConfig = GestureConfig()
) {
    companion object {
        private const val TAG = "CursorGestureHandler"
    }

    // Coroutine scope for async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Gesture callbacks
    private val gestureCallbacks = mutableListOf<(GestureResult) -> Unit>()

    init {
        Log.d(TAG, "CursorGestureHandler initialized with config: $config")
    }

    /**
     * Perform click at current cursor position
     *
     * @return GestureResult indicating success/failure
     */
    suspend fun performClick(): GestureResult {
        val position = positionTracker.getCurrentPosition()
        return performClickAt(position.x, position.y)
    }

    /**
     * Perform click at specific position
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return GestureResult indicating success/failure
     */
    suspend fun performClickAt(x: Float, y: Float): GestureResult {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Performing click at ($x, $y)")

        return try {
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x, y)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, config.tapDuration))
                .build()

            val success = dispatchGestureAsync(gesture)
            val executionTime = System.currentTimeMillis() - startTime

            val result = GestureResult(
                success = success,
                gestureType = GestureType.CLICK,
                message = if (success) "Click successful" else "Click failed",
                executionTime = executionTime
            )

            notifyCallbacks(result)
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error performing click", e)
            val executionTime = System.currentTimeMillis() - startTime
            GestureResult(
                success = false,
                gestureType = GestureType.CLICK,
                message = "Error: ${e.message}",
                executionTime = executionTime
            )
        }
    }

    /**
     * Perform double click at current cursor position
     *
     * @return GestureResult indicating success/failure
     */
    suspend fun performDoubleClick(): GestureResult {
        val position = positionTracker.getCurrentPosition()
        return performDoubleClickAt(position.x, position.y)
    }

    /**
     * Perform double click at specific position
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return GestureResult indicating success/failure
     */
    suspend fun performDoubleClickAt(x: Float, y: Float): GestureResult {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Performing double click at ($x, $y)")

        return try {
            // First tap
            var success = performClickAt(x, y).success

            if (success) {
                // Wait for double tap timing
                delay(config.doubleTapDelay / 2)

                // Second tap
                success = performClickAt(x, y).success
            }

            val executionTime = System.currentTimeMillis() - startTime

            val result = GestureResult(
                success = success,
                gestureType = GestureType.DOUBLE_CLICK,
                message = if (success) "Double click successful" else "Double click failed",
                executionTime = executionTime
            )

            notifyCallbacks(result)
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error performing double click", e)
            val executionTime = System.currentTimeMillis() - startTime
            GestureResult(
                success = false,
                gestureType = GestureType.DOUBLE_CLICK,
                message = "Error: ${e.message}",
                executionTime = executionTime
            )
        }
    }

    /**
     * Perform long press at current cursor position
     *
     * @param duration Duration of long press (default: from config)
     * @return GestureResult indicating success/failure
     */
    suspend fun performLongPress(duration: Long = config.longPressDuration): GestureResult {
        val position = positionTracker.getCurrentPosition()
        return performLongPressAt(position.x, position.y, duration)
    }

    /**
     * Perform long press at specific position
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param duration Duration of long press
     * @return GestureResult indicating success/failure
     */
    suspend fun performLongPressAt(x: Float, y: Float, duration: Long): GestureResult {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Performing long press at ($x, $y) for ${duration}ms")

        return try {
            val path = Path().apply {
                moveTo(x, y)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()

            val success = dispatchGestureAsync(gesture)
            val executionTime = System.currentTimeMillis() - startTime

            val result = GestureResult(
                success = success,
                gestureType = GestureType.LONG_PRESS,
                message = if (success) "Long press successful" else "Long press failed",
                executionTime = executionTime
            )

            notifyCallbacks(result)
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error performing long press", e)
            val executionTime = System.currentTimeMillis() - startTime
            GestureResult(
                success = false,
                gestureType = GestureType.LONG_PRESS,
                message = "Error: ${e.message}",
                executionTime = executionTime
            )
        }
    }

    /**
     * Perform swipe from current cursor position
     *
     * @param direction Swipe direction
     * @param distance Swipe distance in pixels (default: from config)
     * @return GestureResult indicating success/failure
     */
    suspend fun performSwipe(
        direction: Direction,
        distance: Float = config.swipeDistance
    ): GestureResult {
        val position = positionTracker.getCurrentPosition()
        val start = PointF(position.x, position.y)
        val end = calculateSwipeEndPoint(start, direction, distance)

        return performSwipeFromTo(start, end, direction)
    }

    /**
     * Perform swipe from start to end point
     *
     * @param start Starting point
     * @param end Ending point
     * @param direction Swipe direction for result
     * @return GestureResult indicating success/failure
     */
    private suspend fun performSwipeFromTo(
        start: PointF,
        end: PointF,
        direction: Direction
    ): GestureResult {
        val startTime = System.currentTimeMillis()
        val gestureType = directionToGestureType(direction)

        Log.d(TAG, "Performing swipe from ($start) to ($end)")

        return try {
            val path = Path().apply {
                moveTo(start.x, start.y)
                lineTo(end.x, end.y)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, config.swipeDuration))
                .build()

            val success = dispatchGestureAsync(gesture)
            val executionTime = System.currentTimeMillis() - startTime

            val result = GestureResult(
                success = success,
                gestureType = gestureType,
                message = if (success) "Swipe successful" else "Swipe failed",
                executionTime = executionTime
            )

            notifyCallbacks(result)
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error performing swipe", e)
            val executionTime = System.currentTimeMillis() - startTime
            GestureResult(
                success = false,
                gestureType = gestureType,
                message = "Error: ${e.message}",
                executionTime = executionTime
            )
        }
    }

    /**
     * Perform drag gesture
     *
     * @param startX Start X coordinate
     * @param startY Start Y coordinate
     * @param endX End X coordinate
     * @param endY End Y coordinate
     * @param duration Drag duration
     * @return GestureResult indicating success/failure
     */
    suspend fun performDrag(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = config.swipeDuration
    ): GestureResult {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Performing drag from ($startX, $startY) to ($endX, $endY)")

        return try {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()

            val success = dispatchGestureAsync(gesture)
            val executionTime = System.currentTimeMillis() - startTime

            val result = GestureResult(
                success = success,
                gestureType = GestureType.DRAG,
                message = if (success) "Drag successful" else "Drag failed",
                executionTime = executionTime
            )

            notifyCallbacks(result)
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error performing drag", e)
            val executionTime = System.currentTimeMillis() - startTime
            GestureResult(
                success = false,
                gestureType = GestureType.DRAG,
                message = "Error: ${e.message}",
                executionTime = executionTime
            )
        }
    }

    /**
     * Perform scroll gesture from current cursor position
     *
     * @param direction Scroll direction (UP or DOWN)
     * @param distance Scroll distance in pixels
     * @return GestureResult indicating success/failure
     */
    suspend fun performScroll(
        direction: Direction,
        distance: Float = config.scrollDistance
    ): GestureResult {
        val position = positionTracker.getCurrentPosition()
        val start = PointF(position.x, position.y)
        val end = calculateSwipeEndPoint(start, direction, distance)

        val gestureType = when (direction) {
            Direction.UP -> GestureType.SCROLL_UP
            Direction.DOWN -> GestureType.SCROLL_DOWN
            else -> GestureType.SCROLL_DOWN
        }

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Performing scroll ${direction.name} from ($start)")

        return try {
            val path = Path().apply {
                moveTo(start.x, start.y)
                lineTo(end.x, end.y)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, config.swipeDuration))
                .build()

            val success = dispatchGestureAsync(gesture)
            val executionTime = System.currentTimeMillis() - startTime

            val result = GestureResult(
                success = success,
                gestureType = gestureType,
                message = if (success) "Scroll successful" else "Scroll failed",
                executionTime = executionTime
            )

            notifyCallbacks(result)
            result

        } catch (e: Exception) {
            Log.e(TAG, "Error performing scroll", e)
            val executionTime = System.currentTimeMillis() - startTime
            GestureResult(
                success = false,
                gestureType = gestureType,
                message = "Error: ${e.message}",
                executionTime = executionTime
            )
        }
    }

    /**
     * Calculate swipe end point from start, direction, and distance
     */
    private fun calculateSwipeEndPoint(
        start: PointF,
        direction: Direction,
        distance: Float
    ): PointF {
        return when (direction) {
            Direction.UP -> PointF(start.x, start.y - distance)
            Direction.DOWN -> PointF(start.x, start.y + distance)
            Direction.LEFT -> PointF(start.x - distance, start.y)
            Direction.RIGHT -> PointF(start.x + distance, start.y)
            Direction.UP_LEFT -> PointF(start.x - distance * 0.707f, start.y - distance * 0.707f)
            Direction.UP_RIGHT -> PointF(start.x + distance * 0.707f, start.y - distance * 0.707f)
            Direction.DOWN_LEFT -> PointF(start.x - distance * 0.707f, start.y + distance * 0.707f)
            Direction.DOWN_RIGHT -> PointF(start.x + distance * 0.707f, start.y + distance * 0.707f)
        }
    }

    /**
     * Convert direction to gesture type
     */
    private fun directionToGestureType(direction: Direction): GestureType {
        return when (direction) {
            Direction.UP, Direction.UP_LEFT, Direction.UP_RIGHT -> GestureType.SWIPE_UP
            Direction.DOWN, Direction.DOWN_LEFT, Direction.DOWN_RIGHT -> GestureType.SWIPE_DOWN
            Direction.LEFT -> GestureType.SWIPE_LEFT
            Direction.RIGHT -> GestureType.SWIPE_RIGHT
        }
    }

    /**
     * Dispatch gesture asynchronously using suspendCancellableCoroutine
     *
     * @param gesture Gesture to dispatch
     * @return true if gesture was dispatched successfully
     */
    private suspend fun dispatchGestureAsync(gesture: GestureDescription): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.v(TAG, "Gesture completed successfully")
                    continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "Gesture was cancelled")
                    continuation.resume(false)
                }
            }

            val dispatched = service.dispatchGesture(gesture, callback, null)

            if (!dispatched) {
                Log.e(TAG, "Failed to dispatch gesture")
                continuation.resume(false)
            }

            // Handle cancellation
            continuation.invokeOnCancellation {
                Log.w(TAG, "Gesture dispatch cancelled by coroutine")
            }
        }
    }

    /**
     * Add gesture result callback
     *
     * @param callback Function to call when gesture completes
     */
    fun addGestureCallback(callback: (GestureResult) -> Unit) {
        gestureCallbacks.add(callback)
        Log.d(TAG, "Gesture callback registered (total: ${gestureCallbacks.size})")
    }

    /**
     * Remove gesture result callback
     */
    fun removeGestureCallback(callback: (GestureResult) -> Unit) {
        gestureCallbacks.remove(callback)
        Log.d(TAG, "Gesture callback unregistered (total: ${gestureCallbacks.size})")
    }

    /**
     * Clear all callbacks
     */
    fun clearCallbacks() {
        gestureCallbacks.clear()
        Log.d(TAG, "All gesture callbacks cleared")
    }

    /**
     * Notify callbacks of gesture result
     */
    private fun notifyCallbacks(result: GestureResult) {
        gestureCallbacks.forEach { callback ->
            try {
                callback(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error in gesture callback", e)
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        clearCallbacks()
        Log.d(TAG, "CursorGestureHandler disposed")
    }
}
