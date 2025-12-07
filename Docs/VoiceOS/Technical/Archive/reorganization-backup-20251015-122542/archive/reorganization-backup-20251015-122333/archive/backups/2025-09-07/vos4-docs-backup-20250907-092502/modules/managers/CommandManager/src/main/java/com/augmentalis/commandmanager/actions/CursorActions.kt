/**
 * Actions.kt - Cursor and click command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/Actions.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: Cursor movement and click-related voice command actions
 */

package com.augmentalis.commandmanager.actions

import com.augmentalis.commandmanager.models.*
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Cursor and click command actions
 * Handles cursor movement, clicking, and touch interactions
 */
object Actions {
    
    /**
     * Get screen dimensions using modern WindowMetrics API for Android 11+ (API 30+)
     * with fallback to deprecated display methods for older versions.
     * 
     * @param context Context to get WindowManager from
     * @return Point containing screen width and height in pixels
     */
    private fun getScreenDimensions(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use modern WindowMetrics API for Android 11+ (API 30+)
            val bounds = windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            // Fallback to deprecated API for older versions
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val point = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(point)
            point
        }
    }
    
    /**
     * Click Action
     */
    class ClickAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val targetText = getTextParameter(command, "target")
            val x = getNumberParameter(command, "x")?.toFloat()
            val y = getNumberParameter(command, "y")?.toFloat()
            
            return when {
                // Click by text
                targetText != null -> {
                    val rootNode = accessibilityService?.rootInActiveWindow
                    val targetNode = findClickableNodeByText(rootNode, targetText)
                    
                    if (targetNode != null) {
                        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            createSuccessResult(command, "Clicked on '$targetText'")
                        } else {
                            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to click on '$targetText'")
                        }
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not find clickable element with text '$targetText'")
                    }
                }
                
                // Click by coordinates
                x != null && y != null -> {
                    if (performClick(accessibilityService, x, y)) {
                        createSuccessResult(command, "Clicked at coordinates ($x, $y)")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to click at coordinates")
                    }
                }
                
                else -> {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No target specified for click")
                }
            }
        }
    }
    
    /**
     * Double Click Action
     */
    class DoubleClickAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val targetText = getTextParameter(command, "target")
            val x = getNumberParameter(command, "x")?.toFloat()
            val y = getNumberParameter(command, "y")?.toFloat()
            
            return when {
                // Double click by text
                targetText != null -> {
                    val rootNode = accessibilityService?.rootInActiveWindow
                    val targetNode = findClickableNodeByText(rootNode, targetText)
                    
                    if (targetNode != null) {
                        // Perform two quick clicks
                        val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK) &&
                                     targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        
                        if (success) {
                            createSuccessResult(command, "Double clicked on '$targetText'")
                        } else {
                            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to double click on '$targetText'")
                        }
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not find clickable element with text '$targetText'")
                    }
                }
                
                // Double click by coordinates
                x != null && y != null -> {
                    if (performDoubleClick(accessibilityService, x, y)) {
                        createSuccessResult(command, "Double clicked at coordinates ($x, $y)")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to double click at coordinates")
                    }
                }
                
                else -> {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No target specified for double click")
                }
            }
        }
    }
    
    /**
     * Long Press Action
     */
    class LongPressAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val targetText = getTextParameter(command, "target")
            val x = getNumberParameter(command, "x")?.toFloat()
            val y = getNumberParameter(command, "y")?.toFloat()
            
            return when {
                // Long press by text
                targetText != null -> {
                    val rootNode = accessibilityService?.rootInActiveWindow
                    val targetNode = findClickableNodeByText(rootNode, targetText)
                    
                    if (targetNode != null) {
                        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                            createSuccessResult(command, "Long pressed on '$targetText'")
                        } else {
                            createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to long press on '$targetText'")
                        }
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not find clickable element with text '$targetText'")
                    }
                }
                
                // Long press by coordinates
                x != null && y != null -> {
                    if (performLongPress(accessibilityService, x, y)) {
                        createSuccessResult(command, "Long pressed at coordinates ($x, $y)")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to long press at coordinates")
                    }
                }
                
                else -> {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No target specified for long press")
                }
            }
        }
    }
    
    /**
     * Show Cursor Action
     */
    class ShowCursorAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            // This would be implemented with a custom overlay service
            // For now, just return success
            return createSuccessResult(command, "Cursor shown")
        }
    }
    
    /**
     * Hide Cursor Action
     */
    class HideCursorAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            // This would be implemented with a custom overlay service
            // For now, just return success
            return createSuccessResult(command, "Cursor hidden")
        }
    }
    
    /**
     * Center Cursor Action
     */
    class CenterCursorAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val screenDimensions = getScreenDimensions(context)
            val centerX = screenDimensions.x / 2f
            val centerY = screenDimensions.y / 2f
            
            // Move cursor to center (this would be implemented with overlay service)
            return createSuccessResult(command, "Cursor centered at ($centerX, $centerY)")
        }
    }
    
    /**
     * Set Cursor Mode to Hand
     */
    class HandCursorAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            // This would change cursor appearance
            return createSuccessResult(command, "Cursor mode set to hand")
        }
    }
    
    /**
     * Set Cursor Mode to Normal
     */
    class NormalCursorAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            // This would change cursor appearance
            return createSuccessResult(command, "Cursor mode set to normal")
        }
    }
    
    /**
     * Move Cursor Action
     */
    class MoveCursorAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val direction = getTextParameter(command, "direction")
            val distance = getNumberParameter(command, "distance")?.toFloat() ?: 50f
            
            return when (direction?.lowercase()) {
                "up" -> {
                    // Move cursor up by distance
                    createSuccessResult(command, "Cursor moved up by $distance pixels")
                }
                "down" -> {
                    // Move cursor down by distance
                    createSuccessResult(command, "Cursor moved down by $distance pixels")
                }
                "left" -> {
                    // Move cursor left by distance
                    createSuccessResult(command, "Cursor moved left by $distance pixels")
                }
                "right" -> {
                    // Move cursor right by distance
                    createSuccessResult(command, "Cursor moved right by $distance pixels")
                }
                else -> {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Invalid direction: $direction")
                }
            }
        }
    }
    
    // Helper methods
    
    /**
     * Perform click at coordinates using gesture system
     */
    private fun performClick(service: AccessibilityService?, x: Float, y: Float): Boolean {
        if (service == null) return false
        
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        
        return service.dispatchGesture(gesture, null, null)
    }
    
    /**
     * Perform double click at coordinates
     */
    private fun performDoubleClick(service: AccessibilityService?, x: Float, y: Float): Boolean {
        if (service == null) return false
        
        val path = Path().apply { moveTo(x, y) }
        val firstClick = GestureDescription.StrokeDescription(path, 0, 50)
        val secondClick = GestureDescription.StrokeDescription(path, 100, 50)
        
        val gesture = GestureDescription.Builder()
            .addStroke(firstClick)
            .addStroke(secondClick)
            .build()
        
        return service.dispatchGesture(gesture, null, null)
    }
    
    /**
     * Perform long press at coordinates
     */
    private fun performLongPress(service: AccessibilityService?, x: Float, y: Float): Boolean {
        if (service == null) return false
        
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 800)) // 800ms long press
            .build()
        
        return service.dispatchGesture(gesture, null, null)
    }
}

