/**
 * UIHandler.kt - Handles UI element interaction
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.accessibility.IVoiceOSContext
import android.accessibilityservice.AccessibilityService as AndroidAccessibilityService

/**
 * Handler for UI element interactions
 */
class UIHandler(
    private val context: IVoiceOSContext
) : ActionHandler {
    
    companion object {
        private const val TAG = "UIHandler"
        
        val SUPPORTED_ACTIONS = listOf(
            "click", "tap", "press",
            "long click", "long press",
            "double tap", "double click",
            "expand", "collapse",
            "check", "uncheck", "toggle",
            "focus", "dismiss", "close"
        )
    }
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()
        
        Log.d(TAG, "Executing UI action: $normalizedAction")
        
        return when {
            // Click/Tap actions
            normalizedAction.startsWith("click ") || 
            normalizedAction.startsWith("tap ") || 
            normalizedAction.startsWith("press ") -> {
                val target = normalizedAction
                    .removePrefix("click ")
                    .removePrefix("tap ")
                    .removePrefix("press ")
                    .trim()
                performClick(target)
            }
            
            // Long click
            normalizedAction.startsWith("long click ") || 
            normalizedAction.startsWith("long press ") -> {
                val target = normalizedAction
                    .removePrefix("long click ")
                    .removePrefix("long press ")
                    .trim()
                performLongClick(target)
            }
            
            // Double tap
            normalizedAction.startsWith("double tap ") || 
            normalizedAction.startsWith("double click ") -> {
                val target = normalizedAction
                    .removePrefix("double tap ")
                    .removePrefix("double click ")
                    .trim()
                performDoubleClick(target)
            }
            
            // Expand/Collapse
            normalizedAction.startsWith("expand ") -> {
                val target = normalizedAction.removePrefix("expand ").trim()
                performExpand(target)
            }
            
            normalizedAction.startsWith("collapse ") -> {
                val target = normalizedAction.removePrefix("collapse ").trim()
                performCollapse(target)
            }
            
            // Check/Toggle
            normalizedAction.startsWith("check ") -> {
                val target = normalizedAction.removePrefix("check ").trim()
                performCheck(target, true)
            }
            
            normalizedAction.startsWith("uncheck ") -> {
                val target = normalizedAction.removePrefix("uncheck ").trim()
                performCheck(target, false)
            }
            
            normalizedAction.startsWith("toggle ") -> {
                val target = normalizedAction.removePrefix("toggle ").trim()
                performToggle(target)
            }
            
            // Focus/Select
            normalizedAction.startsWith("focus ") -> {
                val target = normalizedAction.removePrefix("focus ").trim()
                performFocus(target)
            }

            
            // Dismiss/Close
            normalizedAction == "dismiss" || normalizedAction == "close" -> {
                performDismiss()
            }
            
            else -> {
                Log.w(TAG, "Unknown UI action: $normalizedAction")
                false
            }
        }
    }
    
    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.startsWith(it) }
    }
    
    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS.map { 
            if (it in listOf("dismiss", "close")) {
                it
            } else {
                "$it <element>"
            }
        }
    }
    
    private fun performClick(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target)
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
    
    private fun performLongClick(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target)
        return node?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) ?: false
    }
    
    private fun performDoubleClick(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target)
        if (node == null) return false
        
        // Perform two clicks in quick succession
        val firstClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Thread.sleep(50) // Small delay between clicks
        val secondClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        
        return firstClick && secondClick
    }
    
    private fun performExpand(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target)
        return node?.performAction(AccessibilityNodeInfo.ACTION_EXPAND) ?: false
    }
    
    private fun performCollapse(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target)
        return node?.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE) ?: false
    }
    
    private fun performCheck(target: String, check: Boolean): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target)
        if (node == null) return false
        
        // Check current state
        val isChecked = node.isChecked
        if (isChecked == check) return true // Already in desired state
        
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
    
    private fun performToggle(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target)
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
    
    private fun performFocus(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target)
        return node?.performAction(AccessibilityNodeInfo.ACTION_FOCUS) ?: false
    }
    
    private fun performDismiss(): Boolean {
        // Try to find and click dismiss/close button
        context.getRootNodeInActiveWindow() ?: return false

        // Look for common dismiss elements
        val dismissNode = findNodeByText("dismiss")
            ?: findNodeByText("close")
            ?: findNodeByText("cancel")
            ?: findNodeByText("ok")
            ?: findNodeByDescription("dismiss")
            ?: findNodeByDescription("close")

        if (dismissNode != null) {
            return dismissNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Fallback to back action
        return context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_BACK)
    }
    
    private fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val rootNode = context.getRootNodeInActiveWindow() ?: return null
        return findNodeByTextRecursive(rootNode, text.lowercase())
    }
    
    private fun findNodeByTextRecursive(
        node: AccessibilityNodeInfo, 
        searchText: String
    ): AccessibilityNodeInfo? {
        // Check this node's text
        node.text?.toString()?.lowercase()?.let { nodeText ->
            if (nodeText.contains(searchText)) {
                return node
            }
        }
        
        // Check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByTextRecursive(child, searchText)
                if (result != null) return result
            }
        }
        
        return null
    }
    
    private fun findNodeByDescription(description: String): AccessibilityNodeInfo? {
        val rootNode = context.getRootNodeInActiveWindow() ?: return null
        return findNodeByDescriptionRecursive(rootNode, description.lowercase())
    }
    
    private fun findNodeByDescriptionRecursive(
        node: AccessibilityNodeInfo,
        searchDescription: String
    ): AccessibilityNodeInfo? {
        // Check this node's content description
        node.contentDescription?.toString()?.lowercase()?.let { nodeDescription ->
            if (nodeDescription.contains(searchDescription)) {
                return node
            }
        }
        
        // Check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByDescriptionRecursive(child, searchDescription)
                if (result != null) return result
            }
        }
        
        return null
    }
    
    private fun findNodeAtCoordinatesRecursive(
        node: AccessibilityNodeInfo,
        x: Int, 
        y: Int
    ): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        if (bounds.contains(x, y)) {
            // Check if this node is clickable
            if (node.isClickable) {
                return node
            }
            
            // Check children for more specific match
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    val result = findNodeAtCoordinatesRecursive(child, x, y)
                    if (result != null) return result
                }
            }
            
            // Return this node if no clickable child found
            return node
        }
        
        return null
    }
}