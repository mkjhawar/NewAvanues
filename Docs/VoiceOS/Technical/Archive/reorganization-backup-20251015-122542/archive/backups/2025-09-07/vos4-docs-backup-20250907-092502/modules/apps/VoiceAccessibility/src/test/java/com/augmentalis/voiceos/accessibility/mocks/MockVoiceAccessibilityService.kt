/**
 * MockVoiceAccessibilityService.kt - Mock implementation of VoiceAccessibilityService for unit testing
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-28
 * 
 * Provides a mock implementation of VoiceAccessibilityService for unit testing
 * handlers and action coordinators without requiring full accessibility service setup.
 */
package com.augmentalis.voiceos.accessibility.mocks

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.accessibility.managers.CursorManager
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mock implementation of VoiceAccessibilityService for testing
 * 
 * Simulates accessibility service functionality without requiring 
 * actual accessibility permissions during testing.
 */
class MockVoiceAccessibilityService : VoiceAccessibilityService() {
    
    companion object {
        private const val TAG = "MockVoiceAccessibilityService"
    }
    
    // State tracking
    private val isServiceRunning = AtomicBoolean(true)
    private val gestureCount = AtomicInteger(0)
    private val actionCount = AtomicInteger(0)
    
    // Action tracking
    private val performedGestures = ConcurrentLinkedQueue<PerformedGesture>()
    private val performedActions = ConcurrentLinkedQueue<PerformedAction>()
    private val launchedIntents = ConcurrentLinkedQueue<Intent>()
    private val accessibilityEvents = ConcurrentLinkedQueue<AccessibilityEvent>()
    
    // Mock cursor manager for testing
    private var mockCursorManager: CursorManager? = null
    
    data class PerformedGesture(
        val gestureType: String,
        val coordinates: Pair<Float, Float>? = null,
        val duration: Long = 0,
        val success: Boolean = true,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class PerformedAction(
        val actionType: String,
        val nodeInfo: String? = null,
        val arguments: Bundle? = null,
        val success: Boolean = true,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // AccessibilityService required methods - handle these manually without overriding
    fun handleAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "Mock accessibility event: ${event.eventType}")
        accessibilityEvents.offer(event)
    }
    
    fun handleInterrupt() {
        Log.d(TAG, "Mock accessibility service interrupted")
    }
    
    override fun onCreate() {
        super.onCreate()
        initialize()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dispose()
    }
    
    fun initialize() {
        isServiceRunning.set(true)
        Log.d(TAG, "Mock VoiceAccessibilityService initialized")
    }
    
    fun dispose() {
        isServiceRunning.set(false)
        Log.d(TAG, "Mock VoiceAccessibilityService disposed")
    }
    
    fun mockDispatchGesture(
        gesture: GestureDescription,
        callback: GestureResultCallback?,
        handler: android.os.Handler?
    ): Boolean {
        gestureCount.incrementAndGet()
        
        Log.d(TAG, "Mock dispatching gesture with ${gesture.strokeCount} strokes")
        
        val gestureType = determineGestureType(gesture)
        val coordinates = extractGestureCoordinates(gesture)
        val duration = extractGestureDuration(gesture)
        
        val performedGesture = PerformedGesture(
            gestureType = gestureType,
            coordinates = coordinates,
            duration = duration,
            success = true
        )
        
        performedGestures.offer(performedGesture)
        
        // Simulate successful gesture execution
        handler?.post {
            callback?.onCompleted(gesture)
        } ?: run {
            callback?.onCompleted(gesture)
        }
        
        return true
    }
    
