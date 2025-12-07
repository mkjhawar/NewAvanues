/**
 * AccessibilityService.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/service/AccessibilityService.kt
 * 
 * Created: 2025-01-26 01:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Dedicated accessibility service for VoiceCursor gesture dispatch and interaction
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-26 01:00 PST): Initial creation with gesture dispatch and service lifecycle
 */

package com.augmentalis.voiceos.cursor.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.view.CursorAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Dedicated accessibility service for VoiceCursor
 * Handles gesture dispatch and accessibility interactions
 * Separate from VoiceAccessibility module for independence
 */
class VoiceCursorAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "VoiceCursorAccessibility"
        private var instance: VoiceCursorAccessibilityService? = null
        
        /**
         * Get service instance if available
         */
        fun getInstance(): VoiceCursorAccessibilityService? = instance
        
        /**
         * Check if service is running
         */
        fun isServiceEnabled(): Boolean = instance != null
    }
    
    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    
    // Gesture configuration
    private val clickDuration = 50L // ms
    private val longPressDuration = 1000L // ms
    private val dragStrokeWidth = 10f
    private val scrollDistance = 200f
    
    // Current cursor position for gesture dispatch
    private var currentCursorPosition = CursorOffset(0f, 0f)
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AccessibilityService created")
        instance = this
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService connected")
        
        // Notify overlay service that accessibility is ready
        sendBroadcast(Intent("com.augmentalis.voiceos.cursor.ACCESSIBILITY_READY"))
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AccessibilityService destroyed")
        
        serviceScope.cancel()
        instance = null
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to process accessibility events for cursor functionality
        // This service is primarily for gesture dispatch
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
    }
    
    /**
     * Update current cursor position for gesture dispatch
     */
    fun updateCursorPosition(position: CursorOffset) {
        currentCursorPosition = position
    }
    
    /**
     * Execute cursor action at current position
     */
    fun executeAction(action: CursorAction, position: CursorOffset? = null) {
        val targetPosition = position ?: currentCursorPosition
        
        serviceScope.launch {
            try {
                when (action) {
                    CursorAction.SINGLE_CLICK -> performClick(targetPosition)
                    CursorAction.DOUBLE_CLICK -> performDoubleClick(targetPosition)
                    CursorAction.LONG_PRESS -> performLongPress(targetPosition)
                    CursorAction.DRAG_START -> startDrag(targetPosition)
                    CursorAction.DRAG_END -> endDrag(targetPosition)
                    CursorAction.SCROLL_UP -> performScroll(targetPosition, isUp = true)
                    CursorAction.SCROLL_DOWN -> performScroll(targetPosition, isUp = false)
                    CursorAction.CENTER_CURSOR -> {
                        // This is handled by the cursor view, not accessibility service
                        Log.d(TAG, "Center cursor action - delegated to cursor view")
                    }
                    CursorAction.HIDE_CURSOR -> {
                        // This is handled by the cursor view, not accessibility service
                        Log.d(TAG, "Hide cursor action - delegated to cursor view")
                    }
                    CursorAction.TOGGLE_COORDINATES -> {
                        // This is handled by the cursor view, not accessibility service
                        Log.d(TAG, "Toggle coordinates action - delegated to cursor view")
                    }
                    CursorAction.SHOW_HELP -> {
                        // This is handled by the cursor view, not accessibility service
                        Log.d(TAG, "Show help action - delegated to cursor view")
                    }
                    CursorAction.SHOW_SETTINGS -> {
                        // This is handled by the cursor view, not accessibility service
                        Log.d(TAG, "Show settings action - delegated to cursor view")
                    }
                    CursorAction.CALIBRATE_CLICK -> {
                        // This is handled by the cursor view, not accessibility service
                        Log.d(TAG, "Calibrate click action - delegated to cursor view")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing cursor action: ${action.name}", e)
            }
        }
    }
    
    /**
     * Perform single click gesture
     */
    private fun performClick(position: CursorOffset) {
        val clickPath = Path().apply {
            moveTo(position.x, position.y)
        }
        
        val clickGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, clickDuration))
            .build()
        
        val success = dispatchGesture(clickGesture, object : GestureResultCallback() {
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
     */
    private fun performDoubleClick(position: CursorOffset) {
        // First click
        performClick(position)
        
        // Second click after short delay
        handler.postDelayed({
            performClick(position)
        }, 100)
    }
    
    /**
     * Perform long press gesture
     */
    private fun performLongPress(position: CursorOffset) {
        val longPressPath = Path().apply {
            moveTo(position.x, position.y)
        }
        
        val longPressGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(longPressPath, 0, longPressDuration))
            .build()
        
        val success = dispatchGesture(longPressGesture, object : GestureResultCallback() {
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
     */
    private fun startDrag(position: CursorOffset) {
        // Store drag start position for future drag operations
        dragStartPosition = position
        Log.d(TAG, "Drag started at (${position.x}, ${position.y})")
    }
    
    /**
     * End drag operation
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
        
        val success = dispatchGesture(dragGesture, object : GestureResultCallback() {
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
        
        val success = dispatchGesture(scrollGesture, object : GestureResultCallback() {
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
     */
    private fun calculateDragDuration(start: CursorOffset, end: CursorOffset): Long {
        val distance = kotlin.math.sqrt(
            (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
        )
        // Base duration 200ms + 1ms per pixel of distance, capped at 2000ms
        return (200 + distance.toLong()).coerceAtMost(2000)
    }
    
    /**
     * Check if gesture dispatch is available
     */
    fun isGestureDispatchAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
    }
    
    // Store drag start position for drag operations
    private var dragStartPosition: CursorOffset? = null
}