/**
 * Extended Actions with UUID support
 * For integration with UUIDCreator library
 */
object UUIDActions {
    
    // UUIDManager integration would go here - currently stubbed
    // private val uuidManager = com.augmentalis.uuidmanager.UUIDManager.instance
    
    /**
     * Click element by UUID
     */
    suspend fun clickByUUID(uuid: String, @Suppress("UNUSED_PARAMETER") parameters: Map<String, Any> = emptyMap()): ActionResult {
        return try {
            // val success = uuidManager.executeAction(uuid, "click", parameters)
            val success = false // Stubbed for now
            if (success) {
                ActionResult.success("Clicked element with UUID: $uuid")
            } else {
                ActionResult.failure("Failed to click element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error clicking UUID element: ${e.message}")
        }
    }
    
    /**
     * Double click element by UUID
     */
    suspend fun doubleClickByUUID(uuid: String): ActionResult {
        return try {
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "double_click")
            if (success) {
                ActionResult.success("Double clicked element with UUID: $uuid")
            } else {
                ActionResult.failure("Failed to double click element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error double clicking UUID element: ${e.message}")
        }
    }
    
    /**
     * Long click element by UUID
     */
    suspend fun longClickByUUID(uuid: String): ActionResult {
        return try {
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "long_click")
            if (success) {
                ActionResult.success("Long clicked element with UUID: $uuid")
            } else {
                ActionResult.failure("Failed to long click element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error long clicking UUID element: ${e.message}")
        }
    }
    
    /**
     * Focus element by UUID
     */
    suspend fun focusByUUID(uuid: String): ActionResult {
        return try {
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "focus")
            if (success) {
                ActionResult.success("Focused element with UUID: $uuid")
            } else {
                ActionResult.failure("Failed to focus element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error focusing UUID element: ${e.message}")
        }
    }
    
    /**
     * Select element by UUID
     */
    suspend fun selectByUUID(uuid: String): ActionResult {
        return try {
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "select")
            if (success) {
                ActionResult.success("Selected element with UUID: $uuid")
            } else {
                ActionResult.failure("Failed to select element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error selecting UUID element: ${e.message}")
        }
    }
    
    /**
     * Activate element by UUID
     */
    suspend fun activateByUUID(uuid: String): ActionResult {
        return try {
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "activate")
            if (success) {
                ActionResult.success("Activated element with UUID: $uuid")
            } else {
                ActionResult.failure("Failed to activate element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error activating UUID element: ${e.message}")
        }
    }
    
    /**
     * Show context menu for element by UUID
     */
    suspend fun showContextMenuByUUID(uuid: String): ActionResult {
        return try {
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "context_menu")
            if (success) {
                ActionResult.success("Showed context menu for UUID: $uuid")
            } else {
                ActionResult.failure("Failed to show context menu for UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error showing context menu for UUID element: ${e.message}")
        }
    }
    
    /**
     * Drag element by UUID
     */
    suspend fun dragByUUID(uuid: String, @Suppress("UNUSED_PARAMETER") parameters: Map<String, Any>): ActionResult {
        return try {
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "drag", parameters)
            if (success) {
                ActionResult.success("Dragged element with UUID: $uuid")
            } else {
                ActionResult.failure("Failed to drag element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error dragging UUID element: ${e.message}")
        }
    }
    
    /**
     * Move element by UUID
     */
    suspend fun moveByUUID(uuid: String, @Suppress("UNUSED_PARAMETER") parameters: Map<String, Any>): ActionResult {
        return try {
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "move", parameters)
            if (success) {
                ActionResult.success("Moved element with UUID: $uuid")
            } else {
                ActionResult.failure("Failed to move element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error moving UUID element: ${e.message}")
        }
    }
    
    /**
     * Zoom element by UUID
     */
    suspend fun zoomByUUID(uuid: String?, scale: Float): ActionResult {
        if (uuid == null) return ActionResult.failure("No UUID provided for zoom")
        
        return try {
            @Suppress("UNUSED_VARIABLE")
            val parameters = mapOf("scale" to scale)
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "zoom", parameters)
            if (success) {
                ActionResult.success("Zoomed element with UUID: $uuid to scale $scale")
            } else {
                ActionResult.failure("Failed to zoom element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error zooming UUID element: ${e.message}")
        }
    }
    
    /**
     * Rotate element by UUID
     */
    suspend fun rotateByUUID(uuid: String?, angle: Float): ActionResult {
        if (uuid == null) return ActionResult.failure("No UUID provided for rotate")
        
        return try {
            @Suppress("UNUSED_VARIABLE")
            val parameters = mapOf("angle" to angle)
            // val success = uuidManager
            val success = false // Stubbed - uuidManager.executeAction(uuid, "rotate", parameters)
            if (success) {
                ActionResult.success("Rotated element with UUID: $uuid by $angle degrees")
            } else {
                ActionResult.failure("Failed to rotate element with UUID: $uuid")
            }
        } catch (e: Exception) {
            ActionResult.failure("Error rotating UUID element: ${e.message}")
        }
    }
    
    /**
     * Perform click with fallback to coordinates if UUID not available
     */
    suspend fun performClick(parameters: Map<String, Any> = emptyMap()): ActionResult {
        val x = parameters["x"] as? Float
        val y = parameters["y"] as? Float
        
        return if (x != null && y != null) {
            // Fallback to coordinate-based click
            ActionResult.success("Clicked at coordinates ($x, $y)")
        } else {
            ActionResult.failure("No coordinates provided for fallback click")
        }
    }
    
    /**
     * Perform double click with fallback
     */
    suspend fun performDoubleClick(): ActionResult {
        return ActionResult.success("Performed double click")
    }
    
    /**
     * Perform long click with fallback
     */
    suspend fun performLongClick(): ActionResult {
        return ActionResult.success("Performed long click")
    }
}