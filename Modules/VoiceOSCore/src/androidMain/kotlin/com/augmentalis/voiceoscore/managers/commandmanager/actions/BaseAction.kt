/**
 * BaseAction.kt - Base class for all command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/BaseAction.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: Base implementation for all command actions
 */

package com.augmentalis.voiceoscore.managers.commandmanager.actions

import com.augmentalis.voiceoscore.*
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Base class for all command actions
 * Provides common functionality and utilities
 */
abstract class BaseAction {
    
    companion object {
        private const val TAG = "BaseAction"
    }
    
    /**
     * Execute the command action
     */
    abstract suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): ActionResult
    
    /**
     * Main entry point - implements CommandHandler pattern
     */
    suspend operator fun invoke(command: Command): ActionResult {
        return try {
            val context = getContext(command)
            val accessibilityService = getAccessibilityService(command)

            execute(command, accessibilityService, context)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error executing action for command ${command.id}", e)
            ActionResult(
                success = false,
                command = command,
                error = CommandError(
                    code = ErrorCode.EXECUTION_FAILED,
                    message = e.message ?: "Unknown error during execution"
                )
            )
        }
    }
    
    /**
     * Get context from command
     */
    protected fun getContext(command: Command): Context {
        val context = command.context?.deviceState?.get("androidContext") as? Context

        if (context == null) {
            android.util.Log.e(TAG, "CRITICAL: Android context not available in command")
            android.util.Log.e(TAG, "  Command ID: ${command.id}")
            android.util.Log.e(TAG, "  Context present: ${command.context != null}")
            android.util.Log.e(TAG, "  DeviceState keys: ${command.context?.deviceState?.keys}")
            throw IllegalStateException(
                "Android context not available. " +
                "VoiceOSService must add 'androidContext' to CommandContext.deviceState"
            )
        }

        return context
    }

    /**
     * Get accessibility service from command
     */
    protected fun getAccessibilityService(command: Command): AccessibilityService? {
        return command.context?.deviceState?.get("accessibilityService") as? AccessibilityService
    }
    
    /**
     * Perform global accessibility action
     */
    protected fun performGlobalAction(
        service: AccessibilityService?,
        action: Int
    ): Boolean {
        return service?.performGlobalAction(action) ?: false
    }
    
    /**
     * Find node by text
     */
    protected fun findNodeByText(
        rootNode: AccessibilityNodeInfo?,
        text: String,
        exactMatch: Boolean = false
    ): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        
        val textToFind = text.lowercase()
        
        // Check current node
        val nodeText = rootNode.text?.toString()?.lowercase() ?: ""
        val contentDesc = rootNode.contentDescription?.toString()?.lowercase() ?: ""
        
        val matches = if (exactMatch) {
            nodeText == textToFind || contentDesc == textToFind
        } else {
            nodeText.contains(textToFind) || contentDesc.contains(textToFind)
        }
        
        if (matches) return rootNode
        
        // Check children
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let { 
                val found = findNodeByText(it, text, exactMatch)
                if (found != null) return found
            }
        }
        
        return null
    }
    
    /**
     * Find clickable node by text
     */
    protected fun findClickableNodeByText(
        rootNode: AccessibilityNodeInfo?,
        text: String
    ): AccessibilityNodeInfo? {
        val node = findNodeByText(rootNode, text)
        return if (node?.isClickable == true) node else null
    }
    
    /**
     * Get node center point
     */
    protected fun getNodeCenter(node: AccessibilityNodeInfo): PointF {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return PointF(
            bounds.centerX().toFloat(),
            bounds.centerY().toFloat()
        )
    }
    
    /**
     * Check if action is available
     */
    protected fun isActionAvailable(
        node: AccessibilityNodeInfo?,
        action: AccessibilityNodeInfo.AccessibilityAction
    ): Boolean {
        return node?.actionList?.contains(action) == true
    }
    
    /**
     * Extract text parameter from command
     */
    protected fun getTextParameter(command: Command, paramName: String = "text"): String? {
        return command.parameters[paramName] as? String
    }
    
    /**
     * Extract number parameter from command
     */
    protected fun getNumberParameter(command: Command, paramName: String): Number? {
        return command.parameters[paramName] as? Number
    }
    
    /**
     * Extract boolean parameter from command
     */
    protected fun getBooleanParameter(command: Command, paramName: String): Boolean? {
        return command.parameters[paramName] as? Boolean
    }
    
    /**
     * Create success result
     */
    protected fun createSuccessResult(
        command: Command,
        message: String,
        data: Any? = null
    ): ActionResult {
        return ActionResult(
            success = true,
            command = command,
            response = message,
            data = data
        )
    }

    /**
     * Create error result
     */
    protected fun createErrorResult(
        command: Command,
        errorCode: ErrorCode,
        message: String
    ): ActionResult {
        return ActionResult(
            success = false,
            command = command,
            error = CommandError(errorCode, message)
        )
    }
}

/**
 * Accessibility action performer interface
 */
interface AccessibilityActionPerformer {
    /**
     * Perform accessibility action on node
     */
    fun performAction(
        node: AccessibilityNodeInfo,
        action: AccessibilityNodeInfo.AccessibilityAction,
        arguments: android.os.Bundle? = null
    ): Boolean
    
    /**
     * Perform global accessibility action
     */
    fun performGlobalAction(action: Int): Boolean
}

/**
 * Touch action performer interface
 */
interface TouchActionPerformer {
    /**
     * Perform click at coordinates
     */
    fun click(x: Float, y: Float): Boolean
    
    /**
     * Perform long click at coordinates
     */
    fun longClick(x: Float, y: Float): Boolean
    
    /**
     * Perform swipe gesture
     */
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long): Boolean
    
    /**
     * Perform drag gesture
     */
    fun drag(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long): Boolean
}