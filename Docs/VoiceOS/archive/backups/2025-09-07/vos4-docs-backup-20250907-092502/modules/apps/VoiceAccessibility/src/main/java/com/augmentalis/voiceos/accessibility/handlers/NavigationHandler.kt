/**
 * NavigationHandler.kt - Handles UI navigation actions
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-26
 * 
 * Handles scrolling, swiping, and UI navigation.
 * Implements ActionHandler interface (approved VOS4 exception).
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService

/**
 * Handler for navigation actions
 */
class NavigationHandler(
    private val service: VoiceAccessibilityService
) : ActionHandler {
    
    companion object {
        private const val TAG = "NavigationHandler"
        
        private val SUPPORTED_ACTIONS = listOf(
            "scroll up", "scroll down", 
            "scroll left", "scroll right",
            "swipe up", "swipe down",
            "swipe left", "swipe right",
            "next", "previous",
            "page up", "page down"
        )
    }
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()
        val rootNode = service.rootInActiveWindow
        
        if (rootNode == null) {
            Log.w(TAG, "No active window for navigation")
            return false
        }
        
        return when (normalizedAction) {
            "scroll up", "page up" -> {
                performScrollAction(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            }
            
            "scroll down", "page down" -> {
                performScrollAction(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
            
            "scroll left" -> {
                performScrollAction(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, horizontal = true)
            }
            
            "scroll right" -> {
                performScrollAction(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, horizontal = true)
            }
            
            "swipe up" -> {
                // Swipe up = scroll down (content moves up)
                performScrollAction(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
            
            "swipe down" -> {
                // Swipe down = scroll up (content moves down)  
                performScrollAction(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            }
            
            "swipe left" -> {
                performScrollAction(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, horizontal = true)
            }
            
            "swipe right" -> {
                performScrollAction(rootNode, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, horizontal = true)
            }
            
            "next" -> {
                rootNode.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)
            }
            
            "previous" -> {
                rootNode.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY)
            }
            
            else -> {
                Log.w(TAG, "Unknown navigation action: $normalizedAction")
                false
            }
        }
    }
    
    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized == it }
    }
    
    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }
    
    private fun performScrollAction(
        rootNode: AccessibilityNodeInfo, 
        action: Int,
        horizontal: Boolean = false
    ): Boolean {
        // Find scrollable node
        val scrollableNode = findScrollableNode(rootNode, horizontal)
        return scrollableNode?.performAction(action) ?: false
    }
    
    private fun findScrollableNode(
        node: AccessibilityNodeInfo,
        horizontal: Boolean
    ): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findScrollableNode(child, horizontal)
                if (result != null) return result
            }
        }
        
        return null
    }
}