    fun mockPerformGlobalAction(action: Int): Boolean {
        actionCount.incrementAndGet()
        
        val actionName = when (action) {
            AccessibilityService.GLOBAL_ACTION_BACK -> "GLOBAL_ACTION_BACK"
            AccessibilityService.GLOBAL_ACTION_HOME -> "GLOBAL_ACTION_HOME"
            AccessibilityService.GLOBAL_ACTION_RECENTS -> "GLOBAL_ACTION_RECENTS"
            AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS -> "GLOBAL_ACTION_NOTIFICATIONS"
            AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS -> "GLOBAL_ACTION_QUICK_SETTINGS"
            AccessibilityService.GLOBAL_ACTION_POWER_DIALOG -> "GLOBAL_ACTION_POWER_DIALOG"
            else -> "UNKNOWN_ACTION_$action"
        }
        
        Log.d(TAG, "Mock performing global action: $actionName")
        
        val performedAction = PerformedAction(
            actionType = "global:$actionName",
            success = true
        )
        
        performedActions.offer(performedAction)
        return true
    }
    
    /**
     * Mock implementation of performClick for testing
     */
    override fun performClick(x: Float, y: Float): Boolean {
        Log.d(TAG, "Mock performing click at ($x, $y)")
        
        val performedGesture = PerformedGesture(
            gestureType = "TAP",
            coordinates = Pair(x, y),
            duration = 100L,
            success = true
        )
        
        performedGestures.offer(performedGesture)
        return true
    }
    
    /**
     * Mock implementation of dispatchGesture for testing
     */
    fun dispatchGesture(
        gesture: GestureDescription,
        callback: GestureResultCallback?,
        handler: android.os.Handler?
    ): Boolean {
        return mockDispatchGesture(gesture, callback, handler)
    }
    
    /**
     * Mock implementation of getCursorManager for testing
     */
    override fun getCursorManager(): CursorManager? {
        return mockCursorManager
    }
    
    /**
     * Set mock cursor manager for testing
     */
    fun setMockCursorManager(cursorManager: CursorManager?) {
        mockCursorManager = cursorManager
    }
    
    private fun determineGestureType(gesture: GestureDescription): String {
        if (gesture.strokeCount == 0) return "EMPTY"
        if (gesture.strokeCount == 1) {
            val stroke = gesture.getStroke(0)
            val path = stroke.path
            
            // Analyze path to determine gesture type
            val bounds = Rect()
            path.computeBounds(android.graphics.RectF(bounds), true)
            
            val width = bounds.width()
            val height = bounds.height()
            
            return when {
                width < 50 && height < 50 -> "TAP"
                width > height * 2 -> "SWIPE_HORIZONTAL"
                height > width * 2 -> "SWIPE_VERTICAL"
                else -> "COMPLEX_GESTURE"
            }
        }
        return "MULTI_STROKE"
    }
    
    private fun extractGestureCoordinates(gesture: GestureDescription): Pair<Float, Float>? {
        if (gesture.strokeCount == 0) return null
        
        val stroke = gesture.getStroke(0)
        val path = stroke.path
        
        // Extract starting point
        val pathMeasure = android.graphics.PathMeasure(path, false)
        val coords = floatArrayOf(0f, 0f)
        pathMeasure.getPosTan(0f, coords, null)
        
        return Pair(coords[0], coords[1])
    }
    
    private fun extractGestureDuration(gesture: GestureDescription): Long {
        if (gesture.strokeCount == 0) return 0L
        
        val stroke = gesture.getStroke(0)
        return stroke.duration
    }
    
    // Query methods for testing
    fun isRunning(): Boolean = isServiceRunning.get()
    
    fun getGestureCount(): Int = gestureCount.get()
    
    fun getActionCount(): Int = actionCount.get()
    
    fun getPerformedGestures(): List<PerformedGesture> = performedGestures.toList()
    
    fun getPerformedActions(): List<PerformedAction> = performedActions.toList()
    
    fun getLaunchedIntents(): List<Intent> = launchedIntents.toList()
    
    fun reset() {
        performedGestures.clear()
        performedActions.clear()
        launchedIntents.clear()
        accessibilityEvents.clear()
        gestureCount.set(0)
        actionCount.set(0)
    }
    
    // Callback interface for gesture results
    interface GestureResultCallback {
        fun onCompleted(gesture: GestureDescription)
        fun onCancelled(gesture: GestureDescription) {}
    }
}