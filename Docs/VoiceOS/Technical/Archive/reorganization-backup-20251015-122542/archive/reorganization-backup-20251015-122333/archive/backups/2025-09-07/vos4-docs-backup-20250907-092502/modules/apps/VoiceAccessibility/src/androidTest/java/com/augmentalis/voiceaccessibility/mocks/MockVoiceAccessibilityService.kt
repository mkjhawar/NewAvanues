/**
 * MockVoiceAccessibilityService.kt - Mock implementation of VoiceAccessibilityService
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-28
 * 
 * Provides a mock implementation of VoiceAccessibilityService for testing
 * handlers and action coordinators without requiring full accessibility service setup.
 */
package com.augmentalis.voiceaccessibility.mocks

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
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mock implementation of VoiceAccessibilityService for testing
 * 
 * Simulates accessibility service functionality without requiring 
 * actual accessibility permissions during testing.
 */
class MockVoiceAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "MockVoiceAccessibilityService"
    }
    
    // State tracking
    private val isServiceRunning = AtomicBoolean(false)
    private val gestureCount = AtomicInteger(0)
    private val actionCount = AtomicInteger(0)
    
    // Action tracking
    private val performedGestures = ConcurrentLinkedQueue<PerformedGesture>()
    private val performedActions = ConcurrentLinkedQueue<PerformedAction>()
    private val launchedIntents = ConcurrentLinkedQueue<Intent>()
    private val accessibilityEvents = ConcurrentLinkedQueue<AccessibilityEvent>()
    
    // Mock context
    private val mockContext: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    
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
    
    // Override AccessibilityService methods
    
    override fun onCreate() {
        super.onCreate()
        isServiceRunning.set(true)
        Log.d(TAG, "Mock VoiceAccessibilityService created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning.set(false)
        Log.d(TAG, "Mock VoiceAccessibilityService destroyed")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "Mock accessibility event: ${event.eventType}")
        accessibilityEvents.offer(event)
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Mock accessibility service interrupted")
    }
    
    // Mock gesture dispatch methods
    
    fun mockDispatchGesture(
        gesture: GestureDescription,
        callback: GestureResultCallback?,
        handler: android.os.Handler?
    ): Boolean {
        gestureCount.incrementAndGet()
        
        Log.d(TAG, "Mock dispatching gesture with ${gesture.strokeCount} strokes")
        
        // Simulate gesture processing
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
    
    // Mock node action methods
    
    fun mockPerformGlobalAction(action: Int): Boolean {
        actionCount.incrementAndGet()
        
        val actionName = when (action) {
            GLOBAL_ACTION_BACK -> "GLOBAL_ACTION_BACK"
            GLOBAL_ACTION_HOME -> "GLOBAL_ACTION_HOME"
            GLOBAL_ACTION_RECENTS -> "GLOBAL_ACTION_RECENTS"
            GLOBAL_ACTION_NOTIFICATIONS -> "GLOBAL_ACTION_NOTIFICATIONS"
            GLOBAL_ACTION_QUICK_SETTINGS -> "GLOBAL_ACTION_QUICK_SETTINGS"
            GLOBAL_ACTION_POWER_DIALOG -> "GLOBAL_ACTION_POWER_DIALOG"
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
    
    fun mockPerformNodeAction(nodeInfo: AccessibilityNodeInfo?, action: Int, arguments: Bundle?): Boolean {
        actionCount.incrementAndGet()
        
        val actionName = when (action) {
            AccessibilityNodeInfo.ACTION_CLICK -> "ACTION_CLICK"
            AccessibilityNodeInfo.ACTION_LONG_CLICK -> "ACTION_LONG_CLICK"
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> "ACTION_SCROLL_FORWARD"
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> "ACTION_SCROLL_BACKWARD"
            AccessibilityNodeInfo.ACTION_SET_TEXT -> "ACTION_SET_TEXT"
            AccessibilityNodeInfo.ACTION_FOCUS -> "ACTION_FOCUS"
            else -> "UNKNOWN_NODE_ACTION_$action"
        }
        
        Log.d(TAG, "Mock performing node action: $actionName on ${nodeInfo?.className}")
        
        val performedAction = PerformedAction(
            actionType = "node:$actionName",
            nodeInfo = nodeInfo?.className?.toString(),
            arguments = arguments,
            success = true
        )
        
        performedActions.offer(performedAction)
        return true
    }
    
    // Mock intent launching
    
    fun mockStartActivity(intent: Intent): Boolean {
        Log.d(TAG, "Mock launching intent: ${intent.action} ${intent.component}")
        
        launchedIntents.offer(intent)
        return true
    }
    
    // Mock root node access
    
    fun mockGetRootInActiveWindow(): AccessibilityNodeInfo? {
        // Return a mock node info for testing
        return createMockNodeInfo("MockRootNode", "android.widget.FrameLayout")
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun createMockNodeInfo(text: String, className: String): AccessibilityNodeInfo? {
        // In a real implementation, this would create a proper AccessibilityNodeInfo
        // For testing purposes, we'll return null or a simple mock
        return null
    }
    
    // Utility methods for gesture analysis
    
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
    
    fun getAccessibilityEvents(): List<AccessibilityEvent> = accessibilityEvents.toList()
    
    fun hasPerformedGesture(gestureType: String): Boolean {
        return performedGestures.any { it.gestureType == gestureType }
    }
    
    fun hasPerformedAction(actionType: String): Boolean {
        return performedActions.any { it.actionType.contains(actionType) }
    }
    
    fun hasLaunchedApp(packageName: String): Boolean {
        return launchedIntents.any { intent ->
            intent.component?.packageName == packageName ||
            intent.`package` == packageName
        }
    }
    
    // Test utilities
    
    fun reset() {
        performedGestures.clear()
        performedActions.clear()
        launchedIntents.clear()
        accessibilityEvents.clear()
        gestureCount.set(0)
        actionCount.set(0)
    }
    
    fun getDebugInfo(): String {
        return buildString {
            appendLine("MockVoiceAccessibilityService Debug Info")
            appendLine("Service Running: ${isServiceRunning.get()}")
            appendLine("Gestures Performed: ${gestureCount.get()}")
            appendLine("Actions Performed: ${actionCount.get()}")
            appendLine("Intents Launched: ${launchedIntents.size}")
            appendLine("Accessibility Events: ${accessibilityEvents.size}")
            
            if (performedGestures.isNotEmpty()) {
                appendLine("Recent Gestures:")
                performedGestures.toList().takeLast(3).forEach { gesture ->
                    appendLine("  - ${gesture.gestureType} at ${gesture.coordinates} (${gesture.duration}ms)")
                }
            }
            
            if (performedActions.isNotEmpty()) {
                appendLine("Recent Actions:")
                performedActions.toList().takeLast(3).forEach { action ->
                    appendLine("  - ${action.actionType} on ${action.nodeInfo}")
                }
            }
            
            if (launchedIntents.isNotEmpty()) {
                appendLine("Recent Intents:")
                launchedIntents.toList().takeLast(3).forEach { intent ->
                    appendLine("  - ${intent.action} ${intent.component}")
                }
            }
        }
    }
    
    // Mock context methods that handlers might need
    
    fun getContextForTesting(): Context = mockContext
    
    // Gesture creation utilities for testing
    
    fun createTapGesture(x: Float, y: Float, duration: Long = 100): GestureDescription {
        val path = Path().apply {
            moveTo(x, y)
        }
        
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        return GestureDescription.Builder()
            .addStroke(stroke)
            .build()
    }
    
    fun createSwipeGesture(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        duration: Long = 300
    ): GestureDescription {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        return GestureDescription.Builder()
            .addStroke(stroke)
            .build()
    }
}