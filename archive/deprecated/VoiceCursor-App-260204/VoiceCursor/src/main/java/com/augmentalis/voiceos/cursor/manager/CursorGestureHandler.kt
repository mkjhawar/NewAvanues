/**
 * CursorGestureHandler.kt
 *
 * Created: 2025-09-26 16:20:54 IST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Handles cursor gestures using provided accessibility service
 * Extracted from VoiceCursorAccessibilityService functionality
 * Module: VoiceCursor System
 */

package com.augmentalis.voiceos.cursor.manager

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.view.CursorAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Handles cursor gestures using provided accessibility service
 * Extracted from VoiceCursorAccessibilityService functionality
 */
class CursorGestureHandler(private val accessibilityService: AccessibilityService) {

    companion object {
        private const val TAG = "CursorGestureHandler"
    }

    // Extracted from VoiceCursorAccessibilityService lines 61-67
    private val clickDuration = 50L
    private val longPressDuration = 1000L
    private val dragStrokeWidth = 10f
    private val scrollDistance = 200f
    private var currentCursorPosition = CursorOffset(0f, 0f)
    private var dragStartPosition: CursorOffset? = null

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Update current cursor position for gesture dispatch
     * Extracted from VoiceCursorAccessibilityService.updateCursorPosition() (lines 103-105)
     */
    fun updateCursorPosition(position: CursorOffset) {
        currentCursorPosition = position
    }

    /**
     * Execute cursor action at specified position
     * Extracted from VoiceCursorAccessibilityService.executeAction() (lines 110-152)
     */
    fun executeAction(action: CursorAction, position: CursorOffset): Boolean {
        serviceScope.launch {
            try {
                when (action) {
                    CursorAction.SINGLE_CLICK -> performClick(position)
                    CursorAction.DOUBLE_CLICK -> performDoubleClick(position)
                    CursorAction.LONG_PRESS -> performLongPress(position)
                    CursorAction.DRAG_START -> startDrag(position)
                    CursorAction.DRAG_END -> endDrag(position)
                    CursorAction.SCROLL_UP -> performScroll(position, isUp = true)
                    CursorAction.SCROLL_DOWN -> performScroll(position, isUp = false)
                    CursorAction.CENTER_CURSOR -> {
                        Log.d(TAG, "Center cursor action - delegated to cursor view")
                    }
                    CursorAction.HIDE_CURSOR -> {
                        Log.d(TAG, "Hide cursor action - delegated to cursor view")
                    }
                    CursorAction.TOGGLE_COORDINATES -> {
                        Log.d(TAG, "Toggle coordinates action - delegated to cursor view")
                    }
                    CursorAction.SHOW_HELP -> {
                        Log.d(TAG, "Show help action - delegated to cursor view")
                    }
                    CursorAction.SHOW_SETTINGS -> {
                        Log.d(TAG, "Show settings action - delegated to cursor view")
                    }
                    CursorAction.CALIBRATE_CLICK -> {
                        Log.d(TAG, "Calibrate click action - delegated to cursor view")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing cursor action: ${action.name}", e)
            }
        }
        return true
    }

    /**
     * Perform single click gesture
     * Extracted from VoiceCursorAccessibilityService.performClick() (lines 157-179)
     */
    private fun performClick(position: CursorOffset) {
        val clickPath = Path().apply {
            moveTo(position.x, position.y)
        }

        val clickGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, clickDuration))
            .build()

        val success = accessibilityService.dispatchGesture(clickGesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.d(TAG, "Click gesture completed at (${position.x}, ${position.y})")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Click gesture cancelled")
                }
            }, null)

        if (!success) {
            Log.e(TAG, "Failed to dispatch click gesture")
        }
    }

    /**
     * Perform double click gesture
     * Extracted from VoiceCursorAccessibilityService.performDoubleClick() (lines 184-192)
     */
    private fun performDoubleClick(position: CursorOffset) {
        performClick(position)
        handler.postDelayed({
            performClick(position)
        }, 100)
    }

    /**
     * Perform long press gesture
     * Extracted from VoiceCursorAccessibilityService.performLongPress() (lines 197-219)
     */
    private fun performLongPress(position: CursorOffset) {
        val longPressPath = Path().apply {
            moveTo(position.x, position.y)
        }

        val longPressGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(longPressPath, 0, longPressDuration))
            .build()

        val success = accessibilityService.dispatchGesture(longPressGesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.d(TAG, "Long press gesture completed at (${position.x}, ${position.y})")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Long press gesture cancelled")
                }
            }, null)

        if (!success) {
            Log.e(TAG, "Failed to dispatch long press gesture")
        }
    }

    /**
     * Start drag operation
     * Extracted from VoiceCursorAccessibilityService.startDrag() (lines 224-228)
     */
    private fun startDrag(position: CursorOffset) {
        dragStartPosition = position
        Log.d(TAG, "Drag started at (${position.x}, ${position.y})")
    }

    /**
     * End drag operation
     * Extracted from VoiceCursorAccessibilityService.endDrag() (lines 233-270)
     */
    private fun endDrag(position: CursorOffset) {
        val startPos = dragStartPosition ?: run {
            Log.w(TAG, "Drag end called without drag start")
            return
        }

        val dragPath = Path().apply {
            moveTo(startPos.x, startPos.y)
            lineTo(position.x, position.y)
        }

        val dragGesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    dragPath,
                    0,
                    calculateDragDuration(startPos, position)
                )
            )
            .build()

        val success = accessibilityService.dispatchGesture(dragGesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.d(TAG, "Drag gesture completed from (${startPos.x}, ${startPos.y}) to (${position.x}, ${position.y})")
                    dragStartPosition = null
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Drag gesture cancelled")
                    dragStartPosition = null
                }
            }, null)

        if (!success) {
            Log.e(TAG, "Failed to dispatch drag gesture")
            dragStartPosition = null
        }
    }

    /**
     * Perform scroll gesture
     * Extracted from VoiceCursorAccessibilityService.performScroll() (lines 275-303)
     */
    private fun performScroll(position: CursorOffset, isUp: Boolean) {
        val scrollPath = Path().apply {
            moveTo(position.x, position.y)
            if (isUp) {
                lineTo(position.x, position.y - scrollDistance)
            } else {
                lineTo(position.x, position.y + scrollDistance)
            }
        }

        val scrollGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(scrollPath, 0, 300))
            .build()

        val success = accessibilityService.dispatchGesture(scrollGesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    val direction = if (isUp) "up" else "down"
                    Log.d(TAG, "Scroll $direction gesture completed at (${position.x}, ${position.y})")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w(TAG, "Scroll gesture cancelled")
                }
            }, null)

        if (!success) {
            Log.e(TAG, "Failed to dispatch scroll gesture")
        }
    }

    /**
     * Calculate drag duration based on distance
     * Extracted from VoiceCursorAccessibilityService.calculateDragDuration() (lines 308-314)
     */
    private fun calculateDragDuration(start: CursorOffset, end: CursorOffset): Long {
        val distance = kotlin.math.sqrt(
            (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
        )
        return (200 + distance.toLong()).coerceAtMost(2000)
    }

    /**
     * Check if gesture dispatch is available
     * Extracted from VoiceCursorAccessibilityService.isGestureDispatchAvailable() (lines 319-321)
     */
    fun isGestureDispatchAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        serviceScope.cancel()
        dragStartPosition = null
    }
}