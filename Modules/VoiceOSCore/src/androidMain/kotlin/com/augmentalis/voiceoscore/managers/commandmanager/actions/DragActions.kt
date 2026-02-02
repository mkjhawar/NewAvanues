/**
 * DragActions.kt - Drag and pinch command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/DragActions.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: Drag, drop, and pinch-related voice command actions
 */

package com.augmentalis.voiceoscore.managers.commandmanager.actions

import com.augmentalis.voiceoscore.*
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drag and pinch command actions
 * Handles drag operations, pinch to zoom, and related gestures
 */
object DragActions {
    
    // Drag state management
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    
    /**
     * Start Drag Action
     */
    class StartDragAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val targetText = getTextParameter(command, "target")
            val x = getNumberParameter(command, "x")?.toFloat()
            val y = getNumberParameter(command, "y")?.toFloat()
            
            return when {
                // Start drag by text
                targetText != null -> {
                    val rootNode = accessibilityService?.rootInActiveWindow
                    val targetNode = findNodeByText(rootNode, targetText)
                    
                    if (targetNode != null) {
                        val center = getNodeCenter(targetNode)
                        startDragAt(center.x, center.y)
                        createSuccessResult(command, "Started dragging '$targetText'")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not find element with text '$targetText'")
                    }
                }
                
                // Start drag by coordinates
                x != null && y != null -> {
                    startDragAt(x, y)
                    createSuccessResult(command, "Started dragging at ($x, $y)")
                }
                
                else -> {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No target specified for drag start")
                }
            }
        }
    }
    
    /**
     * Stop Drag Action
     */
    class StopDragAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val targetText = getTextParameter(command, "target")
            val x = getNumberParameter(command, "x")?.toFloat()
            val y = getNumberParameter(command, "y")?.toFloat()
            
            return if (!isDragging) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "No active drag operation")
            } else {
                when {
                    // Drop by text
                    targetText != null -> {
                        val rootNode = accessibilityService?.rootInActiveWindow
                        val targetNode = findNodeByText(rootNode, targetText)
                        
                        if (targetNode != null) {
                            val center = getNodeCenter(targetNode)
                            val success = performDragTo(accessibilityService, center.x, center.y)
                            if (success) {
                                createSuccessResult(command, "Dropped on '$targetText'")
                            } else {
                                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to drop on '$targetText'")
                            }
                        } else {
                            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not find element with text '$targetText'")
                        }
                    }
                    
                    // Drop by coordinates
                    x != null && y != null -> {
                        val success = performDragTo(accessibilityService, x, y)
                        if (success) {
                            createSuccessResult(command, "Dropped at ($x, $y)")
                        } else {
                            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to drop at coordinates")
                        }
                    }
                    
                    else -> {
                        createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No target specified for drag drop")
                    }
                }
            }
        }
    }
    
    /**
     * Drag To Action (combined start and stop)
     */
    class DragToAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val fromText = getTextParameter(command, "from")
            val toText = getTextParameter(command, "to")
            val fromX = getNumberParameter(command, "fromX")?.toFloat()
            val fromY = getNumberParameter(command, "fromY")?.toFloat()
            val toX = getNumberParameter(command, "toX")?.toFloat()
            val toY = getNumberParameter(command, "toY")?.toFloat()
            
            val rootNode = accessibilityService?.rootInActiveWindow
            
            // Determine start coordinates
            val startCoords = when {
                fromText != null -> {
                    val fromNode = findNodeByText(rootNode, fromText)
                    if (fromNode != null) getNodeCenter(fromNode) else null
                }
                fromX != null && fromY != null -> android.graphics.PointF(fromX, fromY)
                else -> null
            }
            
            // Determine end coordinates
            val endCoords = when {
                toText != null -> {
                    val toNode = findNodeByText(rootNode, toText)
                    if (toNode != null) getNodeCenter(toNode) else null
                }
                toX != null && toY != null -> android.graphics.PointF(toX, toY)
                else -> null
            }
            
            return if (startCoords != null && endCoords != null) {
                val success = performFullDrag(accessibilityService, startCoords.x, startCoords.y, endCoords.x, endCoords.y)
                if (success) {
                    createSuccessResult(command, "Dragged from (${startCoords.x}, ${startCoords.y}) to (${endCoords.x}, ${endCoords.y})")
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to perform drag operation")
                }
            } else {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Invalid coordinates for drag operation")
            }
        }
    }
    
    /**
     * Pinch Open Action (Zoom In)
     */
    class PinchOpenAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val centerX = getNumberParameter(command, "x")?.toFloat()
            val centerY = getNumberParameter(command, "y")?.toFloat()
            val scale = getNumberParameter(command, "scale")?.toFloat() ?: 2.0f
            
            val (width, height) = getScreenDimensions(context)
            val actualCenterX = centerX ?: (width / 2f)
            val actualCenterY = centerY ?: (height / 2f)
            
            return if (performPinchOpen(accessibilityService, actualCenterX, actualCenterY, scale)) {
                createSuccessResult(command, "Pinched open (zoom in)")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to pinch open")
            }
        }
    }
    
    /**
     * Pinch Close Action (Zoom Out)
     */
    class PinchCloseAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val centerX = getNumberParameter(command, "x")?.toFloat()
            val centerY = getNumberParameter(command, "y")?.toFloat()
            val scale = getNumberParameter(command, "scale")?.toFloat() ?: 0.5f
            
            val (width, height) = getScreenDimensions(context)
            val actualCenterX = centerX ?: (width / 2f)
            val actualCenterY = centerY ?: (height / 2f)
            
            return if (performPinchClose(accessibilityService, actualCenterX, actualCenterY, scale)) {
                createSuccessResult(command, "Pinched close (zoom out)")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to pinch close")
            }
        }
    }
    
    /**
     * Zoom In Action
     */
    class ZoomInAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val (width, height) = getScreenDimensions(context)
            val centerX = width / 2f
            val centerY = height / 2f
            
            return if (performPinchOpen(accessibilityService, centerX, centerY, 1.5f)) {
                createSuccessResult(command, "Zoomed in")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to zoom in")
            }
        }
    }
    
    /**
     * Zoom Out Action
     */
    class ZoomOutAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val (width, height) = getScreenDimensions(context)
            val centerX = width / 2f
            val centerY = height / 2f
            
            return if (performPinchClose(accessibilityService, centerX, centerY, 0.75f)) {
                createSuccessResult(command, "Zoomed out")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to zoom out")
            }
        }
    }
    
    /**
     * Rotate Action
     */
    class RotateAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val direction = getTextParameter(command, "direction") ?: "right"
            val angle = getNumberParameter(command, "angle")?.toFloat() ?: 90f
            val centerX = getNumberParameter(command, "x")?.toFloat()
            val centerY = getNumberParameter(command, "y")?.toFloat()
            
            val (width, height) = getScreenDimensions(context)
            val actualCenterX = centerX ?: (width / 2f)
            val actualCenterY = centerY ?: (height / 2f)
            
            val clockwise = direction.lowercase() in listOf("right", "clockwise", "cw")
            
            return if (performRotate(accessibilityService, actualCenterX, actualCenterY, angle, clockwise)) {
                createSuccessResult(command, "Rotated $direction by ${angle}°")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to rotate")
            }
        }
    }
    
    // Helper methods
    
    /**
     * Start drag operation
     */
    private fun startDragAt(x: Float, y: Float) {
        isDragging = true
        dragStartX = x
        dragStartY = y
    }
    
    /**
     * Perform drag to coordinates
     */
    private fun performDragTo(service: AccessibilityService?, endX: Float, endY: Float): Boolean {
        if (service == null || !isDragging) return false
        
        val success = performFullDrag(service, dragStartX, dragStartY, endX, endY)
        isDragging = false
        return success
    }
    
    /**
     * Perform complete drag operation
     */
    private fun performFullDrag(
        service: AccessibilityService?,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = 500L
    ): Boolean {
        if (service == null) return false
        
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        
        return service.dispatchGesture(gesture, null, null)
    }
    
    /**
     * Perform pinch open (zoom in) gesture
     */
    private fun performPinchOpen(
        service: AccessibilityService?,
        centerX: Float,
        centerY: Float,
        scale: Float
    ): Boolean {
        if (service == null) return false
        
        val startDistance = 100f
        val endDistance = startDistance * scale
        
        // First finger path
        val path1 = Path().apply {
            moveTo(centerX - startDistance / 2, centerY)
            lineTo(centerX - endDistance / 2, centerY)
        }
        
        // Second finger path
        val path2 = Path().apply {
            moveTo(centerX + startDistance / 2, centerY)
            lineTo(centerX + endDistance / 2, centerY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, 500))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, 500))
            .build()
        
        return service.dispatchGesture(gesture, null, null)
    }
    
    /**
     * Perform pinch close (zoom out) gesture
     */
    private fun performPinchClose(
        service: AccessibilityService?,
        centerX: Float,
        centerY: Float,
        scale: Float
    ): Boolean {
        if (service == null) return false
        
        val startDistance = 200f
        val endDistance = startDistance * scale
        
        // First finger path
        val path1 = Path().apply {
            moveTo(centerX - startDistance / 2, centerY)
            lineTo(centerX - endDistance / 2, centerY)
        }
        
        // Second finger path
        val path2 = Path().apply {
            moveTo(centerX + startDistance / 2, centerY)
            lineTo(centerX + endDistance / 2, centerY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, 500))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, 500))
            .build()
        
        return service.dispatchGesture(gesture, null, null)
    }
    
    /**
     * Perform rotation gesture
     */
    private fun performRotate(
        service: AccessibilityService?,
        centerX: Float,
        centerY: Float,
        angle: Float,
        clockwise: Boolean
    ): Boolean {
        if (service == null) return false
        
        val radius = 100f
        val startAngle = 0f
        val endAngle = if (clockwise) angle else -angle
        
        // Convert angles to radians
        val startRad = Math.toRadians(startAngle.toDouble())
        val endRad = Math.toRadians(endAngle.toDouble())
        
        // Calculate start and end positions for two fingers
        val finger1StartX = centerX + radius * cos(startRad).toFloat()
        val finger1StartY = centerY + radius * sin(startRad).toFloat()
        val finger1EndX = centerX + radius * cos(endRad).toFloat()
        val finger1EndY = centerY + radius * sin(endRad).toFloat()
        
        val finger2StartX = centerX - radius * cos(startRad).toFloat()
        val finger2StartY = centerY - radius * sin(startRad).toFloat()
        val finger2EndX = centerX - radius * cos(endRad).toFloat()
        val finger2EndY = centerY - radius * sin(endRad).toFloat()
        
        // Create paths
        val path1 = Path().apply {
            moveTo(finger1StartX, finger1StartY)
            lineTo(finger1EndX, finger1EndY)
        }
        
        val path2 = Path().apply {
            moveTo(finger2StartX, finger2StartY)
            lineTo(finger2EndX, finger2EndY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, 800))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, 800))
            .build()
        
        return service.dispatchGesture(gesture, null, null)
    }
    
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
}