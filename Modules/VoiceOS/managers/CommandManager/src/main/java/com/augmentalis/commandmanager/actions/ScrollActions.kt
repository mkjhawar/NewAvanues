/**
 * ScrollActions.kt - Scroll and swipe command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/ScrollActions.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: Scroll and swipe-related voice command actions
 */

package com.augmentalis.commandmanager.actions

import com.augmentalis.commandmanager.*
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

/**
 * Scroll and swipe command actions
 * Handles scrolling, swiping, and page navigation
 */
object ScrollActions {
    
    /**
     * Scroll Up Action
     */
    class ScrollUpAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val distance = getNumberParameter(command, "distance")?.toFloat()
            
            return if (performScrollUp(accessibilityService, context, distance)) {
                createSuccessResult(command, "Scrolled up")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to scroll up")
            }
        }
    }
    
    /**
     * Scroll Down Action
     */
    class ScrollDownAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val distance = getNumberParameter(command, "distance")?.toFloat()
            
            return if (performScrollDown(accessibilityService, context, distance)) {
                createSuccessResult(command, "Scrolled down")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to scroll down")
            }
        }
    }
    
    /**
     * Scroll Left Action
     */
    class ScrollLeftAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val distance = getNumberParameter(command, "distance")?.toFloat()
            
            return if (performScrollLeft(accessibilityService, context, distance)) {
                createSuccessResult(command, "Scrolled left")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to scroll left")
            }
        }
    }
    
    /**
     * Scroll Right Action
     */
    class ScrollRightAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val distance = getNumberParameter(command, "distance")?.toFloat()
            
            return if (performScrollRight(accessibilityService, context, distance)) {
                createSuccessResult(command, "Scrolled right")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to scroll right")
            }
        }
    }
    
    /**
     * Page Up Action
     */
    class PageUpAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return if (performPageScroll(accessibilityService, context, true)) {
                createSuccessResult(command, "Page up")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to page up")
            }
        }
    }
    
    /**
     * Page Down Action
     */
    class PageDownAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return if (performPageScroll(accessibilityService, context, false)) {
                createSuccessResult(command, "Page down")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to page down")
            }
        }
    }
    
    /**
     * Swipe Up Action
     */
    class SwipeUpAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val fast = getBooleanParameter(command, "fast") ?: false
            
            return if (performSwipeUp(accessibilityService, context, fast)) {
                createSuccessResult(command, if (fast) "Fast swipe up" else "Swipe up")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to swipe up")
            }
        }
    }
    
    /**
     * Swipe Down Action
     */
    class SwipeDownAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val fast = getBooleanParameter(command, "fast") ?: false
            
            return if (performSwipeDown(accessibilityService, context, fast)) {
                createSuccessResult(command, if (fast) "Fast swipe down" else "Swipe down")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to swipe down")
            }
        }
    }
    
    /**
     * Swipe Left Action
     */
    class SwipeLeftAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val fast = getBooleanParameter(command, "fast") ?: false
            
            return if (performSwipeLeft(accessibilityService, context, fast)) {
                createSuccessResult(command, if (fast) "Fast swipe left" else "Swipe left")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to swipe left")
            }
        }
    }
    
    /**
     * Swipe Right Action
     */
    class SwipeRightAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val fast = getBooleanParameter(command, "fast") ?: false
            
            return if (performSwipeRight(accessibilityService, context, fast)) {
                createSuccessResult(command, if (fast) "Fast swipe right" else "Swipe right")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to swipe right")
            }
        }
    }
    
    /**
     * Scroll To Top Action
     */
    class ScrollToTopAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            var attempts = 0
            val maxAttempts = 10
            
            // Perform multiple scroll up gestures to reach top
            while (attempts < maxAttempts) {
                if (!performScrollUp(accessibilityService, context, null)) {
                    break
                }
                attempts++
                kotlinx.coroutines.delay(200) // Small delay between scrolls
            }
            
            return createSuccessResult(command, "Scrolled to top")
        }
    }
    
    /**
     * Scroll To Bottom Action
     */
    class ScrollToBottomAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            var attempts = 0
            val maxAttempts = 10
            
            // Perform multiple scroll down gestures to reach bottom
            while (attempts < maxAttempts) {
                if (!performScrollDown(accessibilityService, context, null)) {
                    break
                }
                attempts++
                kotlinx.coroutines.delay(200) // Small delay between scrolls
            }
            
            return createSuccessResult(command, "Scrolled to bottom")
        }
    }
    
    // Helper methods
    
    /**
     * Get screen dimensions using modern WindowMetrics API for Android 11+ (API 30+)
     * with fallback to deprecated display methods for older versions.
     */
    private fun getScreenDimensions(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use modern WindowMetrics API for Android 11+ (API 30+)
            val bounds = windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            // Fallback to deprecated API for older versions
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val point = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(point)
            Pair(point.x, point.y)
        }
    }
    
    /**
     * Perform scroll up gesture
     */
    private fun performScrollUp(
        service: AccessibilityService?,
        context: Context,
        distance: Float?
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width / 2f
        val startY = height * 0.7f
        val endY = startY - (distance ?: (height * 0.4f))
        
        return performSwipeGesture(service, startX, startY, startX, endY)
    }
    
    /**
     * Perform scroll down gesture
     */
    private fun performScrollDown(
        service: AccessibilityService?,
        context: Context,
        distance: Float?
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width / 2f
        val startY = height * 0.3f
        val endY = startY + (distance ?: (height * 0.4f))
        
        return performSwipeGesture(service, startX, startY, startX, endY)
    }
    
    /**
     * Perform scroll left gesture
     */
    private fun performScrollLeft(
        service: AccessibilityService?,
        context: Context,
        distance: Float?
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width * 0.7f
        val startY = height / 2f
        val endX = startX - (distance ?: (width * 0.4f))
        
        return performSwipeGesture(service, startX, startY, endX, startY)
    }
    
    /**
     * Perform scroll right gesture
     */
    private fun performScrollRight(
        service: AccessibilityService?,
        context: Context,
        distance: Float?
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width * 0.3f
        val startY = height / 2f
        val endX = startX + (distance ?: (width * 0.4f))
        
        return performSwipeGesture(service, startX, startY, endX, startY)
    }
    
    /**
     * Perform page scroll (full screen height)
     */
    private fun performPageScroll(
        service: AccessibilityService?,
        context: Context,
        up: Boolean
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width / 2f
        val distance = height * 0.8f // Almost full screen
        
        return if (up) {
            val startY = height * 0.8f
            val endY = startY - distance
            performSwipeGesture(service, startX, startY, startX, endY)
        } else {
            val startY = height * 0.2f
            val endY = startY + distance
            performSwipeGesture(service, startX, startY, startX, endY)
        }
    }
    
    /**
     * Perform swipe up gesture
     */
    private fun performSwipeUp(
        service: AccessibilityService?,
        context: Context,
        fast: Boolean
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width / 2f
        val startY = height * 0.8f
        val endY = height * 0.2f
        val duration = if (fast) 100L else 300L
        
        return performSwipeGesture(service, startX, startY, startX, endY, duration)
    }
    
    /**
     * Perform swipe down gesture
     */
    private fun performSwipeDown(
        service: AccessibilityService?,
        context: Context,
        fast: Boolean
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width / 2f
        val startY = height * 0.2f
        val endY = height * 0.8f
        val duration = if (fast) 100L else 300L
        
        return performSwipeGesture(service, startX, startY, startX, endY, duration)
    }
    
    /**
     * Perform swipe left gesture
     */
    private fun performSwipeLeft(
        service: AccessibilityService?,
        context: Context,
        fast: Boolean
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width * 0.8f
        val startY = height / 2f
        val endX = width * 0.2f
        val duration = if (fast) 100L else 300L
        
        return performSwipeGesture(service, startX, startY, endX, startY, duration)
    }
    
    /**
     * Perform swipe right gesture
     */
    private fun performSwipeRight(
        service: AccessibilityService?,
        context: Context,
        fast: Boolean
    ): Boolean {
        if (service == null) return false
        
        val (width, height) = getScreenDimensions(context)
        val startX = width * 0.2f
        val startY = height / 2f
        val endX = width * 0.8f
        val duration = if (fast) 100L else 300L
        
        return performSwipeGesture(service, startX, startY, endX, startY, duration)
    }
    
    /**
     * Perform swipe gesture using accessibility service
     */
    private fun performSwipeGesture(
        service: AccessibilityService,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = 300L
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        
        return service.dispatchGesture(gesture, null, null)
    }
}