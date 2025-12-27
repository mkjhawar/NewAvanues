/**
 * GestureHandler.kt - Handles complex gesture interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.res.Resources
import android.graphics.Path
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewConfiguration
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import com.augmentalis.voiceoscore.accessibility.ui.utils.DisplayUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handler for complex gesture interactions
 */
class GestureHandler(
    private val service: VoiceOSService,
    private val pathFactory: GesturePathFactory = RealGesturePathFactory(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : ActionHandler {

    // Gesture management
    private val lock = AtomicBoolean(false)
    private val gestureQueue = LinkedList<GestureDescription>()
    private val mainScope = coroutineScope
    
    // Timing
    private val tapDuration = ViewConfiguration.getTapTimeout().toLong()
    private val doubleTapDelay = ViewConfiguration.getDoubleTapTimeout().toLong()
    private val longPressDuration = ViewConfiguration.getLongPressTimeout().toLong()
    
    companion object {
        private const val TAG = "GestureHandler"
        
        // Gesture constants
        const val PINCH_DURATION_MS: Int = 400
        const val PINCH_DISTANCE_CLOSE = 200
        const val PINCH_DISTANCE_FAR = 800
        
        val SUPPORTED_ACTIONS = listOf(
            "pinch open", "zoom in", "pinch in",
            "pinch close", "zoom out", "pinch out",
            "drag", "drag to", "drag from",
            "gesture", "path gesture",
            "swipe", "swipe up", "swipe down", "swipe left", "swipe right"
        )
    }
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()
        
        Log.d(TAG, "Executing gesture action: $normalizedAction")
        
        return when {
            // Pinch/Zoom gestures
            normalizedAction == "pinch open" || 
            normalizedAction == "zoom in" || 
            normalizedAction == "pinch in" -> {
                val x = params["x"] as? Int ?: getScreenCenter().first
                val y = params["y"] as? Int ?: getScreenCenter().second
                performPinchOpen(x, y)
            }
            
            normalizedAction == "pinch close" || 
            normalizedAction == "zoom out" || 
            normalizedAction == "pinch out" -> {
                val x = params["x"] as? Int ?: getScreenCenter().first
                val y = params["y"] as? Int ?: getScreenCenter().second
                performPinchClose(x, y)
            }
            
            // Drag gestures
            normalizedAction.startsWith("drag") -> {
                val startX = params["startX"] as? Int
                val startY = params["startY"] as? Int
                val endX = params["endX"] as? Int
                val endY = params["endY"] as? Int
                val duration = params["duration"] as? Long ?: 500L
                
                if (startX != null && startY != null && endX != null && endY != null) {
                    performDrag(startX, startY, endX, endY, duration)
                } else {
                    Log.w(TAG, "Drag gesture requires startX, startY, endX, endY parameters")
                    false
                }
            }
            
            // Swipe gestures
            normalizedAction.startsWith("swipe") -> {
                val direction = normalizedAction.removePrefix("swipe").trim()
                val centerX = params["x"] as? Int ?: getScreenCenter().first
                val centerY = params["y"] as? Int ?: getScreenCenter().second
                val distance = params["distance"] as? Int ?: 400
                performSwipe(direction, centerX, centerY, distance)
            }
            
            // Custom path gestures
            normalizedAction == "gesture" || normalizedAction == "path gesture" -> {
                @Suppress("UNCHECKED_CAST")
                val pathPoints = params["path"] as? List<Point>
                val duration = params["duration"] as? Long ?: 500L
                
                if (pathPoints != null && pathPoints.isNotEmpty()) {
                    performPathGesture(pathPoints, duration)
                } else {
                    Log.w(TAG, "Path gesture requires 'path' parameter with List<Point>")
                    false
                }
            }
            
            else -> {
                Log.w(TAG, "Unknown gesture action: $normalizedAction")
                false
            }
        }
    }
    
    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.startsWith(it) }
    }
    
    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }
    
    /**
     * Perform pinch open (zoom in) gesture
     */
    private fun performPinchOpen(x: Int, y: Int): Boolean {
        return try {
            synchronized(lock) {
                pinchGesture(x, y, PINCH_DISTANCE_CLOSE, PINCH_DISTANCE_FAR)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing pinch open gesture", e)
            false
        }
    }
    
    /**
     * Perform pinch close (zoom out) gesture
     */
    private fun performPinchClose(x: Int, y: Int): Boolean {
        return try {
            synchronized(lock) {
                pinchGesture(x, y, PINCH_DISTANCE_FAR, PINCH_DISTANCE_CLOSE)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing pinch close gesture", e)
            false
        }
    }
    
    /**
     * Handles pinch-to-zoom gestures, allowing zoom in and zoom out actions.
     * Migrated from Legacy with 100% functional equivalence.
     *
     * @param x The x-coordinate of the pinch gesture center.
     * @param y The y-coordinate of the pinch gesture center.
     * @param startSpacing The initial spacing between two fingers.
     * @param endSpacing The final spacing between two fingers.
     */
    private fun pinchGesture(x: Int, y: Int, startSpacing: Int, endSpacing: Int) {
        // Calculate first finger positions
        var x1 = x - startSpacing / 2
        var y1 = y - startSpacing / 2
        var x2 = x - endSpacing / 2
        var y2 = y - endSpacing / 2
        
        // Ensure coordinates are not negative
        if (x1 < 0) x1 = 0
        if (y1 < 0) y1 = 0
        if (x2 < 0) x2 = 0
        if (y2 < 0) y2 = 0
        
        // Create first finger path
        val path1 = pathFactory.createPath()
        path1.moveTo(x1.toFloat(), y1.toFloat())
        path1.lineTo(x2.toFloat(), y2.toFloat())
        val stroke1 = pathFactory.createStroke(
            path1, 0, PINCH_DURATION_MS.toLong(), false
        )

        // Calculate second finger positions
        x1 = x + startSpacing / 2
        y1 = y + startSpacing / 2
        x2 = x + endSpacing / 2
        y2 = y + endSpacing / 2

        // Ensure coordinates don't exceed screen bounds
        val metrics = getDisplayMetrics()
        if (x1 > metrics.widthPixels) x1 = metrics.widthPixels
        if (y1 > metrics.heightPixels) y1 = metrics.heightPixels
        if (x2 > metrics.widthPixels) x2 = metrics.widthPixels
        if (y2 > metrics.heightPixels) y2 = metrics.heightPixels

        // Create second finger path
        val path2 = pathFactory.createPath()
        path2.moveTo(x1.toFloat(), y1.toFloat())
        path2.lineTo(x2.toFloat(), y2.toFloat())
        val stroke2 = pathFactory.createStroke(
            path2, 0, PINCH_DURATION_MS.toLong(), false
        )

        // Build and queue gesture
        val gesture = pathFactory.createGesture(listOf(stroke1, stroke2))
        
        gestureQueue.add(gesture)
        if (gestureQueue.size == 1) {
            dispatchGestureHandler()
        }
    }
    
    /**
     * Perform drag gesture from start to end coordinates
     */
    private fun performDrag(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Long
    ): Boolean {
        return try {
            val path = pathFactory.createPath()
            path.moveTo(startX.toFloat(), startY.toFloat())
            path.lineTo(endX.toFloat(), endY.toFloat())

            val stroke = pathFactory.createStroke(path, 0, duration, false)
            val gesture = pathFactory.createGesture(listOf(stroke))

            dispatchGesture(gesture)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing drag gesture", e)
            false
        }
    }
    
    /**
     * Perform swipe gesture in specified direction
     */
    private fun performSwipe(
        direction: String, 
        centerX: Int, 
        centerY: Int, 
        distance: Int
    ): Boolean {
        val (endX, endY) = when (direction) {
            "up" -> centerX to centerY - distance
            "down" -> centerX to centerY + distance
            "left" -> centerX - distance to centerY
            "right" -> centerX + distance to centerY
            "" -> {
                // Default swipe right if no direction specified
                centerX + distance to centerY
            }
            else -> {
                Log.w(TAG, "Unknown swipe direction: $direction")
                return false
            }
        }
        
        return performDrag(centerX, centerY, endX, endY, 300L)
    }
    
    /**
     * Perform complex path gesture with multiple points
     */
    private fun performPathGesture(pathPoints: List<Point>, duration: Long): Boolean {
        return try {
            if (pathPoints.isEmpty()) return false

            val path = pathFactory.createPath()
            val firstPoint = pathPoints[0]
            path.moveTo(firstPoint.x.toFloat(), firstPoint.y.toFloat())

            // Add remaining points as lines
            for (i in 1 until pathPoints.size) {
                val point = pathPoints[i]
                path.lineTo(point.x.toFloat(), point.y.toFloat())
            }

            val stroke = pathFactory.createStroke(path, 0, duration, false)
            val gesture = pathFactory.createGesture(listOf(stroke))

            dispatchGesture(gesture)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing path gesture", e)
            false
        }
    }
    
    /**
     * Dispatch gesture with callback handling
     */
    private fun dispatchGesture(gesture: GestureDescription): Boolean {
        return try {
            service.dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "Gesture completed successfully")
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "Gesture was cancelled")
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching gesture", e)
            false
        }
    }
    
    /**
     * Dispatch queued gestures with callback handling
     * Migrated from Legacy with 100% functional equivalence
     */
    private fun dispatchGestureHandler() {
        if (gestureQueue.isEmpty()) return
        
        val gesture = gestureQueue[0]
        try {
            val success = service.dispatchGesture(gesture, gestureResultCallback, null)
            if (!success) {
                gestureQueue.clear()
                Log.w(TAG, "Failed to dispatch gesture, clearing queue")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in gesture handler", e)
            gestureQueue.clear()
        }
    }
    
    /**
     * Callback for queued gesture results
     * Migrated from Legacy with 100% functional equivalence
     */
    private val gestureResultCallback: GestureResultCallback = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription) {
            synchronized(lock) {
                if (gestureQueue.isNotEmpty()) {
                    gestureQueue.removeAt(0)
                    if (gestureQueue.isNotEmpty()) {
                        dispatchGestureHandler()
                    }
                }
            }
            super.onCompleted(gestureDescription)
        }
        
        override fun onCancelled(gestureDescription: GestureDescription) {
            synchronized(lock) {
                gestureQueue.clear()
            }
            super.onCancelled(gestureDescription)
        }
    }
    
    /**
     * Get screen center coordinates
     */
    private fun getScreenCenter(): Pair<Int, Int> {
        return try {
            val size = DisplayUtils.getRealScreenSize(service)
            Pair(size.x / 2, size.y / 2)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting screen center", e)
            Pair(0, 0)
        }
    }
    
    /**
     * Get display metrics for boundary checking
     */
    private fun getDisplayMetrics(): DisplayMetrics {
        return try {
            DisplayUtils.getRealDisplayMetrics(service)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting display metrics, using system defaults", e)
            Resources.getSystem().displayMetrics
        }
    }
    
    /**
     * Simple click gesture for coordinate-based taps
     */
    fun performClickAt(x: Float, y: Float): Boolean {
        return try {
            val path = pathFactory.createPath()
            path.moveTo(x, y)
            path.lineTo(x, y)

            val tap = pathFactory.createStroke(path, 0, tapDuration, false)
            val gesture = pathFactory.createGesture(listOf(tap))

            dispatchGesture(gesture)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing click at coordinates", e)
            false
        }
    }
    
    /**
     * Long press gesture for coordinate-based long presses
     */
    fun performLongPressAt(x: Float, y: Float): Boolean {
        return try {
            val path = pathFactory.createPath()
            path.moveTo(x, y)

            val stroke = pathFactory.createStroke(path, 0, longPressDuration, false)
            val gesture = pathFactory.createGesture(listOf(stroke))

            dispatchGesture(gesture)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing long press at coordinates", e)
            false
        }
    }
    
    /**
     * Double click gesture for coordinate-based double taps
     */
    fun performDoubleClickAt(x: Float, y: Float): Boolean {
        return try {
            mainScope.launch {
                // First tap
                performClickAt(x, y)
                // Wait for double tap timing
                delay(doubleTapDelay / 2)
                // Second tap
                performClickAt(x, y)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing double click at coordinates", e)
            false
        }
    }
    
    override fun dispose() {
        gestureQueue.clear()
        Log.d(TAG, "GestureHandler disposed")
    }
}